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

package org.apache.cayenne.gen;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.exp.EnclosingObjectExp;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionParameter;
import org.apache.cayenne.exp.ListExp;
import org.apache.cayenne.exp.ObjPathExp;
import org.apache.cayenne.exp.SubqueryExp;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.projecttools.validation.SelectQueryPaths;
import org.apache.cayenne.query.ColumnSelect;
import org.apache.cayenne.query.FluentSelect;
import org.apache.cayenne.util.Util;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Attributes and Methods for working with Queries.
 *
 * @since 3.0
 */
public class DataMapUtils {

	private static final String OBJECT_TYPE = Object.class.getName();

	/**
	 * Return valid method name based on query name (replace all illegal
	 * characters with underscore '_').
	 * 
	 * @param query descriptor
	 * @return Method name that perform query.
	 */
	public String getQueryMethodName(QueryDescriptor query) {
		return Util.underscoredToJava(query.getName(), true);
	}

	/**
	 * Get all parameter names used in the query, in the order of their first occurrence.
	 *
	 * @param query select query descriptor
	 * @return Parameter names.
	 */
	public Collection<String> getParameterNames(SelectQueryDescriptor query) {
		return parameterTypes(query).keySet();
	}

	/**
	 * Returns whether a method can be generated for the query: the query String can be parsed, its root is mapped,
	 * and the entity names and paths of all its clauses can be resolved.
	 */
	public Boolean isValidParameterNames(SelectQueryDescriptor query) {
		if (!query.isValid() || !(query.getRoot() instanceof ObjEntity || query.getRoot() instanceof DbEntity)) {
			return false;
		}

		return SelectQueryPaths.check(query).isEmpty();
	}

	public boolean hasParameters(SelectQueryDescriptor query) {
		return !getParameterNames(query).isEmpty();
	}

	/**
	 * Get type of parameter for given name.
	 *
	 * @param query descriptor
	 * @param name parameter name
	 * @return Parameter type.
	 */
	public String getParameterType(SelectQueryDescriptor query, String name) {
		return parameterTypes(query).get(name);
	}

	/**
	 * Returns the fully qualified name of the type of the objects that the query fetches: the class of the root
	 * ObjEntity, a DataRow, an Object for a single column, or an Object[] for multiple columns.
	 *
	 * @since 5.0
	 */
	public String getResultType(SelectQueryDescriptor query) {
		FluentSelect<?, ?> parsed = query.getParsedQuery();
		if (parsed instanceof ColumnSelect<?> columnSelect) {
			return columnSelect.getColumns().size() == 1 ? OBJECT_TYPE : Object[].class.getCanonicalName();
		}

		if (query.getRoot() instanceof ObjEntity entity && !query.isFetchingDataRows()) {
			return entity.getClassName();
		}

		return DataRow.class.getName();
	}

	// parameter types by the names of the parameters, in the order of their first occurrence
	private Map<String, String> parameterTypes(SelectQueryDescriptor query) {
		if (!query.isValid()) {
			return Map.of();
		}

		Map<String, String> types = new HashMap<>();
		collectParameterTypes(query.getDataMap(), query.getParsedQuery(), types);

		Map<String, String> ordered = new LinkedHashMap<>();
		for (String name : query.getParameterNames()) {
			ordered.put(Util.underscoredToJava(name, false), types.getOrDefault(name, OBJECT_TYPE));
		}
		return ordered;
	}

	private void collectParameterTypes(DataMap dataMap, FluentSelect<?, ?> query, Map<String, String> types) {
		// the type of a parameter can only be inferred from an ObjEntity attribute or relationship
		ObjEntity entity = query.getEntityName() != null && dataMap != null
				? dataMap.getObjEntity(query.getEntityName())
				: null;

		// the aliases of the columns can be used in place of the paths elsewhere in the query
		Map<String, String> aliasTypes = new HashMap<>();
		if (query.getColumns() != null) {
			for (Property<?> column : query.getColumns()) {
				if (column.getAlias() != null) {
					aliasTypes.put(column.getAlias(), column.getExpression() instanceof ObjPathExp pathExp
							? pathType(entity, pathExp)
							: OBJECT_TYPE);
				}
			}
		}

		Scope scope = new Scope(dataMap, entity, aliasTypes);
		collectParameterTypes(scope, query.getWhere(), types);
		collectParameterTypes(scope, query.getHaving(), types);
	}

	private void collectParameterTypes(Scope scope, Expression expression, Map<String, String> types) {
		if (expression == null || expression instanceof EnclosingObjectExp) {
			return;
		}

		if (expression instanceof SubqueryExp subquery) {
			if (subquery.getQuery() != null) {
				collectParameterTypes(scope.dataMap(), subquery.getQuery(), types);
			}
			return;
		}

		// a parameter takes the type of a path compared to it
		String typeName = OBJECT_TYPE;
		for (int i = 0; i < expression.getOperandCount(); i++) {
			if (expression.getOperand(i) instanceof ObjPathExp pathExp) {
				typeName = scope.aliasTypes().getOrDefault(pathExp.getPath().value(),
						pathType(scope.entity(), pathExp));
			}
		}

		for (int i = 0; i < expression.getOperandCount(); i++) {
			Object operand = expression.getOperand(i);
			switch (operand) {
				case ExpressionParameter parameter -> {
					types.merge(parameter.getName(), typeName, DataMapUtils::moreSpecific);
				}
				case ListExp list -> {
					for (Object value : (Object[]) list.getOperand(0)) {
						if (value instanceof ExpressionParameter parameter) {
							types.merge(parameter.getName(), typeName, DataMapUtils::moreSpecific);
						}
					}
				}
				case Expression child -> collectParameterTypes(scope, child, types);
				case null, default -> {
				}
			}
		}
	}

	// the type of the attribute or relationship at the end of a path, or Object if the path can't be resolved. An
	// invalid path is a problem of the query, reported by the validator, and not of the type inference
	private static String pathType(ObjEntity entity, ObjPathExp pathExp) {
		if (entity == null) {
			return OBJECT_TYPE;
		}

		List<Object> components;
		try {
			components = entity.resolvePath(pathExp.getExpandedPath());
		} catch (ExpressionException e) {
			return OBJECT_TYPE;
		}

		return switch (components.getLast()) {
			case ObjAttribute attribute -> attribute.getType();
			case ObjRelationship relationship -> relationship.getTargetEntity().getClassName();
			default -> OBJECT_TYPE;
		};
	}

	// the root entity of a query (null if unknown) and the types of its column aliases
	private record Scope(DataMap dataMap, ObjEntity entity, Map<String, String> aliasTypes) {
	}

	private static String moreSpecific(String type1, String type2) {
		return OBJECT_TYPE.equals(type1) ? type2 : type1;
	}
}
