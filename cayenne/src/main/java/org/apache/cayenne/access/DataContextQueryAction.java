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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.cache.QueryCacheEntryFactory;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.IteratedQueryDecorator;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.query.RelationshipQuery;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.util.GenericResponse;
import org.apache.cayenne.util.ListResponse;
import org.apache.cayenne.util.ShallowMergeOperation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

class DataContextQueryAction {

    private static final boolean DONE = true;

    private final ObjectContext actingContext;
    private final ObjectContext targetContext;
    private final DataContext actingDataContext;
    private final Query query;
    private final QueryMetadata metadata;
    private final boolean queryOriginator;

    private transient QueryResponse response;

    public DataContextQueryAction(DataContext actingContext, ObjectContext targetContext, Query query) {

        this.actingContext = actingContext;
        this.actingDataContext = actingContext;
        this.query = query;

        // this means that a caller must pass self as both acting context and target
        // context to indicate that a query originated here... null (ROP) or differing
        // context indicates that the query was originated elsewhere, which has
        // consequences in LOCAL_CACHE handling
        this.queryOriginator = targetContext != null && targetContext == actingContext;

        // no special target context and same target context as acting context mean the
        // same thing. "normalize" the internal state to avoid confusion
        this.targetContext = targetContext != actingContext ? targetContext : null;
        this.metadata = query.getMetaData(actingContext.getEntityResolver());
    }

    /**
     * Worker method that performs internal query.
     */
    public QueryResponse execute() {
        if (interceptIteratedQuery() != DONE) {
            if (interceptOIDQuery() != DONE) {
                if (interceptRelationshipQuery() != DONE) {
                    if (interceptRefreshQuery() != DONE) {
                        if (interceptLocalCache() != DONE) {
                            executePostCache();
                        }
                    }
                }
            }
        }

        interceptObjectConversion();
        return response;
    }

    private boolean interceptIteratedQuery() {
        if (query instanceof IteratedQueryDecorator) {
            runQuery();
            return DONE;
        }
        return !DONE;
    }

    private void executePostCache() {
        if (interceptInternalQuery() != DONE) {
            if (interceptPaginatedQuery() != DONE) {
                runQuery();
            }
        }
    }

    /**
     * Transfers fetched objects into the target context if it is different from "acting"
     * context. Note that when this method is invoked, result objects are already
     * registered with acting context by the parent channel.
     */
    protected void interceptObjectConversion() {

        if (targetContext != null && !metadata.isFetchingDataRows()) {

            // rewrite response to contain objects from the query context

            GenericResponse childResponse = new GenericResponse();
            ShallowMergeOperation merger = null;

            for (response.reset(); response.next(); ) {
                if (response.isList()) {
                    List<?> objects = response.currentList();
                    if (objects.isEmpty()) {
                        childResponse.addResultList(objects);
                    } else {

                        // minor optimization, skip Object[] if there are no persistent objects
                        boolean haveObjects = metadata.getResultSetMapping() == null;
                        if (!haveObjects) {
                            for (Object next : metadata.getResultSetMapping()) {
                                if (next instanceof EntityResultSegment) {
                                    haveObjects = true;
                                    break;
                                }
                            }
                        }

                        if (merger == null) {
                            merger = new ShallowMergeOperation(targetContext);
                        }

                        // TODO: Andrus 1/31/2006 - IncrementalFaultList is not properly
                        // transferred between contexts....

                        List<Object> childObjects = new ArrayList<>(objects.size());
                        for (Object object1 : objects) {
                            if (object1 instanceof Persistent object) {
                                childObjects.add(merger.merge(object));
                            } else if (haveObjects && object1 instanceof Object[] parentData) {
                                // merge objects inside Object[]
                                Object[] childData = new Object[parentData.length];
                                System.arraycopy(parentData, 0, childData, 0, parentData.length);
                                for (int i = 0; i < childData.length; i++) {
                                    if (childData[i] instanceof Persistent) {
                                        childData[i] = merger.merge((Persistent) childData[i]);
                                    }
                                }
                                childObjects.add(childData);
                            } else {
                                childObjects.add(object1);
                            }
                        }

                        childResponse.addResultList(childObjects);
                    }
                } else {
                    childResponse.addBatchUpdateCount(response.currentUpdateCount());
                }
            }

            response = childResponse;
        }

    }

    protected boolean interceptInternalQuery() {
        if (query instanceof ObjectsFromDataRowsQuery objectsFromDataRowsQuery) {
            response = new ListResponse(actingDataContext.objectsFromDataRows(
                    objectsFromDataRowsQuery.getDescriptor(),
                    objectsFromDataRowsQuery.getDataRows()));
            return DONE;
        }
        return !DONE;
    }

