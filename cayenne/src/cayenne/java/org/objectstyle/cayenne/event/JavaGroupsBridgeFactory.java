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

import java.lang.reflect.Constructor;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;

/**
 * Factory to create JavaGroupsBridge instances. If JavaGroups library is not installed this
 * factory will return a noop EventBridge as a failover mechanism.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public class JavaGroupsBridgeFactory implements EventBridgeFactory {
    private static Logger logObj = Logger.getLogger(JavaGroupsBridgeFactory.class);

    public static final String MCAST_ADDRESS_DEFAULT = "228.0.0.5";
    public static final String MCAST_PORT_DEFAULT = "22222";

    public static final String MCAST_ADDRESS_PROPERTY =
        "cayenne.JavaGroupsBridge.mcast.address";
    public static final String MCAST_PORT_PROPERTY =
        "cayenne.JavaGroupsBridge.mcast.port";

    /**
     * Defines a property for JavaGroups XML configuration file. Example file can be found at
     * <a href="http://www.filip.net/javagroups/javagroups-protocol.xml">http://www.filip.net/javagroups/javagroups-protocol.xml</a>.
     */
    public static final String JGROUPS_CONFIG_URL_PROPERTY =
        "javagroupsbridge.config.url";

    /**
     * Creates a JavaGroupsBridge instance. Since JavaGroups is not shipped with Cayenne and should be
     * installed separately, a common misconfiguration problem may be the absense of JavaGroups jar file.
     * This factory returns a dummy noop EventBridge, if this is the case. This would
     * allow the application to continue to run, but without remote notifications.
     */
    public EventBridge createEventBridge(EventSubject localSubject, Map properties) {
        try {
            // sniff JavaGroups presence
            Class.forName("org.jgroups.Channel");
            return createJavaGroupsBridge(localSubject, properties);
        } catch (Exception ex) {
            // recover from no JavaGroups
            return createNoopBridge();
        }
    }

    private EventBridge createNoopBridge() {
        logObj.warn(
            "*** Remote events disabled. Reason: JGroups is not available. Download JGroups from http://www.jgroups.org/");
        return new NoopEventBridge();
    }

    private EventBridge createJavaGroupsBridge(
        EventSubject localSubject,
        Map properties) {

        // create JavaGroupsBridge using reflection to avoid triggering
        // ClassNotFound exceptions due to JavaGroups absence.

        try {
            Constructor c =
                Class.forName(
                    "org.objectstyle.cayenne.event.JavaGroupsBridge").getConstructor(
                    new Class[] { EventSubject.class, String.class });

            Object bridge =
                c.newInstance(
                    new Object[] {
                        localSubject,
                        EventBridge.convertToExternalSubject(localSubject)});

            // configure properties
            String multicastAddress = (String) properties.get(MCAST_ADDRESS_PROPERTY);
            String multicastPort = (String) properties.get(MCAST_PORT_PROPERTY);
            String configURL = (String) properties.get(JGROUPS_CONFIG_URL_PROPERTY);

            BeanUtils.setProperty(bridge, "configURL", configURL);
            BeanUtils.setProperty(
                bridge,
                "multicastAddress",
                multicastAddress != null ? multicastAddress : MCAST_ADDRESS_DEFAULT);
            BeanUtils.setProperty(
                bridge,
                "multicastPort",
                multicastPort != null ? multicastPort : MCAST_PORT_DEFAULT);

            return (EventBridge) bridge;
        } catch (Exception ex) {
            throw new CayenneRuntimeException("Error creating JavaGroupsBridge", ex);
        }
    }

    // mockup EventBridge
    class NoopEventBridge extends EventBridge {

        public NoopEventBridge() {
            super(null, null);
        }

        public boolean receivesExternalEvents() {
            return false;
        }

        public boolean receivesLocalEvents() {
            return false;
        }

        protected void startupExternal() {
        }

        protected void shutdownExternal() {
        }

        protected void sendExternalEvent(CayenneEvent localEvent) {
        }

        public void startup(EventManager eventManager, int mode, Object eventsSource) {
            this.eventManager = eventManager;
        }

        public void shutdown() {
        }
    }
}
