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
package org.apache.cayenne.reflect;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.EntityResultMetadata;

/**
 * @since 3.0
 */
class PersistentDescriptorResultMetadata implements EntityResultMetadata {

    ClassDescriptor classDescriptor;
    Map<String, String> fields;

    PersistentDescriptorResultMetadata(ClassDescriptor classDescriptor) {
        this.classDescriptor = classDescriptor;

        // preserve field addition order by using linked map.
        this.fields = new LinkedHashMap<String, String>();
    }

    public ClassDescriptor getClassDescriptor() {
        return classDescriptor;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    void addObjectField(String attributeName, String column) {
        ObjEntity entity = classDescriptor.getEntity();

        ObjAttribute attribute = (ObjAttribute) entity.getAttribute(attributeName);
        fields.put(attribute.getDbAttributePath(), column);
    }

    /**
     * Adds a result set column mapping for a single object property of a specified entity
     * that may differ from the root entity if inheritance is involved.
     */
    void addObjectField(String entityName, String attributeName, String column) {
        ObjEntity entity = classDescriptor.getEntity().getDataMap().getObjEntity(
                entityName);

        ObjAttribute attribute = (ObjAttribute) entity.getAttribute(attributeName);
        fields.put(attribute.getDbAttributePath(), column);
    }

    /**
     * Adds a result set column mapping for a single DbAttribute.
     */
    void addDbField(String dbAttributeName, String column) {
        fields.put(dbAttributeName, column);
    }
}
