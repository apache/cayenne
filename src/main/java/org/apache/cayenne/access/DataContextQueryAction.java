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

package org.apache.cayenne.access;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.util.ListResponse;
import org.apache.cayenne.util.ObjectContextQueryAction;

/**
 * A DataContext-specific version of
 * {@link org.apache.cayenne.util.ObjectContextQueryAction}.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class DataContextQueryAction extends ObjectContextQueryAction {

    public DataContextQueryAction(DataContext actingContext, ObjectContext targetContext,
            Query query) {
        super(actingContext, targetContext, query);
    }

    /**
     * Overrides super implementation to property handle data row fetches.
     */
    @Override
    protected boolean interceptOIDQuery() {
        if (query instanceof ObjectIdQuery) {
            ObjectIdQuery oidQuery = (ObjectIdQuery) query;

            if (!oidQuery.isFetchMandatory()) {
                Object object = actingContext.getGraphManager().getNode(
                        oidQuery.getObjectId());
                if (object != null) {

                    // TODO: andrus, 10/14/2006 - obtaining a row from an object is the
                    // only piece that makes this method different from the super
                    // implementation. This is used in NEW objects sorting on insert. It
                    // would be nice to implement an alternative algorithm that wouldn't
                    // require this hack.
                    if (oidQuery.isFetchingDataRows()) {
                        object = ((DataContext) actingContext)
                                .currentSnapshot((Persistent) object);
                    }
                    // do not return hollow objects
                    else if (((Persistent) object).getPersistenceState() == PersistenceState.HOLLOW) {
                        return !DONE;
                    }

                    this.response = new ListResponse(object);
                    return DONE;
                }
            }
        }

        return !DONE;
    }

    @Override
    protected boolean interceptPaginatedQuery() {
        if (metadata.getPageSize() > 0) {
            response = new ListResponse(new IncrementalFaultList(
                    (DataContext) actingContext,
                    query));
            return DONE;
        }

        return !DONE;
    }

    @Override
    protected QueryCache getQueryCache() {
        return ((DataContext) actingContext).getQueryCache();
    }

    @Override
    protected boolean interceptRefreshQuery() {
        if (query instanceof RefreshQuery) {
            RefreshQuery refreshQuery = (RefreshQuery) query;

            DataContext context = (DataContext) actingContext;

            // handle four separate cases, but do not combine them as it will be
            // unclear how to handle cascading behavior

            // 1. refresh all
            if (refreshQuery.isRefreshAll()) {
                synchronized (context.getObjectStore()) {

                    invalidateLocally(context.getObjectStore(), context
                            .getObjectStore()
                            .getObjectIterator());

                    context.getQueryCache().clear();
                }

                // cascade
                return !DONE;
            }

            // 2. invalidate object collection
            Collection objects = refreshQuery.getObjects();
            if (objects != null && !objects.isEmpty()) {

                synchronized (context.getObjectStore()) {
                    invalidateLocally(context.getObjectStore(), objects.iterator());
                }

                // cascade
                return !DONE;
            }

            // 3. refresh query - have to do it eagerly to refresh the objects involved
            Query cachedQuery = refreshQuery.getQuery();
            if (cachedQuery != null) {

                String cacheKey = cachedQuery
                        .getMetaData(context.getEntityResolver())
                        .getCacheKey();
                context.getQueryCache().remove(cacheKey);

                this.response = context.performGenericQuery(cachedQuery);

                // do not cascade to avoid running query twice
                return DONE;
            }

            // 4. refresh groups...
            String[] groups = refreshQuery.getGroupKeys();
            if (groups != null && groups.length > 0) {

                for (String group : groups) {
                    context.getQueryCache().removeGroup(group);
                }

                // cascade group invalidation
                return !DONE;
            }

            // shouldn't ever happen
            return DONE;
        }

        return !DONE;
    }

    private void invalidateLocally(ObjectStore objectStore, Iterator it) {
        Map<Object, ObjectDiff> diffMap = objectStore.getChangesByObjectId();

        while (it.hasNext()) {
            Persistent object = (Persistent) it.next();

            int state = object.getPersistenceState();

            // we don't care about NEW objects,
            // but we still do care about HOLLOW, since snapshot might still
            // be present
            if (state == PersistenceState.NEW) {
                continue;
            }

            if (state == PersistenceState.MODIFIED || state == PersistenceState.DELETED) {
                // remove cached changes
                diffMap.remove(object.getObjectId());
            }

            object.setPersistenceState(PersistenceState.HOLLOW);
        }
    }
}
