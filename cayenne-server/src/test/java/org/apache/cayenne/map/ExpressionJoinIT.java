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
package org.apache.cayenne.map;

import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.join.ArtistJoinExp;
import org.apache.cayenne.testdo.join.PaintingInfoJoinExp;
import org.apache.cayenne.testdo.join.PaintingJoinExp;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @since 4.2
 */
@UseServerRuntime(CayenneProjects.JOIN_EXP_PROJECT)
public class ExpressionJoinIT extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Inject
    protected DataChannelInterceptor queryInterceptor;

    @Before
    public void createArtistsDataSet() throws Exception {
        TableHelper tArtist = new TableHelper(dbHelper, "ARTIST_JOIN_EXP");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");
        tArtist.insert(1, "qwerty");
        tArtist.insert(2, "qwertyu");

        TableHelper tPaintings = new TableHelper(dbHelper, "PAINTING_JOIN_EXP");
        tPaintings.setColumns("PAINTING_ID", "PAINTING_JOIN_ID", "PAINTING_NAME");
        for (int i = 1; i <= 20; i++) {
            tPaintings.insert(i, i, "abcde" + i);
        }
        tPaintings.insert(21, 21, "custom");

        TableHelper tPaintingsInfo = new TableHelper(dbHelper, "PAINTING_INFO_JOIN_EXP");
        tPaintingsInfo.setColumns("INFO", "INFO_ID", "INFO_JOIN_ID");
        for(int i = 1; i <= 20; i++) {
            tPaintingsInfo.insert("abcde" + i, i, i);
        }
        tPaintingsInfo.insert("custom", 21, 21);
        tPaintingsInfo.insert("custom1", 22, null);
    }

    @Test
    public void testResolvingToManyFault() {
        ArtistJoinExp artist = ObjectSelect.query(ArtistJoinExp.class)
                .where(ExpressionFactory.matchDbExp("ARTIST_ID", 1))
                .selectOne(context);
        assertNotNull(artist);

        List<PaintingJoinExp> paintings = artist.getPaintings();
        assertEquals(10, paintings.size());
    }

    @Test
    public void testResolvingToOneFault() {
        PaintingJoinExp painting = ObjectSelect.query(PaintingJoinExp.class)
                .where(ExpressionFactory.matchDbExp("PAINTING_ID", 11))
                .selectOne(context);
        assertNotNull(painting);

        ArtistJoinExp artist = painting.getToArtist();
        assertNotNull(artist);

    }

    @Test
    public void testResolveToOneRel() {
        PaintingJoinExp painting = ObjectSelect.query(PaintingJoinExp.class)
                .where(ExpressionFactory.matchDbExp("PAINTING_ID", 1))
                .selectOne(context);
        assertNotNull(painting);

        PaintingInfoJoinExp info = painting.getPaintingInfo();
        assertNotNull(info);
        assertEquals(1, info.getInfoJoinId());
    }

    @Test
    public void testColumnQuery() {
        List<PaintingJoinExp> paintings = ObjectSelect.query(ArtistJoinExp.class)
                .column(ArtistJoinExp.PAINTINGS.flat())
                .where(ExpressionFactory.matchDbExp("ARTIST_ID", 1))
                .select(context);

        assertEquals(10, paintings.size());
    }

    @Test
    public void testQualifier() {
        List<ArtistJoinExp> artists = ObjectSelect.query(ArtistJoinExp.class)
                .where(ArtistJoinExp.PAINTINGS.dot(PaintingJoinExp.PAINTING_JOIN_ID).eq(1))
                .select(context);

        assertEquals(1, artists.size());
        assertEquals("qwerty", artists.get(0).getArtistName());
    }

    @Test
    public void testJointPrefetchToMany() {
        ArtistJoinExp artist = ObjectSelect.query(ArtistJoinExp.class)
                .where(ExpressionFactory.matchDbExp("ARTIST_ID", 1))
                .prefetch(ArtistJoinExp.PAINTINGS.joint())
                .selectOne(context);
        assertNotNull(artist);

        assertTrue(artist.readPropertyDirectly(ArtistJoinExp.PAINTINGS.getName()) instanceof List);
        List<PaintingJoinExp> paintings = (List<PaintingJoinExp>)artist.readPropertyDirectly(ArtistJoinExp.PAINTINGS.getName());
        assertEquals(10, paintings.size());
        assertTrue(paintings.get(0).getPaintingName().startsWith("abcd"));
    }

    @Test
    public void testDisjointPrefetchToMany() {
        ArtistJoinExp artist = ObjectSelect.query(ArtistJoinExp.class)
                .prefetch(ArtistJoinExp.PAINTINGS.disjoint())
                .where(ExpressionFactory.matchDbExp("ARTIST_ID", 1))
                .selectOne(context);
        assertNotNull(artist);

        assertTrue(artist.readPropertyDirectly(ArtistJoinExp.PAINTINGS.getName()) instanceof List);
        List<PaintingJoinExp> paintings = (List<PaintingJoinExp>)artist.readPropertyDirectly(ArtistJoinExp.PAINTINGS.getName());
        assertEquals(10, paintings.size());
        assertTrue(paintings.get(0).getPaintingName().startsWith("abcd"));
    }

    @Test
    public void testDisjointByIdPrefetchToMany() {
        ArtistJoinExp artist = ObjectSelect.query(ArtistJoinExp.class)
                .prefetch(ArtistJoinExp.PAINTINGS.disjointById())
                .where(ExpressionFactory.matchDbExp("ARTIST_ID", 1))
                .selectOne(context);
        assertNotNull(artist);

        assertTrue(artist.readPropertyDirectly(ArtistJoinExp.PAINTINGS.getName()) instanceof List);
        List<PaintingJoinExp> paintings = (List<PaintingJoinExp>)artist.readPropertyDirectly(ArtistJoinExp.PAINTINGS.getName());
        assertEquals(10, paintings.size());
        assertTrue(paintings.get(0).getPaintingName().startsWith("abcd"));
    }

    @Test
    public void testJointPrefetchToOne() {
        PaintingJoinExp painting = ObjectSelect.query(PaintingJoinExp.class)
                .prefetch(PaintingJoinExp.TO_ARTIST.joint())
                .where(ExpressionFactory.matchDbExp("PAINTING_ID", 1))
                .selectOne(context);
        assertNotNull(painting);

        assertTrue(painting.readPropertyDirectly(PaintingJoinExp.TO_ARTIST.getName()) instanceof ArtistJoinExp);
        ArtistJoinExp artist = (ArtistJoinExp) painting.readPropertyDirectly(PaintingJoinExp.TO_ARTIST.getName());
        assertEquals("qwerty", artist.getArtistName());
    }

    @Test
    public void testDisjointPrefetchToOne() {
        PaintingJoinExp painting = ObjectSelect.query(PaintingJoinExp.class)
                .prefetch(PaintingJoinExp.TO_ARTIST.disjoint())
                .where(ExpressionFactory.matchDbExp("PAINTING_ID", 1))
                .selectOne(context);
        assertNotNull(painting);

        assertTrue(painting.readPropertyDirectly(PaintingJoinExp.TO_ARTIST.getName()) instanceof ArtistJoinExp);
        ArtistJoinExp artist = (ArtistJoinExp) painting.readPropertyDirectly(PaintingJoinExp.TO_ARTIST.getName());
        assertEquals("qwerty", artist.getArtistName());
    }

    @Test
    public void testDisjointByIdToOne() {
        PaintingJoinExp painting = ObjectSelect.query(PaintingJoinExp.class)
                .prefetch(PaintingJoinExp.TO_ARTIST.disjointById())
                .where(ExpressionFactory.matchDbExp("PAINTING_ID", 1))
                .selectOne(context);
        assertNotNull(painting);

        assertTrue(painting.readPropertyDirectly(PaintingJoinExp.TO_ARTIST.getName()) instanceof ArtistJoinExp);
        ArtistJoinExp artist = (ArtistJoinExp) painting.readPropertyDirectly(PaintingJoinExp.TO_ARTIST.getName());
        assertEquals("qwerty", artist.getArtistName());
    }

    @Test
    public void testTwoExpressionsJoin() {
        PaintingInfoJoinExp paintingInfo = ObjectSelect.query(PaintingInfoJoinExp.class)
                .where(ExpressionFactory.matchDbExp("INFO_ID", 1))
                .selectOne(context);
        assertNotNull(paintingInfo);

        PaintingJoinExp painting = paintingInfo.getToPainting();
        assertNotNull(painting);
        assertEquals("abcde1", painting.getPaintingName());
    }

    @Test
    public void testSelectQueryWithPrefetch() {
        SelectQuery<ArtistJoinExp> artistSelectQuery = new SelectQuery<>(ArtistJoinExp.class);
        artistSelectQuery.addPrefetch(ArtistJoinExp.PAINTINGS.disjoint());
        artistSelectQuery.setQualifier(ExpressionFactory.matchDbExp("ARTIST_ID", 1));

        ArtistJoinExp artist = artistSelectQuery.selectOne(context);

        assertTrue(artist.readPropertyDirectly(ArtistJoinExp.PAINTINGS.getName()) instanceof List);
        List<PaintingJoinExp> paintings = (List<PaintingJoinExp>)artist.readPropertyDirectly(ArtistJoinExp.PAINTINGS.getName());
        assertEquals(10, paintings.size());
        assertTrue(paintings.get(0).getPaintingName().startsWith("abcd"));
    }

    @Test
    public void testCompoundPathInQualifier() {
        List<ArtistJoinExp> artists = ObjectSelect.query(ArtistJoinExp.class)
                .where(ArtistJoinExp.PAINTINGS
                        .dot(PaintingJoinExp.PAINTING_INFO)
                        .dot(PaintingInfoJoinExp.INFO)
                        .eq("abcde1"))
                .select(context);

        assertEquals(1, artists.size());
        assertEquals("qwerty", artists.get(0).getArtistName());
    }

    @Test
    public void testHavingQualifier() {
        List<ArtistJoinExp> artists = ObjectSelect.query(ArtistJoinExp.class)
                .having(ArtistJoinExp.PAINTINGS.count().gt(10L))
                .select(context);

        assertEquals(1, artists.size());
        assertEquals("qwertyu", artists.get(0).getArtistName());
    }

    @Test
    public void testOrder() {
        List<PaintingJoinExp> paintings = ObjectSelect.query(PaintingJoinExp.class)
                .orderBy(PaintingJoinExp.PAINTING_INFO
                        .dot(PaintingInfoJoinExp.INFO_JOIN_ID)
                        .asc())
                .select(context);

        assertEquals(21, paintings.size());
        assertEquals("abcde1", paintings.get(0).getPaintingName());
    }

    @Test
    public void testCachedQuery() {
        ObjectSelect<ArtistJoinExp> artistQuery = ObjectSelect.query(ArtistJoinExp.class)
                .prefetch(ArtistJoinExp.PAINTINGS.disjoint())
                .sharedCache("g1");

        List<ArtistJoinExp> artists = artistQuery.select(context);
        assertEquals(2, artists.size());
        assertTrue(artists.get(0)
                .readPropertyDirectly(ArtistJoinExp.PAINTINGS.getName())
                instanceof List);

        queryInterceptor.runWithQueriesBlocked(() -> {
            List<ArtistJoinExp> artists1 = artistQuery.select(context);
            assertEquals(2, artists1.size());
            assertTrue(artists1.get(0)
                    .readPropertyDirectly(ArtistJoinExp.PAINTINGS.getName())
                    instanceof List);
        });
    }

    @Test
    public void testDeleteRuleCascade() {
        ObjectSelect<PaintingJoinExp> q1 = ObjectSelect.query(PaintingJoinExp.class)
                .where(ExpressionFactory.matchDbExp("PAINTING_ID", 1));
        ObjectSelect<PaintingInfoJoinExp> q2 = ObjectSelect.query(PaintingInfoJoinExp.class)
                .where(ExpressionFactory.matchDbExp("INFO_ID", 1));
        PaintingJoinExp painting1 = q1
                .selectOne(context);
        assertNotNull(painting1);
        PaintingInfoJoinExp info1 = q2
                .selectOne(context);
        assertNotNull(info1);

        context.deleteObject(painting1);
        context.commitChanges();

        PaintingJoinExp painting2 = q1
                .selectOne(context);
        assertNull(painting2);
        PaintingInfoJoinExp info2 = q2
                .selectOne(context);
        assertNull(info2);
    }

    @Test
    public void testCompoundPrefetchDisjoint() {
        List<ArtistJoinExp> artist = ObjectSelect.query(ArtistJoinExp.class)
                .prefetch(ArtistJoinExp.PAINTINGS.disjoint())
                .prefetch(ArtistJoinExp.PAINTINGS
                        .dot(PaintingJoinExp.PAINTING_INFO)
                        .disjoint())
                .where(ExpressionFactory.matchDbExp("ARTIST_ID", 1))
                .select(context);
        assertNotNull(artist);
        queryInterceptor.runWithQueriesBlocked(() -> assertNotNull(artist.get(0)
                .getPaintings().get(0)
                .getPaintingInfo()));
    }

    @Test
    public void testSelectQueryWithDisjointCompoundPrefetch() {
        SelectQuery<ArtistJoinExp> artistSelectQuery = new SelectQuery<>(ArtistJoinExp.class);
        artistSelectQuery.addPrefetch(ArtistJoinExp.PAINTINGS.disjoint());
        artistSelectQuery.addPrefetch(ArtistJoinExp.PAINTINGS
                .dot(PaintingJoinExp.PAINTING_INFO)
                .disjoint());
        artistSelectQuery.setQualifier(ExpressionFactory.matchDbExp("ARTIST_ID", 1));

        ArtistJoinExp artist = artistSelectQuery.selectOne(context);

        assertNotNull(artist);
        queryInterceptor.runWithQueriesBlocked(() -> assertNotNull(artist
                .getPaintings().get(0)
                .getPaintingInfo()));
    }

    @Test
    public void testCompoundPrefetchDisjointById() {
        List<ArtistJoinExp> artist = ObjectSelect.query(ArtistJoinExp.class)
                .prefetch(ArtistJoinExp.PAINTINGS.disjointById())
                .prefetch(ArtistJoinExp.PAINTINGS
                        .dot(PaintingJoinExp.PAINTING_INFO)
                        .disjointById())
                .where(ExpressionFactory.matchDbExp("ARTIST_ID", 1))
                .select(context);
        assertNotNull(artist);
        queryInterceptor.runWithQueriesBlocked(() -> assertNotNull(artist.get(0)
                .getPaintings().get(0)
                .getPaintingInfo()));
    }

    @Test
    public void testCompoundPrefetchJoint() {
        List<ArtistJoinExp> artist = ObjectSelect.query(ArtistJoinExp.class)
                .prefetch(ArtistJoinExp.PAINTINGS.joint())
                .prefetch(ArtistJoinExp.PAINTINGS
                        .dot(PaintingJoinExp.PAINTING_INFO)
                        .joint())
                .where(ExpressionFactory.matchDbExp("ARTIST_ID", 1))
                .select(context);
        assertNotNull(artist);
        queryInterceptor.runWithQueriesBlocked(() -> assertNotNull(artist.get(0)
                .getPaintings().get(0)
                .getPaintingInfo()));
    }

    @Test(expected = CayenneRuntimeException.class)
    public void testWritePropertyDirectly() {
        PaintingJoinExp painting = ObjectSelect.query(PaintingJoinExp.class)
                .where(ExpressionFactory.matchDbExp("PAINTING_ID", 21))
                .selectOne(context);
        assertNotNull(painting);

        PaintingInfoJoinExp info = ObjectSelect.query(PaintingInfoJoinExp.class)
                .where(ExpressionFactory.matchDbExp("INFO_ID", 22))
                .selectOne(context);
        assertNotNull(info);

        info.writeProperty("toPainting", painting);
        info.setPersistenceState(PersistenceState.MODIFIED);
        context.commitChanges();
    }

    @Test(expected = CayenneRuntimeException.class)
    public void testCreateEntitiesWithRels() {
        PaintingJoinExp painting = context.newObject(PaintingJoinExp.class);
        painting.writeProperty("paintingName", "test");

        PaintingInfoJoinExp info = context.newObject(PaintingInfoJoinExp.class);
        info.writeProperty("info", "test1");

        info.writeProperty("toPainting", painting);
        info.setPersistenceState(PersistenceState.NEW);
        painting.setPersistenceState(PersistenceState.NEW);
        context.commitChanges();
    }
}
