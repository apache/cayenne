/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/
package org.apache.cayenne.query;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ProcedureQueryDescriptor;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.SQLTemplateDescriptor;
import org.apache.cayenne.map.SelectQueryDescriptor;

import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A query that is a reference to a named parameterized query stored in the mapping. The actual query is resolved
 * during execution and can be an ObjectSelect, a SQLTemplate or a ProcedureQuery, selecting or not. A selecting
 * mapped query returns its rows via {@link #select(ObjectContext)} and the other selecting shortcuts, and everything
 * any mapped query produced, including update counts, is available via {@link #execute(ObjectContext)}.
 *
 * @param <T> the type of the rows the query selects
 * @since 6.0
 */
public class MappedQuery<T> extends IndirectQuery<T> {

    /**
     * Loads query with the given name, which selects objects of a given persistent class, from the mapping
     * configuration.
     *
     * @param queryName name of the mapped query
     * @param rootClass the Class of objects fetched by this query
     */
    public static <T> MappedQuery<T> query(String queryName, Class<T> rootClass) {
        return new MappedQuery<>(queryName, rootClass);
    }

    /**
     * Loads query with the given name from the mapping configuration.
     *
     * @param queryName name of the mapped query
     */
    public static MappedQuery<?> query(String queryName) {
        return new MappedQuery<>(queryName);
    }

    protected String queryName;
    protected Class<T> resultClass;
    protected Map<String, Object> params;
    protected Integer fetchLimit;
    protected Integer fetchOffset;
    protected Integer statementFetchSize;
    protected Integer queryTimeout;
    protected Integer pageSize;
    protected boolean forceNoCache;

    protected MappedQuery(String queryName) {
        this.queryName = queryName;
    }

    protected MappedQuery(String queryName, Class<T> resultClass) {
        this.queryName = queryName;
        this.resultClass = resultClass;
    }

    public MappedQuery<T> name(String queryName) {
        this.queryName = queryName;
        this.replacementQuery = null;
        return this;
    }

    public MappedQuery<T> params(Map<String, ?> parameters) {
        if (this.params == null) {
            this.params = new HashMap<>(parameters);
        } else {
            this.params.putAll(parameters);
        }

        this.replacementQuery = null;
        return this;
    }

    public MappedQuery<T> param(String name, Object value) {
        if (this.params == null) {
            this.params = new HashMap<>();
        }

        this.params.put(name, value);
        this.replacementQuery = null;
        return this;
    }

    /**
     * Resets query fetch limit - a parameter that defines max number of objects that should be ever be fetched from
     * the database.
     */
    public MappedQuery<T> limit(int fetchLimit) {
        this.fetchLimit = fetchLimit;
        this.replacementQuery = null;
        return this;
    }

    /**
     * Resets query fetch offset - a parameter that defines how many objects should be skipped when reading data from
     * the database.
     */
    public MappedQuery<T> offset(int fetchOffset) {
        this.fetchOffset = fetchOffset;
        this.replacementQuery = null;
        return this;
    }

    /**
     * Sets fetch size of the PreparedStatement generated for this query. Any non-zero value is passed to the driver
     * as is; 0 (the default) leaves the fetch size to the adapter and the driver. A negative value is only meaningful
     * for drivers that give it a special meaning, such as {@link Integer#MIN_VALUE} for streaming on MySQL.
     *
     * @see Statement#setFetchSize(int)
     */
    public MappedQuery<T> statementFetchSize(int statementFetchSize) {
        this.statementFetchSize = statementFetchSize;
        this.replacementQuery = null;
        return this;
    }

    /**
     * Sets query timeout for the PreparedStatement generated for this query.
     *
     * @see Statement#setQueryTimeout(int)
     */
    public MappedQuery<T> queryTimeout(int timeout) {
        this.queryTimeout = timeout;
        this.replacementQuery = null;
        return this;
    }

    /**
     * Resets query page size. A non-negative page size enables query result pagination that saves memory and
     * processing time for large lists if only parts of the result are ever going to be accessed.
     */
    public MappedQuery<T> pageSize(int pageSize) {
        this.pageSize = pageSize;
        this.replacementQuery = null;
        return this;
    }

    /**
     * Forces query cache to be refreshed during the execution of this query.
     */
    public MappedQuery<T> forceNoCache() {
        this.forceNoCache = true;
        this.replacementQuery = null;
        return this;
    }

    @Override
    public T selectFirst(ObjectContext context) {
        return context.selectFirst(limit(1));
    }

    /**
     * Returns a non-null parameter map, substituting all persistent objects in the initial map with ObjectIds.
     */
    Map<String, ?> normalizedParameters() {
        if (params == null || params.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> substitutes = new HashMap<>(params);

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof Persistent persistent) {
                value = persistent.getObjectId();
            }

            substitutes.put(entry.getKey(), value);
        }

        return substitutes;
    }

    @Override
    protected Query<?> createReplacementQuery(EntityResolver resolver) {
        QueryDescriptor descriptor = resolver.getQueryDescriptor(queryName);
        Query<?> query = descriptor.buildQuery(normalizedParameters());

        QueryCacheStrategy cacheStrategyOverride = null;

        if (forceNoCache) {
            QueryCacheStrategy cacheStrategy = query.getMetaData(resolver).getCacheStrategy();
            if (QueryCacheStrategy.LOCAL_CACHE == cacheStrategy) {
                cacheStrategyOverride = QueryCacheStrategy.LOCAL_CACHE_REFRESH;
            } else if (QueryCacheStrategy.SHARED_CACHE == cacheStrategy) {
                cacheStrategyOverride = QueryCacheStrategy.SHARED_CACHE_REFRESH;
            }
        }

        switch (descriptor) {
            case SelectQueryDescriptor ignored -> {
                FluentSelect<?, ?> selectQuery = (FluentSelect<?, ?>) query;
                if (fetchLimit != null) {
                    selectQuery.limit(fetchLimit);
                }
                if (fetchOffset != null) {
                    selectQuery.offset(fetchOffset);
                }
                if (statementFetchSize != null) {
                    selectQuery.statementFetchSize(statementFetchSize);
                }
                if (pageSize != null) {
                    selectQuery.pageSize(pageSize);
                }
                if (cacheStrategyOverride != null) {
                    selectQuery.cacheStrategy(cacheStrategyOverride, selectQuery.getCacheGroup());
                }
            }
            case SQLTemplateDescriptor ignored -> {
                SQLTemplate<?> sqlTemplate = (SQLTemplate<?>) query;
                if (fetchLimit != null) {
                    sqlTemplate.setFetchLimit(fetchLimit);
                }
                if (fetchOffset != null) {
                    sqlTemplate.setFetchOffset(fetchOffset);
                }
                if (statementFetchSize != null) {
                    sqlTemplate.setStatementFetchSize(statementFetchSize);
                }
                if (queryTimeout != null) {
                    sqlTemplate.setQueryTimeout(queryTimeout);
                }
                if (pageSize != null) {
                    sqlTemplate.setPageSize(pageSize);
                }
                if (cacheStrategyOverride != null) {
                    sqlTemplate.setCacheStrategy(cacheStrategyOverride);
                }
            }
            case ProcedureQueryDescriptor ignored -> {
                ProcedureQuery<?> procedureQuery = (ProcedureQuery<?>) query;
                if (fetchLimit != null) {
                    procedureQuery.setFetchLimit(fetchLimit);
                }
                if (fetchOffset != null) {
                    procedureQuery.setFetchOffset(fetchOffset);
                }
                if (statementFetchSize != null) {
                    procedureQuery.setStatementFetchSize(statementFetchSize);
                }
                if (queryTimeout != null) {
                    procedureQuery.setQueryTimeout(queryTimeout);
                }
                if (pageSize != null) {
                    procedureQuery.setPageSize(pageSize);
                }
                if (cacheStrategyOverride != null) {
                    procedureQuery.setCacheStrategy(cacheStrategyOverride);
                }
            }
            default -> throw new CayenneRuntimeException("Unknown query type: %s", descriptor.getClass().getName());
        }

        return query;
    }
}
