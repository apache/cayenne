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
import org.apache.art.ArtistExhibit;
import org.apache.art.Exhibit;
import org.apache.art.Gallery;
import org.apache.art.Painting;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

public class CDOOne2ManyTest extends CayenneDOTestBase {

    public void testSelectWithToManyDBQualifier() throws Exception {
        // setup test, intentionally add more than 1 painting to artist
        // since this reduces a chance that painting and artist primary keys
        // would accidentally match, resulting in success when it should fail
        Artist a1 = newArtist();
        Painting p1 = newPainting();
        Painting p2 = newPainting();
        Painting p3 = newPainting();
        a1.addToPaintingArray(p1);
        a1.addToPaintingArray(p2);
        a1.addToPaintingArray(p3);
        ctxt.commitChanges();

        // do select
        Expression e = ExpressionFactory.matchDbExp("paintingArray", p2);
        SelectQuery q = new SelectQuery(Artist.class, e);

        // *** TESTING THIS ***
        List artists = ctxt.performQuery(q);
        assertEquals(1, artists.size());
        assertSame(a1, artists.get(0));
    }

    public void testSelectWithToManyQualifier() throws Exception {
        // setup test, intentionally add more than 1 painting to artist
        // since this reduces a chance that painting and artist primary keys
        // would accidentally match, resulting in success when it should fail
        Artist a1 = newArtist();
        Painting p1 = newPainting();
        Painting p2 = newPainting();
        Painting p3 = newPainting();
        a1.addToPaintingArray(p1);
        a1.addToPaintingArray(p2);
        a1.addToPaintingArray(p3);
        ctxt.commitChanges();

        // do select
        Expression e = ExpressionFactory.matchExp("paintingArray", p2);
        SelectQuery q = new SelectQuery(Artist.class, e);

        // *** TESTING THIS ***
        List artists = ctxt.performQuery(q);
        assertEquals(1, artists.size());
        assertSame(a1, artists.get(0));
    }

    public void testNewAdd() throws Exception {
        Artist a1 = newArtist();
        Painting p1 = newPainting();

        // *** TESTING THIS ***
        a1.addToPaintingArray(p1);

        // test before save
        assertSame(p1, a1.getPaintingArray().get(0));
        assertSame(a1, p1.getToArtist());

        // do save
        ctxt.commitChanges();
        ctxt = createDataContext();

        // test database data
        Artist a2 = fetchArtist();
        assertEquals(1, a2.getPaintingArray().size());
        assertEquals(paintingName, (a2.getPaintingArray().get(0))
                .getPaintingTitle());
    }

    public void testNewAddMultiples() throws Exception {
        Artist a1 = newArtist();
        Painting p11 = newPainting();
        Painting p12 = newPainting();

        // *** TESTING THIS ***
        a1.addToPaintingArray(p11);
        a1.addToPaintingArray(p12);

        // test before save
        assertEquals(2, a1.getPaintingArray().size());
        assertSame(a1, p11.getToArtist());
        assertSame(a1, p12.getToArtist());

        // do save
        ctxt.commitChanges();
        ctxt = createDataContext();

        // test database data
        Artist a2 = fetchArtist();
        assertEquals(2, a2.getPaintingArray().size());
    }

    public void testRemove1() throws Exception {
        Painting p1 = newPainting();
        Gallery g1 = newGallery();
        g1.addToPaintingArray(p1);

        // do save
        ctxt.commitChanges();
        ctxt = createDataContext();

        // test database data
        Gallery g2 = fetchGallery();
        Painting p2 = (Painting) g2.getPaintingArray().get(0);

        // *** TESTING THIS ***
        g2.removeFromPaintingArray(p2);

        // test before save
        assertEquals(0, g2.getPaintingArray().size());
        assertNull(p2.getToGallery());

        // do save II
        ctxt.commitChanges();
        ctxt = createDataContext();

        Painting p3 = fetchPainting();
        assertNull(p3.getToGallery());

        Gallery g3 = fetchGallery();
        assertEquals(0, g3.getPaintingArray().size());
    }

    public void testRemove2() throws Exception {
        Gallery g1 = newGallery();
        g1.addToPaintingArray(newPainting());
        g1.addToPaintingArray(newPainting());

        // do save
        ctxt.commitChanges();
        ctxt = createDataContext();

        // test database data
        Gallery g2 = fetchGallery();
        assertEquals(2, g2.getPaintingArray().size());
        Painting p2 = (Painting) g2.getPaintingArray().get(0);

        // *** TESTING THIS ***
        g2.removeFromPaintingArray(p2);

        // test before save
        assertEquals(1, g2.getPaintingArray().size());
        assertNull(p2.getToGallery());

        // do save II
        ctxt.commitChanges();
        ctxt = createDataContext();

        Gallery g3 = fetchGallery();
        assertEquals(1, g3.getPaintingArray().size());
    }

    public void testPropagatePK() throws Exception {
        // setup data
        Gallery g1 = newGallery();
        Exhibit e1 = newExhibit(g1);
        Artist a1 = newArtist();
        ctxt.commitChanges();

        // *** TESTING THIS ***
        ArtistExhibit ae1 = (ArtistExhibit) ctxt.newObject("ArtistExhibit");
        e1.addToArtistExhibitArray(ae1);
        a1.addToArtistExhibitArray(ae1);

        // check before save
        assertSame(e1, ae1.getToExhibit());
        assertSame(a1, ae1.getToArtist());

        // save
        // test "assertion" is that commit succeeds (PK of ae1 was set properly)
        ctxt.commitChanges();
    }
}
