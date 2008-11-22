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

package org.apache.cayenne.remote.service;

import java.io.Serializable;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.remote.RemoteSession;

/**
 * An object that stores server side objects for the client session.
 * 
 * @since 1.2
 */
public class ServerSession implements Serializable {

    protected RemoteSession session;
    protected DataChannel channel;

    public ServerSession(RemoteSession session, DataChannel channel) {
        this.session = session;
        this.channel = channel;
    }

    public DataChannel getChannel() {
        return channel;
    }

    public RemoteSession getSession() {
        return session;
    }
}
