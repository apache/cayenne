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
import org.apache.cayenne.exp.parser.ASTAggregateFunctionCall;
import org.apache.cayenne.exp.parser.ASTConcat;
import org.apache.cayenne.exp.parser.ASTCount;
import org.apache.cayenne.exp.parser.ASTDistinct;
import org.apache.cayenne.exp.parser.ASTExtract;
import org.apache.cayenne.exp.parser.ASTLength;
import org.apache.cayenne.exp.parser.ASTLocate;
import org.apache.cayenne.exp.parser.ASTLower;
import org.apache.cayenne.exp.parser.ASTSubstring;
import org.apache.cayenne.exp.parser.ASTTrim;
import org.apache.cayenne.exp.parser.ASTUpper;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
	 * Builds the shape of a result row declared by the query columns: one segment per column, classified into
	 * entity, embeddable and scalar results.
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

		if (expType == Expression.FULL_OBJECT) {
			ObjEntity entity = type != null ? resolver.getObjEntity(type) : fullObjectEntity(exp);
			if (entity == null) {
				throw new CayenneRuntimeException("No entity mapped for column '%s' of type %s", exp, type);
			}
			return entitySegment(entity, resolver);
		}

		if (type != null) {
			return typedSegment(column, resolver);
		}

		if (expType == Expression.OBJ_PATH && getObjEntity() != null) {
			return switch (exp.evaluate(getObjEntity())) {
				case EmbeddedAttribute ignored -> embeddableSegment(exp);
				case ObjRelationship relationship -> {
					if (relationship.isToMany()) {
						throw toManyColumnException();
					}
					yield entitySegment(relationship.getTargetEntity(), resolver);
				}
				case ObjAttribute attribute ->
						new ScalarResultSegment(columnName(column), attribute.getJavaClass(), -1);
				default -> new ScalarResultSegment(columnName(column), null, -1);
			};
		}

		return new ScalarResultSegment(columnName(column), inferType(exp), -1);
	}

	private ResultSegment typedSegment(Property<?> column, EntityResolver resolver) {
		Expression exp = column.getExpression();
		Class<?> type = column.getType();
		int expType = exp.getType();

		if ((expType == Expression.OBJ_PATH || expType == Expression.DB_PATH)
				&& (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type))) {
			throw toManyColumnException();
		}

		if (expType == Expression.OBJ_PATH && Persistent.class.isAssignableFrom(type)) {
			ObjEntity entity = resolver.getObjEntity(type);
			if (entity == null) {
				throw new CayenneRuntimeException("No entity mapped for column '%s' of type %s", exp, type);
			}
			return entitySegment(entity, resolver);
		}

		if (EmbeddableObject.class.isAssignableFrom(type)) {
			return embeddableSegment(exp);
		}

		return new ScalarResultSegment(columnName(column), type, -1);
	}

	private ResultSegment entitySegment(ObjEntity entity, EntityResolver resolver) {
		return new EntityResultSegment(resolver.getClassDescriptor(entity.getName()), Collections.emptyMap(), -1);
	}

	private ResultSegment embeddableSegment(Expression exp) {
		Object o = exp.evaluate(getObjEntity());
		if (!(o instanceof EmbeddedAttribute attribute)) {
			throw new CayenneRuntimeException("EmbeddedAttribute expected, %s found", o);
		}
		return new EmbeddableResultSegment(attribute.getEmbeddable(), Collections.emptyMap(), -1);
	}

	/**
	 * Resolves the entity of a "full object" expression with no declared type: the root entity when the expression
	 * has no operand, otherwise the target of the relationship path it wraps (a "flat" to-many column).
	 */
	private ObjEntity fullObjectEntity(Expression exp) {
		if (exp.getOperandCount() == 0) {
			return getObjEntity();
		}
		if (getObjEntity() != null
				&& exp.getOperand(0) instanceof Expression path
				&& path.getType() == Expression.OBJ_PATH
				&& path.evaluate(getObjEntity()) instanceof ObjRelationship relationship) {
			return relationship.getTargetEntity();
		}
		return null;
	}

	/**
	 * Infers the Java type of an untyped non-path column from its expression, following the types the fluent
	 * Property API assigns to the same functions. Returns null when there is nothing to infer, in which case the
	 * value is read with the type the driver reports.
	 */
	private Class<?> inferType(Expression exp) {
		return switch (exp) {
			case ASTCount ignored -> Long.class;
			case ASTAggregateFunctionCall aggregate -> operandType(aggregate);
			case ASTUpper ignored -> String.class;
			case ASTLower ignored -> String.class;
			case ASTConcat ignored -> String.class;
			case ASTSubstring ignored -> String.class;
			case ASTTrim ignored -> String.class;
			case ASTLength ignored -> Integer.class;
			case ASTLocate ignored -> Integer.class;
			case ASTExtract ignored -> Integer.class;
			default -> null;
		};
	}

	private Class<?> operandType(Expression function) {
		if (function.getOperandCount() == 0 || !(function.getOperand(0) instanceof Expression operand)) {
			return null;
		}
		if (operand instanceof ASTDistinct) {
			return operandType(operand);
		}
		if (operand.getType() == Expression.OBJ_PATH
				&& getObjEntity() != null
				&& operand.evaluate(getObjEntity()) instanceof ObjAttribute attribute) {
			return attribute.getJavaClass();
		}
		return null;
	}

	private static String columnName(Property<?> column) {
		return column.getName() == null ? column.getExpression().expName() : column.getName();
	}

	private static CayenneRuntimeException toManyColumnException() {
		return new CayenneRuntimeException("Can't directly select toMany relationship columns. "
				+ "Either select it with aggregate functions like count() "
				+ "or with flat() function to select full related objects.");
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
