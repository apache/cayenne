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

package org.apache.cayenne;

import java.io.Serializable;
import java.util.Map;

/**
 * <p>
 * A portable global identifier for persistent objects. ObjectId can be temporary (used for transient or new
 * uncommitted objects) or permanent (used for objects that have been already stored in DB).
 * <p>
 * A temporary ObjectId stores object entity name and a pseudo-unique binary key; permanent id stores a map of values
 * from an external persistent store (aka "primary key").
 */
public interface ObjectId extends Serializable {

    /**
     * Creates a temporary ObjectId for a given entity.
     *
     * @since 4.2
     */
    static ObjectId of(String entityName) {
        return new ObjectIdTmp(entityName);
    }

    /**
     * Creates a temporary ObjectId for a given entity, using provided unique id key.
     *
     * @since 4.2
     */
    static ObjectId of(String entityName, byte[] tmpKey) {
        return new ObjectIdTmp(entityName, tmpKey);
    }

    /**
     * Creates a single key/value permanent ObjectId.
     *
     * @since 4.2
     */
    static ObjectId of(String entityName, String keyName, Object value) {
        if (value instanceof Number) {
            return new ObjectIdNumber(entityName, keyName, (Number) value);
        }
        return new ObjectIdSingle(entityName, keyName, value);
    }

    /**
     * Creates an ObjectId using another id as a template, but for a different entity. Useful inside the Cayenne stack
     * when resolving inheritance hierarchies.
     *
     * @since 4.2
     */
    static ObjectId of(String entityName, ObjectId objectId) {
        if (objectId instanceof ObjectIdNumber) {
            ObjectIdNumber id = (ObjectIdNumber) objectId;
            return new ObjectIdNumber(entityName, id.getKeyName(), id.getValue());
        }

        if (objectId instanceof ObjectIdSingle) {
            ObjectIdSingle id = (ObjectIdSingle) objectId;
            return new ObjectIdSingle(entityName, id.getKeyName(), id.getValue());
        }

        if (objectId instanceof ObjectIdTmp) {
            return of(entityName, objectId.getKey());
        }

        return of(entityName, objectId.getIdSnapshot());
    }

    /**
     * Creates an ObjectId, potentially mapped to multiple columns.
     *
     * @since 4.2
     */
    static ObjectId of(String entityName, Map<String, ?> values) {
        if (values.size() == 1) {
            Map.Entry<String, ?> entry = values.entrySet().iterator().next();
            return of(entityName, entry.getKey(), entry.getValue());
        }
        return new ObjectIdCompound(entityName, values);
    }

    boolean isTemporary();

    String getEntityName();

    byte[] getKey();

    Map<String, Object> getIdSnapshot();

    Map<String, Object> getReplacementIdMap();

    ObjectId createReplacementId();

    boolean isReplacementIdAttached();
}
