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
package org.apache.cayenne.query;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.query.resolved.ResolvedProcedure;
import org.apache.cayenne.query.resolved.ResolvedQuery;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.util.ToStringBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A query calling a stored procedure mapped in a DataMap, with a fluent API. A procedure may return any mix of
 * result sets, update counts and OUT parameters, all available via {@link #execute(ObjectContext)}; the selecting
 * shortcuts return the rows of the first result set as objects of the result type or as {@link DataRow}s.
 *
 * @param <T> the type of the rows the query selects
 * @since 6.0
 */
public class ProcedureQuery<T> implements Query<T> {

    /**
     * Creates a query calling the named stored procedure. Any result sets it produces are returned as DataRows. This
     * is the form to use for procedures that update rather than select, when the shape of the result does not matter.
     */
    public static ProcedureQuery<DataRow> query(String procedure) {
        ProcedureQuery<DataRow> query = new ProcedureQuery<>(procedure);
        query.fetchingDataRows = true;
        return query;
    }

    /**
     * Creates a query calling the named stored procedure and fetching the result sets as objects of the given
     * persistent type.
     */
    public static <T> ProcedureQuery<T> query(String procedure, Class<T> resultClass) {
        ProcedureQuery<T> query = new ProcedureQuery<>(procedure);
        query.resultClass = resultClass;
        return query;
    }

    /**
     * Creates a query calling the named stored procedure and fetching the result sets as objects of the named
     * ObjEntity. This is the form used for queries mapped in a DataMap, where the entity is known by name.
     *
     * @since 6.0
     */
    public static ProcedureQuery<Persistent> entityQuery(String procedure, String resultEntityName) {
        ProcedureQuery<Persistent> query = new ProcedureQuery<>(procedure);
        query.resultEntityName = resultEntityName;
        return query;
    }

    protected final String procedureName;
    // the type of the fetched objects: at most one of the two is set
    Class<T> resultClass;
    private String resultEntityName;
    private Map<String, Object> params;
    private int fetchLimit = ResolvedQuery.FETCH_LIMIT_DEFAULT;
    private int fetchOffset = ResolvedQuery.FETCH_OFFSET_DEFAULT;
    private int pageSize = ResolvedQuery.PAGE_SIZE_DEFAULT;
    private int statementFetchSize = ResolvedQuery.STATEMENT_FETCH_SIZE_DEFAULT;
    private int queryTimeout = ResolvedQuery.QUERY_TIMEOUT_DEFAULT;
    private CapsStrategy capsStrategy;
    private boolean fetchingDataRows;
    private List<ProcedureColumn[]> resultDescriptors;
    private QueryCacheStrategy cacheStrategy = QueryCacheStrategy.getDefaultStrategy();
    private String cacheGroup;

    protected ProcedureQuery(String procedureName) {
        this.procedureName = procedureName;
    }

    public ProcedureQuery<T> params(Map<String, ?> parameters) {
        if (this.params == null) {
            this.params = new HashMap<>(parameters);
        } else {
            this.params.putAll(parameters);
        }
        return this;
    }

    public ProcedureQuery<T> param(String name, Object value) {
        if (this.params == null) {
            this.params = new HashMap<>();
        }
        this.params.put(name, value);
        return this;
    }

    public ProcedureQuery<T> limit(int fetchLimit) {
        this.fetchLimit = fetchLimit;
        return this;
    }

    public ProcedureQuery<T> offset(int fetchOffset) {
        this.fetchOffset = fetchOffset;
        return this;
    }

    /**
     * @since 6.0
     */
    public ProcedureQuery<T> pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    /**
     * @since 6.0
     */
    public ProcedureQuery<T> statementFetchSize(int statementFetchSize) {
        this.statementFetchSize = statementFetchSize;
        return this;
    }

    /**
     * @since 6.0
     */
    public ProcedureQuery<T> queryTimeout(int queryTimeout) {
        this.queryTimeout = queryTimeout;
        return this;
    }

    public ProcedureQuery<T> capsStrategy(CapsStrategy capsStrategy) {
        this.capsStrategy = capsStrategy;
        return this;
    }

    /**
     * Makes the query return its rows as DataRows instead of objects.
     *
     * @since 6.0
     */
    @SuppressWarnings("unchecked")
    public ProcedureQuery<DataRow> fetchDataRows() {
        this.fetchingDataRows = true;
        return (ProcedureQuery<DataRow>) this;
    }

    /**
     * Describes the columns of the next result set of the procedure, for drivers that do not describe them (or do it
     * wrong). When any result set is described, all of them must be, in the order they are returned.
     */
    public ProcedureQuery<T> resultDescriptor(ProcedureColumn[] resultDescriptor) {
        if (resultDescriptors == null) {
            resultDescriptors = new ArrayList<>(2);
        }
        resultDescriptors.add(resultDescriptor);
        return this;
    }

    /**
     * @since 6.0
     */
    public ProcedureQuery<T> cacheStrategy(QueryCacheStrategy strategy) {
        this.cacheStrategy = strategy != null ? strategy : QueryCacheStrategy.getDefaultStrategy();
        this.cacheGroup = null;
        return this;
    }

    /**
     * @since 6.0
     */
    public ProcedureQuery<T> cacheStrategy(QueryCacheStrategy strategy, String cacheGroup) {
        return cacheStrategy(strategy).cacheGroup(cacheGroup);
    }

    /**
     * @since 6.0
     */
    public ProcedureQuery<T> cacheGroup(String cacheGroup) {
        this.cacheGroup = cacheGroup;
        return this;
    }

    /**
     * @since 6.0
     */
    public ProcedureQuery<T> localCache() {
        return cacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
    }

    /**
     * @since 6.0
     */
    public ProcedureQuery<T> localCache(String cacheGroup) {
        return cacheStrategy(QueryCacheStrategy.LOCAL_CACHE, cacheGroup);
    }

    /**
     * @since 6.0
     */
    public ProcedureQuery<T> sharedCache() {
        return cacheStrategy(QueryCacheStrategy.SHARED_CACHE);
    }

    /**
     * @since 6.0
     */
    public ProcedureQuery<T> sharedCache(String cacheGroup) {
        return cacheStrategy(QueryCacheStrategy.SHARED_CACHE, cacheGroup);
    }

    @Override
    public T selectFirst(ObjectContext context) {
        return context.selectFirst(limit(1));
    }

    @Override
    public ResolvedProcedure resolve(EntityResolver resolver) {
        Procedure procedure = resolver.getProcedure(procedureName);
        if (procedure == null) {
            throw new CayenneRuntimeException("Unknown procedure: %s", procedureName);
        }

        ObjEntity entity = null;
        if (resultClass != null) {
            entity = resolver.getObjEntity(resultClass);
        } else if (resultEntityName != null) {
            entity = resolver.getObjEntity(resultEntityName);
        }

        DbEntity dbEntity = null;
        ClassDescriptor descriptor = null;
        // theoretically procedure can be in one DataMap, while the Java Class - in another.
        DataMap dataMap = procedure.getDataMap();
        if (entity != null) {
            dbEntity = entity.getDbEntity();
            dataMap = entity.getDataMap();
            descriptor = resolver.getClassDescriptor(entity.getName());
        }

        Map<String, Object> params = this.params != null ? this.params : Collections.emptyMap();
        List<ProcedureColumn[]> resultDescriptors = this.resultDescriptors != null
                ? this.resultDescriptors
                : Collections.emptyList();
        CapsStrategy capsStrategy = this.capsStrategy != null ? this.capsStrategy : CapsStrategy.DEFAULT;
        String cacheKey = cacheStrategy != QueryCacheStrategy.NO_CACHE ? cacheKey(procedure, params) : null;

        return new ResolvedProcedure(this, procedure, entity, dbEntity, dataMap, descriptor, params,
                resultDescriptors, capsStrategy, fetchingDataRows, fetchLimit, fetchOffset, pageSize,
                statementFetchSize, queryTimeout, cacheStrategy, cacheKey, cacheGroup);
    }

    /**
     * Creates a unique key based on procedure and parameters.
     */
    private String cacheKey(Procedure procedure, Map<String, Object> parameters) {
        StringBuilder key = new StringBuilder("proc:").append(procedure.getName());
        if (!parameters.isEmpty()) {
            List<String> keys = new ArrayList<>(parameters.keySet());
            Collections.sort(keys);
            for (String parameterKey : keys) {
                key.append('/').append(parameterKey).append('=').append(parameters.get(parameterKey));
            }
        }

        if (fetchOffset > 0 || fetchLimit > 0) {
            key.append('/');
            if (fetchOffset > 0) {
                key.append('o').append(fetchOffset);
            }
            if (fetchLimit > 0) {
                key.append('l').append(fetchLimit);
            }
        }

        return key.toString();
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this).append("procedureName", procedureName);
        if (resultClass != null) {
            builder.append("resultClass", resultClass.getName());
        } else if (resultEntityName != null) {
            builder.append("resultEntityName", resultEntityName);
        }
        return builder.toString();
    }
}
