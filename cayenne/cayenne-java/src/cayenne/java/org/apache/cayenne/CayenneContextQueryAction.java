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

import java.util.List;

import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.remote.RemoteIncrementalFaultList;
import org.apache.cayenne.util.ListResponse;
import org.apache.cayenne.util.ObjectContextQueryAction;

/**
 * @since 1.2
 * @author Andrus Adamchik
 */
class CayenneContextQueryAction extends ObjectContextQueryAction {

    CayenneContextQueryAction(CayenneContext actingContext, ObjectContext targetContext,
            Query query) {
        super(actingContext, targetContext, query);
    }

    public QueryResponse execute() {

        if (interceptOIDQuery() != DONE) {
            if (interceptRelationshipQuery() != DONE) {
                if (interceptLocalCache() != DONE) {
                    if (interceptPaginatedQuery() != DONE) {
                        runQuery();
                    }
                }
            }
        }

        interceptObjectConversion();
        return response;
    }

    private boolean interceptPaginatedQuery() {
        if (metadata.getPageSize() > 0) {
            response = new ListResponse(new RemoteIncrementalFaultList(
                    actingContext,
                    query));
            return DONE;
        }

        return !DONE;
    }

    private boolean interceptLocalCache() {

        String cacheKey = metadata.getCacheKey();
        if (cacheKey == null) {
            return !DONE;
        }

        boolean cache = QueryMetadata.LOCAL_CACHE.equals(metadata.getCachePolicy());
        boolean cacheOrCacheRefresh = cache
                || QueryMetadata.LOCAL_CACHE_REFRESH.equals(metadata.getCachePolicy());

        if (!cacheOrCacheRefresh) {
            return !DONE;
        }

        CayenneContextGraphManager graphManager = ((CayenneContext) actingContext)
                .internalGraphManager();
        if (cache) {

            List cachedResults = graphManager.getCachedQueryResult(cacheKey);
            if (cachedResults != null) {
                response = new ListResponse(cachedResults);
                return DONE;
            }
        }

        if (interceptPaginatedQuery() != DONE) {
            runQuery();
        }

        graphManager.cacheQueryResult(cacheKey, response.firstList());
        return DONE;
    }
}
