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
package org.apache.cayenne.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.types.ValueObjectType;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.exp.TraversalHandler;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.exp.parser.ASTFunctionCall;
import org.apache.cayenne.exp.parser.ASTScalar;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.EntityResult;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.PathComponent;
import org.apache.cayenne.map.SQLResult;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;
import org.apache.cayenne.util.CayenneMapEntry;

/**
 * @since 3.0
 */
class SelectQueryMetadata extends BaseQueryMetadata {

	private static final long serialVersionUID = 7465922769303943945L;
	
	Map<String, String> pathSplitAliases;
	boolean isSingleResultSetMapping;
	boolean suppressingDistinct;

	@Override
	void copyFromInfo(QueryMetadata info) {
		super.copyFromInfo(info);
		this.pathSplitAliases = new HashMap<>(info.getPathSplitAliases());
	}

	boolean resolve(Object root, EntityResolver resolver, SelectQuery<?> query) {

		if (super.resolve(root, resolver, null)) {
			// generate unique cache key, but only if we are caching..
			if (cacheStrategy != null && cacheStrategy != QueryCacheStrategy.NO_CACHE) {
				this.cacheKey = makeCacheKey(query, resolver);
			}

			resolveAutoAliases(query);
			buildResultSetMappingForColumns(query, resolver);
			isSingleResultSetMapping = query.canReturnScalarValue() && super.isSingleResultSetMapping();

			return true;
		}

		return false;
	}

	private String makeCacheKey(SelectQuery<?> query, EntityResolver resolver) {

		// create a unique key based on entity or columns, qualifier, ordering, fetch offset and limit

		StringBuilder key = new StringBuilder();
		// handler to create string out of expressions, created lazily
		TraversalHandler traversalHandler = null;

		ObjEntity entity = getObjEntity();
		if (entity != null) {
			key.append(entity.getName());
		} else if (dbEntity != null) {
			key.append("db:").append(dbEntity.getName());
		}

		if(query.getColumns() != null && !query.getColumns().isEmpty()) {
			key.append("/");
			traversalHandler = new ToCacheKeyTraversalHandler(resolver.getValueObjectTypeRegistry(), key);
			for(Property<?> property : query.getColumns()) {
				key.append("c:");
				property.getExpression().traverse(traversalHandler);
			}
		}

		if (query.getQualifier() != null) {
			key.append('/');
			if(traversalHandler == null) {
				traversalHandler = new ToCacheKeyTraversalHandler(resolver.getValueObjectTypeRegistry(), key);
			}
			query.getQualifier().traverse(traversalHandler);
		}

		if (!query.getOrderings().isEmpty()) {
			for (Ordering o : query.getOrderings()) {
				key.append('/').append(o.getSortSpecString());
				if (!o.isAscending()) {
					key.append(":d");
				}

				if (o.isCaseInsensitive()) {
					key.append(":i");
				}
			}
		}

		if (query.getFetchOffset() > 0 || query.getFetchLimit() > 0) {
			key.append('/');
			if (query.getFetchOffset() > 0) {
				key.append('o').append(query.getFetchOffset());
			}
			if (query.getFetchLimit() > 0) {
				key.append('l').append(query.getFetchLimit());
			}
		}

		return key.toString();
	}

	private void resolveAutoAliases(SelectQuery<?> query) {
		Expression qualifier = query.getQualifier();
		if (qualifier != null) {
			resolveAutoAliases(qualifier);
		}

		// TODO: include aliases in prefetches? flattened attributes?
	}

	private void resolveAutoAliases(Expression expression) {
		Map<String, String> aliases = expression.getPathAliases();
		if (!aliases.isEmpty()) {
			if (pathSplitAliases == null) {
				pathSplitAliases = new HashMap<>();
			}

			pathSplitAliases.putAll(aliases);
		}

		int len = expression.getOperandCount();
		for (int i = 0; i < len; i++) {
			Object operand = expression.getOperand(i);
			if (operand instanceof Expression) {
				resolveAutoAliases((Expression) operand);
			}
		}
	}

	/**
	 * @since 3.0
	 */
	@Override
	public Map<String, String> getPathSplitAliases() {
		return pathSplitAliases != null ? pathSplitAliases : Collections.<String, String> emptyMap();
	}

	/**
	 * @since 3.0
	 */
	public void addPathSplitAliases(String path, String... aliases) {
		if (aliases == null) {
			throw new NullPointerException("Null aliases");
		}

		if (aliases.length == 0) {
			throw new IllegalArgumentException("No aliases specified");
		}

		if (pathSplitAliases == null) {
			pathSplitAliases = new HashMap<>();
		}

		for (String alias : aliases) {
			pathSplitAliases.put(alias, path);
		}
	}

