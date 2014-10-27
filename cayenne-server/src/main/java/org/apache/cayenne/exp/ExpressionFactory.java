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

package org.apache.cayenne.exp;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.parser.ASTAdd;
import org.apache.cayenne.exp.parser.ASTAnd;
import org.apache.cayenne.exp.parser.ASTBetween;
import org.apache.cayenne.exp.parser.ASTBitwiseAnd;
import org.apache.cayenne.exp.parser.ASTBitwiseLeftShift;
import org.apache.cayenne.exp.parser.ASTBitwiseNot;
import org.apache.cayenne.exp.parser.ASTBitwiseOr;
import org.apache.cayenne.exp.parser.ASTBitwiseRightShift;
import org.apache.cayenne.exp.parser.ASTBitwiseXor;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.exp.parser.ASTDivide;
import org.apache.cayenne.exp.parser.ASTEqual;
import org.apache.cayenne.exp.parser.ASTFalse;
import org.apache.cayenne.exp.parser.ASTGreater;
import org.apache.cayenne.exp.parser.ASTGreaterOrEqual;
import org.apache.cayenne.exp.parser.ASTIn;
import org.apache.cayenne.exp.parser.ASTLess;
import org.apache.cayenne.exp.parser.ASTLessOrEqual;
import org.apache.cayenne.exp.parser.ASTLike;
import org.apache.cayenne.exp.parser.ASTLikeIgnoreCase;
import org.apache.cayenne.exp.parser.ASTList;
import org.apache.cayenne.exp.parser.ASTMultiply;
import org.apache.cayenne.exp.parser.ASTNegate;
import org.apache.cayenne.exp.parser.ASTNot;
import org.apache.cayenne.exp.parser.ASTNotBetween;
import org.apache.cayenne.exp.parser.ASTNotEqual;
import org.apache.cayenne.exp.parser.ASTNotIn;
import org.apache.cayenne.exp.parser.ASTNotLike;
import org.apache.cayenne.exp.parser.ASTNotLikeIgnoreCase;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.exp.parser.ASTOr;
import org.apache.cayenne.exp.parser.ASTPath;
import org.apache.cayenne.exp.parser.ASTSubtract;
import org.apache.cayenne.exp.parser.ASTTrue;
import org.apache.cayenne.exp.parser.ExpressionParser;
import org.apache.cayenne.exp.parser.ExpressionParserTokenManager;
import org.apache.cayenne.exp.parser.JavaCharStream;
import org.apache.cayenne.exp.parser.ParseException;
import org.apache.cayenne.exp.parser.SimpleNode;
import org.apache.cayenne.map.Entity;

/**
 * Helper class to build expressions. Alternatively expressions can be built
 * using {@link org.apache.cayenne.exp.Expression#fromString(String)} method.
 */
public class ExpressionFactory {

	/**
	 * A "split" character, "|", that is understood by some of the
	 * ExpressionFactory methods that require splitting joins in the middle of
	 * the path.
	 * 
	 * @since 3.0
	 */
	public static final char SPLIT_SEPARATOR = '|';

	private static Class<?>[] typeLookup;
	private static volatile int autoAliasId;

	private static final int PARSE_BUFFER_MAX_SIZE = 4096;

