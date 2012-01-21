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

import java.sql.Types;
import java.util.Collections;
import java.util.Date;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

/**
 * Tests objects registration in DataContext, transferring objects between contexts and
 * such.
 */
@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextObjectTrackingTest extends ServerCase {

    @Inject
    protected DataChannelInterceptor queryInterceptor;

    @Inject
    protected DataContext context;

    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected ServerRuntime runtime;

    protected TableHelper tArtist;
    protected TableHelper tPainting;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");

        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns(
                "PAINTING_ID",
                "ARTIST_ID",
                "PAINTING_TITLE",
                "ESTIMATED_PRICE").setColumnTypes(
                Types.INTEGER,
                Types.BIGINT,
                Types.VARCHAR,
                Types.DECIMAL);
    }

    protected void createArtistsDataSet() throws Exception {
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
        tArtist.insert(33003, "artist3");
        tArtist.insert(33004, "artist4");
    }

    protected void createMixedDataSet() throws Exception {
        tArtist.insert(33003, "artist3");
        tPainting.insert(33003, 33003, "P_artist3", 3000);
    }

    public void testUnregisterObject() {

        DataRow row = new DataRow(10);
        row.put("ARTIST_ID", new Integer(1));
        row.put("ARTIST_NAME", "ArtistXYZ");
        row.put("DATE_OF_BIRTH", new Date());
        DataObject obj = context.objectFromDataRow(Artist.class, row);
        ObjectId oid = obj.getObjectId();

        assertEquals(PersistenceState.COMMITTED, obj.getPersistenceState());
        assertSame(context, obj.getObjectContext());
        assertSame(obj, context.getGraphManager().getNode(oid));

        context.unregisterObjects(Collections.singletonList(obj));

        assertEquals(PersistenceState.TRANSIENT, obj.getPersistenceState());
        assertNull(obj.getObjectContext());
        assertNull(obj.getObjectId());
        assertNull(context.getGraphManager().getNode(oid));
        assertNull(context.getObjectStore().getCachedSnapshot(oid));
    }

    public void testInvalidateObjects_Vararg() {

        DataRow row = new DataRow(10);
        row.put("ARTIST_ID", new Integer(1));
        row.put("ARTIST_NAME", "ArtistXYZ");
        row.put("DATE_OF_BIRTH", new Date());
        DataObject obj = context.objectFromDataRow(Artist.class, row);
        ObjectId oid = obj.getObjectId();

        assertEquals(PersistenceState.COMMITTED, obj.getPersistenceState());
        assertSame(context, obj.getObjectContext());
        assertSame(obj, context.getGraphManager().getNode(oid));

        context.invalidateObjects(obj);

        assertEquals(PersistenceState.HOLLOW, obj.getPersistenceState());
        assertSame(context, obj.getObjectContext());
        assertSame(oid, obj.getObjectId());
        assertNull(context.getObjectStore().getCachedSnapshot(oid));
        assertNotNull(context.getGraphManager().getNode(oid));
    }

    @Deprecated
    public void testLocalObjectPeerContextMap() throws Exception {
        createArtistsDataSet();

        // must create both contexts before running the queries, as each call to
        // 'createDataContext' clears the cache.

        final ObjectContext peerContext = runtime.getDataDomain().createDataContext();

        Persistent _new = context.newObject(Artist.class);

        final Persistent hollow = context.localObject(new ObjectId(
                "Artist",
                Artist.ARTIST_ID_PK_COLUMN,
                33001), null);
        final DataObject committed = (DataObject) Cayenne.objectForQuery(
                context,
                new ObjectIdQuery(new ObjectId(
                        "Artist",
                        Artist.ARTIST_ID_PK_COLUMN,
                        33002)));

        int modifiedId = 33003;
        final Artist modified = (Artist) Cayenne.objectForQuery(
                context,
                new ObjectIdQuery(new ObjectId(
                        "Artist",
                        Artist.ARTIST_ID_PK_COLUMN,
                        modifiedId)));
        modified.setArtistName("MODDED");
        final DataObject deleted = (DataObject) Cayenne.objectForQuery(
                context,
                new ObjectIdQuery(new ObjectId(
                        "Artist",
                        Artist.ARTIST_ID_PK_COLUMN,
                        33004)));
        context.deleteObjects(deleted);

        assertEquals(PersistenceState.HOLLOW, hollow.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, committed.getPersistenceState());
        assertEquals(PersistenceState.MODIFIED, modified.getPersistenceState());
        assertEquals(PersistenceState.DELETED, deleted.getPersistenceState());
        assertEquals(PersistenceState.NEW, _new.getPersistenceState());

        // now check how objects in different state behave

        // on the one hand sticking an alien NEW object to a peer DataContext doesn't look
        // like a good idea, on the other hand the code detecting whether a given context
        // is a child of another context breaks DataChannel encapsulation (i.e. using
        // "instanceof" to check DataChannel type will make it impossible to use Proxies).

        // try {
        // peerContext.localObjects(Collections.singletonList(_new));
        // fail("A presence of new object should have triggered an exception");
        // }
        // catch (CayenneRuntimeException e) {
        // // expected
        // }

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {

                Persistent hollowPeer = peerContext.localObject(
                        hollow.getObjectId(),
                        null);
                assertEquals(PersistenceState.HOLLOW, hollowPeer.getPersistenceState());
                assertEquals(hollow.getObjectId(), hollowPeer.getObjectId());
                assertSame(peerContext, hollowPeer.getObjectContext());
                assertSame(context, hollow.getObjectContext());

                Persistent committedPeer = peerContext.localObject(
                        committed.getObjectId(),
                        null);
                assertEquals(PersistenceState.HOLLOW, committedPeer.getPersistenceState());
                assertEquals(committed.getObjectId(), committedPeer.getObjectId());
                assertSame(peerContext, committedPeer.getObjectContext());
                assertSame(context, committed.getObjectContext());

                Persistent modifiedPeer = peerContext.localObject(
                        modified.getObjectId(),
                        null);
                assertEquals(PersistenceState.HOLLOW, modifiedPeer.getPersistenceState());
                assertEquals(modified.getObjectId(), modifiedPeer.getObjectId());
                assertSame(peerContext, modifiedPeer.getObjectContext());
                assertSame(context, modified.getObjectContext());

                Persistent deletedPeer = peerContext.localObject(
                        deleted.getObjectId(),
                        null);
                assertEquals(PersistenceState.HOLLOW, deletedPeer.getPersistenceState());
                assertEquals(deleted.getObjectId(), deletedPeer.getObjectId());
                assertSame(peerContext, deletedPeer.getObjectContext());
                assertSame(context, deleted.getObjectContext());
            }
        });
    }

    @Deprecated
    public void testLocalObjectPeerContextNoOverride() throws Exception {
        createArtistsDataSet();

        // must create both contexts before running the queries, as each call to
        // 'createDataContext' clears the cache.

        final ObjectContext peerContext = runtime.getDataDomain().createDataContext();

        int modifiedId = 33003;
        final Artist modified = (Artist) Cayenne.objectForQuery(
                context,
                new ObjectIdQuery(new ObjectId(
                        "Artist",
                        Artist.ARTIST_ID_PK_COLUMN,
                        modifiedId)));
        final Artist peerModified = (Artist) Cayenne.objectForQuery(
                peerContext,
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

                Persistent peerModified2 = peerContext.localObject(
                        modified.getObjectId(),
                        null);

                assertSame(peerModified, peerModified2);
                assertEquals(
                        PersistenceState.MODIFIED,
                        peerModified2.getPersistenceState());
                assertEquals("M2", peerModified.getArtistName());
                assertEquals("M1", modified.getArtistName());
            }
        });
    }
}
