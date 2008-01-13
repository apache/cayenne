package org.apache.cayenne;

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

import java.sql.Timestamp;
import java.util.List;

import org.apache.art.Artist;
import org.apache.art.ArtistExhibit;
import org.apache.art.Exhibit;
import org.apache.art.Gallery;
import org.apache.art.Painting;
import org.apache.art.PaintingInfo;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

public abstract class CayenneDOTestBase extends CayenneCase {

    public static final String artistName = "artist with one painting";
    public static final String galleryName = "my gallery";
    public static final String textReview = "this painting sucks...";
    public static final String paintingName = "painting about nothing";

    static final byte[] paintingImage = new byte[] {
            2, 3, 4, 5
    };

    protected DataContext ctxt;

    @Override
    protected void setUp() throws Exception {
        deleteTestData();
        ctxt = createDataContext();
    }

    protected Exhibit newExhibit(Gallery gallery) {
        Exhibit e1 = (Exhibit) ctxt.newObject("Exhibit");
        e1.setOpeningDate(new Timestamp(System.currentTimeMillis()));
        e1.setClosingDate(new Timestamp(System.currentTimeMillis()));
        e1.setToGallery(gallery);
        return e1;
    }

    protected ArtistExhibit newArtistExhibit() {
        return (ArtistExhibit) ctxt.newObject("ArtistExhibit");
    }

    protected Gallery newGallery() {
        Gallery g1 = (Gallery) ctxt.newObject("Gallery");
        g1.setGalleryName(galleryName);
        return g1;
    }

    protected Artist newArtist() {
        Artist a1 = (Artist) ctxt.newObject("Artist");
        a1.setArtistName(artistName);
        return a1;
    }

    protected Painting newROPainting() {
        Painting p1 = (Painting) ctxt.newObject("ROPainting");
        p1.setPaintingTitle(paintingName);
        return p1;
    }

    protected Painting newPainting() {
        Painting p1 = (Painting) ctxt.newObject("Painting");
        p1.setPaintingTitle(paintingName);
        return p1;
    }

    protected PaintingInfo newPaintingInfo() {
        PaintingInfo p1 = (PaintingInfo) ctxt.newObject("PaintingInfo");
        p1.setTextReview(textReview);
        p1.setImageBlob(paintingImage);
        return p1;
    }

    protected Gallery fetchGallery() {
        SelectQuery q = new SelectQuery("Gallery", ExpressionFactory.matchExp(
                "galleryName",
                galleryName));
        List gls = ctxt.performQuery(q);
        return (gls.size() > 0) ? (Gallery) gls.get(0) : null;
    }

    protected Artist fetchArtist() {
        SelectQuery q = new SelectQuery("Artist", ExpressionFactory.matchExp(
                "artistName",
                artistName));
        List ats = ctxt.performQuery(q);
        return (ats.size() > 0) ? (Artist) ats.get(0) : null;
    }

    protected Painting fetchPainting() {
        SelectQuery q = new SelectQuery("Painting", ExpressionFactory.matchExp(
                "paintingTitle",
                paintingName));
        List pts = ctxt.performQuery(q);
        return (pts.size() > 0) ? (Painting) pts.get(0) : null;
    }

    protected PaintingInfo fetchPaintingInfo() {
        // we are using "LIKE" comparison, since Sybase does not allow
        // "=" comparisons on "text" columns
        SelectQuery q = new SelectQuery(PaintingInfo.class, ExpressionFactory.likeExp(
                "textReview",
                textReview));
        List pts = ctxt.performQuery(q);
        return (pts.size() > 0) ? (PaintingInfo) pts.get(0) : null;
    }
}
