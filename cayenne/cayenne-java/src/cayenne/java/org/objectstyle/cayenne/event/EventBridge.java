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

package org.objectstyle.cayenne.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Iterator;

import org.objectstyle.cayenne.util.IDUtil;
import org.objectstyle.cayenne.util.Util;

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
 * @author Andrus Adamchik
 * @since 1.1
 */
// TODO Andrus, 10/15/2005 - potentially big inefficiency of concrete implementations of
// EventBridgeFactory is that all the expensive resources are managed by the bridge
// itself. Scaling to a big number of bridge instances would require resource pooling to
// be done by the factory singleton.
public abstract class EventBridge implements EventListener {

    /**
     * @deprecated unused since 1.2
     */
    public static final String VM_ID = new String(IDUtil.pseudoUniqueByteSequence16());

    /**
     * @deprecated unused since 1.2
     */
    public static final String VM_ID_PROPERRTY = "VM_ID";

    public static final int RECEIVE_LOCAL = 1;
    public static final int RECEIVE_EXTERNAL = 2;
    public static final int RECEIVE_LOCAL_EXTERNAL = 3;

    protected String externalSubject;
    protected Collection localSubjects;
    protected EventManager eventManager;
    protected int mode;

    protected Object externalEventSource;

    // keeps all listeners so that they are not deallocated
    Collection listeners;

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
    public EventBridge(Collection localSubjects, String externalSubject) {
        this.localSubjects = new HashSet(localSubjects);
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
     * Returns a subject used for events within the local JVM.
     * 
     * @deprecated since 1.2 EventBridge supports multiple local subjects, so use
     *             'getLocalSubjects()' instead. This method returns the first subject
     *             from the subject array for backwards compatibility.
     */
    public EventSubject getLocalSubject() {
        return localSubjects.size() > 0
                ? (EventSubject) localSubjects.iterator().next()
                : null;
    }

    /**
     * Returns a Collection of local EventSubjects.
     * 
     * @since 1.2
     */
    public Collection getLocalSubjects() {
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

            listeners = new ArrayList(localSubjects.size());

            Iterator it = localSubjects.iterator();
            while (it.hasNext()) {

                EventSubject subject = (EventSubject) it.next();
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

            Iterator it = listeners.iterator();
            while (it.hasNext()) {
                SubjectListener listener = (SubjectListener) it.next();
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
     * Invoked by local EventManager when a local event of interest occurred. Internally
     * delegates to "sendExternalEvent" abstract method.
     * 
     * @deprecated Unused since 1.2, as event dispatch is done via internal listeners.
     */
    public void onLocalEvent(CayenneEvent event) throws Exception {
        if (event.getSource() != getExternalEventSource()) {
            sendExternalEvent(event);
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