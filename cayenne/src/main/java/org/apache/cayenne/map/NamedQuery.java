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
import org.apache.cayenne.query.MappedQuery;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.resolved.ResolvedQuery;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * A query defined in the DataMap under a name. It is a description of the query, that is assembled into a runtime
 * query by {@link MappedQuery}.
 *
 * @since 6.0
 */
public abstract sealed class NamedQuery implements ProjectNode, XMLSerializable
        permits NamedSelectQuery, NamedSQLQuery, NamedProcedureQuery {

    public static final String OBJ_ENTITY_ROOT = "objEntity";
    public static final String DB_ENTITY_ROOT = "dbEntity";
    public static final String PROCEDURE_ROOT = "procedure";
    public static final String DATA_MAP_ROOT = "dataMap";
    public static final String JAVA_CLASS_ROOT = "javaClass";

    protected String name;
    protected DataMap dataMap;
    protected Object root;
    protected QueryCacheStrategy cacheStrategy;
    protected String cacheGroup;
    protected boolean fetchingDataRows;
    protected int pageSize;
    protected int statementFetchSize;

    protected NamedQuery() {
        this.cacheStrategy = QueryCacheStrategy.getDefaultStrategy();
        this.fetchingDataRows = ResolvedQuery.FETCHING_DATA_ROWS_DEFAULT;
        this.pageSize = ResolvedQuery.PAGE_SIZE_DEFAULT;
        this.statementFetchSize = ResolvedQuery.STATEMENT_FETCH_SIZE_DEFAULT;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DataMap getDataMap() {
        return dataMap;
    }

    public void setDataMap(DataMap dataMap) {
        this.dataMap = dataMap;
    }

    public Object getRoot() {
        return root;
    }

    public void setRoot(Object root) {
        this.root = root;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getStatementFetchSize() {
        return statementFetchSize;
    }

    public void setStatementFetchSize(int statementFetchSize) {
        this.statementFetchSize = statementFetchSize;
    }

    public boolean isFetchingDataRows() {
        return fetchingDataRows;
    }

    public void setFetchingDataRows(boolean fetchingDataRows) {
        this.fetchingDataRows = fetchingDataRows;
    }

    public QueryCacheStrategy getCacheStrategy() {
        return cacheStrategy;
    }

    public void setCacheStrategy(QueryCacheStrategy cacheStrategy) {
        this.cacheStrategy = cacheStrategy != null ? cacheStrategy : QueryCacheStrategy.getDefaultStrategy();
    }

    public String getCacheGroup() {
        return cacheGroup;
    }

    public void setCacheGroup(String cacheGroup) {
        this.cacheGroup = cacheGroup;
    }

    @Override
    public <T> T acceptVisitor(ProjectNodeVisitor<T> visitor) {
        return visitor.visitQuery(this);
    }

    protected void encodeCacheGroup(XMLEncoder encoder) {
        if (cacheGroup != null && !cacheGroup.isEmpty()) {
            encoder.start("cacheGroup").cdata(cacheGroup, true).end();
        }
    }
}