	static {

		// make sure all types are small integers, then we can use
		// them as indexes in lookup array
		int[] allTypes = new int[] { Expression.AND, Expression.OR,
				Expression.NOT, Expression.EQUAL_TO, Expression.NOT_EQUAL_TO,
				Expression.LESS_THAN, Expression.GREATER_THAN,
				Expression.LESS_THAN_EQUAL_TO,
				Expression.GREATER_THAN_EQUAL_TO, Expression.BETWEEN,
				Expression.IN, Expression.LIKE, Expression.LIKE_IGNORE_CASE,
				Expression.ADD, Expression.SUBTRACT, Expression.MULTIPLY,
				Expression.DIVIDE, Expression.NEGATIVE, Expression.OBJ_PATH,
				Expression.DB_PATH, Expression.LIST, Expression.NOT_BETWEEN,
				Expression.NOT_IN, Expression.NOT_LIKE,
				Expression.NOT_LIKE_IGNORE_CASE, Expression.TRUE,
				Expression.FALSE, Expression.BITWISE_NOT,
				Expression.BITWISE_AND, Expression.BITWISE_OR,
				Expression.BITWISE_XOR, Expression.BITWISE_LEFT_SHIFT,
				Expression.BITWISE_RIGHT_SHIFT };

		int max = 0;
		int min = 0;
		int allLen = allTypes.length;
		for (int i = 0; i < allLen; i++) {
			if (allTypes[i] > max)
				max = allTypes[i];
			else if (allTypes[i] < min)
				min = allTypes[i];
		}

		// sanity check....
		if (max > 500)
			throw new RuntimeException("Types values are too big: " + max);
		if (min < 0)
			throw new RuntimeException("Types values are too small: " + min);

		// now we know that if types are used as indexes,
		// they will fit in array "max + 1" long (though gaps are possible)

		typeLookup = new Class[max + 1];

		typeLookup[Expression.AND] = ASTAnd.class;
		typeLookup[Expression.OR] = ASTOr.class;
		typeLookup[Expression.BETWEEN] = ASTBetween.class;
		typeLookup[Expression.NOT_BETWEEN] = ASTNotBetween.class;

		// binary types
		typeLookup[Expression.EQUAL_TO] = ASTEqual.class;
		typeLookup[Expression.NOT_EQUAL_TO] = ASTNotEqual.class;
		typeLookup[Expression.LESS_THAN] = ASTLess.class;
		typeLookup[Expression.GREATER_THAN] = ASTGreater.class;
		typeLookup[Expression.LESS_THAN_EQUAL_TO] = ASTLessOrEqual.class;
		typeLookup[Expression.GREATER_THAN_EQUAL_TO] = ASTGreaterOrEqual.class;
		typeLookup[Expression.IN] = ASTIn.class;
		typeLookup[Expression.NOT_IN] = ASTNotIn.class;
		typeLookup[Expression.LIKE] = ASTLike.class;
		typeLookup[Expression.LIKE_IGNORE_CASE] = ASTLikeIgnoreCase.class;
		typeLookup[Expression.NOT_LIKE] = ASTNotLike.class;
		typeLookup[Expression.NOT_LIKE_IGNORE_CASE] = ASTNotLikeIgnoreCase.class;
		typeLookup[Expression.ADD] = ASTAdd.class;
		typeLookup[Expression.SUBTRACT] = ASTSubtract.class;
		typeLookup[Expression.MULTIPLY] = ASTMultiply.class;
		typeLookup[Expression.DIVIDE] = ASTDivide.class;

		typeLookup[Expression.NOT] = ASTNot.class;
		typeLookup[Expression.NEGATIVE] = ASTNegate.class;
		typeLookup[Expression.OBJ_PATH] = ASTObjPath.class;
		typeLookup[Expression.DB_PATH] = ASTDbPath.class;
		typeLookup[Expression.LIST] = ASTList.class;

		typeLookup[Expression.TRUE] = ASTTrue.class;
		typeLookup[Expression.FALSE] = ASTFalse.class;

		typeLookup[Expression.BITWISE_NOT] = ASTBitwiseNot.class;
		typeLookup[Expression.BITWISE_OR] = ASTBitwiseOr.class;
		typeLookup[Expression.BITWISE_AND] = ASTBitwiseAnd.class;
		typeLookup[Expression.BITWISE_XOR] = ASTBitwiseXor.class;
		typeLookup[Expression.BITWISE_LEFT_SHIFT] = ASTBitwiseLeftShift.class;
		typeLookup[Expression.BITWISE_RIGHT_SHIFT] = ASTBitwiseRightShift.class;
	}

	/**
	 * Creates a new expression for the type requested. If type is unknown,
	 * ExpressionException is thrown.
	 */
	public static Expression expressionOfType(int type) {
		if (type < 0 || type >= typeLookup.length) {
			throw new ExpressionException("Bad expression type: " + type);
		}

		if (typeLookup[type] == null) {
			throw new ExpressionException("Bad expression type: " + type);
		}

		// expected this
		if (SimpleNode.class.isAssignableFrom(typeLookup[type])) {
			try {
				return (Expression) typeLookup[type].newInstance();
			} catch (Exception ex) {
				throw new ExpressionException("Error creating expression", ex);
			}
		}

		throw new ExpressionException("Bad expression type: " + type);
	}

