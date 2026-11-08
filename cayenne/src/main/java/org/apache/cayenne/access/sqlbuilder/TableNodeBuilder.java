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

package org.apache.cayenne.access.sqlbuilder;

import java.util.Objects;

import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.TableNode;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledTable;

/**
 * @since 4.2
 */
public class TableNodeBuilder implements NodeBuilder {

    private final String tableName;
    private final CompiledTable table;

    private String alias;

    TableNodeBuilder(String tableName) {
        this.tableName = Objects.requireNonNull(tableName);
        this.table = null;
    }

    TableNodeBuilder(CompiledTable table) {
        this.table = Objects.requireNonNull(table);
        this.tableName = table.name();
    }

    public TableNodeBuilder as(String alias) {
        this.alias = alias;
        return this;
    }

    public ColumnNodeBuilder column(String column) {
        return new ColumnNodeBuilder(tableName, column);
    }

    public ColumnNodeBuilder column(CompiledColumn column) {
        return new ColumnNodeBuilder(tableName, column);
    }

    public String getAlias() {
        return alias;
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public Node build() {
        if(table != null) {
            return new TableNode(table, alias);
        }
        return new TableNode(tableName, alias);
    }

}
