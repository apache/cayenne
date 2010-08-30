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
/* ====================================================================
 *
 * Copyright(c) 2003, Andriy Shapochka
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *
 * 3. Neither the name of the ASHWOOD nor the
 *    names of its contributors may be used to endorse or
 *    promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 *
 * This software consists of voluntary contributions made by
 * individuals on behalf of the ASHWOOD Project and was originally
 * created by Andriy Shapochka.
 *
 */
package org.apache.cayenne.ashwood.dbutil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class Table implements Serializable {

    private String catalog;
    private String schema;
    private String name;

    private Collection<Column> columns = new ArrayList<Column>(1);
    private Collection<ForeignKey> foreignKeys = new ArrayList<ForeignKey>(1);
    private Collection<PrimaryKey> primaryKeys = new ArrayList<PrimaryKey>(1);

    public Table() {
    }

    public Table(String catalog, String schema, String name) {
        setCatalog(catalog);
        setSchema(schema);
        setName(name);
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getSchema() {
        return schema;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name != null ? name : "";
    }

    public static String composeFullName(String catalog, String schema, String tableName) {

        StringBuilder buffer = new StringBuilder();

        if (catalog != null) {
            buffer.append(catalog).append('.');
        }

        if (schema != null) {
            buffer.append(schema).append('.');
        }

        if (tableName != null) {
            buffer.append(tableName);
        }

        return buffer.toString();
    }

    public String getFullName() {
        return composeFullName(catalog, schema, name);
    }

    public Collection<PrimaryKey> getPrimaryKeys() {
        return Collections.unmodifiableCollection(primaryKeys);
    }

    public Collection<ForeignKey> getForeignKeys() {
        return Collections.unmodifiableCollection(foreignKeys);
    }

    public Collection<Column> getColumns() {
        return Collections.unmodifiableCollection(columns);
    }

    public void addColumn(Column column) {
        columns.add(column);
        column.setOwner(this);
    }

    public boolean removeColumn(Column column) {
        column.setOwner(null);
        return columns.remove(column);
    }

    public void addPrimaryKey(PrimaryKey primaryKey) {
        primaryKeys.add(primaryKey);
        primaryKey.setOwner(this);
    }

    public boolean removePrimaryKey(PrimaryKey primaryKey) {
        primaryKey.setOwner(null);
        return primaryKeys.remove(primaryKey);
    }

    public void addForeignKey(ForeignKey foreignKey) {
        foreignKeys.add(foreignKey);
        foreignKey.setOwner(this);
    }

    public boolean removeForeignKey(ForeignKey foreignKey) {
        foreignKey.setOwner(null);
        return foreignKeys.remove(foreignKey);
    }
}
