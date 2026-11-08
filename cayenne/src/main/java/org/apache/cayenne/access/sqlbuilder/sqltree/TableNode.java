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

import org.apache.cayenne.access.sqlbuilder.SQLAppendable;
import org.apache.cayenne.access.sqlbuilder.SQLGenerationContext;
import org.apache.cayenne.map.compiled.CompiledTable;

import java.util.Objects;

/**
 * @since 4.2
 */
public class TableNode extends Node {

    private final CompiledTable table;
    private final String tableName;
    private final String alias;

    public TableNode(String tableName, String alias) {
        this.table = null;
        this.tableName = Objects.requireNonNull(tableName);
        this.alias = alias;
    }

    public TableNode(CompiledTable table, String alias) {
        this.table = Objects.requireNonNull(table);
        this.tableName = null;
        this.alias = alias;
    }

    @Override
    public SQLAppendable append(SQLAppendable buffer, SQLGenerationContext context) {
        buffer.appendTokenSeparator();

        if(table != null) {
            if(table.catalog() != null) {
                buffer.appendQuoted(table.catalog()).append('.');
            }
            if(table.schema() != null) {
                buffer.appendQuoted(table.schema()).append('.');
            }
            buffer.appendQuoted(table.name());
        } else {
            buffer.appendQuoted(tableName);
        }

        // omit the alias for a single-table statement
        if (alias != null && (context == null || !context.isSingleTableSQL())) {
            buffer.appendTokenSeparator().appendQuoted(alias);
        }
        return buffer;
    }

    @Override
    public Node copy() {
        return new TableNode(tableName, alias);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TableNode tableNode = (TableNode) o;
        return Objects.equals(tableName, tableNode.tableName) && Objects.equals(alias, tableNode.alias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tableName, alias);
    }
}
