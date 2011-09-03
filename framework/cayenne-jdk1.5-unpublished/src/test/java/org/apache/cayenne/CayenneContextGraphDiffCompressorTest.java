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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.DataChannelSyncStats;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class CayenneContextGraphDiffCompressorTest extends ClientCase {

    @Inject(ClientCase.ROP_CLIENT_KEY)
    protected DataChannelInterceptor clientServerInterceptor;

    @Inject
    protected CayenneContext context;

    public void testMultipleSimpleProperties() {

        ClientMtTable1 o1 = context.newObject(ClientMtTable1.class);
        o1.setGlobalAttribute1("v1");
        o1.setGlobalAttribute1("v2");

        DataChannelSyncStats stats = clientServerInterceptor
                .runWithSyncStatsCollection(new UnitTestClosure() {

                    public void execute() {
                        context.commitChanges();
                    }
                });

        assertEquals(1, stats.nodePropertiesChanged);
        assertEquals(1, stats.nodesCreated);
    }

    public void testComplimentaryArcs() {

        ClientMtTable1 o1 = context.newObject(ClientMtTable1.class);
        ClientMtTable2 o2 = context.newObject(ClientMtTable2.class);
        o2.setTable1(o1);
        o2.setTable1(null);

        DataChannelSyncStats stats = clientServerInterceptor
                .runWithSyncStatsCollection(new UnitTestClosure() {

                    public void execute() {
                        context.commitChanges();
                    }
                });

        assertEquals(0, stats.nodePropertiesChanged);
        assertEquals(2, stats.nodesCreated);
        assertEquals(0, stats.arcsCreated);
        assertEquals(0, stats.arcsDeleted);
    }

    public void testDelete() {

        ClientMtTable1 o1 = context.newObject(ClientMtTable1.class);
        o1.setGlobalAttribute1("v1");
        context.deleteObjects(o1);

        DataChannelSyncStats stats = clientServerInterceptor
                .runWithSyncStatsCollection(new UnitTestClosure() {

                    public void execute() {
                        context.commitChanges();
                    }
                });
        assertEquals(0, stats.nodePropertiesChanged);
        assertEquals(0, stats.nodesCreated);
        assertEquals(0, stats.nodesRemoved);
    }

}
