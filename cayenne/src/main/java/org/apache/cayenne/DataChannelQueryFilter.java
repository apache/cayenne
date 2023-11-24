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

package org.apache.cayenne;

import org.apache.cayenne.query.Query;

/**
 * An interface of a filter that allows to intercept DataChannel query operations.
 * Query filters allow to implement chains of custom processors around a DataChannel.
 * <p>
 * Example: <pre>{@code
 * public class MyQueryFilter implements DataChannelQueryFilter {
 *     public QueryResponse onQuery(ObjectContext originatingContext, Query query,
 *                                  DataChannelQueryFilterChain filterChain) {
 *         System.out.println("Do something before query");
 *         // process query or return some custom response
 *         QueryResponse response = filterChain.onQuery(originatingContext, query);
 *         System.out.println("Do something after query");
 *         return response;
 *     }
 * }}</pre>
 *
 * @see DataChannelSyncFilter
 * @see org.apache.cayenne.configuration.runtime.CoreModuleExtender#addQueryFilter(DataChannelQueryFilter)
 *
 * @since 4.1
 */
@FunctionalInterface
public interface DataChannelQueryFilter {

    /**
     * @param originatingContext originating context of query
     * @param query that is processed
     * @param filterChain chain of query filters to invoke after this filter
     * @return query response
     */
    QueryResponse onQuery(ObjectContext originatingContext, Query query, DataChannelQueryFilterChain filterChain);

}
