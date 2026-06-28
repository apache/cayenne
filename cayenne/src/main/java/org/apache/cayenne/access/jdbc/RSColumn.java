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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * A descriptor of a ResultSet column.
 *
 * @since 5.0
 */
public record RSColumn(
        String name,
        String dataRowKey,
        int jdbcType,
        ExtendedType<?> type,
        DbAttribute attribute) {

    public RSColumn {
        if (dataRowKey == null) {
            dataRowKey = name;
        }
    }

    /**
     * Returns true if another object is a ColumnDescriptor with the same name and data row key. Other fields are
     * ignored in the equality test.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RSColumn rhs)) {
            return false;
        }

        return Objects.equals(name, rhs.name)
                && Objects.equals(dataRowKey, rhs.dataRowKey);
    }

    /**
     * @since 1.2
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, dataRowKey);
    }

    /**
     * Creates a {@link RowBuilder} that assembles the array of {@link RSColumn}s describing a result row.
     *
     * @since 5.0
     */
    public static RowBuilder rowBuilder() {
        return new RowBuilder();
    }

    public static class RowBuilder {

        private static final Logger LOGGER = LoggerFactory.getLogger(RowBuilder.class);

        private static final Function<String, String> UPPERCASE_TRANSFORMER = input ->
                input != null ? input.toUpperCase() : null;

        private static final Function<String, String> LOWERCASE_TRANSFORMER = input ->
                input != null ? input.toLowerCase() : null;

        private RSColumn[] columns;
        private ResultSetMetaData resultSetMetadata;
        private Function<String, String> caseTransformer;
        private Map<String, String> typeOverrides;
        private boolean mergeColumnsWithRsMetadata;
        private boolean validateDuplicateColumnNames;

        /**
         * Returns the array of {@link RSColumn}s describing the result row, with an {@link ExtendedType}
         * resolved for each column.
         */
        public RSColumn[] build(ExtendedTypeMap typeMap) throws SQLException, IllegalStateException {

            RSColumn[] columnsForRD;

            if (this.resultSetMetadata != null) {
                // do merge between explicitly-set columns and ResultSetMetadata
                // explicitly-set columns take precedence
                columnsForRD = mergeResultSetAndPresetColumns(typeMap);
            } else if (this.columns != null) {
                // use explicitly-set columns
                columnsForRD = this.columns;
            } else {
                throw new IllegalStateException(
                        "Can't build row descriptor, both 'columns' and 'resultSetMetadata' are null");
            }

            return performTransformAndTypeOverride(columnsForRD, typeMap);
        }

        /**
         * @return array of columns for ResultSet with overriding ColumnDescriptors from
         * 'columns' Note: column will be overlooked, if column name is empty
         */
        protected RSColumn[] mergeResultSetAndPresetColumns(ExtendedTypeMap typeMap) throws SQLException {

            int rsLen = resultSetMetadata.getColumnCount();
            if (rsLen == 0) {
                throw new CayenneRuntimeException("'ResultSetMetadata' is empty.");
            }

            int columnLen = (columns != null) ? columns.length : 0;

            if (mergeColumnsWithRsMetadata && rsLen != columnLen) {
                throw new CayenneRuntimeException("Size of 'ResultSetMetadata' not equals to size of 'columns'.");
            } else if (rsLen < columnLen) {
                throw new CayenneRuntimeException("'ResultSetMetadata' has less elements then 'columns'.");
            } else if (rsLen == columnLen && !mergeColumnsWithRsMetadata) {
                // 'columns' contains ColumnDescriptor for every column
                // in resultSetMetadata. This return is for optimization.
                return columns;
            }

            RSColumn[] rsColumns = new RSColumn[rsLen];
            List<String> duplicates = null;
            Set<String> uniqueNames = null;

            if (validateDuplicateColumnNames) {
                duplicates = new ArrayList<>();
                uniqueNames = new HashSet<>();
            }

            int outputLen = 0;
            for (int i = 0; i < rsLen; i++) {
                String rowkey = resolveDataRowKeyFromResultSet(i + 1);

                // resolve column descriptor from 'columns' or create new
                RSColumn column = getOrCreateColumn(rowkey, columns, i + 1, typeMap);

                // validate uniqueness of names
                if (validateDuplicateColumnNames) {
                    if (!uniqueNames.add(column.dataRowKey())) {
                        duplicates.add(column.dataRowKey());
                    }
                }
                rsColumns[outputLen] = column;
                outputLen++;
            }

            if (validateDuplicateColumnNames && !duplicates.isEmpty()) {
                LOGGER.warn(
                        "Found duplicated columns '{}' in row descriptor. This can lead to errors when converting result to persistent objects.",
                        String.join("', '", duplicates));
            }

            if (outputLen < rsLen) {
                // cut ColumnDescriptor array
                RSColumn[] rsColumnsCut = new RSColumn[outputLen];
                System.arraycopy(rsColumns, 0, rsColumnsCut, 0, outputLen);
                return rsColumnsCut;
            }

            return rsColumns;
        }

        private RSColumn getOrCreateColumn(
                String rowKey,
                RSColumn[] columnArray,
                int position,
                ExtendedTypeMap typeMap) throws SQLException {
            int len = (columnArray != null) ? columnArray.length : 0;
            // go through columnArray to find ColumnDescriptor for specified column
            for (int i = 0; i < len; i++) {
                if (columnArray[i] != null) {
                    if (mergeColumnsWithRsMetadata) {
                        return new RSColumn(rowKey, rowKey, resultSetMetadata.getColumnType(position), columnArray[position - 1].type(), null);
                    } else {
                        String columnRowKey = columnArray[i].dataRowKey();

                        // TODO: andrus, 10/14/2009 - 'equalsIgnoreCase' check can result in
                        // subtle bugs in DBs with case-sensitive column names (or when quotes are
                        // used to force case sensitivity). Alternatively using 'equals' may miss
                        // columns in case-insensitive situations.
                        if (columnRowKey != null && columnRowKey.equalsIgnoreCase(rowKey)) {
                            return columnArray[i];
                        }
                    }
                }
            }
            // columnArray doesn't contain ColumnDescriptor for specified column
            int jdbcType = resultSetMetadata.getColumnType(position);
            ExtendedType<?> type = typeMap.getRegisteredType(TypesMapping.getJavaBySqlType(jdbcType));
            return new RSColumn(rowKey, rowKey, jdbcType, type, null);
        }

        // Return a non-empty string with ColumnLabel or ColumnName or "column_<pos>" for positional column
        // in ResultSetMetaData.
        private String resolveDataRowKeyFromResultSet(int position) throws SQLException {
            String name = resultSetMetadata.getColumnLabel(position);
            if (name == null || name.isEmpty()) {
                name = resultSetMetadata.getColumnName(position);
                if (name == null) {
                    name = "";
                }
            }
            return name;
        }

        private RSColumn[] performTransformAndTypeOverride(RSColumn[] columnArray, ExtendedTypeMap typeMap) {
            if (caseTransformer == null && typeOverrides == null) {
                return columnArray;
            }

            RSColumn[] result = new RSColumn[columnArray.length];
            for (int i = 0; i < columnArray.length; i++) {
                RSColumn column = columnArray[i];

                String name = column.name();
                String dataRowKey = column.dataRowKey();
                ExtendedType<?> type = column.type();

                if (caseTransformer != null) {
                    name = caseTransformer.apply(name);
                    dataRowKey = caseTransformer.apply(dataRowKey);
                }

                // type overrides are keyed by the (possibly transformed) column name
                if (typeOverrides != null) {
                    String javaClass = typeOverrides.get(name);
                    if (javaClass != null) {
                        type = typeMap.getRegisteredType(javaClass);
                    }
                }

                result[i] = new RSColumn(name, dataRowKey, column.jdbcType(), type, column.attribute());
            }
            return result;
        }

        /**
         * Sets an explicit set of columns. The builder may replace these with new instances to enforce the column
         * capitalization policy and column Java type overrides.
         */
        public RowBuilder columns(RSColumn... columns) {
            this.columns = columns;
            return this;
        }

        public RowBuilder resultSet(ResultSet resultSet) throws SQLException {
            this.resultSetMetadata = resultSet.getMetaData();
            return this;
        }

        public RowBuilder useLowercaseColumnNames() {
            this.caseTransformer = LOWERCASE_TRANSFORMER;
            return this;
        }

        public RowBuilder useUppercaseColumnNames() {
            this.caseTransformer = UPPERCASE_TRANSFORMER;
            return this;
        }

        /**
         * Registers a Java type override for the named column. The first override registered for a given column wins:
         * subsequent calls for the same column are ignored, so callers can register higher-priority overrides first and
         * follow with fallback defaults without having to check what is already registered.
         */
        public RowBuilder overrideColumnType(String columnName, String type) {

            if (typeOverrides == null) {
                typeOverrides = new HashMap<>();
            }

            typeOverrides.putIfAbsent(columnName, type);
            return this;
        }

        public RowBuilder validateDuplicateColumnNames() {
            this.validateDuplicateColumnNames = true;
            return this;
        }

        public RowBuilder mergeColumnsWithRsMetadata() {
            this.mergeColumnsWithRsMetadata = true;
            return this;
        }
    }
}
