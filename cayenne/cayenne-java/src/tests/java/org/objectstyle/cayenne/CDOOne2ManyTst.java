/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne;

import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.art.ArtistExhibit;
import org.objectstyle.art.Exhibit;
import org.objectstyle.art.Gallery;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;

public class CDOOne2ManyTst extends CayenneDOTestBase {

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
        assertEquals(paintingName, ((Painting) a2.getPaintingArray().get(0))
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
        ArtistExhibit ae1 = (ArtistExhibit) ctxt
                .createAndRegisterNewObject("ArtistExhibit");
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