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
import de.jexp.jequel.sql.SqlDsl;

/**
 * @since 4.0
 */
public class NumericAliaseExpression extends AbstractExpression implements NumericExpression, Aliased<NumericExpression>, SqlDsl.SqlVisitable {
    private final NumericExpression expression;
    private final String alias;

    public NumericAliaseExpression(NumericExpression expression, String alias) {
        this.expression = expression;
        this.alias = alias;
    }

    @Override
    public NumericExpression getAliased() {
        return expression;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public NumericExpression plus(NumericExpression expression) {
        return this.expression.plus(expression);
    }

    @Override
    public NumericExpression plus(Number expression) {
        return this.expression.plus(expression);
    }

    @Override
    public NumericExpression minus(NumericExpression expression) {
        return this.expression.minus(expression);
    }

    @Override
    public NumericExpression minus(Number expression) {
        return this.expression.minus(expression);
    }

    @Override
    public NumericExpression times(NumericExpression expression) {
        return this.expression.times(expression);
    }

    @Override
    public NumericExpression times(Number expression) {
        return this.expression.times(expression);
    }

    @Override
    public NumericExpression by(NumericExpression expression) {
        return this.expression.by(expression);
    }

    @Override
    public NumericExpression by(Number expression) {
        return this.expression.by(expression);
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return expression.accept(visitor);
    }

    @Override
    public <R> R accept(SqlDsl.SqlVisitor<R> sqlVisitor) {
        return sqlVisitor.visit(this);
    }
}
