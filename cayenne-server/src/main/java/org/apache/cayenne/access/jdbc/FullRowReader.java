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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.util.Util;

/**
 * @since 3.0
 */
class FullRowReader extends BaseRowReader<DataRow> {

    int mapCapacity;

    FullRowReader(RowDescriptor descriptor, QueryMetadata queryMetadata, DataRowPostProcessor postProcessor) {
        super(descriptor, queryMetadata, postProcessor);
        this.mapCapacity = (int) Math.ceil((descriptor.getWidth()) / 0.75);
    }

    @Override
    public DataRow readRow(ResultSet resultSet) {
        try {
            DataRow dataRow = new DataRow(mapCapacity);

            int resultWidth = labels.length;

            // process result row columns,
            for (int i = 0; i < resultWidth; i++) {
                // note: jdbc column indexes start from 1, not 0 unlike
                // everywhere else
                Object val = converters[i].materializeObject(resultSet, i + 1, types[i]);
                dataRow.put(labels[i], val);
            }

            postprocessRow(resultSet, dataRow);

            return dataRow;
        } catch (CayenneRuntimeException cex) {
            // rethrow unmodified
            throw cex;
        } catch (Exception otherex) {
            throw new CayenneRuntimeException("Exception materializing column.", Util.unwindException(otherex));
        }
    }

    void postprocessRow(ResultSet resultSet, DataRow dataRow) throws Exception {
        if (postProcessor != null) {
            postProcessor.postprocessRow(resultSet, dataRow);
        }

        dataRow.setEntityName(entityName);
    }
}
