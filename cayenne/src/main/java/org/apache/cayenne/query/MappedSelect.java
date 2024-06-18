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

import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.ResultIteratorCallback;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.QueryDescriptor;

/**
 * A query that represents a named parameterized selecting query stored in the mapping. The
 * actual query is resolved during execution.
 *
 * @since 4.0
 */
public class MappedSelect<T> extends AbstractMappedQuery implements Select<T> {

    /**
     * Loads query with the given name, which selects objects of a given persistent class,
     * from the mapping configuration.
     *
     * @param queryName name of the mapped query
     * @param rootClass the Class of objects fetched by this query
     */
    public static <T> MappedSelect<T> query(String queryName, Class<T> rootClass) {
        return new MappedSelect<>(queryName, rootClass);
    }

    /**
     * Loads query with the given name from the mapping configuration.
     *
     * @param queryName name of the mapped query
     */
    public static MappedSelect<?> query(String queryName) {
        return new MappedSelect<>(queryName);
    }

    protected Class<T> resultClass;
    protected Integer fetchLimit;
    protected Integer fetchOffset;
    protected Integer statementFetchSize;
    protected Integer queryTimeout;
    protected Integer pageSize;
    protected boolean forceNoCache;

    protected MappedSelect(String queryName) {
        super(queryName);
    }

    protected MappedSelect(String queryName, Class<T> resultClass) {
        super(queryName);
        this.resultClass = resultClass;
    }

    /**
     * Resets query fetch limit - a parameter that defines max number of objects
     * that should be ever be fetched from the database.
     */
    public MappedSelect<T> limit(int fetchLimit) {
        this.fetchLimit = fetchLimit;
        this.replacementQuery = null;
        return this;
    }

    /**
     * Resets query fetch offset - a parameter that defines how many objects
     * should be skipped when reading data from the database.
     */
    public MappedSelect<T> offset(int fetchOffset) {
        this.fetchOffset = fetchOffset;
        this.replacementQuery = null;
        return this;
    }

    /**
     * Sets fetch size of the PreparedStatement generated for this query. Only
     * non-negative values would change the default size.
     *
     * @see Statement#setFetchSize(int)
     */
    public MappedSelect<T> statementFetchSize(int statementFetchSize) {
        this.statementFetchSize = statementFetchSize;
        this.replacementQuery = null;
        return this;
    }

    /**
     * Sets query timeout for the PreparedStatement generated for this query.
     *
     * @see Statement#setQueryTimeout(int)
     * @since 4.2
     */
    public MappedSelect<T> queryTimeout(int timeout) {
        this.queryTimeout = timeout;
        this.replacementQuery = null;
        return this;
    }

    /**
     * Resets query page size. A non-negative page size enables query result
     * pagination that saves memory and processing time for large lists if only
     * parts of the result are ever going to be accessed.
     */
    public MappedSelect<T> pageSize(int pageSize) {
        this.pageSize = pageSize;
        this.replacementQuery = null;
        return this;
    }

    /**
     * Forces query cache to be refreshed during the execution of this query.
     */
    public MappedSelect<T> forceNoCache() {
        this.forceNoCache = true;
        this.replacementQuery = null;
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public MappedSelect<T> params(Map<String, ?> parameters) {
        return (MappedSelect<T>) super.params(parameters);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MappedSelect<T> param(String name, Object value) {
        return (MappedSelect<T>) super.param(name, value);
    }

    public List<T> select(ObjectContext context) {
        return context.select(this);
    }

    @Override
    public T selectOne(ObjectContext context) {
        return context.selectOne(this);
    }

    @Override
    public T selectFirst(ObjectContext context) {
        return context.selectFirst(limit(1));
    }

    @Override
    public void iterate(ObjectContext context, ResultIteratorCallback<T> callback) {
        context.iterate(this, callback);
    }

    @Override
    public ResultIterator<T> iterator(ObjectContext context) {
        return context.iterator(this);
    }

    @Override
    public ResultBatchIterator<T> batchIterator(ObjectContext context, int size) {
        return context.batchIterator(this, size);
    }

    @Override
    protected Query createReplacementQuery(EntityResolver resolver) {
        QueryDescriptor descriptor = resolver.getQueryDescriptor(queryName);

        Query query = super.createReplacementQuery(resolver);

        QueryCacheStrategy cacheStrategyOverride = null;

        if (forceNoCache) {
            QueryCacheStrategy cacheStrategy = query.getMetaData(resolver).getCacheStrategy();
            if (QueryCacheStrategy.LOCAL_CACHE == cacheStrategy) {
                cacheStrategyOverride = QueryCacheStrategy.LOCAL_CACHE_REFRESH;
            } else if (QueryCacheStrategy.SHARED_CACHE == cacheStrategy) {
                cacheStrategyOverride = QueryCacheStrategy.SHARED_CACHE_REFRESH;
            }
        }

        switch (descriptor.getType()) {
            case QueryDescriptor.SELECT_QUERY:
                ObjectSelect<?> selectQuery = (ObjectSelect<?>) query;
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
                    selectQuery.setCacheStrategy(cacheStrategyOverride);
                }
                break;
            case QueryDescriptor.SQL_TEMPLATE:
                SQLTemplate sqlTemplate = (SQLTemplate) query;
                if (fetchLimit != null) {
                    sqlTemplate.setFetchLimit(fetchLimit);
                }
                if (fetchOffset != null) {
                    sqlTemplate.setFetchOffset(fetchOffset);
                }
                if (statementFetchSize != null) {
                    sqlTemplate.setStatementFetchSize(statementFetchSize);
                }
                if(queryTimeout != null) {
                    sqlTemplate.setQueryTimeout(queryTimeout);
                }
                if (pageSize != null) {
                    sqlTemplate.setPageSize(pageSize);
                }
                if (cacheStrategyOverride != null) {
                    sqlTemplate.setCacheStrategy(cacheStrategyOverride);
                }
                break;
            case QueryDescriptor.EJBQL_QUERY:
                EJBQLQuery ejbqlQuery = (EJBQLQuery) query;
                if (fetchLimit != null) {
                    ejbqlQuery.setFetchLimit(fetchLimit);
                }
                if (fetchOffset != null) {
                    ejbqlQuery.setFetchOffset(fetchOffset);
                }
                if (statementFetchSize != null) {
                    ejbqlQuery.setStatementFetchSize(statementFetchSize);
                }
                if(queryTimeout != null) {
                    ejbqlQuery.setQueryTimeout(queryTimeout);
                }
                if (pageSize != null) {
                    ejbqlQuery.setPageSize(pageSize);
                }
                if (cacheStrategyOverride != null) {
                    ejbqlQuery.setCacheStrategy(cacheStrategyOverride);
                }
                break;
            case QueryDescriptor.PROCEDURE_QUERY:
                ProcedureQuery procedureQuery = (ProcedureQuery) query;
                if (fetchLimit != null) {
                    procedureQuery.setFetchLimit(fetchLimit);
                }
                if (fetchOffset != null) {
                    procedureQuery.setFetchOffset(fetchOffset);
                }
                if (statementFetchSize != null) {
                    procedureQuery.setStatementFetchSize(statementFetchSize);
                }
                if(queryTimeout != null) {
                    procedureQuery.setQueryTimeout(queryTimeout);
                }
                if (pageSize != null) {
                    procedureQuery.setPageSize(pageSize);
                }
                if (cacheStrategyOverride != null) {
                    procedureQuery.setCacheStrategy(cacheStrategyOverride);
                }
                break;
            default:
                throw new CayenneRuntimeException("Unknown query type: %s", descriptor.getType());
        }

        return query;
    }
}
