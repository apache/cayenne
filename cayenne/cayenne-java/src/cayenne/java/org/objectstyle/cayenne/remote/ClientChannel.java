/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.remote;

import java.util.List;
import java.util.ListIterator;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataChannel;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.event.EventBridge;
import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.event.EventSubject;
import org.objectstyle.cayenne.graph.CompoundDiff;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.graph.GraphEvent;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryMetadata;
import org.objectstyle.cayenne.util.DeepMergeOperation;

/**
 * A {@link org.objectstyle.cayenne.DataChannel} implementation that accesses a remote
 * server via a ClientConnection.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class ClientChannel implements DataChannel {

    protected ClientConnection connection;
    protected EventManager eventManager;
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

        this.connection = connection;
        this.eventManager = eventManager;
        this.channelEventsEnabled = eventManager != null && channelEventsEnabled;

        setupRemoteChannelListener();
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

            // **** notice using EntityResolver from the context, as this channel does not
            // cache it...
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

    public EntityResolver getEntityResolver() {
        return (EntityResolver) send(new BootstrapMessage(), EntityResolver.class);
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
     * @throws org.objectstyle.cayenne.client.CayenneClientException if an underlying
     *             connector exception occured, or a result is not of expected type.
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
