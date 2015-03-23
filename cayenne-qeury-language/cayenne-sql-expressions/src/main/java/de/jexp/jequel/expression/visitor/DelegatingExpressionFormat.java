/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package de.jexp.jequel.expression.visitor;

import de.jexp.jequel.expression.*;
import de.jexp.jequel.expression.BooleanBinaryExpression;
import de.jexp.jequel.expression.BooleanListExpression;
import de.jexp.jequel.expression.BooleanLiteral;
import de.jexp.jequel.expression.BooleanUnaryExpression;
import de.jexp.jequel.expression.StringLiteral;
import de.jexp.jequel.expression.NumericBinaryExpression;
import de.jexp.jequel.expression.NumericLiteral;
import de.jexp.jequel.expression.NumericUnaryExpression;

public class DelegatingExpressionFormat extends DelegatingFormat<ExpressionFormat> implements ExpressionFormat {

    public DelegatingExpressionFormat(ExpressionFormat format) {
        super(format);
    }

    @Override
    public <V> String visit(LiteralExpression<V> constantExpression) {
        return formatAround(getFormat().visit(constantExpression), constantExpression);
    }

    @Override
    public String visit(NumericLiteral numericLiteral) {
        return formatAround(getFormat().visit(numericLiteral), numericLiteral);
    }

    @Override
    public String visit(BooleanLiteral booleanConstantExpression) {
        return formatAround(getFormat().visit(booleanConstantExpression), booleanConstantExpression);
    }

    @Override
    public String visit(StringLiteral expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    @Override
    public String visit(UnaryExpression expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    @Override
    public String visit(BooleanUnaryExpression expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    @Override
    public String visit(NumericUnaryExpression expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    @Override
    public String visit(BinaryExpression expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    @Override
    public String visit(BooleanBinaryExpression expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    @Override
    public String visit(BooleanListExpression binaryExpression) {
        return formatAround(getFormat().visit(binaryExpression), binaryExpression);
    }

    @Override
    public String visit(NumericBinaryExpression expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    @Override
    public String visit(CompoundExpression expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    @Override
    public String visit(RowListExpression expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    @Override
    public <T> String visit(ParamExpression<T> expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    @Override
    public <E extends Expression> String visit(ExpressionAlias<E> expression) {
        return formatAround(getFormat().visit(expression), expression);
    }

    @Override
    public <T> String visit(VariableExpression path) {
        return formatAround(getFormat().visit(path), path);
    }

}
