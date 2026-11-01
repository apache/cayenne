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
package org.apache.cayenne.map;

import org.apache.cayenne.project.ProjectNode;
import org.apache.cayenne.project.ProjectNodeVisitor;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.resolved.ResolvedQuery;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

import java.util.Map;

/**
 * A query defined in the DataMap under a name. Builds a runtime {@link Query} on demand.
 *
 * @since 6.0
 */
public abstract class NamedQuery implements ProjectNode, XMLSerializable {

    public static final String OBJ_ENTITY_ROOT = "objEntity";

    public static final String DB_ENTITY_ROOT = "dbEntity";

    public static final String PROCEDURE_ROOT = "procedure";

    public static final String DATA_MAP_ROOT = "dataMap";

    public static final String JAVA_CLASS_ROOT = "javaClass";

    protected String name;
    protected DataMap dataMap;
    protected Object root;

    protected QueryCacheStrategy cacheStrategy = QueryCacheStrategy.getDefaultStrategy();
    protected String cacheGroup;
    protected boolean fetchingDataRows = ResolvedQuery.FETCHING_DATA_ROWS_DEFAULT;
    protected int pageSize = ResolvedQuery.PAGE_SIZE_DEFAULT;
    protected int statementFetchSize = ResolvedQuery.STATEMENT_FETCH_SIZE_DEFAULT;

    /**
     * Returns name of the query.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name of the query.
     */
    public void setName(String name) {
        this.name = name;
    }

    public DataMap getDataMap() {
        return dataMap;
    }

    public void setDataMap(DataMap dataMap) {
        this.dataMap = dataMap;
    }

    /**
     * Returns the root of this query.
     */
    public Object getRoot() {
        return root;
    }

    /**
     * Sets the root of this query.
     */
    public void setRoot(Object root) {
        this.root = root;
    }

    /**
     * Returns the page size of the query. Zero means no pagination.
     */
    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Returns the JDBC statement fetch size of the query. Zero means the driver default.
     */
    public int getStatementFetchSize() {
        return statementFetchSize;
    }

    public void setStatementFetchSize(int statementFetchSize) {
        this.statementFetchSize = statementFetchSize;
    }

    /**
     * Returns whether the query fetches DataRows instead of Persistent objects.
     */
    public boolean isFetchingDataRows() {
        return fetchingDataRows;
    }

    public void setFetchingDataRows(boolean fetchingDataRows) {
        this.fetchingDataRows = fetchingDataRows;
    }

    /**
     * Returns the cache strategy of the query, that is never null.
     */
    public QueryCacheStrategy getCacheStrategy() {
        return cacheStrategy;
    }

    /**
     * Sets the cache strategy of the query. A null value resets it to the default strategy.
     */
    public void setCacheStrategy(QueryCacheStrategy cacheStrategy) {
        this.cacheStrategy = cacheStrategy != null ? cacheStrategy : QueryCacheStrategy.getDefaultStrategy();
    }

    /**
     * Returns the cache group of the query, or null if none is set.
     */
    public String getCacheGroup() {
        return cacheGroup;
    }

    public void setCacheGroup(String cacheGroup) {
        this.cacheGroup = cacheGroup;
    }

    /**
     * Assembles Cayenne query instance of appropriate type from this named query.
     */
    public abstract Query<?> buildQuery();

    /**
     * Assembles Cayenne query instance of appropriate type from this named query, applying a map of named
     * parameters to it.
     */
    public abstract Query<?> buildQuery(Map<String, ?> parameters);

    @Override
    public <T> T acceptVisitor(ProjectNodeVisitor<T> visitor) {
        return visitor.visitQuery(this);
    }

    void encodeCacheGroup(XMLEncoder encoder) {
        if (cacheGroup != null && !cacheGroup.isEmpty()) {
            encoder.start("cacheGroup").cdata(cacheGroup, true).end();
        }
    }
}
