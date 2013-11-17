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

import java.io.Serializable;
import java.util.Collection;

import org.jgroups.Channel;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.MessageListener;
import org.jgroups.blocks.PullPushAdapter;

/**
 * Implementation of EventBridge that passes and receives events via JavaGroups
 * communication software.
 * 
 * @since 1.1
 */
public class JavaGroupsBridge extends EventBridge implements MessageListener {

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

    /**
     * @since 1.2
     */
    public JavaGroupsBridge(Collection<EventSubject> localSubjects, String externalSubject) {
        super(localSubjects, externalSubject);
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
            CayenneEvent event = messageObjectToEvent((Serializable) message.getObject());
            if (event != null) {

                onExternalEvent(event);
            }
        }
        catch (Exception ex) {
            // TODO: Andrus, 2/8/2006 logging... Log4J was removed to make this usable on
            // the client
        }
    }

    @Override
    protected void startupExternal() throws Exception {
        // TODO: need to do more research to figure out the best default transport
        // settings
        // to avoid fragmentation, etc.

        // if config file is set, use it, otherwise use a default
        // set of properties, trying to configure multicast address and port
        if (configURL != null) {
            channel = new JChannel(configURL);
        }
        else {
            String configString = buildConfigString();
            channel = new JChannel(configString);
        }

        // Important - discard messages from self
        channel.setOpt(Channel.LOCAL, Boolean.FALSE);
        channel.connect(externalSubject);

        if (receivesExternalEvents()) {
            adapter = new PullPushAdapter(channel, this);
        }
    }

    /**
     * Creates JavaGroups configuration String, using preconfigured multicast port and
     * address.
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

    @Override
    protected void shutdownExternal() throws Exception {
        try {
            if (adapter != null) {
                adapter.stop();
            }

            channel.close();
        }
        finally {
            adapter = null;
            channel = null;
        }
    }

    @Override
    protected void sendExternalEvent(CayenneEvent localEvent) throws Exception {
        Message message = new Message(null, null, eventToMessageObject(localEvent));
        channel.send(message);
    }

    /**
     * Converts CayenneEvent to a serializable object that will be sent via JMS. Default
     * implementation simply returns the event, but subclasses can customize this
     * behavior.
     */
    protected Serializable eventToMessageObject(CayenneEvent event) throws Exception {
        return event;
    }

    /**
     * Converts a Serializable instance to CayenneEvent. Returns null if the object is not
     * supported. Default implementation simply tries to cast the object to CayenneEvent,
     * but subclasses can customize this behavior.
     */
    protected CayenneEvent messageObjectToEvent(Serializable object) throws Exception {
        return (object instanceof CayenneEvent) ? (CayenneEvent) object : null;
    }
}
