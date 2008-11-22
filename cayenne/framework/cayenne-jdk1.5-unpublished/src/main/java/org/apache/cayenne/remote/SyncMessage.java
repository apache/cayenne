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

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.graph.GraphDiff;

/**
 * A message used for synchronization of the child with parent. It defines a few types of
 * synchronization: "flush" (when the child sends its changes without a commit), "commit"
 * (cascading flush with ultimate commit to the database), and "rollback" - cascading
 * reverting of all uncommitted changes.
 * 
 * @since 1.2
 */
public class SyncMessage implements ClientMessage {

    protected transient ObjectContext source;
    protected int type;
    protected GraphDiff senderChanges;

    // private constructor for Hessian deserialization
    @SuppressWarnings("unused")
    private SyncMessage() {

    }

    public SyncMessage(ObjectContext source, int syncType, GraphDiff senderChanges) {
        // validate type
        if (syncType != DataChannel.FLUSH_NOCASCADE_SYNC
                && syncType != DataChannel.FLUSH_CASCADE_SYNC
                && syncType != DataChannel.ROLLBACK_CASCADE_SYNC) {
            throw new IllegalArgumentException("'type' is invalid: " + syncType);
        }

        this.source = source;
        this.type = syncType;
        this.senderChanges = senderChanges;
    }

    /**
     * Returns a source of SyncMessage.
     */
    public ObjectContext getSource() {
        return source;
    }

    public int getType() {
        return type;
    }

    public GraphDiff getSenderChanges() {
        return senderChanges;
    }

    /**
     * Returns a description of the type of message.
     * Possibilities are "flush-sync", "flush-cascade-sync", "rollback-cascade-sync" or "unknown-sync".
     */
    @Override
    public String toString() {
        switch (type) {
            case DataChannel.FLUSH_NOCASCADE_SYNC:
                return "flush-sync";
            case DataChannel.FLUSH_CASCADE_SYNC:
                return "flush-cascade-sync";
            case DataChannel.ROLLBACK_CASCADE_SYNC:
                return "rollback-cascade-sync";
            default:
                return "unknown-sync";
        }
    }
}
