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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.event.EventBridge;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.event.EventSubject;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.GraphEvent;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.util.DeepMergeOperation;

/**
 * A {@link org.apache.cayenne.DataChannel} implementation that accesses a remote server
 * via a ClientConnection.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class ClientChannel implements DataChannel {

    protected ClientConnection connection;
    protected EventManager eventManager;
    protected EntityResolver entityResolver;
    protected boolean channelEventsEnabled;

    EventBridge remoteChannelListener;

    /**
     * Creates a new channel accessing remote server via provided connection. Channel
     * created using this constructor will post no events of its own and provide its users
     * with a multithreaded EventManager.
     */
    public ClientChannel(ClientConnection connection) {
        this(connection, false);
    }

    public ClientChannel(ClientConnection connection, boolean channelEventsEnabled) {
        this(connection, channelEventsEnabled, new EventManager(2));
    }

    public ClientChannel(ClientConnection connection, boolean channelEventsEnabled,
            EventManager eventManager) throws CayenneRuntimeException {
        this(connection, channelEventsEnabled, eventManager, false);
    }

    /**
     * @param remoteEventsOptional if true, failure to start an EventBridge will not
     *            result in an exception.
     * @since 3.0
     */
    public ClientChannel(ClientConnection connection, boolean channelEventsEnabled,
            EventManager eventManager, boolean remoteEventsOptional)
            throws CayenneRuntimeException {

        this.connection = connection;
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

                            // subclass descriptors will be resolved on the fly... here
                            // find objects base descriptor.
                            ListIterator it = objects.listIterator();
                            while (it.hasNext()) {
                                Persistent object = (Persistent) it.next();
                                ObjectId id = object.getObjectId();

                                // sanity check
                                if (id == null) {
                                    throw new CayenneRuntimeException(
                                            "Server returned an object without an id: "
                                                    + object);
                                }

                                // have to resolve descriptor here for every object, as
                                // often a query will not have any info indicating the
                                // entity type
                                ClassDescriptor descriptor = resolver
                                        .getClassDescriptor(id.getEntityName());

                                it.set(merger.merge(object, descriptor));
                            }
                        }
                    }
                }
            }
        }

        return response;
    }

    public GraphDiff onSync(
            ObjectContext originatingContext,
            GraphDiff changes,
            int syncType) {

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
                            ? (Object) originatingContext
                            : this;
                    GraphEvent e = new GraphEvent(this, postedBy, notification);
                    eventManager.postEvent(e, subject);
                }
            }
        }

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
     *             exception occured, or a result is not of expected type.
     */
    protected Object send(ClientMessage message, Class resultClass) {
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
