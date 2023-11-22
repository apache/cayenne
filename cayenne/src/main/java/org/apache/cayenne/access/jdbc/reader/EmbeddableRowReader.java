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

package org.apache.cayenne.access.jdbc.reader;

import java.sql.ResultSet;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.jdbc.RowDescriptor;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.query.EmbeddableResultSegment;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.util.Util;

/**
 * @since 4.2
 */
class EmbeddableRowReader implements RowReader<DataRow> {

    private final int startIndex;
    private final int mapCapacity;
    private final ExtendedType[] converters;
    private final String[] labels;
    private final int[] types;

    EmbeddableRowReader(RowDescriptor descriptor, QueryMetadata queryMetadata, EmbeddableResultSegment segment) {
        int segmentWidth = segment.getFields().size();
        this.startIndex = segment.getColumnOffset();
        this.converters = new ExtendedType[segmentWidth];
        this.types = new int[segmentWidth];
        this.labels = new String[segmentWidth];

        ExtendedType[] converters = descriptor.getConverters();
        ColumnDescriptor[] columns = descriptor.getColumns();
        for (int i = 0; i < segmentWidth; i++) {
            this.converters[i] = converters[startIndex + i];
            types[i] = columns[startIndex + i].getJdbcType();
            labels[i] = segment.getFields().get(columns[startIndex +i].getName());
        }
        this.mapCapacity = (int) Math.ceil(segmentWidth / 0.75);
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
            return row;
        } catch (CayenneRuntimeException cex) {
            // rethrow unmodified
            throw cex;
        } catch (Exception otherex) {
            throw new CayenneRuntimeException("Exception materializing column.", Util.unwindException(otherex));
        }
    }
}
