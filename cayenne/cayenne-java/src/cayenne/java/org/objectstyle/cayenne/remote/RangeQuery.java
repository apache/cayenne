/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.remote;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.query.PrefetchTreeNode;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryMetadata;
import org.objectstyle.cayenne.query.QueryRouter;
import org.objectstyle.cayenne.query.SQLAction;
import org.objectstyle.cayenne.query.SQLActionVisitor;

/**
 * An Query wrapper that triggers pagination processing on the server. This query is
 * client-only and can't be executed on the server.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class RangeQuery implements Query {

    private String cacheKey;
    private int fetchStartIndex;
    private int fetchLimit;
    private boolean fetchingDataRows;
    private PrefetchTreeNode prefetchTree;
    
    // exists for hessian serialization.
    private RangeQuery() {
        
    }

    /**
     * Creates a PaginatedQuery that returns a single page from an existing cached
     * server-side result list.
     */
    public RangeQuery(String cacheKey, int fetchStartIndex, int fetchLimit,
            QueryMetadata rootMetadata) {
        this.cacheKey = cacheKey;
        this.fetchStartIndex = fetchStartIndex;
        this.fetchLimit = fetchLimit;
        this.fetchingDataRows = rootMetadata.isFetchingDataRows();
        this.prefetchTree = rootMetadata.getPrefetchTree();
    }

    public QueryMetadata getMetaData(EntityResolver resolver) {
        return new QueryMetadata() {

            public String getCacheKey() {
                return cacheKey;
            }

            public int getFetchStartIndex() {
                return fetchStartIndex;
            }

            public int getFetchLimit() {
                return fetchLimit;
            }

            public boolean isFetchingDataRows() {
                return fetchingDataRows;
            }

            public int getPageSize() {
                return 0;
            }

            public String getCachePolicy() {
                return QueryMetadata.NO_CACHE;
            }

            public PrefetchTreeNode getPrefetchTree() {
                return prefetchTree;
            }

            public DataMap getDataMap() {
                throw new UnsupportedOperationException();
            }

            public DbEntity getDbEntity() {
                throw new UnsupportedOperationException();
            }

            public ObjEntity getObjEntity() {
                throw new UnsupportedOperationException();
            }

            public Procedure getProcedure() {
                throw new UnsupportedOperationException();
            }

            public boolean isRefreshingObjects() {
                throw new UnsupportedOperationException();
            }

            public boolean isResolvingInherited() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        throw new UnsupportedOperationException();
    }

    public String getName() {
        throw new UnsupportedOperationException();
    }

    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated as the corresponding interface method is deprecated.
     */
    public Object getRoot() {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated as the corresponding interface method is deprecated.
     */
    public void setName(String name) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated as the corresponding interface method is deprecated.
     */
    public void setRoot(Object root) {
        throw new UnsupportedOperationException();
    }
}
