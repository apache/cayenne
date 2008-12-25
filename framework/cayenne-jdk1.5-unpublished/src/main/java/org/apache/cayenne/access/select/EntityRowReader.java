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
package org.apache.cayenne.access.select;

import java.sql.ResultSet;
import java.util.List;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.util.Util;

/**
 * A RowReader for a single entity result segment unaware of inheritance.
 * 
 * @since 3.0
 */
class EntityRowReader implements RowReader<Object> {

    private ExtendedType[] converters;
    private String[] dataRowKeys;
    private int[] jdbcTypes;
    private int[] columnIndexes;
    private String entityName;
    private int mapCapacity;

    EntityRowReader(String entityName, List<EntitySelectColumn> columns) {
        int len = columns.size();

        this.mapCapacity = (int) Math.ceil(len / 0.75);
        this.converters = new ExtendedType[len];
        this.dataRowKeys = new String[len];
        this.jdbcTypes = new int[len];
        this.columnIndexes = new int[len];

        for (int i = 0; i < len; i++) {
            converters[i] = columns.get(i).getConverter();
            dataRowKeys[i] = columns.get(i).getDataRowKey();
            jdbcTypes[i] = columns.get(i).getJdbcType();
            columnIndexes[i] = i + 1;
        }
    }

    void setColumnOffset(int offset) {
        for (int i = 0; i < columnIndexes.length; i++) {
            columnIndexes[i] = i + offset + 1;
        }
    }

    public Object readRow(ResultSet resultSet) throws CayenneException {
        DataRow row = new DataRow(mapCapacity);
        row.setEntityName(entityName);

        int len = converters.length;

        try {
            for (int i = 0; i < len; i++) {
                Object value = converters[i].materializeObject(
                        resultSet,
                        columnIndexes[i],
                        jdbcTypes[i]);
                row.put(dataRowKeys[i], value);
            }
        }
        catch (CayenneException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new CayenneException("Exception materializing row", Util
                    .unwindException(ex));
        }

        return row;
    }
}
