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
import java.sql.Types;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.reflect.PersistentDescriptor;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.CompoundPainting;
import org.apache.cayenne.testdo.testmap.CompoundPaintingLongNames;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextFlattenedAttributesTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("PAINTING1");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");
        dbHelper.deleteAll("GALLERY");
    }

    private void createTestDataSet() throws Exception {
        TableHelper tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");

        TableHelper tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns(
                "PAINTING_ID",
                "PAINTING_TITLE",
                "ARTIST_ID",
                "ESTIMATED_PRICE",
                "GALLERY_ID").setColumnTypes(
                Types.INTEGER,
                Types.VARCHAR,
                Types.BIGINT,
                Types.DECIMAL,
                Types.INTEGER);

        TableHelper tPaintingInfo = new TableHelper(dbHelper, "PAINTING_INFO");
        tPaintingInfo.setColumns("PAINTING_ID", "TEXT_REVIEW");

        TableHelper tGallery = new TableHelper(dbHelper, "GALLERY");
        tGallery.setColumns("GALLERY_ID", "GALLERY_NAME");

        long dateBase = System.currentTimeMillis();
        for (int i = 1; i <= 4; i++) {
            tArtist.insert(i + 1, "artist" + i, new java.sql.Date(dateBase
                    + 1000
                    * 60
                    * 60
                    * 24
                    * i));
        }

        for (int i = 1; i <= 2; i++) {
            tGallery.insert(i + 2, "gallery" + i);
        }

        for (int i = 1; i <= 8; i++) {

            Integer galleryId = (i == 3) ? null : (i - 1) % 2 + 3;
            tPainting.insert(
                    i,
                    "painting" + i,
                    (i - 1) % 4 + 2,
                    new BigDecimal(1000d),
                    galleryId);

            tPaintingInfo.insert(i, "painting review" + i);
        }

    }

    public void testSelectCompound1() throws Exception {
        createTestDataSet();
        SelectQuery query = new SelectQuery(CompoundPainting.class);
        List<?> objects = context.performQuery(query);

        assertNotNull(objects);
        assertEquals(8, objects.size());
        assertTrue(
                "CompoundPainting expected, got " + objects.get(0).getClass(),
                objects.get(0) instanceof CompoundPainting);

        for (Iterator<?> i = objects.iterator(); i.hasNext();) {
            CompoundPainting painting = (CompoundPainting) i.next();
            Number id = (Number) painting
                    .getObjectId()
                    .getIdSnapshot()
                    .get("PAINTING_ID");
            assertEquals(
                    "CompoundPainting.getPaintingTitle(): " + painting.getPaintingTitle(),
                    "painting" + id,
                    painting.getPaintingTitle());
            if (painting.getToPaintingInfo() == null) {
                assertNull(painting.getTextReview());
            }
            else {
                assertEquals(
                        "CompoundPainting.getTextReview(): " + painting.getTextReview(),
                        "painting review" + id,
                        painting.getTextReview());
            }
            assertEquals(
                    "CompoundPainting.getArtistName(): " + painting.getArtistName(),
                    painting.getToArtist().getArtistName(),
                    painting.getArtistName());
            if (painting.getToGallery() == null) {
                assertNull(painting.getGalleryName());
            }
            else {
                assertEquals(
                        "CompoundPainting.getGalleryName(): " + painting.getGalleryName(),
                        painting.getToGallery().getGalleryName(),
                        painting.getGalleryName());
            }
        }
    }

    // TODO: andrus 1/5/2007 - CAY-952: SelectQuery uses INNER JOIN for flattened
    // attributes, while
    // EJBQLQuery does an OUTER JOIN... which seems like a better idea...
    // 14/01/2010 now it uses LEFT JOIN
    public void testSelectCompound2() throws Exception {
        createTestDataSet();
        SelectQuery query = new SelectQuery(
                CompoundPainting.class,
                ExpressionFactory.matchExp("artistName", "artist2"));
        List<?> objects = context.performQuery(query);

        assertNotNull(objects);
        assertEquals(2, objects.size());
        assertTrue(
                "CompoundPainting expected, got " + objects.get(0).getClass(),
                objects.get(0) instanceof CompoundPainting);

        for (Iterator<?> i = objects.iterator(); i.hasNext();) {
            CompoundPainting painting = (CompoundPainting) i.next();
            assertEquals(PersistenceState.COMMITTED, painting.getPersistenceState());

            assertEquals(
                    "CompoundPainting.getArtistName(): " + painting.getArtistName(),
                    "artist2",
                    painting.getArtistName());
            assertEquals(
                    "CompoundPainting.getArtistName(): " + painting.getGalleryName(),
                    painting.getToGallery().getGalleryName(),
                    painting.getGalleryName());
        }
    }

    /**
     * Emulates the situation when flattened attribute has unusual(long) name, that puts
     * this attribute property to the top of PersistentDescriptor.declaredProperties map,
     * {@link PersistentDescriptor}[105] That forced an error during the building of the
     * SelectQuery statement, CAY-1484
     */
    public void testSelectCompoundLongNames() throws Exception {
        createTestDataSet();
        SelectQuery query = new SelectQuery(CompoundPaintingLongNames.class);
        // the error was thrown on query execution
        List<?> objects = context.performQuery(query);
        assertNotNull(objects);
    }

    public void testSelectEJQBQL() throws Exception {
        createTestDataSet();
        EJBQLQuery query = new EJBQLQuery(
                "SELECT a FROM CompoundPainting a WHERE a.artistName = 'artist2'");
        List<?> objects = context.performQuery(query);

        assertNotNull(objects);
        assertEquals(2, objects.size());
        assertTrue(
                "CompoundPainting expected, got " + objects.get(0).getClass(),
                objects.get(0) instanceof CompoundPainting);
        Iterator<?> i = objects.iterator();
        while (i.hasNext()) {
            CompoundPainting painting = (CompoundPainting) i.next();
            assertEquals(PersistenceState.COMMITTED, painting.getPersistenceState());
        }
    }

    public void testSelectEJQBQLCollectionTheta() throws Exception {
        createTestDataSet();
        EJBQLQuery query = new EJBQLQuery(
                "SELECT DISTINCT a FROM CompoundPainting cp, Artist a "
                        + "WHERE a.artistName=cp.artistName ORDER BY a.artistName");

        List<?> objects = context.performQuery(query);

        assertNotNull(objects);
        assertEquals(4, objects.size());
        Iterator<?> i = objects.iterator();
        int index = 1;
        while (i.hasNext()) {
            Artist artist = (Artist) i.next();
            assertEquals("artist" + index, artist.getArtistName());
            index++;
        }
    }

    public void testSelectEJQBQLLike() throws Exception {
        createTestDataSet();
        EJBQLQuery query = new EJBQLQuery(
                "SELECT a FROM CompoundPainting a WHERE a.artistName LIKE 'artist%' "
                        + "ORDER BY a.paintingTitle");

        List<?> objects = context.performQuery(query);

        assertNotNull(objects);
        assertEquals(8, objects.size());
        Iterator<?> i = objects.iterator();
        int index = 1;
        while (i.hasNext()) {
            CompoundPainting painting = (CompoundPainting) i.next();
            assertEquals("painting" + index, painting.getPaintingTitle());
            index++;
        }
    }

    public void testSelectEJQBQLBetween() throws Exception {
        createTestDataSet();
        EJBQLQuery query = new EJBQLQuery("SELECT a FROM CompoundPainting a "
                + "WHERE a.artistName BETWEEN 'artist1' AND 'artist4' "
                + "ORDER BY a.paintingTitle");

        List<?> objects = context.performQuery(query);

        assertNotNull(objects);
        assertEquals(8, objects.size());
        Iterator<?> i = objects.iterator();
        int index = 1;
        while (i.hasNext()) {
            CompoundPainting painting = (CompoundPainting) i.next();
            assertEquals("painting" + index, painting.getPaintingTitle());
            index++;
        }
    }

    public void testSelectEJQBQLSubquery() throws Exception {
        createTestDataSet();
        EJBQLQuery query = new EJBQLQuery(
                "SELECT g FROM Gallery g WHERE "
                        + "(SELECT COUNT(cp) FROM CompoundPainting cp WHERE g.galleryName=cp.galleryName) = 4");

        List<?> objects = context.performQuery(query);

        assertNotNull(objects);
        assertEquals(1, objects.size());
        Gallery gallery = (Gallery) objects.get(0);
        assertEquals("gallery2", gallery.getGalleryName());

    }

    public void testSelectEJQBQLHaving() throws Exception {
        createTestDataSet();
        EJBQLQuery query = new EJBQLQuery(
                "SELECT cp.galleryName, COUNT(a) from  Artist a, CompoundPainting cp "
                        + "WHERE cp.artistName = a.artistName "
                        + "GROUP BY cp.galleryName "
                        + "HAVING cp.galleryName LIKE 'gallery1'");

        List<Object[]> objects = context.performQuery(query);

        assertNotNull(objects);
        assertEquals(1, objects.size());
        Object[] galleryItem = objects.get(0);
        assertEquals("gallery1", galleryItem[0]);
        assertEquals(3L, galleryItem[1]);
    }

    public void testInsert() {
        CompoundPainting o1 = context.newObject(CompoundPainting.class);
        o1.setArtistName("A1");
        o1.setEstimatedPrice(new BigDecimal(1.0d));
        o1.setGalleryName("G1");
        o1.setPaintingTitle("P1");
        o1.setTextReview("T1");

        context.commitChanges();

        Number artistCount = (Number) Cayenne.objectForQuery(context, new EJBQLQuery(
                "select count(a) from Artist a"));
        assertEquals(1, artistCount.intValue());
        Number paintingCount = (Number) Cayenne.objectForQuery(context, new EJBQLQuery(
                "select count(a) from Painting a"));
        assertEquals(1, paintingCount.intValue());

        Number galleryCount = (Number) Cayenne.objectForQuery(context, new EJBQLQuery(
                "select count(a) from Gallery a"));
        assertEquals(1, galleryCount.intValue());
    }

    public void testDelete() throws Exception {
        // throw in a bit of random overlapping data, to make sure FK/PK correspondence is
        // not purely coincidental
        Artist a = context.newObject(Artist.class);
        a.setArtistName("AX");
        context.commitChanges();

        CompoundPainting o1 = context.newObject(CompoundPainting.class);
        o1.setArtistName("A1");
        o1.setEstimatedPrice(new BigDecimal(1.0d));
        o1.setGalleryName("G1");
        o1.setPaintingTitle("P1");
        o1.setTextReview("T1");

        context.commitChanges();

        context.deleteObjects(o1);
        context.commitChanges();

        Number artistCount = (Number) Cayenne.objectForQuery(context, new EJBQLQuery(
                "select count(a) from Artist a"));
        assertEquals(1, artistCount.intValue());
        Number paintingCount = (Number) Cayenne.objectForQuery(context, new EJBQLQuery(
                "select count(a) from Painting a"));
        assertEquals(0, paintingCount.intValue());

        Number galleryCount = (Number) Cayenne.objectForQuery(context, new EJBQLQuery(
                "select count(a) from Gallery a"));
        assertEquals(0, galleryCount.intValue());
    }

    public void testUpdate() {
        CompoundPainting o1 = context.newObject(CompoundPainting.class);
        o1.setArtistName("A1");
        o1.setEstimatedPrice(new BigDecimal(1d));
        o1.setGalleryName("G1");
        o1.setPaintingTitle("P1");
        o1.setTextReview("T1");

        context.commitChanges();

        o1.setArtistName("X1");
        o1.setEstimatedPrice(new BigDecimal(2d));
        o1.setGalleryName("X1");
        o1.setPaintingTitle("X1");
        o1.setTextReview("X1");

        context.commitChanges();
    }
}
