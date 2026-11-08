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
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.map.compiled.CompiledProcedureQuery;
import org.apache.cayenne.map.compiled.CompiledQuery;
import org.apache.cayenne.map.compiled.CompiledSQLQuery;
import org.apache.cayenne.map.compiled.CompiledSelectQuery;
import org.apache.cayenne.ql.ParsedObjectSelect;
import org.apache.cayenne.query.resolved.ResolvedQuery;

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
public class MappedQuery<T> implements Query<T> {

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
        return this;
    }

    public MappedQuery<T> params(Map<String, ?> parameters) {
        if (this.params == null) {
            this.params = new HashMap<>(parameters);
        } else {
            this.params.putAll(parameters);
        }
        return this;
    }

    public MappedQuery<T> param(String name, Object value) {
        if (this.params == null) {
            this.params = new HashMap<>();
        }

        this.params.put(name, value);
        return this;
    }

    /**
     * Resets query fetch limit - a parameter that defines max number of objects that should be ever be fetched from
     * the database.
     */
    public MappedQuery<T> limit(int fetchLimit) {
        this.fetchLimit = fetchLimit;
        return this;
    }

    /**
     * Resets query fetch offset - a parameter that defines how many objects should be skipped when reading data from
     * the database.
     */
    public MappedQuery<T> offset(int fetchOffset) {
        this.fetchOffset = fetchOffset;
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
        return this;
    }

    /**
     * Sets query timeout for the PreparedStatement generated for this query.
     *
     * @see Statement#setQueryTimeout(int)
     */
    public MappedQuery<T> queryTimeout(int timeout) {
        this.queryTimeout = timeout;
        return this;
    }

    /**
     * Resets query page size. A non-negative page size enables query result pagination that saves memory and
     * processing time for large lists if only parts of the result are ever going to be accessed.
     */
    public MappedQuery<T> pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    /**
     * Forces query cache to be refreshed during the execution of this query.
     */
    public MappedQuery<T> forceNoCache() {
        this.forceNoCache = true;
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
    public ResolvedQuery resolve(CompiledMapping mapping) {
        CompiledQuery namedQuery = mapping.query(queryName);
        if (namedQuery == null) {
            throw new CayenneRuntimeException("Unknown query name: %s", queryName);
        }

        return resolveNamedQuery(namedQuery).resolve(mapping);
    }

    /**
     * Assembles the runtime query of a named query, binding the parameters of this query to it and applying the
     * settings of this query over the ones of the named query.
     */
    Query<?> resolveNamedQuery(CompiledQuery namedQuery) {
        Map<String, ?> parameters = normalizedParameters();
        return switch (namedQuery) {
            case CompiledSelectQuery select -> resolveNamedSelect(select, parameters);
            case CompiledSQLQuery sql -> resolveNamedSQL(sql, parameters);
            case CompiledProcedureQuery procedure -> resolveNamedProcedure(procedure, parameters);
        };
    }

    /**
     * Parses the query String with its "$name" parameters bound to the values from the map. The smallest condition
     * that contains a parameter missing from the map is pruned. The String is parsed anew for every query, as the
     * query is modified here and by the caller.
     */
    private ObjectSelect<?> resolveNamedSelect(CompiledSelectQuery namedQuery, Map<String, ?> parameters) {
        String ql = namedQuery.ql();
        if (ql == null || ql.isBlank()) {
            throw new CayenneRuntimeException("Query '%s' is empty", namedQuery.name());
        }

        ParsedObjectSelect query;
        try {
            query = ParsedObjectSelect.parse(ql, parameters);
        } catch (ExpressionException e) {
            throw new CayenneRuntimeException("Query '%s' can't be parsed: %s", e, namedQuery.name(),
                    e.getUnlabeledMessage());
        }

        // a query rooted in a DbEntity fetches DataRows anyway, and a column query can't fetch them
        if (namedQuery.fetchingDataRows() && !query.isColumnQuery()) {
            query.fetchDataRows();
        }

        // the limit and the offset of the String stay unless set here
        if (fetchLimit != null) {
            query.limit(fetchLimit);
        }
        if (fetchOffset != null) {
            query.offset(fetchOffset);
        }
        if (queryTimeout != null) {
            query.queryTimeout(queryTimeout);
        }

        return query.pageSize(pageSize(namedQuery))
                .statementFetchSize(statementFetchSize(namedQuery))
                .cacheStrategy(cacheStrategy(namedQuery), namedQuery.cacheGroup());
    }

    private SQLQuery<?> resolveNamedSQL(CompiledSQLQuery namedQuery, Map<String, ?> parameters) {
        String sql = namedQuery.sql();
        SQLQuery<?> query = namedQuery.rootEntity() != null
                ? SQLQuery.entityQuery(namedQuery.rootEntity(), sql)
                : SQLQuery.query(sql);
        if (namedQuery.rootMap() != null) {
            query = query.dataMap(namedQuery.rootMap());
        }

        if (namedQuery.fetchingDataRows()) {
            query = query.fetchDataRows();
        }

        for (Map.Entry<String, Integer> entry : namedQuery.prefetches().entrySet()) {
            query.addPrefetch(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, String> entry : namedQuery.adapterSql().entrySet()) {
            query.dialect(entry.getKey(), entry.getValue());
        }

        if (!parameters.isEmpty()) {
            query.params(parameters);
        }
        if (fetchLimit != null) {
            query.limit(fetchLimit);
        }
        if (fetchOffset != null) {
            query.offset(fetchOffset);
        }
        if (queryTimeout != null) {
            query.queryTimeout(queryTimeout);
        }

        return query.pageSize(pageSize(namedQuery))
                .statementFetchSize(statementFetchSize(namedQuery))
                .cacheStrategy(cacheStrategy(namedQuery), namedQuery.cacheGroup())
                .columnNameCaps(namedQuery.columnNameCaps());
    }

    private ProcedureQuery<?> resolveNamedProcedure(CompiledProcedureQuery namedQuery, Map<String, ?> parameters) {
        String procedureName = namedQuery.procedure();
        if (procedureName == null) {
            throw new CayenneRuntimeException("ProcedureQuery '%s' has no procedure", namedQuery.name());
        }

        String resultEntityName = namedQuery.resultEntity();
        ProcedureQuery<?> query = resultEntityName != null
                ? ProcedureQuery.entityQuery(procedureName, resultEntityName)
                : ProcedureQuery.query(procedureName);

        // without a result entity the rows are DataRows regardless of the flag
        if (namedQuery.fetchingDataRows() && resultEntityName != null) {
            query = query.fetchDataRows();
        }

        if (!parameters.isEmpty()) {
            query.params(parameters);
        }
        if (queryTimeout != null) {
            query.queryTimeout(queryTimeout);
        }

        return query.limit(fetchLimit != null ? fetchLimit : namedQuery.fetchLimit())
                .offset(fetchOffset != null ? fetchOffset : namedQuery.fetchOffset())
                .pageSize(pageSize(namedQuery))
                .statementFetchSize(statementFetchSize(namedQuery))
                .cacheStrategy(cacheStrategy(namedQuery), namedQuery.cacheGroup())
                .capsStrategy(namedQuery.columnNameCaps());
    }

    private int pageSize(CompiledQuery namedQuery) {
        return pageSize != null ? pageSize : namedQuery.pageSize();
    }

    private int statementFetchSize(CompiledQuery namedQuery) {
        return statementFetchSize != null ? statementFetchSize : namedQuery.statementFetchSize();
    }

    private QueryCacheStrategy cacheStrategy(CompiledQuery namedQuery) {
        QueryCacheStrategy strategy = namedQuery.cacheStrategy();
        if (!forceNoCache) {
            return strategy;
        }

        return switch (strategy) {
            case LOCAL_CACHE -> QueryCacheStrategy.LOCAL_CACHE_REFRESH;
            case SHARED_CACHE -> QueryCacheStrategy.SHARED_CACHE_REFRESH;
            case null, default -> strategy;
        };
    }
}