	/**
	 * Build DB result descriptor, that will be used to read and convert raw result of ColumnSelect
	 * @since 4.0
	 */
	private void buildResultSetMappingForColumns(SelectQuery<?> query, EntityResolver resolver) {
		if(query.getColumns() == null || query.getColumns().isEmpty()) {
			return;
		}
		
		SQLResult result = new SQLResult();
		for(Property<?> column : query.getColumns()) {
			Expression exp = column.getExpression();
			String name = column.getName() == null ? exp.expName() : column.getName();
			boolean fullObject = false;
			if(exp.getType() == Expression.OBJ_PATH) {
				// check if this is toOne relation
				Expression dbPath = this.getObjEntity().translateToDbPath(exp);
				DbRelationship rel = findRelationByPath(dbEntity, dbPath);
				if(rel != null && !rel.isToMany()) {
					// it this path is toOne relation, than select full object for it
					fullObject = true;
				}
			} else if(exp.getType() == Expression.FULL_OBJECT) {
				fullObject = true;
			}

			if(fullObject) {
				// detected full object column
				if(getPageSize() > 0) {
					// for paginated queries keep only IDs
					result.addEntityResult(buildEntityIdResultForColumn(column, resolver));
				} else {
					// will unwrap to full set of db-columns (with join prefetch optionally)
					result.addEntityResult(buildEntityResultForColumn(query, column, resolver));
				}
			} else {
				// scalar column
				result.addColumnResult(name);
			}
		}
		resultSetMapping = result.getResolvedComponents(resolver);
	}

	/**
	 * Collect metadata for result with ObjectId (used for paginated queries with FullObject columns)
	 *
	 * @param column full object column
	 * @param resolver entity resolver
	 * @return Entity result
	 */
	private EntityResult buildEntityIdResultForColumn(Property<?> column, EntityResolver resolver) {
		EntityResult result = new EntityResult(column.getType());
		DbEntity entity = resolver.getObjEntity(column.getType()).getDbEntity();
		for(DbAttribute attribute : entity.getPrimaryKeys()) {
			result.addDbField(attribute.getName(), attribute.getName());
		}
		return result;
	}

	private DbRelationship findRelationByPath(DbEntity entity, Expression exp) {
		DbRelationship r = null;
		for (PathComponent<DbAttribute, DbRelationship> component : entity.resolvePath(exp, getPathSplitAliases())) {
			r = component.getRelationship();
		}
		return r;
	}

	/**
	 * Collect metadata for column that will be unwrapped to full entity in the final SQL
	 * (possibly including joint prefetch).
	 * This information will be used to correctly create Persistent object back from raw result.
	 *
	 * This method is actually repeating logic of
	 * {@link org.apache.cayenne.access.translator.select.DefaultSelectTranslator#appendQueryColumns}.
	 * Here we don't care about intermediate joins and few other things so it's shorter.
	 * Logic of these methods should be unified and simplified, possibly to a single source of metadata,
	 * generated only once and used everywhere.
	 *
	 * @param query original query
	 * @param column full object column
	 * @param resolver entity resolver to get ObjEntity and ClassDescriptor
	 * @return Entity result
	 */
	private EntityResult buildEntityResultForColumn(SelectQuery<?> query, Property<?> column, EntityResolver resolver) {
		final EntityResult result = new EntityResult(column.getType());

		// Collecting visitor for ObjAttributes and toOne relationships
		PropertyVisitor visitor = new PropertyVisitor() {
			public boolean visitAttribute(AttributeProperty property) {
				ObjAttribute oa = property.getAttribute();
				Iterator<CayenneMapEntry> dbPathIterator = oa.getDbPathIterator();
				while (dbPathIterator.hasNext()) {
					CayenneMapEntry pathPart = dbPathIterator.next();
					if (pathPart instanceof DbAttribute) {
						result.addDbField(pathPart.getName(), pathPart.getName());
					}
				}
				return true;
			}

			public boolean visitToMany(ToManyProperty property) {
				return true;
			}

			public boolean visitToOne(ToOneProperty property) {
				DbRelationship dbRel = property.getRelationship().getDbRelationships().get(0);
				List<DbJoin> joins = dbRel.getJoins();
				for (DbJoin join : joins) {
					if(!join.getSource().isPrimaryKey()) {
						result.addDbField(join.getSource().getName(), join.getSource().getName());
					}
				}
				return true;
			}
		};

		ObjEntity oe = resolver.getObjEntity(column.getType());
		DbEntity table = oe.getDbEntity();

		// Additionally collect PKs
		for (DbAttribute dba : table.getPrimaryKeys()) {
			result.addDbField(dba.getName(), dba.getName());
		}

		ClassDescriptor descriptor = resolver.getClassDescriptor(oe.getName());
		descriptor.visitAllProperties(visitor);

		// Collection columns for joint prefetch
		if(query.getPrefetchTree() != null) {
			for (PrefetchTreeNode prefetch : query.getPrefetchTree().adjacentJointNodes()) {
				// for each prefetch add columns from the target entity
				Expression prefetchExp = ExpressionFactory.exp(prefetch.getPath());
				ASTDbPath dbPrefetch = (ASTDbPath) oe.translateToDbPath(prefetchExp);
				DbRelationship r = findRelationByPath(table, dbPrefetch);
				if (r == null) {
					throw new CayenneRuntimeException("Invalid joint prefetch '%s' for entity: %s"
							, prefetch, oe.getName());
				}

				// go via target OE to make sure that Java types are mapped correctly...
				ObjRelationship targetRel = (ObjRelationship) prefetchExp.evaluate(oe);
				ObjEntity targetEntity = targetRel.getTargetEntity();
				prefetch.setEntityName(targetRel.getSourceEntity().getName());

				String labelPrefix = dbPrefetch.getPath();
				Set<String> seenNames = new HashSet<>();
				for (ObjAttribute oa : targetEntity.getAttributes()) {
					Iterator<CayenneMapEntry> dbPathIterator = oa.getDbPathIterator();
					while (dbPathIterator.hasNext()) {
						Object pathPart = dbPathIterator.next();
						if (pathPart instanceof DbAttribute) {
							DbAttribute attribute = (DbAttribute) pathPart;
							if(seenNames.add(attribute.getName())) {
								result.addDbField(labelPrefix + '.' + attribute.getName(), labelPrefix + '.' + attribute.getName());
							}
						}
					}
				}

				// append remaining target attributes such as keys
				DbEntity targetDbEntity = r.getTargetEntity();
				for (DbAttribute attribute : targetDbEntity.getAttributes()) {
					if(seenNames.add(attribute.getName())) {
						result.addDbField(labelPrefix + '.' + attribute.getName(), labelPrefix + '.' + attribute.getName());
					}
				}
			}
		}

		return result;
	}

