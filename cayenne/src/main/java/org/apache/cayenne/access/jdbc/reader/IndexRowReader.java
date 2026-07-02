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
import org.apache.cayenne.access.jdbc.RSColumn;

import java.sql.ResultSet;

/**
 * A {@link RowReader} that materializes a DataRow from a set of columns read at explicit, possibly non-contiguous,
 * result-set positions. Unlike {@link OffsetRowReader}, each column carries its own position, so the columns need not
 * form a contiguous run.
 */
class IndexRowReader implements RowReader<DataRow> {

    private final RSColumn[] columns;
    private final int[] jdbcIndexes;
    private final String entityName;
    private final int mapCapacity;

    static RowReader<DataRow> of(RSColumn[] columns, int[] jdbcIndexes, String entityName) {
        return new IndexRowReader(columns, jdbcIndexes, entityName);
    }

    private IndexRowReader(RSColumn[] columns, int[] jdbcIndexes, String entityName) {
        this.columns = columns;
        this.jdbcIndexes = jdbcIndexes;
        this.entityName = entityName;
        this.mapCapacity = (int) Math.ceil(columns.length / 0.75);
    }

    @Override
    public DataRow readRow(ResultSet resultSet) {
        DataRow dataRow = new DataRow(mapCapacity);

        try {
            for (int i = 0; i < columns.length; i++) {
                RSColumn column = columns[i];
                Object val = column.reader().materializeObject(resultSet, jdbcIndexes[i], column.rsType());
                dataRow.put(column.dataRowName(), val);
            }

            dataRow.setEntityName(entityName);
            return dataRow;
        } catch (CayenneRuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CayenneRuntimeException("Exception materializing column.", ex);
        }
    }
}
