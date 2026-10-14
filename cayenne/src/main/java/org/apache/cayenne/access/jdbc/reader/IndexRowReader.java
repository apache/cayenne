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

package org.apache.cayenne.access.jdbc.reader;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.DataRowLayout;
import org.apache.cayenne.access.jdbc.RSColumn;

import java.sql.ResultSet;

/**
 * A {@link RowReader} that materializes a DataRow from a set of columns read at explicit, possibly non-contiguous,
 * result-set positions. Unlike {@link OffsetRowReader}, each column carries its own position, so the columns need not
 * form a contiguous run.
 */
class IndexRowReader implements RowReader<DataRow> {

    private final RSColumn[] columns;
    private final DataRowLayout layout;
    private final int[] jdbcIndexes;
    private final String entityName;
    private final long version;

    static RowReader<DataRow> of(RSColumn[] columns, int[] jdbcIndexes, String entityName, long version) {
        return new IndexRowReader(columns, jdbcIndexes, entityName, version);
    }

    private IndexRowReader(RSColumn[] columns, int[] jdbcIndexes, String entityName, long version) {
        this.columns = columns;
        this.layout = FullRowReader.layout(columns);
        this.jdbcIndexes = jdbcIndexes;
        this.entityName = entityName;
        this.version = version;
    }

    @Override
    public DataRow readRow(ResultSet resultSet) {
        Object[] values = new Object[columns.length];
        try {
            for (int i = 0; i < columns.length; i++) {
                RSColumn column = columns[i];
                values[i] = column.reader().materializeObject(resultSet, jdbcIndexes[i], column.rsType());
            }
            return DataRow.of(entityName, layout, values, version);
        } catch (CayenneRuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CayenneRuntimeException("Exception materializing column.", ex);
        }
    }
}
