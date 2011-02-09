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

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelFilter;
import org.apache.cayenne.DataChannelFilterChain;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.annotation.PostLoad;
import org.apache.cayenne.annotation.PostPersist;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.query.Query;

/**
 * A {@link DataChannelFilter} that implements UUID relationships read functionality.
 * 
 * @since 3.1
 */
public class UuidRelationshipFilter implements DataChannelFilter {

    private UuidRelationshipFaultingStrategy faultingStrategy;

    public void init(DataChannel channel) {
        this.faultingStrategy = createFaultingStrategy();
    }

    protected UuidRelationshipFaultingStrategy createFaultingStrategy() {
        return new UuidRelationshipBatchFaultingStrategy();
    }

    public GraphDiff onSync(
            ObjectContext context,
            GraphDiff diff,
            int syncType,
            DataChannelFilterChain chain) {
        // noop for now
        return chain.onSync(context, diff, syncType);
    }

    public QueryResponse onQuery(
            ObjectContext context,
            Query query,
            DataChannelFilterChain chain) {

        try {
            return chain.onQuery(context, query);
        }
        finally {
            faultingStrategy.afterQuery();
        }
    }

    /**
     * A lifecycle callback method that delegates object post load event processing to the
     * underlying faulting strategy.
     */
    @PostLoad(entityAnnotations = UuidRelationship.class)
    @PostPersist(entityAnnotations = UuidRelationship.class)
    void postLoad(DataObject object) {
        faultingStrategy.afterObjectLoaded(object);
    }
}
