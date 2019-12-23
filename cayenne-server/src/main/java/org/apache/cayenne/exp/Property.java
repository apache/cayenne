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

import java.util.Collection;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.property.BaseProperty;
import org.apache.cayenne.exp.property.ComparableProperty;
import org.apache.cayenne.exp.property.RelationshipProperty;

/**
 * <p>
 * A property in a {@link org.apache.cayenne.DataObject}.
 * </p>
 * <p>
 * Used to construct Expressions quickly and with type-safety, and to construct Orderings.
 * </p>
 * <p>
 * Instances of this class are immutable.
 * </p>
 * <p>
 * Must be created via factory methods {@link Property#create(String, Class) Property.create(..)}
 * </p>
 *
 * @param <E> The type this property returns.
 *
 * @see Property#create(String, Class)
 * @see Property#create(Expression, Class)
 * @see Property#create(String, Expression, Class)
 *
 * @see org.apache.cayenne.exp.property.PropertyFactory
 *
 * @since 4.0
 * @deprecated since 4.2 in favour of type-specific set of properties, see {@link org.apache.cayenne.exp.property.PropertyFactory}
 * and {@link org.apache.cayenne.exp.property} package.
 */
@Deprecated
public class Property<E> extends BaseProperty<E> implements ComparableProperty<E>, RelationshipProperty<E> {

    /**
     * <p>Property that can be used in COUNT(*) queries</p>
     * <p>
     * <pre>{@code
     * List<Object[]> result = ObjectSelect
     *         .columnQuery(Artist.class, Property.COUNT, Artist.ARTIST_NAME)
     *         .having(Property.COUNT.gt(1L))
     *         .select(context);
     * }</pre>
     * </p>
     * @deprecated since 4.2 use {@link org.apache.cayenne.exp.property.PropertyFactory#COUNT}
     */
    @Deprecated
    public static final Property<Long> COUNT = Property.create(FunctionExpressionFactory.countExp(), Long.class);

    /**
     * Constructs a new property with the given name and type.
     *
     * @param name of the property (usually it's obj path)
     * @param type of the property
     *
     * @see Property#create(String, Class)
     */
    protected Property(final String name, final Class<E> type) {
        super(name, null, type);
    }

    /**
     * Constructs a new property with the given name and expression
     *
     * @param name of the property (will be used as alias for the expression)
     * @param expression expression for property
     * @param type of the property
     *
     * @see Property#create(String, Expression, Class)
     */
    protected Property(final String name, final Expression expression, final Class<E> type) {
        super(name, expression, type);
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
        return ExpressionFactory.likeExp(path(), pattern);
    }

    /**
     * @param pattern    a properly escaped pattern matching property value. Pattern
     *                   may include "_" and "%" wildcard symbols to match any single
     *                   character or a sequence of characters.
     * @param escapeChar an escape character used in the pattern to escape "%" and "_".
     * @return An expression for a Database "LIKE" query.
     */
    public Expression like(String pattern, char escapeChar) {
        return ExpressionFactory.likeExp(path(), pattern, escapeChar);
    }

    /**
     * @return An expression for a case insensitive "LIKE" query.
     */
    public Expression likeIgnoreCase(String pattern) {
        return ExpressionFactory.likeIgnoreCaseExp(path(), pattern);
    }

    /**
     * @return An expression for a Database "NOT LIKE" query.
     */
    public Expression nlike(String value) {
        return ExpressionFactory.notLikeExp(path(), value);
    }

    /**
     * @return An expression for a case insensitive "NOT LIKE" query.
     */
    public Expression nlikeIgnoreCase(String value) {
        return ExpressionFactory.notLikeIgnoreCaseExp(path(), value);
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
        return ExpressionFactory.containsExp(path(), substring);
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
        return ExpressionFactory.startsWithExp(path(), value);
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
        return ExpressionFactory.endsWithExp(path(), value);
    }