	/**
	 * Applies a few default rules for adding operands to expressions. In
	 * particular wraps all lists into LIST expressions. Applied only in path
	 * expressions.
	 */
	protected static Object wrapPathOperand(Object op) {
		if (op instanceof Collection<?>) {
			return new ASTList((Collection<?>) op);
		} else if (op instanceof Object[]) {
			return new ASTList((Object[]) op);
		} else {
			return op;
		}
	}

	/**
	 * Creates an expression that matches any of the key-values pairs in
	 * <code>map</code>.
	 * <p>
	 * For each pair <code>pairType</code> operator is used to build a binary
	 * expression. Key is considered to be a DB_PATH expression. OR is used to
	 * join pair binary expressions.
	 */
	public static Expression matchAnyDbExp(Map<String, ?> map, int pairType) {
		List<Expression> pairs = new ArrayList<Expression>(map.size());

		for (Map.Entry<String, ?> entry : map.entrySet()) {
			Expression exp = expressionOfType(pairType);
			exp.setOperand(0, new ASTDbPath(entry.getKey()));
			exp.setOperand(1, wrapPathOperand(entry.getValue()));
			pairs.add(exp);
		}

		return joinExp(Expression.OR, pairs);
	}

	/**
	 * Creates an expression that matches all key-values pairs in
	 * <code>map</code>.
	 * <p>
	 * For each pair <code>pairType</code> operator is used to build a binary
	 * expression. Key is considered to be a DB_PATH expression. AND is used to
	 * join pair binary expressions.
	 */
	public static Expression matchAllDbExp(Map<String, ?> map, int pairType) {
		List<Expression> pairs = new ArrayList<Expression>(map.size());

		for (Map.Entry<String, ?> entry : map.entrySet()) {
			Expression exp = expressionOfType(pairType);
			exp.setOperand(0, new ASTDbPath(entry.getKey()));
			exp.setOperand(1, wrapPathOperand(entry.getValue()));
			pairs.add(exp);
		}

		return joinExp(Expression.AND, pairs);
	}

	/**
	 * Creates an expression that matches any of the key-values pairs in the
	 * <code>map</code>.
	 * <p>
	 * For each pair <code>pairType</code> operator is used to build a binary
	 * expression. Key is considered to be a OBJ_PATH expression. OR is used to
	 * join pair binary expressions.
	 */
	public static Expression matchAnyExp(Map<String, ?> map, int pairType) {
		List<Expression> pairs = new ArrayList<Expression>(map.size());

		for (Map.Entry<String, ?> entry : map.entrySet()) {

			Expression exp = expressionOfType(pairType);
			exp.setOperand(0, new ASTObjPath(entry.getKey()));
			exp.setOperand(1, wrapPathOperand(entry.getValue()));
			pairs.add(exp);
		}

		return joinExp(Expression.OR, pairs);
	}

	/**
	 * Creates an expression to match a collection of values against a single
	 * path expression. <h3>Splits</h3>
	 * <p>
	 * Note that "path" argument here can use a split character (a pipe symbol -
	 * '|') instead of dot to indicate that relationship following a path should
	 * be split into a separate set of joins. There can only be one split at
	 * most. Split must always precede a relationship. E.g.
	 * "|exhibits.paintings", "exhibits|paintings", etc.
	 * 
	 * @param path
	 * @param values
	 * @since 3.0
	 */
	public static Expression matchAllExp(String path, Collection<?> values) {

		if (values == null) {
			throw new NullPointerException("Null values collection");
		}

		if (values.size() == 0) {
			return new ASTTrue();
		}

		return matchAllExp(path, values.toArray());
	}

