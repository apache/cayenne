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

package org.apache.cayenne.access;

import java.util.List;

import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.util.ListResponse;
import org.apache.cayenne.util.ObjectContextQueryAction;

/**
 * A DataContext-specific version of
 * {@link org.apache.cayenne.util.ObjectContextQueryAction}.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
// TODO: Andrus, 2/2/2006 - all these DataContext extensions should become available to
// CayenneContext as well....
class DataContextQueryAction extends ObjectContextQueryAction {

    public DataContextQueryAction(DataContext actingContext, ObjectContext targetContext,
            Query query) {
        super(actingContext, targetContext, query);
    }

    public QueryResponse execute() {
        if (interceptPaginatedQuery() != DONE) {
            if (interceptOIDQuery() != DONE) {
                if (interceptRelationshipQuery() != DONE) {
                    if (interceptLocalCache() != DONE) {
                        runQuery();
                    }
                }
            }
        }

        interceptObjectConversion();
        return response;
    }

    /**
     * Overrides super implementation to property handle data row fetches.
     */
    protected boolean interceptOIDQuery() {
        if (query instanceof ObjectIdQuery) {
            ObjectIdQuery oidQuery = (ObjectIdQuery) query;

            if (!oidQuery.isFetchMandatory()) {
                Object object = actingContext.getGraphManager().getNode(
                        oidQuery.getObjectId());
                if (object != null) {

                    if (oidQuery.isFetchingDataRows()) {
                        object = ((DataContext) actingContext)
                                .currentSnapshot((DataObject) object);
                    }

                    this.response = new ListResponse(object);
                    return DONE;
                }
            }
        }

        return !DONE;
    }

    private boolean interceptPaginatedQuery() {
        if (metadata.getPageSize() > 0) {
            response = new ListResponse(new IncrementalFaultList(
                    (DataContext) actingContext,
                    query));
            return DONE;
        }

        return !DONE;
    }

    /*
     * Wraps execution in local cache checks.
     */
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

        ObjectStore objectStore = ((DataContext) actingContext).getObjectStore();
        if (cache) {

            List cachedResults = objectStore.getCachedQueryResult(cacheKey);
            if (cachedResults != null) {
                response = new ListResponse(cachedResults);
                return DONE;
            }
        }

        runQuery();
        objectStore.cacheQueryResult(cacheKey, response.firstList());
        return DONE;
    }
}
