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
import org.apache.cayenne.lifecycle.ref.ReferenceableHandler;

/**
 * @since 3.1
 */
public class UuidRelationshipHandler {

    protected ReferenceableHandler referenceableHandler;

    public UuidRelationshipHandler(ReferenceableHandler referenceableHandler) {
        this.referenceableHandler = referenceableHandler;
    }

    public String uuidRelationshipName(String uuidPropertyName) {
        return "cay:related:" + uuidPropertyName;
    }

    public String uuidPropertyName(DataObject object) {

        UuidRelationship annotation = object.getClass().getAnnotation(
                UuidRelationship.class);

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
     * Establishes a UUID relationship between two objects. Objects must be registered in
     * the same ObjectContext. "from" argument is the object annotated with
     * UuidRelationship. Second argument can optionally be null.
     */
    public void relate(DataObject from, DataObject to) {

        if (from.getObjectContext() == null) {
            throw new IllegalArgumentException("'from' has null ObjectContext");
        }

        String property = uuidPropertyName(from);
        String relationship = uuidRelationshipName(property);

        if (to != null) {
            if (to.getObjectContext() == null) {
                throw new IllegalArgumentException("'to' has null ObjectContext");
            }

            if (from.getObjectContext() != to.getObjectContext()) {
                throw new IllegalArgumentException(
                        "'from' and 'to' objects are registered in different ObjectContexts");
            }

            if (to.getObjectId().isTemporary()
                    && !to.getObjectId().isReplacementIdAttached()) {
                throw new UnsupportedOperationException("TODO - eager ID generation");
            }

            String uuid = referenceableHandler.getUuid(to);

            from.writeProperty(property, uuid);
            from.writePropertyDirectly(relationship, to);
        }
        else {
            from.writeProperty(property, null);
            from.writePropertyDirectly(relationship, null);
        }
    }
}
