/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.remote;

import org.slf4j.Logger;
import org.apache.cayenne.CayenneRuntimeException;
import org.slf4j.LoggerFactory;

/**
 * A common base class for concrete ClientConnection implementations. Provides message
 * logging functionality via slf4j logging.
 * 
 * @since 1.2
 */
public abstract class BaseConnection implements ClientConnection {

    protected Logger logger;
    protected long messageId;
    
    /**
     * Default constructor that initializes logging and a single threaded EventManager.
     */
    protected BaseConnection() {
        this.logger = LoggerFactory.getLogger(getClass());
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
