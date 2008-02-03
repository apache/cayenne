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
package org.apache.cayenne;

import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.remote.ClientChannel;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;

public class CayenneContextGraphDiffCompressorTest extends CayenneCase {

    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources.getResources().getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testMultipleSimpleProperties() {
        DiffCounter serverChannel = new DiffCounter(getDomain());
        LocalConnection connection = new LocalConnection(
                serverChannel,
                LocalConnection.HESSIAN_SERIALIZATION);
        ClientChannel channel = new ClientChannel(connection);
        CayenneContext context = new CayenneContext(channel);

        ClientMtTable1 o1 = context.newObject(ClientMtTable1.class);
        o1.setGlobalAttribute1("v1");
        o1.setGlobalAttribute1("v2");

        context.commitChanges();
        assertEquals(1, serverChannel.nodePropertiesChanged);
        assertEquals(1, serverChannel.nodesCreated);
    }

    public void testComplimentaryArcs() {
        DiffCounter serverChannel = new DiffCounter(getDomain());
        LocalConnection connection = new LocalConnection(
                serverChannel,
                LocalConnection.HESSIAN_SERIALIZATION);
        ClientChannel channel = new ClientChannel(connection);
        CayenneContext context = new CayenneContext(channel);

        ClientMtTable1 o1 = context.newObject(ClientMtTable1.class);
        ClientMtTable2 o2 = context.newObject(ClientMtTable2.class);
        o2.setTable1(o1);
        o2.setTable1(null);

        context.commitChanges();
        assertEquals(0, serverChannel.nodePropertiesChanged);
        assertEquals(2, serverChannel.nodesCreated);
        assertEquals(0, serverChannel.arcsCreated);
        assertEquals(0, serverChannel.arcsDeleted);
    }

    public void testDelete() {
        DiffCounter serverChannel = new DiffCounter(getDomain());
        LocalConnection connection = new LocalConnection(
                serverChannel,
                LocalConnection.HESSIAN_SERIALIZATION);
        ClientChannel channel = new ClientChannel(connection);
        CayenneContext context = new CayenneContext(channel);

        ClientMtTable1 o1 = context.newObject(ClientMtTable1.class);
        o1.setGlobalAttribute1("v1");
        context.deleteObject(o1);

        context.commitChanges();
        assertEquals(0, serverChannel.nodePropertiesChanged);
        assertEquals(0, serverChannel.nodesCreated);
        assertEquals(0, serverChannel.nodesRemoved);
    }

    final class DiffCounter extends ClientServerChannel implements GraphChangeHandler {

        int arcsCreated;
        int arcsDeleted;
        int nodesCreated;
        int nodeIdsChanged;
        int nodePropertiesChanged;
        int nodesRemoved;

        public DiffCounter(DataDomain domain) {
            super(domain);
        }

        @Override
        public GraphDiff onSync(
                ObjectContext originatingContext,
                GraphDiff changes,
                int syncType) {

            changes.apply(this);

            return super.onSync(originatingContext, changes, syncType);
        }

        public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
            arcsCreated++;
        }

        public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
            arcsDeleted++;
        }

        public void nodeCreated(Object nodeId) {
            nodesCreated++;
        }

        public void nodeIdChanged(Object nodeId, Object newId) {
            nodeIdsChanged++;
        }

        public void nodePropertyChanged(
                Object nodeId,
                String property,
                Object oldValue,
                Object newValue) {
            nodePropertiesChanged++;
        }

        public void nodeRemoved(Object nodeId) {
            nodesRemoved++;
        }
    }
}
