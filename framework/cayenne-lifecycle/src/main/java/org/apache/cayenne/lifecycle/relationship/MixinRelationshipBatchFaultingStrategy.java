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

public class MixinRelationshipBatchFaultingStrategy implements
        MixinRelationshipFaultingStrategy {

    private ThreadLocal<UuidBatchFault> batchFaultHolder;

    public MixinRelationshipBatchFaultingStrategy() {
        this.batchFaultHolder = new ThreadLocal<UuidBatchFault>();
    }

    @Override
    public void afterObjectLoaded(DataObject object) {

        UuidBatchFault batchFault = batchFaultHolder.get();

        if (batchFault == null) {
            batchFault = new UuidBatchFault(object.getObjectContext());
            batchFaultHolder.set(batchFault);
        }

        String uuidProperty = uuidPropertyName(object);
        String uuidRelationship = uuidRelationshipName(uuidProperty);
        String uuid = (String) object.readProperty(uuidProperty);
        if (uuid == null) {
            object.writePropertyDirectly(uuidRelationship, null);
        }
        else {
            batchFault.addUuid(uuid);
            object.writePropertyDirectly(
                    uuidRelationship,
                    new UuidFault(batchFault, uuid));
        }
    }

    @Override
    public void afterQuery() {
        batchFaultHolder.set(null);
    }

    String uuidRelationshipName(String uuidPropertyName) {
        return "cay:related:" + uuidPropertyName;
    }

    String uuidPropertyName(DataObject object) {

        MixinRelationship annotation = object.getClass().getAnnotation(
                MixinRelationship.class);

        // TODO: look it up in the superclasses??
        if (annotation == null) {
            throw new IllegalArgumentException(
                    "Object class is not annotated with 'MixinRelationship': "
                            + object.getClass().getName());
        }

        // TODO: I guess we'll need to cache this metadata for performance if we are to
        // support inheritance lookups, etc.
        
        return annotation.value();
    }
}