	/**
	 * @since 3.0
	 */
	public static Expression matchAllExp(String path, Object... values) {

		if (values == null) {
			throw new NullPointerException("Null values collection");
		}

		if (values.length == 0) {
			return new ASTTrue();
		}

		int split = path.indexOf(SPLIT_SEPARATOR);

		List<Expression> matches = new ArrayList<Expression>(values.length);

		if (split >= 0 && split < path.length() - 1) {

			int splitEnd = path.indexOf(Entity.PATH_SEPARATOR, split + 1);

			String beforeSplit = split > 0 ? path.substring(0, split) + "."
					: "";
			String afterSplit = splitEnd > 0 ? "."
					+ path.substring(splitEnd + 1) : "";
			String aliasBase = "split" + autoAliasId++ + "_";
			String splitChunk = splitEnd > 0 ? path.substring(split + 1,
					splitEnd) : path.substring(split + 1);

			// fix the path - replace split with dot if it's in the middle, or
			// strip it if
			// it's in the beginning
			path = split == 0 ? path.substring(1) : path.replace(
					SPLIT_SEPARATOR, '.');

			int i = 0;
			for (Object value : values) {

				String alias = aliasBase + i;
				String aliasedPath = beforeSplit + alias + afterSplit;
				i++;

				ASTPath pathExp = new ASTObjPath(aliasedPath);
				pathExp.setPathAliases(Collections.singletonMap(alias,
						splitChunk));
				matches.add(new ASTEqual(pathExp, value));
			}
		} else {
			for (Object value : values) {
				matches.add(new ASTEqual(new ASTObjPath(path), value));
			}
		}

		return joinExp(Expression.AND, matches);
	}

	/**
	 * Creates an expression that matches all key-values pairs in
	 * <code>map</code>.
	 * <p>
	 * For each pair <code>pairType</code> operator is used to build a binary
	 * expression. Key is considered to be a OBJ_PATH expression. AND is used to
	 * join pair binary expressions.
	 */
	public static Expression matchAllExp(Map<String, ?> map, int pairType) {
		List<Expression> pairs = new ArrayList<Expression>(map.size());

		for (Map.Entry<String, ?> entry : map.entrySet()) {

			Expression exp = expressionOfType(pairType);
			exp.setOperand(0, new ASTObjPath(entry.getKey()));
			exp.setOperand(1, wrapPathOperand(entry.getValue()));
			pairs.add(exp);
		}

		return joinExp(Expression.AND, pairs);
	}

	/**
	 * A convenience method to create an DB_PATH "equal to" expression.
	 */
	public static Expression matchDbExp(String pathSpec, Object value) {
		return new ASTEqual(new ASTDbPath(pathSpec), value);
	}

	/**
	 * A convenience method to create an DB_PATH "not equal to" expression.
	 */
	public static Expression noMatchDbExp(String pathSpec, Object value) {
		return new ASTNotEqual(new ASTDbPath(pathSpec), value);
	}

	/**
	 * A convenience method to create an OBJ_PATH "equal to" expression.
	 */
	public static Expression matchExp(String pathSpec, Object value) {
		return new ASTEqual(new ASTObjPath(pathSpec), value);
	}

	/**
	 * A convenience method to create an OBJ_PATH "not equal to" expression.
	 */
	public static Expression noMatchExp(String pathSpec, Object value) {
		return new ASTNotEqual(new ASTObjPath(pathSpec), value);
	}

	/**
	 * A convenience method to create an OBJ_PATH "less than" expression.
	 */
	public static Expression lessExp(String pathSpec, Object value) {
		return new ASTLess(new ASTObjPath(pathSpec), value);
	}

	/**
	 * A convenience method to create an DB_PATH "less than" expression.
	 * 
	 * @since 3.0
	 */
	public static Expression lessDbExp(String pathSpec, Object value) {
		return new ASTLess(new ASTDbPath(pathSpec), value);
	}

	/**
	 * A convenience method to create an OBJ_PATH "less than or equal to"
	 * expression.
	 */
	public static Expression lessOrEqualExp(String pathSpec, Object value) {
		return new ASTLessOrEqual(new ASTObjPath(pathSpec), value);
	}

	/**
	 * A convenience method to create an DB_PATH "less than or equal to"
	 * expression.
	 * 
	 * @since 3.0
	 */
	public static Expression lessOrEqualDbExp(String pathSpec, Object value) {
		return new ASTLessOrEqual(new ASTDbPath(pathSpec), value);
	}

	/**
	 * A convenience method to create an OBJ_PATH "greater than" expression.
	 */
	public static Expression greaterExp(String pathSpec, Object value) {
		return new ASTGreater(new ASTObjPath(pathSpec), value);
	}

