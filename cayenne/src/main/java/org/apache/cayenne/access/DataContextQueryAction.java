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

package org.apache.cayenne.access;

import org.apache.cayenne.QueryResult;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.cache.QueryCacheEntryFactory;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.QueryMetadata;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

class DataContextQueryAction {

    private static final boolean DONE = true;

    private final DataContext context;
    private final Query query;
    private final QueryMetadata metadata;
    private final boolean iteratedResult;
    private final boolean ignoreLocalCache;
    private List<QueryResult> response;

    public DataContextQueryAction(DataContext context, Query query, boolean iteratedResult, boolean ignoreLocalCache) {
        this.context = context;
        this.query = query;
        this.metadata = query.getMetaData(context.getEntityResolver());
        this.iteratedResult = iteratedResult;
        this.ignoreLocalCache = ignoreLocalCache;
    }

    /**
     * Worker method that performs internal query.
     */
    public List<QueryResult> execute() {
        if (interceptIteratedQuery() != DONE) {
            if (interceptLocalCache() != DONE) {
                executePostCache();
            }
        }

        return response;
    }

    private boolean interceptIteratedQuery() {
        if (iteratedResult) {
            runQuery();
            return DONE;
        }
        return !DONE;
    }

    private void executePostCache() {
        if (interceptPaginatedQuery() != DONE) {
            runQuery();
        }
    }


    protected boolean interceptPaginatedQuery() {
        if (metadata.getPageSize() > 0) {
            // this will select raw ids
            runQuery();

            List<?> rawIds = QueryResults.firstList(response);
            int maxIdQualifierSize = context.getChannel().getDataDomain().getMaxIdQualifierSize();
            IncrementalFaultList<?> paginatedList = createIncrementalFaultList(rawIds, maxIdQualifierSize);

            // replace result with a paginated list that will deal with id-to-object resolution
            response = List.of(new QueryResult.Select<>(paginatedList));
            return DONE;
        }

        return !DONE;
    }

    private IncrementalFaultList<?> createIncrementalFaultList(List<?> rawIds, int maxIdQualifierSize) {
        // just a sanity check
        Objects.requireNonNull(rawIds, "Trying to execute paginated query that is not a select query");
        if (isMixedResultsForPaginatedQuery()) {
            return new MixedResultIncrementalFaultList<>(context, query, maxIdQualifierSize, rawIds);
        } else {
            DbEntity dbEntity = metadata.getDbEntity();
            if (dbEntity != null && dbEntity.getPrimaryKeys().size() == 1) {
                return new SimpleIdIncrementalFaultList<>(context, query, maxIdQualifierSize, rawIds);
            } else {
                return new IncrementalFaultList<>(context, query, maxIdQualifierSize, rawIds);
            }
        }
    }

    private boolean isMixedResultsForPaginatedQuery() {
        boolean mixedResults = false;
        List<Object> rsMapping = metadata.getResultSetMapping();
        if (rsMapping != null) {
            if (rsMapping.size() > 1) {
                mixedResults = true;
            } else if (rsMapping.size() == 1) {
                mixedResults = !(rsMapping.getFirst() instanceof EntityResultSegment)
                        || !metadata.isSingleResultSetMapping();
            }
        }
        return mixedResults;
    }

    protected boolean interceptLocalCache() {

        // If the query was originated in a child context, don't use this context local cache...
        // TODO: I suppose if the request came from a child context, instead of ignoring cache at this level, we should
        //  attempt to read from it, but never write (we don't want another copy of the object list). Or maybe writing
        //  is fine too. The objects are resolved here anyways
        if (ignoreLocalCache) {
            return !DONE;
        }

        if (metadata.getCacheKey() == null) {
            return !DONE;
        }

        boolean cache = QueryCacheStrategy.LOCAL_CACHE == metadata.getCacheStrategy();
        boolean cacheOrCacheRefresh = cache
                || QueryCacheStrategy.LOCAL_CACHE_REFRESH == metadata.getCacheStrategy();

        if (!cacheOrCacheRefresh) {
            return !DONE;
        }

        QueryCache queryCache = getQueryCache();
        QueryCacheEntryFactory factory = getCacheObjectFactory();

        if (cache) {
            boolean wasResponseNull = (response == null);
            List<?> cachedResults = queryCache.get(metadata, factory);

            // response may already be initialized by the factory above ... it is null if
            // there was a preexisting cache entry
            // the cache may hold no list at all if the query produced no select result
            if (response == null || wasResponseNull) {
                response = cachedResults != null ? List.of(new QueryResult.Select<>(cachedResults)) : List.of();
            }
        } else {
            // on cache-refresh request, fetch without blocking and fill the cache
            queryCache.put(metadata, factory.createObject());
        }

        return DONE;
    }

    protected QueryCache getQueryCache() {
        return context.getQueryCache();
    }

    protected QueryCacheEntryFactory getCacheObjectFactory() {
        return () -> {
            executePostCache();
            List<?> result = QueryResults.firstList(response);
            // make an immutable list to make sure callers don't mess it up
            return result != null ? Collections.unmodifiableList(result) : null;
        };
    }

    /**
     * Fetches data from the channel.
     */
    protected void runQuery() {
        this.response = context.getChannel().onQuery(context, query, iteratedResult);
    }
}
