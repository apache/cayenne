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

import org.apache.cayenne.access.types.ExtendedType;

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
