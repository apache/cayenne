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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.parser.ASTPath;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.Orderings;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.reflect.PropertyUtils;

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
 * @since 4.0
 */
public class Property<E> {

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
     */
    public static final Property<Long> COUNT = Property.create(FunctionExpressionFactory.countExp(), Long.class);

    /**
     * Name of the property in the object
     */
    private final String name;

    /**
     * Expression provider for the property
     */
    private final ExpressionProvider expressionProvider;

    /**
     * Explicit type of the property
     */
    private final Class<? super E> type;

    /**
     * Constructs a new property with the given name.
     *
     * @param name name of the property (usually it's obj path)
     *
     * @see Property#create(String, Class)
     * @deprecated use factory method Property.create("propertyName", PropertyType.class)
     */
    @Deprecated
	public Property(final String name) {
        this(name, null);
    }

    /**
     * Constructs a new property with the given name and type.
     *
     * @param name of the property (usually it's obj path)
     * @param type of the property
     *
     * @see Property#create(String, Class)
     */
    protected Property(final String name, final Class<? super E> type) {
        this.name = name;
        expressionProvider = new ExpressionProvider() {
            @Override
            public Expression get() {
                return ExpressionFactory.pathExp(name);
            }
        };
        this.type = type;
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
    protected Property(final String name, final Expression expression, final Class<? super E> type) {
        this.name = name;
        expressionProvider = new ExpressionProvider() {
            @Override
            public Expression get() {
                return expression.deepCopy();
            }
        };
        this.type = type;
    }

    /**
     * @return Name of the property in the object.
     */
    public String getName() {
        return name;
    }

    /**
     * @return alias for this property
     */
    public String getAlias() {
        if(getName() == null) {
            return null;
        }

        // check if default name for Path expression is overridden
        Expression exp = getExpression();
        if(exp instanceof ASTPath) {
            if(((ASTPath) exp).getPath().equals(getName())) {
                return null;
            }
        }

        return getName();
    }

    /**
     * This method returns fresh copy of the expression for each call.
     * @return expression that represents this Property
     */
    public Expression getExpression() {
        return expressionProvider.get();
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : expressionProvider.get().hashCode();
        if(type != null) {
            result = 31 * result + type.hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Property<?> property = (Property<?>) o;
        if (name != null ? !name.equals(property.name) : property.name != null) return false;
        if (name == null && !expressionProvider.get().equals(property.expressionProvider.get())) return false;
        return (type == null ? property.type == null : type.equals(property.type));
    }

    /**
     * Constructs a property path by appending the argument to the existing property separated by a dot.
     *
     * @return a newly created Property object.
     */
    public Property<Object> dot(String property) {
        return create(getName() + "." + property, null);
    }

    /**
     * Constructs a new property path by appending the argument to the existing property separated by a dot.
     *
     * @return a newly created Property object.
     */
    public <T> Property<T> dot(Property<T> property) {
        return create(getName() + "." + property.getName(), property.getType());
    }

    /**
     * Returns a version of this property that represents an OUTER join. It is
     * up to caller to ensure that the property corresponds to a relationship,
     * as "outer" attributes make no sense.
     */
    public Property<E> outer() {
        return isOuter() ? this : create(name + "+", type);
    }

    private boolean isOuter() {
        return name.endsWith("+");
    }

    /**
     * Converts this property to a path expression.
     * This method is equivalent of getExpression() which is preferred as more generic.
     *
     * @return a newly created expression.
     * @see Property#getExpression()
     */
    public Expression path() {
        return getExpression();
    }

    /**
     * @return An expression representing null.
     */
    public Expression isNull() {
        return ExpressionFactory.matchExp(getExpression(), null);
    }

    /**
     * @return An expression representing a non-null value.
     */
    public Expression isNotNull() {
        return ExpressionFactory.matchExp(getExpression(), null).notExp();
    }

    /**
     * @return An expression representing equality to TRUE.
     */
    public Expression isTrue() {
        return ExpressionFactory.matchExp(getExpression(), Boolean.TRUE);
    }

    /**
     * @return An expression representing equality to FALSE.
     */
    public Expression isFalse() {
        return ExpressionFactory.matchExp(getExpression(), Boolean.FALSE);
    }

    /**
     * @return An expression representing equality to a value.
     */
    public Expression eq(E value) {
        return ExpressionFactory.matchExp(getExpression(), value);
    }

    /**
     * @return An expression representing equality between two attributes
     * (columns).
     */
    public Expression eq(Property<?> value) {
        return ExpressionFactory.matchExp(getExpression(), value.getExpression());
    }

    /**
     * @return An expression representing inequality to a value.
     */
    public Expression ne(E value) {
        return ExpressionFactory.noMatchExp(getExpression(), value);
    }

    /**
     * @return An expression representing inequality between two attributes
     * (columns).
     */
    public Expression ne(Property<?> value) {
        return ExpressionFactory.noMatchExp(getExpression(), value.getExpression());
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
     * @return An expression for a Database "NOT LIKE" query.
     */
    public Expression nlike(String value) {
        return ExpressionFactory.notLikeExp(getExpression(), value);
    }

    /**
     * @return An expression for a case insensitive "NOT LIKE" query.
     */
    public Expression nlikeIgnoreCase(String value) {
        return ExpressionFactory.notLikeIgnoreCaseExp(getExpression(), value);
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
     * @param lower The lower bound.
     * @param upper The upper bound.
     * @return An expression checking for objects between a lower and upper
     * bound inclusive
     */
    public Expression between(E lower, E upper) {
        return ExpressionFactory.betweenExp(getExpression(), lower, upper);
    }

    /**
     * @return An expression for finding objects with values in the given set.
     */
    public Expression in(E firstValue, E... moreValues) {

        int moreValuesLength = moreValues != null ? moreValues.length : 0;

        Object[] values = new Object[moreValuesLength + 1];
        values[0] = firstValue;

        if (moreValuesLength > 0) {
            System.arraycopy(moreValues, 0, values, 1, moreValuesLength);
        }

        return ExpressionFactory.inExp(getExpression(), values);
    }

    /**
     * @return An expression for finding objects with values not in the given
     * set.
     */
    public Expression nin(E firstValue, E... moreValues) {

        int moreValuesLength = moreValues != null ? moreValues.length : 0;

        Object[] values = new Object[moreValuesLength + 1];
        values[0] = firstValue;

        if (moreValuesLength > 0) {
            System.arraycopy(moreValues, 0, values, 1, moreValuesLength);
        }

        return ExpressionFactory.notInExp(getExpression(), values);
    }

    /**
     * @return An expression for finding objects with values in the given set.
     */
    public Expression in(Collection<E> values) {
        return ExpressionFactory.inExp(getExpression(), values);
    }

    /**
     * @return An expression for finding objects with values not in the given
     * set.
     */
    public Expression nin(Collection<E> values) {
        return ExpressionFactory.notInExp(getExpression(), values);
    }

    /**
     * @return A greater than Expression.
     */
    public Expression gt(E value) {
        return ExpressionFactory.greaterExp(getExpression(), value);
    }

    /**
     * @return Represents a greater than relationship between two attributes
     * (columns).
     */
    public Expression gt(Property<?> value) {
        return ExpressionFactory.greaterExp(getExpression(), value.getExpression());
    }

    /**
     * @return A greater than or equal to Expression.
     */
    public Expression gte(E value) {
        return ExpressionFactory.greaterOrEqualExp(getExpression(), value);
    }

    /**
     * @return Represents a greater than or equal relationship between two
     * attributes (columns).
     */
    public Expression gte(Property<?> value) {
        return ExpressionFactory.greaterOrEqualExp(getExpression(), value.getExpression());
    }

    /**
     * @return A less than Expression.
     */
    public Expression lt(E value) {
        return ExpressionFactory.lessExp(getExpression(), value);
    }

    /**
     * @return Represents a less than relationship between two attributes
     * (columns).
     */
    public Expression lt(Property<?> value) {
        return ExpressionFactory.lessExp(getExpression(), value.getExpression());
    }

    /**
     * @return A less than or equal to Expression.
     */
    public Expression lte(E value) {
        return ExpressionFactory.lessOrEqualExp(getExpression(), value);
    }

    /**
     * @return Represents a less than or equal relationship between two
     * attributes (columns).
     */
    public Expression lte(Property<?> value) {
        return ExpressionFactory.lessOrEqualExp(getExpression(), value.getExpression());
    }

    /**
     * @return Ascending sort orderings on this property.
     */
    public Ordering asc() {
        return new Ordering(getExpression(), SortOrder.ASCENDING);
    }

    /**
     * @return Ascending sort orderings on this property.
     */
	public Orderings ascs() {
		return new Orderings(asc());
	}

    /**
     * @return Ascending case insensitive sort orderings on this property.
     */
    public Ordering ascInsensitive() {
        return new Ordering(getExpression(), SortOrder.ASCENDING_INSENSITIVE);
    }

    /**
     * @return Ascending case insensitive sort orderings on this property.
     */
	public Orderings ascInsensitives() {
		return new Orderings(ascInsensitive());
	}

    /**
     * @return Descending sort orderings on this property.
     */
    public Ordering desc() {
        return new Ordering(getExpression(), SortOrder.DESCENDING);
    }

    /**
     * @return Descending sort orderings on this property.
     */
    public Orderings descs() {
        return new Orderings(desc());
    }

    /**
     * @return Descending case insensitive sort orderings on this property.
     */
    public Ordering descInsensitive() {
        return new Ordering(getExpression(), SortOrder.DESCENDING_INSENSITIVE);
    }

    /**
     * @return Descending case insensitive sort orderings on this property.
     */
    public Orderings descInsensitives() {
        return new Orderings(descInsensitive());
    }

    /**
     * Returns a prefetch tree that follows this property path, potentially
     * spanning a number of phantom nodes, and having a single leaf with "joint"
     * prefetch semantics.
     */
    public PrefetchTreeNode joint() {
        return PrefetchTreeNode.withPath(getName(), PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
    }

    /**
     * Returns a prefetch tree that follows this property path, potentially
     * spanning a number of phantom nodes, and having a single leaf with
     * "disjoint" prefetch semantics.
     */
    public PrefetchTreeNode disjoint() {
        return PrefetchTreeNode.withPath(getName(), PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);
    }

    /**
     * Returns a prefetch tree that follows this property path, potentially
     * spanning a number of phantom nodes, and having a single leaf with
     * "disjoint by id" prefetch semantics.
     */
    public PrefetchTreeNode disjointById() {
        return PrefetchTreeNode.withPath(getName(), PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS);
    }

    /**
     * Extracts property value from an object using JavaBean-compatible
     * introspection with one addition - a property can be a dot-separated
     * property name path.
     */
    @SuppressWarnings("unchecked")
    public E getFrom(Object bean) {
        return (E) PropertyUtils.getProperty(bean, getName());
    }

    /**
     * Extracts property value from a collection of objects using
     * JavaBean-compatible introspection with one addition - a property can be a
     * dot-separated property name path.
     */
    public List<E> getFromAll(Collection<?> beans) {
        List<E> result = new ArrayList<>(beans.size());
        for (Object bean : beans) {
            result.add(getFrom(bean));
        }
        return result;
    }

    /**
     * Sets a property value in 'obj' using JavaBean-compatible introspection
     * with one addition - a property can be a dot-separated property name path.
     */
    public void setIn(Object bean, E value) {
        PropertyUtils.setProperty(bean, getName(), value);
    }

    /**
     * Sets a property value in a collection of objects using
     * JavaBean-compatible introspection with one addition - a property can be a
     * dot-separated property name path.
     */
    public void setInAll(Collection<?> beans, E value) {
        for (Object bean : beans) {
            setIn(bean, value);
        }
    }

    /**
     * @see FunctionExpressionFactory#countExp(Expression)
     */
    public Property<Long> count() {
        return create(FunctionExpressionFactory.countExp(getExpression()), Long.class);
    }

    /**
     * @see FunctionExpressionFactory#maxExp(Expression)
     */
    public Property<E> max() {
        return create(FunctionExpressionFactory.maxExp(getExpression()), getType());
    }

    /**
     * @see FunctionExpressionFactory#minExp(Expression)
     */
    public Property<E> min() {
        return create(FunctionExpressionFactory.minExp(getExpression()), getType());
    }

    /**
     * @see FunctionExpressionFactory#avgExp(Expression)
     */
    public Property<E> avg() {
        return create(FunctionExpressionFactory.avgExp(getExpression()), getType());
    }

    /**
     * @see FunctionExpressionFactory#sumExp(Expression)
     */
    public Property<E> sum() {
        return create(FunctionExpressionFactory.sumExp(getExpression()), getType());
    }

    /**
     * @see FunctionExpressionFactory#modExp(Expression, Number)
     */
    public Property<E> mod(Number number) {
        return create(FunctionExpressionFactory.modExp(getExpression(), number), getType());
    }

    /**
     * @see FunctionExpressionFactory#absExp(Expression)
     */
    public Property<E> abs() {
        return create(FunctionExpressionFactory.absExp(getExpression()), getType());
    }

    /**
     * @see FunctionExpressionFactory#sqrtExp(Expression)
     */
    public Property<E> sqrt() {
        return create(FunctionExpressionFactory.sqrtExp(getExpression()), getType());
    }

    /**
     * @see FunctionExpressionFactory#lengthExp(Expression)
     */
    public Property<Integer> length() {
        return create(FunctionExpressionFactory.lengthExp(getExpression()), Integer.class);
    }

    /**
     * @see FunctionExpressionFactory#locateExp(String, Expression)
     */
    public Property<Integer> locate(String string) {
        return create(FunctionExpressionFactory.locateExp(ExpressionFactory.wrapScalarValue(string), getExpression()), Integer.class);
    }

    /**
     * @see FunctionExpressionFactory#locateExp(Expression, Expression)
     */
    public Property<Integer> locate(Property<? extends String> property) {
        return create(FunctionExpressionFactory.locateExp(property.getExpression(), getExpression()), Integer.class);
    }

    /**
     * @see FunctionExpressionFactory#trimExp(Expression)
     */
    public Property<String> trim() {
        return create(FunctionExpressionFactory.trimExp(getExpression()), String.class);
    }

    /**
     * @see FunctionExpressionFactory#upperExp(Expression)
     */
    public Property<String> upper() {
        return create(FunctionExpressionFactory.upperExp(getExpression()), String.class);
    }

    /**
     * @see FunctionExpressionFactory#lowerExp(Expression)
     */
    public Property<String> lower() {
        return create(FunctionExpressionFactory.lowerExp(getExpression()), String.class);
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
        exp[i++] = getExpression();
        for(Object arg : args) {
            if(arg instanceof Property) {
                exp[i++] = ((Property) arg).getExpression();
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
        return create(FunctionExpressionFactory.substringExp(getExpression(), offset, length), String.class);
    }

    /**
     * Creates alias with different name for this property
     */
    public Property<E> alias(String alias) {
        return new Property<>(alias, this.getExpression(), this.getType());
    }

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
    public <T extends Persistent> Property<T> flat(Class<? super T> tClass) {
        if(!Collection.class.isAssignableFrom(type) && !Map.class.isAssignableFrom(type)) {
            throw new CayenneRuntimeException("Can use flat() function only on Property mapped on toMany relationship.");
        }
        return create(ExpressionFactory.fullObjectExp(getExpression()), tClass);
    }

    public Class<? super E> getType() {
        return type;
    }

    /**
     * Creates property with name and type
     * @see Property#create(Expression, Class)
     * @see Property#create(String, Expression, Class)
     */
    public static <T> Property<T> create(String name, Class<? super T> type) {
        return new Property<>(name, type);
    }

    /**
     * Creates property with expression and type
     * @see Property#create(String, Class)
     * @see Property#create(String, Expression, Class)
     */
    public static <T> Property<T> create(Expression expression, Class<? super T> type) {
        return new Property<>(null, expression, type);
    }

    /**
     * Creates property with name, expression and type
     * @see Property#create(String, Class)
     * @see Property#create(Expression, Class)
     */
    public static <T> Property<T> create(String name, Expression expression, Class<? super T> type) {
        return new Property<>(name, expression, type);
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
    public static <T extends Persistent> Property<T> createSelf(Class<? super T> type) {
        return new Property<>(null, ExpressionFactory.fullObjectExp(), type);
    }

    /**
     * Since Expression is mutable we need to provide clean Expression for every getter call.
     * So to keep Property itself immutable we use ExpressionProvider.
     * @see Property#Property(String, Class)
     * @see Property#Property(String, Expression, Class)
     */
    private interface ExpressionProvider {
        Expression get();
    }
}