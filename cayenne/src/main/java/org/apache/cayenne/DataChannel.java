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

package org.apache.cayenne;

import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.event.EventSubject;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;

import java.util.Collection;
import java.util.List;

/**
 * DataChannel is an abstraction used by ObjectContexts to obtain mapping metadata and
 * access a persistent store. There is rarely a need to use it directly.
 *
 * @since 1.2
 */
public interface DataChannel {

    /**
     * A synchronization type that results in changes from an ObjectContext to be recorded
     * in the parent DataChannel. If the parent is itself an ObjectContext, changes are
     * NOT propagated any further.
     */
    int FLUSH_NOCASCADE_SYNC = 1;

    /**
     * A synchronization type that results in changes from an ObjectContext to be recorded
     * in the parent DataChannel. If the parent is itself an ObjectContext, it is expected
     * to send its own sync message to its parent DataChannel to cascade synchronization
     * all the way down the stack.
     */
    int FLUSH_CASCADE_SYNC = 2;

    /**
     * A synchronization type that results in cascading rollback of changes through the
     * DataChannel stack.
     */
    int ROLLBACK_CASCADE_SYNC = 3;

    EventSubject GRAPH_CHANGED_SUBJECT = EventSubject.getSubject(
            DataChannel.class,
            "graphChanged");

    EventSubject GRAPH_FLUSHED_SUBJECT = EventSubject.getSubject(
            DataChannel.class,
            "graphFlushed");

    EventSubject GRAPH_ROLLEDBACK_SUBJECT = EventSubject.getSubject(
            DataChannel.class,
            "graphRolledback");

    /**
     * Returns an EventManager associated with this channel. Channel may return null if
     * EventManager is not available for any reason.
     */
    EventManager getEventManager();

    /**
     * Returns an EntityResolver instance that contains runtime mapping information.
     */
    EntityResolver getEntityResolver();

    /**
     * Returns the parent channel of this channel in the DataChannel stack, or null if this is a
     * root channel (such as a {@link org.apache.cayenne.access.DataDomain}).
     *
     * @since 5.0
     */
    DataChannel getParent();

    /**
     * Executes a query, using provided <em>context</em> to register persistent objects if
     * query returns any objects.
     *
     * @param context        an ObjectContext that originated the query, used to register result objects.
     * @param query          a query to execute.
     * @param iteratedResult if true, the result is returned as a {@link ResultIterator} accessible via
     *                       {@link QueryResponse#firstIterator()}, and the caller is responsible for closing
     *                       it. If false, the result is fully read into a list.
     * @return a generic response object that encapsulates result of the execution.
     * @since 5.0
     */
    QueryResponse onQuery(ObjectContext context, Query query, boolean iteratedResult);

    /**
     * Invalidates objects with the given ids in this channel and all its parents, so that they are refetched on the
     * next access. In a context, matching registered objects are turned HOLLOW and their uncommitted changes are
     * discarded. At the root of the channel stack, their snapshots are evicted from the snapshot cache, and peer
     * contexts are notified via a snapshot event. Objects unknown to a given channel are ignored.
     * <p>
     * This is a callback invoked by a child context after it has invalidated its own objects. Application code should
     * call {@link ObjectContext#invalidateObjects(Collection)} instead.
     *
     * @param context   an ObjectContext that originated the invalidation.
     * @param objectIds ids of the objects to invalidate.
     * @since 5.0
     */
    void onInvalidate(ObjectContext context, Collection<ObjectId> objectIds);

    /**
     * Resolves an object by id, returning it registered with the originating context. The object is looked up in the
     * caches down the channel stack before being fetched from the database, so the returned object may reflect a
     * cached state.
     * <p>
     * This is a callback invoked by a child context to resolve a HOLLOW object, or to look up an object it has no
     * registered copy of. Application code should call {@link ObjectContext#objectForPK(ObjectId)} instead.
     *
     * @param context an ObjectContext that originated the request and that the returned object belongs to.
     * @param id      id of the object to resolve.
     * @return the object registered with the originating context, or null if no matching row exists.
     * @since 5.0
     */
    Persistent onIdQuery(ObjectContext context, ObjectId id);

    /**
     * Resolves objects related to a given object via a mapped relationship, returning them registered with the
     * originating context.
     *
     * @param context          an ObjectContext that originated the request and that the returned objects belong to.
     * @param sourceId         id of the object on the source side of the relationship.
     * @param relationshipName name of the relationship to resolve.
     * @return related objects registered with the originating context. An empty list means that a to-one target is
     * null, or a to-many relationship has no matching objects.
     * @since 5.0
     */
    List<? extends Persistent> onResolveRelationship(ObjectContext context, ObjectId sourceId, String relationshipName);

    /**
     * Processes synchronization request from a child ObjectContext, returning a GraphDiff
     * that describes changes to objects made on the receiving end as a result of
     * synchronization.
     *
     * @param context  an ObjectContext that initiated the sync. Can be null.
     * @param changes  diff from the context that initiated the sync.
     * @param syncType One of {@link #FLUSH_NOCASCADE_SYNC}, {@link #FLUSH_CASCADE_SYNC}, {@link #ROLLBACK_CASCADE_SYNC}.
     */
    GraphDiff onSync(ObjectContext context, GraphDiff changes, int syncType);
}
