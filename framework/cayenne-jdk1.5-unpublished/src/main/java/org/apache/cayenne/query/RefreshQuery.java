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
package org.apache.cayenne.query;

import java.util.Arrays;
import java.util.Collection;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;

/**
 * A query that allows to explicitly clear both object and list caches either via refetch
 * (eager refresh) or invalidate (lazy refresh).
 * 
 * @since 3.0
 */
public class RefreshQuery implements Query {

    protected Collection<?> objects;
    protected Query query;
    protected String[] groupKeys;

    /**
     * Creates a RefreshQuery that does full refresh of all registered objects, cascading
     * refresh all the way to the shared cache.
     */
    public RefreshQuery() {

    }

    /**
     * Creates a RefreshQuery that refreshes a collection of objects, including
     * invalidation of their relationships.
     */
    public RefreshQuery(Collection<?> objects) {
        this.objects = objects;
    }

    /**
     * Creates a RefreshQuery that refreshes a single object, including invalidation of
     * its relationships.
     */
    public RefreshQuery(Persistent object) {
        this(Arrays.asList(object));
    }

    /**
     * Creates a RefreshQuery that refreshes results of a query and individual objects in
     * the result.
     */
    public RefreshQuery(Query query) {
        this.query = query;
    }

    /**
     * Creates a RefreshQuery that refreshes query results identified by group keys.
     */
    public RefreshQuery(String... groupKeys) {
        this.groupKeys = groupKeys;
    }
    
    /**
     * @since 3.1
     */
    public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
        return visitor.visitQuery(this);
    }

    public QueryMetadata getMetaData(EntityResolver resolver) {
        return new BaseQueryMetadata();
    }

    public String getName() {
        return null;
    }

    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        // noop
    }

    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        throw new CayenneRuntimeException("Unsupported operation");
    }

    public boolean isRefreshAll() {
        return objects == null && query == null && groupKeys == null;
    }

    public String[] getGroupKeys() {
        return groupKeys;
    }

    public Collection<?> getObjects() {
        return objects;
    }

    /**
     * Returns an internal query, overriding cache policy to force a refresh. Returns null
     * if no query was set.
     */
    public Query getQuery() {

        if (query == null) {
            return null;
        }

        return new Query() {

            public SQLAction createSQLAction(SQLActionVisitor visitor) {
                throw new CayenneRuntimeException("Unsupported");
            }
            
            public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
                return visitor.visitQuery(this);
            }

            public QueryMetadata getMetaData(EntityResolver resolver) {
                QueryMetadata md = query.getMetaData(resolver);

                QueryMetadataWrapper wrappedMd = new QueryMetadataWrapper(md);
                if (QueryCacheStrategy.LOCAL_CACHE == md.getCacheStrategy()) {
                    wrappedMd.override(
                            QueryMetadata.CACHE_STRATEGY_PROPERTY,
                            QueryCacheStrategy.LOCAL_CACHE_REFRESH);
                }
                else if (QueryCacheStrategy.SHARED_CACHE == md.getCacheStrategy()) {
                    wrappedMd.override(
                            QueryMetadata.CACHE_STRATEGY_PROPERTY,
                            QueryCacheStrategy.SHARED_CACHE_REFRESH);
                }

                return wrappedMd;
            }

            public String getName() {
                return query.getName();
            }

            public void route(
                    QueryRouter router,
                    EntityResolver resolver,
                    Query substitutedQuery) {
                query.route(router, resolver, this);
            }

            public DataMap getDataMap() {
                return query.getDataMap();
            }
        };
    }

    public DataMap getDataMap() {
        return null;
    }
}
