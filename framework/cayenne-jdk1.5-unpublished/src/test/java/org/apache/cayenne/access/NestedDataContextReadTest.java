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
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class NestedDataContextReadTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DataChannelInterceptor queryInterceptor;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tArtist;
    private TableHelper tPainting;

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
                "PAINTING_TITLE",
                "ARTIST_ID",
                "ESTIMATED_PRICE").setColumnTypes(
                Types.INTEGER,
                Types.VARCHAR,
                Types.BIGINT,
                Types.DECIMAL);
    }

    private void createArtistsDataSet() throws Exception {
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
        tArtist.insert(33003, "artist3");
        tArtist.insert(33004, "artist4");
    }

    private void createRelationshipDataSet() throws Exception {
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
        tArtist.insert(33003, "artist3");
        tArtist.insert(33004, "artist4");
        tPainting.insert(33001, "P_artist1", 33001, 3000);
        tPainting.insert(33002, "P_artist2", 33002, 3000);
        tPainting.insert(33003, "P_artist3", 33003, 3000);
        tPainting.insert(33004, "P_artist4", 33004, 3000);
        tPainting.insert(33005, "P_artist5", null, 3000);
    }

    private void createPrefetchingDataSet() throws Exception {
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
        tPainting.insert(33001, "P_artist1", 33001, 3000);
        tPainting.insert(33006, "P_artist6", 33001, 3000);
    }

    public void testCreateChildDataContext() {
        context.setValidatingObjectsOnCommit(true);

        ObjectContext child1 = context.createChildContext();

        assertNotNull(child1);
        assertSame(context, child1.getChannel());
        assertTrue(((DataContext) child1).isValidatingObjectsOnCommit());

        context.setValidatingObjectsOnCommit(false);

        ObjectContext child2 = context.createChildContext();

        assertNotNull(child2);
        assertSame(context, child2.getChannel());
        assertFalse(((DataContext) child2).isValidatingObjectsOnCommit());

        // second level of nesting
        ObjectContext child21 = child2.createChildContext();

        assertNotNull(child21);
        assertSame(child2, child21.getChannel());
        assertFalse(((DataContext) child2).isValidatingObjectsOnCommit());
    }

    public void testLocalObjectSynchronize() throws Exception {
        createArtistsDataSet();

        final ObjectContext childContext = context.createChildContext();

        final Persistent _new = context.newObject(Artist.class);

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
        modified.setArtistName("M1");
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

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                Persistent newPeer = childContext.localObject(_new.getObjectId(), _new);

                assertEquals(_new.getObjectId(), newPeer.getObjectId());
                assertEquals(PersistenceState.COMMITTED, newPeer.getPersistenceState());

                assertSame(childContext, newPeer.getObjectContext());
                assertSame(context, _new.getObjectContext());

                Persistent hollowPeer = childContext.localObject(
                        hollow.getObjectId(),
                        hollow);
                assertEquals(PersistenceState.HOLLOW, hollowPeer.getPersistenceState());
                assertEquals(hollow.getObjectId(), hollowPeer.getObjectId());
                assertSame(childContext, hollowPeer.getObjectContext());
                assertSame(context, hollow.getObjectContext());

                Persistent committedPeer = childContext.localObject(committed
                        .getObjectId(), committed);
                assertEquals(PersistenceState.COMMITTED, committedPeer
                        .getPersistenceState());
                assertEquals(committed.getObjectId(), committedPeer.getObjectId());
                assertSame(childContext, committedPeer.getObjectContext());
                assertSame(context, committed.getObjectContext());

                Artist modifiedPeer = (Artist) childContext.localObject(modified
                        .getObjectId(), modified);
                assertEquals(PersistenceState.COMMITTED, modifiedPeer
                        .getPersistenceState());
                assertEquals(modified.getObjectId(), modifiedPeer.getObjectId());
                assertEquals("M1", modifiedPeer.getArtistName());
                assertSame(childContext, modifiedPeer.getObjectContext());
                assertSame(context, modified.getObjectContext());

                Persistent deletedPeer = childContext.localObject(
                        deleted.getObjectId(),
                        deleted);
                assertEquals(PersistenceState.COMMITTED, deletedPeer
                        .getPersistenceState());
                assertEquals(deleted.getObjectId(), deletedPeer.getObjectId());
                assertSame(childContext, deletedPeer.getObjectContext());
                assertSame(context, deleted.getObjectContext());

            }
        });
    }

    public void testLocalObjectsNoOverride() throws Exception {
        createArtistsDataSet();

        final ObjectContext childContext = context.createChildContext();

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
                Persistent peerModified2 = childContext.localObject(modified
                        .getObjectId(), modified);
                assertSame(peerModified, peerModified2);
                assertEquals(PersistenceState.MODIFIED, peerModified2
                        .getPersistenceState());
                assertEquals("M2", peerModified.getArtistName());
                assertEquals("M1", modified.getArtistName());
            }
        });
    }

    public void testLocalObjectRelationship() throws Exception {

        final ObjectContext childContext = context.createChildContext();

        Artist _new = context.newObject(Artist.class);
        final Painting _newP = context.newObject(Painting.class);
        _new.addToPaintingArray(_newP);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                Painting painting = (Painting) childContext.localObject(_newP
                        .getObjectId(), _newP);
                assertEquals(PersistenceState.COMMITTED, painting.getPersistenceState());
                assertNotNull(painting.getToArtist());
                assertEquals(PersistenceState.COMMITTED, painting
                        .getToArtist()
                        .getPersistenceState());
            }
        });
    }

    public void testSelect() throws Exception {
        createArtistsDataSet();

        ObjectContext child = context.createChildContext();

        // test how different object states appear in the child on select

        Persistent _new = context.newObject(Artist.class);

        Persistent hollow = context.localObject(new ObjectId(
                "Artist",
                Artist.ARTIST_ID_PK_COLUMN,
                33001), null);
        DataObject committed = (DataObject) Cayenne.objectForQuery(
                context,
                new ObjectIdQuery(new ObjectId(
                        "Artist",
                        Artist.ARTIST_ID_PK_COLUMN,
                        33002)));

        int modifiedId = 33003;
        Artist modified = (Artist) Cayenne.objectForQuery(context, new ObjectIdQuery(
                new ObjectId("Artist", Artist.ARTIST_ID_PK_COLUMN, modifiedId)));
        modified.setArtistName("MODDED");
        DataObject deleted = (DataObject) Cayenne.objectForQuery(
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

        List<?> objects = child.performQuery(new SelectQuery(Artist.class));
        assertEquals("All but NEW object must have been included", 4, objects.size());

        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            DataObject next = (DataObject) it.next();
            assertEquals(PersistenceState.COMMITTED, next.getPersistenceState());

            int id = Cayenne.intPKForObject(next);
            if (id == modifiedId) {
                assertEquals("MODDED", next.readProperty(Artist.ARTIST_NAME_PROPERTY));
            }
        }
    }

    public void testReadToOneRelationship() throws Exception {
        createRelationshipDataSet();

        final ObjectContext child = context.createChildContext();

        // test how different object states appear in the child on select

        int hollowTargetSrcId = 33001;
        int modifiedTargetSrcId = 33002;
        int deletedTargetSrcId = 33003;
        int committedTargetSrcId = 33004;
        int newTargetSrcId = 33005;

        Painting hollowTargetSrc = Cayenne.objectForPK(
                context,
                Painting.class,
                hollowTargetSrcId);
        Artist hollowTarget = hollowTargetSrc.getToArtist();

        Painting modifiedTargetSrc = Cayenne.objectForPK(
                context,
                Painting.class,
                modifiedTargetSrcId);
        Artist modifiedTarget = modifiedTargetSrc.getToArtist();
        modifiedTarget.setArtistName("M1");

        final Painting deletedTargetSrc = Cayenne.objectForPK(
                context,
                Painting.class,
                deletedTargetSrcId);
        Artist deletedTarget = deletedTargetSrc.getToArtist();
        deletedTargetSrc.setToArtist(null);
        context.deleteObjects(deletedTarget);

        Painting committedTargetSrc = Cayenne.objectForPK(
                context,
                Painting.class,
                committedTargetSrcId);
        Artist committedTarget = committedTargetSrc.getToArtist();
        committedTarget.getArtistName();

        final Painting newTargetSrc = Cayenne.objectForPK(
                context,
                Painting.class,
                newTargetSrcId);
        Artist newTarget = context.newObject(Artist.class);
        newTarget.setArtistName("N1");
        newTargetSrc.setToArtist(newTarget);

        assertEquals(PersistenceState.COMMITTED, hollowTargetSrc.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, modifiedTargetSrc.getPersistenceState());
        assertEquals(PersistenceState.MODIFIED, deletedTargetSrc.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, committedTargetSrc.getPersistenceState());
        assertEquals(PersistenceState.MODIFIED, newTargetSrc.getPersistenceState());

        assertEquals(PersistenceState.HOLLOW, hollowTarget.getPersistenceState());
        assertEquals(PersistenceState.MODIFIED, modifiedTarget.getPersistenceState());
        assertEquals(PersistenceState.DELETED, deletedTarget.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, committedTarget.getPersistenceState());
        assertEquals(PersistenceState.NEW, newTarget.getPersistenceState());

        // run an ordered query, so we can address specific objects directly by index
        SelectQuery q = new SelectQuery(Painting.class);
        q.addOrdering(Painting.PAINTING_TITLE_PROPERTY, SortOrder.ASCENDING);
        final List<?> childSources = child.performQuery(q);
        assertEquals(5, childSources.size());

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                Painting childHollowTargetSrc = (Painting) childSources.get(0);
                assertSame(child, childHollowTargetSrc.getObjectContext());
                Artist childHollowTarget = childHollowTargetSrc.getToArtist();
                assertNotNull(childHollowTarget);
                assertEquals(PersistenceState.HOLLOW, childHollowTarget
                        .getPersistenceState());
                assertSame(child, childHollowTarget.getObjectContext());

                Artist childModifiedTarget = ((Painting) childSources.get(1))
                        .getToArtist();

                assertEquals(PersistenceState.COMMITTED, childModifiedTarget
                        .getPersistenceState());
                assertSame(child, childModifiedTarget.getObjectContext());
                assertEquals("M1", childModifiedTarget.getArtistName());

                Painting childDeletedTargetSrc = (Painting) childSources.get(2);
                // make sure we got the right object...
                assertEquals(deletedTargetSrc.getObjectId(), childDeletedTargetSrc
                        .getObjectId());
                Artist childDeletedTarget = childDeletedTargetSrc.getToArtist();
                assertNull(childDeletedTarget);

                Artist childCommittedTarget = ((Painting) childSources.get(3))
                        .getToArtist();
                assertEquals(PersistenceState.COMMITTED, childCommittedTarget
                        .getPersistenceState());
                assertSame(child, childCommittedTarget.getObjectContext());

                Painting childNewTargetSrc = (Painting) childSources.get(4);
                // make sure we got the right object...
                assertEquals(newTargetSrc.getObjectId(), childNewTargetSrc.getObjectId());
                Artist childNewTarget = childNewTargetSrc.getToArtist();
                assertNotNull(childNewTarget);
                assertEquals(PersistenceState.COMMITTED, childNewTarget
                        .getPersistenceState());
                assertSame(child, childNewTarget.getObjectContext());
                assertEquals("N1", childNewTarget.getArtistName());
            }
        });
    }

    public void testPrefetchingToOne() throws Exception {
        createPrefetchingDataSet();

        final ObjectContext child = context.createChildContext();

        final ObjectId prefetchedId = new ObjectId(
                "Artist",
                Artist.ARTIST_ID_PK_COLUMN,
                new Integer(33001));

        SelectQuery q = new SelectQuery(Painting.class);
        q.addOrdering(Painting.PAINTING_TITLE_PROPERTY, SortOrder.ASCENDING);
        q.addPrefetch(Painting.TO_ARTIST_PROPERTY);

        final List<?> results = child.performQuery(q);

        // blockQueries();

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals(2, results.size());
                Iterator<?> it = results.iterator();
                while (it.hasNext()) {
                    Painting o = (Painting) it.next();
                    assertEquals(PersistenceState.COMMITTED, o.getPersistenceState());
                    assertSame(child, o.getObjectContext());

                    Artist o1 = o.getToArtist();
                    assertNotNull(o1);
                    assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());
                    assertSame(child, o1.getObjectContext());
                    assertEquals(prefetchedId, o1.getObjectId());
                }
            }
        });
    }

    public void testPrefetchingToMany() throws Exception {
        createPrefetchingDataSet();

        final ObjectContext child = context.createChildContext();

        SelectQuery q = new SelectQuery(Artist.class);
        q.addOrdering(Artist.ARTIST_NAME_PROPERTY, SortOrder.ASCENDING);
        q.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY);

        final List<?> results = child.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                Artist o1 = (Artist) results.get(0);
                assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());
                assertSame(child, o1.getObjectContext());

                List<?> children1 = o1.getPaintingArray();

                assertEquals(2, children1.size());
                Iterator<?> it = children1.iterator();
                while (it.hasNext()) {
                    Painting o = (Painting) it.next();
                    assertEquals(PersistenceState.COMMITTED, o.getPersistenceState());
                    assertSame(child, o.getObjectContext());

                    assertEquals(o1, o.getToArtist());
                }

                Artist o2 = (Artist) results.get(1);
                assertEquals(PersistenceState.COMMITTED, o2.getPersistenceState());
                assertSame(child, o2.getObjectContext());

                List<?> children2 = o2.getPaintingArray();

                assertEquals(0, children2.size());
            }
        });
    }

    public void testObjectFromDataRow() throws Exception {

        DataContext childContext = (DataContext) context.createChildContext();

        DataRow row = new DataRow(8);
        row.put("ARTIST_ID", 5l);
        row.put("ARTIST_NAME", "A");
        row.put("DATE_OF_BIRTH", new Date());

        Artist artist = childContext.objectFromDataRow(Artist.class, row);
        assertNotNull(artist);
        assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());
        assertSame(childContext, artist.getObjectContext());
        Object parentArtist = context.getObjectStore().getNode(artist.getObjectId());
        assertNotNull(parentArtist);
        assertNotSame(artist, parentArtist);
    }
}
