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
package org.apache.cayenne.lifecycle.ref;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.annotation.PostLoad;
import org.apache.cayenne.annotation.PostPersist;
import org.apache.cayenne.lifecycle.uuid.UuidCoder;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;

/**
 * A {@link MixinHandler} that injects {@link Referenceable#UUID_PROPERTY} into
 * DataObjects and provides methods to lookup objects by UUID, as well as read UUID of the
 * existing objects.
 * 
 * @since 3.1
 */
public class ReferenceableHandler {

    protected EntityResolver entityResolver;
    protected Map<String, UuidCoder> coders;

    public ReferenceableHandler(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
        this.coders = new ConcurrentHashMap<String, UuidCoder>();
    }

    /**
     * Returns a Referenceable object matching provided UUID and bound in provided
     * {@link ObjectContext}.
     */
    public Object getReferenceable(ObjectContext context, String uuid) {

        String entityName = UuidCoder.getEntityName(uuid);
        UuidCoder coder = getCoder(entityName);
        ObjectId oid = coder.toObjectId(uuid);
        return Cayenne.objectForPK(context, oid);
    }

    public String getUuid(Object referenceable) {

        if (referenceable == null) {
            throw new NullPointerException("Null object");
        }

        if (referenceable instanceof DataObject) {

            // even if this is not a registered Referenceable, don't see a
            // problem if we return a UUID.
            DataObject dataObject = (DataObject) referenceable;
            String uuid = (String) dataObject
                    .readPropertyDirectly(Referenceable.UUID_PROPERTY);

            if (uuid == null) {
                throw new IllegalArgumentException(
                        "No UUID set. An object is either not a Referenceable "
                                + "or is NEW or TRANSIENT.");
            }

            return uuid;
        }
        else {
            throw new IllegalArgumentException("Object is not a DataObject: "
                    + referenceable.getClass().getName());
        }
    }

    /**
     * A lifecycle listener method that initialzes DataObject UUID property.
     */
    @PostLoad(entityAnnotations = Referenceable.class)
    @PostPersist(entityAnnotations = Referenceable.class)
    protected void initProperties(DataObject object) {

        UuidCoder coder = getCoder(object.getObjectId().getEntityName());
        String uuid = coder.toUuid(object.getObjectId());
        object.writePropertyDirectly(Referenceable.UUID_PROPERTY, uuid);
    }

    protected UuidCoder getCoder(String entityName) {
        UuidCoder coder = coders.get(entityName);
        if (coder == null) {

            // TODO: check @Referenceable annotation?
            ObjEntity entity = entityResolver.getObjEntity(entityName);
            if (entity == null) {
                throw new IllegalArgumentException("Entity '"
                        + entityName
                        + "' is not a known referenceable");
            }

            coder = new UuidCoder(entity);
            coders.put(entity.getName(), coder);
        }

        return coder;

    }
}
