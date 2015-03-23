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
package de.jexp.jequel.table;

import de.jexp.jequel.expression.AbstractExpression;
import de.jexp.jequel.expression.Aliased;
import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.PathExpression;
import de.jexp.jequel.expression.visitor.ExpressionVisitor;
import de.jexp.jequel.sql.SqlDsl;

import java.util.List;
import java.util.Map;

/**
 * @since 4.0
 */
public class AliasedTable<A extends ITable<?>> extends AbstractExpression implements ITable<A>, Aliased<A> {

    private final A table;
    private final String alias;

    public AliasedTable(A table, String alias) {
        this.table = table;
        this.alias = alias;
    }

    @Override
    public A getAliased() {
        return table;
    }

    public String getAlias() {
        return alias;
    }

    @Override
    public IColumn getField(String name) {
        return table.getField(name);
    }

    @Override
    public Map<String, IColumn> getFields() {
        return table.getFields();
    }

    @Override
    public List<PathExpression> columns() {
        return table.columns();
    }

    @Override
    public A as(String alias) {
        return (A) table.as(alias);
    }

    @Override
    public String getValue() {
        return table.getValue();
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return table.accept(visitor);
    }

    @Override
    public <R> R accept(SqlDsl.SqlVisitor<R> sqlVisitor) {
        return sqlVisitor.visit((Aliased<? extends Expression>) this);
    }

    @Override
    public String getName() {
        return alias;
    }

    @Override
    public String getTableName() {
        return table.getName();
    }
}
