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

import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.reflect.PropertyUtils;

/**
 * <p>
 * A property in a DataObject.
 * </p>
 * <p>
 * <p>
 * Used to construct Expressions quickly and with type-safety, and to construct
 * Orderings
 * </p>
 * <p>
 * <p>
 * Instances of this class are immutable
 * </p>
 *
 * @param <E> The type this property returns.
 * @since 4.0
 */
public class Property<E> {

    /**
     * Name of the property in the object
     */
    private final String name;

    /**
     * Constructs a new property with the given name.
     */
    public Property(String name) {
        this.name = name;
    }

    /**
     * @return Name of the property in the object.
     */
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Property && ((Property<?>) obj).getName().equals(getName());
    }

    /**
     * Constructs a property path by appending the argument to the existing property separated by a dot.
     *
     * @return a newly created Property object.
     */
    public Property<Object> dot(String property) {
        return new Property<Object>(getName() + "." + property);
    }

    /**
     * Constructs a new property path by appending the argument to the existing property separated by a dot.
     *
     * @return a newly created Property object.
     */
    public <T> Property<T> dot(Property<T> property) {
        return new Property<T>(getName() + "." + property.getName());
    }

    /**
     * Returns a version of this property that represents an OUTER join. It is
     * up to caller to ensure that the property corresponds to a relationship,
     * as "outer" attributes make no sense.
     */
    public Property<E> outer() {
        return isOuter() ? this : new Property<E>(name + "+");
    }

    private boolean isOuter() {
        return name.endsWith("+");
    }

    /**
     * Converts this property to a path expression.
     *
     * @return a newly created expression.
     */
    public Expression path() {
        return ExpressionFactory.pathExp(getName());
    }

    /**
     * @return An expression representing null.
     */
    public Expression isNull() {
        return ExpressionFactory.matchExp(getName(), null);
    }

    /**
     * @return An expression representing a non-null value.
     */
    public Expression isNotNull() {
        return ExpressionFactory.matchExp(getName(), null).notExp();
    }

    /**
     * @return An expression representing equality to TRUE.
     */
    public Expression isTrue() {
        return ExpressionFactory.matchExp(getName(), Boolean.TRUE);
    }

    /**
     * @return An expression representing equality to FALSE.
     */
    public Expression isFalse() {
        return ExpressionFactory.matchExp(getName(), Boolean.FALSE);
    }

    /**
     * @return An expression representing equality to a value.
     */
    public Expression eq(E value) {
        return ExpressionFactory.matchExp(getName(), value);
    }

    /**
     * @return An expression representing equality between two attributes
     * (columns).
     */
    public Expression eq(Property<?> value) {
        return ExpressionFactory.matchExp(getName(), new ASTObjPath(value.getName()));
    }

    /**
     * @return An expression representing inequality to a value.
     */
    public Expression ne(E value) {
        return ExpressionFactory.noMatchExp(getName(), value);
    }

    /**
     * @return An expression representing inequality between two attributes
     * (columns).
     */
    public Expression ne(Property<?> value) {
        return ExpressionFactory.noMatchExp(getName(), new ASTObjPath(value.getName()));
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
        return ExpressionFactory.likeExp(getName(), pattern);
    }

    /**
     * @param pattern    a properly escaped pattern matching property value. Pattern
     *                   may include "_" and "%" wildcard symbols to match any single
     *                   character or a sequence of characters.
     * @param escapeChar an escape character used in the pattern to escape "%" and "_".
     * @return An expression for a Database "LIKE" query.
     */
    public Expression like(String pattern, char escapeChar) {
        return ExpressionFactory.likeExp(getName(), pattern, escapeChar);
    }

    /**
     * @return An expression for a case insensitive "LIKE" query.
     */
    public Expression likeIgnoreCase(String pattern) {
        return ExpressionFactory.likeIgnoreCaseExp(getName(), pattern);
    }

    /**
     * @return An expression for a Database "NOT LIKE" query.
     */
    public Expression nlike(String value) {
        return ExpressionFactory.notLikeExp(getName(), value);
    }

    /**
     * @return An expression for a case insensitive "NOT LIKE" query.
     */
    public Expression nlikeIgnoreCase(String value) {
        return ExpressionFactory.notLikeIgnoreCaseExp(getName(), value);
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
        return ExpressionFactory.containsExp(getName(), substring);
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
        return ExpressionFactory.startsWithExp(getName(), value);
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
        return ExpressionFactory.endsWithExp(getName(), value);
    }

    /**
     * Same as {@link #contains(String)}, only using case-insensitive
     * comparison.
     */
    public Expression containsIgnoreCase(String value) {
        return ExpressionFactory.containsIgnoreCaseExp(getName(), value);
    }

    /**
     * Same as {@link #startsWith(String)}, only using case-insensitive
     * comparison.
     */
    public Expression startsWithIgnoreCase(String value) {
        return ExpressionFactory.startsWithIgnoreCaseExp(getName(), value);
    }

    /**
     * Same as {@link #endsWith(String)}, only using case-insensitive
     * comparison.
     */
    public Expression endsWithIgnoreCase(String value) {
        return ExpressionFactory.endsWithIgnoreCaseExp(getName(), value);
    }

    /**
     * @param lower The lower bound.
     * @param upper The upper bound.
     * @return An expression checking for objects between a lower and upper
     * bound inclusive
     */
    public Expression between(E lower, E upper) {
        return ExpressionFactory.betweenExp(getName(), lower, upper);
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

        return ExpressionFactory.inExp(getName(), values);
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

        return ExpressionFactory.notInExp(getName(), values);
    }

    /**
     * @return An expression for finding objects with values in the given set.
     */
    public Expression in(Collection<E> values) {
        return ExpressionFactory.inExp(getName(), values);
    }

    /**
     * @return An expression for finding objects with values not in the given
     * set.
     */
    public Expression nin(Collection<E> values) {
        return ExpressionFactory.notInExp(getName(), values);
    }

    /**
     * @return A greater than Expression.
     */
    public Expression gt(E value) {
        return ExpressionFactory.greaterExp(getName(), value);
    }

    /**
     * @return Represents a greater than relationship between two attributes
     * (columns).
     */
    public Expression gt(Property<?> value) {
        return ExpressionFactory.greaterExp(getName(), new ASTObjPath(value.getName()));
    }

    /**
     * @return A greater than or equal to Expression.
     */
    public Expression gte(E value) {
        return ExpressionFactory.greaterOrEqualExp(getName(), value);
    }

    /**
     * @return Represents a greater than or equal relationship between two
     * attributes (columns).
     */
    public Expression gte(Property<?> value) {
        return ExpressionFactory.greaterOrEqualExp(getName(), new ASTObjPath(value.getName()));
    }

    /**
     * @return A less than Expression.
     */
    public Expression lt(E value) {
        return ExpressionFactory.lessExp(getName(), value);
    }

    /**
     * @return Represents a less than relationship between two attributes
     * (columns).
     */
    public Expression lt(Property<?> value) {
        return ExpressionFactory.lessExp(getName(), new ASTObjPath(value.getName()));
    }

    /**
     * @return A less than or equal to Expression.
     */
    public Expression lte(E value) {
        return ExpressionFactory.lessOrEqualExp(getName(), value);
    }

    /**
     * @return Represents a less than or equal relationship between two
     * attributes (columns).
     */
    public Expression lte(Property<?> value) {
        return ExpressionFactory.lessOrEqualExp(getName(), new ASTObjPath(value.getName()));
    }

    /**
     * @return Ascending sort orderings on this property.
     */
    public Ordering asc() {
        return new Ordering(getName(), SortOrder.ASCENDING);
    }

    /**
     * @return Ascending sort orderings on this property.
     */
    public List<Ordering> ascs() {
        List<Ordering> result = new ArrayList<Ordering>(1);
        result.add(asc());
        return result;
    }

    /**
     * @return Ascending case insensitive sort orderings on this property.
     */
    public Ordering ascInsensitive() {
        return new Ordering(getName(), SortOrder.ASCENDING_INSENSITIVE);
    }

    /**
     * @return Ascending case insensitive sort orderings on this property.
     */
    public List<Ordering> ascInsensitives() {
        List<Ordering> result = new ArrayList<Ordering>(1);
        result.add(ascInsensitive());
        return result;
    }

    /**
     * @return Descending sort orderings on this property.
     */
    public Ordering desc() {
        return new Ordering(getName(), SortOrder.DESCENDING);
    }

    /**
     * @return Descending sort orderings on this property.
     */
    public List<Ordering> descs() {
        List<Ordering> result = new ArrayList<Ordering>(1);
        result.add(desc());
        return result;
    }

    /**
     * @return Descending case insensitive sort orderings on this property.
     */
    public Ordering descInsensitive() {
        return new Ordering(getName(), SortOrder.DESCENDING_INSENSITIVE);
    }

    /**
     * @return Descending case insensitive sort orderings on this property.
     */
    public List<Ordering> descInsensitives() {
        List<Ordering> result = new ArrayList<Ordering>(1);
        result.add(descInsensitive());
        return result;
    }

    /**
     * Returns a prefetch tree that follows this property path, potentially
     * spanning a number of phantom nodes, and having a single leaf with "joint"
     * prefetch semantics.
     */
    public PrefetchTreeNode joint() {
        return PrefetchTreeNode.withPath(name, PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
    }

    /**
     * Returns a prefetch tree that follows this property path, potentially
     * spanning a number of phantom nodes, and having a single leaf with
     * "disjoint" prefetch semantics.
     */
    public PrefetchTreeNode disjoint() {
        return PrefetchTreeNode.withPath(name, PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);
    }

    /**
     * Returns a prefetch tree that follows this property path, potentially
     * spanning a number of phantom nodes, and having a single leaf with
     * "disjoint by id" prefetch semantics.
     */
    public PrefetchTreeNode disjointById() {
        return PrefetchTreeNode.withPath(name, PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS);
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
        List<E> result = new ArrayList<E>(beans.size());
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

}