	/**
	 * A convenience method to create an DB_PATH "greater than" expression.
	 * 
	 * @since 3.0
	 */
	public static Expression greaterDbExp(String pathSpec, Object value) {
		return new ASTGreater(new ASTDbPath(pathSpec), value);
	}

	/**
	 * A convenience method to create an OBJ_PATH "greater than or equal to"
	 * expression.
	 */
	public static Expression greaterOrEqualExp(String pathSpec, Object value) {
		return new ASTGreaterOrEqual(new ASTObjPath(pathSpec), value);
	}

	/**
	 * A convenience method to create an DB_PATH "greater than or equal to"
	 * expression.
	 * 
	 * @since 3.0
	 */
	public static Expression greaterOrEqualDbExp(String pathSpec, Object value) {
		return new ASTGreaterOrEqual(new ASTDbPath(pathSpec), value);
	}

	/**
	 * A convenience shortcut for building IN expression. Return ASTFalse for
	 * empty collection.
	 */
	public static Expression inExp(String pathSpec, Object... values) {
		if (values.length == 0) {
			return new ASTFalse();
		}
		return new ASTIn(new ASTObjPath(pathSpec), new ASTList(values));
	}

	/**
	 * A convenience shortcut for building IN DB expression. Return ASTFalse for
	 * empty collection.
	 */
	public static Expression inDbExp(String pathSpec, Object... values) {
		if (values.length == 0) {
			return new ASTFalse();
		}
		return new ASTIn(new ASTDbPath(pathSpec), new ASTList(values));
	}

	/**
	 * A convenience shortcut for building IN expression. Return ASTFalse for
	 * empty collection.
	 */
	public static Expression inExp(String pathSpec, Collection<?> values) {
		if (values.isEmpty()) {
			return new ASTFalse();
		}
		return new ASTIn(new ASTObjPath(pathSpec), new ASTList(values));
	}

	/**
	 * A convenience shortcut for building IN DB expression. Return ASTFalse for
	 * empty collection.
	 */
	public static Expression inDbExp(String pathSpec, Collection<?> values) {
		if (values.isEmpty()) {
			return new ASTFalse();
		}
		return new ASTIn(new ASTDbPath(pathSpec), new ASTList(values));
	}

	/**
	 * A convenience shortcut for building NOT_IN expression. Return ASTTrue for
	 * empty collection.
	 */
	public static Expression notInExp(String pathSpec, Collection<?> values) {
		if (values.isEmpty()) {
			return new ASTTrue();
		}
		return new ASTNotIn(new ASTObjPath(pathSpec), new ASTList(values));
	}

	/**
	 * A convenience shortcut for building NOT_IN expression. Return ASTTrue for
	 * empty collection.
	 * 
	 * @since 3.0
	 */
	public static Expression notInDbExp(String pathSpec, Collection<?> values) {
		if (values.isEmpty()) {
			return new ASTTrue();
		}
		return new ASTNotIn(new ASTDbPath(pathSpec), new ASTList(values));
	}

	/**
	 * A convenience shortcut for building NOT_IN expression. Return ASTTrue for
	 * empty collection.
	 * 
	 * @since 1.0.6
	 */
	public static Expression notInExp(String pathSpec, Object... values) {
		if (values.length == 0) {
			return new ASTTrue();
		}
		return new ASTNotIn(new ASTObjPath(pathSpec), new ASTList(values));
	}

	/**
	 * A convenience shortcut for building NOT_IN expression. Return ASTTrue for
	 * empty collection.
	 * 
	 * @since 3.0
	 */
	public static Expression notInDbExp(String pathSpec, Object... values) {
		if (values.length == 0) {
			return new ASTTrue();
		}
		return new ASTNotIn(new ASTDbPath(pathSpec), new ASTList(values));
	}

	/**
	 * A convenience shortcut for building BETWEEN expressions.
	 */
	public static Expression betweenExp(String pathSpec, Object value1,
			Object value2) {
		return new ASTBetween(new ASTObjPath(pathSpec), value1, value2);
	}

	/**
	 * A convenience shortcut for building BETWEEN expressions.
	 * 
	 * @since 3.0
	 */
	public static Expression betweenDbExp(String pathSpec, Object value1,
			Object value2) {
		return new ASTBetween(new ASTDbPath(pathSpec), value1, value2);
	}

