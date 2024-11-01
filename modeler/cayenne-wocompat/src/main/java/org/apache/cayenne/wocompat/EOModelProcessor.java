/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.wocompat;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.SQLTemplateDescriptor;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.wocompat.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Class for converting stored Apple EOModel mapping files to Cayenne DataMaps.
 */
public class EOModelProcessor {

	private static final Logger logger = LoggerFactory.getLogger(EOModelProcessor.class);

	protected Predicate<String> prototypeChecker;

	public EOModelProcessor() {
		prototypeChecker = entityName -> {
            if (entityName == null) {
                return false;
            }
            return entityName.startsWith("EO") && entityName.endsWith("Prototypes");
        };
	}

	/**
	 * Returns index.eomodeld contents as a Map.
	 * 
	 * @since 4.0
	 */
	// TODO: refactor EOModelHelper to provide a similar method without loading
	// all entity files in memory... here we simply copied stuff from
	// EOModelHelper
	public Map loadModeIndex(URL url) throws Exception {

		String urlString = url.toExternalForm();

		if (!urlString.endsWith(".eomodeld")) {
			url = new URL(urlString + ".eomodeld");
		}

		Parser plistParser = new Parser();

		try (InputStream in = new URL(url, "index.eomodeld").openStream();) {
			plistParser.ReInit(in);
			return (Map) plistParser.propertyList();
		}
	}

	/**
	 * Performs EOModel loading.
	 * 
	 * @param url
	 *            URL of ".eomodeld" directory.
	 */
	public DataMap loadEOModel(URL url) throws Exception {
		return loadEOModel(url, false);
	}

	/**
	 * Performs EOModel loading.
	 * 
	 * @param url
	 *            URL of ".eomodeld" directory.
	 * @param generateClientClass
	 *            if true then loading of EOModel is java client classes aware
	 *            and the following processing will work with Java client class
	 *            settings of the EOModel.
	 */
	public DataMap loadEOModel(URL url, boolean generateClientClass) throws Exception {
		EOModelHelper helper = makeHelper(url);

		// create empty map
		DataMap dataMap = helper.getDataMap();

		// process enitities ... throw out prototypes ... for now
		List<String> modelNames = new ArrayList<>(helper.modelNamesAsList());
		modelNames = modelNames.stream().filter(prototypeChecker.negate()).collect(Collectors.toList());
		modelNames.forEach( name -> {
			// create and register entity
			makeEntity(helper, name, generateClientClass);
		});

		// now sort following inheritance hierarchy
		modelNames.sort(new InheritanceComparator(dataMap));

		// after all entities are loaded, process attributes
		Iterator<String> it = modelNames.iterator();
		while (it.hasNext()) {
			String name = it.next();

			EOObjEntity e = (EOObjEntity) dataMap.getObjEntity(name);
			// process entity attributes
			makeAttributes(helper, e);
		}

		// after all entities are loaded, process relationships
		it = modelNames.iterator();
		while (it.hasNext()) {
			String name = (String) it.next();
			makeRelationships(helper, dataMap.getObjEntity(name));
		}

		// after all normal relationships are loaded, process flattened
		// relationships
		it = modelNames.iterator();
		while (it.hasNext()) {
			String name = (String) it.next();
			makeFlatRelationships(helper, dataMap.getObjEntity(name));
		}

		// now create missing reverse DB (but not OBJ) relationships
		// since Cayenne requires them
		it = modelNames.iterator();
		while (it.hasNext()) {
			String name = (String) it.next();
			DbEntity dbEntity = dataMap.getObjEntity(name).getDbEntity();

			if (dbEntity != null) {
				makeReverseDbRelationships(dbEntity);
			}
		}

		// build SelectQueries out of EOFetchSpecifications...
		it = modelNames.iterator();
		while (it.hasNext()) {
			String name = (String) it.next();
			Iterator queries = helper.queryNames(name);
			while (queries.hasNext()) {
				String queryName = (String) queries.next();
				EOObjEntity entity = (EOObjEntity) dataMap.getObjEntity(name);
				makeQuery(helper, entity, queryName);
			}
		}

		return dataMap;
	}

	/**
	 * Returns whether an Entity is an EOF EOPrototypes entity. According to EOF
	 * conventions EOPrototypes and EO[Adapter]Prototypes entities are
	 * considered to be prototypes.
	 * 
	 * @since 1.1
	 */
	protected boolean isPrototypesEntity(String entityName) {
		return prototypeChecker.test(entityName);
	}

