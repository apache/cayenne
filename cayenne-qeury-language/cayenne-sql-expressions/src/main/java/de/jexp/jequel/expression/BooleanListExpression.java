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

import de.jexp.jequel.literals.Operator;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * @since 4.0
 */
public class BooleanListExpression extends BooleanAbstractExpression {

    private final List<BooleanExpression> expressions;
    private final Operator operator;

    protected BooleanListExpression(Operator operator, List<BooleanExpression> expressions) {
        this.expressions = new ArrayList<BooleanExpression>(expressions);
        this.operator = operator;
    }

    protected BooleanListExpression(Operator operator, BooleanExpression... expressions) {
        this(operator, asList(expressions));
    }

    public List<BooleanExpression> getExpressions() {
        return expressions;
    }

    @Override
    public BooleanExpression and(BooleanExpression expression) {
        if (operator == Operator.AND) {
            expressions.add(expression);
            return this;
        }

        return super.and(expression);
    }

    @Override
    public BooleanExpression or(BooleanExpression expression) {
        if (operator == Operator.OR) {
            expressions.add(expression);
            return this;
        }

        return super.or(expression);
    }

    public <K> void process(ExpressionProcessor<K> expressionProcessor) {
        for (Expression expression : getExpressions()) {
            expressionProcessor.process(expression);
        }
    }

    public Operator getOperator() {
        return operator;
    }


    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
