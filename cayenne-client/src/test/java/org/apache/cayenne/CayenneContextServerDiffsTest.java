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
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.reflect.LifecycleCallbackRegistry;
import org.apache.cayenne.remote.ClientChannelServerDiffsListener1;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.testdo.mt.MtTable1;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class CayenneContextServerDiffsTest extends ClientCase {

    @Inject
    private ClientServerChannel clientServerChannel;

    @Inject
    private CayenneContext context;

    public void testReturnDiffInPrePersist() {

        LifecycleCallbackRegistry callbackRegistry = clientServerChannel
                .getEntityResolver()
                .getCallbackRegistry();

        try {

            callbackRegistry.addListener(
                    LifecycleEvent.POST_ADD,
                    MtTable1.class,
                    new ClientChannelServerDiffsListener1(),
                    "prePersist");

            ClientMtTable1 o = context.newObject(ClientMtTable1.class);
            o.setServerAttribute1("YY");
            context.commitChanges();

            assertFalse(o.getObjectId().isTemporary());
            assertEquals(PersistenceState.COMMITTED, o.getPersistenceState());
            assertEquals("XXX", o.getGlobalAttribute1());
        }
        finally {
            callbackRegistry.clear();
        }
    }

    public void testReturnDiffInPreUpdate() {
        LifecycleCallbackRegistry callbackRegistry = clientServerChannel
                .getEntityResolver()
                .getCallbackRegistry();

        try {

            callbackRegistry.addListener(
                    LifecycleEvent.PRE_UPDATE,
                    MtTable1.class,
                    new ClientChannelServerDiffsListener1(),
                    "preUpdate");

            ClientMtTable1 o = context.newObject(ClientMtTable1.class);
            o.setServerAttribute1("YY");
            context.commitChanges();

            assertNull(o.getGlobalAttribute1());

            o.setServerAttribute1("XX");
            context.commitChanges();

            assertFalse(o.getObjectId().isTemporary());
            assertEquals(PersistenceState.COMMITTED, o.getPersistenceState());
            assertEquals("111", o.getGlobalAttribute1());
        }
        finally {
            callbackRegistry.clear();
        }
    }

    public void testReturnDiffClientArcChanges() {

        LifecycleCallbackRegistry callbackRegistry = clientServerChannel
                .getEntityResolver()
                .getCallbackRegistry();

        try {
            callbackRegistry.addListener(
                    LifecycleEvent.POST_ADD,
                    MtTable1.class,
                    new ClientChannelServerDiffsListener1(),
                    "prePersist");

            ClientMtTable1 o = context.newObject(ClientMtTable1.class);
            ClientMtTable2 o1 = context.newObject(ClientMtTable2.class);
            o.addToTable2Array(o1);
            context.commitChanges();

            assertFalse(o.getObjectId().isTemporary());
            assertEquals(PersistenceState.COMMITTED, o.getPersistenceState());
            assertEquals("XXX", o.getGlobalAttribute1());
        }
        finally {
            callbackRegistry.clear();
        }
    }

    public void testReturnDiffServerArcChanges() {

        LifecycleCallbackRegistry callbackRegistry = clientServerChannel
                .getEntityResolver()
                .getCallbackRegistry();

        try {
            callbackRegistry.addListener(
                    LifecycleEvent.POST_ADD,
                    MtTable1.class,
                    new ClientChannelServerDiffsListener1(),
                    "prePersistAddRelationship");

            ClientMtTable1 o = context.newObject(ClientMtTable1.class);
            ClientMtTable2 o1 = context.newObject(ClientMtTable2.class);
            o.addToTable2Array(o1);
            context.commitChanges();

            assertFalse(o.getObjectId().isTemporary());
            assertEquals(PersistenceState.COMMITTED, o.getPersistenceState());
            assertEquals(2, o.getTable2Array().size());

        }
        finally {
            callbackRegistry.clear();
        }
    }
}
