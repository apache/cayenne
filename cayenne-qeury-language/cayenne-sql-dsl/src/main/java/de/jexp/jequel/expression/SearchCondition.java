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

import de.jexp.jequel.expression.logical.AbstractBooleanExpression;
import de.jexp.jequel.expression.logical.BooleanExpression;
import de.jexp.jequel.expression.logical.BooleanLiteral;
import de.jexp.jequel.expression.visitor.ExpressionVisitor;

/**
 * TODO I wanna it also will be immutable
 * */
public class SearchCondition extends AbstractBooleanExpression {
    private BooleanExpression expr = BooleanLiteral.NULL;

    @Override
    public BooleanExpression and(BooleanExpression expression) {
        this.expr = expr.and(expression);

        return this.expr;
    }

    @Override
    public BooleanExpression or(BooleanExpression expression) {
        this.expr = expr.or(expression);

        return this.expr;
    }

    @Override
    public BooleanExpression is(BooleanLiteral expression) {
        this.expr = expr.is(expression);

        return this.expr;
    }

    @Override
    public BooleanExpression isNot(BooleanLiteral expression) {
        this.expr = expr.isNot(expression);

        return this.expr;
    }

    public BooleanExpression getBooleanExpression() {
        return expr;
    }

    public <R> R accept(ExpressionVisitor<R> visitor) {
        return this.getBooleanExpression().accept(visitor);
    }

    public <K> void process(ExpressionProcessor<K> expressionProcessor) {
        if (expr != null) {
            expr.process(expressionProcessor);
        }
    }
}
