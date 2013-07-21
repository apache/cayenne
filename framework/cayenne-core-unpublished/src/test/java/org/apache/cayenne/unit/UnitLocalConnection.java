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

package org.apache.cayenne.unit;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.remote.ClientMessage;
import org.apache.cayenne.remote.service.LocalConnection;

/**
 * A ClientConnection that allows to block/unblock test client/server communications.
 * 
 */
public class UnitLocalConnection extends LocalConnection {

    protected boolean blockingMessages;

    public UnitLocalConnection(DataChannel handler) {
        super(handler);
    }

    public UnitLocalConnection(DataChannel handler, int serializationPolicy) {
        super(handler, serializationPolicy);
    }

    @Override
    public Object sendMessage(ClientMessage message) throws CayenneRuntimeException {
        checkMessageAllowed();
        return super.sendMessage(message);
    }

    public boolean isBlockingMessages() {
        return blockingMessages;
    }

    public void setBlockingMessages(boolean blockingQueries) {
        this.blockingMessages = blockingQueries;
    }

    public void checkMessageAllowed() throws AssertionFailedError {
        Assert.assertFalse("Message is unexpected.", blockingMessages);
    }
}
