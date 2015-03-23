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

    NumericLiteral createNumeric(Number value);

    NumericUnaryExpression createNumeric(UnaryOperator operator, NumericExpression first);

    NumericBinaryExpression createNumeric(Operator operator, NumericExpression first, NumericExpression second);

    NumericPathExpression createNumericPath(String path);

    BooleanLiteral boolTrue();

    BooleanLiteral boolFalse();

    BooleanLiteral boolNull();

    BooleanUnaryExpression createBoolean(UnaryOperator operator, Expression first);

    BooleanBinaryExpression createBoolean(Operator operator, Expression first, Expression second);

    BooleanListExpression createBooleanList(Operator operator, BooleanExpression... expressions);

    StringLiteral create(String value);

    <E extends Expression> UnaryExpression<E> createUnary(UnaryOperator operator, E exp);

    <E extends Expression> BinaryExpression<E> createBinary(E first, Operator operator, E second);

    SimpleListExpression create(Delimeter delim, Expression... expressions);

    <V> LiteralExpression<V> create(V value);

    ParamExpression createParam(String paramName);

    <T> ParamExpression<T> createParam(String paramName, T paramValue);

    <T> ParamExpression<T> createParam(T paramValue);

    PathExpression path(String path);

    SqlLiteral sql(String sql);
}
