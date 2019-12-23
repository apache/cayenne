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
package org.apache.cayenne.query;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.TraversalHandler;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.map.DefaultEntityResultSegment;
import org.apache.cayenne.map.DefaultScalarResultSegment;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 3.0
 * @deprecated since 4.2
 */
@Deprecated
class SelectQueryMetadata extends BaseQueryMetadata {

	private static final long serialVersionUID = 7465922769303943945L;

	private static final ScalarResultSegment SCALAR_RESULT_SEGMENT
			= new DefaultScalarResultSegment(null, -1);
	private static final EntityResultSegment ENTITY_RESULT_SEGMENT
			= new DefaultEntityResultSegment(null, null, -1);
	
	private Map<String, String> pathSplitAliases;
	private boolean isSingleResultSetMapping;
	private boolean suppressingDistinct;

	@Override
	void copyFromInfo(QueryMetadata info) {
		super.copyFromInfo(info);
		this.pathSplitAliases = new HashMap<>(info.getPathSplitAliases());
	}

	boolean resolve(Object root, EntityResolver resolver, SelectQuery<?> query) {

		if (super.resolve(root, resolver)) {
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
			traversalHandler = new ToCacheKeyTraversalHandler(resolver.getValueObjectTypeRegistry(), key);
			for(Property<?> property : query.getColumns()) {
				key.append("/c:");
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

		if (query.getHavingQualifier() != null) {
			key.append('/');
			if(traversalHandler == null) {
				traversalHandler = new ToCacheKeyTraversalHandler(resolver.getValueObjectTypeRegistry(), key);
			}
			query.getHavingQualifier().traverse(traversalHandler);
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

		// add prefetch to cache key per CAY-2349
		if(query.getPrefetchTree() != null) {
			query.getPrefetchTree().traverse(new ToCacheKeyPrefetchProcessor(key));
		}

		return key.toString();
	}

	private void resolveAutoAliases(SelectQuery<?> query) {
		resolveQualifierAliases(query);
		resolveColumnsAliases(query);
        resolveOrderingAliases(query);
		resolveHavingQualifierAliases(query);
		// TODO: include aliases in prefetches? flattened attributes?
	}

	private void resolveQualifierAliases(SelectQuery<?> query) {
		Expression qualifier = query.getQualifier();
		if (qualifier != null) {
			resolveAutoAliases(qualifier);
		}
	}

	private void resolveColumnsAliases(SelectQuery<?> query) {
        Collection<Property<?>> columns = query.getColumns();
        if(columns != null) {
            for(Property<?> property : columns) {
                Expression propertyExpression = property.getExpression();
                if(propertyExpression != null) {
                    resolveAutoAliases(propertyExpression);
                }
            }
        }
    }

    private void resolveOrderingAliases(SelectQuery<?> query) {
        List<Ordering> orderings = query.getOrderings();
        if(orderings != null) {
            for(Ordering ordering : orderings) {
                Expression sortSpec = ordering.getSortSpec();
                if(sortSpec != null) {
                    resolveAutoAliases(sortSpec);
                }
            }
        }
    }

    private void resolveHavingQualifierAliases(SelectQuery<?> query) {
        Expression havingQualifier = query.getHavingQualifier();
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

	/**
	 * @since 3.0
	 */
	@Override
	public Map<String, String> getPathSplitAliases() {
		return pathSplitAliases != null ? pathSplitAliases : Collections.emptyMap();
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

		resultSetMapping = new ArrayList<>(query.getColumns().size());
		for(Property<?> column : query.getColumns()) {
			// for each column we need only to know if it's entity or scalar
			Expression exp = column.getExpression();
			boolean fullObject = false;
			if(exp.getType() == Expression.OBJ_PATH) {
				// check if this is toOne relation
				Object rel = exp.evaluate(getObjEntity());
				// it this path is toOne relation, than select full object for it
				fullObject = rel instanceof ObjRelationship && !((ObjRelationship) rel).isToMany();
			} else if(exp.getType() == Expression.FULL_OBJECT) {
				fullObject = true;
			}

			if(fullObject) {
				resultSetMapping.add(ENTITY_RESULT_SEGMENT);
			} else {
				resultSetMapping.add(SCALAR_RESULT_SEGMENT);
			}
		}
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
}
