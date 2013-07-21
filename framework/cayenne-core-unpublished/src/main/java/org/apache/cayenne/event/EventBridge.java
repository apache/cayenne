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

package org.apache.cayenne.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashSet;

import org.apache.cayenne.util.Util;

/**
 * An object that passes events between a local EventManager and some other event dispatch
 * mechanism. The most common example is sending local events to remote JVMs and receiving
 * remote events dispatched by those VMs. EventBridge makes possible to connect a network
 * of regular EventManagers in a single "virtual" distributed EventManager.
 * </p>
 * <p>
 * EventBridge encapsulates a transport agreed upon by all paries (such as JMS) and
 * maintains an array of "local" subjects to communicate with local EventManager, and a
 * single "remote" subject - to use for "external" communications that are
 * transport-specific.
 * <p>
 * Subclasses that require special setup to listen for external events should implement
 * <code>startupExternal()</code> method accordingly.
 * </p>
 * <p>
 * This class is an example of <a
 * href="http://en.wikipedia.org/wiki/Bridge_pattern">"bridge" design pattern</a>, hence
 * the name.
 * </p>
 * 
 * @since 1.1
 */
// TODO Andrus, 10/15/2005 - potentially big inefficiency of concrete implementations of
// EventBridgeFactory is that all the expensive resources are managed by the bridge
// itself. Scaling to a big number of bridge instances would require resource pooling to
// be done by the factory singleton.
public abstract class EventBridge implements EventListener {

    public static final int RECEIVE_LOCAL = 1;
    public static final int RECEIVE_EXTERNAL = 2;
    public static final int RECEIVE_LOCAL_EXTERNAL = 3;

    protected String externalSubject;
    protected Collection<EventSubject> localSubjects;
    protected EventManager eventManager;
    protected int mode;

    protected Object externalEventSource;

    // keeps all listeners so that they are not deallocated
    Collection<SubjectListener> listeners;

