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
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.util.Util;

/**
 * @since 3.0
 */
class IdRowReader<T> extends BaseRowReader<T> {

    protected int[] pkIndices;

    public IdRowReader(RowDescriptor descriptor, QueryMetadata queryMetadata, DataRowPostProcessor postProcessor) {
        super(descriptor, queryMetadata, postProcessor);

        DbEntity dbEntity = queryMetadata.getDbEntity();
        if (dbEntity == null) {
            throw new CayenneRuntimeException("Null root DbEntity, can't index PK");
        }

        int len = dbEntity.getPrimaryKeys().size();

        // sanity check
        if (len == 0) {
            throw new CayenneRuntimeException("Root DBEntity has no PK defined: " + dbEntity);
        }

        int[] pk = new int[len];
        ColumnDescriptor[] columns = descriptor.getColumns();
        for (int i = 0, j = 0; i < columns.length; i++) {
            DbAttribute a = dbEntity.getAttribute(columns[i].getName());
            if (a != null && a.isPrimaryKey()) {
                pk[j++] = i;
            }
        }

        this.pkIndices = pk;
    }

    @Override
    public T readRow(ResultSet resultSet) {
        try {
            if (pkIndices.length == 1) {
                return readSingleId(resultSet);
            } else {
                return readIdMap(resultSet);
            }
        } catch (CayenneRuntimeException cex) {
            // rethrow unmodified
            throw cex;
        } catch (Exception otherex) {
            throw new CayenneRuntimeException("Exception materializing id column.", Util.unwindException(otherex));
        }
    }

    private T readSingleId(ResultSet resultSet) throws Exception {

        // dereference column index
        int index = pkIndices[0];

        // note: jdbc column indexes start from 1, not 0 as in arrays
        @SuppressWarnings("unchecked")
        T val = (T) converters[index].materializeObject(resultSet, index + 1, types[index]);

        // note that postProcessor overrides are not applied. ID mapping must be
        // the
        // same across inheritance hierarchy, so overrides do not make sense.
        return val;
    }

    @SuppressWarnings("unchecked")
    private T readIdMap(ResultSet resultSet) throws Exception {

        DataRow idRow = new DataRow(2);
        idRow.setEntityName(entityName);
        int len = pkIndices.length;

        for (int i = 0; i < len; i++) {

            // dereference column index
            int index = pkIndices[i];

            // note: jdbc column indexes start from 1, not 0 as in arrays
            Object val = converters[index].materializeObject(resultSet, index + 1, types[index]);
            idRow.put(labels[index], val);
        }

        if (postProcessor != null) {
            postProcessor.postprocessRow(resultSet, idRow);
        }

        return (T) idRow;
    }
}
