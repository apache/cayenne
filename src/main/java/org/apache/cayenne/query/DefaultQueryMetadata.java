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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * A QueryMetadata implementation that returns all the defaults.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class DefaultQueryMetadata implements QueryMetadata {

    static final QueryMetadata defaultMetadata = new DefaultQueryMetadata();

    /**
     * To simplify overriding this implementation checks whether there is a non-null
     * entity or procedure, and uses its DataMap.
     */
    public DataMap getDataMap() {
        if (getObjEntity() != null) {
            return getObjEntity().getDataMap();
        }

        if (getDbEntity() != null) {
            return getDbEntity().getDataMap();
        }

        if (getProcedure() != null) {
            return getProcedure().getDataMap();
        }

        return null;
    }
    
    /**
     * @since 3.0
     */
    public SQLResultSetMapping getResultSetMapping() {
        return null;
    }

    public DbEntity getDbEntity() {
        return null;
    }

    public ObjEntity getObjEntity() {
        return null;
    }
    
    public ClassDescriptor getClassDescriptor() {
        return null;
    }

    public Procedure getProcedure() {
        return null;
    }

    public String getCacheKey() {
        return null;
    }
    
    public String[] getCacheGroups() {
        return null;
    }

    public String getCachePolicy() {
        return QueryMetadata.CACHE_POLICY_DEFAULT;
    }

    public boolean isFetchingDataRows() {
        return QueryMetadata.FETCHING_DATA_ROWS_DEFAULT;
    }

    public boolean isRefreshingObjects() {
        return QueryMetadata.REFRESHING_OBJECTS_DEFAULT;
    }

    public boolean isResolvingInherited() {
        return QueryMetadata.RESOLVING_INHERITED_DEFAULT;
    }

    public int getPageSize() {
        return QueryMetadata.PAGE_SIZE_DEFAULT;
    }
    
    public int getFetchStartIndex() {
        return -1;
    }

    public int getFetchLimit() {
        return QueryMetadata.FETCH_LIMIT_DEFAULT;
    }

    public PrefetchTreeNode getPrefetchTree() {
        return null;
    }
}
