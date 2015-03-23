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

import de.jexp.jequel.sql.SqlDsl;

/**
 * @since 4.0
 */
public class AliasedColumn<JavaType> extends AbstractExpression implements IColumn<JavaType>, Aliased<IColumn<JavaType>> {

    private final IColumn<JavaType> column;
    private final String alias;

    public AliasedColumn(IColumn<JavaType> column, String alias) {
        this.column = column;
        this.alias = alias;
    }

    @Override
    public ITable getTable() {
        return column.getTable();
    }

    @Override
    public String getName() {
        return column.getName();
    }

    @Override
    public boolean isPrimaryKey() {
        return column.isPrimaryKey();
    }

    @Override
    public boolean isMandatory() {
        return column.isMandatory();
    }

    @Override
    public int getJdbcType() {
        return column.getJdbcType();
    }

    @Override
    public String getTableName() {
        return column.getTableName();
    }

    @Override
    public String getValue() {
        return column.getValue();
    }

    @Override
    public IColumn<JavaType> as(String alias) {
        return column.as(alias);
    }

    @Override
    public IColumn<JavaType> getAliased() {
        return column;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public <R> R accept(SqlDsl.SqlVisitor<R> visitor) {
        return visitor.visit((Aliased<? extends Expression>) this);
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