	/**
	 * A convenience shortcut for building NOT_BETWEEN expressions.
	 */
	public static Expression notBetweenExp(String pathSpec, Object value1,
			Object value2) {
		return new ASTNotBetween(new ASTObjPath(pathSpec), value1, value2);
	}

	/**
	 * A convenience shortcut for building NOT_BETWEEN expressions.
	 * 
	 * @since 3.0
	 */
	public static Expression notBetweenDbExp(String pathSpec, Object value1,
			Object value2) {
		return new ASTNotBetween(new ASTDbPath(pathSpec), value1, value2);
	}

	/**
	 * A convenience shortcut for building LIKE expression.
	 */
	public static Expression likeExp(String pathSpec, Object value) {
		return new ASTLike(new ASTObjPath(pathSpec), value);
	}

	/**
	 * <p>
	 * A convenience shortcut for building LIKE expression.
	 * </p>
	 * <p>
	 * The escape character allows for escaping meta-characters in the LIKE
	 * clause. Note that the escape character cannot be '?'. To specify no
	 * escape character, supply 0 as the escape character.
	 * </p>
	 * 
	 * @since 3.0.1
	 */
	public static Expression likeExp(String pathSpec, Object value,
			char escapeChar) {
		return new ASTLike(new ASTObjPath(pathSpec), value, escapeChar);
	}

	/**
	 * A convenience shortcut for building LIKE DB_PATH expression.
	 * 
	 * @since 3.0
	 */
	public static Expression likeDbExp(String pathSpec, Object value) {
		return new ASTLike(new ASTDbPath(pathSpec), value);
	}

	/**
	 * <p>
	 * A convenience shortcut for building LIKE DB_PATH expression.
	 * </p>
	 * <p>
	 * The escape character allows for escaping meta-characters in the LIKE
	 * clause. Note that the escape character cannot be '?'. To specify no
	 * escape character, supply 0 as the escape character.
	 * </p>
	 * 
	 * @since 3.0.1
	 */
	public static Expression likeDbExp(String pathSpec, Object value,
			char escapeChar) {
		return new ASTLike(new ASTDbPath(pathSpec), value, escapeChar);
	}

	/**
	 * A convenience shortcut for building NOT_LIKE expression.
	 */
	public static Expression notLikeExp(String pathSpec, Object value) {
		return new ASTNotLike(new ASTObjPath(pathSpec), value);
	}

	/**
	 * <p>
	 * A convenience shortcut for building NOT_LIKE expression.
	 * </p>
	 * <p>
	 * The escape character allows for escaping meta-characters in the LIKE
	 * clause. Note that the escape character cannot be '?'. To specify no
	 * escape character, supply 0 as the escape character.
	 * </p>
	 * 
	 * @since 3.0.1
	 */
	public static Expression notLikeExp(String pathSpec, Object value,
			char escapeChar) {
		return new ASTNotLike(new ASTObjPath(pathSpec), value, escapeChar);
	}

	/**
	 * A convenience shortcut for building NOT_LIKE expression.
	 * 
	 * @since 3.0
	 */
	public static Expression notLikeDbExp(String pathSpec, Object value) {
		return new ASTNotLike(new ASTDbPath(pathSpec), value);
	}

	/**
	 * <p>
	 * A convenience shortcut for building NOT_LIKE expression.
	 * </p>
	 * <p>
	 * The escape character allows for escaping meta-characters in the LIKE
	 * clause. Note that the escape character cannot be '?'. To specify no
	 * escape character, supply 0 as the escape character.
	 * </p>
	 * 
	 * @since 3.0.1
	 */
	public static Expression notLikeDbExp(String pathSpec, Object value,
			char escapeChar) {
		return new ASTNotLike(new ASTDbPath(pathSpec), value, escapeChar);
	}

	/**
	 * A convenience shortcut for building LIKE_IGNORE_CASE expression.
	 */
	public static Expression likeIgnoreCaseExp(String pathSpec, Object value) {
		return new ASTLikeIgnoreCase(new ASTObjPath(pathSpec), value);
	}

