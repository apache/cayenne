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

import java.util.List;
import java.util.Map;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.reflect.ClassDescriptor;

public class MockQueryMetadata implements QueryMetadata {

    public List<Object> getResultSetMapping() {
        return null;
    }

    public ObjEntity getObjEntity() {
        return null;
    }

    public DbEntity getDbEntity() {
        return null;
    }

    public Procedure getProcedure() {
        return null;
    }

    public Map<String, String> getPathSplitAliases() {
        return null;
    }

    public DataMap getDataMap() {
        return null;
    }

    public QueryCacheStrategy getCacheStrategy() {
        return null;
    }

    public String getCacheKey() {
        return null;
    }

    public String[] getCacheGroups() {
        return null;
    }

    public ClassDescriptor getClassDescriptor() {
        return null;
    }

    public boolean isFetchingDataRows() {
        return false;
    }

    public boolean isRefreshingObjects() {
        return false;
    }

    public boolean isResolvingInherited() {
        return false;
    }

    public int getPageSize() {
        return 0;
    }

    public int getFetchOffset() {
        return -1;
    }

    public int getFetchLimit() {
        return 0;
    }

    public PrefetchTreeNode getPrefetchTree() {
        return null;
    }

    public Query getOrginatingQuery() {
        return null;
    }

    public int getStatementFetchSize() {
        return 0;
    }
}
