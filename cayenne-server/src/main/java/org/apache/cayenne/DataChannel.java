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
    public static final int FLUSH_NOCASCADE_SYNC = 1;

    /**
     * A synchronization type that results in changes from an ObjectContext to be recorded
     * in the parent DataChannel. If the parent is itself an ObjectContext, it is expected
     * to send its own sync message to its parent DataChannel to cascade synchronization
     * all the way down the stack.
     */
    public static final int FLUSH_CASCADE_SYNC = 2;

    /**
     * A synchronization type that results in cascading rollback of changes through the
     * DataChannel stack.
     */
    public static final int ROLLBACK_CASCADE_SYNC = 3;

    public static final EventSubject GRAPH_CHANGED_SUBJECT = EventSubject.getSubject(
            DataChannel.class,
            "graphChanged");

    public static final EventSubject GRAPH_FLUSHED_SUBJECT = EventSubject.getSubject(
            DataChannel.class,
            "graphFlushed");

    public static final EventSubject GRAPH_ROLLEDBACK_SUBJECT = EventSubject.getSubject(
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
     * Executes a query, using provided <em>context</em> to register persistent objects if
     * query returns any objects.
     * 
     * @param originatingContext an ObjectContext that originated the query, used to
     *            register result objects.
     * @return a generic response object that encapsulates result of the execution.
     */
    QueryResponse onQuery(ObjectContext originatingContext, Query query);

    /**
     * Processes synchronization request from a child ObjectContext, returning a GraphDiff
     * that describes changes to objects made on the receiving end as a result of
     * synchronization.
     * 
     * @param originatingContext an ObjectContext that initiated the sync. Can be null.
     * @param changes diff from the context that initiated the sync.
     * @param syncType One of {@link #FLUSH_NOCASCADE_SYNC}, {@link #FLUSH_CASCADE_SYNC},
     *            {@link #ROLLBACK_CASCADE_SYNC}.
     */
    GraphDiff onSync(ObjectContext originatingContext, GraphDiff changes, int syncType);
}
