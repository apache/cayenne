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

import java.util.Objects;

import org.apache.cayenne.access.sqlbuilder.QuotingAppendable;
import org.apache.cayenne.map.DbEntity;

/**
 * @since 4.2
 */
public class TableNode extends Node {

    private final DbEntity dbEntity;
    private final String tableName;
    private final String alias;

    public TableNode(String tableName, String alias) {
        this.dbEntity = null;
        this.tableName = Objects.requireNonNull(tableName);
        this.alias = alias;
    }

    public TableNode(DbEntity dbEntity, String alias) {
        this.dbEntity = Objects.requireNonNull(dbEntity);
        this.tableName = null;
        this.alias = alias;
    }

    @Override
    public QuotingAppendable append(QuotingAppendable buffer) {
        buffer.append(' ');

        if(dbEntity != null) {
            if(dbEntity.getCatalog() != null) {
                buffer.appendQuoted(dbEntity.getCatalog()).append('.');
            }
            if(dbEntity.getSchema() != null) {
                buffer.appendQuoted(dbEntity.getSchema()).append('.');
            }
            buffer.appendQuoted(dbEntity.getName());
        } else {
            buffer.appendQuoted(tableName);
        }

        if (alias != null) {
            buffer.append(' ').appendQuoted(alias);
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
