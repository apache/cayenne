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

import org.apache.cayenne.graph.GraphDiff;

/**
 * An interface of a filter that allows to intercept DataChannel sync operations.
 * Sync filters allow to implement chains of custom processors around a DataChannel.
 * <p>
 * Example: <pre>{@code
 * public class MySyncFilter implements DataChannelSyncFilter {
 *     public GraphDiff onSync(ObjectContext originatingContext,
 *                             GraphDiff changes,
 *                             int syncType,
 *                             DataChannelSyncFilterChain filterChain) {
 *         System.out.println("Do something before sync");
 *         // process changes or return some custom diff
 *         GraphDiff diff = filterChain.onSync(originatingContext, changes, syncType);
 *         System.out.println("Do something after sync");
 *         return diff;
 *     }
 * }}</pre>
 *
 * @see DataChannelQueryFilter
 * @see org.apache.cayenne.configuration.runtime.CoreModuleExtender#addSyncFilter(Class, boolean)
 *
 * @since 4.1
 */
@FunctionalInterface
public interface DataChannelSyncFilter {

    /**
     * @param originatingContext originating context of changes
     * @param changes diff that is being processed
     * @param syncType type of sync; possible values: {@link DataChannel#FLUSH_NOCASCADE_SYNC},
     *                 {@link DataChannel#FLUSH_CASCADE_SYNC},
     *                 {@link DataChannel#ROLLBACK_CASCADE_SYNC}
     * @param filterChain chain of sync filters to invoke after this filter
     * @return final context diff
     *
     * @see DataChannel#FLUSH_NOCASCADE_SYNC
     * @see DataChannel#FLUSH_CASCADE_SYNC
     * @see DataChannel#ROLLBACK_CASCADE_SYNC
     */
    GraphDiff onSync(
            ObjectContext originatingContext,
            GraphDiff changes,
            int syncType,
            DataChannelSyncFilterChain filterChain);

}
