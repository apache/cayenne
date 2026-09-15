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
import org.apache.cayenne.EmbeddableObject;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * @since 4.2
 */
class ColumnSelectMetadata extends ObjectSelectMetadata {

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
			this.resultSetMapping = buildResultSetMapping(query, resolver);
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

	/**
	 * Builds the shape of a result row declared by the query columns: one segment per column, classified the same
	 * way the select translator classifies columns into entity, embeddable and scalar results. Column offsets and
	 * field labels depend on the generated SQL and are not known here, so the segments carry none; the translator
	 * produces the column-resolved counterpart of this list for the row reader.
	 */
	private List<ResultSegment> buildResultSetMapping(ColumnSelect<?> query, EntityResolver resolver) {
		Collection<Property<?>> columns = query.getColumns();
		if (columns == null || columns.isEmpty()) {
			return null;
		}

		List<ResultSegment> segments = new ArrayList<>(columns.size());
		for (Property<?> column : columns) {
			segments.add(resultSegment(column, resolver));
		}
		return Collections.unmodifiableList(segments);
	}

	private ResultSegment resultSegment(Property<?> column, EntityResolver resolver) {
		Expression exp = column.getExpression();
		Class<?> type = column.getType();
		int expType = exp.getType();

		boolean fullObject = expType == Expression.FULL_OBJECT
				|| (type != null && expType == Expression.OBJ_PATH && Persistent.class.isAssignableFrom(type));

		if (fullObject) {
			ObjEntity entity = resolver.getObjEntity(type);
			if (entity == null) {
				throw new CayenneRuntimeException("No entity mapped for column '%s' of type %s", exp, type);
			}
			return new EntityResultSegment(resolver.getClassDescriptor(entity.getName()), Collections.emptyMap(), -1);
		}

		if (type != null && EmbeddableObject.class.isAssignableFrom(type)) {
			Object o = exp.evaluate(getObjEntity());
			if (!(o instanceof EmbeddedAttribute attribute)) {
				throw new CayenneRuntimeException("EmbeddedAttribute expected, %s found", o);
			}
			return new EmbeddableResultSegment(attribute.getEmbeddable(), Collections.emptyMap(), -1);
		}

		String name = column.getName() == null ? exp.expName() : column.getName();
		return new ScalarResultSegment(name, -1);
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
