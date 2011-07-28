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

import java.sql.Date;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.ArtistExhibit;
import org.apache.cayenne.testdo.testmap.Exhibit;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextEJBQLFetchJoinTest extends ServerCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected DataChannelInterceptor queryBlocker;

    protected TableHelper tArtist;
    protected TableHelper tPainting;
    protected TableHelper tGallery;
    protected TableHelper tExhibit;
    protected TableHelper tArtistExhibit;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");
        dbHelper.deleteAll("EXHIBIT");
        dbHelper.deleteAll("GALLERY");

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

        tGallery = new TableHelper(dbHelper, "GALLERY");
        tGallery.setColumns("GALLERY_ID", "GALLERY_NAME");

        tExhibit = new TableHelper(dbHelper, "EXHIBIT");
        tExhibit.setColumns("EXHIBIT_ID", "GALLERY_ID", "CLOSING_DATE", "OPENING_DATE");

        tArtistExhibit = new TableHelper(dbHelper, "ARTIST_EXHIBIT");
        tArtistExhibit.setColumns("ARTIST_ID", "EXHIBIT_ID");
    }

    protected void createOneFetchJoinDataSet() throws Exception {
        tArtist.insert(1, "A1");
        tArtist.insert(2, "A2");
        tArtist.insert(3, "A3");

        tPainting.insert(1, 1, "P11", 3000d);
        tPainting.insert(2, 2, "P2", 5000d);
        tPainting.insert(3, 1, "P12", 3000d);
    }

    protected void createMultipleFetchJoinsDataSet() throws Exception {
        createOneFetchJoinDataSet();

        tGallery.insert(1, "gallery1");
        tGallery.insert(2, "gallery2");

        long t = System.currentTimeMillis();

        tExhibit.insert(1, 1, new Date(1 + 10000), new Date(t + 20000));
        tExhibit.insert(2, 1, new Date(1 + 30000), new Date(t + 40000));

        tArtistExhibit.insert(1, 1);
        tArtistExhibit.insert(1, 2);
    }

    public void testFetchJoinForOneEntity() throws Exception {
        createOneFetchJoinDataSet();

        String ejbql = "SELECT a FROM Artist a JOIN FETCH a.paintingArray ";

        EJBQLQuery query = new EJBQLQuery(ejbql);

        final List<?> objects = context.performQuery(query);

        queryBlocker.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {

                assertEquals(2, objects.size());

                Iterator<?> it = objects.iterator();
                while (it.hasNext()) {
                    Artist a = (Artist) it.next();
                    List<Painting> list = a.getPaintingArray();

                    assertNotNull(list);
                    assertFalse(((ValueHolder) list).isFault());

                    for (Painting p : list) {
                        assertEquals(PersistenceState.COMMITTED, p.getPersistenceState());
                        // make sure properties are not null..
                        assertNotNull(p.getPaintingTitle());
                    }
                }
            }
        });
    }

    public void testSeveralFetchJoins() throws Exception {
        createMultipleFetchJoinsDataSet();

        String ejbql = "SELECT a "
                + "FROM Artist a JOIN FETCH a.paintingArray JOIN FETCH a.artistExhibitArray "
                + "WHERE a.artistName = 'A1'";

        EJBQLQuery query = new EJBQLQuery(ejbql);

        final List<?> objects = context.performQuery(query);

        queryBlocker.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {

                assertEquals(1, objects.size());

                Artist a = (Artist) objects.get(0);
                assertEquals("A1", a.getArtistName());

                List<Painting> paintings = a.getPaintingArray();

                assertNotNull(paintings);
                assertFalse(((ValueHolder) paintings).isFault());
                assertEquals(2, paintings.size());

                List<String> expectedPaintingsNames = new ArrayList<String>();
                expectedPaintingsNames.add("P11");
                expectedPaintingsNames.add("P12");

                Iterator<Painting> paintingsIterator = paintings.iterator();
                while (paintingsIterator.hasNext()) {
                    Painting p = paintingsIterator.next();
                    assertEquals(PersistenceState.COMMITTED, p.getPersistenceState());
                    assertNotNull(p.getPaintingTitle());
                    assertTrue(expectedPaintingsNames.contains(p.getPaintingTitle()));
                }

                List<ArtistExhibit> exibits = a.getArtistExhibitArray();

                assertNotNull(exibits);
                assertFalse(((ValueHolder) exibits).isFault());
                assertEquals(2, exibits.size());

                Iterator<ArtistExhibit> exibitsIterator = exibits.iterator();
                while (exibitsIterator.hasNext()) {
                    ArtistExhibit ae = exibitsIterator.next();
                    assertEquals(PersistenceState.COMMITTED, ae.getPersistenceState());
                    assertNotNull(ae.getObjectId());

                }
            }
        });
    }

    public void testSeveralEntitiesFetchJoins() throws Exception {
        createMultipleFetchJoinsDataSet();

        String ejbql = "SELECT DISTINCT a , g "
                + "FROM Artist a JOIN FETCH a.paintingArray , Gallery g JOIN FETCH g.exhibitArray "
                + "WHERE a.artistName='A1' AND g.galleryName='gallery1'";

        EJBQLQuery query = new EJBQLQuery(ejbql);

        final List<?> objects = context.performQuery(query);

        queryBlocker.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {

                assertNotNull(objects);
                assertFalse(objects.isEmpty());
                assertEquals(1, objects.size());
            }
        });
    }

    public void testSeveralEntitiesAndScalarFetchInnerJoins() throws Exception {
        createMultipleFetchJoinsDataSet();

        String ejbql = "SELECT DISTINCT a, a.artistName , g "
                + "FROM Artist a JOIN FETCH a.paintingArray, Gallery g JOIN FETCH g.exhibitArray "
                + "ORDER BY a.artistName";

        EJBQLQuery query = new EJBQLQuery(ejbql);

        final List<?> objects = context.performQuery(query);

        queryBlocker.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {

                assertEquals(2, objects.size());

                Object[] firstRow = (Object[]) objects.get(0);
                Artist a = (Artist) firstRow[0];
                assertEquals("A1", a.getArtistName());

                List<Painting> paintings = a.getPaintingArray();

                assertNotNull(paintings);
                assertFalse(((ValueHolder) paintings).isFault());
                assertEquals(2, paintings.size());

                List<String> expectedPaintingsNames = new ArrayList<String>();
                expectedPaintingsNames.add("P11");
                expectedPaintingsNames.add("P12");

                Iterator<Painting> paintingsIterator = paintings.iterator();
                while (paintingsIterator.hasNext()) {
                    Painting p = paintingsIterator.next();
                    assertEquals(PersistenceState.COMMITTED, p.getPersistenceState());
                    assertNotNull(p.getPaintingTitle());
                    assertTrue(expectedPaintingsNames.contains(p.getPaintingTitle()));
                }
                String artistName = (String) firstRow[1];
                assertEquals("A1", artistName);

                Gallery g1 = (Gallery) firstRow[2];
                assertEquals("gallery1", g1.getGalleryName());

                List<Exhibit> exibits = g1.getExhibitArray();

                assertNotNull(exibits);
                assertFalse(((ValueHolder) exibits).isFault());
                assertEquals(2, exibits.size());

                Object[] secondRow = (Object[]) objects.get(1);
                a = (Artist) secondRow[0];
                assertEquals("A2", a.getArtistName());

                paintings = a.getPaintingArray();

                assertNotNull(paintings);
                assertFalse(((ValueHolder) paintings).isFault());
                assertEquals(1, paintings.size());

                expectedPaintingsNames = new ArrayList<String>();
                expectedPaintingsNames.add("P2");

                paintingsIterator = paintings.iterator();
                while (paintingsIterator.hasNext()) {
                    Painting p = paintingsIterator.next();
                    assertEquals(PersistenceState.COMMITTED, p.getPersistenceState());
                    assertNotNull(p.getPaintingTitle());
                    assertTrue(expectedPaintingsNames.contains(p.getPaintingTitle()));
                }
                artistName = (String) secondRow[1];
                assertEquals("A2", artistName);

                Gallery g2 = (Gallery) secondRow[2];
                assertEquals(g1, g2);
            }
        });
    }

    public void testSeveralEntitiesAndScalarFetchOuterJoins() throws Exception {
        createMultipleFetchJoinsDataSet();

        String ejbql = "SELECT DISTINCT a, a.artistName , g "
                + "FROM Artist a LEFT JOIN FETCH a.paintingArray, Gallery g LEFT JOIN FETCH g.exhibitArray "
                + "ORDER BY a.artistName, g.galleryName";

        EJBQLQuery query = new EJBQLQuery(ejbql);

        final List<?> objects = context.performQuery(query);
        queryBlocker.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {

                assertEquals(6, objects.size());

                Object[] row = (Object[]) objects.get(0);
                Artist a1 = (Artist) row[0];
                assertEquals("A1", a1.getArtistName());

                List<Painting> paintings = a1.getPaintingArray();

                assertNotNull(paintings);
                assertFalse(((ValueHolder) paintings).isFault());
                assertEquals(2, paintings.size());

                List<String> expectedPaintingsNames = new ArrayList<String>();
                expectedPaintingsNames.add("P11");
                expectedPaintingsNames.add("P12");

                Iterator<Painting> paintingsIterator = paintings.iterator();
                while (paintingsIterator.hasNext()) {
                    Painting p = paintingsIterator.next();
                    assertEquals(PersistenceState.COMMITTED, p.getPersistenceState());
                    assertNotNull(p.getPaintingTitle());
                    assertTrue(expectedPaintingsNames.contains(p.getPaintingTitle()));
                }
                String artistName1 = (String) row[1];
                assertEquals("A1", artistName1);

                Gallery g1 = (Gallery) row[2];
                assertEquals("gallery1", g1.getGalleryName());

                List<?> exibits = g1.getExhibitArray();

                assertNotNull(exibits);
                assertFalse(((ValueHolder) exibits).isFault());
                assertEquals(2, exibits.size());

                row = (Object[]) objects.get(1);

                assertEquals(a1, row[0]);
                assertEquals(artistName1, row[1]);

                Gallery g2 = (Gallery) row[2];
                assertEquals("gallery2", g2.getGalleryName());

                exibits = g2.getExhibitArray();

                assertTrue(exibits.isEmpty());

                row = (Object[]) objects.get(2);

                Artist a2 = (Artist) row[0];
                assertEquals("A2", a2.getArtistName());

                paintings = a2.getPaintingArray();

                assertNotNull(paintings);
                assertEquals(1, paintings.size());

                Painting p = paintings.get(0);
                assertEquals(PersistenceState.COMMITTED, p.getPersistenceState());
                assertNotNull(p.getPaintingTitle());
                assertEquals("P2", p.getPaintingTitle());

                String artistName2 = (String) row[1];
                assertEquals("A2", artistName2);
                assertEquals(g1, row[2]);

                row = (Object[]) objects.get(3);

                assertEquals(a2, row[0]);
                assertEquals(artistName2, row[1]);
                assertEquals(g2, row[2]);

                row = (Object[]) objects.get(4);

                Artist a3 = (Artist) row[0];
                assertEquals("A3", a3.getArtistName());

                paintings = a3.getPaintingArray();

                assertTrue(paintings.isEmpty());

                String artistName3 = (String) row[1];
                assertEquals("A3", artistName3);
                assertEquals(g1, row[2]);

                row = (Object[]) objects.get(5);

                assertEquals(a3, row[0]);
                assertEquals(artistName3, row[1]);
                assertEquals(g2, row[2]);
            }
        });
    }
}
