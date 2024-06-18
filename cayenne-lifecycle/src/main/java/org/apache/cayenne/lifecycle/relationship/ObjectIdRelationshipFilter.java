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

import org.apache.cayenne.DataChannelQueryFilter;
import org.apache.cayenne.DataChannelQueryFilterChain;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.annotation.PostLoad;
import org.apache.cayenne.annotation.PostPersist;
import org.apache.cayenne.annotation.PostUpdate;
import org.apache.cayenne.query.Query;

/**
 * A {@link DataChannelQueryFilter} that implements ObjectId relationships read functionality.
 * 
 * @since 3.1
 */
public class ObjectIdRelationshipFilter implements DataChannelQueryFilter {

    private final ObjectIdRelationshipFaultingStrategy faultingStrategy;

    public ObjectIdRelationshipFilter() {
        this.faultingStrategy = createFaultingStrategy();
    }

    protected ObjectIdRelationshipFaultingStrategy createFaultingStrategy() {
        return new ObjectIdRelationshipBatchFaultingStrategy();
    }

    public QueryResponse onQuery(ObjectContext context, Query query, DataChannelQueryFilterChain chain) {
        try {
            return chain.onQuery(context, query);
        } finally {
            faultingStrategy.afterQuery();
        }
    }

    @PostUpdate(entityAnnotations = ObjectIdRelationship.class)
    @PostPersist(entityAnnotations = ObjectIdRelationship.class)
    void postCommit(Persistent object) {
        // invalidate after commit to ensure UUID property is re-read...
        object.getObjectContext().invalidateObjects(object);
    }

    /**
     * A lifecycle callback method that delegates object post load event processing to the
     * underlying faulting strategy.
     */
    @PostLoad(entityAnnotations = ObjectIdRelationship.class)
    void postLoad(Persistent object) {
        faultingStrategy.afterObjectLoaded(object);
    }
}