    /**
     * Same as {@link #contains(String)}, only using case-insensitive
     * comparison.
     */
    public Expression containsIgnoreCase(String value) {
        return ExpressionFactory.containsIgnoreCaseExp(path(), value);
    }

    /**
     * Same as {@link #startsWith(String)}, only using case-insensitive
     * comparison.
     */
    public Expression startsWithIgnoreCase(String value) {
        return ExpressionFactory.startsWithIgnoreCaseExp(path(), value);
    }

    /**
     * Same as {@link #endsWith(String)}, only using case-insensitive
     * comparison.
     */
    public Expression endsWithIgnoreCase(String value) {
        return ExpressionFactory.endsWithIgnoreCaseExp(path(), value);
    }

    /**
     * @see FunctionExpressionFactory#lengthExp(Expression)
     */
    public Property<Integer> length() {
        return create(FunctionExpressionFactory.lengthExp(path()), Integer.class);
    }

    /**
     * @see FunctionExpressionFactory#locateExp(String, Expression)
     */
    public Property<Integer> locate(String string) {
        return create(FunctionExpressionFactory.locateExp(ExpressionFactory.wrapScalarValue(string), path()), Integer.class);
    }

    /**
     * @see FunctionExpressionFactory#locateExp(Expression, Expression)
     */
    public Property<Integer> locate(Property<? extends String> property) {
        return create(FunctionExpressionFactory.locateExp(property.path(), path()), Integer.class);
    }

    /**
     * @see FunctionExpressionFactory#trimExp(Expression)
     */
    public Property<String> trim() {
        return create(FunctionExpressionFactory.trimExp(path()), String.class);
    }

    /**
     * @see FunctionExpressionFactory#upperExp(Expression)
     */
    public Property<String> upper() {
        return create(FunctionExpressionFactory.upperExp(path()), String.class);
    }

    /**
     * @see FunctionExpressionFactory#lowerExp(Expression)
     */
    public Property<String> lower() {
        return create(FunctionExpressionFactory.lowerExp(path()), String.class);
    }

    /**
     * <p>Arguments will be converted as follows:
     * <ul>
     *      <li>if argument is a {@link Property} than its expression will be used</li>
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
    public Property<String> concat(Object... args) {
        Expression[] exp = new Expression[args.length + 1];
        int i = 0;
        exp[i++] = path();
        for(Object arg : args) {
            if(arg instanceof org.apache.cayenne.exp.property.Property) {
                exp[i++] = ((org.apache.cayenne.exp.property.Property<?>) arg).getExpression();
            } else if(arg instanceof Expression) {
                exp[i++] = (Expression) arg;
            } else if(arg != null) {
                exp[i++] = ExpressionFactory.wrapScalarValue(arg.toString());
            }
        }
        return create(FunctionExpressionFactory.concatExp(exp), String.class);
    }

    /**
     * @see FunctionExpressionFactory#substringExp(Expression, int, int)
     */
    public Property<String> substring(int offset, int length) {
        return create(FunctionExpressionFactory.substringExp(path(), offset, length), String.class);
    }

    // TODO: end of StringProperty related methods

    // TODO: start of NumericProperty related methods

    /**
     * @see FunctionExpressionFactory#avgExp(Expression)
     */
    public Property<E> avg() {
        return create(FunctionExpressionFactory.avgExp(path()), getType());
    }

    /**
     * @see FunctionExpressionFactory#sumExp(Expression)
     */
    public Property<E> sum() {
        return create(FunctionExpressionFactory.sumExp(path()), getType());
    }

    /**
     * @see FunctionExpressionFactory#modExp(Expression, Number)
     */
    public Property<E> mod(Number number) {
        return create(FunctionExpressionFactory.modExp(path(), number), getType());
    }

    /**
     * @see FunctionExpressionFactory#absExp(Expression)
     */
    public Property<E> abs() {
        return create(FunctionExpressionFactory.absExp(path()), getType());
    }

    /**
     * @see FunctionExpressionFactory#sqrtExp(Expression)
     */
    public Property<E> sqrt() {
        return create(FunctionExpressionFactory.sqrtExp(path()), getType());
    }

