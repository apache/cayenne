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
import org.apache.cayenne.reflect.ClassDescriptor;

import java.sql.ResultSet;

/**
 * A {@link RowReader} that materializes a contiguous run of result-set columns starting at a given offset.
 */
class OffsetRowReader implements RowReader<DataRow> {

    protected final String entityName;
    private final RSColumn[] columns;
    private final int offset;
    private final int mapCapacity;

    // possible inherotance
    static RowReader<DataRow> of(RSColumn[] columns, int offset, ClassDescriptor cd) {
        String entityName = cd != null ? cd.getEntity().getName() : null;
        return cd != null && cd.hasSubclasses()
                ? new InheritanceAwareOffsetRowReader(columns, offset, entityName, cd.getEntityInheritanceTree())
                : new OffsetRowReader(columns, offset, entityName);
    }

    // fixed entity name, no inheritance
    static RowReader<DataRow> of(RSColumn[] columns, int offset, String entityName) {
        return new OffsetRowReader(columns, offset, entityName);
    }

    // no entity name, no inheritance
    static RowReader<DataRow> of(RSColumn[] columns, int offset) {
        return new OffsetRowReader(columns, offset, null);
    }

    private OffsetRowReader(RSColumn[] columns, int offset, String entityName) {
        this.columns = columns;
        this.offset = offset;
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
                Object val = column.reader().materializeObject(resultSet, offset + i + 1, column.rsType());
                dataRow.put(column.dataRowName(), val);
            }

            dataRow.setEntityName(resolveEntityName(dataRow));

            return dataRow;
        } catch (CayenneRuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CayenneRuntimeException("Exception materializing column.", ex);
        }
    }

    protected String resolveEntityName(DataRow dataRow) {
        return entityName;
    }

    private static class InheritanceAwareOffsetRowReader extends OffsetRowReader {

        private final EntityInheritanceTree entityInheritanceTree;

        InheritanceAwareOffsetRowReader(RSColumn[] columns, int startIndex, String entityName,
                                        EntityInheritanceTree entityInheritanceTree) {
            super(columns, startIndex, entityName);
            this.entityInheritanceTree = entityInheritanceTree;
        }

        @Override
        protected String resolveEntityName(DataRow dataRow) {
            ObjEntity entity = entityInheritanceTree.entityMatchingRow(dataRow);
            return entity != null ? entity.getName() : entityName;
        }
    }
}
