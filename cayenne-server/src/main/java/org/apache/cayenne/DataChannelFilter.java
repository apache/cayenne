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
package org.apache.cayenne;

import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.query.Query;

/**
 * An interface of a filter that allows to intercept DataChannel operations. Filters allow
 * to implement chains of custom processors around a DataChannel, that can be used for
 * security, monitoring, business logic, providing context to lifecycle event listeners,
 * etc.
 * 
 * @since 3.1
 * @deprecated since 4.1 use {@link DataChannelQueryFilter} and {@link DataChannelSyncFilter}
 */
@Deprecated
public interface DataChannelFilter extends DataChannelSyncFilter, DataChannelQueryFilter {

    void init(DataChannel channel);

    QueryResponse onQuery(ObjectContext originatingContext, Query query,
                          DataChannelFilterChain filterChain);

    GraphDiff onSync(ObjectContext originatingContext, GraphDiff changes, int syncType,
                     DataChannelFilterChain filterChain);

    /**
     * Adapter method that allows to use old DataChannelFilter as new query filter
     */
    @Override
    default QueryResponse onQuery(ObjectContext originatingContext, Query query,
                                  DataChannelQueryFilterChain filterChain) {
        return onQuery(originatingContext, query, new DataChannelFilterChain(){
            @Override
            public QueryResponse onQuery(ObjectContext originatingContext, Query query) {
                return filterChain.onQuery(originatingContext, query);
            }
        });
    }

    /**
     * Adapter method that allows to use old DataChannelFilter as new sync filter
     */
    @Override
    default GraphDiff onSync(ObjectContext originatingContext, GraphDiff changes, int syncType,
                             DataChannelSyncFilterChain filterChain) {
        return onSync(originatingContext, changes, syncType, new DataChannelFilterChain(){
            @Override
            public GraphDiff onSync(ObjectContext originatingContext, GraphDiff changes, int syncType) {
                return filterChain.onSync(originatingContext, changes, syncType);
            }
        });
    }
}
