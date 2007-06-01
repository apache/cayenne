package org.objectstyle.cayenne;
/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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

import java.sql.Timestamp;
import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.art.ArtistExhibit;
import org.objectstyle.art.Exhibit;
import org.objectstyle.art.Gallery;
import org.objectstyle.art.Painting;
import org.objectstyle.art.PaintingInfo;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;

public class CayenneDOTestBase extends CayenneTestCase {
    public static final String artistName = "artist with one painting";
    public static final String galleryName = "my gallery";
    public static final String textReview = "this painting sucks...";
    public static final String paintingName = "painting about nothing";
    public static final String groupName = "a group";

    static final byte[] paintingImage = new byte[] { 2, 3, 4, 5 };

    protected DataContext ctxt;

    protected void setUp() throws Exception {
        deleteTestData();
        ctxt = getDomain().createDataContext();
    }

    protected Exhibit newExhibit(Gallery gallery) {
        Exhibit e1 = (Exhibit) ctxt.createAndRegisterNewObject("Exhibit");
        e1.setOpeningDate(new Timestamp(System.currentTimeMillis()));
        e1.setClosingDate(new Timestamp(System.currentTimeMillis()));
        e1.setToGallery(gallery);
        return e1;
    }

    protected ArtistExhibit newArtistExhibit() {
        return (ArtistExhibit) ctxt.createAndRegisterNewObject("ArtistExhibit");
    }

    protected Gallery newGallery() {
        Gallery g1 = (Gallery) ctxt.createAndRegisterNewObject("Gallery");
        g1.setGalleryName(galleryName);
        return g1;
    }

    protected Artist newArtist() {
        Artist a1 = (Artist) ctxt.createAndRegisterNewObject("Artist");
        a1.setArtistName(artistName);
        return a1;
    }

    protected Painting newROPainting() {
        Painting p1 = (Painting) ctxt.createAndRegisterNewObject("ROPainting");
        p1.setPaintingTitle(paintingName);
        return p1;
    }

    protected Painting newPainting() {
        Painting p1 = (Painting) ctxt.createAndRegisterNewObject("Painting");
        p1.setPaintingTitle(paintingName);
        return p1;
    }

    protected PaintingInfo newPaintingInfo() {
        PaintingInfo p1 = (PaintingInfo) ctxt.createAndRegisterNewObject("PaintingInfo");
        p1.setTextReview(textReview);
        p1.setImageBlob(paintingImage);
        return p1;
    }

    protected Gallery fetchGallery() {
        SelectQuery q =
            new SelectQuery(
                "Gallery",
                ExpressionFactory.matchExp("galleryName", galleryName));
        List gls = ctxt.performQuery(q);
        return (gls.size() > 0) ? (Gallery) gls.get(0) : null;
    }

    protected Artist fetchArtist() {
        SelectQuery q =
            new SelectQuery(
                "Artist",
                ExpressionFactory.matchExp("artistName", artistName));
        List ats = ctxt.performQuery(q);
        return (ats.size() > 0) ? (Artist) ats.get(0) : null;
    }

    protected Painting fetchPainting() {
        SelectQuery q =
            new SelectQuery(
                "Painting",
                ExpressionFactory.matchExp("paintingTitle", paintingName));
        List pts = ctxt.performQuery(q);
        return (pts.size() > 0) ? (Painting) pts.get(0) : null;
    }

    protected PaintingInfo fetchPaintingInfo() {
        // we are using "LIKE" comparison, since Sybase does not allow
        // "=" comparisons on "text" columns
        SelectQuery q =
            new SelectQuery(
                PaintingInfo.class,
                ExpressionFactory.likeExp("textReview", textReview));
        List pts = ctxt.performQuery(q);
        return (pts.size() > 0) ? (PaintingInfo) pts.get(0) : null;
    }
}
