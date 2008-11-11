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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.event.EventBridge;

/**
 * A connection object used to interact with a remote Cayenne server. Connection supports
 * synchronous interaction via {@link #sendMessage(ClientMessage)} and asynchronous
 * listening for server events.
 * 
 * @since 1.2
 */
public interface ClientConnection {

    /**
     * Returns an EventBridge that receives remote server events. Caller would normally
     * register returned bridge with a local EventManager, thus allowing local listeners
     * to receive server events.
     * 
     * @return An EventBridge or null if server events are not supported.
     */
    EventBridge getServerEventBridge() throws CayenneRuntimeException;

    /**
     * Sends a synchronous ClientMessage to the server, returning a reply.
     */
    Object sendMessage(ClientMessage message) throws CayenneRuntimeException;
}
