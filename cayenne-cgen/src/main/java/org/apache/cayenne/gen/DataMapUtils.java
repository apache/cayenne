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
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionParameter;
import org.apache.cayenne.exp.InExp;
import org.apache.cayenne.exp.ListExp;
import org.apache.cayenne.exp.NotInExp;
import org.apache.cayenne.exp.ObjPathExp;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.projecttools.validation.SelectQueryPaths;
import org.apache.cayenne.projecttools.validation.SelectQueryPaths.Clause;
import org.apache.cayenne.projecttools.validation.SelectQueryPaths.PathRef;
import org.apache.cayenne.query.FluentSelect;
import org.apache.cayenne.util.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.SourceVersion;

/**
 * Attributes and Methods for working with Queries.
 *
 * @since 3.0
 */
public class DataMapUtils {

	private static final String OBJECT_TYPE = Object.class.getName();

	// the names of the variables of the generated query methods
	private static final Set<String> RESERVED_NAMES = Set.of("context", "query");

	// the parameters are requested many times per query by the templates, the identity of the parsed query detects a
	// change of the query String
	private final Map<SelectQueryDescriptor, CachedParameters> parameters = new IdentityHashMap<>();

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
	 * Get all parameter names used in the query, in the order of their first occurrence. The names are the Java names
	 * of the parameters, see {@link QueryParameter#javaName()}.
	 *
	 * @param query select query descriptor
	 * @return Parameter names.
	 * @deprecated since 5.0, use {@link #getParameters(SelectQueryDescriptor)}, that provides both the names to bind
	 * the parameters by and their Java names
	 */
	@Deprecated(since = "5.0")
	public Collection<String> getParameterNames(SelectQueryDescriptor query) {
		return getParameters(query).stream().map(QueryParameter::javaName).toList();
	}

	/**
	 * Returns the "$name" parameters of the query, in the order of their first occurrence, each with the name to bind
	 * it by, a unique Java identifier to use for it in the generated code, and its type.
	 *
	 * @since 5.0
	 */
	public List<QueryParameter> getParameters(SelectQueryDescriptor query) {
		FluentSelect<?, ?> parsed = query.isValid() ? query.getParsedQuery() : null;
		CachedParameters cached = parameters.get(query);
		if (cached == null || cached.parsed() != parsed) {
			cached = new CachedParameters(parsed, parsed != null ? computeParameters(query) : List.of());
			parameters.put(query, cached);
		}
		return cached.parameters();
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
		return !getParameters(query).isEmpty();
	}

	/**
	 * Get type of parameter for given name.
	 *
	 * @param query descriptor
	 * @param name the Java name of the parameter, see {@link QueryParameter#javaName()}
	 * @return Parameter type.
	 * @deprecated since 5.0, use {@link #getParameters(SelectQueryDescriptor)}
	 */
	@Deprecated(since = "5.0")
	public String getParameterType(SelectQueryDescriptor query, String name) {
		for (QueryParameter parameter : getParameters(query)) {
			if (parameter.javaName().equals(name)) {
				return parameter.type();
			}
		}
		return null;
	}

	/**
	 * Returns the fully qualified name of the type of the objects that the query fetches: the class of the root
	 * ObjEntity, a DataRow, an Object for a single column, or an Object[] for multiple columns.
	 *
	 * @since 5.0
	 */
	public String getResultType(SelectQueryDescriptor query) {
		return switch (query.getResultKind()) {
			case ENTITY -> ((ObjEntity) query.getRoot()).getClassName();
			case SCALAR -> OBJECT_TYPE;
			case ARRAY -> Object[].class.getCanonicalName();
			case DATA_ROW -> DataRow.class.getName();
			// a query with an unmapped root can't be run, but gets a type anyway
			case null -> DataRow.class.getName();
		};
	}

	private List<QueryParameter> computeParameters(SelectQueryDescriptor query) {
		Map<String, String> types = parameterTypes(query.getDataMap(), query.getParsedQuery());

		// the Java names must be unique and must not clash with the keywords and the variables of the generated method
		Set<String> javaNames = new HashSet<>(RESERVED_NAMES);
		List<QueryParameter> result = new ArrayList<>();
		for (String name : query.getParameterNames()) {
			String javaName = uniqueJavaName(name, javaNames);
			result.add(new QueryParameter(name, javaName, types.getOrDefault(name, OBJECT_TYPE)));
		}
		return List.copyOf(result);
	}

	private static String uniqueJavaName(String name, Set<String> taken) {
		String base = Util.underscoredToJava(name, false);
		if (base.isEmpty() || !Character.isJavaIdentifierStart(base.charAt(0))) {
			base = "p" + base;
		}

		String javaName = base;
		for (int i = 2; SourceVersion.isKeyword(javaName) || !taken.add(javaName); i++) {
			javaName = base + i;
		}
		return javaName;
	}

	private static Map<String, String> parameterTypes(DataMap dataMap, FluentSelect<?, ?> query) {
		Map<String, String> types = new HashMap<>();
		if (dataMap == null) {
			return types;
		}

		// a parameter takes the type of a path compared to it, i.e. of the last path among the operands of the
		// expression that has the parameter as its operand
		Map<Expression, String> pathTypes = new IdentityHashMap<>();
		SelectQueryPaths.walk(dataMap, query, path -> {
			if ((path.clause() == Clause.WHERE || path.clause() == Clause.HAVING)
					&& path.path() instanceof ObjPathExp && path.parent() != null) {
				pathTypes.put(path.parent(), pathType(path));
			}
		});

		pathTypes.forEach((expression, typeName) -> {
			// the right-hand side parameter of "in" is a collection of the values of the path
			boolean in = expression instanceof InExp || expression instanceof NotInExp;

			for (int i = 0; i < expression.getOperandCount(); i++) {
				switch (expression.getOperand(i)) {
					case ExpressionParameter parameter -> {
						String parameterType = in && i == 1 ? collectionType(typeName) : typeName;
						types.merge(parameter.getName(), parameterType, DataMapUtils::moreSpecific);
					}
					case ListExp list -> {
						for (Object value : (Object[]) list.getOperand(0)) {
							if (value instanceof ExpressionParameter parameter) {
								types.merge(parameter.getName(), typeName, DataMapUtils::moreSpecific);
							}
						}
					}
					case null, default -> {
					}
				}
			}
		});
		return types;
	}

	// the type of the attribute or relationship at the end of a path, or Object if the path can't be resolved. An
	// invalid path is a problem of the query, reported by the validator, and not of the type inference
	private static String pathType(PathRef path) {
		if (path.components() == null) {
			return OBJECT_TYPE;
		}

		return switch (path.components().getLast()) {
			case ObjAttribute attribute -> attribute.getType();
			case ObjRelationship relationship -> relationship.getTargetEntity().getClassName();
			default -> OBJECT_TYPE;
		};
	}

	private static String collectionType(String elementType) {
		return Collection.class.getName() + "<" + elementType + ">";
	}

	private static String moreSpecific(String type1, String type2) {
		if (OBJECT_TYPE.equals(type1) || type1.equals(type2)) {
			return type2;
		}
		return OBJECT_TYPE.equals(type2) ? type1 : OBJECT_TYPE;
	}

	private record CachedParameters(FluentSelect<?, ?> parsed, List<QueryParameter> parameters) {
	}

	/**
	 * A "$name" parameter of a select query.
	 *
	 * @since 5.0
	 */
	public record QueryParameter(String name, String javaName, String type) {
	}
}
