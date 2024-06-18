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

import java.util.List;
import java.util.Map;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 4.0
 */
public class QueryMetadataProxy implements QueryMetadata {
    protected QueryMetadata mdDelegate;

    protected QueryMetadataProxy(QueryMetadata mdDelegate) {
        this.mdDelegate = mdDelegate;
    }

    public String getCacheGroup() {
        return mdDelegate.getCacheGroup();
    }

    @Override
    public String getCacheKey() {
        return mdDelegate.getCacheKey();
    }

    @Override
    public QueryCacheStrategy getCacheStrategy() {
        return mdDelegate.getCacheStrategy();
    }

    @Override
    public ClassDescriptor getClassDescriptor() {
        return mdDelegate.getClassDescriptor();
    }

    @Override
    public DataMap getDataMap() {
        return mdDelegate.getDataMap();
    }

    @Override
    public DbEntity getDbEntity() {
        return mdDelegate.getDbEntity();
    }

    @Override
    public int getFetchLimit() {
        return mdDelegate.getFetchLimit();
    }

    @Override
    public int getFetchOffset() {
        return mdDelegate.getFetchOffset();
    }

    @Override
    public ObjEntity getObjEntity() {
        return mdDelegate.getObjEntity();
    }

    @Override
    public Query getOriginatingQuery() {
        return mdDelegate.getOriginatingQuery();
    }

    @Override
    public int getPageSize() {
        return mdDelegate.getPageSize();
    }

    @Override
    public PrefetchTreeNode getPrefetchTree() {
        return mdDelegate.getPrefetchTree();
    }

    @Override
    public Map<String, String> getPathSplitAliases() {
        return mdDelegate.getPathSplitAliases();
    }

    @Override
    public Procedure getProcedure() {
        return mdDelegate.getProcedure();
    }

    @Override
    public List<Object> getResultSetMapping() {
        return mdDelegate.getResultSetMapping();
    }

    @Override
    public boolean isSingleResultSetMapping() {
        return mdDelegate.isSingleResultSetMapping();
    }

    @Override
    public boolean isFetchingDataRows() {
        return mdDelegate.isFetchingDataRows();
    }

    @Override
    public boolean isRefreshingObjects() {
        return mdDelegate.isRefreshingObjects();
    }

    @Override
    public int getStatementFetchSize() {
        return mdDelegate.getStatementFetchSize();
    }

    @Override
    public int getQueryTimeout() {
        return mdDelegate.getQueryTimeout();
    }

    @Override
    public boolean isSuppressingDistinct() {
        return mdDelegate.isSuppressingDistinct();
    }
}
