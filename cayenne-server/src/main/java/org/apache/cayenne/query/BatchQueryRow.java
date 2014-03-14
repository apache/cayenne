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

import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.map.DbAttribute;
import org.apache.commons.collections.Factory;

/**
 * Represents a single row of values in a BatchQuery.
 * 
 * @since 3.2
 */
public abstract class BatchQueryRow {

    protected ObjectId objectId;
    protected Map<String, Object> qualifier;

    public BatchQueryRow(ObjectId objectId, Map<String, Object> qualifier) {
        this.objectId = objectId;
        this.qualifier = qualifier;
    }

    public abstract Object getValue(int i);

    public Map<String, Object> getQualifier() {
        return qualifier;
    }

    public ObjectId getObjectId() {
        return objectId;
    }

    /**
     * Used by subclasses to resolve deferred values on demand. This is useful
     * when a certain value comes from a generated key of another master object.
     */
    protected Object getValue(Map<String, Object> valueMap, DbAttribute attribute) {

        Object value = valueMap.get(attribute.getName());

        // if a value is a Factory, resolve it here...
        // slight chance that a normal value will implement Factory interface???
        if (value instanceof Factory) {
            value = ((Factory) value).create();
            valueMap.put(attribute.getName(), value);

            // update replacement id
            if (attribute.isPrimaryKey()) {
                // sanity check
                if (value == null) {
                    String name = attribute.getEntity() != null ? attribute.getEntity().getName() : "<null>";
                    throw new CayenneRuntimeException("Failed to generate PK: " + name + "." + attribute.getName());
                }

                ObjectId id = getObjectId();
                if (id != null) {
                    // always override with fresh value as this is what's in the
                    // DB
                    id.getReplacementIdMap().put(attribute.getName(), value);
                }
            }
        }

        return value;
    }
}
