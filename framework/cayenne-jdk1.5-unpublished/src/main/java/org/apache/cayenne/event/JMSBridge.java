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

import javax.jms.Message;
import javax.jms.MessageFormatException;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.apache.cayenne.util.IDUtil;

/**
 * Implementation of EventBridge that passes and receives events via JMS (Java Messaging
 * Service). JMSBridge uses "publish/subscribe" model for communication with external
 * agents.
 * 
 * @since 1.1
 */
public class JMSBridge extends EventBridge implements MessageListener {

    static final String VM_ID = new String(IDUtil.pseudoUniqueByteSequence16());
    static final String VM_ID_PROPERRTY = "VM_ID";

    protected String topicConnectionFactoryName;

    protected TopicConnection sendConnection;
    protected TopicSession sendSession;
    protected TopicConnection receivedConnection;
    protected TopicPublisher publisher;
    protected TopicSubscriber subscriber;

    public JMSBridge(EventSubject localSubject, String externalSubject) {
        super(localSubject, externalSubject);
    }

    /**
     * @since 1.2
     */
    public JMSBridge(Collection<EventSubject> localSubjects, String externalSubject) {
        super(localSubjects, externalSubject);
    }

    /**
     * JMS MessageListener implementation. Injects received events to the EventManager
     * local event queue.
     */
    public void onMessage(Message message) {

        try {
            Object vmID = message.getObjectProperty(JMSBridge.VM_ID_PROPERRTY);
            if (JMSBridge.VM_ID.equals(vmID)) {
                return;
            }

            if (!(message instanceof ObjectMessage)) {
                return;
            }

            ObjectMessage objectMessage = (ObjectMessage) message;
            CayenneEvent event = messageObjectToEvent(objectMessage.getObject());
            if (event != null) {
                onExternalEvent(event);
            }

        }
        catch (MessageFormatException mfex) {
            // TODO: Andrus, 2/8/2006 logging... Log4J was removed to make this usable on
            // the client
        }
        catch (Exception ex) {
            // TODO: Andrus, 2/8/2006 logging... Log4J was removed to make this usable on
            // the client
        }
    }

    /**
     * @return Name of javax.jms.TopicConnectionFactory accessible via JNDI.
     */
    public String getTopicConnectionFactoryName() {
        return topicConnectionFactoryName;
    }

    public void setTopicConnectionFactoryName(String name) {
        this.topicConnectionFactoryName = name;
    }

    /**
     * Starts up JMS machinery for "publish/subscribe" model.
     */
    @Override
    protected void startupExternal() throws Exception {
        Context jndiContext = new InitialContext();
        TopicConnectionFactory connectionFactory = (TopicConnectionFactory) jndiContext
                .lookup(topicConnectionFactoryName);

        Topic topic = null;

        try {
            topic = (Topic) jndiContext.lookup(externalSubject);
        }
        catch (NameNotFoundException ex) {
            // can't find topic, try to create it
            topic = topicNotFound(jndiContext, ex);

            if (topic == null) {
                throw ex;
            }
        }

        // config publisher
        if (receivesLocalEvents()) {
            this.sendConnection = connectionFactory.createTopicConnection();
            this.sendSession = sendConnection.createTopicSession(
                    false,
                    Session.AUTO_ACKNOWLEDGE);
            this.publisher = sendSession.createPublisher(topic);
        }

        // config subscriber
        if (receivesExternalEvents()) {
            this.receivedConnection = connectionFactory.createTopicConnection();
            this.subscriber = receivedConnection.createTopicSession(
                    false,
                    Session.AUTO_ACKNOWLEDGE).createSubscriber(topic);
            this.subscriber.setMessageListener(this);
            this.receivedConnection.start();
        }
    }

    /**
     * Attempts to create missing Topic. Since Topic creation is JMS-implementation
     * specific, this task is left to subclasses. Current implementation simply rethrows
     * the exception.
     */
    protected Topic topicNotFound(Context jndiContext, NamingException ex)
            throws Exception {
        throw ex;
    }

    /**
     * Closes all resources used to communicate via JMS.
     */
    @Override
    protected void shutdownExternal() throws Exception {
        Exception lastException = null;

        if (publisher != null) {
            try {
                publisher.close();
            }
            catch (Exception ex) {
                lastException = ex;
            }
        }

        if (subscriber != null) {
            try {
                subscriber.close();
            }
            catch (Exception ex) {
                lastException = ex;
            }
        }

        if (receivedConnection != null) {
            try {
                receivedConnection.close();
            }
            catch (Exception ex) {
                lastException = ex;
            }
        }

        if (sendSession != null) {
            try {
                sendSession.close();
            }
            catch (Exception ex) {
                lastException = ex;
            }
        }

        if (sendConnection != null) {
            try {
                sendConnection.close();
            }
            catch (Exception ex) {
                lastException = ex;
            }
        }

        publisher = null;
        subscriber = null;
        receivedConnection = null;
        sendConnection = null;
        sendSession = null;

        if (lastException != null) {
            throw lastException;
        }
    }

    @Override
    protected void sendExternalEvent(CayenneEvent localEvent) throws Exception {
        ObjectMessage message = sendSession
                .createObjectMessage(eventToMessageObject(localEvent));
        message.setObjectProperty(JMSBridge.VM_ID_PROPERRTY, JMSBridge.VM_ID);
        publisher.publish(message);
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
