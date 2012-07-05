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

package org.apache.cayenne.remote;

import java.util.List;
import java.util.ListIterator;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelSyncCallbackAction;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.event.EventBridge;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.event.EventSubject;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.GraphDiffCompressor;
import org.apache.cayenne.graph.GraphEvent;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.util.DeepMergeOperation;
import org.apache.cayenne.util.ToStringBuilder;

/**
 * A {@link org.apache.cayenne.DataChannel} implementation that accesses a remote server
 * via a ClientConnection.
 * 
 * @since 1.2
 */
public class ClientChannel implements DataChannel {

    protected ClientConnection connection;
    protected EventManager eventManager;
    protected EntityResolver entityResolver;
    protected boolean channelEventsEnabled;
    protected GraphDiffCompressor diffCompressor;

    EventBridge remoteChannelListener;

    /**
     * @param remoteEventsOptional if true, failure to start an EventBridge will not
     *            result in an exception.
     * @since 3.0
     */
    public ClientChannel(ClientConnection connection, boolean channelEventsEnabled,
            EventManager eventManager, boolean remoteEventsOptional)
            throws CayenneRuntimeException {

        this.connection = connection;
        this.diffCompressor = new GraphDiffCompressor();
        this.eventManager = eventManager;
        this.channelEventsEnabled = eventManager != null && channelEventsEnabled;

        if (!remoteEventsOptional) {
            setupRemoteChannelListener();
        }
        else {
            try {
                setupRemoteChannelListener();
            }
            catch (CayenneRuntimeException e) {

            }
        }
    }

    /**
     * @since 3.1
     */
    public ClientConnection getConnection() {
        return connection;
    }