	/**
	 * Creates an returns new EOModelHelper to process EOModel. Exists mostly
	 * for the benefit of subclasses.
	 */
	protected EOModelHelper makeHelper(URL url) throws Exception {
		return new EOModelHelper(url);
	}

	/**
	 * Creates a Cayenne query out of EOFetchSpecification data.
	 * 
	 * @since 1.1
	 */
	protected QueryDescriptor makeQuery(EOModelHelper helper, EOObjEntity entity, String queryName) {

		DataMap dataMap = helper.getDataMap();
		Map queryPlist = helper.queryPListMap(entity.getName(), queryName);
		if (queryPlist == null) {
			return null;
		}

		QueryDescriptor query;
		if (queryPlist.containsKey("hints")) { // just a predefined SQL query
			query = makeEOSQLQueryDescriptor(entity, queryPlist);
		} else {
			query = makeEOQueryDescriptor(entity, queryPlist);
		}
		query.setName(entity.qualifiedQueryName(queryName));
		dataMap.addQueryDescriptor(query);

		return query;
	}

	protected QueryDescriptor makeEOQueryDescriptor(ObjEntity root, Map plistMap) {
		SelectQueryDescriptor descriptor = QueryDescriptor.selectQueryDescriptor();
		descriptor.setRoot(root);

		descriptor.setDistinct("YES".equalsIgnoreCase((String) plistMap.get("usesDistinct")));

		Object fetchLimit = plistMap.get("fetchLimit");
		if (fetchLimit != null) {
			try {
				if (fetchLimit instanceof Number) {
					descriptor.setProperty(QueryMetadata.FETCH_LIMIT_PROPERTY,
							String.valueOf(((Number) fetchLimit).intValue()));
				} else if (isNumeric(fetchLimit.toString())) {
					descriptor.setProperty(QueryMetadata.FETCH_LIMIT_PROPERTY, fetchLimit.toString());
				}
			} catch (NumberFormatException nfex) {
				// ignoring...
			}
		}

		// sort orderings
		List<Map<String, String>> orderings = (List<Map<String, String>>) plistMap.get("sortOrderings");
		if (orderings != null && !orderings.isEmpty()) {
			for (Map<String, String> ordering : orderings) {
				boolean asc = !"compareDescending:".equals(ordering.get("selectorName"));
				String key = ordering.get("key");
				if (key != null) {
					descriptor.addOrdering(new Ordering(key, asc ? SortOrder.ASCENDING : SortOrder.DESCENDING));
				}
			}
		}

		// qualifiers
		Map<String, ?> qualifierMap = (Map<String, ?>) plistMap.get("qualifier");
		if (qualifierMap != null && !qualifierMap.isEmpty()) {
			descriptor.setQualifier(EOQuery.EOFetchSpecificationParser.makeQualifier((EOObjEntity) root, qualifierMap));
		}

		// prefetches
		List prefetches = (List) plistMap.get("prefetchingRelationshipKeyPaths");
		if (prefetches != null && !prefetches.isEmpty()) {
			Iterator it = prefetches.iterator();
			while (it.hasNext()) {
				descriptor.addPrefetch((String) it.next(), PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
			}
		}

		// data rows - note that we do not support fetching individual columns
		// in the
		// modeler...
		if (plistMap.containsKey("rawRowKeyPaths")) {
			descriptor.setProperty(QueryMetadata.FETCHING_DATA_ROWS_PROPERTY, String.valueOf(true));
		}

		return descriptor;
	}

	protected QueryDescriptor makeEOSQLQueryDescriptor(ObjEntity root, Map plistMap) {
		SQLTemplateDescriptor descriptor = QueryDescriptor.sqlTemplateDescriptor();
		descriptor.setRoot(root);

		Object fetchLimit = plistMap.get("fetchLimit");
		if (fetchLimit != null) {
			try {
				if (fetchLimit instanceof Number) {
					descriptor.setProperty(QueryMetadata.FETCH_LIMIT_PROPERTY,
							String.valueOf(((Number) fetchLimit).intValue()));
				} else if (isNumeric(fetchLimit.toString())) {
					descriptor.setProperty(QueryMetadata.FETCH_LIMIT_PROPERTY, fetchLimit.toString());
				}
			} catch (NumberFormatException nfex) {
				// ignoring...
			}
		}

		//query
		// TODO: doesn't work with Stored Procedures.
		Map hints = (Map) plistMap.get("hints");
		if (hints != null && !hints.isEmpty()) {
			String sqlExpression = (String) hints.get("EOCustomQueryExpressionHintKey");
			if (sqlExpression != null) {
				descriptor.setSql(sqlExpression);
			}
		}

		return descriptor;
	}

	/**
	 * Creates and returns a new ObjEntity linked to a corresponding DbEntity.
	 */
	protected EOObjEntity makeEntity(EOModelHelper helper, String name, boolean generateClientClass) {

		DataMap dataMap = helper.getDataMap();
		Map entityPlist = helper.entityPListMap(name);

		// create ObjEntity
		EOObjEntity objEntity = new EOObjEntity(name);
		objEntity.setEoMap(entityPlist);
		String parent = (String) entityPlist.get("parent");
		objEntity.setClassName(helper.entityClass(name, generateClientClass));

		if (parent != null) {
			objEntity.setSubclass(true);
			objEntity.setSuperClassName(helper.entityClass(parent, generateClientClass));
		}

		// add flag whether this entity is set as abstract in the model
		objEntity.setAbstractEntity("Y".equals(entityPlist.get("isAbstractEntity")));

		// create DbEntity...since EOF allows the same table to be
		// associated with multiple EOEntities, check for name duplicates
		String dbEntityName = (String) entityPlist.get("externalName");
		if (dbEntityName != null) {

			// ... if inheritance is involved and parent hierarchy uses the same
			// DBEntity,
			// do not create a DbEntity...
			boolean createDbEntity = true;
			if (parent != null) {
				String parentName = parent;
				while (parentName != null) {
					Map parentData = helper.entityPListMap(parentName);
					if (parentData == null) {
						break;
					}

					String parentExternalName = (String) parentData.get("externalName");
					if (parentExternalName == null) {
						parentName = (String) parentData.get("parent");
						continue;
					}

					if (dbEntityName.equals(parentExternalName)) {
						createDbEntity = false;
					}

					break;
				}
			}

			if (createDbEntity) {
				int i = 0;
				String dbEntityBaseName = dbEntityName;
				while (dataMap.getDbEntity(dbEntityName) != null) {
					dbEntityName = dbEntityBaseName + i++;
				}

				objEntity.setDbEntityName(dbEntityName);
				DbEntity de = new DbEntity(dbEntityName);
				dataMap.addDbEntity(de);
			}
		}

		// set various flags
		objEntity.setReadOnly("Y".equals(entityPlist.get("isReadOnly")));
		objEntity.setSuperEntityName((String) entityPlist.get("parent"));

		dataMap.addObjEntity(objEntity);

		return objEntity;
	}
	
	private static boolean externalTypeIsClob(String type) {
		if( type == null ) return false;
		return "CLOB".equalsIgnoreCase(type) || "TEXT".equalsIgnoreCase(type);
	}

	/**
	 * Create ObjAttributes of the specified entity, as well as DbAttributes of
	 * the corresponding DbEntity.
	 */
	protected void makeAttributes(EOModelHelper helper, EOObjEntity objEntity) {
		Map entityPlistMap = helper.entityPListMap(objEntity.getName());
		List primaryKeys = (List) entityPlistMap.get("primaryKeyAttributes");

		List classProperties;
		classProperties = (List) entityPlistMap.get("classProperties");

		List attributes = (List) entityPlistMap.get("attributes");
		DbEntity dbEntity = objEntity.getDbEntity();

		if (primaryKeys == null) {
			primaryKeys = Collections.EMPTY_LIST;
		}

		if (classProperties == null) {
			classProperties = Collections.EMPTY_LIST;
		}

		if (attributes == null) {
			attributes = Collections.EMPTY_LIST;
		}

		// detect single table inheritance
		boolean singleTableInheritance = false;
		String parentName = (String) entityPlistMap.get("parent");
		while (parentName != null) {
			Map parentData = helper.entityPListMap(parentName);
			if (parentData == null) {
				break;
			}

			String parentExternalName = (String) parentData.get("externalName");
			if (parentExternalName == null) {
				parentName = (String) parentData.get("parent");
				continue;
			}

			if (dbEntity.getName() != null && dbEntity.getName().equals(parentExternalName)) {
				singleTableInheritance = true;
			}

			break;
		}

		Iterator it = attributes.iterator();
		while (it.hasNext()) {
			Map attrMap = (Map) it.next();

			String prototypeName = (String) attrMap.get("prototypeName");
			Map prototypeAttrMap = helper.getPrototypeAttributeMapFor(prototypeName);

			String dbAttrName = getStringValueFromMap( "columnName", attrMap, prototypeAttrMap );
			String attrName = getStringValueFromMap( "name", attrMap, prototypeAttrMap );
			String attrType = getStringValueFromMap( "valueClassName", attrMap, prototypeAttrMap );
			String valueType = getStringValueFromMap( "valueType", attrMap, prototypeAttrMap );
			String externalType = getStringValueFromMap( "externalType", attrMap, prototypeAttrMap );

			String javaType = helper.javaTypeForEOModelerType(attrType, valueType);
			EODbAttribute dbAttr = null;

			if (dbAttrName != null && dbEntity != null) {

				// if inherited attribute, skip it for DbEntity...
				if (!singleTableInheritance || dbEntity.getAttribute(dbAttrName) == null) {

					// create DbAttribute...since EOF allows the same column name for
					// more than one Java attribute, we need to check for name duplicates
					int i = 0;
					String dbAttributeBaseName = dbAttrName;
					while (dbEntity.getAttribute(dbAttrName) != null) {
						dbAttrName = dbAttributeBaseName + i++;
					}

					int sqlType = TypesMapping.getSqlTypeByJava(javaType);
					int width = getInt("width", attrMap, prototypeAttrMap, -1);
					if (sqlType == Types.VARCHAR && width < 0 && externalTypeIsClob(externalType)) {
						// CLOB, or TEXT as PostgreSQL calls it, is usally noted as having no width. In order to
						// not mistake any VARCHAR columns that just happen to have no width set in the model
						// for CLOB columns, use externalType as an additional check.
						sqlType = Types.CLOB;
					} else if(sqlType == TypesMapping.NOT_DEFINED && externalType != null) {
						// At this point we usually hit a custom Java class through a prototype, which isn't resolvable
						// with the model alone. But we can use the externalType as a hint. If that still doesn't match
						// anything, sqlType will still be NOT_DEFINED.
						sqlType = TypesMapping.getSqlTypeByName(externalType.toUpperCase());
					}
					dbAttr = new EODbAttribute(dbAttrName, sqlType, dbEntity);
					dbAttr.setEoAttributeName(attrName);
					dbEntity.addAttribute(dbAttr);

					if (width >= 0) {
						dbAttr.setMaxLength(width);
					}

					int scale = getInt("scale", attrMap, prototypeAttrMap, -1);
					if (scale >= 0) {
						dbAttr.setScale(scale);
					}

					if (primaryKeys.contains(attrName))
						dbAttr.setPrimaryKey(true);

					Object allowsNull = attrMap.get("allowsNull");
					// TODO: Unclear that allowsNull should be inherited from EOPrototypes
					// if (null == allowsNull) allowsNull = prototypeAttrMap.get("allowsNull");;

					dbAttr.setMandatory(!"Y".equals(allowsNull));
				}
			}

			if (classProperties.contains(attrName)) {
				EOObjAttribute attr = new EOObjAttribute(attrName, javaType, objEntity);

				// set readOnly flag of Attribute if either attribute is read or
				// if entity is readOnly
				String entityReadOnlyString = (String) entityPlistMap.get("isReadOnly");
				String attributeReadOnlyString = (String) attrMap.get("isReadOnly");
				if ("Y".equals(entityReadOnlyString) || "Y".equals(attributeReadOnlyString)) {
					attr.setReadOnly(true);
				}

				// set name instead of the actual attribute, as it may be
				// inherited....
				attr.setDbAttributePath(dbAttrName);
				objEntity.addAttribute(attr);
			}
		}
	}

	private String getStringValueFromMap( String key, Map attrMap, Map prototypeAttrMap ) {
		String dbAttrName = (String) attrMap.get(key);
		if (null == dbAttrName) {
			dbAttrName = (String) prototypeAttrMap.get(key);
		}
		return dbAttrName;
	}

	int getInt(String key, Map map, Map prototypes, int defaultValue) {

		Object value = map.get(key);
		if (value == null) {
			value = prototypes.get(key);
		}

		if (value == null) {
			return defaultValue;
		}

		// per CAY-752, value can be a String or a Number, so handle both
		if (value instanceof Number) {
			return ((Number) value).intValue();
		} else {
			try {
				return Integer.parseInt(value.toString());
			} catch (NumberFormatException nfex) {
				return defaultValue;
			}
		}
	}

	/**
	 * Create ObjRelationships of the specified entity, as well as
	 * DbRelationships of the corresponding DbEntity.
	 */
	protected void makeRelationships(EOModelHelper helper, ObjEntity objEntity) {
		Map entityPlistMap = helper.entityPListMap(objEntity.getName());
		List classProps = (List) entityPlistMap.get("classProperties");
		List rinfo = (List) entityPlistMap.get("relationships");

		Collection attributes = (Collection) entityPlistMap.get("attributes");

		if (rinfo == null) {
			return;
		}

		if (classProps == null) {
			classProps = Collections.EMPTY_LIST;
		}

		if (attributes == null) {
			attributes = Collections.EMPTY_LIST;
		}

		DbEntity dbSrc = objEntity.getDbEntity();
		Iterator it = rinfo.iterator();
		while (it.hasNext()) {
			Map relMap = (Map) it.next();
			String targetName = (String) relMap.get("destination");

			// ignore flattened relationships for now
			if (targetName == null) {
				continue;
			}

			String relName = (String) relMap.get("name");
			boolean toMany = "Y".equals(relMap.get("isToMany"));
			boolean isFK = !"Y".equals(relMap.get("propagatesPrimaryKey"));
			ObjEntity target = helper.getDataMap().getObjEntity(targetName);

			// target maybe null for cross-EOModel relationships
			// ignoring those now.
			if (target == null) {
				continue;
			}

			DbEntity dbTarget = target.getDbEntity();
			Map targetPlistMap = helper.entityPListMap(targetName);
			Collection targetAttributes = (Collection) targetPlistMap.get("attributes");
			DbRelationship dbRel = null;

			// process underlying DbRelationship
			// Note: there is no flattened rel. support here....
			// Note: source maybe null, e.g. an abstract entity.
			if (dbSrc != null && dbTarget != null) {

				// in case of inheritance EOF stores duplicates of all inherited
				// relationships, so we must skip this relationship in DB entity
				// if it is
				// already there...

				dbRel = dbSrc.getRelationship(relName);
				if (dbRel == null) {

					dbRel = new DbRelationship();
					dbRel.setSourceEntity(dbSrc);
					dbRel.setTargetEntityName(dbTarget);
					dbRel.setToMany(toMany);
					dbRel.setName(relName);
					dbRel.setFK(isFK);
					dbSrc.addRelationship(dbRel);

					List joins = (List) relMap.get("joins");
					Iterator jIt = joins.iterator();
					while (jIt.hasNext()) {
						Map joinMap = (Map) jIt.next();

						DbJoin join = new DbJoin(dbRel);

						// find source attribute dictionary and extract the
						// column name
						String sourceAttributeName = (String) joinMap.get("sourceAttribute");
						join.setSourceName(columnName(attributes, sourceAttributeName));

						String targetAttributeName = (String) joinMap.get("destinationAttribute");

						join.setTargetName(columnName(targetAttributes, targetAttributeName));
						dbRel.addJoin(join);
					}
				}
			}

			// only create obj relationship if it is a class property
			if (classProps.contains(relName)) {
				ObjRelationship rel = new ObjRelationship();
				rel.setName(relName);
				rel.setSourceEntity(objEntity);
				rel.setTargetEntityName(target);
				objEntity.addRelationship(rel);

				if (dbRel != null) {
					rel.addDbRelationship(dbRel);
				}
			}
		}
	}

	/**
	 * Create reverse DbRelationships that were not created so far, since
	 * Cayenne requires them.
	 * 
	 * @since 1.0.5
	 */
	protected void makeReverseDbRelationships(DbEntity dbEntity) {
		if (dbEntity == null) {
			throw new NullPointerException("Attempt to create reverse relationships for the null DbEntity.");
		}

		// iterate over a copy of the collection, since in case of
		// reflexive relationships, we may modify source entity relationship map

		for (DbRelationship relationship : new ArrayList<>(dbEntity.getRelationships())) {

			if (relationship.getReverseRelationship() == null) {
				DbRelationship reverse = relationship.createReverseRelationship();
				reverse.setName(NameBuilder.builder(reverse, reverse.getSourceEntity())
						// TODO: we can do better with ObjectNameGenerator
						.baseName(relationship.getName() + "Reverse")
						.name());
				relationship.getTargetEntity().addRelationship(reverse);
			}
		}
	}

	/**
	 * Create Flattened ObjRelationships of the specified entity.
	 */
	protected void makeFlatRelationships(EOModelHelper helper, ObjEntity e) {
		Map info = helper.entityPListMap(e.getName());
		List rinfo = (List) info.get("relationships");
		if (rinfo == null) {
			return;
		}

		Iterator it = rinfo.iterator();
		while (it.hasNext()) {
			Map relMap = (Map) it.next();
			String targetPath = (String) relMap.get("definition");

			// ignore normal relationships
			if (targetPath == null) {
				continue;
			}

			ObjRelationship flatRel = new ObjRelationship();
			flatRel.setName((String) relMap.get("name"));
			flatRel.setSourceEntity(e);

			try {
				flatRel.setDbRelationshipPath(targetPath);
			} catch (ExpressionException ex) {
				logger.warn("Invalid relationship: " + targetPath);
				continue;
			}

			// find target entity
			Map entityInfo = info;
			StringTokenizer toks = new StringTokenizer(targetPath, ".");
			while (toks.hasMoreTokens() && entityInfo != null) {
				String pathComponent = toks.nextToken();

				// get relationship info and reset entityInfo, so that we could
				// use
				// entityInfo state as an indicator of valid flat relationship
				// enpoint
				// outside the loop
				Collection relationshipInfo = (Collection) entityInfo.get("relationships");
				entityInfo = null;

				if (relationshipInfo == null) {
					break;
				}

				Iterator rit = relationshipInfo.iterator();
				while (rit.hasNext()) {
					Map pathRelationship = (Map) rit.next();
					if (pathComponent.equals(pathRelationship.get("name"))) {
						String targetName = (String) pathRelationship.get("destination");
						entityInfo = helper.entityPListMap(targetName);
						break;
					}
				}
			}

			if (entityInfo != null) {
				flatRel.setTargetEntityName((String) entityInfo.get("name"));
			}

			e.addRelationship(flatRel);
		}
	}

	/**
	 * Locates an attribute map matching the name and returns column name for
	 * this attribute.
	 * 
	 * @since 1.1
	 */
	String columnName(Collection entityAttributes, String attributeName) {
		if (attributeName == null) {
			return null;
		}

		Iterator it = entityAttributes.iterator();
		while (it.hasNext()) {
			Map map = (Map) it.next();
			if (attributeName.equals(map.get("name"))) {
				return (String) map.get("columnName");
			}
		}

		return null;
	}

	static boolean isNumeric(String str) {
		if (str == null) {
			return false;
		}

		for(int i = 0; i < str.length(); ++i) {
			if (!Character.isDigit(str.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	// sorts ObjEntities so that subentities in inheritance hierarchy are shown
	// last
	final class InheritanceComparator implements Comparator {

		DataMap dataMap;

		InheritanceComparator(DataMap dataMap) {
			this.dataMap = dataMap;
		}

		@Override
		public int compare(Object o1, Object o2) {
			if (o1 == null) {
				return o2 != null ? -1 : 0;
			} else if (o2 == null) {
				return 1;
			}

			String name1 = o1.toString();
			String name2 = o2.toString();

			ObjEntity e1 = dataMap.getObjEntity(name1);
			ObjEntity e2 = dataMap.getObjEntity(name2);

			return compareEntities(e1, e2);
		}

		int compareEntities(ObjEntity e1, ObjEntity e2) {
			if (e1 == null) {
				return e2 != null ? -1 : 0;
			} else if (e2 == null) {
				return 1;
			}

			// entity goes first if it is a direct or indirect superentity of
			// another
			// one
			if (e1.isSubentityOf(e2)) {
				return 1;
			}

			if (e2.isSubentityOf(e1)) {
				return -1;
			}

			// sort alphabetically
			return e1.getName().compareTo(e2.getName());
		}
	}
}
