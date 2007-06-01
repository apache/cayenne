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

import java.io.Serializable;

import org.apache.log4j.Logger;
import org.jgroups.Channel;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.MessageListener;
import org.jgroups.blocks.PullPushAdapter;

/**
 * Implementation of EventBridge that passes and receives events via JavaGroups 
 * communication software.
 * 
 * @author Andrei Adamchik
 * @since 1.1
 */
public class JavaGroupsBridge extends EventBridge implements MessageListener {
    private static Logger logObj = Logger.getLogger(JavaGroupsBridge.class);

    // TODO: Meaning of "state" in JGroups is not yet clear to me
    protected byte[] state;

    protected Channel channel;
    protected PullPushAdapter adapter;
    protected String multicastAddress;
    protected String multicastPort;
    protected String configURL;

    /**
     * Creates new instance of JavaGroupsBridge.
     */
    public JavaGroupsBridge(EventSubject localSubject, String externalSubject) {
        super(localSubject, externalSubject);
    }

    public String getConfigURL() {
        return configURL;
    }

    public void setConfigURL(String configURL) {
        this.configURL = configURL;
    }

    public String getMulticastAddress() {
        return multicastAddress;
    }

    public void setMulticastAddress(String multicastAddress) {
        this.multicastAddress = multicastAddress;
    }

    public String getMulticastPort() {
        return multicastPort;
    }

    public void setMulticastPort(String multicastPort) {
        this.multicastPort = multicastPort;
    }

    public byte[] getState() {
        return state;
    }

    public void setState(byte[] state) {
        this.state = state;
    }

    /**
     * Implementation of org.javagroups.MessageListener - a callback method to process
     * incoming messages.
     */
    public void receive(Message message) {
        try {
            if (logObj.isDebugEnabled()) {
                logObj.debug("Received Message from: " + message.getSrc());
            }

            CayenneEvent event = messageObjectToEvent((Serializable) message.getObject());
            if (event != null) {
                if (logObj.isDebugEnabled()) {
                    logObj.debug("Received CayenneEvent: " + event.getClass().getName());
                }

                onExternalEvent(event);
            }
        } catch (Exception ex) {
            logObj.info("Exception while processing message: ", ex);
        }

    }

    protected void startupExternal() throws Exception {
        // TODO: need to do more research to figure out the best default transport settings
        // to avoid fragmentation, etc.

        // if config file is set, use it, otherwise use a default
        // set of properties, trying to configure multicast address and port
        if (configURL != null) {
            logObj.debug("creating channel with configuration from " + configURL);
            channel = new JChannel(configURL);
        } else {
            String configString = buildConfigString();
            logObj.debug("creating channel with properties: " + configString);
            channel = new JChannel(configString);
        }

        // Important - discard messages from self
        channel.setOpt(Channel.LOCAL, Boolean.FALSE);
        channel.connect(externalSubject);
        logObj.debug("channel connected.");

        if (receivesExternalEvents()) {
            adapter = new PullPushAdapter(channel, this);
        }
    }

    /**
     * Creates JavaGroups configuration String, using preconfigured
     * multicast port and address. 
     */
    protected String buildConfigString() {
        if (multicastAddress == null) {
            throw new IllegalStateException("'multcastAddress' is not set");
        }

        if (multicastPort == null) {
            throw new IllegalStateException("'multcastPort' is not set");
        }

        return "UDP(mcast_addr="
            + multicastAddress
            + ";mcast_port="
            + multicastPort
            + ";ip_ttl=32):"
            + "PING(timeout=3000;num_initial_members=6):"
            + "FD(timeout=3000):"
            + "VERIFY_SUSPECT(timeout=1500):"
            + "pbcast.NAKACK(gc_lag=10;retransmit_timeout=600,1200,2400,4800):"
            + "pbcast.STABLE(desired_avg_gossip=10000):"
            + "FRAG:"
            + "pbcast.GMS(join_timeout=5000;join_retry_timeout=2000;"
            + "shun=true;print_local_addr=false)";
    }

    protected void shutdownExternal() throws Exception {
        try {
            if (adapter != null) {
                adapter.stop();
            }

            channel.close();
        } finally {
            adapter = null;
            channel = null;
        }
    }

    protected void sendExternalEvent(CayenneEvent localEvent) throws Exception {
        logObj.debug("Sending event remotely: " + localEvent);
        Message message = new Message(null, null, eventToMessageObject(localEvent));
        channel.send(message);
    }

    /**
     * Converts CayenneEvent to a serializable object that will be sent via JMS. 
     * Default implementation simply returns the event, but subclasses can customize
     * this behavior.
     */
    protected Serializable eventToMessageObject(CayenneEvent event) throws Exception {
        return event;
    }

    /**
     * Converts a Serializable instance to CayenneEvent. Returns null if the object
     * is not supported. Default implementation simply tries to cast the object to
     * CayenneEvent, but subclasses can customize this behavior.
     */
    protected CayenneEvent messageObjectToEvent(Serializable object) throws Exception {
        return (object instanceof CayenneEvent) ? (CayenneEvent) object : null;
    }
}
