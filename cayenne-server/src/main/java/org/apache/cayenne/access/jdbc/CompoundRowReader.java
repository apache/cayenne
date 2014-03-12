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

/**
 * A row reader for complex result sets resolved as object arrays.
 * 
 * @since 3.0
 */
class CompoundRowReader implements RowReader<Object[]> {

    private RowReader<?>[] readers;

    CompoundRowReader(int width) {
        this.readers = new RowReader[width];
    }

    void addRowReader(int pos, RowReader<?> reader) {
        this.readers[pos] = reader;
    }

    @Override
    public Object[] readRow(ResultSet resultSet) {

        int width = readers.length;
        Object[] row = new Object[width];

        for (int i = 0; i < width; i++) {
            row[i] = readers[i].readRow(resultSet);
        }

        return row;
    }

    @Override
    public void setPostProcessor(DataRowPostProcessor postProcessor) {
        for (RowReader<?> reader : readers) {
            reader.setPostProcessor(postProcessor);
        }
    }
}
