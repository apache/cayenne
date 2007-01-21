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
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.trans.SelectTranslator;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;

/**
 * Deals with DataRow type conversion in inheritance situations.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class DataRowPostProcessor {

    private EntityInheritanceTree inheritanceTree;
    private Map columnOverrides;
    private Collection defaultOverrides;

    // factory method
    static DataRowPostProcessor createPostProcessor(SelectTranslator translator) {
        Map attributeOverrides = translator.getAttributeOverrides();
        if (attributeOverrides.isEmpty()) {
            return null;
        }

        ColumnDescriptor[] columns = translator.getResultColumns();

        Map columnOverrides = new HashMap(2);

        Iterator it = attributeOverrides.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();

            ObjAttribute attribute = (ObjAttribute) entry.getKey();
            Entity entity = attribute.getEntity();

            String key = null;
            int jdbcType = TypesMapping.NOT_DEFINED;
            int index = -1;
            for (int i = 0; i < columns.length; i++) {
                if (columns[i] == entry.getValue()) {

                    // if attribute type is the same as column, there is no conflict
                    if (!attribute.getType().equals(columns[i].getJavaClass())) {
                        // note that JDBC index is "1" based
                        index = i + 1;
                        jdbcType = columns[i].getJdbcType();
                        key = columns[i].getLabel();
                    }

                    break;
                }
            }

            if (index < 1) {
                continue;
            }

            ExtendedType converter = translator
                    .getAdapter()
                    .getExtendedTypes()
                    .getRegisteredType(attribute.getType());

            Collection overrides = null;

            if (columnOverrides == null) {
                columnOverrides = new HashMap(2);
            }
            else {
                overrides = (Collection) columnOverrides.get(entity.getName());
            }

            if (overrides == null) {
                overrides = new ArrayList(3);
                columnOverrides.put(entity.getName(), overrides);
            }

            overrides.add(new ColumnOverride(index, key, converter, jdbcType));
        }

        // inject null post-processor
        return columnOverrides != null ? new DataRowPostProcessor(translator
                .getRootInheritanceTree(), columnOverrides) : null;
    }

    private DataRowPostProcessor(EntityInheritanceTree inheritanceTree,
            Map columnOverrides) {

        if (inheritanceTree != null && inheritanceTree.getChildren().size() > 0) {
            this.inheritanceTree = inheritanceTree;
            this.columnOverrides = columnOverrides;
        }
        else {
            if (columnOverrides.size() != 1) {
                throw new IllegalArgumentException(
                        "No inheritance - there must be only one override set");
            }

            defaultOverrides = (Collection) columnOverrides.values().iterator().next();
        }
    }

    void postprocessRow(ResultSet resultSet, DataRow row) throws Exception {

        Collection overrides = getOverrides(row);

        if (overrides != null) {
            Iterator it = overrides.iterator();
            while (it.hasNext()) {
                ColumnOverride override = (ColumnOverride) it.next();

                Object newValue = override.converter.materializeObject(
                        resultSet,
                        override.index,
                        override.jdbcType);
                row.put(override.key, newValue);
            }
        }
    }

    private final Collection getOverrides(DataRow row) {
        if (defaultOverrides != null) {
            return defaultOverrides;
        }
        else {
            ObjEntity entity = inheritanceTree.entityMatchingRow(row);
            return entity != null
                    ? (Collection) columnOverrides.get(entity.getName())
                    : null;
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
