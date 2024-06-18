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

package org.apache.cayenne.exp.property;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.FunctionExpressionFactory;
import org.apache.cayenne.exp.path.CayennePath;

/**
 * Property that represents attributes mapped on string types
 * <p>
 * String type is an any type inherited from {@link CharSequence}.
 * <p>
 * Provides basic string functions like {@link #like(String)}, {@link #concat(Object...)}, {@link #upper()}
 * and {@link #contains(String)}}.
 * <p>
 * Example:<pre>{@code
 * ObjectSelect.query(Artist.class)
 *      .where(Artist.FIRST_NAME.trim().concat(Artist.LAST_NAME.trim()).length().gt(30))
 * }</pre>
 *
 * @see org.apache.cayenne.exp.property
 * @since 4.2
 */
public class StringProperty<E extends CharSequence> extends BaseProperty<E> implements ComparableProperty<E> {

    /**
     * Constructs a new property with the given name and expression
     *
     * @param path       of the property (will be used as alias for the expression)
     * @param expression expression for property
     * @param type       of the property
     * @see PropertyFactory#createString(String, Expression, Class)
     */
    protected StringProperty(CayennePath path, Expression expression, Class<E> type) {
        super(path, expression, type);
    }

    /**
     * @param pattern a pattern matching property value. Pattern may include "_" and
     *                "%" wildcard symbols to match any single character or a
     *                sequence of characters. To prevent "_" and "%" from being
     *                treated as wildcards, they need to be escaped and escape char
     *                passed with {@link #like(String, char)} method.
     * @return An expression for a Database "LIKE" query.
     */
    public Expression like(String pattern) {
        return ExpressionFactory.likeExp(getExpression(), pattern);
    }

    /**
     * @param pattern a pattern matching property value. Pattern may include "_" and
     *                "%" wildcard symbols to match any single character or a
     *                sequence of characters.
     * @return An expression for a Database "LIKE" query.
     */
    public Expression like(StringProperty<?> pattern) {
        return ExpressionFactory.likeExp(getExpression(), pattern.getExpression());
    }

    /**
     * @param pattern    a properly escaped pattern matching property value. Pattern
     *                   may include "_" and "%" wildcard symbols to match any single
     *                   character or a sequence of characters.
     * @param escapeChar an escape character used in the pattern to escape "%" and "_".
     * @return An expression for a Database "LIKE" query.
     */
    public Expression like(String pattern, char escapeChar) {
        return ExpressionFactory.likeExp(getExpression(), pattern, escapeChar);
    }

    /**
     * @return An expression for a case insensitive "LIKE" query.
     */
    public Expression likeIgnoreCase(String pattern) {
        return ExpressionFactory.likeIgnoreCaseExp(getExpression(), pattern);
    }

    /**
     * @return An expression for a case insensitive "LIKE" query.
     */
    public Expression likeIgnoreCase(StringProperty<?> pattern) {
        return ExpressionFactory.likeIgnoreCaseExp(getExpression(), pattern.getExpression());
    }

    /**
     * @return An expression for a Database "NOT LIKE" query.
     */
    public Expression nlike(String value) {
        return ExpressionFactory.notLikeExp(getExpression(), value);
    }

    /**
     * @return An expression for a Database "NOT LIKE" query.
     */
    public Expression nlike(StringProperty<?> value) {
        return ExpressionFactory.notLikeExp(getExpression(), value.getExpression());
    }

    /**
     * @return An expression for a case insensitive "NOT LIKE" query.
     */
    public Expression nlikeIgnoreCase(String value) {
        return ExpressionFactory.notLikeIgnoreCaseExp(getExpression(), value);
    }

    /**
     * @return An expression for a case insensitive "NOT LIKE" query.
     */
    public Expression nlikeIgnoreCase(StringProperty<?> value) {
        return ExpressionFactory.notLikeIgnoreCaseExp(getExpression(), value.getExpression());
    }

    /**
     * Creates an expression for a database "LIKE" query with the value converted to a pattern matching anywhere in the
     * String.
     *
     * @param substring a String to match against property value. "_" and "%" symbols
     *                  are NOT treated as wildcards and are escaped when converted to
     *                  a LIKE expression.
     * @return a newly created expression.
     */
    public Expression contains(String substring) {
        return ExpressionFactory.containsExp(getExpression(), substring);
    }

    /**
     * Creates an expression for a database "LIKE" query with the value converted to a pattern matching the beginning of
     * a String.
     *
     * @param value a String to match against property value. "_" and "%" symbols
     *              are NOT treated as wildcards and are escaped when converted to
     *              a LIKE expression.
     * @return a newly created expression.
     */
    public Expression startsWith(String value) {
        return ExpressionFactory.startsWithExp(getExpression(), value);
    }

