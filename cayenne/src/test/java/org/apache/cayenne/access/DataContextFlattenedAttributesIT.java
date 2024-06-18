/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.access.translator.select.DefaultSelectTranslator;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DefaultEntityResultSegment;
import org.apache.cayenne.query.ColumnSelect;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SelectById;
import org.apache.cayenne.reflect.PersistentDescriptor;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.CompoundPainting;
import org.apache.cayenne.testdo.testmap.CompoundPaintingLongNames;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.PaintingInfo;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextFlattenedAttributesIT extends RuntimeCase {

    @Inject
    private CayenneRuntime runtime;

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tArtist;
    private TableHelper tPainting;
    private TableHelper tPaintingInfo;
    private TableHelper tGallery;

    @Before
    public void createTestDataSetStructure() {
        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");

        tPainting = new TableHelper(dbHelper, "PAINTING");
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

        tPaintingInfo = new TableHelper(dbHelper, "PAINTING_INFO");
        tPaintingInfo.setColumns("PAINTING_ID", "TEXT_REVIEW");

        tGallery = new TableHelper(dbHelper, "GALLERY");
        tGallery.setColumns("GALLERY_ID", "GALLERY_NAME");
    }

    private void createTestDataSet() throws Exception {
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

    @Test
    public void testSelectCompound1() throws Exception {
        createTestDataSet();
        List<CompoundPainting> objects = ObjectSelect.query(CompoundPainting.class).select(context);

        assertNotNull(objects);
        assertEquals(8, objects.size());
        assertNotNull("CompoundPainting expected, got null", objects.get(0));

        for (CompoundPainting painting : objects) {
            Number id = (Number) painting.getObjectId().getIdSnapshot().get("PAINTING_ID");
            assertEquals(
                    "CompoundPainting.getPaintingTitle(): " + painting.getPaintingTitle(),
                    "painting" + id,
                    painting.getPaintingTitle());
            if (painting.getToPaintingInfo() == null) {
                assertNull(painting.getTextReview());
            } else {
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
            } else {
                assertEquals(
                        "CompoundPainting.getGalleryName(): " + painting.getGalleryName(),
                        painting.getToGallery().getGalleryName(),
                        painting.getGalleryName());
            }
        }
    }

    // TODO: andrus 1/5/2007 - CAY-952: SelectQuery uses INNER JOIN for flattened
    // attributes, while EJBQLQuery does an OUTER JOIN... which seems like a better idea...
    // 14/01/2010 now it uses LEFT JOIN
    @Test
    public void testSelectCompound2() throws Exception {
        createTestDataSet();

        List<CompoundPainting> objects = ObjectSelect.query(CompoundPainting.class, CompoundPainting.ARTIST_NAME.eq("artist2"))
                .select(context);

        assertNotNull(objects);
        assertEquals(2, objects.size());
        assertTrue("CompoundPainting expected, got null", objects.get(0) != null);

        for (CompoundPainting painting : objects) {
            assertEquals(PersistenceState.COMMITTED, painting.getPersistenceState());

            assertEquals(
                    "CompoundPainting.getArtistName(): " + painting.getArtistName(),
                    "artist2",
                    painting.getArtistName());
            assertEquals(
                    "CompoundPainting.getGalleryName(): " + painting.getGalleryName(),
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
    @Test
    public void testSelectCompoundLongNames() throws Exception {
        createTestDataSet();
        // the error was thrown on query execution
        List<?> objects = ObjectSelect.query(CompoundPaintingLongNames.class).select(context);
        assertNotNull(objects);
    }

    @Test
    public void testColumnQueryWithFlattenedAttribute() throws Exception {
        createTestDataSet();
        ColumnSelect<Object[]> originalQuery = ObjectSelect.query(CompoundPaintingLongNames.class)
                .columns(CompoundPaintingLongNames.SELF);

        DataNode dataNode = context.getParentDataDomain().getDataNodes().iterator().next();
        DefaultSelectTranslator translator =
                new DefaultSelectTranslator(originalQuery, dataNode.getAdapter(), context.getEntityResolver());

        translator.getSql();

        DefaultEntityResultSegment segment = (DefaultEntityResultSegment) originalQuery
                .getMetaData(context.getEntityResolver())
                .getResultSetMapping()
                .get(0);

        assertEquals(12, segment.getFields().size());
        assertEquals(12, translator.getResultColumns().length);
        assertEquals(segment.getFields().size(), translator.getResultColumns().length);
    }

    @Test
    public void testSelectColumnQuery() throws Exception {
        createTestDataWithDeletion();

        ColumnSelect<CompoundPaintingLongNames> originalQuery = ObjectSelect.query(CompoundPaintingLongNames.class)
                .column(CompoundPaintingLongNames.SELF);

        CompoundPaintingLongNames beforeCompoundPainting = originalQuery
                .where(CompoundPaintingLongNames.PAINTING_ID_PK_PROPERTY.eq(1))
                .selectOne(context);

        String beforeArtistNameFromContext = beforeCompoundPainting.getArtistLongName();
        String beforePaintingTitleFromContext = beforeCompoundPainting.getPaintingTitle();

        String beforeArtistNameFromDatabase = (String) tArtist.selectAll().get(0)[1];
        String beforePaintingTitleFromDatabase = (String) tPainting.selectAll().get(0)[1];

        assertNotNull(beforeArtistNameFromDatabase);
        assertNotNull(beforePaintingTitleFromDatabase);

        assertEquals(beforeArtistNameFromDatabase.trim(), beforeArtistNameFromContext);
        assertEquals(beforePaintingTitleFromDatabase.trim(), beforePaintingTitleFromContext);

        beforeCompoundPainting.setArtistLongName("some");
        beforeCompoundPainting.setPaintingTitle("omes");
        context.commitChanges();

        CompoundPaintingLongNames afterCompoundPainting = originalQuery
                .where(CompoundPaintingLongNames.PAINTING_ID_PK_PROPERTY.eq(1))
                .selectOne(context);

        String afterArtistNameFromContext = afterCompoundPainting.getArtistLongName();
        String afterPaintingTitleFromContext = afterCompoundPainting.getPaintingTitle();

        String afterArtistNameFromDatabase = (String) tArtist.selectAll().get(0)[1];
        String afterPaintingTitleFromDatabase = (String) tPainting.selectAll().get(0)[1];

        assertNotNull(afterArtistNameFromDatabase);
        assertNotNull(afterPaintingTitleFromDatabase);

        assertEquals((afterArtistNameFromDatabase).trim(), afterArtistNameFromContext);
        assertEquals((afterPaintingTitleFromDatabase).trim(), afterPaintingTitleFromContext);
    }

    @Test
    public void testObjectSelectQuery() throws Exception {
        createTestDataWithDeletion();

        ObjectSelect<CompoundPaintingLongNames> originalQuery = ObjectSelect.query(CompoundPaintingLongNames.class);

        CompoundPaintingLongNames beforeCompoundPainting = originalQuery
                .where(CompoundPaintingLongNames.PAINTING_ID_PK_PROPERTY.eq(1))
                .selectOne(context);

        String beforeArtistNameFromContext = beforeCompoundPainting.getArtistLongName();
        String beforePaintingTitleFromContext = beforeCompoundPainting.getPaintingTitle();

        String beforeArtistNameFromDatabase = (String) tArtist.selectAll().get(0)[1];
        String beforePaintingTitleFromDatabase = (String) tPainting.selectAll().get(0)[1];

        assertNotNull(beforeArtistNameFromDatabase);
        assertNotNull(beforePaintingTitleFromDatabase);

        assertEquals(beforeArtistNameFromDatabase.trim(), beforeArtistNameFromContext);
        assertEquals(beforePaintingTitleFromDatabase.trim(), beforePaintingTitleFromContext);

        beforeCompoundPainting.setArtistLongName("some");
        beforeCompoundPainting.setPaintingTitle("omes");
        context.commitChanges();

        CompoundPaintingLongNames afterCompoundPainting = originalQuery
                .where(CompoundPaintingLongNames.PAINTING_ID_PK_PROPERTY.eq(1))
                .selectOne(context);

        String afterArtistNameFromContext = afterCompoundPainting.getArtistLongName();
        String afterPaintingTitleFromContext = afterCompoundPainting.getPaintingTitle();

        String afterArtistNameFromDatabase = (String) tArtist.selectAll().get(0)[1];
        String afterPaintingTitleFromDatabase = (String) tPainting.selectAll().get(0)[1];

        assertNotNull(afterArtistNameFromDatabase);
        assertNotNull(afterPaintingTitleFromDatabase);

        assertEquals(afterArtistNameFromDatabase.trim(), afterArtistNameFromContext);
        assertEquals(afterPaintingTitleFromDatabase.trim(), afterPaintingTitleFromContext);
    }

    @Test
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
        for (Object object : objects) {
            CompoundPainting painting = (CompoundPainting) object;
            assertEquals(PersistenceState.COMMITTED, painting.getPersistenceState());
        }
    }

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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
        assertEquals(2, artistCount.intValue());
        Number paintingCount = (Number) Cayenne.objectForQuery(context, new EJBQLQuery(
                "select count(a) from Painting a"));
        assertEquals(0, paintingCount.intValue());

        Number galleryCount = (Number) Cayenne.objectForQuery(context, new EJBQLQuery(
                "select count(a) from Gallery a"));
        assertEquals(1, galleryCount.intValue());
    }

    @Test
    public void testDelete2() throws Exception {
        createTestDataSet();

        long infoCount = ObjectSelect.query(PaintingInfo.class).selectCount(context);
        assertEquals("PaintingInfo", 8, infoCount);

        List<CompoundPainting> objects = ObjectSelect.query(CompoundPainting.class)
                .where(CompoundPainting.ARTIST_NAME.eq("artist2"))
                .select(context);

        // Should have two paintings by the same artist
        assertEquals("Paintings", 2, objects.size());

        CompoundPainting cp0 = objects.get(0);
        CompoundPainting cp1 = objects.get(1);

        // Both paintings are at the same gallery
        assertEquals("Gallery", cp0.getGalleryName(), cp1.getGalleryName());

        context.invalidateObjects(cp0);
        context.deleteObjects(cp1);
        context.commitChanges();

        // Delete should only have deleted the painting and its info,
        // the painting's artist and gallery should not be deleted.

        objects = ObjectSelect.query(CompoundPainting.class)
                .where(CompoundPainting.ARTIST_NAME.eq("artist2"))
                .select(runtime.newContext());
        
        // Should now only have one painting by artist2
        assertEquals("Painting", 1, objects.size());
        // and that painting should have a valid gallery
        assertNotNull("Gallery is null", objects.get(0).getToGallery());
        assertNotNull("GalleryName is null", objects.get(0).getToGallery().getGalleryName());
        
        // There should be one less painting info now
        infoCount = ObjectSelect.query(PaintingInfo.class).selectCount(context);
        assertEquals("PaintingInfo", 7, infoCount);
    }

    @Test
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

    @Test
    public void testUpdateDifferentContext() {
        Object id;
        {
            // insert
            ObjectContext context1 = runtime.newContext();
            CompoundPainting o1 = context1.newObject(CompoundPainting.class);
            o1.setArtistName("A1");
            o1.setEstimatedPrice(BigDecimal.valueOf(1));
            o1.setGalleryName("G1");
            o1.setPaintingTitle("P1");
            o1.setTextReview("T1");

            context1.commitChanges();
            id = Cayenne.pkForObject(o1);
        }

        {
            // read and update
            ObjectContext context2 = runtime.newContext();
            CompoundPainting o2 = SelectById.query(CompoundPainting.class, id).selectFirst(context2);

            o2.setArtistName("AX1");
            o2.setEstimatedPrice(BigDecimal.valueOf(2));
            o2.setGalleryName("XG1");
            o2.setPaintingTitle("PX1");
            o2.setTextReview("TX1");

            context2.commitChanges();
        }

        {
            // read and check
            ObjectContext context3 = runtime.newContext();
            CompoundPainting o3 = SelectById.query(CompoundPainting.class, id).selectFirst(context3);

            assertEquals("AX1", o3.getArtistName());
            assertEquals(0, BigDecimal.valueOf(2).compareTo(o3.getEstimatedPrice()));
            assertEquals("XG1", o3.getGalleryName());
            assertEquals("PX1", o3.getPaintingTitle());
            assertEquals("TX1", o3.getTextReview());
        }
    }

    /**
     * Guarantee initial structure of database via deletion all data inside
     */
    private void createTestDataWithDeletion() throws Exception {
        tPaintingInfo.deleteAll();
        tPainting.deleteAll();
        tGallery.deleteAll();
        tArtist.deleteAll();

        long dateBase = System.currentTimeMillis();
        tArtist.insert(1, "artist1", new java.sql.Date(dateBase + 1000 * 60 * 60 * 24));
        tGallery.insert(1, "gallery1");
        tPainting.insert(1, "painting1", 1, new BigDecimal("1000"), 1);
        tPaintingInfo.insert(1, "painting review1");
    }
}
