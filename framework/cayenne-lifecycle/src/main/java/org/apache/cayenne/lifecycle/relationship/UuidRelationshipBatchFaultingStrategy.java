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

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.DataObject;

/**
 * A faulting strategy that does batch-faulting of related objects whenever a first UUID
 * relationship is accessed.
 * 
 * @since 3.1
 */
public class UuidRelationshipBatchFaultingStrategy implements
        UuidRelationshipFaultingStrategy {

    private ThreadLocal<List<UuidBatchSourceItem>> batchSources;

    public UuidRelationshipBatchFaultingStrategy() {
        this.batchSources = new ThreadLocal<List<UuidBatchSourceItem>>();
    }

    public void afterObjectLoaded(DataObject object) {

        String uuidProperty = uuidPropertyName(object);
        String uuidRelationship = uuidRelationshipName(uuidProperty);
        String uuid = (String) object.readProperty(uuidProperty);
        if (uuid == null) {
            object.writePropertyDirectly(uuidRelationship, null);
        }
        else {
            List<UuidBatchSourceItem> sources = batchSources.get();

            if (sources == null) {
                sources = new ArrayList<UuidBatchSourceItem>();
                batchSources.set(sources);
            }

            sources.add(new UuidBatchSourceItem(object, uuid, uuidRelationship));
        }
    }

    public void afterQuery() {

        List<UuidBatchSourceItem> sources = batchSources.get();
        if (sources != null) {
            batchSources.set(null);

            UuidBatchFault batchFault = new UuidBatchFault(sources
                    .get(0)
                    .getObject()
                    .getObjectContext(), sources);

            for (UuidBatchSourceItem source : sources) {
                source.getObject().writePropertyDirectly(
                        source.getUuidRelationship(),
                        new UuidFault(batchFault, source.getUuid()));
            }
        }
    }

    String uuidRelationshipName(String uuidPropertyName) {
        return "cay:related:" + uuidPropertyName;
    }

    String uuidPropertyName(DataObject object) {

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

}
