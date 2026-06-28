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
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.reflect.ClassDescriptor;

import java.sql.ResultSet;

/**
 * @since 3.0
 */
class FullRowReader implements RowReader<DataRow> {

    private final RSColumn[] columns;
    private final int mapCapacity;
    protected final String entityName;

    static RowReader<DataRow> of(RSColumn[] columns, QueryMetadata queryMetadata) {

        ObjEntity objEntity = queryMetadata.getObjEntity();
        String entityName = objEntity != null ? objEntity.getName() : null;

        ClassDescriptor classDescriptor = queryMetadata.getClassDescriptor();
        if (classDescriptor != null && classDescriptor.hasSubclasses()) {
            return new InheritanceAwareRowReader(columns, entityName, classDescriptor.getEntityInheritanceTree());
        }

        return new FullRowReader(columns, entityName);
    }

    public FullRowReader(RSColumn[] columns, String entityName) {
        this.columns = columns;
        this.entityName = entityName;
        this.mapCapacity = (int) Math.ceil(columns.length / 0.75);
    }

    @Override
    public DataRow readRow(ResultSet resultSet) {
        DataRow dataRow = new DataRow(mapCapacity);
        int w = columns.length;

        try {

            for (int i = 0; i < w; i++) {
                RSColumn column = columns[i];
                // jdbc column indexes start from 1, not 0 unlike everywhere else
                Object val = column.reader().materializeObject(resultSet, i + 1, column.rsType());
                dataRow.put(column.dataRowName(), val);
            }

            postprocessRow(resultSet, dataRow);

            return dataRow;
        } catch (CayenneRuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CayenneRuntimeException("Exception materializing column.", ex);
        }
    }

    protected void postprocessRow(ResultSet resultSet, DataRow dataRow) {
        dataRow.setEntityName(entityName);
    }

    private static class InheritanceAwareRowReader extends FullRowReader {

        private final EntityInheritanceTree entityInheritanceTree;

        InheritanceAwareRowReader(RSColumn[] columns, String entityName, EntityInheritanceTree entityInheritanceTree) {
            super(columns, entityName);
            this.entityInheritanceTree = entityInheritanceTree;
        }

        @Override
        protected void postprocessRow(ResultSet resultSet, DataRow dataRow) {
            ObjEntity entity = entityInheritanceTree.entityMatchingRow(dataRow);
            dataRow.setEntityName(entity != null ? entity.getName() : entityName);
        }
    }
}
