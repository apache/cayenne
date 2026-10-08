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

package org.apache.cayenne.wocompat;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.ExpressionParameter;
import org.apache.cayenne.exp.EqualExp;
import org.apache.cayenne.exp.NotEqualExp;
import org.apache.cayenne.exp.LikeExp;
import org.apache.cayenne.exp.LikeIgnoreCaseExp;
import org.apache.cayenne.exp.LessExp;
import org.apache.cayenne.exp.LessOrEqualExp;
import org.apache.cayenne.exp.GreaterExp;
import org.apache.cayenne.exp.GreaterOrEqualExp;
import org.apache.cayenne.exp.path.CayennePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * EOFetchSpecificationParser parses EOFetchSpecifications from a
 * WebObjects-style EOModel. It recursively builds Cayenne Expression
 * objects and assembles them into the final aggregate Expression.
 */
@SuppressWarnings("unchecked")
class EOFetchSpecificationParser {

	// Xcode/EOModeler expressions have a colon at the end of the selector
	// name
	// (just like standard Objective-C syntax). WOLips does not. Add both
	// sets to the hash map to handle both types of models.

	// Selector strings (Java-base).
	static final String IS_EQUAL_TO = "isEqualTo";
	static final String IS_NOT_EQUAL_TO = "isNotEqualTo";
	static final String IS_LIKE = "isLike";
	static final String CASE_INSENSITIVE_LIKE = "isCaseInsensitiveLike";
	static final String IS_LESS_THAN = "isLessThan";
	static final String IS_LESS_THAN_OR_EQUAL_TO = "isLessThanOrEqualTo";
	static final String IS_GREATER_THAN = "isGreaterThan";
	static final String IS_GREATER_THAN_OR_EQUAL_TO = "isGreaterThanOrEqualTo";

	private static final String OBJ_C = ":"; // Objective-C syntax addition.

	private static Map<String, Supplier<Expression>> selectorToExpressionBridge;
	private static final Logger LOGGER = LoggerFactory.getLogger(EOFetchSpecificationParser.class);

	/**
	 * selectorToExpressionBridge is just a mapping of EOModeler's selector types to factories of the equivalent
	 * Cayenne expressions.
	 */
	static synchronized Map<String, Supplier<Expression>> selectorToExpressionBridge() {
		// Initialize selectorToExpressionBridge if needed.
		if (null == selectorToExpressionBridge) {
			selectorToExpressionBridge = new HashMap<>();

			selectorToExpressionBridge.put(IS_EQUAL_TO, EqualExp::new);
			selectorToExpressionBridge.put(IS_EQUAL_TO + OBJ_C, EqualExp::new);

			selectorToExpressionBridge.put(IS_NOT_EQUAL_TO, NotEqualExp::new);
			selectorToExpressionBridge.put(IS_NOT_EQUAL_TO + OBJ_C, NotEqualExp::new);

			selectorToExpressionBridge.put(IS_LIKE, LikeExp::new);
			selectorToExpressionBridge.put(IS_LIKE + OBJ_C, LikeExp::new);

			selectorToExpressionBridge.put(CASE_INSENSITIVE_LIKE, LikeIgnoreCaseExp::new);
			selectorToExpressionBridge.put(CASE_INSENSITIVE_LIKE + OBJ_C, LikeIgnoreCaseExp::new);

			selectorToExpressionBridge.put(IS_LESS_THAN, LessExp::new);
			selectorToExpressionBridge.put(IS_LESS_THAN + OBJ_C, LessExp::new);

			selectorToExpressionBridge.put(IS_LESS_THAN_OR_EQUAL_TO, LessOrEqualExp::new);
			selectorToExpressionBridge.put(IS_LESS_THAN_OR_EQUAL_TO + OBJ_C, LessOrEqualExp::new);

			selectorToExpressionBridge.put(IS_GREATER_THAN, GreaterExp::new);
			selectorToExpressionBridge.put(IS_GREATER_THAN + OBJ_C, GreaterExp::new);

			selectorToExpressionBridge.put(IS_GREATER_THAN_OR_EQUAL_TO, GreaterOrEqualExp::new);
			selectorToExpressionBridge.put(IS_GREATER_THAN_OR_EQUAL_TO + OBJ_C, GreaterOrEqualExp::new);
		}
		return selectorToExpressionBridge;
	}

	/**
	 * isAggregate determines whether a qualifier is "aggregate" -- has
	 * children -- or "simple".
	 * 
	 * @param qualifier
	 *            - a Map containing the qualifier settings
	 * @return boolean indicating whether the qualifier is "aggregate"
	 *         qualifier
	 */
	static boolean isAggregate(Map<String, ?> qualifier) {
		boolean result = true;

		String theClass = (String) qualifier.get("class");
		if (theClass == null) {
			return false; // should maybe throw an exception?
		}
		if (theClass.equalsIgnoreCase("EOKeyValueQualifier")
				|| theClass.equalsIgnoreCase("EOKeyComparisonQualifier")) {
			result = false;
		}

		return result;
	}

