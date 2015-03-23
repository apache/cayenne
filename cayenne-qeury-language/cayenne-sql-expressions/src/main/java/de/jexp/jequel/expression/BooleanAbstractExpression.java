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

/**
 * @since 4.0
 */
public abstract class BooleanAbstractExpression extends AbstractExpression implements BooleanExpression {
    @Override
    public BooleanExpression and(BooleanExpression expression) {
        return factory().createBooleanList(Operator.AND, this, expression);
    }

    @Override
    public BooleanExpression or(BooleanExpression expression) {
        return factory().createBooleanList(Operator.OR, this, expression);
    }

    @Override
    public BooleanExpression is(BooleanLiteral expression) {
        return factory().createBoolean(Operator.IS, this, expression);
    }

    @Override
    public BooleanExpression isNot(BooleanLiteral expression) {
        return factory().createBoolean(Operator.IS_NOT, this, expression);
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }
}
