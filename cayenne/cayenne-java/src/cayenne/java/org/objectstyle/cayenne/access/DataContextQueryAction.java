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
package org.objectstyle.cayenne.access;

import java.util.List;

import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryMetadata;
import org.objectstyle.cayenne.query.ObjectIdQuery;
import org.objectstyle.cayenne.util.ListResponse;
import org.objectstyle.cayenne.util.ObjectContextQueryAction;

/**
 * A DataContext-specific version of
 * {@link org.objectstyle.cayenne.util.ObjectContextQueryAction}.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
// TODO: Andrus, 2/2/2006 - all these DataContext extensions should become available to
// CayenneContext as well....
class DataContextQueryAction extends ObjectContextQueryAction {

    public DataContextQueryAction(DataContext actingContext, ObjectContext targetContext,
            Query query) {
        super(actingContext, targetContext, query);
    }

    public QueryResponse execute() {
        if (interceptPaginatedQuery() != DONE) {
            if (interceptOIDQuery() != DONE) {
                if (interceptRelationshipQuery() != DONE) {
                    if (interceptLocalCache() != DONE) {
                        runQuery();
                    }
                }
            }
        }

        interceptObjectConversion();
        return response;
    }

    /**
     * Overrides super implementation to property handle data row fetches.
     */
    protected boolean interceptOIDQuery() {
        if (query instanceof ObjectIdQuery) {
            ObjectIdQuery oidQuery = (ObjectIdQuery) query;

            if (!oidQuery.isFetchMandatory()) {
                Object object = actingContext.getGraphManager().getNode(
                        oidQuery.getObjectId());
                if (object != null) {

                    if (oidQuery.isFetchingDataRows()) {
                        object = ((DataContext) actingContext)
                                .currentSnapshot((DataObject) object);
                    }

                    this.response = new ListResponse(object);
                    return DONE;
                }
            }
        }

        return !DONE;
    }

    private boolean interceptPaginatedQuery() {
        if (metadata.getPageSize() > 0) {
            response = new ListResponse(new IncrementalFaultList(
                    (DataContext) actingContext,
                    query));
            return DONE;
        }

        return !DONE;
    }

    /*
     * Wraps execution in local cache checks.
     */
    private boolean interceptLocalCache() {

        String cacheKey = metadata.getCacheKey();
        if (cacheKey == null) {
            return !DONE;
        }

        boolean cache = QueryMetadata.LOCAL_CACHE.equals(metadata.getCachePolicy());
        boolean cacheOrCacheRefresh = cache
                || QueryMetadata.LOCAL_CACHE_REFRESH.equals(metadata.getCachePolicy());

        if (!cacheOrCacheRefresh) {
            return !DONE;
        }

        ObjectStore objectStore = ((DataContext) actingContext).getObjectStore();
        if (cache) {

            List cachedResults = objectStore.getCachedQueryResult(cacheKey);
            if (cachedResults != null) {
                response = new ListResponse(cachedResults);
                return DONE;
            }
        }

        runQuery();
        objectStore.cacheQueryResult(cacheKey, response.firstList());
        return DONE;
    }
}
