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

package org.apache.cayenne;

import java.util.List;

import org.apache.art.Artist;
import org.apache.art.Gallery;
import org.apache.art.Painting;
import org.apache.art.ROPainting;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

public class CDOMany2OneTest extends CayenneDOTestBase {

    public void testReadRO1() throws Exception {

        // setup test
        Artist a1 = newArtist();
        Painting p1 = newPainting();
        a1.addToPaintingArray(p1);
        ctxt.commitChanges();

        // do select
        Expression e = ExpressionFactory.matchExp("toArtist", a1);
        SelectQuery q = new SelectQuery("ROPainting", e);

        // *** TESTING THIS ***
        List paints = ctxt.performQuery(q);
        assertEquals(1, paints.size());

        ROPainting rop1 = (ROPainting) paints.get(0);
        assertSame(a1, rop1.getToArtist());
    }

    public void testReadRO2() throws Exception {

        // setup test
        Artist a1 = newArtist();
        Painting p1 = newPainting();
        a1.addToPaintingArray(p1);
        ctxt.commitChanges();

        ctxt = createDataContext();

        // do select
        Expression e = ExpressionFactory.matchExp("toArtist", a1);
        SelectQuery q = new SelectQuery("ROPainting", e);

        // *** TESTING THIS ***
        List paints = ctxt.performQuery(q);
        assertEquals(1, paints.size());

        ROPainting rop1 = (ROPainting) paints.get(0);
        assertNotNull(rop1.getToArtist());

        // trigger fetch
        rop1.getToArtist().getArtistName();
        assertEquals(PersistenceState.COMMITTED, rop1.getToArtist().getPersistenceState());
    }

    public void testSelectViaRelationship() throws Exception {

        // setup test
        Artist a1 = newArtist();
        Painting p1 = newPainting();
        a1.addToPaintingArray(p1);
        ctxt.commitChanges();

        // do select
        Expression e = ExpressionFactory.matchExp("toArtist", a1);
        SelectQuery q = new SelectQuery("Painting", e);

        // *** TESTING THIS ***
        List paints = ctxt.performQuery(q);
        assertEquals(1, paints.size());
        assertSame(p1, paints.get(0));
    }

    public void testSelectViaMultiRelationship() throws Exception {

        // setup test
        Artist a1 = newArtist();
        Painting p1 = newPainting();
        Painting p2 = newPainting();
        Gallery g1 = newGallery();
        a1.addToPaintingArray(p1);
        a1.addToPaintingArray(p2);
        p1.setToGallery(g1);
        p2.setToGallery(g1);
        ctxt.commitChanges();

        // do select
        Expression e = ExpressionFactory.matchExp("paintingArray.toGallery", g1);
        SelectQuery q = new SelectQuery("Artist", e);

        // *** TESTING THIS ***
        List artists = ctxt.performQuery(q);
        assertEquals(1, artists.size());
        assertSame(a1, artists.get(0));
    }

    public void testNewAdd() throws Exception {
        Artist a1 = newArtist();
        Painting p1 = newPainting();

        // *** TESTING THIS ***
        p1.setToArtist(a1);

        // test before save
        assertSame(a1, p1.getToArtist());
        assertEquals(1, a1.getPaintingArray().size());
        assertSame(p1, a1.getPaintingArray().get(0));

        // do save
        ctxt.commitChanges();
        ctxt = createDataContext();

        // test database data
        Painting p2 = fetchPainting();
        Artist a2 = p2.getToArtist();
        assertNotNull(a2);
        assertEquals(artistName, a2.getArtistName());
    }

    public void testRemove() throws Exception {
        Painting p1 = newPainting();
        Gallery g1 = newGallery();
        p1.setToGallery(g1);

        // do save
        ctxt.commitChanges();
        ctxt = createDataContext();

        // test database data
        Painting p2 = fetchPainting();
        Gallery g2 = p2.getToGallery();

        // *** TESTING THIS ***
        p2.setToGallery(null);

        // test before save
        assertEquals(0, g2.getPaintingArray().size());
        assertNull(p2.getToGallery());

        // do save II
        ctxt.commitChanges();
        ctxt = createDataContext();

        Painting p3 = fetchPainting();
        assertNull(p3.getToGallery());
    }

    public void testReplace() throws Exception {
        String altGalleryName = "alt gallery";

        Painting p1 = newPainting();
        Gallery g1 = newGallery();
        g1.setGalleryName(altGalleryName);

        p1.setToGallery(g1);

        // do save
        ctxt.commitChanges();
        ctxt = createDataContext();

        // test database data
        Painting p2 = fetchPainting();
        Gallery g21 = p2.getToGallery();
        assertNotNull(g21);
        assertEquals(altGalleryName, g21.getGalleryName());
        assertEquals(1, g21.getPaintingArray().size());
        assertSame(p2, g21.getPaintingArray().get(0));

        Gallery g22 = newGallery();

        // *** TESTING THIS ***
        p2.setToGallery(g22);

        // test before save
        assertEquals(0, g21.getPaintingArray().size());
        assertEquals(1, g22.getPaintingArray().size());
        assertSame(p2, g22.getPaintingArray().get(0));

        // do save II
        ctxt.commitChanges();
        ctxt = createDataContext();

        Painting p3 = fetchPainting();
        Gallery g3 = p3.getToGallery();
        assertNotNull(g3);
        assertEquals(galleryName, g3.getGalleryName());
        assertEquals(1, g3.getPaintingArray().size());
        assertSame(p3, g3.getPaintingArray().get(0));
    }

    public void testSavedAdd() throws Exception {
        Painting p1 = newPainting();
        assertEquals(p1.getObjectId(), ctxt
                .localObject(p1.getObjectId(), null)
                .getObjectId());
        assertTrue(ctxt.hasChanges());

        // do save
        ctxt.commitChanges();
        ctxt = createDataContext();

        // test database data
        Painting p2 = fetchPainting();
        assertNull(p2.getToGallery());

        Gallery g2 = newGallery();

        // *** TESTING THIS ***
        p2.setToGallery(g2);

        // test before save
        assertEquals(1, g2.getPaintingArray().size());
        assertSame(p2, g2.getPaintingArray().get(0));

        // do save II
        ctxt.commitChanges();
        ctxt = createDataContext();

        Painting p3 = fetchPainting();
        Gallery g3 = p3.getToGallery();
        assertNotNull(g3);
        assertEquals(galleryName, g3.getGalleryName());
        assertEquals(1, g3.getPaintingArray().size());
        assertSame(p3, g3.getPaintingArray().get(0));
    }
}
