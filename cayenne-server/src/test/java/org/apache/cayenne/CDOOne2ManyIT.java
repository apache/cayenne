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

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.ArtistExhibit;
import org.apache.cayenne.testdo.testmap.Exhibit;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class CDOOne2ManyIT extends ServerCase {

    @Inject
    private ServerRuntime runtime;

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    protected TableHelper tArtist;
    protected TableHelper tPainting;

    @Before
    public void setUp() throws Exception {
        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns("PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "GALLERY_ID");
    }

    @Test
    public void testSelectWithToManyDBQualifier() {

        // intentionally add more than 1 painting to artist
        // since this reduces a chance that painting and artist primary keys
        // would accidentally match, resulting in success when it should fail

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("Xyz");

        Painting p1 = context.newObject(Painting.class);
        p1.setPaintingTitle("1");
        a1.addToPaintingArray(p1);

        Painting p2 = context.newObject(Painting.class);
        p2.setPaintingTitle("2");
        a1.addToPaintingArray(p2);

        Painting p3 = context.newObject(Painting.class);
        p3.setPaintingTitle("3");
        a1.addToPaintingArray(p3);

        context.commitChanges();

        // do select
        Expression e = ExpressionFactory.matchDbExp("paintingArray", p2);

        // *** TESTING THIS ***
        List<Artist> artists = ObjectSelect.query(Artist.class, e).select(context);
        assertEquals(1, artists.size());
        assertSame(a1, artists.get(0));
    }

    @Test
    public void testSelectWithToManyQualifier() {

        // intentionally add more than 1 painting to artist
        // since this reduces a chance that painting and artist primary keys
        // would accidentally match, resulting in success when it should fail

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("Xyz");

        Painting p1 = context.newObject(Painting.class);
        p1.setPaintingTitle("1");
        a1.addToPaintingArray(p1);

        Painting p2 = context.newObject(Painting.class);
        p2.setPaintingTitle("2");
        a1.addToPaintingArray(p2);

        Painting p3 = context.newObject(Painting.class);
        p3.setPaintingTitle("3");
        a1.addToPaintingArray(p3);

        context.commitChanges();

        // do select
        Expression e = ExpressionFactory.matchExp("paintingArray", p2);

        // *** TESTING THIS ***
        List<Artist> artists = ObjectSelect.query(Artist.class, e).select(context);
        assertEquals(1, artists.size());
        assertSame(a1, artists.get(0));
    }

    @Test
    public void testNewAdd() throws Exception {
        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("XyzQ");

        Painting p1 = context.newObject(Painting.class);
        p1.setPaintingTitle("1");

        // *** TESTING THIS ***
        a1.addToPaintingArray(p1);

        // test before save
        assertSame(p1, a1.getPaintingArray().get(0));
        assertSame(a1, p1.getToArtist());

        context.commitChanges();

        // test database data

        Object[] aRow = tArtist.select();

        // have to trim CHAR column to ensure consistent comparison results across DB's
        // should really be using VARCHAR in this test
        assertEquals("XyzQ", String.valueOf(aRow[1]).trim());

        Object[] pRow = tPainting.select();
        assertEquals("1", pRow[1]);
        assertEquals(aRow[0], pRow[2]);
    }

    @Test
    public void testNewAddMultiples() {
        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("XyzV");

        Painting p1 = context.newObject(Painting.class);
        p1.setPaintingTitle("1");
        a1.addToPaintingArray(p1);

        Painting p2 = context.newObject(Painting.class);
        p2.setPaintingTitle("2");
        a1.addToPaintingArray(p2);

        // test before save
        assertEquals(2, a1.getPaintingArray().size());
        assertSame(a1, p1.getToArtist());
        assertSame(a1, p2.getToArtist());

        context.commitChanges();

        ObjectContext context2 = runtime.newContext();

        // test database data
        Artist a2 = ObjectSelect.query(Artist.class).selectOne(context2);
        assertEquals(2, a2.getPaintingArray().size());
    }

    @Test
    public void testRemove1() {
        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("XyzE");

        Painting p1 = context.newObject(Painting.class);
        p1.setPaintingTitle("1");
        a1.addToPaintingArray(p1);

        context.commitChanges();

        ObjectContext context2 = runtime.newContext();

        // test database data
        Artist a2 = ObjectSelect.query(Artist.class).selectOne(context2);
        Painting p2 = a2.getPaintingArray().get(0);

        // *** TESTING THIS ***
        a2.removeFromPaintingArray(p2);

        // test before save
        assertEquals(0, a2.getPaintingArray().size());
        assertNull(p2.getToGallery());

        // do save II
        context2.commitChanges();

        ObjectContext context3 = runtime.newContext();

        Painting p3 = ObjectSelect.query(Painting.class).selectOne(context3);
        assertNull(p3.getToArtist());

        Artist a3 = ObjectSelect.query(Artist.class).selectOne(context3);
        assertEquals(0, a3.getPaintingArray().size());
    }

    @Test
    public void testRemove2() {
        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("XyzQ");

        Painting p01 = context.newObject(Painting.class);
        p01.setPaintingTitle("1");
        a1.addToPaintingArray(p01);

        Painting p02 = context.newObject(Painting.class);
        p02.setPaintingTitle("2");
        a1.addToPaintingArray(p02);

        context.commitChanges();

        ObjectContext context2 = runtime.newContext();

        // test database data
        Artist a2 = ObjectSelect.query(Artist.class).selectOne(context2);
        assertEquals(2, a2.getPaintingArray().size());
        Painting p2 = a2.getPaintingArray().get(0);

        // *** TESTING THIS ***
        a2.removeFromPaintingArray(p2);

        // test before save
        assertEquals(1, a2.getPaintingArray().size());
        assertNull(p2.getToArtist());

        // do save II
        context2.commitChanges();

        ObjectContext context3 = runtime.newContext();

        Artist a3 = ObjectSelect.query(Artist.class).selectOne(context3);
        assertEquals(1, a3.getPaintingArray().size());
    }

    @Test
    public void testPropagatePK() {
        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("XyBn");

        Gallery g1 = context.newObject(Gallery.class);
        g1.setGalleryName("Tyu");

        Exhibit e1 = context.newObject(Exhibit.class);
        e1.setToGallery(g1);
        e1.setOpeningDate(new Date());
        e1.setClosingDate(new Date());

        context.commitChanges();

        // *** TESTING THIS ***
        ArtistExhibit ae1 = context.newObject(ArtistExhibit.class);
        e1.addToArtistExhibitArray(ae1);
        a1.addToArtistExhibitArray(ae1);

        // check before save
        assertSame(e1, ae1.getToExhibit());
        assertSame(a1, ae1.getToArtist());

        // save
        // test "assertion" is that commit succeeds (PK of ae1 was set properly)
        context.commitChanges();
    }

    @Ignore("See CAY-2851 for details")
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
        g22.addToPaintingArray(p2);

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
}
