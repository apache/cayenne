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
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 4.2
 */
class ObjectSelectMetadata extends BaseQueryMetadata {

	private static final long serialVersionUID = -4936484509363047672L;

	protected Map<String, String> pathSplitAliases;

	@Override
	void copyFromInfo(QueryMetadata info) {
		super.copyFromInfo(info);
		this.pathSplitAliases = new HashMap<>(info.getPathSplitAliases());
	}

	boolean resolve(Object root, EntityResolver resolver, ObjectSelect<?> query) {

		if (super.resolve(root, resolver)) {
			// generate unique cache key, but only if we are caching..
			if (cacheStrategy != null && cacheStrategy != QueryCacheStrategy.NO_CACHE) {
				this.cacheKey = makeCacheKey(query, resolver);
			}

			resolveAutoAliases(query);
			return true;
		}

		return false;
	}

	protected String makeCacheKey(FluentSelect<?, ?> query, EntityResolver resolver) {

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

		if (query.getColumns() != null && !query.getColumns().isEmpty()) {
			traversalHandler = new ToCacheKeyTraversalHandler(resolver.getValueObjectTypeRegistry(), key);
			for (Property<?> property : query.getColumns()) {
				key.append("/c:");
				property.getExpression().traverse(traversalHandler);
			}
		}

		if (query.getWhere() != null) {
			key.append('/');
            traversalHandler = new ToCacheKeyTraversalHandler(resolver.getValueObjectTypeRegistry(), key);
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

		if (query.getHaving() != null) {
			key.append('/');
			if(traversalHandler == null) {
				traversalHandler = new ToCacheKeyTraversalHandler(resolver.getValueObjectTypeRegistry(), key);
			}
			query.getHaving().traverse(traversalHandler);
		}

		if (fetchLimit > 0 || fetchOffset > 0) {
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

	protected void resolveAutoAliases(FluentSelect<?, ?> query) {
		resolveQualifierAliases(query);
        resolveOrderingAliases(query);
		resolveHavingQualifierAliases(query);
	}

	protected void resolveQualifierAliases(FluentSelect<?, ?> query) {
		Expression qualifier = query.getWhere();
		if (qualifier != null) {
			resolveAutoAliases(qualifier);
		}
	}

	protected void resolveHavingQualifierAliases(FluentSelect<?, ?> query) {
		Expression havingQualifier = query.getHaving();
		if(havingQualifier != null) {
			resolveAutoAliases(havingQualifier);
		}
	}

	protected void resolveOrderingAliases(FluentSelect<?, ?> query) {
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

	protected void resolveAutoAliases(Expression expression) {
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

}
