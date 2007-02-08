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

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.GraphEvent;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;

/**
 * A DataChannel adapter that connects client ObjectContext children to a server
 * ObjectContext.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class ClientServerChannel implements DataChannel {

    protected DataContext serverContext;
    protected boolean lifecycleEventsEnabled;
    protected Map paginatedResults;

    public ClientServerChannel(DataDomain domain) {
        this(domain, false);
    }

    public ClientServerChannel(DataDomain domain, boolean lifecycleEventsEnabled) {
        this(domain.createDataContext(), lifecycleEventsEnabled);
    }

    ClientServerChannel(DataContext serverContext, boolean lifecycleEventsEnabled) {
        this.serverContext = serverContext;
        this.lifecycleEventsEnabled = lifecycleEventsEnabled;
    }

    public QueryResponse onQuery(ObjectContext context, Query query) {
        return new ClientServerChannelQueryAction(this, query).execute();
    }

    synchronized void addPaginatedResult(String cacheKey, IncrementalFaultList result) {
        if (paginatedResults == null) {
            paginatedResults = new HashMap();
        }

        paginatedResults.put(cacheKey, result);
    }

    synchronized IncrementalFaultList getPaginatedResult(String cacheKey) {
        return (paginatedResults != null) ? (IncrementalFaultList) paginatedResults
                .get(cacheKey) : null;
    }

    DataContext getServerContext() {
        return serverContext;
    }

    public EntityResolver getEntityResolver() {
        return serverContext.getEntityResolver();
    }

    public EventManager getEventManager() {
        return serverContext != null ? serverContext.getEventManager() : null;
    }

    public boolean isLifecycleEventsEnabled() {
        return lifecycleEventsEnabled;
    }

    public void setLifecycleEventsEnabled(boolean lifecycleEventsEnabled) {
        this.lifecycleEventsEnabled = lifecycleEventsEnabled;
    }

    public GraphDiff onSync(
            ObjectContext originatingContext,
            GraphDiff changes,
            int syncType) {

        // sync client changes
        switch (syncType) {
            case DataChannel.ROLLBACK_CASCADE_SYNC:
                return onRollback(changes);
            case DataChannel.FLUSH_NOCASCADE_SYNC:
                return onFlush(changes);
            case DataChannel.FLUSH_CASCADE_SYNC:
                return onCommit(changes);
            default:
                throw new CayenneRuntimeException("Unrecognized SyncMessage type: "
                        + syncType);
        }
    }

    GraphDiff onRollback(GraphDiff childDiff) {

        if (serverContext.hasChanges()) {
            serverContext.rollbackChanges();

            if (lifecycleEventsEnabled) {
                EventManager eventManager = getEventManager();
                if (eventManager != null) {
                    eventManager.postEvent(
                            new GraphEvent(this, null),
                            DataChannel.GRAPH_ROLLEDBACK_SUBJECT);
                }
            }
        }

        return null;
    }

    /**
     * Applies child diff, without returning anything back.
     */
    GraphDiff onFlush(GraphDiff childDiff) {
        childDiff.apply(new ChildDiffLoader(serverContext));

        if (lifecycleEventsEnabled) {
            EventManager eventManager = getEventManager();

            if (eventManager != null) {
                eventManager.postEvent(
                        new GraphEvent(this, childDiff),
                        DataChannel.GRAPH_CHANGED_SUBJECT);
            }
        }

        return null;
    }

    /**
     * Applies child diff, and then commits.
     */
    GraphDiff onCommit(GraphDiff childDiff) {
        GraphDiff diff = serverContext.onContextFlush(null, childDiff, true);

        GraphDiff returnClientDiff;

        if (diff.isNoop()) {
            returnClientDiff = diff;
        }
        else {
            // create client diff
            ServerToClientDiffConverter clientConverter = new ServerToClientDiffConverter(
                    serverContext.getEntityResolver());
            diff.apply(clientConverter);
            returnClientDiff = clientConverter.getClientDiff();
        }

        if (lifecycleEventsEnabled) {
            EventManager eventManager = getEventManager();

            if (eventManager != null) {
                CompoundDiff notification = new CompoundDiff();
                notification.add(childDiff);
                notification.add(returnClientDiff);

                eventManager.postEvent(
                        new GraphEvent(this, notification),
                        DataChannel.GRAPH_FLUSHED_SUBJECT);
            }
        }

        return returnClientDiff;
    }
}
