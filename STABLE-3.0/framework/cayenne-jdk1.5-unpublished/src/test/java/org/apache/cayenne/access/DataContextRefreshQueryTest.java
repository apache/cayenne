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

import org.apache.art.Artist;
import org.apache.art.Painting;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextRefreshQueryTest extends CayenneCase {

    public void testRefreshCollection() throws Exception {
        deleteTestData();
        createTestData("testRefreshCollection");

        DataContext context = createDataContext();

        SelectQuery q = new SelectQuery(Artist.class);
        q.addOrdering("db:ARTIST_ID", true);
        List artists = context.performQuery(q);

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
        deleteTestData();
        createTestData("testRefreshCollection");

        DataContext context = createDataContext();

        SelectQuery q = new SelectQuery(Painting.class);
        q.addOrdering("db:PAINTING_ID", true);
        List paints = context.performQuery(q);

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

        createTestData("testRefreshCollectionToOneUpdate");

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
        deleteTestData();
        createTestData("testRefreshCollection");

        DataContext context = createDataContext();

        SelectQuery q = new SelectQuery(Artist.class);
        q.addOrdering("db:ARTIST_ID", true);
        List artists = context.performQuery(q);

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
        deleteTestData();
        createTestData("testRefreshObjectToMany");

        DataContext context = createDataContext();

        Artist a = DataObjectUtils.objectForPK(context, Artist.class, 33001l);
        assertEquals(2, a.getPaintingArray().size());

        createTestData("testRefreshObjectToManyUpdate");

        RefreshQuery refresh = new RefreshQuery(a);
        context.performQuery(refresh);
        assertEquals(PersistenceState.HOLLOW, a.getPersistenceState());
        assertEquals(1, a.getPaintingArray().size());
    }

    public void testRefreshQueryResultsLocalCache() throws Exception {
        deleteTestData();
        createTestData("testRefreshCollection");

        DataContext context = createDataContext();

        Expression qual = ExpressionFactory.matchExp(
                Painting.PAINTING_TITLE_PROPERTY,
                "P2");
        SelectQuery q = new SelectQuery(Painting.class, qual);
        q.addOrdering("db:PAINTING_ID", true);
        q.setCachePolicy(QueryMetadata.LOCAL_CACHE);
        q.setCacheGroups("X");
        List paints = context.performQuery(q);

        // fetch P1 separately from cached query
        Painting p1 = DataObjectUtils.objectForPK(context, Painting.class, 33001);

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

        createTestData("testRefreshCollectionToOneUpdate");

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
        deleteTestData();
        createTestData("testRefreshCollection");

        DataContext context = createDataContext();

        Expression qual = ExpressionFactory.matchExp(
                Painting.PAINTING_TITLE_PROPERTY,
                "P2");
        SelectQuery q = new SelectQuery(Painting.class, qual);
        q.addOrdering("db:PAINTING_ID", true);
        q.setCachePolicy(QueryMetadata.SHARED_CACHE);
        q.setCacheGroups("X");
        List paints = context.performQuery(q);

        // fetch P1 separately from cached query
        Painting p1 = DataObjectUtils.objectForPK(context, Painting.class, 33001);

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

        createTestData("testRefreshCollectionToOneUpdate");

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
        deleteTestData();
        createTestData("testRefreshCollection");

        DataContext context = createDataContext();

        Expression qual = ExpressionFactory.matchExp(
                Painting.PAINTING_TITLE_PROPERTY,
                "P2");
        SelectQuery q = new SelectQuery(Painting.class, qual);
        q.addOrdering("db:PAINTING_ID", true);
        q.setCachePolicy(QueryMetadata.LOCAL_CACHE);
        q.setCacheGroups("X");
        List paints = context.performQuery(q);

        // fetch P1 separately from cached query
        Painting p1 = DataObjectUtils.objectForPK(context, Painting.class, 33001);

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

        createTestData("testRefreshCollectionToOneUpdate");

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
        deleteTestData();
        createTestData("testRefreshCollection");

        DataContext context = createDataContext();

        SelectQuery q = new SelectQuery(Artist.class);
        q.addOrdering("db:ARTIST_ID", true);
        List artists = context.performQuery(q);

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
