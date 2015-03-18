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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.jdbc.RowDescriptor;
import org.apache.cayenne.access.jdbc.reader.DataRowPostProcessor.ColumnOverride;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.ScalarResultSegment;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 4.0
 */
public class DefaultRowReaderFactory implements RowReaderFactory {

    @Override
    public RowReader<?> rowReader(RowDescriptor descriptor, QueryMetadata queryMetadata, DbAdapter adapter,
            Map<ObjAttribute, ColumnDescriptor> attributeOverrides) {

        ClassDescriptor classDescriptor = queryMetadata.getClassDescriptor();
        ObjEntity objEntity = queryMetadata.getObjEntity();
        DbEntity dbEntity = queryMetadata.getDbEntity();
        int pageSize = queryMetadata.getPageSize();

        DataRowPostProcessor postProcessor = create(descriptor, adapter.getExtendedTypes(), attributeOverrides, classDescriptor);

        List<Object> rsMapping = queryMetadata.getResultSetMapping();
        if (rsMapping == null) {
            return createFullRowReader(descriptor, postProcessor, objEntity, dbEntity, classDescriptor, pageSize);
        }

        int resultWidth = rsMapping.size();
        if (resultWidth == 0) {
            throw new CayenneRuntimeException("Empty result descriptor");
        } else if (resultWidth == 1) {

            Object segment = rsMapping.get(0);

            if (segment instanceof EntityResultSegment) {
                return createEntityRowReader(descriptor, (EntityResultSegment) segment,
                        postProcessor, objEntity, dbEntity, pageSize);
            } else {
                return new ScalarRowReader<Object>(descriptor, (ScalarResultSegment) segment);
            }
        } else {
            CompoundRowReader reader = new CompoundRowReader(resultWidth);

            for (int i = 0; i < resultWidth; i++) {
                Object segment = rsMapping.get(i);

                if (segment instanceof EntityResultSegment) {
                    reader.addRowReader(i, createEntityRowReader(descriptor, (EntityResultSegment) segment,
                                    postProcessor, objEntity, dbEntity, pageSize));
                } else {
                    reader.addRowReader(i, new ScalarRowReader<Object>(descriptor, (ScalarResultSegment) segment));
                }
            }

            return reader;
        }
    }

    private RowReader<?> createEntityRowReader(RowDescriptor descriptor, EntityResultSegment resultMetadata,
                                               DataRowPostProcessor postProcessor, ObjEntity objEntity, DbEntity dbEntity, int pageSize) {

        if (pageSize > 0) {
            return new IdRowReader<Object>(descriptor, postProcessor, objEntity, dbEntity);
        } else if (resultMetadata.getClassDescriptor() != null && resultMetadata.getClassDescriptor().hasSubclasses()) {
            return new InheritanceAwareEntityRowReader(descriptor, resultMetadata, postProcessor);
        } else {
            return new EntityRowReader(descriptor, resultMetadata, postProcessor);
        }
    }

    private RowReader<?> createFullRowReader(RowDescriptor descriptor, DataRowPostProcessor postProcessor,
                                             ObjEntity objEntity, DbEntity dbEntity, ClassDescriptor classDescriptor, int pageSize) {

        if (pageSize > 0) {
            return new IdRowReader<Object>(descriptor, postProcessor, objEntity, dbEntity);
        } else if (classDescriptor != null && classDescriptor.hasSubclasses()) {
            return new InheritanceAwareRowReader(descriptor, postProcessor, objEntity, classDescriptor.getEntityInheritanceTree());
        } else {
            return new FullRowReader(descriptor, postProcessor, objEntity);
        }
    }


    private DataRowPostProcessor create(RowDescriptor rowDescriptor, ExtendedTypeMap extendedTypes,
                                        Map<ObjAttribute, ColumnDescriptor> attributeOverrides, ClassDescriptor classDescriptor) {

        if (attributeOverrides.isEmpty()) {
            return null;
        }

        ColumnDescriptor[] columns = rowDescriptor.getColumns();

        Map<String, Collection<ColumnOverride>> columnOverrides = new HashMap<String, Collection<ColumnOverride>>(2);

        for (Entry<ObjAttribute, ColumnDescriptor> entry : attributeOverrides.entrySet()) {

            ObjAttribute attribute = entry.getKey();
            Entity entity = attribute.getEntity();

            String key = null;
            int jdbcType = TypesMapping.NOT_DEFINED;
            int index = -1;
            for (int i = 0; i < columns.length; i++) {
                if (columns[i] == entry.getValue()) {

                    // if attribute type is the same as column, there is no
                    // conflict
                    if (!attribute.getType().equals(columns[i].getJavaClass())) {
                        // note that JDBC index is "1" based
                        index = i + 1;
                        jdbcType = columns[i].getJdbcType();
                        key = columns[i].getDataRowKey();
                    }

                    break;
                }
            }

            if (index < 1) {
                continue;
            }

            ExtendedType converter = extendedTypes.getRegisteredType(attribute.getType());

            Collection<ColumnOverride> overrides = columnOverrides.get(entity.getName());

            if (overrides == null) {
                overrides = new ArrayList<ColumnOverride>(3);
                columnOverrides.put(entity.getName(), overrides);
            }

            overrides.add(new ColumnOverride(index, key, converter, jdbcType));
        }

        // inject null post-processor
        if (columnOverrides.isEmpty()) {
            return null;
        }

        return new DataRowPostProcessor(classDescriptor, columnOverrides);
    }

}
