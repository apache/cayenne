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
package org.apache.cayenne.util;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.CayenneContext;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.configuration.rop.client.ClientRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class ShallowMergeOperation_ClientTest extends ClientCase {

    @Inject
    private ClientRuntime runtime;

    @Inject
    private CayenneContext context;

    @Inject
    private DataChannelInterceptor queryInterceptor;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tMtTable1;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("MT_TABLE2");
        dbHelper.deleteAll("MT_TABLE1");
        dbHelper.deleteAll("MT_JOIN45");
        dbHelper.deleteAll("MT_TABLE4");
        dbHelper.deleteAll("MT_TABLE5");

        tMtTable1 = new TableHelper(dbHelper, "MT_TABLE1");
        tMtTable1.setColumns("TABLE1_ID", "GLOBAL_ATTRIBUTE1", "SERVER_ATTRIBUTE1");
    }

    private void createMtTable1DataSet() throws Exception {
        tMtTable1.insert(33001, "g1", "s1");
        tMtTable1.insert(33002, "g2", "s2");
        tMtTable1.insert(33003, "g3", "s3");
        tMtTable1.insert(33004, "g4", "s4");
    }

    public void testMerge_Relationship() throws Exception {

        ObjectContext childContext = runtime.newContext(context);
        final ShallowMergeOperation op = new ShallowMergeOperation(childContext);

        ClientMtTable1 _new = context.newObject(ClientMtTable1.class);
        final ClientMtTable2 _new2 = context.newObject(ClientMtTable2.class);
        _new.addToTable2Array(_new2);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                ClientMtTable2 child2 = op.merge(_new2);
                assertEquals(PersistenceState.COMMITTED, child2.getPersistenceState());
                assertNotNull(child2.getTable1());
                assertEquals(PersistenceState.COMMITTED, child2
                        .getTable1()
                        .getPersistenceState());
            }
        });
    }

    public void testMerge_NoOverride() throws Exception {

        ObjectContext childContext = runtime.newContext(context);
        final ShallowMergeOperation op = new ShallowMergeOperation(childContext);

        final ClientMtTable1 modified = context.newObject(ClientMtTable1.class);
        context.commitChanges();

        final ClientMtTable1 peerModified = (ClientMtTable1) Cayenne.objectForQuery(
                childContext,
                new ObjectIdQuery(modified.getObjectId()));

        modified.setGlobalAttribute1("M1");
        peerModified.setGlobalAttribute1("M2");

        assertEquals(PersistenceState.MODIFIED, modified.getPersistenceState());
        assertEquals(PersistenceState.MODIFIED, peerModified.getPersistenceState());

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                Persistent peerModified2 = op.merge(modified);
                assertSame(peerModified, peerModified2);
                assertEquals(
                        PersistenceState.MODIFIED,
                        peerModified2.getPersistenceState());
                assertEquals("M2", peerModified.getGlobalAttribute1());
                assertEquals("M1", modified.getGlobalAttribute1());
            }
        });
    }

    public void testMerge_PersistenceStates() throws Exception {

        createMtTable1DataSet();

        final ObjectContext childContext = runtime.newContext(context);
        final ShallowMergeOperation op = new ShallowMergeOperation(childContext);

        final ClientMtTable1 _new = context.newObject(ClientMtTable1.class);

        final ClientMtTable1 hollow = Cayenne.objectForPK(
                context,
                ClientMtTable1.class,
                33001);
        context.invalidateObjects(hollow);

        final ClientMtTable1 committed = Cayenne.objectForPK(
                context,
                ClientMtTable1.class,
                33002);

        final ClientMtTable1 modified = Cayenne.objectForPK(
                context,
                ClientMtTable1.class,
                33003);
        modified.setGlobalAttribute1("XXX");

        final ClientMtTable1 deleted = Cayenne.objectForPK(
                context,
                ClientMtTable1.class,
                33004);
        context.deleteObjects(deleted);

        assertEquals(PersistenceState.HOLLOW, hollow.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, committed.getPersistenceState());
        assertEquals(PersistenceState.MODIFIED, modified.getPersistenceState());
        assertEquals(PersistenceState.DELETED, deleted.getPersistenceState());
        assertEquals(PersistenceState.NEW, _new.getPersistenceState());

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                Persistent newPeer = op.merge(_new);

                assertEquals(_new.getObjectId(), newPeer.getObjectId());
                assertEquals(PersistenceState.COMMITTED, newPeer.getPersistenceState());

                assertSame(childContext, newPeer.getObjectContext());
                assertSame(context, _new.getObjectContext());

                Persistent hollowPeer = op.merge(hollow);
                assertEquals(PersistenceState.HOLLOW, hollowPeer.getPersistenceState());
                assertEquals(hollow.getObjectId(), hollowPeer.getObjectId());
                assertSame(childContext, hollowPeer.getObjectContext());
                assertSame(context, hollow.getObjectContext());

                Persistent committedPeer = op.merge(committed);
                assertEquals(
                        PersistenceState.COMMITTED,
                        committedPeer.getPersistenceState());
                assertEquals(committed.getObjectId(), committedPeer.getObjectId());
                assertSame(childContext, committedPeer.getObjectContext());
                assertSame(context, committed.getObjectContext());

                ClientMtTable1 modifiedPeer = op.merge(modified);
                assertEquals(
                        PersistenceState.COMMITTED,
                        modifiedPeer.getPersistenceState());
                assertEquals(modified.getObjectId(), modifiedPeer.getObjectId());
                assertEquals("XXX", modifiedPeer.getGlobalAttribute1());
                assertSame(childContext, modifiedPeer.getObjectContext());
                assertSame(context, modified.getObjectContext());

                Persistent deletedPeer = op.merge(deleted);
                assertEquals(
                        PersistenceState.COMMITTED,
                        deletedPeer.getPersistenceState());
                assertEquals(deleted.getObjectId(), deletedPeer.getObjectId());
                assertSame(childContext, deletedPeer.getObjectContext());
                assertSame(context, deleted.getObjectContext());
            }
        });
    }
}
