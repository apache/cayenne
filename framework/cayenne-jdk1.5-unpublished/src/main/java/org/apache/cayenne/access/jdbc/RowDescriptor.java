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
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeMap;

/**
 * A descriptor of a result row obtained from a database.
 * 
 * @since 1.2
 */
public class RowDescriptor {

    protected ColumnDescriptor[] columns;
    protected ExtendedType[] converters;

    /**
     * Creates an empty RowDescriptor. Intended mainly for testing and use by subclasses.
     */
    protected RowDescriptor() {

    }

    /**
     * Creates a fully initialized RowDescriptor.
     * 
     * @since 3.0
     */
    public RowDescriptor(ColumnDescriptor[] columns, ExtendedType[] converters) {
        this.columns = columns;
        this.converters = converters;
    }

    /**
     * Creates a RowDescriptor for an array of columns.
     * 
     * @deprecated since 3.0 use {@link RowDescriptorBuilder}.
     */
    public RowDescriptor(ColumnDescriptor[] columns, ExtendedTypeMap types) {
        this.columns = columns;
        indexTypes(types);
    }

    /**
     * Creates new RowDescriptor using ResultSet metadata to determine the columns.
     * 
     * @deprecated since 3.0 use {@link RowDescriptorBuilder}.
     */
    public RowDescriptor(ResultSet resultSet, ExtendedTypeMap types) {
        this(resultSet, types, null);
    }

    /**
     * Creates new RowDescriptor using ResultSet metadata to determine the columns. Note
     * that if javaTypeOverrides array is null, default JDBC to Java types mapping is
     * used.
     * 
     * @deprecated since 3.0 use {@link RowDescriptorBuilder}.
     */
    public RowDescriptor(ResultSet resultSet, ExtendedTypeMap types, Map javaTypeOverrides) {

        initFromResultSet(resultSet);

        if (javaTypeOverrides != null) {
            overrideJavaTypes(javaTypeOverrides);
        }

        indexTypes(types);
    }

    /**
     * Initializes converters for columns.
     * 
     * @deprecated since 3.0 use {@link RowDescriptorBuilder}.
     */
    protected void indexTypes(ExtendedTypeMap types) {
        this.converters = new ExtendedType[columns.length];
        for (int i = 0; i < columns.length; i++) {
            converters[i] = types.getRegisteredType(columns[i].getJavaClass());
        }
    }

    /**
     * Builds columns list from ResultSet metadata.
     * 
     * @deprecated since 3.0 use {@link RowDescriptorBuilder}.
     */
    protected void initFromResultSet(ResultSet resultSet) {
        try {
            ResultSetMetaData md = resultSet.getMetaData();
            int len = md.getColumnCount();
            if (len == 0) {
                throw new CayenneRuntimeException("No columns in ResultSet.");
            }

            this.columns = new ColumnDescriptor[len];

            for (int i = 0; i < len; i++) {
                columns[i] = new ColumnDescriptor(md, i + 1);
            }
        }
        catch (SQLException sqex) {
            throw new CayenneRuntimeException("Error reading metadata.", sqex);
        }
    }

    /**
     * Overrides Java types of result columns. Keys in the map must correspond to the
     * names of the columns.
     * 
     * @deprecated since 3.0 use {@link RowDescriptorBuilder}.
     */
    protected void overrideJavaTypes(Map overrides) {

        for (ColumnDescriptor column : columns) {
            String type = (String) overrides.get(column.getName());

            if (type != null) {
                column.setJavaClass(type);
            }
        }
    }

    /**
     * Returns a number of columns in a row.
     */
    public int getWidth() {
        return columns.length;
    }

    /**
     * Returns column descriptors.
     */
    public ColumnDescriptor[] getColumns() {
        return columns;
    }

    /**
     * Returns extended types for columns.
     */
    public ExtendedType[] getConverters() {
        return converters;
    }
}
