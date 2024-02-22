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
 * Property that represents date/time attribute.
 * <pre>{@code
 * ObjectSelect.query(Artist.class)
 *      .where(Artist.DATE_OF_BIRTH.year().lte(1900))
 *      .or(Artist.DATE_OF_BIRTH.month().between(6, 8))
 * }</pre>
 *
 * @see org.apache.cayenne.exp.property
 * @since 4.2
 */
public class DateProperty<E> extends BaseProperty<E> implements ComparableProperty<E> {

    /**
     * Constructs a new property with the given name and expression
     *
     * @param path       of the property (will be used as alias for the expression)
     * @param expression expression for property
     * @param type       of the property
     */
    protected DateProperty(CayennePath path, Expression expression, Class<E> type) {
        super(path, expression, type);
    }

    /**
     * It is a caller responsibility to check that underlying attribute has year component
     *
     * @return new property that represents year component of this date property
     * @see FunctionExpressionFactory#yearExp(Expression)
     */
    public NumericProperty<Integer> year() {
        return PropertyFactory.createNumeric(FunctionExpressionFactory.yearExp(getExpression()), Integer.class);
    }

    /**
     * It is a caller responsibility to check that underlying attribute has month component
     *
     * @return new property that represents month component of this date property
     * @see FunctionExpressionFactory#monthExp(Expression)
     */
    public NumericProperty<Integer> month() {
        return PropertyFactory.createNumeric(FunctionExpressionFactory.monthExp(getExpression()), Integer.class);
    }

    /**
     * It is a caller responsibility to check that underlying attribute has day component
     *
     * @return new property that represents day of month component of this date property
     * @see FunctionExpressionFactory#dayOfMonthExp(Expression)
     */
    public NumericProperty<Integer> dayOfMonth() {
        return PropertyFactory.createNumeric(FunctionExpressionFactory.dayOfMonthExp(getExpression()), Integer.class);
    }

    /**
     * It is a caller responsibility to check that underlying attribute has day component
     *
     * @return new property that represents day of year component of this date property
     * @see FunctionExpressionFactory#dayOfMonthExp(Expression)
     */
    public NumericProperty<Integer> dayOfYear() {
        return PropertyFactory.createNumeric(FunctionExpressionFactory.dayOfYearExp(getExpression()), Integer.class);
    }

    /**
     * It is a caller responsibility to check that underlying attribute has time component
     *
     * @return new property that represents hour component of this time property
     * @see FunctionExpressionFactory#hourExp(Expression)
     */
    public NumericProperty<Integer> hour() {
        return PropertyFactory.createNumeric(FunctionExpressionFactory.hourExp(getExpression()), Integer.class);
    }

    /**
     * It is a caller responsibility to check that underlying attribute has time component
     *
     * @return new property that represents minute component of this time property
     * @see FunctionExpressionFactory#minuteExp(Expression)
     */
    public NumericProperty<Integer> minute() {
        return PropertyFactory.createNumeric(FunctionExpressionFactory.minuteExp(getExpression()), Integer.class);
    }

    /**
     * It is a caller responsibility to check that underlying attribute has time component
     *
     * @return new property that represents second component of this time property
     * @see FunctionExpressionFactory#secondExp(Expression)
     */
    public NumericProperty<Integer> second() {
        return PropertyFactory.createNumeric(FunctionExpressionFactory.secondExp(getExpression()), Integer.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DateProperty<E> alias(String alias) {
        return PropertyFactory.createDate(alias, this.getExpression(), this.getType());
    }

    /**
     * @see FunctionExpressionFactory#maxExp(Expression)
     */
    public DateProperty<E> max() {
        return PropertyFactory.createDate(FunctionExpressionFactory.maxExp(getExpression()), getType());
    }

    /**
     * @see FunctionExpressionFactory#minExp(Expression)
     */
    public DateProperty<E> min() {
        return PropertyFactory.createDate(FunctionExpressionFactory.minExp(getExpression()), getType());
    }

    /**
     * @return property that will be translated relative to parent query
     */
    public DateProperty<E> enclosing() {
        return PropertyFactory.createDate(ExpressionFactory.enclosingObjectExp(getExpression()), getType());
    }
}
