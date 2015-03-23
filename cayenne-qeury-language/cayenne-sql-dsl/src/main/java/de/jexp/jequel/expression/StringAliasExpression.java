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
public class StringAliasExpression extends AbstractExpression implements StringExpression, Aliased<StringExpression>, SqlDsl.SqlVisitable {

    private final StringExpression expressions;
    private final String alias;

    public StringAliasExpression(StringExpression expressions, String alias) {
        this.expressions = expressions;
        this.alias = alias;
    }

    @Override
    public BooleanExpression like(StringExpression expression) {
        return expressions.like(expression);
    }

    @Override
    public BooleanExpression likeIgnoreCase(StringExpression expression) {
        return expressions.likeIgnoreCase(expression);
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return expressions.accept(visitor);
    }

    @Override
    public StringExpression getAliased() {
        return expressions;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public <R> R accept(SqlDsl.SqlVisitor<R> sqlVisitor) {
        return sqlVisitor.visit(this);
    }
}
