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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.event.EventBridge;
import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.remote.ClientMessage;

/**
 * A noop CayenneConnector used for unit testing. Accumulates commands sent via this
 * connector without doing anything with them.
 * 
 */
public class MockClientConnection implements ClientConnection {

    protected Collection commands;
    protected Object fakeResponse;

    public MockClientConnection() {
        this(null);
    }

    public MockClientConnection(Object defaultResponse) {
        this.commands = new ArrayList();
        this.fakeResponse = defaultResponse;
    }

    public void reset() {
        commands.clear();
        fakeResponse = null;
    }

    public EventBridge getServerEventBridge() throws CayenneRuntimeException {
        return null;
    }

    public void setResponse(Object fakeResponse) {
        this.fakeResponse = fakeResponse;
    }

    public Collection getCommands() {
        return commands;
    }

    public Object sendMessage(ClientMessage command) {
        commands.add(command);
        return fakeResponse;
    }
}
