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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.remote.BootstrapMessage;
import org.apache.cayenne.remote.ClientMessage;
import org.apache.cayenne.remote.QueryMessage;
import org.apache.cayenne.remote.SyncMessage;

/**
 * A helper class to match message types with DataChannel methods.
 * 
 * @since 1.2
 */
class DispatchHelper {

    static Object dispatch(DataChannel channel, ClientMessage message) {
        // do most common messages first...
        if (message instanceof QueryMessage) {
            return channel.onQuery(null, ((QueryMessage) message).getQuery());
        }
        else if (message instanceof SyncMessage) {
            SyncMessage sync = (SyncMessage) message;
            return channel.onSync(null, sync.getSenderChanges(), sync.getType());
        }
        else if (message instanceof BootstrapMessage) {
            return channel.getEntityResolver().getClientEntityResolver();
        }
        else {
            throw new CayenneRuntimeException(
                    "Message dispatch error. Unsupported message: " + message);
        }
    }
}
