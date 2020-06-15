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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.FunctionExpressionFactory;
import org.apache.cayenne.exp.parser.ASTPath;
import org.apache.cayenne.query.ColumnSelect;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.Orderings;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.reflect.PropertyUtils;

/**
 * Property that represents generic attribute.
 * <p>
 * Provides equality checks and sorting API along with some utility methods.
 *
 * @see org.apache.cayenne.exp.property
 * @since 4.2
 */
public class BaseProperty<E> implements Property<E> {

    /**
     * Name of the property in the object
     */
    protected final String name;

    /**
     * Expression provider for the property
     */
    protected final Supplier<Expression> expressionSupplier;

    /**
     * Explicit type of the property
     */
    protected final Class<E> type;

    /**
     * Constructs a new property with the given name and expression
     *
     * @param name of the property (will be used as alias for the expression)
     * @param expression expression for property
     * @param type of the property
     *
     * @see PropertyFactory#createBase(String, Expression, Class)
     */
    @SuppressWarnings({"unchecked", "Convert2Lambda", "Anonymous2MethodRef"})
    protected BaseProperty(String name, Expression expression, Class<? super E> type) {
        this.name = name;
        // can't use lambda here, see CAY-2635
        if(expression == null) {
            this.expressionSupplier = new Supplier<Expression>() {
                @Override
                public Expression get() {
                    return ExpressionFactory.pathExp(name);
                }
            };
        } else {
            this.expressionSupplier = new Supplier<Expression>() {
                @Override
                public Expression get() {
                    return expression.deepCopy();
                }
            };
        }
        this.type = (Class<E>)type;
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
        return expressionSupplier.get();
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : expressionSupplier.get().hashCode();
        if(type != null) {
            result = 31 * result + type.hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BaseProperty<?> property = (BaseProperty<?>) o;
        if (name != null ? !name.equals(property.name) : property.name != null) {
            return false;
        }
        if (name == null && !expressionSupplier.get().equals(property.expressionSupplier.get())) {
            return false;
        }
        return (type == null ? property.type == null : type.equals(property.type));
    }

    /**
     * Converts this property to a path expression.
     * This method is equivalent of getExpression() which is preferred as more generic.
     *
     * @return a newly created expression.
     * @see BaseProperty#getExpression()
     * @deprecated since 4.2, use {@link #getExpression()} method instead
     */
    @Deprecated
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
        return ExpressionFactory.noMatchExp(getExpression(), null);
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
    public NumericProperty<Long> count() {
        return PropertyFactory.createNumeric(FunctionExpressionFactory.countExp(getExpression()), Long.class);
    }

    /**
     * @see FunctionExpressionFactory#countDistinctExp(Expression)
     */
    public NumericProperty<Long> countDistinct() {
        return PropertyFactory.createNumeric(FunctionExpressionFactory.countDistinctExp(getExpression()), Long.class);
    }

    /**
     * Creates alias with different name for this property
     */
    public BaseProperty<E> alias(String alias) {
        return PropertyFactory.createBase(alias, this.getExpression(), this.getType());
    }

    /**
     * @return type of entity attribute described by this property
     */
    public Class<E> getType() {
        return type;
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
    public Expression eq(BaseProperty<?> value) {
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
    public Expression ne(BaseProperty<?> value) {
        return ExpressionFactory.noMatchExp(getExpression(), value.getExpression());
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
     * @return An expression for finding objects with values in the given subquery
     */
    public Expression in(ColumnSelect<? extends E> subquery) {
        return ExpressionFactory.inExp(getExpression(), subquery);
    }

    /**
     * @return An expression for finding objects with values not in the given subquery
     */
    public Expression nin(ColumnSelect<? extends E> subquery) {
        return ExpressionFactory.notInExp(getExpression(), subquery);
    }

    /**
     * @return property that will be translated relative to parent query
     */
    public BaseProperty<E> enclosing() {
        return PropertyFactory.createBase(ExpressionFactory.enclosingObjectExp(getExpression()), getType());
    }

    /**
     * @return An expression for calling functionName with first argument equals to <b>this</b> property
     *      and provided additional arguments
     */
    public <T> BaseProperty<T> function(String functionName, Class<T> returnType, BaseProperty<?>... arguments) {
        Object[] expressions = new Expression[arguments.length + 1];
        expressions[0] = getExpression();
        for(int i=1; i<=arguments.length; i++) {
            expressions[i] = arguments[i-1].getExpression();
        }
        return PropertyFactory.createBase(FunctionExpressionFactory.functionCall(functionName, expressions), returnType);
    }

    /**
     * @return An expression for calling functionName with first argument equals to <b>this</b> property
     *      and provided additional arguments
     */
    public <T> BaseProperty<T> function(String functionName, Class<T> returnType, Object... arguments) {
        Object[] expressions = new Object[arguments.length + 1];
        expressions[0] = getExpression();
        System.arraycopy(arguments, 0, expressions, 1, arguments.length);
        return PropertyFactory.createBase(FunctionExpressionFactory.functionCall(functionName, expressions), returnType);
    }

    /**
     * @return An expression for using operator with first argument equals to <b>this</b> property
     *      and provided additional arguments
     */
    public <T> BaseProperty<T> operator(String operator, Class<T> returnType, BaseProperty<?>... arguments) {
        Object[] expressions = new Expression[arguments.length + 1];
        expressions[0] = getExpression();
        for(int i=1; i<=arguments.length; i++) {
            expressions[i] = arguments[i-1].getExpression();
        }
        return PropertyFactory.createBase(FunctionExpressionFactory.operator(operator, expressions), returnType);
    }

    /**
     * @return An expression for using operator with first argument equals to <b>this</b> property
     *      and provided additional arguments
     */
    public <T> BaseProperty<T> operator(String operator, Class<T> returnType, Object... arguments) {
        Object[] expressions = new Object[arguments.length + 1];
        expressions[0] = getExpression();
        System.arraycopy(arguments, 0, expressions, 1, arguments.length);
        return PropertyFactory.createBase(FunctionExpressionFactory.operator(operator, expressions), returnType);
    }

}
