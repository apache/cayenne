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

public class ForeignKey implements Serializable {

    private String pkTableCatalog;
    private String pkTableSchema;
    private String pkTableName;
    private String pkColumnName;
    private Table owner;
    private String columnName;
    private short updateRule;
    private short deleteRule;
    private String name;
    private String pkName;
    private short deferrability;
    private short keySequence;

    public ForeignKey() {
    }

    public ForeignKey(Column fkColumn) {
        columnName = fkColumn.getName();
        owner = fkColumn.getOwner();
        keySequence = (short) (owner.getForeignKeys().size() + 1);
        name = owner.getName() + "_FK" + keySequence;
    }

    public String getPkTableCatalog() {
        return pkTableCatalog;
    }

    public void setPkTableCatalog(String pkTableCatalog) {
        this.pkTableCatalog = pkTableCatalog;
    }

    public void setPkTableSchema(String pkTableSchema) {
        this.pkTableSchema = pkTableSchema;
    }

    public String getPkTableSchema() {
        return pkTableSchema;
    }

    public void setPkTableName(String pkTableName) {
        this.pkTableName = pkTableName;
    }

    public String getPkTableName() {
        return pkTableName;
    }

    public void setPkColumnName(String pkColumnName) {
        this.pkColumnName = pkColumnName;
    }

    public String getPkColumnName() {
        return pkColumnName;
    }

    public void setOwner(Table owner) {
        this.owner = owner;
    }

    public Table getOwner() {
        return owner;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setKeySequence(short keySequence) {
        this.keySequence = keySequence;
    }

    public short getKeySequence() {
        return keySequence;
    }

    public void setUpdateRule(short updateRule) {
        this.updateRule = updateRule;
    }

    public short getUpdateRule() {
        return updateRule;
    }

    public void setDeleteRule(short deleteRule) {
        this.deleteRule = deleteRule;
    }

    public short getDeleteRule() {
        return deleteRule;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setPkName(String pkName) {
        this.pkName = pkName;
    }

    public String getPkName() {
        return pkName;
    }

    public void setDeferrability(short deferrability) {
        this.deferrability = deferrability;
    }

    public short getDeferrability() {
        return deferrability;
    }
}