    /**
     * A utility method that performs consistent translation from an EventSubject to a
     * String that can be used by external transport as subject for distributed
     * communications. Substitutes all chars that can be incorrectly interpreted by
     * whoever (JNDI, ...?).
     */
    public static String convertToExternalSubject(EventSubject localSubject) {
        char[] chars = localSubject.getSubjectName().toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '/' || chars[i] == '.') {
                chars[i] = '_';
            }
        }

        return new String(chars);
    }

    /**
     * Creates an EventBridge with a single local subject.
     */
    public EventBridge(EventSubject localSubject, String externalSubject) {
        this(Collections.singleton(localSubject), externalSubject);
    }

    /**
     * Creates an EventBridge with multiple local subjects and a single external subject.
     * 
     * @since 1.2
     */
    public EventBridge(Collection<EventSubject> localSubjects, String externalSubject) {
        this.localSubjects = new HashSet<EventSubject>(localSubjects);
        this.externalSubject = externalSubject;
    }

    /**
     * Returns a String subject used to post distributed events.
     */
    public String getExternalSubject() {
        return externalSubject;
    }

    /**
     * Returns true if this bridge is active.
     * 
     * @since 1.2
     */
    public boolean isRunning() {
        return eventManager != null;
    }

    /**
     * Returns a Collection of local EventSubjects.
     * 
     * @since 1.2
     */
    public Collection<EventSubject> getLocalSubjects() {
        return localSubjects;
    }

    /**
     * Returns local EventManager used by the bridge. Returned value will be null before
     * the bridge is started and after it is shutdown.
     * 
     * @since 1.2
     */
    public EventManager getEventManager() {
        return eventManager;
    }

    /**
     * Returns an object used as a source of local events posted in response to remote
     * events. If externalEventSource wasn't setup during bridge startup (or if the bridge
     * is not started), returns this object.
     * 
     * @since 1.2
     */
    public Object getExternalEventSource() {
        return externalEventSource != null ? externalEventSource : this;
    }

    /**
     * Returns true if the bridge is configured to receive local events from its internal
     * EventManager.
     */
    public boolean receivesLocalEvents() {
        return mode == RECEIVE_LOCAL_EXTERNAL || mode == RECEIVE_LOCAL;
    }

    /**
     * Returns true if the bridge is configured to receive external events.
     */
    public boolean receivesExternalEvents() {
        return mode == RECEIVE_LOCAL_EXTERNAL || mode == RECEIVE_EXTERNAL;
    }

    /**
     * Starts EventBridge in the specified mode and locally listening to all event sources
     * that post on a preconfigured subject. Remote events reposted locally will have this
     * EventBridge as their source.
     * 
     * @param eventManager EventManager used to send and receive local events.
     * @param mode One of the possible modes of operation - RECEIVE_EXTERNAL,
     *            RECEIVE_LOCAL, RECEIVE_LOCAL_EXTERNAL.
     */
    public void startup(EventManager eventManager, int mode) throws Exception {
        this.startup(eventManager, mode, null);
    }

    /**
     * Starts EventBridge in the specified mode and locally listening to a specified event
     * source. Remote events reposted locally will have this EventBridge as their source.
     * 
     * @param eventManager EventManager used to send and receive local events.
     * @param mode One of the possible modes of operation - RECEIVE_EXTERNAL,
     *            RECEIVE_LOCAL, RECEIVE_LOCAL_EXTERNAL.
     * @param localEventSource If not null, only events originating from localEventSource
     *            object will be processed by this bridge.
     */
    public void startup(EventManager eventManager, int mode, Object localEventSource)
            throws Exception {
        startup(eventManager, mode, localEventSource, null);
    }

    /**
     * Starts EventBridge in the specified mode.
     * 
     * @param eventManager EventManager used to send and receive local events.
     * @param mode One of the possible modes of operation - RECEIVE_EXTERNAL,
     *            RECEIVE_LOCAL, RECEIVE_LOCAL_EXTERNAL.
     * @param localEventSource If not null, only events originating from localEventSource
     *            object will be processed by this bridge.
     * @param remoteEventSource If not null, remoteEventSource object will be used as
     *            standby source of local events posted by this EventBridge in response to
     *            remote events.
     * @since 1.2
     */
    public void startup(
            EventManager eventManager,
            int mode,
            Object localEventSource,
            Object remoteEventSource) throws Exception {

        if (eventManager == null) {
            throw new IllegalArgumentException("'eventManager' can't be null.");
        }

        // uninstall old event manager
        if (this.eventManager != null) {
            shutdown();
        }

        this.externalEventSource = remoteEventSource;
        this.eventManager = eventManager;
        this.mode = mode;

        if (receivesLocalEvents() && !localSubjects.isEmpty()) {

            listeners = new ArrayList<SubjectListener>(localSubjects.size());

            for (EventSubject subject : localSubjects) {
                SubjectListener listener = new SubjectListener(subject);

                listeners.add(listener);
                eventManager.addNonBlockingListener(
                        listener,
                        "onLocalEvent",
                        CayenneEvent.class,
                        subject,
                        localEventSource);
            }
        }

        startupExternal();
    }

    /**
     * Starts an external interface of the EventBridge.
     */
    protected abstract void startupExternal() throws Exception;

    /**
     * Stops listening for events on both local and external interfaces.
     */
    public void shutdown() throws Exception {

        this.externalEventSource = null;

        if (listeners != null && eventManager != null) {

            for (SubjectListener listener : listeners) {
                eventManager.removeListener(listener, listener.subject);
            }

            eventManager = null;
            listeners = null;
        }

        shutdownExternal();
    }

    /**
     * Shuts down the external interface of the EventBridge, cleaning up and releasing any
     * resources used to communicate external events.
     */
    protected abstract void shutdownExternal() throws Exception;

    /**
     * Helper method intended to be called explicitly by subclasses to asynchronously post
     * an event obtained from a remote source. Subclasses do not have to use this method,
     * but they probably should for consistency.
     */
    protected void onExternalEvent(CayenneEvent event) {
        if (eventManager != null) {

            EventSubject localSubject = event.getSubject();

            // check for valid subject
            if (localSubject == null || !localSubjects.contains(localSubject)) {
                return;
            }

            event.setSource(getExternalEventSource());
            event.setPostedBy(this);

            // inject external eveny to the event manager queue.. leave it up to the
            // listeners to figure out correct synchronization.
            eventManager.postEvent(event, localSubject);
        }
        else {
            throw new IllegalStateException(
                    "Can't post events. EventBridge was not started properly. "
                            + "EventManager is null.");
        }
    }

    /**
     * Sends a Cayenne event over the transport supported by this bridge.
     */
    protected abstract void sendExternalEvent(CayenneEvent localEvent) throws Exception;

    final class SubjectListener {

        EventSubject subject;

        SubjectListener(EventSubject subject) {
            this.subject = subject;
        }

        void onLocalEvent(CayenneEvent event) throws Exception {

            // ignore events posted by this Bridge...
            if (event.getSource() != getExternalEventSource()
                    && event.getPostedBy() != EventBridge.this) {

                // make sure external event has the right subject, if not make a clone
                // with the right one...
                if (!subject.equals(event.getSubject())) {
                    CayenneEvent clone = (CayenneEvent) Util.cloneViaSerialization(event);
                    clone.setSubject(subject);
                    clone.setPostedBy(event.getPostedBy());
                    clone.setSource(event.getSource());

                    event = clone;
                }

                sendExternalEvent(event);
            }
        }
    }
}
