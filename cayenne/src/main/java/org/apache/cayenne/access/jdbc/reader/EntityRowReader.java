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
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.util.Util;

import java.sql.ResultSet;

/**
 * @since 3.0
 */
class EntityRowReader implements RowReader<DataRow> {

    private final ExtendedType<?>[] readers;
    private final String[] labels;
    private final int[] types;
    private final int mapCapacity;
    private final int startIndex;
    protected final String entityName;

    static RowReader<DataRow> of(
            ExtendedType<?>[] readers,
            int[] types,
            String[] labels,
            int startIndex,
            ClassDescriptor classDescriptor) {

        String entityName = classDescriptor != null ? classDescriptor.getEntity().getName() : null;
        return classDescriptor != null && classDescriptor.hasSubclasses()
                ? new InheritanceAwareEntityRowReader(readers, types, labels, startIndex, entityName, classDescriptor.getEntityInheritanceTree())
                : new EntityRowReader(readers, types, labels, startIndex, entityName);
    }

    protected EntityRowReader(ExtendedType<?>[] readers, int[] types, String[] labels, int startIndex, String entityName) {
        this.readers = readers;
        this.types = types;
        this.labels = labels;
        this.startIndex = startIndex;
        this.entityName = entityName;
        this.mapCapacity = (int) Math.ceil(readers.length / 0.75);
    }

    @Override
    public DataRow readRow(ResultSet resultSet) {

        try {
            DataRow row = new DataRow(mapCapacity);
            int len = readers.length;

            for (int i = 0; i < len; i++) {

                // note: jdbc column indexes start from 1, not 0 as in arrays
                Object val = readers[i].materializeObject(resultSet, startIndex + i + 1, types[i]);
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

    protected void postprocessRow(ResultSet resultSet, DataRow dataRow) {
        dataRow.setEntityName(entityName);
    }

    private static class InheritanceAwareEntityRowReader extends EntityRowReader {

        private final EntityInheritanceTree entityInheritanceTree;

        InheritanceAwareEntityRowReader(
                ExtendedType<?>[] readers,
                int[] types,
                String[] labels,
                int startIndex,
                String entityName,
                EntityInheritanceTree entityInheritanceTree) {

            super(readers, types, labels, startIndex, entityName);
            this.entityInheritanceTree = entityInheritanceTree;
        }

        @Override
        protected void postprocessRow(ResultSet resultSet, DataRow dataRow) {
            ObjEntity entity = entityInheritanceTree.entityMatchingRow(dataRow);
            dataRow.setEntityName(entity != null ? entity.getName() : entityName);
        }
    }
}
