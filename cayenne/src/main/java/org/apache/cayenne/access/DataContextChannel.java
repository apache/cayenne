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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.graph.ChildDiffLoader;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.util.GenericResponse;
import org.apache.cayenne.util.ShallowMergeOperation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A {@link DataChannel} that connects a nested ObjectContext to its parent {@link DataContext}. All channel
 * operations are delegated to the parent context. Note that the parent context posts channel events on its own behalf,
 * so listeners interested in those events should listen to the context, not to this channel.
 *
 * @since 5.0
 */
public record DataContextChannel(DataContext context) implements DataChannel {

    public DataContextChannel {
        Objects.requireNonNull(context, "Null parent context");
    }

    @Override
    public EventManager getEventManager() {
        return context.getChannel().getEventManager();
    }

    @Override
    public EntityResolver getEntityResolver() {
        return context.getEntityResolver();
    }

    @Override
    public DataChannel getParent() {
        return context.getChannel();
    }

    @Override
    public QueryResponse onQuery(ObjectContext childContext, Query query, boolean iteratedResult) {
        checkChildContext(childContext);

        QueryResponse response = context.onQuery(query, iteratedResult, true);
        QueryMetadata metadata = query.getMetaData(getEntityResolver());

        return metadata.isFetchingDataRows()
                ? response
                : transferObjectsToChildContext(childContext, response, metadata);
    }

    @Override
    public void onInvalidate(ObjectContext childContext, Collection<ObjectId> objectIds) {
        checkChildContext(childContext);

        context.invalidateIds(objectIds);
    }

    @Override
    public Persistent onIdQuery(ObjectContext childContext, ObjectId id) {
        checkChildContext(childContext);

        Persistent object = context.objectForPK(id);
        return object != null ? new ShallowMergeOperation(childContext).merge(object) : null;
    }

    @Override
    public List<Persistent> onRelationshipQuery(ObjectContext childContext, ObjectId sourceId, String relationshipName) {
        checkChildContext(childContext);

        List<? extends Persistent> related = context.resolveRelationship(sourceId, relationshipName, true);
        ShallowMergeOperation merger = new ShallowMergeOperation(childContext);
        List<Persistent> childObjects = new ArrayList<>(related.size());
        for (Persistent object : related) {
            childObjects.add(merger.merge(object));
        }
        return childObjects;
    }

    @Override
    public GraphDiff onSync(ObjectContext childContext, GraphDiff changes, int syncType) {
        checkChildContext(childContext);

        return switch (syncType) {
            case DataChannel.ROLLBACK_CASCADE_SYNC -> {
                context.rollbackChanges();
                yield new CompoundDiff();
            }
            case DataChannel.FLUSH_NOCASCADE_SYNC -> flushChildChanges(childContext, changes, false);
            case DataChannel.FLUSH_CASCADE_SYNC -> flushChildChanges(childContext, changes, true);
            default -> throw new CayenneRuntimeException("Unrecognized SyncMessage type: %d", syncType);
        };
    }

    /**
     * Applies child context changes to the parent context objects, optionally cascading the flush further up the
     * channel chain.
     */
    private GraphDiff flushChildChanges(ObjectContext childContext, GraphDiff changes, boolean cascade) {
        ObjectStore objectStore = context.getObjectStore();

        objectStore.childContextSyncStarted();
        try {
            changes.apply(new ChildDiffLoader(context));
            context.fireDataChannelChanged(childContext, changes);
            return cascade ? context.flushToParent(true) : new CompoundDiff();
        } finally {
            objectStore.childContextSyncStopped();
        }
    }

    private void checkChildContext(ObjectContext childContext) {
        if (childContext == null) {
            throw new IllegalArgumentException("Originating context must be a child of " + context + ", not null");
        }

        if (childContext == context) {
            throw new IllegalArgumentException("Originating context must be a child of " + context
                    + ", not the context itself");
        }
    }

    private QueryResponse transferObjectsToChildContext(
            ObjectContext childContext,
            QueryResponse response,
            QueryMetadata metadata) {

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
                        merger = new ShallowMergeOperation(childContext);
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

        return childResponse;
    }
}
