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
import java.util.Collections;
import java.util.List;

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
    protected List<FieldResult> fields;

    public EntityResult(Class<?> entityClass) {
        this.entityClass = entityClass;
    }

    public EntityResult(String entityName) {
        this.entityName = entityName;
    }

    public ClassDescriptor getClassDescriptor(EntityResolver resolver) {
        return resolver.getClassDescriptor(getEntity(resolver).getName());
    }

    /**
     * Returns entity result fields normalized to represent DbAttributes.
     */
    public FieldResult[] getDbFields(EntityResolver resolver) {
        FieldResult[] fields = (this.fields != null)
                ? new FieldResult[this.fields.size()]
                : new FieldResult[0];
        ObjEntity entity = null;

        for (int i = 0; i < fields.length; i++) {
            FieldResult field = this.fields.get(i);

            if (!field.isDbAttribute()) {
                if (entity == null) {
                    entity = getEntity(resolver);
                }

                ObjAttribute attribute = (ObjAttribute) entity.getAttribute(field
                        .getAttributeName());

                // TODO: andrus 2/8/2008 - flattened attributes support
                field = new FieldResult(
                        attribute.getDbAttributeName(),
                        field.getColumn(),
                        true);
            }

            fields[i] = field;
        }

        return fields;
    }

    public ObjEntity getEntity(EntityResolver resolver) {
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

    public void addObjectField(String attributeName, String column) {
        addField(new FieldResult(attributeName, column, false));
    }

    public void addDbField(String dbAttributeName, String column) {
        addField(new FieldResult(dbAttributeName, column, true));
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

    public List<FieldResult> getFields() {
        return fields != null ? fields : Collections.EMPTY_LIST;
    }
}
