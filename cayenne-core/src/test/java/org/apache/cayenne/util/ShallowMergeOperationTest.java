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
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class ShallowMergeOperationTest extends ServerCase {

    @Inject
    private ServerRuntime runtime;

    @Inject
    private DataContext context;

    @Inject
    private DataChannelInterceptor queryInterceptor;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tArtist;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");

        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

    }

    private void createArtistsDataSet() throws Exception {
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
        tArtist.insert(33003, "artist3");
        tArtist.insert(33004, "artist4");
    }

    public void testMerge_Relationship() throws Exception {

        ObjectContext childContext = runtime.newContext(context);
        final ShallowMergeOperation op = new ShallowMergeOperation(childContext);

        Artist _new = context.newObject(Artist.class);
        final Painting _newP = context.newObject(Painting.class);
        _new.addToPaintingArray(_newP);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                Painting painting = op.merge(_newP);

                assertEquals(PersistenceState.COMMITTED, painting.getPersistenceState());
                assertNotNull(painting.getToArtist());
                assertEquals(PersistenceState.COMMITTED, painting
                        .getToArtist()
                        .getPersistenceState());
            }
        });
    }

    public void testMerge_NoOverride() throws Exception {
        createArtistsDataSet();

        ObjectContext childContext = runtime.newContext(context);
        final ShallowMergeOperation op = new ShallowMergeOperation(childContext);

        int modifiedId = 33003;
        final Artist modified = (Artist) Cayenne.objectForQuery(
                context,
                new ObjectIdQuery(new ObjectId(
                        "Artist",
                        Artist.ARTIST_ID_PK_COLUMN,
                        modifiedId)));
        final Artist peerModified = (Artist) Cayenne.objectForQuery(
                childContext,
                new ObjectIdQuery(new ObjectId(
                        "Artist",
                        Artist.ARTIST_ID_PK_COLUMN,
                        modifiedId)));

        modified.setArtistName("M1");
        peerModified.setArtistName("M2");

        assertEquals(PersistenceState.MODIFIED, modified.getPersistenceState());
        assertEquals(PersistenceState.MODIFIED, peerModified.getPersistenceState());

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                Persistent peerModified2 = op.merge(modified);
                assertSame(peerModified, peerModified2);
                assertEquals(
                        PersistenceState.MODIFIED,
                        peerModified2.getPersistenceState());
                assertEquals("M2", peerModified.getArtistName());
                assertEquals("M1", modified.getArtistName());
            }
        });
    }

    public void testMerge_PersistenceStates() throws Exception {
        createArtistsDataSet();

        final ObjectContext childContext = runtime.newContext(context);
        final ShallowMergeOperation op = new ShallowMergeOperation(childContext);

        final Artist _new = context.newObject(Artist.class);

        final Artist hollow = Cayenne.objectForPK(context, Artist.class, 33001);
        context.invalidateObjects(hollow);

        final Artist committed = Cayenne.objectForPK(context, Artist.class, 33002);

        final Artist modified = Cayenne.objectForPK(context, Artist.class, 33003);
        modified.setArtistName("M1");

        final Artist deleted = Cayenne.objectForPK(context, Artist.class, 33004);
        context.deleteObjects(deleted);

        assertEquals(PersistenceState.HOLLOW, hollow.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, committed.getPersistenceState());
        assertEquals(PersistenceState.MODIFIED, modified.getPersistenceState());
        assertEquals(PersistenceState.DELETED, deleted.getPersistenceState());
        assertEquals(PersistenceState.NEW, _new.getPersistenceState());

        // now check how objects in different state behave
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

                Artist modifiedPeer = op.merge(modified);
                assertEquals(
                        PersistenceState.COMMITTED,
                        modifiedPeer.getPersistenceState());
                assertEquals(modified.getObjectId(), modifiedPeer.getObjectId());
                assertEquals("M1", modifiedPeer.getArtistName());
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
