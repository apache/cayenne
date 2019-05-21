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

import org.apache.cayenne.access.sqlbuilder.sqltree.ColumnNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.UnescapedColumnNode;
import org.apache.cayenne.map.DbAttribute;

/**
 * @since 4.2
 */
public class ColumnNodeBuilder implements ExpressionTrait {

    private final String table;
    private final String column;

    private boolean unescaped;
    private String alias;
    private DbAttribute attribute;

    ColumnNodeBuilder(String table, String field) {
        this.table = table;
        this.column = Objects.requireNonNull(field);
    }

    ColumnNodeBuilder(String table, DbAttribute attribute) {
        this.table = table;
        this.column = Objects.requireNonNull(attribute).getName();
        this.attribute = attribute;
    }

    public ColumnNodeBuilder as(String alias) {
        this.alias = alias;
        return this;
    }

    public ColumnNodeBuilder unescaped() {
        this.unescaped = true;
        return this;
    }

    public ColumnNodeBuilder attribute(DbAttribute attribute) {
        this.attribute = attribute;
        return this;
    }

    public OrderingNodeBuilder desc() {
        return new OrderingNodeBuilder(this).desc();
    }

    public OrderingNodeBuilder asc() {
        return new OrderingNodeBuilder(this).asc();
    }

    @Override
    public Node build() {
        if(unescaped) {
            return new UnescapedColumnNode(table, column, alias, attribute);
        }
        return new ColumnNode(table, column, alias, attribute);
    }

}
