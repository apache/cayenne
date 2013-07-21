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

package org.apache.cayenne.remote;

import java.util.List;
import java.util.Map;

import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.QueryRouter;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SQLActionVisitor;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * A Query that fetches a range of objects from a previously fetched server-side paginated
 * list. This query is client-only and can't be executed on the server.
 * 
 * @since 1.2
 */
class RangeQuery implements Query {

    private String cacheKey;
    private int fetchOffset;
    private int fetchLimit;
    private Query originatingQuery;

    // exists for hessian serialization.
    @SuppressWarnings("unused")
    private RangeQuery() {

    }

    /**
     * Creates a query that returns a single page from an existing cached server-side
     * result list.
     */
    RangeQuery(String cacheKey, int fetchStartIndex, int fetchLimit,
            Query originatingQuery) {
        this.cacheKey = cacheKey;
        this.fetchOffset = fetchStartIndex;
        this.fetchLimit = fetchLimit;
        this.originatingQuery = originatingQuery;
    }

    /**
     * @since 3.1
     */
    public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
        return visitor.visitQuery(this);
    }

    public QueryMetadata getMetaData(EntityResolver resolver) {
        final QueryMetadata originatingMetadata = originatingQuery.getMetaData(resolver);

        return new QueryMetadata() {

            public Query getOrginatingQuery() {
                return originatingQuery;
            }

            public List<Object> getResultSetMapping() {
                return null;
            }

            public String getCacheKey() {
                return cacheKey;
            }

            public String[] getCacheGroups() {
                return null;
            }

            public int getFetchOffset() {
                return fetchOffset;
            }

            public int getFetchLimit() {
                return fetchLimit;
            }

            public boolean isFetchingDataRows() {
                return originatingMetadata.isFetchingDataRows();
            }

            public int getPageSize() {
                return 0;
            }

            /**
             * @since 3.0
             */
            public QueryCacheStrategy getCacheStrategy() {
                return QueryCacheStrategy.getDefaultStrategy();
            }

            public PrefetchTreeNode getPrefetchTree() {
                return originatingMetadata.getPrefetchTree();
            }

            public DataMap getDataMap() {
                throw new UnsupportedOperationException();
            }

            public DbEntity getDbEntity() {
                throw new UnsupportedOperationException();
            }

            public ObjEntity getObjEntity() {
                throw new UnsupportedOperationException();
            }

            public ClassDescriptor getClassDescriptor() {
                throw new UnsupportedOperationException();
            }

            public Procedure getProcedure() {
                throw new UnsupportedOperationException();
            }

            public Map<String, String> getPathSplitAliases() {
                throw new UnsupportedOperationException();
            }

            public boolean isRefreshingObjects() {
                throw new UnsupportedOperationException();
            }

            public int getStatementFetchSize() {
                return 0;
            }
        };
    }

    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        throw new UnsupportedOperationException();
    }

    public String getName() {
        throw new UnsupportedOperationException();
    }

    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.1
     */
    public DataMap getDataMap() {
        throw new UnsupportedOperationException();
    }
}
