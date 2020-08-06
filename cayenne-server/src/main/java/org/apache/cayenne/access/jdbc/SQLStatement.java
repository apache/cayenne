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

package org.apache.cayenne.access.jdbc;

import org.apache.cayenne.access.translator.ParameterBinding;

/**
 * A PreparedStatement descriptor containing a String of SQL and an array of parameters.
 * SQLStatement is essentially a "compiled" version of any single query.
 * 
 * @since 1.1
 */
public class SQLStatement {

    protected String sql;
    protected ParameterBinding[] bindings;
    protected ColumnDescriptor[] resultColumns;

    public SQLStatement() {
    }

    public SQLStatement(String sql, ParameterBinding[] bindings) {
        this(sql, null, bindings);
    }

    /**
     * @since 1.2
     */
    public SQLStatement(String sql, ColumnDescriptor[] resultColumns,
                        ParameterBinding[] bindings) {

        setSql(sql);
        setBindings(bindings);
        setResultColumns(resultColumns);
    }

    /**
     * @since 1.2
     */
    public ColumnDescriptor[] getResultColumns() {
        return resultColumns;
    }

    /**
     * @since 1.2
     */
    public void setResultColumns(ColumnDescriptor[] descriptors) {
        resultColumns = descriptors;
    }

    public ParameterBinding[] getBindings() {
        return bindings;
    }

    public String getSql() {
        return sql;
    }

    public void setBindings(ParameterBinding[] bindings) {
        this.bindings = bindings;
    }

    public void setSql(String string) {
        sql = string;
    }
}
