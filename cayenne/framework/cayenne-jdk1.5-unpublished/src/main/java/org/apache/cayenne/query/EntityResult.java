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
package org.apache.cayenne.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * A metadata object that provides mapping of a set of result columns to an ObjEntity.
 * Used by {@link SQLResultSetMapping}.
 * 
 * @since 3.0
 * @author Andrus Adamchik
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

    public ClassDescriptor getClassDescriptor(EntityResolver resolver) {
        return resolver.getClassDescriptor(getRootEntity(resolver).getName());
    }

    /**
     * Returns a map of result column names to attribute db paths from the root entity.
     */
    public Map<String, String> getDbFields(EntityResolver resolver) {

        if (this.fields == null) {
            return Collections.EMPTY_MAP;
        }

        Map<String, String> dbFields = new HashMap<String, String>();
        for (FieldResult field : fields) {

            if (field.isDbAttribute()) {
                dbFields.put(field.getAttributeName(), field.getColumn());
            }
            else {
                ObjEntity entity = field.getEntityName() != null ? resolver
                        .getObjEntity(field.getEntityName()) : getRootEntity(resolver);

                ObjAttribute attribute = (ObjAttribute) entity.getAttribute(field
                        .getAttributeName());
                dbFields.put(attribute.getDbAttributePath(), field.getColumn());
            }
        }

        return dbFields;
    }

    public ObjEntity getRootEntity(EntityResolver resolver) {
        if (entityName != null) {
            return resolver.getObjEntity(entityName);
        }
        else if (entityClass != null) {
            return resolver.lookupObjEntity(entityClass);
        }
        else {
            throw new IllegalStateException("Both entity name and class are null");
        }
    }

    /**
     * Adds a result set column mapping for a single object property of the root entity.
     */
    public void addObjectField(String attributeName, String column) {
        addField(new FieldResult(null, attributeName, column, false));
    }

    /**
     * Adds a result set column mapping for a single object property of a specified entity
     * that may differ from the root entity if inheritance is involved.
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

    public Collection<FieldResult> getFields() {
        return fields != null ? fields : Collections.EMPTY_LIST;
    }
}