    /**
     * Creates an expression for a database "LIKE" query with the value
     * converted to a pattern matching the tail of a String.
     *
     * @param value a String to match against property value. "_" and "%" symbols
     *              are NOT treated as wildcards and are escaped when converted to
     *              a LIKE expression.
     * @return a newly created expression.
     */
    public Expression endsWith(String value) {
        return ExpressionFactory.endsWithExp(getExpression(), value);
    }

    /**
     * Same as {@link #contains(String)}, only using case-insensitive
     * comparison.
     */
    public Expression containsIgnoreCase(String value) {
        return ExpressionFactory.containsIgnoreCaseExp(getExpression(), value);
    }

    /**
     * Same as {@link #startsWith(String)}, only using case-insensitive
     * comparison.
     */
    public Expression startsWithIgnoreCase(String value) {
        return ExpressionFactory.startsWithIgnoreCaseExp(getExpression(), value);
    }

    /**
     * Same as {@link #endsWith(String)}, only using case-insensitive
     * comparison.
     */
    public Expression endsWithIgnoreCase(String value) {
        return ExpressionFactory.endsWithIgnoreCaseExp(getExpression(), value);
    }

    /**
     * @see FunctionExpressionFactory#lengthExp(Expression)
     */
    public NumericProperty<Integer> length() {
        return PropertyFactory.createNumeric(
                FunctionExpressionFactory.lengthExp(getExpression()),
                Integer.class
        );
    }

    /**
     * @see FunctionExpressionFactory#locateExp(String, Expression)
     */
    public NumericProperty<Integer> locate(String string) {
        return PropertyFactory.createNumeric(
                FunctionExpressionFactory.locateExp(ExpressionFactory.wrapScalarValue(string), getExpression()),
                Integer.class
        );
    }

    /**
     * @see FunctionExpressionFactory#locateExp(Expression, Expression)
     */
    public NumericProperty<Integer> locate(StringProperty<? extends String> property) {
        return PropertyFactory.createNumeric(
                FunctionExpressionFactory.locateExp(property.getExpression(), getExpression()),
                Integer.class
        );
    }

    /**
     * @see FunctionExpressionFactory#trimExp(Expression)
     */
    public StringProperty<String> trim() {
        return PropertyFactory.createString(FunctionExpressionFactory.trimExp(getExpression()), String.class);
    }

    /**
     * @see FunctionExpressionFactory#upperExp(Expression)
     */
    public StringProperty<String> upper() {
        return PropertyFactory.createString(FunctionExpressionFactory.upperExp(getExpression()), String.class);
    }

    /**
     * @see FunctionExpressionFactory#lowerExp(Expression)
     */
    public StringProperty<String> lower() {
        return PropertyFactory.createString(FunctionExpressionFactory.lowerExp(getExpression()), String.class);
    }

    /**
     * <p>Arguments will be converted as follows:
     * <ul>
     *      <li>if argument is a {@link BaseProperty} than its expression will be used</li>
     *      <li>if argument is a {@link Expression} than it will be used as is </li>
     *      <li>all other values will be converted to String</li>
     * </ul>
     * </p>
     * <p>
     *     Usage:
     *     <pre>{@code
     *     Property<String> fullName = Artist.FIRST_NAME.concat(" ", Artist.SECOND_NAME);
     *     }</pre>
     * </p>
     * @see FunctionExpressionFactory#concatExp(Expression...)
     */
    public StringProperty<String> concat(Object... args) {
        Expression[] exp = new Expression[args.length + 1];
        int i = 0;
        exp[i++] = getExpression();
        for(Object arg : args) {
            if(arg instanceof BaseProperty) {
                exp[i++] = ((BaseProperty<?>) arg).getExpression();
            } else if(arg instanceof Expression) {
                exp[i++] = (Expression) arg;
            } else if(arg != null) {
                exp[i++] = ExpressionFactory.wrapScalarValue(arg.toString());
            }
        }
        return PropertyFactory.createString(FunctionExpressionFactory.concatExp(exp), String.class);
    }

    /**
     * @see FunctionExpressionFactory#substringExp(Expression, int, int)
     */
    public StringProperty<String> substring(int offset, int length) {
        return PropertyFactory.createString(
                FunctionExpressionFactory.substringExp(getExpression(), offset, length),
                String.class
        );
    }

    /**
     * @see FunctionExpressionFactory#substringExp(Expression, Expression, Expression)
     */
    public StringProperty<String> substring(NumericProperty<?> offset, NumericProperty<?> length) {
        return PropertyFactory.createString(
                FunctionExpressionFactory.substringExp(getExpression(), offset.getExpression(), length.getExpression()),
                String.class
        );
    }

    /**
     * Creates alias with different name for this property
     */
    @Override
    public StringProperty<E> alias(String alias) {
        return PropertyFactory.createString(alias, this.getExpression(), this.getType());
    }

    /**
     * @inheritDoc
     */
    @Override
    public StringProperty<E> enclosing() {
        return PropertyFactory.createString(ExpressionFactory.enclosingObjectExp(getExpression()), getType());
    }

}
