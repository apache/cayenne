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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.art.Artist;
import org.apache.art.ArtistExhibit;
import org.apache.art.Gallery;
import org.apache.art.Painting;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextEJBQLFetchJoinTest extends CayenneCase {
    public static final String INSERT_ARTIST = "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (?,?)";
    public static final String INSERT_PAINTING = "INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ARTIST_ID, ESTIMATED_PRICE) VALUES (?, ?, ?, ?)";
    public static final String INSERT_GALLERY = "INSERT INTO GALLERY (GALLERY_ID, GALLERY_NAME) VALUES (?,?)";
    public static final String INSERT_EXIBIT = "INSERT INTO EXHIBIT (EXHIBIT_ID, GALLERY_ID, CLOSING_DATE, OPENING_DATE) VALUES (?, ?, ?, ?)";
    public static final String INSERT_ARTIST_EXIBIT = "INSERT INTO ARTIST_EXHIBIT (ARTIST_ID, EXHIBIT_ID) VALUES (?, ?)";

    Connection conn; 
    @Override
    protected void setUp() throws Exception {
        deleteTestData();
    }

    private void populateTables(TestData data) throws Exception {
                
        Object[][] artistsData = {
                {1, "A1"},
                {2, "A2"},
                {3, "A3"}
        };
        Object[][] paintingsData = {
                {1, "P11", 1, new BigDecimal(3000d)},
                {2, "P2", 2, new BigDecimal(5000d)},
                {3, "P12", 1, new BigDecimal(3000d)}
        };
        Object[][] galleriesData = {
                {1, "gallery1"},
                {2, "gallery2"}
        };
        Object[][] exibitsData = {
                {1, 1, new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 1), new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 2)},
                {2, 1, new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 3), new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 4)}
        };
        Object[][] artistExibitsData = {
                {1, 1},
                {1, 2}
        };
        conn = getConnection();
        try {
            conn.setAutoCommit(false);

            switch (data) {
                case ONE_ENTITY_FETCH_JOIN:
                    insertArtists(artistsData);
                    insertPaintings(paintingsData);
                    break;
                case SEVERAL_FETCH_JOINS:
                    insertArtists(artistsData);
                    insertPaintings(paintingsData);
                    insertGalleries(galleriesData);
                    insertExibits(exibitsData);
                    insertArtistExibits(artistExibitsData);
                    break;
            }

        }
        finally {
            conn.close();
        }

    }

    
    public void testFetchJoinForOneEntity() throws Exception {
        populateTables(TestData.ONE_ENTITY_FETCH_JOIN);
        String ejbql = "SELECT a FROM Artist a JOIN FETCH a.paintingArray ";

        EJBQLQuery query = new EJBQLQuery(ejbql);

        DataContext context = createDataContext();

        List objects = context.performQuery(query);

        blockQueries();
        try {
            assertEquals(2, objects.size());

            Iterator it = objects.iterator();
            while (it.hasNext()) {
                Artist a = (Artist) it.next();
                List list = a.getPaintingArray();

                assertNotNull(list);
                assertFalse(((ValueHolder) list).isFault());

                Iterator children = list.iterator();
                while (children.hasNext()) {
                    Painting p = (Painting) children.next();
                    assertEquals(PersistenceState.COMMITTED, p.getPersistenceState());
                    // make sure properties are not null..
                    assertNotNull(p.getPaintingTitle());
                }
            }
        }
        finally {
            unblockQueries();
        }
    }

    public void testSeveralFetchJoins() throws Exception {
        populateTables(TestData.SEVERAL_FETCH_JOINS);
        String ejbql = "SELECT a "
                + "FROM Artist a JOIN FETCH a.paintingArray JOIN FETCH a.artistExhibitArray "
                + "WHERE a.artistName = 'A1'";

        EJBQLQuery query = new EJBQLQuery(ejbql);

        DataContext context = createDataContext();

        List objects = context.performQuery(query);

        blockQueries();
        try {
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
        finally {
            unblockQueries();
        }
    }

    public void testSeveralEntitiesFetchJoins() throws Exception {
        populateTables(TestData.SEVERAL_FETCH_JOINS);
        String ejbql = "SELECT DISTINCT a , g "
                + "FROM Artist a JOIN FETCH a.paintingArray , Gallery g JOIN FETCH g.exhibitArray "
                + "WHERE a.artistName='A1' AND g.galleryName='gallery1'";

        EJBQLQuery query = new EJBQLQuery(ejbql);

        DataContext context = createDataContext();

        List objects = context.performQuery(query);

        blockQueries();
        try {
            assertNotNull(objects);
            assertFalse(objects.isEmpty());
            assertEquals(1, objects.size());
        }
        finally {
            unblockQueries();
        }
    }

    public void testSeveralEntitiesAndScalarFetchInnerJoins() throws Exception {
        populateTables(TestData.SEVERAL_FETCH_JOINS);
        String ejbql = "SELECT DISTINCT a, a.artistName , g "
                + "FROM Artist a JOIN FETCH a.paintingArray, Gallery g JOIN FETCH g.exhibitArray "
                + "ORDER BY a.artistName";

        EJBQLQuery query = new EJBQLQuery(ejbql);

        DataContext context = createDataContext();

        List objects = context.performQuery(query);

        blockQueries();
        try {
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

            List exibits = g1.getExhibitArray();

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
        finally {
            unblockQueries();
        }
    }

    public void testSeveralEntitiesAndScalarFetchOuterJoins() throws Exception {
        populateTables(TestData.SEVERAL_FETCH_JOINS);
        String ejbql = "SELECT DISTINCT a, a.artistName , g "
                + "FROM Artist a LEFT JOIN FETCH a.paintingArray, Gallery g LEFT JOIN FETCH g.exhibitArray "
                + "ORDER BY a.artistName, g.galleryName";

        EJBQLQuery query = new EJBQLQuery(ejbql);

        DataContext context = createDataContext();

        List objects = context.performQuery(query);

        blockQueries();
        try {
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

            List exibits = g1.getExhibitArray();

            assertNotNull(exibits);
            assertFalse(((ValueHolder) exibits).isFault());
            assertEquals(2, exibits.size());

            row = (Object[]) objects.get(1);

            assertEquals(a1, (Artist) row[0]);
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

            assertEquals(a2, (Artist) row[0]);
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

            assertEquals(a3, (Artist) row[0]);
            assertEquals(artistName3, row[1]);
            assertEquals(g2, row[2]);
        }
        finally {
            unblockQueries();
        }
    }

    private void insertArtists(Object[][] artistsData) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(INSERT_ARTIST);
        for (int i = 0; i < artistsData.length; i++) {
            stmt.setInt(1, (Integer)artistsData[i][0]);
            stmt.setString(2, (String) artistsData[i][1]);
            stmt.executeUpdate();
        }
        stmt.close();
    }

    private void insertPaintings(Object[][] paintingsData) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(INSERT_PAINTING);
        for (int i = 0; i < paintingsData.length; i++) {
            stmt.setInt(1, (Integer)paintingsData[i][0]);
            stmt.setString(2, (String) paintingsData[i][1]);
            stmt.setInt(3, (Integer)paintingsData[i][2]);
            stmt.setBigDecimal(4, (BigDecimal) paintingsData[i][3]);
            stmt.executeUpdate();
        }
        stmt.close();
    }
    
    private void insertGalleries(Object[][] galleriesData) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(INSERT_GALLERY);
        for (int i = 0; i < galleriesData.length; i++) {
            stmt.setInt(1, (Integer)galleriesData[i][0]);
            stmt.setString(2, (String) galleriesData[i][1]);
            stmt.executeUpdate();
        }
        stmt.close();
    }
    
    private void insertExibits(Object[][] exibitsData) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(INSERT_EXIBIT);
        for (int i = 0; i < exibitsData.length; i++) {
            stmt.setInt(1, (Integer)exibitsData[i][0]);
            stmt.setInt(2, (Integer) exibitsData[i][1]);
            stmt.setDate(3, (Date)exibitsData[i][2]);
            stmt.setDate(4, (Date)exibitsData[i][3]);
            stmt.executeUpdate();
        }
        stmt.close();
    }
    
    private void insertArtistExibits(Object[][] artistExibitsData) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(INSERT_ARTIST_EXIBIT);
        for (int i = 0; i < artistExibitsData.length; i++) {
            stmt.setInt(1, (Integer)artistExibitsData[i][0]);
            stmt.setInt(2, (Integer) artistExibitsData[i][1]);
            stmt.executeUpdate();
        }
        stmt.close();
    }
    
    enum TestData{
        ONE_ENTITY_FETCH_JOIN, SEVERAL_FETCH_JOINS
    }
}
