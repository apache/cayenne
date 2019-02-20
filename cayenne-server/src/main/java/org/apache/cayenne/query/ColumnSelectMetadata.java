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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.TraversalHandler;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.exp.property.BaseProperty;
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
 * @since 4.2
 */
class ColumnSelectMetadata extends BaseQueryMetadata {

	private static final long serialVersionUID = -3622675304651257963L;

	private Map<String, String> pathSplitAliases;
	private boolean isSingleResultSetMapping;
	private boolean suppressingDistinct;

	@Override
	void copyFromInfo(QueryMetadata info) {
		super.copyFromInfo(info);
		this.pathSplitAliases = new HashMap<>(info.getPathSplitAliases());
	}

	boolean resolve(Object root, EntityResolver resolver, ColumnSelect<?> query) {

		if (super.resolve(root, resolver)) {
			// generate unique cache key, but only if we are caching..
			if (cacheStrategy != null && cacheStrategy != QueryCacheStrategy.NO_CACHE) {
				this.cacheKey = makeCacheKey(query, resolver);
			}

			resolveAutoAliases(query);
			buildResultSetMappingForColumns(query, resolver);
			isSingleResultSetMapping = query.isSingleColumn() && super.isSingleResultSetMapping();

			return true;
		}

		return false;
	}

	private String makeCacheKey(ColumnSelect<?> query, EntityResolver resolver) {

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
			traversalHandler = new ToCacheKeyTraversalHandler(resolver.getValueObjectTypeRegistry(), key);
			for(BaseProperty<?> property : query.getColumns()) {
				key.append("/c:");
				property.getExpression().traverse(traversalHandler);
			}
		}

		if (query.getWhere() != null) {
			key.append('/');
			if(traversalHandler == null) {
				traversalHandler = new ToCacheKeyTraversalHandler(resolver.getValueObjectTypeRegistry(), key);
			}
			query.getWhere().traverse(traversalHandler);
		}

		if (query.getOrderings() != null && !query.getOrderings().isEmpty()) {
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

		if (fetchOffset > 0 || fetchLimit > 0) {
			key.append('/');
			if (fetchOffset > 0) {
				key.append('o').append(fetchOffset);
			}
			if (fetchLimit > 0) {
				key.append('l').append(fetchLimit);
			}
		}

		// add prefetch to cache key per CAY-2349
		if(prefetchTree != null) {
			prefetchTree.traverse(new ToCacheKeyPrefetchProcessor(key));
		}

		return key.toString();
	}

	private void resolveAutoAliases(ColumnSelect<?> query) {
		resolveQualifierAliases(query);
		resolveColumnsAliases(query);
        resolveOrderingAliases(query);
		resolveHavingQualifierAliases(query);
		// TODO: include aliases in prefetches? flattened attributes?
	}

	private void resolveQualifierAliases(ColumnSelect<?> query) {
		Expression qualifier = query.getWhere();
		if (qualifier != null) {
			resolveAutoAliases(qualifier);
		}
	}

	private void resolveColumnsAliases(ColumnSelect<?> query) {
        Collection<BaseProperty<?>> columns = query.getColumns();
        if(columns != null) {
            for(BaseProperty<?> property : columns) {
                Expression propertyExpression = property.getExpression();
                if(propertyExpression != null) {
                    resolveAutoAliases(propertyExpression);
                }
            }
        }
    }

    private void resolveOrderingAliases(ColumnSelect<?> query) {
        Collection<Ordering> orderings = query.getOrderings();
        if(orderings != null) {
            for(Ordering ordering : orderings) {
                Expression sortSpec = ordering.getSortSpec();
                if(sortSpec != null) {
                    resolveAutoAliases(sortSpec);
                }
            }
        }
    }

    private void resolveHavingQualifierAliases(ColumnSelect<?> query) {
        Expression havingQualifier = query.getHaving();
        if(havingQualifier != null) {
            resolveAutoAliases(havingQualifier);
        }
    }

	private void resolveAutoAliases(Expression expression) {
		Map<String, String> aliases = expression.getPathAliases();
		if (!aliases.isEmpty()) {
			if (pathSplitAliases == null) {
				pathSplitAliases = new HashMap<>();
			}

			for(Map.Entry<String, String> entry : aliases.entrySet()) {
				pathSplitAliases.compute(entry.getKey(), (key, value) -> {
					if(value != null && !value.equals(entry.getValue())){
						throw new CayenneRuntimeException("Can't add the same alias to different path segments.");
					} else {
						return entry.getValue();
					}
				});
			}
		}

		int len = expression.getOperandCount();
		for (int i = 0; i < len; i++) {
			Object operand = expression.getOperand(i);
			if (operand instanceof Expression) {
				resolveAutoAliases((Expression) operand);
			}
		}
	}

	@Override
	public Map<String, String> getPathSplitAliases() {
		return pathSplitAliases != null ? pathSplitAliases : Collections.emptyMap();
	}

	/**
	 * Build DB result descriptor, that will be used to read and convert raw result of ColumnSelect
	 */
	private void buildResultSetMappingForColumns(ColumnSelect<?> query, EntityResolver resolver) {
		if(query.getColumns() == null || query.getColumns().isEmpty()) {
			return;
		}
		
		SQLResult result = new SQLResult();
		for(BaseProperty<?> column : query.getColumns()) {
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
	private EntityResult buildEntityIdResultForColumn(BaseProperty<?> column, EntityResolver resolver) {
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
	 * @param query original query
	 * @param column full object column
	 * @param resolver entity resolver to get ObjEntity and ClassDescriptor
	 * @return Entity result
	 */
	private EntityResult buildEntityResultForColumn(ColumnSelect<?> query, BaseProperty<?> column, EntityResolver resolver) {
		// This method is actually repeating logic of DescriptorColumnExtractor.
		// Here we don't care about intermediate joins and few other things so it's shorter.
	 	// Logic of these methods should be unified and simplified, possibly to a single source of metadata,
	 	// generated only once and used everywhere.

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
		if(prefetchTree != null) {
			for (PrefetchTreeNode prefetch : prefetchTree.adjacentJointNodes()) {
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

	@Override
	public boolean isSingleResultSetMapping() {
		return isSingleResultSetMapping;
	}

	@Override
	public boolean isSuppressingDistinct() {
		return suppressingDistinct;
	}

	public void setSuppressingDistinct(boolean suppressingDistinct) {
		this.suppressingDistinct = suppressingDistinct;
	}
}
