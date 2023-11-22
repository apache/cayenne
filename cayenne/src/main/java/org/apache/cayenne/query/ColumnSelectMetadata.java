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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.map.DefaultEntityResultSegment;
import org.apache.cayenne.map.DefaultScalarResultSegment;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjRelationship;

/**
 * @since 4.2
 */
class ColumnSelectMetadata extends ObjectSelectMetadata {

	private static final long serialVersionUID = -3622675304651257963L;

	private static final ScalarResultSegment SCALAR_RESULT_SEGMENT
			= new DefaultScalarResultSegment(null, -1);
	private static final EntityResultSegment ENTITY_RESULT_SEGMENT
			= new DefaultEntityResultSegment(null, null, -1);

	private boolean isSingleResultSetMapping;
	private boolean suppressingDistinct;
	private Function<?, ?> resultMapper;

	boolean resolve(Object root, EntityResolver resolver, ColumnSelect<?> query) {

		if (super.resolve(root, resolver)) {
			// generate unique cache key, but only if we are caching..
			if (cacheStrategy != null && cacheStrategy != QueryCacheStrategy.NO_CACHE) {
				this.cacheKey = makeCacheKey(query, resolver);
			}

			resolveAutoAliases(query);
			buildResultSetMappingForColumns(query);
			isSingleResultSetMapping = query.isSingleColumn();
			return true;
		}

		return false;
	}

	@Override
	protected void resolveAutoAliases(FluentSelect<?, ?> query) {
		super.resolveAutoAliases(query);
		resolveColumnsAliases(query);
	}

	protected void resolveColumnsAliases(FluentSelect<?, ?> query) {
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

	@Override
	public Map<String, String> getPathSplitAliases() {
		return pathSplitAliases != null ? pathSplitAliases : Collections.emptyMap();
	}

	/**
	 * NOTE: this is a dirty logic, we calculate hollow resultSetMapping here and later in translator
	 * (see ColumnExtractorStage and extractors) discard this and calculate it with full info.
	 *
	 * This result set mapping required by paginated queries that need only result type (entity/scalar) not
	 * full info. So we can optimize this a bit and pair calculation with translation that do same thing to provide
	 * result column descriptors.
	 */
	private void buildResultSetMappingForColumns(ColumnSelect<?> query) {
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

	@SuppressWarnings("unchecked")
	void setResultMapper(Function<?, ?> resultMapper) {
		if(this.resultMapper != null) {
			this.resultMapper = this.resultMapper.andThen((Function)resultMapper);
		} else {
			this.resultMapper = resultMapper;
		}
	}

	@Override
	public Function<?, ?> getResultMapper() {
		return resultMapper;
	}
}
