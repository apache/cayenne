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
package org.apache.cayenne.lifecycle.relationship;

import org.apache.cayenne.DataObject;
import org.apache.cayenne.lifecycle.id.IdCoder;

/**
 * @since 3.1
 */
public class ObjectIdRelationshipHandler {

    protected IdCoder referenceableHandler;

    public ObjectIdRelationshipHandler(IdCoder referenceableHandler) {
        this.referenceableHandler = referenceableHandler;
    }

    public String objectIdRelationshipName(String uuidPropertyName) {
        return "cay:related:" + uuidPropertyName;
    }

    public String objectIdPropertyName(DataObject object) {

        ObjectIdRelationship annotation = object.getClass().getAnnotation(
                ObjectIdRelationship.class);

        if (annotation == null) {
            throw new IllegalArgumentException(
                    "Object class is not annotated with @UuidRelationship: "
                            + object.getClass().getName());
        }

        // TODO: I guess we'll need to cache this metadata for performance if we are to
        // support inheritance lookups, etc.

        return annotation.value();
    }

    /**
     * Establishes an ObjectId relationship between two objects. Objects must be
     * registered in the same ObjectContext. "from" argument is the object annotated with
     * UuidRelationship. Second argument can optionally be null.
     */
    public void relate(DataObject from, DataObject to) {

        if (from.getObjectContext() == null) {
            throw new IllegalArgumentException("'from' has null ObjectContext");
        }

        String property = objectIdPropertyName(from);
        String relationship = objectIdRelationshipName(property);

        if (to != null) {
            if (to.getObjectContext() == null) {
                throw new IllegalArgumentException("'to' has null ObjectContext");
            }

            if (from.getObjectContext() != to.getObjectContext()) {
                throw new IllegalArgumentException(
                        "'from' and 'to' objects are registered in different ObjectContexts");
            }

            from.writePropertyDirectly(relationship, to);

            if (to.getObjectId().isTemporary()
                    && !to.getObjectId().isReplacementIdAttached()) {

                // defer ObjectId resolving till commit
                from.writeProperty(property, new ObjectIdPropagatedValueFactory(
                        referenceableHandler,
                        to));
            }
            else {
                String uuid = referenceableHandler.getStringId(to);
                from.writeProperty(property, uuid);
            }
        }
        else {
            from.writeProperty(property, null);
            from.writePropertyDirectly(relationship, null);
        }
    }
}
