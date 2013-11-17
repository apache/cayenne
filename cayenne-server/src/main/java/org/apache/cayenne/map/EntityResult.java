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
package org.apache.cayenne.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.util.ToStringBuilder;

/**
 * A metadata object that provides mapping of a set of result columns to an
 * ObjEntity. Used by {@link SQLResult}. Note that fields in the EntityResult
 * are not required to follow the order of columns in the actual query, and can
 * be added in the arbitrary order.
 * 
 * @since 3.0
 */
public class EntityResult {

    protected String entityName;
    protected Class<?> entityClass;
    protected Collection<FieldResult> fields;

    public EntityResult(Class<?> entityClass) {
        this.entityClass = entityClass;
    }

    public EntityResult(String entityName) {
        this.entityName = entityName;
    }

    /**
     * Returns a map of result column names to attribute db paths from the root
     * entity.
     */
    public Map<String, String> getDbFields(EntityResolver resolver) {

        if (this.fields == null) {
            return Collections.EMPTY_MAP;
        }

        Map<String, String> dbFields = new HashMap<String, String>();

        for (FieldResult field : fields) {
            if (field.isDbAttribute() || field.getAttributeName().startsWith("fetch.")) {
                dbFields.put(field.getAttributeName(), field.getColumn());
            } else {
                ObjEntity entity = field.getEntityName() != null ? resolver.getObjEntity(field.getEntityName())
                        : getRootEntity(resolver);

                ObjAttribute attribute = entity.getAttribute(field.getAttributeName());
                dbFields.put(attribute.getDbAttributePath(), field.getColumn());
            }
        }

        return dbFields;
    }

    private ObjEntity getRootEntity(EntityResolver resolver) {
        if (entityName != null) {
            return resolver.getObjEntity(entityName);
        } else if (entityClass != null) {
            return resolver.getObjEntity(entityClass);
        } else {
            throw new IllegalStateException("Both entity name and class are null");
        }
    }

    /**
     * Adds a result set column mapping for a single object property of the root
     * entity.
     */
    public void addObjectField(String attributeName, String column) {
        addField(new FieldResult(null, attributeName, column, false));
    }

    /**
     * Adds a result set column mapping for a single object property of a
     * specified entity that may differ from the root entity if inheritance is
     * involved.
     */
    public void addObjectField(String entityName, String attributeName, String column) {
        addField(new FieldResult(entityName, attributeName, column, false));
    }

    /**
     * Adds a result set column mapping for a single DbAttribute.
     */
    public void addDbField(String dbAttributeName, String column) {
        addField(new FieldResult(null, dbAttributeName, column, true));
    }

    void addField(FieldResult field) {
        if (fields == null) {
            fields = new ArrayList<FieldResult>();
        }

        fields.add(field);
    }

    public String getEntityName() {
        return entityName;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    class FieldResult {

        protected String entityName;
        protected String attributeName;
        protected String column;
        protected boolean dbAttribute;

        FieldResult(String entityName, String attributeName, String column, boolean dbAttribute) {

            this.entityName = entityName;
            this.attributeName = attributeName;
            this.column = column;
            this.dbAttribute = dbAttribute;
        }

        public String getEntityName() {
            return entityName;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public String getColumn() {
            return column;
        }

        public boolean isDbAttribute() {
            return dbAttribute;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this).append("attributeName", attributeName).append("column", column)
                    .append("db", dbAttribute).toString();
        }
    }
}
