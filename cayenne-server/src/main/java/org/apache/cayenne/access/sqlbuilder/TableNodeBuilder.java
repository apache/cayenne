/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access.sqlbuilder;

import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.TableNode;
import org.apache.cayenne.map.DbAttribute;

/**
 * @since 4.2
 */
public class TableNodeBuilder implements NodeBuilder {

    private final String tableName;

    private String alias;

    TableNodeBuilder(String tableName) {
        this.tableName = tableName;
    }

    public TableNodeBuilder as(String alias) {
        this.alias = alias;
        return this;
    }

    public ColumnNodeBuilder column(String column) {
        return new ColumnNodeBuilder(tableName, column);
    }

    public ColumnNodeBuilder column(DbAttribute attribute) {
        return new ColumnNodeBuilder(tableName, attribute);
    }

    public String getAlias() {
        return alias;
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public Node build() {
        return new TableNode(tableName, alias);
    }

}
