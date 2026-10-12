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
import org.apache.cayenne.RowLayout;
import org.apache.cayenne.access.jdbc.RSColumn;
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.reflect.ClassDescriptor;

import java.sql.ResultSet;
import java.util.Objects;

/**
 * @since 3.0
 */
class FullRowReader implements RowReader<DataRow> {

    private final RSColumn[] columns;
    private final RowLayout layout;
    private final long version;
    protected final String entityName;

    static RowReader<DataRow> of(RSColumn[] columns, QueryMetadata queryMetadata, long version) {

        ObjEntity objEntity = queryMetadata.getObjEntity();
        String entityName = objEntity != null ? objEntity.getName() : null;

        ClassDescriptor classDescriptor = queryMetadata.getClassDescriptor();
        if (classDescriptor != null && classDescriptor.hasSubclasses()) {
            return new InheritanceAwareRowReader(columns, entityName, classDescriptor.getEntityInheritanceTree(),
                    version);
        }

        return new FullRowReader(columns, entityName, version);
    }

    /**
     * Returns a layout of the columns' DataRow names, in column order.
     */
    static RowLayout layout(RSColumn[] columns) {
        String[] names = new String[columns.length];
        for (int i = 0; i < columns.length; i++) {
            names[i] = columns[i].dataRowName();
        }
        return new RowLayout(names);
    }

    private FullRowReader(RSColumn[] columns, String entityName, long version) {
        this.columns = columns;
        this.layout = layout(columns);
        this.entityName = entityName;
        this.version = version;
    }

    @Override
    public DataRow readRow(ResultSet resultSet) {
        Object[] values = new Object[columns.length];
        readColumns(resultSet, values);
        return row(DataRow.of(entityName, layout, values, version));
    }

    /**
     * Returns the row to return for the given row read with the default entity name.
     */
    protected DataRow row(DataRow row) {
        return row;
    }

    private void readColumns(ResultSet resultSet, Object[] target) {
        int w = columns.length;

        try {
            for (int i = 0; i < w; i++) {
                RSColumn column = columns[i];
                // jdbc column indexes start from 1, not 0 unlike everywhere else
                target[i] = column.reader().materializeObject(resultSet, i + 1, column.rsType());
            }
        } catch (CayenneRuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CayenneRuntimeException("Exception materializing column.", ex);
        }
    }

    private static class InheritanceAwareRowReader extends FullRowReader {

        private final EntityInheritanceTree entityInheritanceTree;

        private InheritanceAwareRowReader(RSColumn[] columns, String entityName,
                                          EntityInheritanceTree entityInheritanceTree, long version) {
            super(columns, entityName, version);
            this.entityInheritanceTree = entityInheritanceTree;
        }

        @Override
        protected DataRow row(DataRow row) {
            return withMatchingEntity(row, entityInheritanceTree, entityName);
        }
    }

    /**
     * Returns the row with the name of the entity of the inheritance tree that the row matches, which is the given
     * row when that is its entity already or no entity matches.
     */
    static DataRow withMatchingEntity(DataRow row, EntityInheritanceTree tree, String defaultEntityName) {
        ObjEntity entity = tree.entityMatchingRow(row);
        String entityName = entity != null ? entity.getName() : defaultEntityName;
        return Objects.equals(entityName, row.getEntityName()) ? row : row.withEntityName(entityName);
    }
}