    /**
     * @since 3.1
     */
    public boolean isChannelEventsEnabled() {
        return channelEventsEnabled;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public QueryResponse onQuery(ObjectContext context, Query query) {

        QueryResponse response = (QueryResponse) send(
                new QueryMessage(query),
                QueryResponse.class);

        // if needed, register objects in provided context, rewriting the response
        // (assuming all lists are mutable)

        if (context != null) {

            EntityResolver resolver = context.getEntityResolver();
            QueryMetadata info = query.getMetaData(resolver);

            if (!info.isFetchingDataRows()) {

                response.reset();

                while (response.next()) {
                    if (response.isList()) {

                        List objects = response.currentList();

                        if (!objects.isEmpty()) {

                            DeepMergeOperation merger = new DeepMergeOperation(context);
                            List<Object> rsMapping = info.getResultSetMapping();
                            if (rsMapping == null) {
                                convertSingleObjects(resolver, objects, merger);
                            }
                            else {
                                if (rsMapping.size() == 1) {
                                    if (rsMapping.get(0) instanceof EntityResultSegment) {
                                        convertSingleObjects(resolver, objects, merger);
                                    }
                                }
                                else {
                                    processMixedResult(
                                            resolver,
                                            objects,
                                            merger,
                                            rsMapping);

                                }
                            }
                        }
                    }
                }
            }
        }

        return response;
    }

    private void processMixedResult(
            EntityResolver resolver,
            List<Object[]> objects,
            DeepMergeOperation merger,
            List<Object> rsMapping) {

        int width = rsMapping.size();
        for (int i = 0; i < width; i++) {
            if (rsMapping.get(i) instanceof EntityResultSegment) {
                for (Object[] object : objects) {
                    object[i] = convertObject(resolver, merger, (Persistent) object[i]);
                }
            }
        }
    }

    private void convertSingleObjects(
            EntityResolver resolver,
            List objects,
            DeepMergeOperation merger) {

        ListIterator it = objects.listIterator();
        while (it.hasNext()) {
            Object next = it.next();
            it.set(convertObject(resolver, merger, (Persistent) next));
        }
    }

    private Object convertObject(
            EntityResolver resolver,
            DeepMergeOperation merger,
            Persistent object) {

        ObjectId id = object.getObjectId();

        // sanity check
        if (id == null) {
            throw new CayenneRuntimeException("Server returned an object without an id: "
                    + object);
        }

        return merger.merge(object);
    }

    public GraphDiff onSync(
            ObjectContext originatingContext,
            GraphDiff changes,
            int syncType) {

        DataChannelSyncCallbackAction callbackAction = DataChannelSyncCallbackAction
                .getCallbackAction(
                        getEntityResolver().getCallbackRegistry(),
                        originatingContext.getGraphManager(),
                        changes,
                        syncType);
        callbackAction.applyPreCommit();

        changes = diffCompressor.compress(changes);

        GraphDiff replyDiff = (GraphDiff) send(new SyncMessage(
                originatingContext,
                syncType,
                changes), GraphDiff.class);

        if (channelEventsEnabled) {
            EventSubject subject;

            switch (syncType) {
                case DataChannel.ROLLBACK_CASCADE_SYNC:
                    subject = DataChannel.GRAPH_ROLLEDBACK_SUBJECT;
                    break;
                case DataChannel.FLUSH_NOCASCADE_SYNC:
                    subject = DataChannel.GRAPH_CHANGED_SUBJECT;
                    break;
                case DataChannel.FLUSH_CASCADE_SYNC:
                    subject = DataChannel.GRAPH_FLUSHED_SUBJECT;
                    break;
                default:
                    subject = null;
            }

            if (subject != null) {

                // combine message sender changes and message receiver changes into a
                // single event
                boolean sentNoop = changes == null || changes.isNoop();
                boolean receivedNoop = replyDiff == null || replyDiff.isNoop();

                if (!sentNoop || !receivedNoop) {
                    CompoundDiff notification = new CompoundDiff();

                    if (!sentNoop) {
                        notification.add(changes);
                    }

                    if (!receivedNoop) {
                        notification.add(replyDiff);
                    }

                    Object postedBy = (originatingContext != null)
                            ? originatingContext
                            : this;
                    GraphEvent e = new GraphEvent(this, postedBy, notification);
                    eventManager.postEvent(e, subject);
                }
            }
        }

        callbackAction.applyPostCommit();
        return replyDiff;
    }

    /**
     * Returns EntityResolver obtained from the server. On first access, this method sends
     * a message to the server to retrieve the EntityResolver. On subsequent calls locally
     * cached resolver is used.
     */
    public EntityResolver getEntityResolver() {
        if (entityResolver == null) {
            synchronized (this) {
                if (entityResolver == null) {
                    entityResolver = (EntityResolver) send(
                            new BootstrapMessage(),
                            EntityResolver.class);
                }
            }
        }

        return entityResolver;
    }

    /**
     * Starts up an EventBridge to listen for remote updates. Returns true if the listener
     * was setup, false if not. False can be returned if the underlying connection doesn't
     * support events of if there is no EventManager available.
     */
    protected boolean setupRemoteChannelListener() throws CayenneRuntimeException {
        if (eventManager == null) {
            return false;
        }

        EventBridge bridge = connection.getServerEventBridge();
        if (bridge == null) {
            return false;
        }

        try {
            // make sure events are sent on behalf of this channel...and received from all
            bridge.startup(eventManager, EventBridge.RECEIVE_LOCAL_EXTERNAL, null, this);
        }
        catch (Exception e) {
            throw new CayenneRuntimeException("Error starting EventBridge " + bridge, e);
        }

        this.remoteChannelListener = bridge;
        return true;
    }

    /**
     * Sends a message via connector, getting a result as an instance of a specific class.
     * 
     * @throws org.apache.cayenne.CayenneRuntimeException if an underlying connector
     *             exception occurred, or a result is not of expected type.
     */
    protected Object send(ClientMessage message, Class<?> resultClass) {
        Object result = connection.sendMessage(message);

        if (result != null && !resultClass.isInstance(result)) {
            String resultString = new ToStringBuilder(result).toString();
            throw new CayenneRuntimeException("Expected result type: "
                    + resultClass.getName()
                    + ", actual: "
                    + resultString);
        }

        return result;
    }
}