    /**
     * Handles {@link ObjectIdQuery}, properly handling data row fetches.
     */
    protected boolean interceptOIDQuery() {
        if (query instanceof ObjectIdQuery oidQuery) {

            if (!oidQuery.isFetchMandatory()) {
                Object object = polymorphicObjectFromCache(oidQuery.getObjectId());
                if (object != null) {

                    // TODO: andrus, 10/14/2006 - obtaining a row from an object is the
                    // only piece that makes this method different from a plain cache
                    // lookup. This is used in NEW objects sorting on insert. It would be
                    // nice to implement an alternative algorithm that wouldn't require
                    // this hack.
                    if (oidQuery.isFetchingDataRows()) {
                        object = actingDataContext.currentSnapshot((Persistent) object);
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

    // TODO: bunch of copy/paset from DataDomainQueryAction
    protected Object polymorphicObjectFromCache(ObjectId superOid) {
        Object object = actingContext.getGraphManager().getNode(superOid);
        if (object != null) {
            return object;
        }

        EntityInheritanceTree inheritanceTree = actingContext.getEntityResolver().getInheritanceTree(superOid.getEntityName());
        if (!inheritanceTree.getChildren().isEmpty()) {
            object = polymorphicObjectFromCache(inheritanceTree, superOid);
        }

        return object;
    }

    private Object polymorphicObjectFromCache(EntityInheritanceTree superNode, ObjectId superOid) {

        for (EntityInheritanceTree child : superNode.getChildren()) {
            ObjectId id = ObjectId.of(child.getEntity().getName(), superOid);
            Object object = actingContext.getGraphManager().getNode(id);
            if (object != null) {
                return object;
            }

            object = polymorphicObjectFromCache(child, superOid);
            if (object != null) {
                return object;
            }
        }

        return null;
    }

    protected boolean interceptRelationshipQuery() {

        if (query instanceof RelationshipQuery relationshipQuery) {
            if (!relationshipQuery.isRefreshing()) {

                // don't intercept to-many relationships if fetch is done to the same
                // context as the root context of this action - this will result in an
                // infinite loop.

                if (targetContext == null
                        && relationshipQuery.getRelationship(
                        actingContext.getEntityResolver()).isToMany()) {
                    return !DONE;
                }

                ObjectId id = relationshipQuery.getObjectId();
                Object object = actingContext.getGraphManager().getNode(id);

                if (object != null) {

                    ClassDescriptor descriptor = actingContext
                            .getEntityResolver()
                            .getClassDescriptor(id.getEntityName());

                    if (!descriptor.isFault(object)) {

                        ArcProperty property = (ArcProperty) descriptor
                                .getProperty(relationshipQuery.getRelationshipName());

                        if (!property.isFault(object)) {

                            Object related = property.readPropertyDirectly(object);

                            // null to-one
                            List<?> result = switch (related) {
                                case null -> new ArrayList<>(1);

                                // to-many List
                                case List list -> list;

                                // to-many Set
                                case Set set -> new ArrayList<>(set);

                                // to-many Map
                                case Map map -> new ArrayList<>(map.values());

                                // non-null to-one
                                // TODO: any risks of returning an immutable list here?
                                default -> Collections.singletonList(related);
                            };

                            this.response = new ListResponse(result);
                            return DONE;

                        }

                        // Workaround for CAY-1183. If a Relationship query is being sent
                        // from child context, we assure that local object is not NEW and
                        // relationship - unresolved (this way exception will occur). This
                        // helps when faulting objects that were committed to parent
                        // context (this), but not to database. Checking type of context's
                        // channel is the only way to ensure that we are on the top level
                        // of context hierarchy (there might be more than one-level-deep
                        // nested contexts).
                        if (((Persistent) object).getPersistenceState() == PersistenceState.NEW
                                && !(actingContext.getParent() instanceof ObjectContext)) {
                            this.response = new ListResponse();
                            return DONE;
                        }
                    }
                }
            }
        }

        return !DONE;
    }

    protected boolean interceptPaginatedQuery() {
        if (metadata.getPageSize() > 0) {
            // this will select raw ids
            runQuery();

            List<?> rawIds = response.firstList();
            int maxIdQualifierSize = actingDataContext.getParentDataDomain().getMaxIdQualifierSize();
            IncrementalFaultList<?> paginatedList = createIncrementalFaultList(rawIds, maxIdQualifierSize);

            // replace result with a paginated list that will deal with id-to-object resolution
            response = new ListResponse(paginatedList);
            return DONE;
        }

        return !DONE;
    }

    private IncrementalFaultList<?> createIncrementalFaultList(List<?> rawIds, int maxIdQualifierSize) {
        // just a sanity check
        Objects.requireNonNull(rawIds, "Trying to execute paginated query that is not a select query");
        if (isMixedResultsForPaginatedQuery()) {
            return new MixedResultIncrementalFaultList<>(actingDataContext, query, maxIdQualifierSize, rawIds);
        } else {
            DbEntity dbEntity = metadata.getDbEntity();
            if (dbEntity != null && dbEntity.getPrimaryKeys().size() == 1) {
                return new SimpleIdIncrementalFaultList<>(actingDataContext, query, maxIdQualifierSize, rawIds);
            } else {
                return new IncrementalFaultList<>(actingDataContext, query, maxIdQualifierSize, rawIds);
            }
        }
    }

    private boolean isMixedResultsForPaginatedQuery() {
        boolean mixedResults = false;
        List<Object> rsMapping = metadata.getResultSetMapping();
        if (rsMapping != null) {
            if (rsMapping.size() > 1) {
                mixedResults = true;
            } else if (rsMapping.size() == 1) {
                mixedResults = !(rsMapping.getFirst() instanceof EntityResultSegment)
                        || !metadata.isSingleResultSetMapping();
            }
        }
        return mixedResults;
    }

    protected boolean interceptRefreshQuery() {
        if (query instanceof RefreshQuery refreshQuery) {

            // handle four separate cases, but do not combine them as it will be
            // unclear how to handle cascading behavior

            // 1. refresh all
            if (refreshQuery.isRefreshAll()) {
                synchronized (actingDataContext.getObjectStore()) {

                    invalidateLocally(actingDataContext.getObjectStore(), actingDataContext
                            .getObjectStore()
                            .getObjectIterator());

                    actingDataContext.getQueryCache().clear();
                }

                // cascade
                return !DONE;
            }

            // 2. invalidate object collection
            Collection<?> objects = refreshQuery.getObjects();
            if (objects != null && !objects.isEmpty()) {

                synchronized (actingDataContext.getObjectStore()) {
                    invalidateLocally(actingDataContext.getObjectStore(), objects.iterator());
                }

                // cascade
                return !DONE;
            }

            // 3. refresh query - have to do it eagerly to refresh the objects involved
            Query cachedQuery = refreshQuery.getQuery();
            if (cachedQuery != null) {

                String cacheKey = cachedQuery
                        .getMetaData(actingDataContext.getEntityResolver())
                        .getCacheKey();
                actingDataContext.getQueryCache().remove(cacheKey);

                this.response = actingDataContext.performGenericQuery(cachedQuery);

                // do not cascade to avoid running query twice
                return DONE;
            }

            // 4. refresh groups...
            String[] groups = refreshQuery.getGroupKeys();
            if (groups != null && groups.length > 0) {

                for (String group : groups) {
                    actingDataContext.getQueryCache().removeGroup(group);
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

    protected boolean interceptLocalCache() {

        if (metadata.getCacheKey() == null) {
            return !DONE;
        }

        // ignore local cache unless this context originated the query...
        if (!queryOriginator) {
            return !DONE;
        }

        boolean cache = QueryCacheStrategy.LOCAL_CACHE == metadata.getCacheStrategy();
        boolean cacheOrCacheRefresh = cache
                || QueryCacheStrategy.LOCAL_CACHE_REFRESH == metadata.getCacheStrategy();

        if (!cacheOrCacheRefresh) {
            return !DONE;
        }

        QueryCache queryCache = getQueryCache();
        QueryCacheEntryFactory factory = getCacheObjectFactory();

        if (cache) {
            boolean wasResponseNull = (response == null);
            List cachedResults = queryCache.get(metadata, factory);

            // response may already be initialized by the factory above ... it is null if
            // there was a preexisting cache entry
            if (response == null || wasResponseNull) {
                response = new ListResponse(cachedResults);
            }
        } else {
            // on cache-refresh request, fetch without blocking and fill the cache
            queryCache.put(metadata, factory.createObject());
        }

        return DONE;
    }

    protected QueryCache getQueryCache() {
        return actingDataContext.getQueryCache();
    }

    protected QueryCacheEntryFactory getCacheObjectFactory() {
        return () -> {
            executePostCache();
            List result = response.firstList();
            // make an immutable list to make sure callers don't mess it up
            return result != null ? Collections.unmodifiableList(result) : null;
        };
    }

    /**
     * Fetches data from the channel.
     */
    protected void runQuery() {
        this.response = actingContext.getParent().onQuery(actingContext, query);
    }
}
