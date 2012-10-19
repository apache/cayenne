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
package org.apache.cayenne.lifecycle.id;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;

/**
 * An object to encode/decode ObjectIds for all entities found in a mapping
 * namespace.
 * 
 * @since 3.1
 */
public class IdCoder {

    protected EntityResolver entityResolver;
    protected Map<String, EntityIdCoder> coders;

    public IdCoder(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
        this.coders = new ConcurrentHashMap<String, EntityIdCoder>();
    }

    /**
     * Returns an ObjectId for a given String ID.
     */
    public ObjectId getObjectId(String id) {

        String entityName = EntityIdCoder.getEntityName(id);
        EntityIdCoder coder = getCoder(entityName);
        return coder.toObjectId(id);
    }

    /**
     * @since 3.2
     */
    public String getStringId(ObjectId id) {
        EntityIdCoder coder = getCoder(id.getEntityName());
        return coder.toStringId(id);
    }

    public String getStringId(Persistent object) {

        if (object == null) {
            throw new NullPointerException("Null object");
        }

        ObjectId id = object.getObjectId();
        return getStringId(id);
    }

    protected EntityIdCoder getCoder(String entityName) {
        EntityIdCoder coder = coders.get(entityName);
        if (coder == null) {

            coder = createCoder(entityName);
            coders.put(entityName, coder);
        }

        return coder;
    }

    protected EntityIdCoder createCoder(String entityName) {
        ObjEntity entity = entityResolver.getObjEntity(entityName);
        if (entity == null) {
            throw new IllegalArgumentException("Entity '" + entityName
                    + "' is not mapped");
        }

        return new EntityIdCoder(entity);
    }
}
