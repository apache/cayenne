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
import java.util.Map;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.query.EntityResultMetadata;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.util.Util;

/**
 * @since 3.0
 */
class EntityRowReader extends BaseRowReader<DataRow> {

    private int[] valueIndices;
    private int mapCapacity;

    EntityRowReader(RowDescriptor descriptor, QueryMetadata queryMetadata,
            EntityResultMetadata entityResult) {
        super(descriptor, queryMetadata);

        Map<String, String> fields = entityResult.getFields();

        this.mapCapacity = (int) Math.ceil((fields.size()) / 0.75);
        this.valueIndices = new int[fields.size()];

        ColumnDescriptor[] columns = descriptor.getColumns();
        for (int i = 0, j = 0; i < columns.length; i++) {
            if (fields.containsKey(columns[i].getName())) {
                valueIndices[j++] = i;
            }
        }

    }

    @Override
    public DataRow readRow(ResultSet resultSet) throws CayenneException {
        
        try {
            DataRow idRow = new DataRow(mapCapacity);
            idRow.setEntityName(entityName);
            int len = valueIndices.length;

            for (int i = 0; i < len; i++) {

                // dereference column index
                int index = valueIndices[i];

                // note: jdbc column indexes start from 1, not 0 as in arrays
                Object val = converters[index].materializeObject(
                        resultSet,
                        index + 1,
                        types[index]);
                idRow.put(labels[index], val);
            }

            if (postProcessor != null) {
                postProcessor.postprocessRow(resultSet, idRow);
            }

            return idRow;
        }
        catch (CayenneException cex) {
            // rethrow unmodified
            throw cex;
        }
        catch (Exception otherex) {
            throw new CayenneException("Exception materializing id column.", Util
                    .unwindException(otherex));
        }
    }
}
