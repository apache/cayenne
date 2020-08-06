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
import java.util.Collection;
import java.util.Map;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.map.EntityInheritanceTree;
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

    DataRowPostProcessor(ClassDescriptor classDescriptor, Map<String, Collection<ColumnOverride>> columnOverrides) {

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
