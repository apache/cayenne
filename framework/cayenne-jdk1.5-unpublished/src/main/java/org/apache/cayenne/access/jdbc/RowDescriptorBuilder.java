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

        ColumnDescriptor[] columnsForRD;

        if (this.resultSetMetadata != null) {
            // do merge between explicitly-set columns and ResultSetMetadata
            // explicitly-set columns take precedence
            columnsForRD = mergeResultSetAndPresetColumns();
        }
        else if (this.columns != null) {
            // use explicitly-set columns
            columnsForRD = this.columns;
        }
        else {
            throw new IllegalStateException(
                    "Can't build RowDescriptor, both 'columns' and 'resultSetMetadata' are null");
        }

        performTransformAndTypeOverride(columnsForRD);
        ExtendedType[] converters = new ExtendedType[columnsForRD.length];
        for (int i = 0; i < columnsForRD.length; i++) {
            converters[i] = typeMap.getRegisteredType(columnsForRD[i].getJavaClass());
        }

        return new RowDescriptor(columnsForRD, converters);
    }

    /**
     * @return array of columns for ResultSet with overriding ColumnDescriptors from
     *         'columns' Note: column will be overlooked, if column name is empty
     */
    protected ColumnDescriptor[] mergeResultSetAndPresetColumns() throws SQLException {

        int rsLen = resultSetMetadata.getColumnCount();
        if (rsLen == 0) {
            throw new CayenneRuntimeException("'ResultSetMetadata' is empty.");
        }

        int columnLen = (columns != null) ? columns.length : 0;

        if (rsLen < columnLen) {
            throw new CayenneRuntimeException(
                    "'ResultSetMetadata' has less elements then 'columns'.");
        }
        else if (rsLen == columnLen) {
            // 'columns' contains ColumnDescriptor for every column
            // in resultSetMetadata. This return is for optimization.
            return columns;
        }

        ColumnDescriptor[] rsColumns = new ColumnDescriptor[rsLen];

        int outputLen = 0;
        for (int i = 0; i < rsLen; i++) {
            String rowkey = resolveDataRowKeyFromResultSet(i + 1);
            
            // resolve column descriptor from 'columns' or create new
            rsColumns[outputLen] = getColumnDescriptor(rowkey, columns, i + 1);
            outputLen++;
        }
        if (outputLen < rsLen) {
            // cut ColumnDescriptor array
            ColumnDescriptor[] rsColumnsCut = new ColumnDescriptor[outputLen];
            System.arraycopy(rsColumns, 0, rsColumnsCut, 0, outputLen);
            return rsColumnsCut;
        }

        return rsColumns;
    }

    /**
     * @return ColumnDescriptor from columnArray, if columnArray contains descriptor for
     *         this column, or new ColumnDescriptor.
     */
    private ColumnDescriptor getColumnDescriptor(
            String rowKey,
            ColumnDescriptor[] columnArray,
            int position) throws SQLException {
        int len = (columnArray != null) ? columnArray.length : 0;
        // go through columnArray to find ColumnDescriptor for specified column
        for (int i = 0; i < len; i++) {
            if (columnArray[i] != null) {
                String columnRowKey = columnArray[i].getDataRowKey();
                
                // TODO: andrus, 10/14/2009 - 'equalsIgnoreCase' check can result in
                // subtle bugs in DBs with case-sensitive column names (or when quotes are
                // used to force case sensitivity). Alternatively using 'equals' may miss
                // columns in case-insensitive situations.
                if (columnRowKey != null && columnRowKey.equalsIgnoreCase(rowKey)) {
                    return columnArray[i];
                }
            }
        }
        // columnArray doesn't contain ColumnDescriptor for specified column
        return new ColumnDescriptor(rowKey, resultSetMetadata, position);
    }

    /**
     * Return not empty string with ColumnLabel or ColumnName or "column_" + position for
     * for specified (by it's position) column in ResultSetMetaData.
     */
    private String resolveDataRowKeyFromResultSet(int position) throws SQLException {
        String name = resultSetMetadata.getColumnLabel(position);
        if (name == null || name.length() == 0) {
            name = resultSetMetadata.getColumnName(position);
            if (name == null) {
                name = "";
            }
        }
        return name;
    }

    private void performTransformAndTypeOverride(ColumnDescriptor[] columnArray) {
        int len = columnArray.length;

        if (caseTransformer != null) {
            for (int i = 0; i < len; i++) {

                columnArray[i].setDataRowKey((String) caseTransformer
                        .transform(columnArray[i].getDataRowKey()));
                columnArray[i].setName((String) caseTransformer.transform(columnArray[i]
                        .getName()));
            }
        }
        if (typeOverrides != null) {
            for (int i = 0; i < len; i++) {
                String type = typeOverrides.get(columnArray[i].getName());
                if (type != null) {
                    columnArray[i].setJavaClass(type);
                }
            }
        }
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
