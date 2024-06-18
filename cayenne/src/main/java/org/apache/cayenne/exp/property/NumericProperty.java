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
import org.apache.cayenne.exp.parser.ASTAdd;
import org.apache.cayenne.exp.parser.ASTDivide;
import org.apache.cayenne.exp.parser.ASTMultiply;
import org.apache.cayenne.exp.parser.ASTNegate;
import org.apache.cayenne.exp.parser.ASTSubtract;
import org.apache.cayenne.exp.path.CayennePath;

/**
 * Property that represents attributes mapped on numeric types
 * <p>
 * Numeric type is an any type inherited from {@link Number}.
 * <p>
 * Provides basic math functions like {@link #mod(Number)}, {@link #abs()} and {@link #sqrt()}.
 * It is also implements {@link ComparableProperty} interface.
 *
 * @see org.apache.cayenne.exp.property
 * @since 4.2
 */
public class NumericProperty<E extends Number> extends BaseProperty<E> implements ComparableProperty<E> {

    /**
     * Constructs a new property with the given name and expression
     *
     * @param path       of the property (will be used as alias for the expression)
     * @param expression expression for property
     * @param type       of the property
     * @see PropertyFactory#createNumeric(String, Expression, Class)
     */
    protected NumericProperty(CayennePath path, Expression expression, Class<E> type) {
        super(path, expression, type);
    }

    /**
     * @see FunctionExpressionFactory#avgExp(Expression)
     */
    public NumericProperty<E> avg() {
        return PropertyFactory.createNumeric(FunctionExpressionFactory.avgExp(getExpression()), getType());
    }

    /**
     * @see FunctionExpressionFactory#sumExp(Expression)
     */
    public NumericProperty<E> sum() {
        return PropertyFactory.createNumeric(FunctionExpressionFactory.sumExp(getExpression()), getType());
    }

    /**
     * {@inheritDoc}
     */
    public NumericProperty<E> max() {
        return PropertyFactory.createNumeric(FunctionExpressionFactory.maxExp(getExpression()), getType());
    }

    /**
     * {@inheritDoc}
     */
    public NumericProperty<E> min() {
        return PropertyFactory.createNumeric(FunctionExpressionFactory.minExp(getExpression()), getType());
    }

    /**
     * @see FunctionExpressionFactory#modExp(Expression, Number)
     */
    public NumericProperty<E> mod(Number number) {
        return PropertyFactory.createNumeric(FunctionExpressionFactory.modExp(getExpression(), number), getType());
    }

    /**
     * @see FunctionExpressionFactory#modExp(Expression, Number)
     */
    public NumericProperty<E> mod(NumericProperty<?> number) {
        return PropertyFactory.createNumeric(FunctionExpressionFactory.modExp(getExpression(), number.getExpression()), getType());
    }

    /**
     * @see FunctionExpressionFactory#absExp(Expression)
     *
     * @return new property that represents abs() function call with current property as argument
     */
    public NumericProperty<E> abs() {
        return PropertyFactory.createNumeric(FunctionExpressionFactory.absExp(getExpression()), getType());
    }

    /**
     * @see FunctionExpressionFactory#sqrtExp(Expression)
     *
     * @return new property that represents sqrt() function call with current property as argument
     */
    public NumericProperty<E> sqrt() {
        return PropertyFactory.createNumeric(FunctionExpressionFactory.sqrtExp(getExpression()), getType());
    }

    /**
     * @return new property that represents '+' operator with current property as argument
     */
    public NumericProperty<E> add(E value) {
        return PropertyFactory.createNumeric(new ASTAdd(getExpression(), value), getType());
    }

    /**
     * @return new property that represents '+' operator with current property as argument
     */
    public NumericProperty<E> add(NumericProperty<?> value) {
        return PropertyFactory.createNumeric(new ASTAdd(getExpression(), value.getExpression()), getType());
    }

    /**
     * @return new property that represents '-' operator with current property as argument
     */
    public NumericProperty<E> sub(E value) {
        return PropertyFactory.createNumeric(new ASTSubtract(getExpression(), value), getType());
    }

    /**
     * @return new property that represents '-' operator with current property as argument
     */
    public NumericProperty<E> sub(NumericProperty<?> value) {
        return PropertyFactory.createNumeric(new ASTSubtract(getExpression(), value.getExpression()), getType());
    }

    /**
     * @return new property that represents '/' operator with current property as argument
     */
    public NumericProperty<E> div(E value) {
        return PropertyFactory.createNumeric(new ASTDivide(getExpression(), value), getType());
    }

    /**
     * @return new property that represents '/' operator with current property as argument
     */
    public NumericProperty<E> div(NumericProperty<?> value) {
        return PropertyFactory.createNumeric(new ASTDivide(getExpression(), value.getExpression()), getType());
    }

    /**
     * @return new property that represents '*' operator with current property as argument
     */
    public NumericProperty<E> mul(E value) {
        return PropertyFactory.createNumeric(new ASTMultiply(getExpression(), value), getType());
    }

    /**
     * @return new property that represents '*' operator with current property as argument
     */
    public NumericProperty<E> mul(NumericProperty<?> value) {
        return PropertyFactory.createNumeric(new ASTMultiply(getExpression(), value.getExpression()), getType());
    }

    /**
     * @return new property that represents negative value of current property
     */
    public NumericProperty<E> neg() {
        return PropertyFactory.createNumeric(new ASTNegate(getExpression()), getType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NumericProperty<E> alias(String alias) {
        return PropertyFactory.createNumeric(alias, this.getExpression(), this.getType());
    }

    /**
     * @return property that will be translated relative to parent query
     */
    public NumericProperty<E> enclosing() {
        return PropertyFactory.createNumeric(ExpressionFactory.enclosingObjectExp(getExpression()), getType());
    }

}
