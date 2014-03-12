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
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.util.Util;

/**
 * @since 3.0
 */
class EntityRowReader implements RowReader<DataRow> {

    private ExtendedType[] converters;
    private String[] labels;
    private int[] types;

    String entityName;
    private int mapCapacity;
    private int startIndex;

    DataRowPostProcessor postProcessor;

    EntityRowReader(RowDescriptor descriptor, EntityResultSegment segmentMetadata, DataRowPostProcessor postProcessor) {

        this.postProcessor = postProcessor;

        ClassDescriptor classDescriptor = segmentMetadata.getClassDescriptor();

        if (classDescriptor != null) {
            this.entityName = classDescriptor.getEntity().getName();
        }

        int segmentWidth = segmentMetadata.getFields().size();
        this.startIndex = segmentMetadata.getColumnOffset();
        this.converters = new ExtendedType[segmentWidth];
        this.types = new int[segmentWidth];
        this.labels = new String[segmentWidth];

        ExtendedType[] converters = descriptor.getConverters();
        ColumnDescriptor[] columns = descriptor.getColumns();
        for (int i = 0; i < segmentWidth; i++) {
            this.converters[i] = converters[startIndex + i];
            types[i] = columns[startIndex + i].getJdbcType();

            // query translator may change the order of fields compare to the
            // entity
            // result, so figure out DataRow labels by doing reverse lookup of
            // RowDescriptor labels...
            if (columns[startIndex + i].getDataRowKey().contains(".")) {
                // if the dataRowKey contains ".", it is prefetched column and
                // we can use
                // it instead of search the name by alias
                labels[i] = columns[startIndex + i].getDataRowKey();
            } else {
                labels[i] = segmentMetadata.getColumnPath(columns[startIndex + i].getDataRowKey());
            }
        }
    }

    @Override
    public DataRow readRow(ResultSet resultSet) {

        try {
            DataRow row = new DataRow(mapCapacity);
            int len = converters.length;

            for (int i = 0; i < len; i++) {

                // note: jdbc column indexes start from 1, not 0 as in arrays
                Object val = converters[i].materializeObject(resultSet, startIndex + i + 1, types[i]);
                row.put(labels[i], val);
            }

            postprocessRow(resultSet, row);

            return row;
        } catch (CayenneRuntimeException cex) {
            // rethrow unmodified
            throw cex;
        } catch (Exception otherex) {
            throw new CayenneRuntimeException("Exception materializing id column.", Util.unwindException(otherex));
        }
    }

    void postprocessRow(ResultSet resultSet, DataRow dataRow) throws Exception {
        if (postProcessor != null) {
            postProcessor.postprocessRow(resultSet, dataRow);
        }

        dataRow.setEntityName(entityName);
    }
}
