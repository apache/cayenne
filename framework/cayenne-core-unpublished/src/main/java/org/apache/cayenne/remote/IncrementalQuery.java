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
 * A client wrapper for the incremental query that overrides the metadata to ensure that
 * query result is cached on the server, so that subranges could be retrieved at a later
 * time.
 * 
 * @since 1.2
 */
class IncrementalQuery implements Query {

    private Query query;
    private String cacheKey;

    IncrementalQuery(Query query, String cacheKey) {
        this.query = query;
        this.cacheKey = cacheKey;
    }
    
    /**
     * @since 3.1
     */
    public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
        return visitor.visitQuery(this);
    }

    public QueryMetadata getMetaData(EntityResolver resolver) {
        final QueryMetadata metadata = query.getMetaData(resolver);

        // the way paginated queries work on the server is that they are never cached
        // (IncrementalFaultList interception happens before cache interception). So
        // overriding caching settings in the metadata will only affect
        // ClientServerChannel behavior

        return new QueryMetadata() {

            public Query getOrginatingQuery() {
                return null;
            }

            public String getCacheKey() {
                return cacheKey;
            }

            public List<Object> getResultSetMapping() {
                return metadata.getResultSetMapping();
            }

            public String[] getCacheGroups() {
                return metadata.getCacheGroups();
            }

            /**
             * @since 3.0
             */
            public QueryCacheStrategy getCacheStrategy() {
                return metadata.getCacheStrategy();
            }

            public DataMap getDataMap() {
                return metadata.getDataMap();
            }

            public DbEntity getDbEntity() {
                return metadata.getDbEntity();
            }

            public int getFetchLimit() {
                return metadata.getFetchLimit();
            }

            public int getFetchOffset() {
                return metadata.getFetchOffset();
            }

            public ObjEntity getObjEntity() {
                return metadata.getObjEntity();
            }

            public ClassDescriptor getClassDescriptor() {
                return metadata.getClassDescriptor();
            }

            public int getPageSize() {
                return metadata.getPageSize();
            }

            public PrefetchTreeNode getPrefetchTree() {
                return metadata.getPrefetchTree();
            }

            public Procedure getProcedure() {
                return metadata.getProcedure();
            }

            public Map<String, String> getPathSplitAliases() {
                return metadata.getPathSplitAliases();
            }

            public boolean isFetchingDataRows() {
                return metadata.isFetchingDataRows();
            }

            public boolean isRefreshingObjects() {
                return metadata.isRefreshingObjects();
            }

            public int getStatementFetchSize() {
                return metadata.getStatementFetchSize();
            }
        };
    }

    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        query.route(router, resolver, substitutedQuery);
    }

    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        return query.createSQLAction(visitor);
    }

    public String getName() {
        return query.getName();
    }

    /**
     * @since 3.1
     */
    public DataMap getDataMap() {
        return query.getDataMap();
    }
}
