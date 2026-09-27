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
import org.apache.cayenne.ql.QLParser;
import org.apache.cayenne.ql.QLParserTokenManager;
import org.apache.cayenne.ql.JavaCharStream;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.query.ColumnSelect;
import org.apache.cayenne.query.FluentSelect;

import java.io.Reader;
import java.io.StringReader;
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

	private static volatile int autoAliasId;

	private static final int PARSE_BUFFER_MAX_SIZE = 4096;

	/**
	 * Applies a few default rules for adding operands to expressions. In
	 * particular wraps all lists into LIST expressions. Applied only in path
	 * expressions.
	 */
	protected static Object wrapPathOperand(Object op) {
		if (op instanceof Collection<?>) {
			return new ListExp((Collection<?>) op);
		} else if (op instanceof Object[]) {
			return new ListExp(op);
		} else {
			return op;
		}
	}

	/**
	 * Creates an expression that matches any of the key-values pairs in <code>map</code>: an OR of "key = value"
	 * comparisons, each key taken as a DB_PATH.
	 *
	 * @since 5.0
	 */
	public static Expression matchAnyDbExp(Map<String, ?> map) {
		return or(makeDbPathPairs(map));
	}

	/**
	 * Creates an expression that matches all key-values pairs in <code>map</code>: an AND of "key = value"
	 * comparisons, each key taken as a DB_PATH.
	 *
	 * @since 5.0
	 */
	public static Expression matchAllDbExp(Map<String, ?> map) {
		return and(makeDbPathPairs(map));
	}

	private static List<Expression> makeDbPathPairs(Map<String, ?> map) {
		List<Expression> pairs = new ArrayList<>(map.size());
		for (Map.Entry<String, ?> entry : map.entrySet()) {
			pairs.add(new EqualExp(new DbPathExp(entry.getKey()), wrapPathOperand(entry.getValue())));
		}
		return pairs;
	}

	/**
	 * Creates an expression that matches any of the key-values pairs in the <code>map</code>: an OR of
	 * "key = value" comparisons, each key taken as an OBJ_PATH.
	 *
	 * @since 5.0
	 */
	public static Expression matchAnyExp(Map<String, ?> map) {
		return or(makeObjPathPairs(map));
	}

	/**
	 * Creates an expression that matches all key-values pairs in <code>map</code>: an AND of "key = value"
	 * comparisons, each key taken as an OBJ_PATH.
	 *
	 * @since 5.0
	 */
	public static Expression matchAllExp(Map<String, ?> map) {
		return and(makeObjPathPairs(map));
	}

	private static List<Expression> makeObjPathPairs(Map<String, ?> map) {
		List<Expression> pairs = new ArrayList<>(map.size());
		for (Map.Entry<String, ?> entry : map.entrySet()) {
			pairs.add(new EqualExp(new ObjPathExp(entry.getKey()), wrapPathOperand(entry.getValue())));
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
			return new TrueExp();
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
			return new TrueExp();
		}

		Function<String, PathExp> pathProvider;
		if (path.startsWith(DbPathExp.DB_PREFIX)) {
			pathProvider = p -> (PathExp) new DbPathExp(p);
			path = path.substring(DbPathExp.DB_PREFIX.length());
		} else {
			pathProvider = p -> (PathExp) new ObjPathExp(p);
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

				PathExp pathExp = pathProvider.apply(aliasedPath);
				pathExp.setPathAliases(Collections.singletonMap(alias, splitChunk));
				matches.add(new EqualExp(pathExp, value));
			}
		} else {
			for (Object value : values) {
				matches.add(new EqualExp(pathProvider.apply(path), value));
			}
		}

		return and(matches);
	}

	/**
	 * A convenience method to create an DB_PATH "equal to" expression.
	 */
	public static Expression matchDbExp(String pathSpec, Object value) {
		return new EqualExp(new DbPathExp(pathSpec), value);
	}

	/**
	 * A convenience method to create an DB_PATH "not equal to" expression.
	 */
	public static Expression noMatchDbExp(String pathSpec, Object value) {
		return new NotEqualExp(new DbPathExp(pathSpec), value);
	}

	/**
	 * A convenience method to create an OBJ_PATH "equal to" expression.
	 */
	public static Expression matchExp(String pathSpec, Object value) {
		return matchExp(new ObjPathExp(pathSpec), value);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#matchExp(String, Object)
	 */
	public static Expression matchExp(Expression exp, Object value) {
		return new EqualExp(exp, value);
	}

	/**
	 * A convenience method to create an OBJ_PATH "not equal to" expression.
	 */
	public static Expression noMatchExp(String pathSpec, Object value) {
		return noMatchExp(new ObjPathExp(pathSpec), value);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#noMatchExp(String, Object)
	 */
	public static Expression noMatchExp(Expression exp, Object value) {
		return new NotEqualExp(exp, value);
	}

	/**
	 * A convenience method to create an DBID_PATH "equal to" expression.
	 * @since 4.2
	 */
	public static Expression matchDbIdExp(String pathSpec, Object value) {
		return matchExp(new DbIdPathExp(pathSpec), value);
	}

	/**
	 * A convenience method to create an DBID_PATH "not equal to" expression.
	 * @since 4.2
	 */
	public static Expression noMatchDbIdExp(String pathSpec, Object value) {
		return noMatchExp(new DbIdPathExp(pathSpec), value);
	}

	/**
	 * A convenience method to create an OBJ_PATH "less than" expression.
	 */
	public static Expression lessExp(String pathSpec, Object value) {
		return lessExp(new ObjPathExp(pathSpec), value);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#lessExp(String, Object)
	 */
	public static Expression lessExp(Expression exp, Object value) {
		return new LessExp(exp, value);
	}

	/**
	 * A convenience method to create an DB_PATH "less than" expression.
	 * 
	 * @since 3.0
	 */
	public static Expression lessDbExp(String pathSpec, Object value) {
		return new LessExp(new DbPathExp(pathSpec), value);
	}

	/**
	 * A convenience method to create an OBJ_PATH "less than or equal to"
	 * expression.
	 */
	public static Expression lessOrEqualExp(String pathSpec, Object value) {
		return lessOrEqualExp(new ObjPathExp(pathSpec), value);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#lessOrEqualExp(String, Object)
	 */
	public static Expression lessOrEqualExp(Expression exp, Object value) {
		return new LessOrEqualExp(exp, value);
	}

	/**
	 * A convenience method to create an DB_PATH "less than or equal to"
	 * expression.
	 * 
	 * @since 3.0
	 */
	public static Expression lessOrEqualDbExp(String pathSpec, Object value) {
		return new LessOrEqualExp(new DbPathExp(pathSpec), value);
	}

	/**
	 * A convenience method to create an OBJ_PATH "greater than" expression.
	 */
	public static Expression greaterExp(String pathSpec, Object value) {
		return greaterExp(new ObjPathExp(pathSpec), value);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#greaterExp(String, Object)
	 */
	public static Expression greaterExp(Expression exp, Object value) {
		return new GreaterExp(exp, value);
	}

	/**
	 * A convenience method to create an DB_PATH "greater than" expression.
	 * 
	 * @since 3.0
	 */
	public static Expression greaterDbExp(String pathSpec, Object value) {
		return new GreaterExp(new DbPathExp(pathSpec), value);
	}

	/**
	 * A convenience method to create an OBJ_PATH "greater than or equal to"
	 * expression.
	 */
	public static Expression greaterOrEqualExp(String pathSpec, Object value) {
		return greaterOrEqualExp(new ObjPathExp(pathSpec), value);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#greaterOrEqualExp(String, Object)
	 */
	public static Expression greaterOrEqualExp(Expression exp, Object value) {
		return new GreaterOrEqualExp(exp, value);
	}

	/**
	 * A convenience method to create an DB_PATH "greater than or equal to"
	 * expression.
	 * 
	 * @since 3.0
	 */
	public static Expression greaterOrEqualDbExp(String pathSpec, Object value) {
		return new GreaterOrEqualExp(new DbPathExp(pathSpec), value);
	}

	/**
	 * A convenience shortcut for building IN expression. Return FalseExp for
	 * empty collection.
	 */
	public static Expression inExp(String pathSpec, Object... values) {
		return inExp(new ObjPathExp(pathSpec), values);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#inExp(String, Object[])
	 */
	public static Expression inExp(Expression exp, Object... values) {
		if (values.length == 0) {
			return new FalseExp();
		}
		return new InExp(exp, new ListExp(values));
	}

	/**
	 * A convenience shortcut for building IN DB expression. Return FalseExp for
	 * empty collection.
	 */
	public static Expression inDbExp(String pathSpec, Object... values) {
		if (values.length == 0) {
			return new FalseExp();
		}
		return new InExp(new DbPathExp(pathSpec), new ListExp(values));
	}

	/**
	 * A convenience shortcut for building IN expression. Return FalseExp for
	 * empty collection.
	 */
	public static Expression inExp(String pathSpec, Collection<?> values) {
		return inExp(new ObjPathExp(pathSpec), values);
	}

	/**
	 * A convenience shortcut for building IN DBID expression. Return FalseExp for
	 * empty collection.
	 * @since 4.2
	 */
	public static Expression inDbIdExp(String pathSpec, Object... values) {
		if (values.length == 0) {
			return new FalseExp();
		}
		return new InExp(new DbIdPathExp(pathSpec), new ListExp(values));
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#inExp(String, Collection)
	 */
	public static Expression inExp(Expression exp, Collection<?> values) {
		if (values.isEmpty()) {
			return new FalseExp();
		}
		return new InExp(exp, new ListExp(values));
	}

	/**
	 * A convenience shortcut for building IN DB expression. Return FalseExp for
	 * empty collection.
	 */
	public static Expression inDbExp(String pathSpec, Collection<?> values) {
		if (values.isEmpty()) {
			return new FalseExp();
		}
		return new InExp(new DbPathExp(pathSpec), new ListExp(values));
	}

	/**
	 * A convenience shortcut for building IN DBID expression. Return FalseExp for
	 * empty collection.
	 * @since 4.2
	 */
	public static Expression inDbIdExp(String pathSpec, Collection<?> values) {
		if (values.isEmpty()) {
			return new FalseExp();
		}
		return new InExp(new DbIdPathExp(pathSpec), new ListExp(values));
	}

	/**
	 * A convenience shortcut for building NOT_IN expression. Return TrueExp for
	 * empty collection.
	 */
	public static Expression notInExp(String pathSpec, Collection<?> values) {
		return notInExp(new ObjPathExp(pathSpec), values);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#notInExp(String, Collection)
	 */
	public static Expression notInExp(Expression exp, Collection<?> values) {
		if (values.isEmpty()) {
			return new TrueExp();
		}
		return new NotInExp(exp, new ListExp(values));
	}

	/**
	 * A convenience shortcut for building NOT_IN expression. Return TrueExp for
	 * empty collection.
	 * 
	 * @since 3.0
	 */
	public static Expression notInDbExp(String pathSpec, Collection<?> values) {
		if (values.isEmpty()) {
			return new TrueExp();
		}
		return new NotInExp(new DbPathExp(pathSpec), new ListExp(values));
	}

	/**
	 * A convenience shortcut for building NOT_IN expression. Return TrueExp for
	 * empty collection.
	 *
	 * @since 4.2
	 */
	public static Expression notInDbIdExp(String pathSpec, Collection<?> values) {
		if (values.isEmpty()) {
			return new TrueExp();
		}
		return new NotInExp(new DbIdPathExp(pathSpec), new ListExp(values));
	}

	/**
	 * A convenience shortcut for building NOT_IN expression. Return TrueExp for
	 * empty collection.
	 * 
	 * @since 1.0.6
	 */
	public static Expression notInExp(String pathSpec, Object... values) {
		return notInExp(new ObjPathExp(pathSpec), values);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#notInExp(String, Object[])
	 */
	public static Expression notInExp(Expression exp, Object... values) {
		if (values.length == 0) {
			return new TrueExp();
		}
		return new NotInExp(exp, new ListExp(values));
	}

	/**
	 * A convenience shortcut for building NOT_IN expression. Return TrueExp for
	 * empty collection.
	 * 
	 * @since 3.0
	 */
	public static Expression notInDbExp(String pathSpec, Object... values) {
		if (values.length == 0) {
			return new TrueExp();
		}
		return new NotInExp(new DbPathExp(pathSpec), new ListExp(values));
	}

	/**
	 * A convenience shortcut for building NOT_IN expression. Return TrueExp for
	 * empty collection.
	 *
	 * @since 4.2
	 */
	public static Expression notInDbIdExp(String pathSpec, Object... values) {
		if (values.length == 0) {
			return new TrueExp();
		}
		return new NotInExp(new DbIdPathExp(pathSpec), new ListExp(values));
	}

	/**
	 * A convenience shortcut for building BETWEEN expressions.
	 */
	public static Expression betweenExp(String pathSpec, Object value1, Object value2) {
		return betweenExp(new ObjPathExp(pathSpec), value1, value2);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#betweenExp(String, Object, Object)
	 */
	public static Expression betweenExp(Expression exp, Object value1, Object value2) {
		return new BetweenExp(exp, value1, value2);
	}

	/**
	 * A convenience shortcut for building BETWEEN expressions.
	 * 
	 * @since 3.0
	 */
	public static Expression betweenDbExp(String pathSpec, Object value1, Object value2) {
		return new BetweenExp(new DbPathExp(pathSpec), value1, value2);
	}

	/**
	 * A convenience shortcut for building NOT_BETWEEN expressions.
	 */
	public static Expression notBetweenExp(String pathSpec, Object value1, Object value2) {
		return notBetweenExp(new ObjPathExp(pathSpec), value1, value2);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#notBetweenExp(String, Object, Object)
	 */
	public static Expression notBetweenExp(Expression exp, Object value1, Object value2) {
		return new NotBetweenExp(exp, value1, value2);
	}

	/**
	 * A convenience shortcut for building NOT_BETWEEN expressions.
	 * 
	 * @since 3.0
	 */
	public static Expression notBetweenDbExp(String pathSpec, Object value1, Object value2) {
		return new NotBetweenExp(new DbPathExp(pathSpec), value1, value2);
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

	static LikeExp likeExpInternal(String pathSpec, Object value, char escapeChar) {
		return likeExpInternal(new ObjPathExp(pathSpec), value, escapeChar);
	}

	static LikeExp likeExpInternal(Expression expression, Object value, char escapeChar) {
		return withEscapeChar(new LikeExp(expression, value), escapeChar);
	}

	/**
	 * A convenience shortcut for building LIKE DB_PATH expression.
	 * 
	 * @since 3.0
	 */
	public static Expression likeDbExp(String pathSpec, Object value) {
		return new LikeExp(new DbPathExp(pathSpec), value);
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
		return withEscapeChar(new LikeExp(dbPathExp(pathSpec), value), escapeChar);
	}

	/**
	 * A convenience shortcut for building NOT_LIKE expression.
	 */
	public static Expression notLikeExp(String pathSpec, Object value) {
		return notLikeExp(new ObjPathExp(pathSpec), value);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#notLikeExp(String, Object)
	 */
	public static Expression notLikeExp(Expression exp, Object value) {
		return new NotLikeExp(exp, value);
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
		return notLikeExp(new ObjPathExp(pathSpec), value, escapeChar);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#notLikeExp(String, Object)
	 */
	public static Expression notLikeExp(Expression exp, Object value, char escapeChar) {
		return withEscapeChar(new NotLikeExp(exp, value), escapeChar);
	}

	/**
	 * A convenience shortcut for building NOT_LIKE expression.
	 * 
	 * @since 3.0
	 */
	public static Expression notLikeDbExp(String pathSpec, Object value) {
		return new NotLikeExp(new DbPathExp(pathSpec), value);
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
		return withEscapeChar(new NotLikeExp(dbPathExp(pathSpec), value), escapeChar);
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

	static LikeIgnoreCaseExp likeIgnoreCaseExpInternal(String pathSpec, Object value, char escapeChar) {
		return likeIgnoreCaseExp(new ObjPathExp(pathSpec), value, escapeChar);
	}

	static LikeIgnoreCaseExp likeIgnoreCaseExp(Expression exp, Object value, char escapeChar) {
		return withEscapeChar(new LikeIgnoreCaseExp(exp, value), escapeChar);
	}

	/**
	 * A convenience shortcut for building LIKE_IGNORE_CASE expression.
	 * 
	 * @since 3.0
	 */
	public static Expression likeIgnoreCaseDbExp(String pathSpec, Object value) {
		return new LikeIgnoreCaseExp(new DbPathExp(pathSpec), value);
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
		return withEscapeChar(new LikeIgnoreCaseExp(dbPathExp(pathSpec), value), escapeChar);
	}

	/**
	 * A convenience shortcut for building NOT_LIKE_IGNORE_CASE expression.
	 */
	public static Expression notLikeIgnoreCaseExp(String pathSpec, Object value) {
		return notLikeIgnoreCaseExp(new ObjPathExp(pathSpec), value);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#notLikeIgnoreCaseExp(String, Object)
	 */
	public static Expression notLikeIgnoreCaseExp(Expression exp, Object value) {
		return new NotLikeIgnoreCaseExp(exp, value);
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
		return notLikeIgnoreCaseExp(new ObjPathExp(pathSpec), value, escapeChar);
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#notLikeIgnoreCaseExp(String, Object, char)
	 */
	public static Expression notLikeIgnoreCaseExp(Expression exp, Object value, char escapeChar) {
		return withEscapeChar(new NotLikeIgnoreCaseExp(exp, value), escapeChar);
	}

	/**
	 * A convenience shortcut for building NOT_LIKE_IGNORE_CASE expression.
	 * 
	 * @since 3.0
	 */
	public static Expression notLikeIgnoreCaseDbExp(String pathSpec, Object value) {
		return new NotLikeIgnoreCaseExp(new DbPathExp(pathSpec), value);
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
		return withEscapeChar(new NotLikeIgnoreCaseExp(dbPathExp(pathSpec), value), escapeChar);
	}

	/**
	 * @return An expression for a database "LIKE" query with the value
	 *         converted to a pattern matching anywhere in the String.
	 * @since 4.0
	 */
	public static Expression containsExp(String pathSpec, String value) {
		LikeExp like = likeExpInternal(pathSpec, value, (char) 0);
		LikeExpressionHelper.toContains(like);
		return like;
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#containsExp(String, String)
	 */
	public static Expression containsExp(Expression exp, String value) {
		LikeExp like = likeExpInternal(exp, value, (char) 0);
		LikeExpressionHelper.toContains(like);
		return like;
	}

	/**
	 * @return An expression for a database "LIKE" query with the value
	 *         converted to a pattern matching the beginning of the String.
	 * @since 4.0
	 */
	public static Expression startsWithExp(String pathSpec, String value) {
		LikeExp like = likeExpInternal(pathSpec, value, (char) 0);
		LikeExpressionHelper.toStartsWith(like);
		return like;
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#startsWithExp(String, String)
	 */
	public static Expression startsWithExp(Expression exp, String value) {
		LikeExp like = likeExpInternal(exp, value, (char) 0);
		LikeExpressionHelper.toStartsWith(like);
		return like;
	}

	/**
	 * @return An expression for a database "LIKE" query with the value
	 *         converted to a pattern matching the beginning of the String.
	 * @since 4.0
	 */
	public static Expression endsWithExp(String pathSpec, String value) {
		LikeExp like = likeExpInternal(pathSpec, value, (char) 0);
		LikeExpressionHelper.toEndsWith(like);
		return like;
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#endsWithExp(String, String)
	 */
	public static Expression endsWithExp(Expression exp, String value) {
		LikeExp like = likeExpInternal(exp, value, (char) 0);
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
		LikeIgnoreCaseExp like = likeIgnoreCaseExpInternal(pathSpec, value, (char) 0);
		LikeExpressionHelper.toContains(like);
		return like;
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#containsIgnoreCaseExp(String, String)
	 */
	public static Expression containsIgnoreCaseExp(Expression exp, String value) {
		LikeIgnoreCaseExp like = likeIgnoreCaseExp(exp, value, (char) 0);
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
		LikeIgnoreCaseExp like = likeIgnoreCaseExpInternal(pathSpec, value, (char) 0);
		LikeExpressionHelper.toStartsWith(like);
		return like;
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#startsWithIgnoreCaseExp(String, String)
	 */
	public static Expression startsWithIgnoreCaseExp(Expression exp, String value) {
		LikeIgnoreCaseExp like = likeIgnoreCaseExp(exp, value, (char) 0);
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
		LikeIgnoreCaseExp like = likeIgnoreCaseExpInternal(pathSpec, value, (char) 0);
		LikeExpressionHelper.toEndsWith(like);
		return like;
	}

	/**
	 * @since 4.0
	 * @see ExpressionFactory#endsWithIgnoreCaseExp(String, String)
	 */
	public static Expression endsWithIgnoreCaseExp(Expression exp, String value) {
		LikeIgnoreCaseExp like = likeIgnoreCaseExp(exp, value, (char) 0);
		LikeExpressionHelper.toEndsWith(like);
		return like;
	}

	/**
	 * @param pathSpec a String "obj:" path.
	 * @return a new "obj:" path expression for the specified String path.
	 * @since 4.0
	 */
	public static Expression pathExp(String pathSpec) {
		return new ObjPathExp(pathSpec);
	}

	/**
	 * @param path a path value.
	 * @return a new "obj:" path expression for the specified path.
	 * @since 5.0
	 */
	public static Expression pathExp(CayennePath path) {
		return new ObjPathExp(path);
	}

	/**
	 * @param pathSpec a String db: path.
	 * @return a new "db:" path expression for the specified String path.
	 *
	 * @since 4.0
	 */
	public static Expression dbPathExp(String pathSpec) {
		return new DbPathExp(pathSpec);
	}

	/**
	 * @param path a path value
	 * @return a new "db:" path expression for the specified path.
	 *
	 * @since 5.0
	 */
	public static Expression dbPathExp(CayennePath path) {
		return new DbPathExp(path);
	}

	/**
	 * @param pathSpec a String "dbid:" path
	 * @return a new "dbid:" path expression for the specified String path
	 *
	 * @since 4.2
	 */
	public static Expression dbIdPathExp(String pathSpec) {
		return new DbIdPathExp(pathSpec);
	}

	/**
	 * @param pathSpec a "dbid:" path value
	 * @return a new "dbid:" path expression for the specified path
	 *
	 * @since 5.0
	 */
	public static Expression dbIdPathExp(CayennePath pathSpec) {
		return new DbIdPathExp(pathSpec);
	}


	/**
	 * A convenience shortcut for boolean true expression.
	 * 
	 * @since 3.0
	 */
	public static Expression expTrue() {
		return new TrueExp();
	}

	/**
	 * A convenience shortcut for boolean false expression.
	 * 
	 * @since 3.0
	 */
	public static Expression expFalse() {
		return new FalseExp();
	}

	/**
	 * Joins the expressions as the operands of the given (empty) AND or OR node. A single expression is returned as
	 * is, and an empty collection gives null.
	 */
	private static Expression join(AggregateConditionExp join, Collection<Expression> expressions) {
		int len = expressions.size();
		if (len == 0) {
			return null;
		}

		return join(join, expressions.toArray(new Expression[len]));
	}

	private static Expression join(AggregateConditionExp join, Expression... expressions) {

		int len = expressions != null ? expressions.length : 0;
		if (len == 0) {
			return null;
		}

		Expression currentExp = expressions[0];
		if (len == 1) {
			return currentExp;
		}

		for (int i = 0; i < len; i++) {
			join.setOperand(i, expressions[i]);
		}
		return join;
	}

	/**
	 * Creates an expression that matches the primary key of object in
	 * <code>ObjectId</code>'s <code>IdSnapshot</code> for the argument
	 * <code>object</code>.
	 */
	public static Expression matchExp(Persistent object) {
		return matchAllDbExp(object.getObjectId().getIdSnapshot());
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

		return or(pairs);
	}

	public static Expression fullObjectExp() {
		return new FullObjectExp();
	}

	/**
	 * @deprecated a relationship path expression is all that is needed to select related objects as a column
	 */
	@Deprecated(since = "5.0", forRemoval = true)
	public static Expression fullObjectExp(Expression exp) {
		return exp;
	}

	/**
	 * @since 4.2
	 */
	public static Expression enclosingObjectExp(Expression exp) {
		return new EnclosingObjectExp(exp);
	}

	/**
	 * @since 4.0
	 */
	public static Expression and(Collection<Expression> expressions) {
		return join(new AndExp(), expressions);
	}

	/**
	 * @since 4.0
	 */
	public static Expression and(Expression... expressions) {
		return join(new AndExp(), expressions);
	}

	/**
	 * @since 4.0
	 */
	public static Expression or(Collection<Expression> expressions) {
		return join(new OrExp(), expressions);
	}

	/**
	 * @since 4.0
	 */
	public static Expression or(Expression... expressions) {
		return join(new OrExp(), expressions);
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
		// parameters are bound by the parser as it goes, so they also reach the nested selects
		boolean bind = parameters != null && parameters.length > 0;
		return fromString(expressionString, bind ? parameters : null);
	}

	/**
	 * Wraps a value into an expression, for the places that take an expression rather than a value: e.g. a constant
	 * select column, or a whole where clause.
	 *
	 * @since 4.0
	 */
	public static Expression wrapScalarValue(Object value) {
		return new ScalarExp(value);
	}

	/**
	 * Parses string, converting it to Expression. If string does not represent
	 * a semantically correct expression, an ExpressionException is thrown.
	 * 
	 * @since 4.0
	 */
	private static Expression fromString(String expressionString, Object[] parameters) {

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
		QLParserTokenManager tm = new QLParserTokenManager(stream);
		QLParser parser = new QLParser(tm);

		parser.setParameters(parameters);

		try {
			Expression expression = parser.expression();
			parser.checkParametersBound();
			return expression;
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
		return new ExistsExp(new SubqueryExp(subQuery));
	}

	/**
	 * Builds expression representing EXIST subquery over a given path
	 * @param exp expression to use for an EXISTS
	 * @return expression representing exists subquery
	 * @since 5.0
	 */
	public static Expression exists(Expression exp) {
		return new ExistsExp(exp);
	}

	/**
	 * @param subQuery {@link org.apache.cayenne.query.ObjectSelect} or {@link ColumnSelect}
	 * @since 4.2
	 */
	public static Expression notExists(FluentSelect<?, ?> subQuery) {
		return new NotExistsExp(new SubqueryExp(subQuery));
	}

	/**
	 * Builds expression representing NOT EXIST subquery over a given path
	 * @param exp expression to use for an NOT EXISTS
	 * @return expression representing exists subquery
	 * @since 5.0
	 */
	public static Expression notExists(Expression exp) {
		return new NotExistsExp(exp);
	}

	/**
	 * @since 4.2
	 */
	public static Expression inExp(Expression exp, ColumnSelect<?> subQuery) {
		return new InExp(exp, new SubqueryExp(subQuery));
	}

	/**
	 * @since 4.2
	 */
	public static Expression notInExp(Expression exp, ColumnSelect<?> subQuery) {
		return new NotInExp(exp, new SubqueryExp(subQuery));
	}

	/**
	 * @since 5.0
	 */
	public static Expression all(ColumnSelect<?> subquery) {
		return new AllExp(new SubqueryExp(subquery));
	}

	/**
	 * @since 5.0
	 */
	public static Expression any(ColumnSelect<?> subquery) {
		return new AnyExp(new SubqueryExp(subquery));
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
			expressions.add(new WhenExp(whenExp.get(i)));
			expressions.add(new ThenExp(thenExp.get(i)));
		}
		if (caseDefault != null) {
			expressions.add(new ElseExp(caseDefault));
		}
		return new CaseWhenExp(expressions.toArray());
	}

	private static <T extends PatternMatchExp> T withEscapeChar(T exp, char escapeChar) {
		exp.setEscapeChar(escapeChar);
		return exp;
	}
}
