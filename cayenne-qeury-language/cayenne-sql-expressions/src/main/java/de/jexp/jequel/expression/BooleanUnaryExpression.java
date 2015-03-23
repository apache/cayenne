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

import de.jexp.jequel.expression.visitor.ExpressionVisitor;
import de.jexp.jequel.literals.UnaryOperator;

/**
 * TODO: only boolean UnaryOperator should be acceptable
 * NOT
 * EXISTS
 * NOT_EXISTS
 * */
public class BooleanUnaryExpression extends BooleanAbstractExpression {
    private final UnaryExpression<Expression> unaryExpression;

    protected BooleanUnaryExpression(UnaryOperator operator, Expression first) {
        this.unaryExpression = new UnaryExpression<Expression>(operator, first);
    }

    public UnaryExpression getUnaryExpression() {
        return unaryExpression;
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visit(this);
    }
}