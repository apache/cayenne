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

import java.util.List;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextRefreshQueryTest extends ServerCase {

    @Inject
    protected DataContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tArtist;
    protected TableHelper tPainting;

    @Override
    public void setUpAfterInjection() throws Exception {
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
                "ESTIMATED_PRICE");
    }

    protected void createRefreshCollectionDataSet() throws Exception {
        tArtist.insert(33001, "c");
        tArtist.insert(33002, "b");
        tPainting.insert(33001, "P1", 33001, 3000);
        tPainting.insert(33002, "P2", 33001, 4000);
    }

    protected void createRefreshCollectionToOneUpdateDataSet() throws Exception {
        tPainting.update().set("ARTIST_ID", 33002).execute();
    }

    protected void createRefreshObjectToManyDataSet() throws Exception {
        tArtist.insert(33001, "c");
        tPainting.insert(33001, "P1", 33001, 3000);
        tPainting.insert(33002, "P2", 33001, 4000);
    }

    protected void createRefreshObjectToManyUpdateDataSet() throws Exception {
        tPainting.delete().where("PAINTING_ID", 33001).execute();
    }

    public void testRefreshCollection() throws Exception {
        createRefreshCollectionDataSet();

        SelectQuery q = new SelectQuery(Artist.class);
        q.addOrdering("db:ARTIST_ID", SortOrder.ASCENDING);
        List<?> artists = context.performQuery(q);

        Artist a1 = (Artist) artists.get(0);
        Artist a2 = (Artist) artists.get(1);

        assertEquals(2, a1.getPaintingArray().size());
        assertEquals(0, a2.getPaintingArray().size());

        assertNotNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(a1.getObjectId()));
        assertNotNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(a2.getObjectId()));

        RefreshQuery refresh = new RefreshQuery(artists);
        context.performQuery(refresh);

        assertNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(a1.getObjectId()));
        assertNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(a2.getObjectId()));

        assertEquals(PersistenceState.HOLLOW, a1.getPersistenceState());
        assertEquals(PersistenceState.HOLLOW, a2.getPersistenceState());

        assertTrue(((ValueHolder) a1.readProperty(Artist.PAINTING_ARRAY_PROPERTY))
                .isFault());
        assertTrue(((ValueHolder) a2.readProperty(Artist.PAINTING_ARRAY_PROPERTY))
                .isFault());
    }

    public void testRefreshCollectionToOne() throws Exception {
        createRefreshCollectionDataSet();

        SelectQuery q = new SelectQuery(Painting.class);
        q.addOrdering("db:PAINTING_ID", SortOrder.ASCENDING);
        List<?> paints = context.performQuery(q);

        Painting p1 = (Painting) paints.get(0);
        Painting p2 = (Painting) paints.get(1);

        Artist a1 = p1.getToArtist();
        assertSame(a1, p2.getToArtist());

        assertNotNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(p1.getObjectId()));
        assertNotNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(p2.getObjectId()));

        createRefreshCollectionToOneUpdateDataSet();

        RefreshQuery refresh = new RefreshQuery(paints);
        context.performQuery(refresh);

        assertNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(p1.getObjectId()));
        assertNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(p2.getObjectId()));

        assertEquals(PersistenceState.HOLLOW, p1.getPersistenceState());
        assertEquals(PersistenceState.HOLLOW, p2.getPersistenceState());

        assertNotSame(a1, p1.getToArtist());
        assertNotSame(a1, p2.getToArtist());
        assertEquals("b", p1.getToArtist().getArtistName());
    }

    public void testRefreshSingleObject() throws Exception {
        createRefreshCollectionDataSet();

        SelectQuery q = new SelectQuery(Artist.class);
        q.addOrdering("db:ARTIST_ID", SortOrder.ASCENDING);
        List<?> artists = context.performQuery(q);

        Artist a1 = (Artist) artists.get(0);

        assertEquals(2, a1.getPaintingArray().size());

        assertNotNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(a1.getObjectId()));

        RefreshQuery refresh = new RefreshQuery(a1);
        context.performQuery(refresh);

        assertNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(a1.getObjectId()));

        assertEquals(PersistenceState.HOLLOW, a1.getPersistenceState());

        assertTrue(((ValueHolder) a1.readProperty(Artist.PAINTING_ARRAY_PROPERTY))
                .isFault());
    }

    public void testRefreshObjectToMany() throws Exception {
        createRefreshObjectToManyDataSet();

        Artist a = Cayenne.objectForPK(context, Artist.class, 33001l);
        assertEquals(2, a.getPaintingArray().size());

        createRefreshObjectToManyUpdateDataSet();

        RefreshQuery refresh = new RefreshQuery(a);
        context.performQuery(refresh);
        assertEquals(PersistenceState.HOLLOW, a.getPersistenceState());
        assertEquals(1, a.getPaintingArray().size());
    }

    public void testRefreshQueryResultsLocalCache() throws Exception {
        createRefreshCollectionDataSet();

        Expression qual = ExpressionFactory.matchExp(
                Painting.PAINTING_TITLE_PROPERTY,
                "P2");
        SelectQuery q = new SelectQuery(Painting.class, qual);
        q.addOrdering("db:PAINTING_ID", SortOrder.ASCENDING);
        q.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        q.setCacheGroups("X");
        List<?> paints = context.performQuery(q);

        // fetch P1 separately from cached query
        Painting p1 = Cayenne.objectForPK(context, Painting.class, 33001);

        Painting p2 = (Painting) paints.get(0);
        Artist a1 = p2.getToArtist();
        assertSame(a1, p1.getToArtist());

        assertNotNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(p1.getObjectId()));

        assertNotNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(p2.getObjectId()));

        createRefreshCollectionToOneUpdateDataSet();

        RefreshQuery refresh = new RefreshQuery(q);
        context.performQuery(refresh);

        assertNotNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(p1.getObjectId()));

        // probably refreshed eagerly
        assertNotNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(p2.getObjectId()));

        assertEquals(PersistenceState.COMMITTED, p1.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, p2.getPersistenceState());

        assertSame(a1, p1.getToArtist());
        assertNotSame(a1, p2.getToArtist());
        assertEquals("c", p1.getToArtist().getArtistName());
        assertEquals("b", p2.getToArtist().getArtistName());
    }

    public void testRefreshQueryResultsSharedCache() throws Exception {
        createRefreshCollectionDataSet();

        Expression qual = ExpressionFactory.matchExp(
                Painting.PAINTING_TITLE_PROPERTY,
                "P2");
        SelectQuery q = new SelectQuery(Painting.class, qual);
        q.addOrdering("db:PAINTING_ID", SortOrder.ASCENDING);
        q.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        q.setCacheGroups("X");
        List<?> paints = context.performQuery(q);

        // fetch P1 separately from cached query
        Painting p1 = Cayenne.objectForPK(context, Painting.class, 33001);

        Painting p2 = (Painting) paints.get(0);
        Artist a1 = p2.getToArtist();
        assertSame(a1, p1.getToArtist());

        assertNotNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(p1.getObjectId()));

        assertNotNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(p2.getObjectId()));

        createRefreshCollectionToOneUpdateDataSet();

        RefreshQuery refresh = new RefreshQuery(q);
        context.performQuery(refresh);

        assertNotNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(p1.getObjectId()));

        // probably refreshed eagerly
        assertNotNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(p2.getObjectId()));

        assertEquals(PersistenceState.COMMITTED, p1.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, p2.getPersistenceState());

        assertSame(a1, p1.getToArtist());
        assertNotSame(a1, p2.getToArtist());
        assertEquals("c", p1.getToArtist().getArtistName());
        assertEquals("b", p2.getToArtist().getArtistName());
    }

    public void testRefreshQueryResultGroupLocal() throws Exception {
        createRefreshCollectionDataSet();

        Expression qual = ExpressionFactory.matchExp(
                Painting.PAINTING_TITLE_PROPERTY,
                "P2");
        SelectQuery q = new SelectQuery(Painting.class, qual);
        q.addOrdering("db:PAINTING_ID", SortOrder.ASCENDING);
        q.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        q.setCacheGroups("X");
        List<?> paints = context.performQuery(q);

        // fetch P1 separately from cached query
        Painting p1 = Cayenne.objectForPK(context, Painting.class, 33001);

        Painting p2 = (Painting) paints.get(0);
        Artist a1 = p2.getToArtist();
        assertSame(a1, p1.getToArtist());

        assertNotNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(p1.getObjectId()));

        assertNotNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(p2.getObjectId()));

        createRefreshCollectionToOneUpdateDataSet();

        // results are served from cache and therefore are not refreshed
        context.performQuery(q);
        assertSame(a1, p1.getToArtist());
        assertSame(a1, p2.getToArtist());
        assertEquals("c", p1.getToArtist().getArtistName());
        assertEquals("c", p2.getToArtist().getArtistName());

        RefreshQuery refresh = new RefreshQuery("X");

        // this should invalidate results for the next query run
        context.performQuery(refresh);

        // this should force a refresh
        context.performQuery(q);

        assertEquals(PersistenceState.COMMITTED, p1.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, p2.getPersistenceState());

        assertSame(a1, p1.getToArtist());
        assertNotSame(a1, p2.getToArtist());
        assertEquals("c", p1.getToArtist().getArtistName());
        assertEquals("b", p2.getToArtist().getArtistName());
    }

    public void testRefreshAll() throws Exception {
        createRefreshCollectionDataSet();

        SelectQuery q = new SelectQuery(Artist.class);
        q.addOrdering("db:ARTIST_ID", SortOrder.ASCENDING);
        List<?> artists = context.performQuery(q);

        Artist a1 = (Artist) artists.get(0);
        Artist a2 = (Artist) artists.get(1);
        Painting p1 = a1.getPaintingArray().get(0);
        Painting p2 = a1.getPaintingArray().get(0);

        assertNotNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(a1.getObjectId()));
        assertNotNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(a2.getObjectId()));
        assertNotNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(p1.getObjectId()));
        assertNotNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(p2.getObjectId()));

        RefreshQuery refresh = new RefreshQuery();
        context.performQuery(refresh);

        assertNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(a1.getObjectId()));
        assertNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(a2.getObjectId()));
        assertNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(p1.getObjectId()));
        assertNull(context
                .getParentDataDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshot(p2.getObjectId()));

        assertEquals(PersistenceState.HOLLOW, a1.getPersistenceState());
        assertEquals(PersistenceState.HOLLOW, a2.getPersistenceState());
        assertEquals(PersistenceState.HOLLOW, p1.getPersistenceState());
        assertEquals(PersistenceState.HOLLOW, p2.getPersistenceState());
    }
}
