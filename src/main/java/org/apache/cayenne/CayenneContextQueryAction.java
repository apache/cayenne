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

package org.apache.cayenne;

import java.util.Collection;
import java.util.Iterator;

import org.apache.cayenne.cache.QueryCacheEntryFactory;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.remote.RemoteIncrementalFaultList;
import org.apache.cayenne.util.ListResponse;
import org.apache.cayenne.util.ObjectContextQueryAction;

/**
 * @since 1.2
 * @author Andrus Adamchik
 */
class CayenneContextQueryAction extends ObjectContextQueryAction {

    CayenneContextQueryAction(CayenneContext actingContext, ObjectContext targetContext,
            Query query) {
        super(actingContext, targetContext, query);
    }

    @Override
    protected boolean interceptPaginatedQuery() {
        if (metadata.getPageSize() > 0) {
            response = new ListResponse(new RemoteIncrementalFaultList(
                    actingContext,
                    query));
            return DONE;
        }

        return !DONE;
    }

    @Override
    protected QueryCache getQueryCache() {
        return ((CayenneContext) actingContext).getQueryCache();
    }

    @Override
    protected QueryCacheEntryFactory getCacheObjectFactory() {
        return new QueryCacheEntryFactory() {

            public Object createObject() {
                if (interceptPaginatedQuery() != DONE) {
                    runQuery();
                }
                return response.firstList();
            }
        };
    }

    @Override
    protected boolean interceptRefreshQuery() {
        if (query instanceof RefreshQuery) {
            RefreshQuery refreshQuery = (RefreshQuery) query;

            CayenneContext context = (CayenneContext) actingContext;

            // handle 4 separate scenarios, but do not combine them as it will be
            // unclear how to handle cascading behavior

            // 1. refresh all
            if (refreshQuery.isRefreshAll()) {

                invalidateLocally(context.internalGraphManager(), context
                        .internalGraphManager()
                        .registeredNodes()
                        .iterator());
                context.getQueryCache().clear();

                // cascade
                return !DONE;
            }

            // 2. invalidate object collection
            Collection<?> objects = refreshQuery.getObjects();
            if (objects != null && !objects.isEmpty()) {

                invalidateLocally(context.internalGraphManager(), objects.iterator());

                // cascade
                return !DONE;
            }

            // 3. refresh query - have to do it eagerly to refresh the objects involved
            if (refreshQuery.getQuery() != null) {
                Query cachedQuery = refreshQuery.getQuery();

                String cacheKey = cachedQuery
                        .getMetaData(context.getEntityResolver())
                        .getCacheKey();
                context.getQueryCache().remove(cacheKey);

                this.response = context.performGenericQuery(cachedQuery);

                // do not cascade to avoid running query twice
                return DONE;
            }

            // 4. refresh groups...
            if (refreshQuery.getGroupKeys() != null
                    && refreshQuery.getGroupKeys().length > 0) {

                String[] groups = refreshQuery.getGroupKeys();
                for (String group : groups) {
                    context.getQueryCache().removeGroup(group);
                }

                // cascade group invalidation
                return !DONE;
            }
        }

        return !DONE;
    }

    private void invalidateLocally(CayenneContextGraphManager graphManager, Iterator<?> it) {
        while (it.hasNext()) {
            Persistent object = (Persistent) it.next();

            // we don't care about NEW objects,
            // but we still do care about HOLLOW, since snapshot might still be
            // present
            if (object.getPersistenceState() == PersistenceState.NEW) {
                continue;
            }

            object.setPersistenceState(PersistenceState.HOLLOW);

            // remove cached changes
            graphManager.changeLog.unregisterNode(object.getObjectId());
            graphManager.stateLog.unregisterNode(object.getObjectId());
        }
    }
}
