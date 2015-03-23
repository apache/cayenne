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
package de.jexp.jequel.expression;

import de.jexp.jequel.literals.Delimeter;
import de.jexp.jequel.literals.Operator;
import de.jexp.jequel.literals.UnaryOperator;

/**
 * @since 4.0
 */
public interface ExpressionsFactory {
    <V> ConstantExpression<V> createConstantExpression(V value);

    NumericLiteral createNumeric(Number value);

    NumericUnaryExpression createNumeric(UnaryOperator operator, NumericExpression first);

    NumericBinaryExpression createNumeric(NumericExpression first, Operator operator, NumericExpression second);

    BooleanLiteral boolTrue();

    BooleanLiteral boolFalse();

    BooleanLiteral boolNull();

    BooleanUnaryExpression createBoolean(UnaryOperator operator, Expression first);

    BooleanBinaryExpression createBoolean(Expression first, Operator operator, Expression second);

    BooleanListExpression createBoolean(Operator operator, BooleanExpression... expressions);

    StringLiteral create(String value);

    <E extends Expression> UnaryExpression<E> createUnary(UnaryOperator operator, E exp);

    <E extends Expression> BinaryExpression<E> createBinary(E first, Operator operator, E second);

    <A extends RowListExpression> RowListExpression<A> create(Delimeter delim, Expression... expressions);

    <V> LiteralExpression<V> create(V value);

    ParamExpression createParam(String paramName);

    <T> ParamExpression<T> createParam(String paramName, T paramValue);

    <T> ParamExpression<T> createParam(T paramValue);

    VariableNumericLiteralExpression pathNumeric(String path);

    VariableExpression path(String path);

    SqlLiteral sql(String sql);


}
