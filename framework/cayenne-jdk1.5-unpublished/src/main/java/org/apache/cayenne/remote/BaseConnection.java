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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cayenne.CayenneRuntimeException;

/**
 * A common base class for concrete ClientConnection implementations. Provides message
 * logging functionality via commons-logging.
 * 
 * @since 1.2
 */
public abstract class BaseConnection implements ClientConnection {

    protected Log logger;
    protected long messageId;
    protected long readTimeout = 0L;
    
    /**
     * Default constructor that initializes logging and a single threaded EventManager.
     */
    protected BaseConnection() {
        this.logger = LogFactory.getLog(getClass());
    }

    /**
     * Invokes 'beforeSendMessage' on self, then invokes 'doSendMessage'. Implements basic
     * logging functionality. Do not override this method unless absolutely necessary.
     * Override 'beforeSendMessage' and 'doSendMessage' instead.
     */
    public Object sendMessage(ClientMessage message) throws CayenneRuntimeException {
        if (message == null) {
            throw new NullPointerException("Null message");
        }

        beforeSendMessage(message);

        // log start...
        long t0 = 0;
        String messageLabel = "";

        // using sequential number for message id ... it can be useful for some basic
        // connector stats.
        long messageId = this.messageId++;

        if (logger.isInfoEnabled()) {
            t0 = System.currentTimeMillis();
            messageLabel = message.toString();
            logger.info("--- Message " + messageId + ": " + messageLabel);
        }

        Object response;
        try {
            response = doSendMessage(message);
        }
        catch (CayenneRuntimeException e) {

            // log error
            if (logger.isInfoEnabled()) {
                long time = System.currentTimeMillis() - t0;
                logger.info("*** Message error for "
                        + messageId
                        + ": "
                        + messageLabel
                        + " - took "
                        + time
                        + " ms.");
            }

            throw e;
        }

        // log success...
        if (logger.isInfoEnabled()) {
            long time = System.currentTimeMillis() - t0;
            logger.info("=== Message "
                    + messageId
                    + ": "
                    + messageLabel
                    + " done - took "
                    + time
                    + " ms.");
        }

        return response;
    }

    /**
     * Returns a count of processed messages since the beginning of life of this
     * connector.
     */
    public long getProcessedMessagesCount() {
        return messageId + 1;
    }

    /**
     * The socket timeout on requests in milliseconds. Defaults to infinity.
     * 
     * @since 3.1
     */
    public long getReadTimeout() {
        return readTimeout;
    }
    
    /**
     * Sets the socket timeout.
     * 
     * @param readTimeout The socket timeout on requests in milliseconds.
     * 
     * @since 3.1
     */
    public void setReadTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
    }
    
    /**
     * Called before logging the beginning of message processing.
     */
    protected abstract void beforeSendMessage(ClientMessage message)
            throws CayenneRuntimeException;

    /**
     * The worker method invoked to process message.
     */
    protected abstract Object doSendMessage(ClientMessage message)
            throws CayenneRuntimeException;
}
