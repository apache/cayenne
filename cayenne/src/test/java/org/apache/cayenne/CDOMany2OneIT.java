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

package org.apache.cayenne;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.testdo.testmap.ROPainting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.Types;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class CDOMany2OneIT extends RuntimeCase {

    @Inject
    private CayenneRuntime runtime;

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    protected TableHelper tArtist;
    protected TableHelper tPainting;
    protected TableHelper tGallery;

    @Before
    public void setUp() throws Exception {
        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting
                .setColumns("PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "GALLERY_ID")
                .setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.BIGINT, Types.INTEGER);

        tGallery = new TableHelper(dbHelper, "GALLERY");
        tGallery.setColumns("GALLERY_ID", "GALLERY_NAME");
    }

    private void createArtistWithPaintingDataSet() throws Exception {
        tArtist.insert(8, "aX");
        tPainting.insert(6, "pW", 8, null);
    }

    private void createArtistWithPaintingsInGalleryDataSet() throws Exception {
        tArtist.insert(8, "aX");
        tGallery.insert(11, "Ge");
        tPainting.insert(6, "pW1", 8, 11);
        tPainting.insert(7, "pW2", 8, 11);

    }

    @Test
    public void testMultipleToOneDeletion() {

        // was a problem per CAY-901

        Painting p = context.newObject(Painting.class);
        p.setPaintingTitle("P1");

        Artist a = context.newObject(Artist.class);
        a.setArtistName("A1");

        Gallery g = context.newObject(Gallery.class);
        g.setGalleryName("G1");

        p.setToArtist(a);
        p.setToGallery(g);
        context.commitChanges();

        p.setToArtist(null);
        p.setToGallery(null);

        context.commitChanges();

        SQLTemplate q = new SQLTemplate(Painting.class, "SELECT * from PAINTING");
        q.setColumnNamesCapitalization(CapsStrategy.UPPER);
        q.setFetchingDataRows(true);

        DataRow row = (DataRow) Cayenne.objectForQuery(context, q);
        assertNotNull(row);
        assertEquals("P1", row.get("PAINTING_TITLE"));
        assertNull(row.get("ARTIST_ID"));
        assertNull(row.get("GALLERY_ID"));
    }

    @Test
    public void testReadRO1() throws Exception {

        createArtistWithPaintingDataSet();

        Artist a1 = Cayenne.objectForPK(context, Artist.class, 8);

        List<ROPainting> paints = ObjectSelect.query(ROPainting.class, ROPainting.TO_ARTIST.eq(a1)).select(context);
        assertEquals(1, paints.size());

        ROPainting rop1 = paints.get(0);
        assertSame(a1, rop1.getToArtist());
    }

    @Test
    public void testReadRO2() throws Exception {

        createArtistWithPaintingDataSet();

        Artist a1 = Cayenne.objectForPK(context, Artist.class, 8);

        List<ROPainting> paints = ObjectSelect.query(ROPainting.class, ROPainting.TO_ARTIST.eq(a1)).select(context);
        assertEquals(1, paints.size());

        ROPainting rop1 = paints.get(0);
        assertNotNull(rop1.getToArtist());

        // trigger fetch
        rop1.getToArtist().getArtistName();
        assertEquals(PersistenceState.COMMITTED, rop1.getToArtist().getPersistenceState());
    }

    @Test
    public void testSelectViaRelationship() throws Exception {

        createArtistWithPaintingDataSet();
        Artist a1 = Cayenne.objectForPK(context, Artist.class, 8);
        Painting p1 = Cayenne.objectForPK(context, Painting.class, 6);

        List<Painting> paints = ObjectSelect.query(Painting.class, Painting.TO_ARTIST.eq(a1)).select(context);
        assertEquals(1, paints.size());
        assertSame(p1, paints.get(0));
    }

    @Test
    public void testSelectViaMultiRelationship() throws Exception {

        createArtistWithPaintingsInGalleryDataSet();

        Artist a1 = Cayenne.objectForPK(context, Artist.class, 8);
        Gallery g1 = Cayenne.objectForPK(context, Gallery.class, 11);

        List<Artist> artists = ObjectSelect.query(Artist.class)
                .where(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).eq(g1))
                .select(context);
        assertEquals(1, artists.size());
        assertSame(a1, artists.get(0));
    }

    @Test
    public void testNewAdd() throws Exception {
        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("bL");

        Painting p1 = context.newObject(Painting.class);
        p1.setPaintingTitle("xa");

        p1.setToArtist(a1);

        assertSame(a1, p1.getToArtist());
        assertEquals(1, a1.getPaintingArray().size());
        assertSame(p1, a1.getPaintingArray().get(0));

        context.commitChanges();

        assertEquals(Cayenne.longPKForObject(a1), tArtist.getLong("ARTIST_ID"));
        assertEquals(Cayenne.longPKForObject(a1), tPainting.getLong("ARTIST_ID"));
    }

    @Test
    public void testRemove() {
        Painting p1 = context.newObject(Painting.class);
        p1.setPaintingTitle("xa");

        Gallery g1 = context.newObject(Gallery.class);
        g1.setGalleryName("yT");

        p1.setToGallery(g1);

        // do save
        context.commitChanges();

        ObjectContext context2 = runtime.newContext();

        // test database data
        Painting p2 = ObjectSelect.query(Painting.class).selectOne(context2);
        Gallery g2 = p2.getToGallery();

        p2.setToGallery(null);

        // test before save
        assertEquals(0, g2.getPaintingArray().size());
        assertNull(p2.getToGallery());

        // do save II
        context2.commitChanges();

        ObjectContext context3 = runtime.newContext();

        Painting p3 = ObjectSelect.query(Painting.class).selectOne(context3);
        assertNull(p3.getToGallery());
    }

    @Test
    public void testReplace() {

        Painting p1 = context.newObject(Painting.class);
        p1.setPaintingTitle("xa");

        Gallery g1 = context.newObject(Gallery.class);
        g1.setGalleryName("yTW");

        p1.setToGallery(g1);

        context.commitChanges();
        ObjectContext context2 = runtime.newContext();

        // test database data
        Painting p2 = ObjectSelect.query(Painting.class).selectOne(context2);
        Gallery g21 = p2.getToGallery();
        assertNotNull(g21);
        assertEquals("yTW", g21.getGalleryName());
        assertEquals(1, g21.getPaintingArray().size());
        assertSame(p2, g21.getPaintingArray().get(0));

        Gallery g22 = context2.newObject(Gallery.class);
        g22.setGalleryName("rE");
        p2.setToGallery(g22);

        // test before save
        assertEquals(0, g21.getPaintingArray().size());
        assertEquals(1, g22.getPaintingArray().size());
        assertSame(p2, g22.getPaintingArray().get(0));

        // do save II
        context2.commitChanges();

        ObjectContext context3 = runtime.newContext();

        Painting p3 = ObjectSelect.query(Painting.class).selectOne(context3);
        Gallery g3 = p3.getToGallery();
        assertNotNull(g3);
        assertEquals("rE", g3.getGalleryName());
        assertEquals(1, g3.getPaintingArray().size());
        assertSame(p3, g3.getPaintingArray().get(0));
    }

    @Test
    public void testSavedAdd() {
        Painting p1 = context.newObject(Painting.class);
        p1.setPaintingTitle("xa");

        assertTrue(context.hasChanges());

        // do save
        context.commitChanges();
        ObjectContext context2 = runtime.newContext();

        // test database data
        Painting p2 = ObjectSelect.query(Painting.class).selectOne(context2);
        assertNull(p2.getToGallery());

        Gallery g2 = context2.newObject(Gallery.class);
        g2.setGalleryName("rE");

        p2.setToGallery(g2);

        // test before save
        assertEquals(1, g2.getPaintingArray().size());
        assertSame(p2, g2.getPaintingArray().get(0));

        // do save II
        context2.commitChanges();
        ObjectContext context3 = runtime.newContext();

        Painting p3 = ObjectSelect.query(Painting.class).selectOne(context3);
        Gallery g3 = p3.getToGallery();
        assertNotNull(g3);
        assertEquals("rE", g3.getGalleryName());
        assertEquals(1, g3.getPaintingArray().size());
        assertSame(p3, g3.getPaintingArray().get(0));
    }
}
