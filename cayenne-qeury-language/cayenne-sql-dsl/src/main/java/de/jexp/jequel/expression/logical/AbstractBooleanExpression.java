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
package de.jexp.jequel.expression.logical;

import de.jexp.jequel.expression.AbstractExpression;
import de.jexp.jequel.literals.Operator;

/**
 * @since 4.0
 */
public abstract class AbstractBooleanExpression extends AbstractExpression implements BooleanExpression {
    @Override
    public BooleanExpression and(BooleanExpression expression) {
        return new BooleanListExpression(Operator.AND, this, expression);
    }

    @Override
    public BooleanExpression or(BooleanExpression expression) {
        return new BooleanListExpression(Operator.OR, this, expression);
    }

    @Override
    public BooleanExpression is(BooleanLiteral expression) {
        return new BooleanBinaryExpression(this, Operator.IS, expression);
    }

    @Override
    public BooleanExpression isNot(BooleanLiteral expression) {
        return new BooleanBinaryExpression(this, Operator.IS_NOT, expression);
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }
}
