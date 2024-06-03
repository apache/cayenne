/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access.sqlbuilder.sqltree;

import org.apache.cayenne.access.sqlbuilder.QuotingAppendable;
import org.apache.cayenne.map.DbAttribute;

import java.util.Objects;

/**
 * @since 4.2
 */
public class ColumnNode extends Node {

    protected final String table;
    protected final String column;
    protected final DbAttribute attribute;
    protected String alias;

    public ColumnNode(String table, String column, String alias, DbAttribute attribute) {
        super(NodeType.COLUMN);
        this.table = table;
        this.column = column;
        this.alias = alias;
        this.attribute = attribute;
    }

    @Override
    public QuotingAppendable append(QuotingAppendable buffer) {
        buffer.append(' ');
        if (table != null) {
            buffer.appendQuoted(table).append('.');
        }
        buffer.appendQuoted(column);
        if (alias != null) {
            buffer.append(' ').appendQuoted(alias);
        }
        return buffer;
    }

    public String getTable() {
        return table;
    }

    public String getColumn() {
        return column;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public DbAttribute getAttribute() {
        return attribute;
    }

    @Override
    public Node copy() {
        return new ColumnNode(table, column, alias, attribute);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ColumnNode that = (ColumnNode) o;
        return Objects.equals(table, that.table) && Objects.equals(column, that.column) && Objects.equals(alias, that.alias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), table, column, alias);
    }
}
