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

import de.jexp.jequel.expression.visitor.ExpressionVisitor;

public class BooleanLiteral extends AbstractBooleanExpression {
    public static final BooleanLiteral TRUE = new BooleanLiteral(true);
    public static final BooleanLiteral FALSE = new BooleanLiteral(false);
    public static final BooleanLiteral NULL = new BooleanLiteral(null) {
        @Override
        public BooleanExpression and(BooleanExpression expression) {
            return expression;
        }

        @Override
        public BooleanExpression or(BooleanExpression expression) {
            return expression;
        }
    };

    private final Boolean value;

    private BooleanLiteral(Boolean value) {
        this.value = value;
    }

    @Override
    public BooleanExpression is(BooleanLiteral expression) {
        throw new UnsupportedOperationException("'is' operation applicable only at non literal boolean expression ");
    }

    @Override
    public BooleanExpression isNot(BooleanLiteral expression) {
        throw new UnsupportedOperationException("'isNot' operation applicable only at non literal boolean expression ");
    }

    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visit(this);
    }

    public Boolean getValue() {
        return value;
    }
}