	/**
	 * <p>
	 * A convenience shortcut for building LIKE_IGNORE_CASE expression.
	 * </p>
	 * <p>
	 * The escape character allows for escaping meta-characters in the LIKE
	 * clause. Note that the escape character cannot be '?'. To specify no
	 * escape character, supply 0 as the escape character.
	 * </p>
	 * 
	 * @since 3.0.1
	 */
	public static Expression likeIgnoreCaseExp(String pathSpec, Object value,
			char escapeChar) {
		return new ASTLikeIgnoreCase(new ASTObjPath(pathSpec), value,
				escapeChar);
	}

	/**
	 * A convenience shortcut for building LIKE_IGNORE_CASE expression.
	 * 
	 * @since 3.0
	 */
	public static Expression likeIgnoreCaseDbExp(String pathSpec, Object value) {
		return new ASTLikeIgnoreCase(new ASTDbPath(pathSpec), value);
	}

	/**
	 * <p>
	 * A convenience shortcut for building LIKE_IGNORE_CASE expression.
	 * </p>
	 * <p>
	 * The escape character allows for escaping meta-characters in the LIKE
	 * clause. Note that the escape character cannot be '?'. To specify no
	 * escape character, supply 0 as the escape character.
	 * </p>
	 * 
	 * @since 3.0.1
	 */
	public static Expression likeIgnoreCaseDbExp(String pathSpec, Object value,
			char escapeChar) {
		return new ASTLikeIgnoreCase(new ASTDbPath(pathSpec), value, escapeChar);
	}

	/**
	 * A convenience shortcut for building NOT_LIKE_IGNORE_CASE expression.
	 */
	public static Expression notLikeIgnoreCaseExp(String pathSpec, Object value) {
		return new ASTNotLikeIgnoreCase(new ASTObjPath(pathSpec), value);
	}

	/**
	 * <p>
	 * A convenience shortcut for building NOT_LIKE_IGNORE_CASE expression.
	 * </p>
	 * <p>
	 * The escape character allows for escaping meta-characters in the LIKE
	 * clause. Note that the escape character cannot be '?'. To specify no
	 * escape character, supply 0 as the escape character.
	 * </p>
	 * 
	 * @since 3.0.1
	 */
	public static Expression notLikeIgnoreCaseExp(String pathSpec,
			Object value, char escapeChar) {
		return new ASTNotLikeIgnoreCase(new ASTObjPath(pathSpec), value,
				escapeChar);
	}

	/**
	 * A convenience shortcut for building NOT_LIKE_IGNORE_CASE expression.
	 * 
	 * @since 3.0
	 */
	public static Expression notLikeIgnoreCaseDbExp(String pathSpec,
			Object value) {
		return new ASTNotLikeIgnoreCase(new ASTDbPath(pathSpec), value);
	}

	/**
	 * <p>
	 * A convenience shortcut for building NOT_LIKE_IGNORE_CASE expression.
	 * </p>
	 * <p>
	 * The escape character allows for escaping meta-characters in the LIKE
	 * clause. Note that the escape character cannot be '?'. To specify no
	 * escape character, supply 0 as the escape character.
	 * </p>
	 * 
	 * @since 3.0.1
	 */
	public static Expression notLikeIgnoreCaseDbExp(String pathSpec,
			Object value, char escapeChar) {
		return new ASTNotLikeIgnoreCase(new ASTDbPath(pathSpec), value,
				escapeChar);
	}

	/**
	 * A convenience shortcut for boolean true expression.
	 * 
	 * @since 3.0
	 */
	public static Expression expTrue() {
		return new ASTTrue();
	}

	/**
	 * A convenience shortcut for boolean false expression.
	 * 
	 * @since 3.0
	 */
	public static Expression expFalse() {
		return new ASTFalse();
	}

	/**
	 * Joins all expressions, making a single expression. <code>type</code> is
	 * used as an expression type for expressions joining each one of the items
	 * on the list. <code>type</code> must be binary expression type.
	 * <p>
	 * For example, if type is Expression.AND, resulting expression would match
	 * all expressions in the list. If type is Expression.OR, resulting
	 * expression would match any of the expressions.
	 * </p>
	 */
	public static Expression joinExp(int type,
			Collection<Expression> expressions) {
		int len = expressions.size();
		if (len == 0) {
			return null;
		}

		return join(type, expressions.toArray(new Expression[len]));
	}

