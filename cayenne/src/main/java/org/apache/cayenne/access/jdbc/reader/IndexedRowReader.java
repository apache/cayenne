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
 * A {@link RowReader} that materializes a subset of the result columns.
 */
class IndexedRowReader implements RowReader<DataRow> {

    private final String entityName;
    private final RSColumn[] columns;
    private final int[] indices;
    private final int mapCapacity;

    public IndexedRowReader(RSColumn[] columns, String entityName, int[] indices) {
        this.columns = columns;
        this.entityName = entityName;
        this.mapCapacity = (int) Math.ceil(indices.length / 0.75);
        this.indices = indices;
    }

    @Override
    public DataRow readRow(ResultSet resultSet) {
        DataRow dataRow = new DataRow(mapCapacity);

        try {

            for (int index : indices) {
                RSColumn column = columns[index];
                // jdbc column indexes start from 1, not 0 unlike everywhere else
                Object val = column.reader().materializeObject(resultSet, index + 1, column.rsType());
                dataRow.put(column.dataRowName(), val);
            }

            dataRow.setEntityName(entityName);

            return dataRow;
        } catch (CayenneRuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CayenneRuntimeException("Exception materializing id column.", ex);
        }
    }
}
