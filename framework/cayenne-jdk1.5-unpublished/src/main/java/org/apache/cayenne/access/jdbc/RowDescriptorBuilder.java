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
package org.apache.cayenne.access.jdbc;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.commons.collections.Transformer;

/**
 * A builder class that helps to assemble {@link RowDescriptor} instances from various
 * types of inputs.
 * 
 * @since 3.0
 */
public class RowDescriptorBuilder {

    private static final Transformer UPPERCASE_TRANSFORMER = new Transformer() {

        public Object transform(Object input) {
            return input != null ? input.toString().toUpperCase() : null;
        }
    };

    private static final Transformer LOWERCASE_TRANSFORMER = new Transformer() {

        public Object transform(Object input) {
            return input != null ? input.toString().toLowerCase() : null;
        }
    };

    protected ColumnDescriptor[] columns;
    protected ResultSetMetaData resultSetMetadata;

    protected Transformer caseTransformer;
    protected Map<String, String> typeOverrides;

    /**
     * Returns a RowDescriptor built based on the builder internal state.
     */
    public RowDescriptor getDescriptor(ExtendedTypeMap typeMap) throws SQLException,
            IllegalStateException {

        // explicitly-set columns take precedence over the columns derived from
        // ResultSetMetadata

        ColumnDescriptor[] columns;

        if (this.columns != null) {
            columns = columnsFromPresetColumns();
        }
        else if (this.resultSetMetadata != null) {
            columns = columnsFromResultSet();
        }
        else {
            throw new IllegalStateException(
                    "Can't build RowDescriptor, both 'columns' and 'resultSetMetadata' are null");
        }

        ExtendedType[] converters = new ExtendedType[columns.length];
        for (int i = 0; i < columns.length; i++) {
            converters[i] = typeMap.getRegisteredType(columns[i].getJavaClass());
        }

        return new RowDescriptor(columns, converters);
    }

    protected ColumnDescriptor[] columnsFromPresetColumns() {
        int len = columns.length;

        if (caseTransformer != null) {
            for (int i = 0; i < len; i++) {

                String oldLabel = columns[i].getDataRowKey();
                String oldName = columns[i].getName();

                String newLabel = (String) caseTransformer.transform(oldLabel);
                columns[i].setDataRowKey(newLabel);

                // do we even need to check this?
                if (oldName.equals(oldLabel)) {
                    columns[i].setName(newLabel);
                }
            }
        }

        if (typeOverrides != null) {
            for (int i = 0; i < len; i++) {

                String type = typeOverrides.get(columns[i].getName());

                if (type != null) {
                    columns[i].setJavaClass(type);
                }
            }
        }

        return this.columns;
    }

    protected ColumnDescriptor[] columnsFromResultSet() throws SQLException {

        int len = resultSetMetadata.getColumnCount();
        if (len == 0) {
            throw new CayenneRuntimeException("No columns in ResultSet.");
        }

        ColumnDescriptor[] columns = new ColumnDescriptor[len];

        for (int i = 0; i < len; i++) {

            int position = i + 1;
            String name = resultSetMetadata.getColumnLabel(position);
            if (name == null || name.length() == 0) {
                name = resultSetMetadata.getColumnName(position);

                if (name == null || name.length() == 0) {
                    name = "column_" + position;
                }
            }

            if (caseTransformer != null) {
                name = (String) caseTransformer.transform(name);
            }

            int jdbcType = resultSetMetadata.getColumnType(position);

            String javaClass = null;
            if (typeOverrides != null) {
                javaClass = typeOverrides.get(name);
            }

            if (javaClass == null) {
                javaClass = resultSetMetadata.getColumnClassName(position);
            }

            columns[i] = new ColumnDescriptor(name, jdbcType, javaClass);
        }

        return columns;
    }

    /**
     * Sets an explicit set of columns. Note that the array passed as an argument can
     * later be modified by the build to enforce column capitalization policy and columns
     * Java types overrides.
     */
    public RowDescriptorBuilder setColumns(ColumnDescriptor[] columns) {
        this.columns = columns;
        return this;
    }

    public RowDescriptorBuilder setResultSet(ResultSet resultSet) throws SQLException {
        this.resultSetMetadata = resultSet.getMetaData();
        return this;
    }

    public RowDescriptorBuilder useLowercaseColumnNames() {
        this.caseTransformer = LOWERCASE_TRANSFORMER;
        return this;
    }

    public RowDescriptorBuilder useUppercaseColumnNames() {
        this.caseTransformer = UPPERCASE_TRANSFORMER;
        return this;
    }

    public RowDescriptorBuilder overrideColumnType(String columnName, String type) {

        if (typeOverrides == null) {
            typeOverrides = new HashMap<String, String>();
        }
        
        typeOverrides.put(columnName, type);
        return this;
    }
    
    public boolean isOverriden(String columnName) {
        return typeOverrides != null && typeOverrides.containsKey(columnName);
    }
}