	private static Expression join(int type, Expression... expressions) {

		int len = expressions != null ? expressions.length : 0;
		if (len == 0) {
			return null;
		}

		Expression currentExp = expressions[0];
		if (len == 1) {
			return currentExp;
		}

		Expression exp = expressionOfType(type);
		for (int i = 0; i < len; i++) {
			exp.setOperand(i, expressions[i]);
		}
		return exp;
	}

	/**
	 * Creates an expression that matches the primary key of object in
	 * <code>ObjectId</code>'s <code>IdSnapshot</code> for the argument
	 * <code>object</code>.
	 */
	public static Expression matchExp(Persistent object) {
		return matchAllDbExp(object.getObjectId().getIdSnapshot(),
				Expression.EQUAL_TO);
	}

	/**
	 * Creates an expression that matches any of the objects contained in the
	 * list <code>objects</code>
	 */
	public static Expression matchAnyExp(List<? extends Persistent> objects) {
		if (objects == null || objects.size() == 0) {
			return expFalse();
		}

		return matchAnyExp(objects.toArray(new Persistent[objects.size()]));
	}

	/**
	 * Creates an expression that matches any of the objects contained in the
	 * <code>objects</code> array
	 */
	public static Expression matchAnyExp(Persistent... objects) {
		if (objects == null || objects.length == 0) {
			return expFalse();
		}

		List<Expression> pairs = new ArrayList<Expression>(objects.length);

		for (Persistent object : objects) {
			pairs.add(matchExp(object));
		}

		return joinExp(Expression.OR, pairs);
	}

	/**
	 * @since 3.2
	 */
	public static Expression and(Collection<Expression> expressions) {
		return joinExp(Expression.AND, expressions);
	}

	/**
	 * @since 3.2
	 */
	public static Expression and(Expression... expressions) {
		return join(Expression.AND, expressions);
	}

	/**
	 * @since 3.2
	 */
	public static Expression or(Collection<Expression> expressions) {
		return joinExp(Expression.OR, expressions);
	}

	/**
	 * @since 3.2
	 */
	public static Expression or(Expression... expressions) {
		return join(Expression.OR, expressions);
	}

	/**
	 * Parses string, converting it to Expression and optionally binding
	 * positional parameters. If a string does not represent a semantically
	 * correct expression, an ExpressionException is thrown.
	 * <p>
	 * Binding of parameters by name (as opposed to binding by position) can be
	 * achieved by chaining this call with {@link Expression#params(Map)}.
	 * 
	 * @since 3.2
	 */
	public static Expression exp(String expressionString, Object... parameters) {
		Expression e = fromString(expressionString);

		if (parameters != null && parameters.length > 0) {
			// apply parameters in-place... it is wasteful to clone the
			// expression that hasn't been exposed to the callers
			e.inPlaceParamsArray(parameters);
		}

		return e;
	}

	/**
	 * Parses string, converting it to Expression. If string does not represent
	 * a semantically correct expression, an ExpressionException is thrown.
	 * 
	 * @since 3.2
	 */
	private static Expression fromString(String expressionString) {

		if (expressionString == null) {
			throw new NullPointerException("Null expression string.");
		}

		// optimizing parser buffers per CAY-1667...
		// adding 1 extra char to the buffer size above the String length, as
		// otherwise resizing still occurs at the end of the stream
		int bufferSize = expressionString.length() > PARSE_BUFFER_MAX_SIZE ? PARSE_BUFFER_MAX_SIZE
				: expressionString.length() + 1;
		Reader reader = new StringReader(expressionString);
		JavaCharStream stream = new JavaCharStream(reader, 1, 1, bufferSize);
		ExpressionParserTokenManager tm = new ExpressionParserTokenManager(
				stream);
		ExpressionParser parser = new ExpressionParser(tm);

		try {
			return parser.expression();
		} catch (ParseException ex) {

			// can be null
			String message = ex.getMessage();
			throw new ExpressionException(message != null ? message : "", ex);
		} catch (Throwable th) {
			// can be null
			String message = th.getMessage();

			// another common error is TokenManagerError
			throw new ExpressionException(message != null ? message : "", th);
		}
	}
}
