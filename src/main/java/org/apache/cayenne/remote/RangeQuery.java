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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.QueryRouter;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SQLActionVisitor;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * An Query wrapper that triggers pagination processing on the server. This query is
 * client-only and can't be executed on the server.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class RangeQuery implements Query {

    private String cacheKey;
    private int fetchStartIndex;
    private int fetchLimit;
    private boolean fetchingDataRows;
    private PrefetchTreeNode prefetchTree;

    // exists for hessian serialization.
    private RangeQuery() {

    }

    /**
     * Creates a PaginatedQuery that returns a single page from an existing cached
     * server-side result list.
     */
    public RangeQuery(String cacheKey, int fetchStartIndex, int fetchLimit,
            QueryMetadata rootMetadata) {
        this.cacheKey = cacheKey;
        this.fetchStartIndex = fetchStartIndex;
        this.fetchLimit = fetchLimit;
        this.fetchingDataRows = rootMetadata.isFetchingDataRows();
        this.prefetchTree = rootMetadata.getPrefetchTree();
    }

    public QueryMetadata getMetaData(EntityResolver resolver) {
        return new QueryMetadata() {

            public String getCacheKey() {
                return cacheKey;
            }

            public String[] getCacheGroups() {
                return null;
            }

            public int getFetchStartIndex() {
                return fetchStartIndex;
            }

            public int getFetchLimit() {
                return fetchLimit;
            }

            public boolean isFetchingDataRows() {
                return fetchingDataRows;
            }

            public int getPageSize() {
                return 0;
            }

            public String getCachePolicy() {
                return QueryMetadata.NO_CACHE;
            }

            public PrefetchTreeNode getPrefetchTree() {
                return prefetchTree;
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

            public boolean isRefreshingObjects() {
                throw new UnsupportedOperationException();
            }

            public boolean isResolvingInherited() {
                throw new UnsupportedOperationException();
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
}