	/**
	 * @since 4.0
	 */
	@Override
	public boolean isSingleResultSetMapping() {
		return isSingleResultSetMapping;
	}

	/**
	 * @since 4.0
	 */
	@Override
	public boolean isSuppressingDistinct() {
		return suppressingDistinct;
	}

	/**
	 * @since 4.0
	 */
	public void setSuppressingDistinct(boolean suppressingDistinct) {
		this.suppressingDistinct = suppressingDistinct;
	}

	/**
	 * Expression traverse handler to create cache key string out of Expression.
	 * {@link Expression#appendAsString(Appendable)} where previously used for that,
	 * but it can't handle custom value objects properly (see CAY-2210).
	 *
	 * @see ValueObjectTypeRegistry
	 *
	 * @since 4.0
	 */
	static class ToCacheKeyTraversalHandler implements TraversalHandler {

		private ValueObjectTypeRegistry registry;
		private StringBuilder out;

		ToCacheKeyTraversalHandler(ValueObjectTypeRegistry registry, StringBuilder out) {
			this.registry = registry;
			this.out = out;
		}

		@Override
		public void finishedChild(Expression node, int childIndex, boolean hasMoreChildren) {
			out.append(',');
		}

		@Override
		public void startNode(Expression node, Expression parentNode) {
			if(node.getType() == Expression.FUNCTION_CALL) {
				out.append(((ASTFunctionCall)node).getFunctionName()).append('(');
			} else {
				out.append(node.getType()).append('(');
			}
		}

		@Override
		public void endNode(Expression node, Expression parentNode) {
			out.append(')');
		}

		@Override
		public void objectNode(Object leaf, Expression parentNode) {
			if(leaf == null) {
				out.append("null");
				return;
			}

			if(leaf instanceof ASTScalar) {
				leaf = ((ASTScalar) leaf).getValue();
			} else if(leaf instanceof Object[]) {
				for(Object value : (Object[])leaf) {
					objectNode(value, parentNode);
					out.append(',');
				}
				return;
			}

			if (leaf instanceof Persistent) {
				ObjectId id = ((Persistent) leaf).getObjectId();
				Object encode = (id != null) ? id : leaf;
				out.append(encode);
			} else if (leaf instanceof Enum<?>) {
				Enum<?> e = (Enum<?>) leaf;
				out.append("e:").append(leaf.getClass().getName()).append(':').append(e.ordinal());
			} else {
				ValueObjectType<Object, ?> valueObjectType;
				if (registry == null || (valueObjectType = registry.getValueType(leaf.getClass())) == null) {
					// Registry will be null in cayenne-client context.
					// Maybe we shouldn't create cache key at all in that case...
					out.append(leaf);
				} else {
					out.append(valueObjectType.toCacheKey(leaf));
				}
			}
		}
	};
}
