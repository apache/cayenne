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

import de.jexp.jequel.expression.BinaryExpression;
import de.jexp.jequel.expression.BooleanBinaryExpression;
import de.jexp.jequel.expression.BooleanLiteral;
import de.jexp.jequel.expression.BooleanUnaryExpression;
import de.jexp.jequel.expression.CompoundExpression;
import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.ExpressionAlias;
import de.jexp.jequel.expression.ParamExpression;
import de.jexp.jequel.expression.RowListExpression;
import de.jexp.jequel.expression.StringLiteral;
import de.jexp.jequel.expression.UnaryExpression;
import de.jexp.jequel.expression.NumericBinaryExpression;
import de.jexp.jequel.expression.NumericUnaryExpression;

/**
 * TODO
 *
 * This class do travailing thought expression left-to-right
 * and delegate processing of tree nodes to another visitor
 *
 * @since 4.0
 */
public class TraversalVisitor<R> extends DefaultExpressionVisitor<R> {

    @Override
    public R visit(NumericUnaryExpression numericUnaryExpression) {
        return visit(numericUnaryExpression.getUnaryExpression());
    }

    @Override
    public R visit(NumericBinaryExpression binaryExpression) {
        return visit(binaryExpression.getBinaryExpression());
    }

    @Override
    public R visit(BooleanLiteral booleanConstantExpression) {
        return null;
    }

    @Override
    public R visit(StringLiteral stringLiteral) {
        return null;
    }

    @Override
    public R visit(UnaryExpression unaryExpression) {
        return unaryExpression.getExpression().accept(this);
    }

    @Override
    public R visit(BooleanUnaryExpression booleanUnaryExpression) {
        return null;
    }

    @Override
    public R visit(BinaryExpression binaryExpression) {
        return null;
    }

    @Override
    public R visit(BooleanBinaryExpression binaryExpression) {
        return null;
    }



    @Override
    public R visit(CompoundExpression listExpression) {
        return null;
    }

    @Override
    public R visit(RowListExpression rowTupleExpression) {
        return null;
    }

    @Override
    public <T> R visit(ParamExpression<T> paramExpression) {
        return null;
    }

    @Override
    public <E extends Expression> R visit(ExpressionAlias<E> expression) {
        return null;
    }
}