    // TODO: end of NumericProperty related methods

    /**
     * <p>Create new "flat" property for toMany relationship.</p>
     * <p>
     *     Example:
     *     <pre>{@code
     *     List<Object[]> result = ObjectSelect
     *          .columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.PAINTING_ARRAY.flat(Painting.class))
     *          .select(context);
     *     }</pre>
     * </p>
     */
    public <T extends Persistent> Property<T> flat(Class<T> tClass) {
        if(!Collection.class.isAssignableFrom(type) && !Map.class.isAssignableFrom(type)) {
            throw new CayenneRuntimeException("Can use flat() function only on Property mapped on toMany relationship.");
        }
        return create(ExpressionFactory.fullObjectExp(path()), tClass);
    }

    /**
     * Creates alias with different name for this property
     */
    @Override
    public Property<E> alias(String alias) {
        return new Property<>(alias, this.getExpression(), this.getType());
    }

    @Override
    public Property<E> outer() {
        return getName().endsWith("+")
                ? this
                : Property.create(getName() + "+", getType());
    }

    public <T> Property<T> dot(Property<T> property) {
        return Property.create(getName() + "." + property.getName(), property.getType());
    }

    /**
     * Constructs a property path by appending the argument to the existing property separated by a dot.
     *
     * @return a newly created Property object.
     */
    @Override
    public Property<Object> dot(String property) {
        return Property.create(getName() + "." + property, null);
    }

    // TODO: this method has incompatible return type in BaseProperty
//    /**
//     * @see FunctionExpressionFactory#countExp(Expression)
//     */
//    @Override
//    public NumericProperty<Long> count() {
//        return Property.create(FunctionExpressionFactory.countExp(getExpression()), Long.class);
//    }

    /**
     * @see FunctionExpressionFactory#maxExp(Expression)
     */
    @Override
    public Property<E> max() {
        return Property.create(FunctionExpressionFactory.maxExp(path()), getType());
    }

    /**
     * @see FunctionExpressionFactory#minExp(Expression)
     */
    @Override
    public Property<E> min() {
        return Property.create(FunctionExpressionFactory.minExp(path()), getType());
    }

    /**
     * Creates property with name and type
     * @see Property#create(Expression, Class)
     * @see Property#create(String, Expression, Class)
     */
    @SuppressWarnings("unchecked")
    public static <T> Property<T> create(String name, Class<? super T> type) {
        return (Property<T>)new Property<>(name, type);
    }

    /**
     * Creates property with expression and type
     * @see Property#create(String, Class)
     * @see Property#create(String, Expression, Class)
     */
    @SuppressWarnings("unchecked")
    public static <T> Property<T> create(Expression expression, Class<? super T> type) {
        return (Property<T>)new Property<>(null, expression, type);
    }

    /**
     * Creates property with name, expression and type
     * @see Property#create(String, Class)
     * @see Property#create(Expression, Class)
     */
    @SuppressWarnings("unchecked")
    public static <T> Property<T> create(String name, Expression expression, Class<? super T> type) {
        return (Property<T>)new Property<>(name, expression, type);
    }

    /**
     * <p>
     * Creates "self" Property for persistent class.
     * This property can be used to select full object along with some of it properties (or
     * properties that can be resolved against query root)
     * </p>
     * <p>
     *     Here is sample code, that will select all Artists and count of their Paintings:
     *     <pre>{@code
     *     Property<Artist> artistFull = Property.createSelf(Artist.class);
     *     List<Object[]> result = ObjectSelect
     *          .columnQuery(Artist.class, artistFull, Artist.PAINTING_ARRAY.count())
     *          .select(context);
     *     }
     *     </pre>
     * </p>
     */
    @SuppressWarnings("unchecked")
    public static <T extends Persistent> Property<T> createSelf(Class<? super T> type) {
        return (Property<T>)new Property<>(null, ExpressionFactory.fullObjectExp(), type);
    }

}