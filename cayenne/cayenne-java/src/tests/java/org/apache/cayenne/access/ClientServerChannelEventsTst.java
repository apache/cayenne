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

package org.apache.cayenne.access;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.GraphEvent;
import org.apache.cayenne.graph.NodeCreateOperation;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneTestCase;
import org.apache.cayenne.unit.CayenneTestResources;

public class ClientServerChannelEventsTst extends CayenneTestCase {

    protected AccessStack buildAccessStack() {
        return CayenneTestResources
                .getResources()
                .getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testCommitEventSubject() {
        CommitListener listener = new CommitListener();

        ClientServerChannel channel = new ClientServerChannel(getDomain(), true);

        channel.getEventManager().addListener(
                listener,
                "notificationPosted",
                GraphEvent.class,
                DataChannel.GRAPH_FLUSHED_SUBJECT,
                channel);

        GraphDiff diff = new NodeCreateOperation(new ObjectId("MtTable1"));
        channel.onSync(null, diff, DataChannel.FLUSH_CASCADE_SYNC);

        assertTrue(listener.notificationPosted);
    }

    public void testFlushEventSubject() {
        CommitListener listener = new CommitListener();

        ClientServerChannel channel = new ClientServerChannel(getDomain(), true);

        channel.getEventManager().addListener(
                listener,
                "notificationPosted",
                GraphEvent.class,
                DataChannel.GRAPH_CHANGED_SUBJECT,
                channel);

        GraphDiff diff = new NodeCreateOperation(new ObjectId("MtTable1"));
        channel.onSync(null, diff, DataChannel.FLUSH_NOCASCADE_SYNC);
        assertTrue(listener.notificationPosted);
    }

    public void testRollbackEventSubject() {
        CommitListener listener = new CommitListener();

        ClientServerChannel channel = new ClientServerChannel(getDomain(), true);

        GraphDiff diff = new NodeCreateOperation(new ObjectId("MtTable1"));
        channel.onSync(null, diff, DataChannel.FLUSH_NOCASCADE_SYNC);

        channel.getEventManager().addListener(
                listener,
                "notificationPosted",
                GraphEvent.class,
                DataChannel.GRAPH_ROLLEDBACK_SUBJECT,
                channel);

        channel.onSync(null, null, DataChannel.ROLLBACK_CASCADE_SYNC);
        assertTrue(listener.notificationPosted);
    }

    class CommitListener {

        boolean notificationPosted;

        void notificationPosted(GraphEvent e) {
            notificationPosted = true;
        }
    }
}
