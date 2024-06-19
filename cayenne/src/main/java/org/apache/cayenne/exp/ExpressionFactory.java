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

package org.apache.cayenne.exp;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.parser.ASTAdd;
import org.apache.cayenne.exp.parser.ASTAll;
import org.apache.cayenne.exp.parser.ASTAnd;
import org.apache.cayenne.exp.parser.ASTAny;
import org.apache.cayenne.exp.parser.ASTBetween;
import org.apache.cayenne.exp.parser.ASTBitwiseAnd;
import org.apache.cayenne.exp.parser.ASTBitwiseLeftShift;
import org.apache.cayenne.exp.parser.ASTBitwiseNot;
import org.apache.cayenne.exp.parser.ASTBitwiseOr;
import org.apache.cayenne.exp.parser.ASTBitwiseRightShift;
import org.apache.cayenne.exp.parser.ASTBitwiseXor;
import org.apache.cayenne.exp.parser.ASTCaseWhen;
import org.apache.cayenne.exp.parser.ASTDbIdPath;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.exp.parser.ASTDivide;
import org.apache.cayenne.exp.parser.ASTElse;
import org.apache.cayenne.exp.parser.ASTEnclosingObject;
import org.apache.cayenne.exp.parser.ASTEqual;
import org.apache.cayenne.exp.parser.ASTExists;
import org.apache.cayenne.exp.parser.ASTFalse;
import org.apache.cayenne.exp.parser.ASTFullObject;
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
import org.apache.cayenne.exp.parser.ASTNotExists;
import org.apache.cayenne.exp.parser.ASTNotIn;
import org.apache.cayenne.exp.parser.ASTNotLike;
import org.apache.cayenne.exp.parser.ASTNotLikeIgnoreCase;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.exp.parser.ASTOr;
import org.apache.cayenne.exp.parser.ASTPath;
import org.apache.cayenne.exp.parser.ASTScalar;
import org.apache.cayenne.exp.parser.ASTSubquery;
import org.apache.cayenne.exp.parser.ASTSubtract;
import org.apache.cayenne.exp.parser.ASTThen;
import org.apache.cayenne.exp.parser.ASTTrue;
import org.apache.cayenne.exp.parser.ASTWhen;
import org.apache.cayenne.exp.parser.ExpressionParser;
import org.apache.cayenne.exp.parser.ExpressionParserTokenManager;
import org.apache.cayenne.exp.parser.JavaCharStream;
import org.apache.cayenne.exp.parser.SimpleNode;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.query.ColumnSelect;
import org.apache.cayenne.query.FluentSelect;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Helper class to build expressions.
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

	private static Constructor<? extends SimpleNode>[] typeLookup;
	private static volatile int autoAliasId;

	private static final int PARSE_BUFFER_MAX_SIZE = 4096;

	static {
		// make sure all types are small integers, then we can use them as indexes in lookup array
		int[] allTypes = new int[] { Expression.AND, Expression.OR, Expression.NOT, Expression.EQUAL_TO,
				Expression.NOT_EQUAL_TO, Expression.LESS_THAN, Expression.GREATER_THAN, Expression.LESS_THAN_EQUAL_TO,
				Expression.GREATER_THAN_EQUAL_TO, Expression.BETWEEN, Expression.IN, Expression.LIKE,
				Expression.LIKE_IGNORE_CASE, Expression.ADD, Expression.SUBTRACT, Expression.MULTIPLY,
				Expression.DIVIDE, Expression.NEGATIVE, Expression.OBJ_PATH, Expression.DB_PATH, Expression.LIST,
				Expression.NOT_BETWEEN, Expression.NOT_IN, Expression.NOT_LIKE, Expression.NOT_LIKE_IGNORE_CASE,
				Expression.TRUE, Expression.FALSE, Expression.BITWISE_NOT, Expression.BITWISE_AND,
				Expression.BITWISE_OR, Expression.BITWISE_XOR, Expression.BITWISE_LEFT_SHIFT,
				Expression.BITWISE_RIGHT_SHIFT };

		int max = 0;
		for (int type : allTypes) {
			// sanity check....
			if (type > 500) {
				throw new RuntimeException("Types values are too big: " + type);
			} else if (type < 0) {
				throw new RuntimeException("Types values are too small: " + type);
			}
			if (type > max) {
				max = type;
			}
		}

		// now we know that if types are used as indexes,
		// they will fit in array "max + 1" long (though gaps are possible)
		@SuppressWarnings("unchecked")
		Constructor<? extends SimpleNode>[] lookupTable = (Constructor<? extends SimpleNode>[]) new Constructor[max + 1];
		typeLookup = lookupTable;

		try {
			typeLookup[Expression.AND] = ASTAnd.class.getDeclaredConstructor();
			typeLookup[Expression.OR] = ASTOr.class.getDeclaredConstructor();
			typeLookup[Expression.BETWEEN] = ASTBetween.class.getDeclaredConstructor();
			typeLookup[Expression.NOT_BETWEEN] = ASTNotBetween.class.getDeclaredConstructor();

			// binary types
			typeLookup[Expression.EQUAL_TO] = ASTEqual.class.getDeclaredConstructor();
			typeLookup[Expression.NOT_EQUAL_TO] = ASTNotEqual.class.getDeclaredConstructor();
			typeLookup[Expression.LESS_THAN] = ASTLess.class.getDeclaredConstructor();
			typeLookup[Expression.GREATER_THAN] = ASTGreater.class.getDeclaredConstructor();
			typeLookup[Expression.LESS_THAN_EQUAL_TO] = ASTLessOrEqual.class.getDeclaredConstructor();
			typeLookup[Expression.GREATER_THAN_EQUAL_TO] = ASTGreaterOrEqual.class.getDeclaredConstructor();
			typeLookup[Expression.IN] = ASTIn.class.getDeclaredConstructor();
			typeLookup[Expression.NOT_IN] = ASTNotIn.class.getDeclaredConstructor();
			typeLookup[Expression.LIKE] = ASTLike.class.getDeclaredConstructor();
			typeLookup[Expression.LIKE_IGNORE_CASE] = ASTLikeIgnoreCase.class.getDeclaredConstructor();
			typeLookup[Expression.NOT_LIKE] = ASTNotLike.class.getDeclaredConstructor();
			typeLookup[Expression.NOT_LIKE_IGNORE_CASE] = ASTNotLikeIgnoreCase.class.getDeclaredConstructor();
			typeLookup[Expression.ADD] = ASTAdd.class.getDeclaredConstructor();
			typeLookup[Expression.SUBTRACT] = ASTSubtract.class.getDeclaredConstructor();
			typeLookup[Expression.MULTIPLY] = ASTMultiply.class.getDeclaredConstructor();
			typeLookup[Expression.DIVIDE] = ASTDivide.class.getDeclaredConstructor();

			typeLookup[Expression.NOT] = ASTNot.class.getDeclaredConstructor();
			typeLookup[Expression.NEGATIVE] = ASTNegate.class.getDeclaredConstructor();
			typeLookup[Expression.OBJ_PATH] = ASTObjPath.class.getDeclaredConstructor();
			typeLookup[Expression.DB_PATH] = ASTDbPath.class.getDeclaredConstructor();
			typeLookup[Expression.LIST] = ASTList.class.getDeclaredConstructor();

			typeLookup[Expression.TRUE] = ASTTrue.class.getDeclaredConstructor();
			typeLookup[Expression.FALSE] = ASTFalse.class.getDeclaredConstructor();

			typeLookup[Expression.BITWISE_NOT] = ASTBitwiseNot.class.getDeclaredConstructor();
			typeLookup[Expression.BITWISE_OR] = ASTBitwiseOr.class.getDeclaredConstructor();
			typeLookup[Expression.BITWISE_AND] = ASTBitwiseAnd.class.getDeclaredConstructor();
			typeLookup[Expression.BITWISE_XOR] = ASTBitwiseXor.class.getDeclaredConstructor();
			typeLookup[Expression.BITWISE_LEFT_SHIFT] = ASTBitwiseLeftShift.class.getDeclaredConstructor();
			typeLookup[Expression.BITWISE_RIGHT_SHIFT] = ASTBitwiseRightShift.class.getDeclaredConstructor();
		} catch (NoSuchMethodException ex) {
			throw new ExpressionException("Wrong expression type found", ex);
		}
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
		try {
			return typeLookup[type].newInstance();
		} catch (Exception ex) {
			throw new ExpressionException("Error creating expression", ex);
		}
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
		List<Expression> pairs = makeDbPathPairs(map, pairType);
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
		List<Expression> pairs = makeDbPathPairs(map, pairType);
		return joinExp(Expression.AND, pairs);
	}

	private static List<Expression> makeDbPathPairs(Map<String, ?> map, int pairType) {
		List<Expression> pairs = new ArrayList<>(map.size());

		for (Map.Entry<String, ?> entry : map.entrySet()) {

			Expression exp = expressionOfType(pairType);
			exp.setOperand(0, new ASTDbPath(entry.getKey()));
			exp.setOperand(1, wrapPathOperand(entry.getValue()));
			pairs.add(exp);
		}

		return pairs;
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
		List<Expression> pairs = makeObjPathPairs(map, pairType);
		return joinExp(Expression.OR, pairs);
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
		List<Expression> pairs = makeObjPathPairs(map, pairType);
		return joinExp(Expression.AND, pairs);
	}

	private static List<Expression> makeObjPathPairs(Map<String, ?> map, int pairType) {
		List<Expression> pairs = new ArrayList<>(map.size());

		for (Map.Entry<String, ?> entry : map.entrySet()) {

			Expression exp = expressionOfType(pairType);
			exp.setOperand(0, new ASTObjPath(entry.getKey()));
			exp.setOperand(1, wrapPathOperand(entry.getValue()));
			pairs.add(exp);
		}

		return pairs;
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
	 * @param path expression
	 * @param values collection to match
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

		Function<String, ASTPath> pathProvider;
		if (path.startsWith(ASTDbPath.DB_PREFIX)) {
			pathProvider = ASTDbPath::new;
			path = path.substring(ASTDbPath.DB_PREFIX.length());
		} else {
			pathProvider = ASTObjPath::new;
		}

		int split = path.indexOf(SPLIT_SEPARATOR);

		List<Expression> matches = new ArrayList<>(values.length);

		if (split >= 0 && split < path.length() - 1) {

			int splitEnd = path.indexOf(Entity.PATH_SEPARATOR, split + 1);

			String beforeSplit = split > 0 ? path.substring(0, split) : "";
			String afterSplit = splitEnd > 0 ? "." + path.substring(splitEnd + 1) : "";
			String aliasBase = "split" + autoAliasId++ + "_";
			String splitChunk = splitEnd > 0 ? path.substring(split + 1, splitEnd) : path.substring(split + 1);

			// fix the path - replace split with dot if it's in the middle, or
			// strip it if it's in the beginning
			path = split == 0 ? path.substring(1) : path.replace(SPLIT_SEPARATOR, '.');

			int i = 0;
			for (Object value : values) {

				String alias = aliasBase + i;
				String aliasedPath = beforeSplit + alias + afterSplit;
				i++;

				ASTPath pathExp = pathProvider.apply(aliasedPath);
				pathExp.setPathAliases(Collections.singletonMap(alias, splitChunk));
				matches.add(new ASTEqual(pathExp, value));
			}
		} else {
			for (Object value : values) {
				matches.add(new ASTEqual(pathProvider.apply(path), value));
			}
		}

		return joinExp(Expression.AND, matches);
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
		return matchExp(new ASTObjPath(pathSpec), value);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#matchExp(String, Object)
	 */
	public static Expression matchExp(Expression exp, Object value) {
		if(!(exp instanceof SimpleNode)) {
			throw new IllegalArgumentException("exp should be instance of SimpleNode");
		}
		return new ASTEqual((SimpleNode)exp, value);
	}

	/**
	 * A convenience method to create an OBJ_PATH "not equal to" expression.
	 */
	public static Expression noMatchExp(String pathSpec, Object value) {
		return noMatchExp(new ASTObjPath(pathSpec), value);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#noMatchExp(String, Object)
	 */
	public static Expression noMatchExp(Expression exp, Object value) {
		if(!(exp instanceof SimpleNode)) {
			throw new IllegalArgumentException("exp should be instance of SimpleNode");
		}
		return new ASTNotEqual((SimpleNode)exp, value);
	}

	/**
	 * A convenience method to create an DBID_PATH "equal to" expression.
	 * @since 4.2
	 */
	public static Expression matchDbIdExp(String pathSpec, Object value) {
		return matchExp(new ASTDbIdPath(pathSpec), value);
	}

	/**
	 * A convenience method to create an DBID_PATH "not equal to" expression.
	 * @since 4.2
	 */
	public static Expression noMatchDbIdExp(String pathSpec, Object value) {
		return noMatchExp(new ASTDbIdPath(pathSpec), value);
	}

	/**
	 * A convenience method to create an OBJ_PATH "less than" expression.
	 */
	public static Expression lessExp(String pathSpec, Object value) {
		return lessExp(new ASTObjPath(pathSpec), value);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#lessExp(String, Object)
	 */
	public static Expression lessExp(Expression exp, Object value) {
		if(!(exp instanceof SimpleNode)) {
			throw new IllegalArgumentException("exp should be instance of SimpleNode");
		}
		return new ASTLess((SimpleNode)exp, value);
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
		return lessOrEqualExp(new ASTObjPath(pathSpec), value);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#lessOrEqualExp(String, Object)
	 */
	public static Expression lessOrEqualExp(Expression exp, Object value) {
		if(!(exp instanceof SimpleNode)) {
			throw new IllegalArgumentException("exp should be instance of SimpleNode");
		}
		return new ASTLessOrEqual((SimpleNode)exp, value);
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
		return greaterExp(new ASTObjPath(pathSpec), value);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#greaterExp(String, Object)
	 */
	public static Expression greaterExp(Expression exp, Object value) {
		if(!(exp instanceof SimpleNode)) {
			throw new IllegalArgumentException("exp should be instance of SimpleNode");
		}
		return new ASTGreater((SimpleNode)exp, value);
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
		return greaterOrEqualExp(new ASTObjPath(pathSpec), value);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#greaterOrEqualExp(String, Object)
	 */
	public static Expression greaterOrEqualExp(Expression exp, Object value) {
		if(!(exp instanceof SimpleNode)) {
			throw new IllegalArgumentException("exp should be instance of SimpleNode");
		}
		return new ASTGreaterOrEqual((SimpleNode)exp, value);
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
		return inExp(new ASTObjPath(pathSpec), values);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#inExp(String, Object[])
	 */
	public static Expression inExp(Expression exp, Object... values) {
		if (values.length == 0) {
			return new ASTFalse();
		}
		if(!(exp instanceof SimpleNode)) {
			throw new IllegalArgumentException("exp should be instance of SimpleNode");
		}
		return new ASTIn((SimpleNode)exp, new ASTList(values));
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
		return inExp(new ASTObjPath(pathSpec), values);
	}

	/**
	 * A convenience shortcut for building IN DBID expression. Return ASTFalse for
	 * empty collection.
	 * @since 4.2
	 */
	public static Expression inDbIdExp(String pathSpec, Object... values) {
		if (values.length == 0) {
			return new ASTFalse();
		}
		return new ASTIn(new ASTDbIdPath(pathSpec), new ASTList(values));
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#inExp(String, Collection)
	 */
	public static Expression inExp(Expression exp, Collection<?> values) {
		if (values.isEmpty()) {
			return new ASTFalse();
		}
		if(!(exp instanceof SimpleNode)) {
			throw new IllegalArgumentException("exp should be instance of SimpleNode");
		}
		return new ASTIn((SimpleNode)exp, new ASTList(values));
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
	 * A convenience shortcut for building IN DBID expression. Return ASTFalse for
	 * empty collection.
	 * @since 4.2
	 */
	public static Expression inDbIdExp(String pathSpec, Collection<?> values) {
		if (values.isEmpty()) {
			return new ASTFalse();
		}
		return new ASTIn(new ASTDbIdPath(pathSpec), new ASTList(values));
	}

	/**
	 * A convenience shortcut for building NOT_IN expression. Return ASTTrue for
	 * empty collection.
	 */
	public static Expression notInExp(String pathSpec, Collection<?> values) {
		return notInExp(new ASTObjPath(pathSpec), values);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#notInExp(String, Collection)
	 */
	public static Expression notInExp(Expression exp, Collection<?> values) {
		if (values.isEmpty()) {
			return new ASTTrue();
		}
		if(!(exp instanceof SimpleNode)) {
			throw new IllegalArgumentException("exp should be instance of SimpleNode");
		}
		return new ASTNotIn((SimpleNode)exp, new ASTList(values));
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
	 * @since 4.2
	 */
	public static Expression notInDbIdExp(String pathSpec, Collection<?> values) {
		if (values.isEmpty()) {
			return new ASTTrue();
		}
		return new ASTNotIn(new ASTDbIdPath(pathSpec), new ASTList(values));
	}

	/**
	 * A convenience shortcut for building NOT_IN expression. Return ASTTrue for
	 * empty collection.
	 * 
	 * @since 1.0.6
	 */
	public static Expression notInExp(String pathSpec, Object... values) {
		return notInExp(new ASTObjPath(pathSpec), values);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#notInExp(String, Object[])
	 */
	public static Expression notInExp(Expression exp, Object... values) {
		if (values.length == 0) {
			return new ASTTrue();
		}
		if(!(exp instanceof SimpleNode)) {
			throw new IllegalArgumentException("exp should be instance of SimpleNode");
		}
		return new ASTNotIn((SimpleNode)exp, new ASTList(values));
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
	 * A convenience shortcut for building NOT_IN expression. Return ASTTrue for
	 * empty collection.
	 *
	 * @since 4.2
	 */
	public static Expression notInDbIdExp(String pathSpec, Object... values) {
		if (values.length == 0) {
			return new ASTTrue();
		}
		return new ASTNotIn(new ASTDbIdPath(pathSpec), new ASTList(values));
	}

	/**
	 * A convenience shortcut for building BETWEEN expressions.
	 */
	public static Expression betweenExp(String pathSpec, Object value1, Object value2) {
		return betweenExp(new ASTObjPath(pathSpec), value1, value2);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#betweenExp(String, Object, Object)
	 */
	public static Expression betweenExp(Expression exp, Object value1, Object value2) {
		if(!(exp instanceof SimpleNode)) {
			throw new IllegalArgumentException("exp should be instance of SimpleNode");
		}
		return new ASTBetween((SimpleNode)exp, value1, value2);
	}

	/**
	 * A convenience shortcut for building BETWEEN expressions.
	 * 
	 * @since 3.0
	 */
	public static Expression betweenDbExp(String pathSpec, Object value1, Object value2) {
		return new ASTBetween(new ASTDbPath(pathSpec), value1, value2);
	}

	/**
	 * A convenience shortcut for building NOT_BETWEEN expressions.
	 */
	public static Expression notBetweenExp(String pathSpec, Object value1, Object value2) {
		return notBetweenExp(new ASTObjPath(pathSpec), value1, value2);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#notBetweenExp(String, Object, Object)
	 */
	public static Expression notBetweenExp(Expression exp, Object value1, Object value2) {
		if(!(exp instanceof SimpleNode)) {
			throw new IllegalArgumentException("exp should be instance of SimpleNode");
		}
		return new ASTNotBetween((SimpleNode)exp, value1, value2);
	}

	/**
	 * A convenience shortcut for building NOT_BETWEEN expressions.
	 * 
	 * @since 3.0
	 */
	public static Expression notBetweenDbExp(String pathSpec, Object value1, Object value2) {
		return new ASTNotBetween(new ASTDbPath(pathSpec), value1, value2);
	}

	/**
	 * A convenience shortcut for building LIKE expression.
	 */
	public static Expression likeExp(String pathSpec, Object value) {
		return likeExpInternal(pathSpec, value, (char) 0);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#likeExp(String, Object)
	 */
	public static Expression likeExp(Expression exp, Object value) {
		return likeExpInternal(exp, value, (char) 0);
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
	public static Expression likeExp(String pathSpec, Object value, char escapeChar) {
		return likeExpInternal(pathSpec, value, escapeChar);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#likeExp(String, Object)
	 */
	public static Expression likeExp(Expression exp, Object value, char escapeChar) {
		return likeExpInternal(exp, value, escapeChar);
	}

	static ASTLike likeExpInternal(String pathSpec, Object value, char escapeChar) {
		return likeExpInternal(new ASTObjPath(pathSpec), value, escapeChar);
	}

	static ASTLike likeExpInternal(Expression expression, Object value, char escapeChar) {
		if(!(expression instanceof SimpleNode)) {
			throw new IllegalArgumentException("exp should be instance of SimpleNode");
		}
		return new ASTLike((SimpleNode) expression, value, escapeChar);
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
	public static Expression likeDbExp(String pathSpec, Object value, char escapeChar) {
		return new ASTLike(new ASTDbPath(pathSpec), value, escapeChar);
	}

	/**
	 * A convenience shortcut for building NOT_LIKE expression.
	 */
	public static Expression notLikeExp(String pathSpec, Object value) {
		return notLikeExp(new ASTObjPath(pathSpec), value);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#notLikeExp(String, Object)
	 */
	public static Expression notLikeExp(Expression exp, Object value) {
		if(!(exp instanceof SimpleNode)) {
			throw new IllegalArgumentException("exp should be instance of SimpleNode");
		}
		return new ASTNotLike((SimpleNode)exp, value);
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
	public static Expression notLikeExp(String pathSpec, Object value, char escapeChar) {
		return notLikeExp(new ASTObjPath(pathSpec), value, escapeChar);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#notLikeExp(String, Object)
	 */
	public static Expression notLikeExp(Expression exp, Object value, char escapeChar) {
		if(!(exp instanceof SimpleNode)) {
			throw new IllegalArgumentException("exp should be instance of SimpleNode");
		}
		return new ASTNotLike((SimpleNode)exp, value, escapeChar);
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
	public static Expression notLikeDbExp(String pathSpec, Object value, char escapeChar) {
		return new ASTNotLike(new ASTDbPath(pathSpec), value, escapeChar);
	}

	/**
	 * A convenience shortcut for building LIKE_IGNORE_CASE expression.
	 */
	public static Expression likeIgnoreCaseExp(String pathSpec, Object value) {
		return likeIgnoreCaseExpInternal(pathSpec, value, (char) 0);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#likeIgnoreCaseExp(String, Object)
	 */
	public static Expression likeIgnoreCaseExp(Expression exp, Object value) {
		return likeIgnoreCaseExp(exp, value, (char) 0);
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
	public static Expression likeIgnoreCaseExp(String pathSpec, Object value, char escapeChar) {
		return likeIgnoreCaseExpInternal(pathSpec, value, escapeChar);
	}

	static ASTLikeIgnoreCase likeIgnoreCaseExpInternal(String pathSpec, Object value, char escapeChar) {
		return likeIgnoreCaseExp(new ASTObjPath(pathSpec), value, escapeChar);
	}

	static ASTLikeIgnoreCase likeIgnoreCaseExp(Expression exp, Object value, char escapeChar) {
		if(!(exp instanceof SimpleNode)) {
			throw new IllegalArgumentException("exp should be instance of SimpleNode");
		}
		return new ASTLikeIgnoreCase((SimpleNode) exp, value, escapeChar);
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
	public static Expression likeIgnoreCaseDbExp(String pathSpec, Object value, char escapeChar) {
		return new ASTLikeIgnoreCase(new ASTDbPath(pathSpec), value, escapeChar);
	}

	/**
	 * A convenience shortcut for building NOT_LIKE_IGNORE_CASE expression.
	 */
	public static Expression notLikeIgnoreCaseExp(String pathSpec, Object value) {
		return notLikeIgnoreCaseExp(new ASTObjPath(pathSpec), value);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#notLikeIgnoreCaseExp(String, Object)
	 */
	public static Expression notLikeIgnoreCaseExp(Expression exp, Object value) {
		if(!(exp instanceof SimpleNode)) {
			throw new IllegalArgumentException("exp should be instance of SimpleNode");
		}
		return new ASTNotLikeIgnoreCase((SimpleNode)exp, value);
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
	public static Expression notLikeIgnoreCaseExp(String pathSpec, Object value, char escapeChar) {
		return notLikeIgnoreCaseExp(new ASTObjPath(pathSpec), value, escapeChar);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#notLikeIgnoreCaseExp(String, Object, char)
	 */
	public static Expression notLikeIgnoreCaseExp(Expression exp, Object value, char escapeChar) {
		if(!(exp instanceof SimpleNode)) {
			throw new IllegalArgumentException("exp should be instance of SimpleNode");
		}
		return new ASTNotLikeIgnoreCase((SimpleNode)exp, value, escapeChar);
	}

	/**
	 * A convenience shortcut for building NOT_LIKE_IGNORE_CASE expression.
	 * 
	 * @since 3.0
	 */
	public static Expression notLikeIgnoreCaseDbExp(String pathSpec, Object value) {
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
	public static Expression notLikeIgnoreCaseDbExp(String pathSpec, Object value, char escapeChar) {
		return new ASTNotLikeIgnoreCase(new ASTDbPath(pathSpec), value, escapeChar);
	}

	/**
	 * @return An expression for a database "LIKE" query with the value
	 *         converted to a pattern matching anywhere in the String.
	 * @since 4.0
	 */
	public static Expression containsExp(String pathSpec, String value) {
		ASTLike like = likeExpInternal(pathSpec, value, (char) 0);
		LikeExpressionHelper.toContains(like);
		return like;
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#containsExp(String, String)
	 */
	public static Expression containsExp(Expression exp, String value) {
		ASTLike like = likeExpInternal(exp, value, (char) 0);
		LikeExpressionHelper.toContains(like);
		return like;
	}

	/**
	 * @return An expression for a database "LIKE" query with the value
	 *         converted to a pattern matching the beginning of the String.
	 * @since 4.0
	 */
	public static Expression startsWithExp(String pathSpec, String value) {
		ASTLike like = likeExpInternal(pathSpec, value, (char) 0);
		LikeExpressionHelper.toStartsWith(like);
		return like;
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#startsWithExp(String, String)
	 */
	public static Expression startsWithExp(Expression exp, String value) {
		ASTLike like = likeExpInternal(exp, value, (char) 0);
		LikeExpressionHelper.toStartsWith(like);
		return like;
	}

	/**
	 * @return An expression for a database "LIKE" query with the value
	 *         converted to a pattern matching the beginning of the String.
	 * @since 4.0
	 */
	public static Expression endsWithExp(String pathSpec, String value) {
		ASTLike like = likeExpInternal(pathSpec, value, (char) 0);
		LikeExpressionHelper.toEndsWith(like);
		return like;
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#endsWithExp(String, String)
	 */
	public static Expression endsWithExp(Expression exp, String value) {
		ASTLike like = likeExpInternal(exp, value, (char) 0);
		LikeExpressionHelper.toEndsWith(like);
		return like;
	}

	/**
	 * Same as {@link #containsExp(String, String)} only using case-insensitive
	 * comparison.
	 * 
	 * @since 4.0
	 */
	public static Expression containsIgnoreCaseExp(String pathSpec, String value) {
		ASTLikeIgnoreCase like = likeIgnoreCaseExpInternal(pathSpec, value, (char) 0);
		LikeExpressionHelper.toContains(like);
		return like;
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#containsIgnoreCaseExp(String, String)
	 */
	public static Expression containsIgnoreCaseExp(Expression exp, String value) {
		ASTLikeIgnoreCase like = likeIgnoreCaseExp(exp, value, (char) 0);
		LikeExpressionHelper.toContains(like);
		return like;
	}

	/**
	 * Same as {@link #startsWithExp(String, String)} only using
	 * case-insensitive comparison.
	 * 
	 * @since 4.0
	 */
	public static Expression startsWithIgnoreCaseExp(String pathSpec, String value) {
		ASTLikeIgnoreCase like = likeIgnoreCaseExpInternal(pathSpec, value, (char) 0);
		LikeExpressionHelper.toStartsWith(like);
		return like;
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#startsWithIgnoreCaseExp(String, String)
	 */
	public static Expression startsWithIgnoreCaseExp(Expression exp, String value) {
		ASTLikeIgnoreCase like = likeIgnoreCaseExp(exp, value, (char) 0);
		LikeExpressionHelper.toStartsWith(like);
		return like;
	}

	/**
	 * Same as {@link #endsWithExp(String, String)} only using case-insensitive
	 * comparison.
	 * 
	 * @since 4.0
	 */
	public static Expression endsWithIgnoreCaseExp(String pathSpec, String value) {
		ASTLikeIgnoreCase like = likeIgnoreCaseExpInternal(pathSpec, value, (char) 0);
		LikeExpressionHelper.toEndsWith(like);
		return like;
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#endsWithIgnoreCaseExp(String, String)
	 */
	public static Expression endsWithIgnoreCaseExp(Expression exp, String value) {
		ASTLikeIgnoreCase like = likeIgnoreCaseExp(exp, value, (char) 0);
		LikeExpressionHelper.toEndsWith(like);
		return like;
	}

	/**
	 * @param pathSpec a String "obj:" path.
	 * @return a new "obj:" path expression for the specified String path.
	 * @since 4.0
	 */
	public static Expression pathExp(String pathSpec) {
		return new ASTObjPath(pathSpec);
	}

	/**
	 * @param path a path value.
	 * @return a new "obj:" path expression for the specified path.
	 * @since 5.0
	 */
	public static Expression pathExp(CayennePath path) {
		return new ASTObjPath(path);
	}

	/**
	 * @param pathSpec a String db: path.
	 * @return a new "db:" path expression for the specified String path.
	 *
	 * @since 4.0
	 */
	public static Expression dbPathExp(String pathSpec) {
		return new ASTDbPath(pathSpec);
	}

	/**
	 * @param path a path value
	 * @return a new "db:" path expression for the specified path.
	 *
	 * @since 5.0
	 */
	public static Expression dbPathExp(CayennePath path) {
		return new ASTDbPath(path);
	}

	/**
	 * @param pathSpec a String "dbid:" path
	 * @return a new "dbid:" path expression for the specified String path
	 *
	 * @since 4.2
	 */
	public static Expression dbIdPathExp(String pathSpec) {
		return new ASTDbIdPath(pathSpec);
	}

	/**
	 * @param pathSpec a "dbid:" path value
	 * @return a new "dbid:" path expression for the specified path
	 *
	 * @since 5.0
	 */
	public static Expression dbIdPathExp(CayennePath pathSpec) {
		return new ASTDbIdPath(pathSpec);
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
	public static Expression joinExp(int type, Collection<Expression> expressions) {
		int len = expressions.size();
		if (len == 0) {
			return null;
		}

		return joinExp(type, expressions.toArray(new Expression[len]));
	}

	/**
	 * Joins all expressions, making a single expression. <code>type</code> is
	 * used as an expression type for expressions joining each one of the items
	 * in the array. <code>type</code> must be binary expression type.
	 * <p>
	 * For example, if type is Expression.AND, resulting expression would match
	 * all expressions in the list. If type is Expression.OR, resulting
	 * expression would match any of the expressions.
	 * </p>
	 * @since 4.1
	 */
	public static Expression joinExp(int type, Expression... expressions) {

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
		return matchAllDbExp(object.getObjectId().getIdSnapshot(), Expression.EQUAL_TO);
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

		List<Expression> pairs = new ArrayList<>(objects.length);

		for (Persistent object : objects) {
			pairs.add(matchExp(object));
		}

		return joinExp(Expression.OR, pairs);
	}

	public static Expression fullObjectExp() {
		return new ASTFullObject();
	}

	public static Expression fullObjectExp(Expression exp) {
		return new ASTFullObject(exp);
	}

	/**
	 * @since 4.2
	 */
	public static Expression enclosingObjectExp(Expression exp) {
		return new ASTEnclosingObject(exp);
	}

	/**
	 * @since 4.0
	 */
	public static Expression and(Collection<Expression> expressions) {
		return joinExp(Expression.AND, expressions);
	}

	/**
	 * @since 4.0
	 */
	public static Expression and(Expression... expressions) {
		return joinExp(Expression.AND, expressions);
	}

	/**
	 * @since 4.0
	 */
	public static Expression or(Collection<Expression> expressions) {
		return joinExp(Expression.OR, expressions);
	}

	/**
	 * @since 4.0
	 */
	public static Expression or(Expression... expressions) {
		return joinExp(Expression.OR, expressions);
	}

	/**
	 * Parses string, converting it to Expression and optionally binding
	 * positional parameters. If a string does not represent a semantically
	 * correct expression, an ExpressionException is thrown.
	 * <p>
	 * Binding of parameters by name (as opposed to binding by position) can be
	 * achieved by chaining this call with {@link Expression#params(Map)}.
	 * 
	 * @since 4.0
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
	 * Wrap value into ASTScalar
	 * @since 4.0
	 */
	public static Expression wrapScalarValue(Object value) {
		return new ASTScalar(value);
	}

	/**
	 * Parses string, converting it to Expression. If string does not represent
	 * a semantically correct expression, an ExpressionException is thrown.
	 * 
	 * @since 4.0
	 */
	private static Expression fromString(String expressionString) {

		if (expressionString == null) {
			throw new NullPointerException("Null expression string.");
		}

		// optimizing parser buffers per CAY-1667...
		// adding 1 extra char to the buffer size above the String length, as
		// otherwise resizing still occurs at the end of the stream
		int bufferSize = expressionString.length() > PARSE_BUFFER_MAX_SIZE ?
				PARSE_BUFFER_MAX_SIZE : expressionString.length() + 1;
		Reader reader = new StringReader(expressionString);
		JavaCharStream stream = new JavaCharStream(reader, 1, 1, bufferSize);
		ExpressionParserTokenManager tm = new ExpressionParserTokenManager(stream);
		ExpressionParser parser = new ExpressionParser(tm);

		try {
			return parser.expression();
		} catch (Throwable th) {
			String message = th.getMessage();
			throw new ExpressionException("%s", th, message != null ? message : "");
		}
	}

	/**
	 * @param subQuery {@link org.apache.cayenne.query.ObjectSelect} or {@link ColumnSelect}
	 * @since 4.2
	 */
	public static Expression exists(FluentSelect<?, ?> subQuery) {
		return new ASTExists(new ASTSubquery(subQuery));
	}

	/**
	 * @param subQuery {@link org.apache.cayenne.query.ObjectSelect} or {@link ColumnSelect}
	 * @since 4.2
	 */
	public static Expression notExists(FluentSelect<?, ?> subQuery) {
		return new ASTNotExists(new ASTSubquery(subQuery));
	}

	/**
	 * @since 4.2
	 */
	public static Expression inExp(Expression exp, ColumnSelect<?> subQuery) {
		if(!(exp instanceof SimpleNode)) {
			throw new IllegalArgumentException("exp should be instance of SimpleNode");
		}
		return new ASTIn((SimpleNode)exp, new ASTSubquery(subQuery));
	}

	/**
	 * @since 4.2
	 */
	public static Expression notInExp(Expression exp, ColumnSelect<?> subQuery) {
		if(!(exp instanceof SimpleNode)) {
			throw new IllegalArgumentException("exp should be instance of SimpleNode");
		}
		return new ASTNotIn((SimpleNode)exp, new ASTSubquery(subQuery));
	}

	/**
	 * @since 5.0
	 */
	public static Expression all(ColumnSelect<?> subquery) {
		return new ASTAll(new ASTSubquery(subquery));
	}

	/**
	 * @since 5.0
	 */
	public static Expression any(ColumnSelect<?> subquery) {
		return new ASTAny(new ASTSubquery(subquery));
	}

	/**
	 * @since 5.0
	 */
	public static Expression caseWhen(List<Expression> whenExp, List<Expression> thenExp) {
		return caseWhen(whenExp, thenExp, null);
	}

	/**
	 * @since 5.0
	 */
	public static Expression caseWhen(List<Expression> whenExp, List<Expression> thenExp, Expression caseDefault) {
		if (whenExp.size() != thenExp.size()) {
			throw new ExpressionException("Each member in the \"When\"-\"Then\" pairs must be defined");
		}
		List<Expression> expressions = new ArrayList<>();
		for (int i = 0; i < whenExp.size(); i++) {
			expressions.add(new ASTWhen(whenExp.get(i)));
			expressions.add(new ASTThen(thenExp.get(i)));
		}
		boolean hasDefault = false;
		if (caseDefault != null) {
			expressions.add(new ASTElse(caseDefault));
			hasDefault = true;
		}
		return new ASTCaseWhen(hasDefault, expressions.toArray(new Expression[0]));
	}
}
