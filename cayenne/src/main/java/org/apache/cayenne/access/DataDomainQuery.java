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
package org.apache.cayenne.access;

import java.util.List;
import java.util.Map;

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
 * A query that allows DataContext to retrieve parent DataDomain through a chain of
 * decorator DataChannels.
 * 
 * @since 3.0
 */
class DataDomainQuery implements Query, QueryMetadata {

    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        throw new UnsupportedOperationException("Not an executable query.");
    }

    public QueryMetadata getMetaData(EntityResolver resolver) {
        return this;
    }

    public List<Object> getResultSetMapping() {
        return null;
    }

    /**
     * @since 4.0
     */
    @Override
    public boolean isSingleResultSetMapping() {
        return false;
    }

    /**
     * @since 4.0
     */
    public Query getOriginatingQuery() {
        return null;
    }

    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
    }

    /**
     * @since 4.0
     */
    public String getCacheGroup() {
        return null;
    }

    public String getCacheKey() {
        return null;
    }

    public Map<String, String> getPathSplitAliases() {
        return null;
    }

    public QueryCacheStrategy getCacheStrategy() {
        return null;
    }

    public DataMap getDataMap() {
        return null;
    }

    public DbEntity getDbEntity() {
        return null;
    }

    public int getFetchLimit() {
        return 0;
    }

    public int getFetchOffset() {
        return 0;
    }

    public ObjEntity getObjEntity() {
        return null;
    }

    public ClassDescriptor getClassDescriptor() {
        return null;
    }

    public int getPageSize() {
        return 0;
    }

    public PrefetchTreeNode getPrefetchTree() {
        return null;
    }

    public Procedure getProcedure() {
        return null;
    }

    public boolean isFetchingDataRows() {
        // must return true, otherwise the stack will attempt Persistent conversion
        return true;
    }

    public boolean isRefreshingObjects() {
        return false;
    }

    public boolean isResolvingInherited() {
        return false;
    }

    public int getStatementFetchSize() {
        return 0;
    }

    @Override
    public int getQueryTimeout() {
        return QueryMetadata.QUERY_TIMEOUT_DEFAULT;
    }

    @Override
    public boolean isSuppressingDistinct() {
        return false;
    }
}
