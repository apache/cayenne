/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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

import java.util.EventListener;

import org.objectstyle.cayenne.util.IDUtil;

/**
 * A bridge between Cayenne EventManager and other possible event sources.
 * Prime example of using EventBridge is for routing events dispatched locally by EventManager
 * to the remote JVMs via some transport mechanism (e.g. JMS). EventBridge maintains two 
 * event subjects. A "local" subject - to communicate with local EventManager, and a "remote"
 * subject - to work with an external events interface.
 * 
 * <p>If a subclass needs to prepare itself to receive incoming events it should
 * properly implement <code>startup(EventManager)</code> method.</p>
 * 
 * <p>This class is an example of the <a href="http://en.wikipedia.org/wiki/Bridge_pattern">"bridge"</a> 
 * design pattern, hence the name.
 * </p>
 * 
 * @author Andrei Adamchik
 * @since 1.1
 */
public abstract class EventBridge implements EventListener {
    public static final String VM_ID = new String(IDUtil.pseudoUniqueByteSequence16());
    public static final String VM_ID_PROPERRTY = "VM_ID";

    public static final int RECEIVE_LOCAL = 1;
    public static final int RECEIVE_EXTERNAL = 2;
    public static final int RECEIVE_LOCAL_EXTERNAL = 3;

    protected String externalSubject;
    protected EventSubject localSubject;
    protected EventManager eventManager;
    protected int mode;

    /**
     * Performs consistent translation from EventSubjects to a String that can be used
     * by external transport as subject for distributed communications. Substitutes all 
     * chars that can be incorrectly interpreted by whoever (JNDI, ...?).
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

    public EventBridge(EventSubject localSubject, String externalSubject) {
        this.localSubject = localSubject;
        this.externalSubject = externalSubject;
    }

    /**
     * Returns a String subject used to post distributed events.
     */
    public String getExternalSubject() {
        return externalSubject;
    }

    /**
     * Returns a subject used for events within the local JVM.
     */
    public EventSubject getLocalSubject() {
        return localSubject;
    }

    public boolean receivesLocalEvents() {
        return mode == RECEIVE_LOCAL_EXTERNAL || mode == RECEIVE_LOCAL;
    }

    public boolean receivesExternalEvents() {
        return mode == RECEIVE_LOCAL_EXTERNAL || mode == RECEIVE_EXTERNAL;
    }

    /**
     * Sets up this EventBridge to receive local events from the instance of
     * EventManager. Internally calls "startupExternal".
     */
    public void startup(EventManager eventManager, int mode) throws Exception {
        this.startup(eventManager, mode, null);
    }

    public void startup(EventManager eventManager, int mode, Object eventsSource)
        throws Exception {
        // uninstall old event manager
        if (this.eventManager != null) {
            // maybe leave external interface open?
            // on the other hand, the approach below is probably cleaner
            // since nothing is known about the previous state
            shutdown();
        }

        if (eventManager == null) {
            throw new NullPointerException("'eventManager' can't be null.");
        }

        this.eventManager = eventManager;
        this.mode = mode;

        if (receivesLocalEvents()) {
            // by default set as a non-blocking listener
            // also, listen only for source events
            eventManager.addNonBlockingListener(
                this,
                "onLocalEvent",
                CayenneEvent.class,
                localSubject,
                eventsSource);
        }

        startupExternal();
    }

    /**
      * Starts the external interface of the EventBridge.
      */
    protected abstract void startupExternal() throws Exception;

    /**
     * Stops receiving events on both local and external interfaces.
     */
    public void shutdown() throws Exception {
        this.eventManager.removeListener(this);
        this.eventManager = null;

        shutdownExternal();
    }

    /**
     * Shuts down the external interface of the EventBridge, cleaning up
     * and releasing any resources used to communicate external events.
     */
    protected abstract void shutdownExternal() throws Exception;

    /**
     * Helper method for sucblasses to asynchronously post an event obtained from a remote
     * source. Subclasses do not have to use this method, but they probably should for
     * consistency.
     */
    public void onExternalEvent(CayenneEvent event) {
        if (eventManager != null) {

            // initialize event sources
            event.setPostedBy(this);
            if (event.getSource() == null) {
                event.setSource(this);
            }

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
     * Invoked by local EventManager when a local event of interest occurred.
     * Internally delegates to "sendExternalEvent" abstract method.
     */
    public void onLocalEvent(CayenneEvent event) throws Exception {
        if (event.getSource() != this) {
            sendExternalEvent(event);
        }
    }

    protected abstract void sendExternalEvent(CayenneEvent localEvent) throws Exception;
}
