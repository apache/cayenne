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
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link RowReader} that materializes a contiguous run of result-set columns starting at a given offset.
 */
class OffsetRowReader implements RowReader<DataRow> {

    protected final String entityName;
    private final int mapCapacity;
    private final long version;
    private final RSColumn[] columns;
    private final int offset;

    // possible inheritance
    static OffsetRowReader of(RSColumn[] columns, int offset, ClassDescriptor cd, long version) {
        String entityName = cd != null ? cd.getEntity().getName() : null;
        return cd != null && cd.hasSubclasses()
                ? new InheritanceAwareOffsetRowReader(columns, offset, entityName, cd.getEntityInheritanceTree(),
                        version)
                : new OffsetRowReader(columns, offset, entityName, version);
    }

    // no entity name, no inheritance
    static OffsetRowReader of(RSColumn[] columns, int offset, long version) {
        return new OffsetRowReader(columns, offset, null, version);
    }

    private OffsetRowReader(RSColumn[] columns, int offset, String entityName, long version) {
        this.columns = columns;
        this.offset = offset;
        this.entityName = entityName;
        this.version = version;
        this.mapCapacity = (int) Math.ceil(columns.length / 0.75);
    }

    @Override
    public DataRow readRow(ResultSet resultSet) {
        HashMap<String, Object> values = new HashMap<>(mapCapacity);
        readColumns(resultSet, values);
        return new DataRow(entityName(values), values, version);
    }

    /**
     * Returns the entity name of a row with the given values.
     */
    protected String entityName(Map<String, Object> values) {
        return entityName;
    }

    private void readColumns(ResultSet resultSet, Map<String, Object> target) {
        int w = columns.length;

        try {
            for (int i = 0; i < w; i++) {
                RSColumn column = columns[i];
                // jdbc column indexes start from 1, not 0 unlike everywhere else
                Object val = column.reader().materializeObject(resultSet, offset + i + 1, column.rsType());
                target.put(column.dataRowName(), val);
            }
        } catch (CayenneRuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CayenneRuntimeException("Exception materializing column.", ex);
        }
    }

    private static class InheritanceAwareOffsetRowReader extends OffsetRowReader {

        private final EntityInheritanceTree entityInheritanceTree;

        private InheritanceAwareOffsetRowReader(
                RSColumn[] columns,
                int startIndex,
                String entityName,
                EntityInheritanceTree entityInheritanceTree,
                long version) {

            super(columns, startIndex, entityName, version);
            this.entityInheritanceTree = entityInheritanceTree;
        }

        @Override
        protected String entityName(Map<String, Object> values) {
            ObjEntity entity = entityInheritanceTree.entityMatchingRow(values);
            return entity != null ? entity.getName() : entityName;
        }
    }
}
