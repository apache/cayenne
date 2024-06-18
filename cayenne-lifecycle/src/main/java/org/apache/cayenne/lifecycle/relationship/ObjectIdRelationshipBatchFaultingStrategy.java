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
package org.apache.cayenne.lifecycle.relationship;

import org.apache.cayenne.Persistent;

import java.util.ArrayList;
import java.util.List;

/**
 * A faulting strategy that does batch-faulting of related objects whenever a first
 * ObjectId relationship is accessed.
 * 
 * @since 3.1
 */
public class ObjectIdRelationshipBatchFaultingStrategy implements ObjectIdRelationshipFaultingStrategy {

    private ThreadLocal<List<ObjectIdBatchSourceItem>> batchSources;

    public ObjectIdRelationshipBatchFaultingStrategy() {
        this.batchSources = new ThreadLocal<>();
    }

    public void afterObjectLoaded(Persistent object) {

        String uuidProperty = objectIdPropertyName(object);
        String uuidRelationship = objectIdRelationshipName(uuidProperty);
        String uuid = (String) object.readProperty(uuidProperty);
        if (uuid == null) {
            object.writePropertyDirectly(uuidRelationship, null);
        }
        else {
            List<ObjectIdBatchSourceItem> sources = batchSources.get();

            if (sources == null) {
                sources = new ArrayList<>();
                batchSources.set(sources);
            }

            sources.add(new ObjectIdBatchSourceItem(object, uuid, uuidRelationship));
        }
    }

    public void afterQuery() {

        List<ObjectIdBatchSourceItem> sources = batchSources.get();
        if (sources != null) {
            batchSources.set(null);

            ObjectIdBatchFault batchFault = new ObjectIdBatchFault(sources
                    .get(0)
                    .getObject()
                    .getObjectContext(), sources);

            for (ObjectIdBatchSourceItem source : sources) {
                source.getObject().writePropertyDirectly(
                        source.getObjectIdRelationship(),
                        new ObjectIdFault(batchFault, source.getId()));
            }
        }
    }

    String objectIdRelationshipName(String uuidPropertyName) {
        return "cay:related:" + uuidPropertyName;
    }

    String objectIdPropertyName(Persistent object) {

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

}
