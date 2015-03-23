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

public interface ExpressionVisitor<R> {
    <V> R visit(LiteralExpression<V> constantExpression);

    R visit(NumericLiteral numericLiteral);

    R visit(NumericUnaryExpression numericUnaryExpression);

    R visit(NumericBinaryExpression binaryExpression);

    R visit(BooleanLiteral booleanConstantExpression);

    R visit(BooleanUnaryExpression booleanUnaryExpression);

    R visit(BooleanBinaryExpression binaryExpression);

    R visit(BooleanListExpression binaryExpression);

    R visit(StringLiteral stringLiteral);

    R visit(UnaryExpression unaryExpression);

    R visit(BinaryExpression binaryExpression);

    R visit(CompoundExpression listExpression);

    R visit(RowListExpression rowTupleExpression);

    <T> R visit(ParamExpression<T> paramExpression);

    <E extends Expression> R visit(ExpressionAlias<E> expression);

    /* TableVisitor */

    <T> R visit(VariableExpression field);
    /*
    <T> R visit(Field<T> field);

    R visit(JoinTable joinTable);

    R visit(BaseTable table);
    */
}
