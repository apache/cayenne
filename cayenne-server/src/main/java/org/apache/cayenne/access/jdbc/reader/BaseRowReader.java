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

import java.sql.ResultSet;

import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.jdbc.RowDescriptor;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.QueryMetadata;

/**
 * @since 3.0
 */
abstract class BaseRowReader<T> implements RowReader<T> {

    ExtendedType[] converters;
    String[] labels;
    int[] types;
    DataRowPostProcessor postProcessor;
    String entityName;

    BaseRowReader(RowDescriptor descriptor, QueryMetadata queryMetadata, DataRowPostProcessor postProcessor) {
        ObjEntity rootObjEntity = queryMetadata.getObjEntity();
        if (rootObjEntity != null) {
            this.entityName = rootObjEntity.getName();
        }

        this.postProcessor = postProcessor;
        this.converters = descriptor.getConverters();

        ColumnDescriptor[] columns = descriptor.getColumns();
        int width = columns.length;
        this.labels = new String[width];
        this.types = new int[width];

        for (int i = 0; i < width; i++) {
            labels[i] = columns[i].getDataRowKey();
            types[i] = columns[i].getJdbcType();
        }
    }

    @Override
    public abstract T readRow(ResultSet resultSet);

}
