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

package org.apache.cayenne.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.cache.QueryCacheEntryFactory;
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.query.*;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * A helper class that implements
 * {@link org.apache.cayenne.DataChannel#onQuery(ObjectContext, Query)} logic on behalf of
 * an ObjectContext.
 * <p>
 * <i>Intended for internal use only.</i>
 * </p>
 * 
 * @since 1.2
 */
public abstract class ObjectContextQueryAction {

    protected static final boolean DONE = true;

    protected ObjectContext targetContext;
    protected ObjectContext actingContext;
    protected Query query;
    protected QueryMetadata metadata;
    protected boolean queryOriginator;

    protected transient QueryResponse response;

    public ObjectContextQueryAction(ObjectContext actingContext,
            ObjectContext targetContext, Query query) {

        this.actingContext = actingContext;
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

            for (response.reset(); response.next();) {
                if (response.isList()) {
                    List objects = response.currentList();
                    if (objects.isEmpty()) {
                        childResponse.addResultList(objects);
                    } else {

                        // minor optimization, skip Object[] if there are no persistent objects
                        boolean haveObjects = metadata.getResultSetMapping() == null;
                        if(!haveObjects) {
                            for (Object next : metadata.getResultSetMapping()) {
                                if(next instanceof EntityResultSegment) {
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
                            if(object1 instanceof Persistent) {
                                Persistent object = (Persistent) object1;
                                childObjects.add(merger.merge(object));
                            } else if(haveObjects && object1 instanceof Object[]) {
                                // merge objects inside Object[]
                                Object[] parentData = (Object[]) object1;
                                Object[] childData = new Object[parentData.length];
                                System.arraycopy(parentData, 0, childData, 0, parentData.length);
                                for(int i=0; i<childData.length; i++) {
                                    if(childData[i] instanceof Persistent) {
                                        childData[i] = merger.merge((Persistent)childData[i]);
                                    }
                                }
                                childObjects.add(childData);
                            } else {
                                childObjects.add(object1);
                            }
                        }

                        childResponse.addResultList(childObjects);
                    }
                }
                else {
                    childResponse.addBatchUpdateCount(response.currentUpdateCount());
                }
            }

            response = childResponse;
        }

    }

    protected boolean interceptInternalQuery() {
        return !DONE;
    }

    protected boolean interceptOIDQuery() {
        if (query instanceof ObjectIdQuery) {
            ObjectIdQuery oidQuery = (ObjectIdQuery) query;

            if (!oidQuery.isFetchMandatory() && !oidQuery.isFetchingDataRows()) {
                Object object = polymorphicObjectFromCache(
                        oidQuery.getObjectId());
                if (object != null) {

                    // do not return hollow objects
                    if (((Persistent) object).getPersistenceState() == PersistenceState.HOLLOW) {
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

        if (query instanceof RelationshipQuery) {
            RelationshipQuery relationshipQuery = (RelationshipQuery) query;
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

                            List result;

                            // null to-one
                            if (related == null) {
                                result = new ArrayList(1);
                            }
                            // to-many List
                            else if (related instanceof List) {
                                result = (List) related;
                            }
                            // to-many Set
                            else if (related instanceof Set) {
                                result = new ArrayList((Set) related);
                            }
                            // to-many Map
                            else if (related instanceof Map) {
                                result = new ArrayList(((Map) related).values());
                            }
                            // non-null to-one
                            else {
                                result = new ArrayList(1);
                                result.add(related);
                            }

                            this.response = new ListResponse(result);
                            return DONE;

                        }

                        /**
                         * Workaround for CAY-1183. If a Relationship query is being sent
                         * from child context, we assure that local object is not NEW and
                         * relationship - unresolved (this way exception will occur). This
                         * helps when faulting objects that were committed to parent
                         * context (this), but not to database. Checking type of context's
                         * channel is the only way to ensure that we are on the top level
                         * of context hierarchy (there might be more than one-level-deep
                         * nested contexts).
                         */
                        if (((Persistent) object).getPersistenceState() == PersistenceState.NEW
                                && !(actingContext.getChannel() instanceof ObjectContext)) {
                            this.response = new ListResponse();
                            return DONE;
                        }
                    }
                }
            }
        }

        return !DONE;
    }

    /**
     * @since 3.0
     */
    protected abstract boolean interceptPaginatedQuery();

    /**
     * @since 3.0
     */
    protected abstract boolean interceptRefreshQuery();

    /**
     * @since 3.0
     */
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

    /**
     * @since 3.0
     */
    protected QueryCache getQueryCache() {
        return ((DataContext) actingContext).getQueryCache();
    }

    /**
     * @since 3.0
     */
    protected QueryCacheEntryFactory getCacheObjectFactory() {
        return new QueryCacheEntryFactory() {

            public List createObject() {
                executePostCache();
                List result = response.firstList();
                // make an immutable list to make sure callers don't mess it up
                return result != null ? Collections.unmodifiableList(result) : null;
            }
        };
    }

    /**
     * Fetches data from the channel.
     */
    protected void runQuery() {
        this.response = actingContext.getChannel().onQuery(actingContext, query);
    }
}