	/**
	 * Returns a factory of the Cayenne expression equivalent to the selector of an EOModeler FetchSpecification
	 * qualifier, or null for an unknown selector.
	 */
	static Supplier<Expression> expressionForQualifier(Map<String, ?> qualifierMap) {
		return selectorToExpressionBridge().get((String) qualifierMap.get("selectorName"));
	}

	/**
	 * makeQualifier recursively builds an Expression for each condition in
	 * the qualifierMap and assembles from them the complex Expression to
	 * represent the entire EOFetchSpecification.
	 * 
	 * @param qualifierMap
	 *            - Map representation of EOFetchSpecification
	 * @return Expression translation of the EOFetchSpecification
	 */
	static Expression makeQualifier(EOObjEntity entity, Map<String, ?> qualifierMap) {
		if (isAggregate(qualifierMap)) {
			// the fetch specification has more than one qualifier
			String aggregateClass = (String) qualifierMap.get("class"); // AND, OR, NOT

			if (aggregateClass.equalsIgnoreCase("EONotQualifier")) {
				// NOT qualifiers only have one child, keyed with
				// "qualifier"
				Map<String, ?> child = (Map<String, ?>) qualifierMap.get("qualifier");
				// build the child expression
				Expression childExp = makeQualifier(entity, child);

				return childExp.notExp(); // add the "not" clause and return
											// the
				// result
			} else {
				// AND, OR qualifiers can have multiple children, keyed with
				// "qualifiers"
				// get the list of children
				List<Map<String, ?>> children = (List<Map<String, ?>>) qualifierMap.get("qualifiers");
				if (children != null) {
					ArrayList<Expression> childExpressions = new ArrayList<>();
					// build an Expression for each child
					for (Map<String, ?> child : children) {
						Expression childExp = makeQualifier(entity, child);
						childExpressions.add(childExp);
					}
					// join the child expressions and return the result
					if (aggregateClass.equalsIgnoreCase("EOAndQualifier")) {
						return ExpressionFactory.and(childExpressions);
					}
					if (aggregateClass.equalsIgnoreCase("EOOrQualifier")) {
						return ExpressionFactory.or(childExpressions);
					}
					throw new ExpressionException("Unknown aggregate qualifier class: " + aggregateClass);
				}
			}

		} // end if isAggregate(qualifierMap)...

		// the query has a single qualifier
		// get expression selector type
		String qualifierClass = (String) qualifierMap.get("class");

		// the key or key path we're comparing
		String key = null;
		// the key, keyPath, value, or parameterized value against which
		// we're
		// comparing the key
		Object comparisonValue = null;

		if ("EOKeyComparisonQualifier".equals(qualifierClass)) {
			// Comparing two keys or key paths
			key = (String) qualifierMap.get("leftValue");
			comparisonValue = qualifierMap.get("rightValue");
			// FIXME: I think EOKeyComparisonQualifier style Expressions are not supported...
			return null;
		} else if ("EOKeyValueQualifier".equals(qualifierClass)) {
			// Comparing key with a value or parameterized value
			key = (String) qualifierMap.get("key");
			Object value = qualifierMap.get("value");

			if (value instanceof Map) {
				Map<String, String> valueMap = (Map<String, String>) value;
				String objClass = valueMap.get("class"); // can be a
				// qualifier class or java type
				if ("EOQualifierVariable".equals(objClass) && valueMap.containsKey("_key")) {
					// make a parameterized expression
					String paramName = valueMap.get("_key");
					comparisonValue = new ExpressionParameter(paramName);
				} else {
					Object queryVal = valueMap.get("value");
					if ("NSNumber".equals(objClass)) {
						// comparison to NSNumber -- cast
						comparisonValue = queryVal;
					} else if ("EONull".equals(objClass)) {
						// comparison to null
						comparisonValue = null;
					} else { // Could there be other types? boolean, date,
								// etc.???
								// no cast
						comparisonValue = queryVal;
					}
				}

			} else if (value instanceof String) {
				// value expression
				comparisonValue = value;
			} // end if (value instanceof Map) else...
		}

		// check whether the key is an object path; if at least one
		// component is not,
		// switch to db path..

		Expression keyExp = ExpressionFactory.exp(key);
		try {
			entity.resolvePath(CayennePath.of(key));
		} catch (ExpressionException e) {
			try {
				keyExp = entity.translateToDbPath(keyExp);
			} catch (Exception dbpathEx) {
				LOGGER.warn("Couldn't find {} in {} in EOModel", keyExp, entity.getName());
			}
		}

		Supplier<Expression> expressionFactory = expressionForQualifier(qualifierMap);
		if (expressionFactory == null) {
			LOGGER.warn("Unsupported qualifier selector: {}", qualifierMap.get("selectorName"));
			return null;
		}

		Expression exp = expressionFactory.get();
		exp.setOperand(0, keyExp);
		exp.setOperand(1, comparisonValue);
		return exp;
	}
}
