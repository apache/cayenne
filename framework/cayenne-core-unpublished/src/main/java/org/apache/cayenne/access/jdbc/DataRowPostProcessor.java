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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.trans.SelectTranslator;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * Deals with DataRow type conversion in inheritance situations.
 * 
 * @since 1.2
 */
class DataRowPostProcessor {

    private EntityInheritanceTree inheritanceTree;
    private Map<String, Collection<ColumnOverride>> columnOverrides;
    private Collection<ColumnOverride> defaultOverrides;

    // factory method
    static DataRowPostProcessor createPostProcessor(SelectTranslator translator) {
        Map<ObjAttribute, ColumnDescriptor> attributeOverrides = translator.getAttributeOverrides();
        if (attributeOverrides.isEmpty()) {
            return null;
        }

        ColumnDescriptor[] columns = translator.getResultColumns();

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

            ExtendedType converter = translator.getAdapter().getExtendedTypes().getRegisteredType(attribute.getType());

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

        ClassDescriptor rootDescriptor = translator.getQueryMetadata().getClassDescriptor();

        return new DataRowPostProcessor(rootDescriptor, columnOverrides);
    }

    private DataRowPostProcessor(ClassDescriptor classDescriptor,
            Map<String, Collection<ColumnOverride>> columnOverrides) {

        if (classDescriptor != null && classDescriptor.hasSubclasses()) {
            this.inheritanceTree = classDescriptor.getEntityInheritanceTree();
            this.columnOverrides = columnOverrides;
        } else {
            if (columnOverrides.size() != 1) {
                throw new IllegalArgumentException("No inheritance - there must be only one override set");
            }

            defaultOverrides = columnOverrides.values().iterator().next();
        }
    }

    void postprocessRow(ResultSet resultSet, DataRow row) throws Exception {

        Collection<ColumnOverride> overrides = getOverrides(row);

        if (overrides != null) {
            for (final ColumnOverride override : overrides) {

                Object newValue = override.converter.materializeObject(resultSet, override.index, override.jdbcType);
                row.put(override.key, newValue);
            }
        }
    }

    private final Collection<ColumnOverride> getOverrides(DataRow row) {
        if (defaultOverrides != null) {
            return defaultOverrides;
        } else {
            ObjEntity entity = inheritanceTree.entityMatchingRow(row);
            return entity != null ? columnOverrides.get(entity.getName()) : null;
        }
    }

    static final class ColumnOverride {

        int index;
        int jdbcType;
        String key;
        ExtendedType converter;

        ColumnOverride(int index, String key, ExtendedType converter, int jdbcType) {
            this.index = index;
            this.key = key;
            this.converter = converter;
            this.jdbcType = jdbcType;
        }
    }
}
