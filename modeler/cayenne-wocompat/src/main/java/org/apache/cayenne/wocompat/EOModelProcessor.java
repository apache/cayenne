/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.wocompat;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.AbstractQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.util.NamedObjectFactory;
import org.apache.cayenne.wocompat.parser.Parser;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class for converting stored Apple EOModel mapping files to Cayenne DataMaps.
 */
public class EOModelProcessor {

    private static final Log logger = LogFactory.getLog(EOModelProcessor.class);

    protected Predicate prototypeChecker;

    public EOModelProcessor() {
        prototypeChecker = new Predicate() {

            public boolean evaluate(Object object) {
                if (object == null) {
                    return false;
                }

                String entityName = object.toString();
                return entityName.startsWith("EO") && entityName.endsWith("Prototypes");
            }
        };
    }

    /**
     * @deprecated since 3.2 in favor of {@link #loadModeIndex(URL)}.
     */
    @Deprecated
    public Map loadModeIndex(String path) throws Exception {
        return loadModeIndex(new File(path).toURI().toURL());
    }

    /**
     * Returns index.eomodeld contents as a Map.
     * 
     * @since 3.2
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
        InputStream in = new URL(url, "index.eomodeld").openStream();

        try {
            plistParser.ReInit(in);
            return (Map) plistParser.propertyList();
        } finally {
            in.close();
        }
    }

    /**
     * @deprecated since 3.2 in favor of {@link #loadEOModel(URL)}.
     */
    @Deprecated
    public DataMap loadEOModel(String path) throws Exception {
        return loadEOModel(path, false);
    }

    /**
     * @deprecated since 3.2 in favor of {@link #loadEOModel(URL, boolean)}.
     */
    @Deprecated
    public DataMap loadEOModel(String path, boolean generateClientClass) throws Exception {
        return loadEOModel(new File(path).toURI().toURL(), generateClientClass);
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
        List modelNames = new ArrayList(helper.modelNamesAsList());
        CollectionUtils.filter(modelNames, PredicateUtils.notPredicate(prototypeChecker));

        Iterator it = modelNames.iterator();
        while (it.hasNext()) {
            String name = (String) it.next();

            // create and register entity
            makeEntity(helper, name, generateClientClass);
        }

        // now sort following inheritance hierarchy
        Collections.sort(modelNames, new InheritanceComparator(dataMap));

        // after all entities are loaded, process attributes
        it = modelNames.iterator();
        while (it.hasNext()) {
            String name = (String) it.next();

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
        return prototypeChecker.evaluate(entityName);
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
    protected Query makeQuery(EOModelHelper helper, EOObjEntity entity, String queryName) {

        DataMap dataMap = helper.getDataMap();
        Map queryPlist = helper.queryPListMap(entity.getName(), queryName);
        if (queryPlist == null) {
            return null;
        }

        AbstractQuery query;
        if (queryPlist.containsKey("hints")) { // just a predefined SQL query
            query = new EOSQLQuery(entity, queryPlist);
        } else {
            query = new EOQuery(entity, queryPlist);
        }
        query.setName(entity.qualifiedQueryName(queryName));
        dataMap.addQuery(query);

        return query;
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
        objEntity.setServerOnly(!generateClientClass);
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

    /**
     * Create ObjAttributes of the specified entity, as well as DbAttributes of
     * the corresponding DbEntity.
     */
    protected void makeAttributes(EOModelHelper helper, EOObjEntity objEntity) {
        Map entityPlistMap = helper.entityPListMap(objEntity.getName());
        List primaryKeys = (List) entityPlistMap.get("primaryKeyAttributes");

        List classProperties;
        if (objEntity.isServerOnly()) {
            classProperties = (List) entityPlistMap.get("classProperties");
        } else {
            classProperties = (List) entityPlistMap.get("clientClassProperties");
        }

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

            String dbAttrName = (String) attrMap.get("columnName");
            if (null == dbAttrName) {
                dbAttrName = (String) prototypeAttrMap.get("columnName");
            }

            String attrName = (String) attrMap.get("name");
            if (null == attrName) {
                attrName = (String) prototypeAttrMap.get("name");
            }

            String attrType = (String) attrMap.get("valueClassName");
            if (null == attrType) {
                attrType = (String) prototypeAttrMap.get("valueClassName");
            }

            String valueType = (String) attrMap.get("valueType");
            if (valueType == null) {
                valueType = (String) prototypeAttrMap.get("valueType");
            }

            String javaType = helper.javaTypeForEOModelerType(attrType, valueType);
            EODbAttribute dbAttr = null;

            if (dbAttrName != null && dbEntity != null) {

                // if inherited attribute, skip it for DbEntity...
                if (!singleTableInheritance || dbEntity.getAttribute(dbAttrName) == null) {

                    // create DbAttribute...since EOF allows the same column
                    // name for
                    // more than one Java attribute, we need to check for name
                    // duplicates
                    int i = 0;
                    String dbAttributeBaseName = dbAttrName;
                    while (dbEntity.getAttribute(dbAttrName) != null) {
                        dbAttrName = dbAttributeBaseName + i++;
                    }

                    dbAttr = new EODbAttribute(dbAttrName, TypesMapping.getSqlTypeByJava(javaType), dbEntity);
                    dbAttr.setEoAttributeName(attrName);
                    dbEntity.addAttribute(dbAttr);

                    int width = getInt("width", attrMap, prototypeAttrMap, -1);
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
                    // TODO: Unclear that allowsNull should be inherited from
                    // EOPrototypes
                    // if (null == allowsNull) allowsNull =
                    // prototypeAttrMap.get("allowsNull");;

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
            boolean toDependentPK = "Y".equals(relMap.get("propagatesPrimaryKey"));
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
                    dbRel.setTargetEntity(dbTarget);
                    dbRel.setToMany(toMany);
                    dbRel.setName(relName);
                    dbRel.setToDependentPK(toDependentPK);
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
                rel.setTargetEntity(target);
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

        for (DbRelationship relationship : new ArrayList<DbRelationship>(dbEntity.getRelationships())) {

            if (relationship.getReverseRelationship() == null) {
                DbRelationship reverse = relationship.createReverseRelationship();

                String name = NamedObjectFactory.createName(DbRelationship.class, reverse.getSourceEntity(),
                        relationship.getName() + "Reverse");
                reverse.setName(name);
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

    // sorts ObjEntities so that subentities in inheritance hierarchy are shown
    // last
    final class InheritanceComparator implements Comparator {

        DataMap dataMap;

        InheritanceComparator(DataMap dataMap) {
            this.dataMap = dataMap;
        }

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
