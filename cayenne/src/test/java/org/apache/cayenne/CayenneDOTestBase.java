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
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.testdo.testmap.PaintingInfo;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;

import java.util.List;

public abstract class CayenneDOTestBase extends RuntimeCase {

    public static final String artistName = "artist with one painting";
    public static final String galleryName = "my gallery";
    public static final String textReview = "this painting sucks...";
    public static final String paintingName = "painting about nothing";

    static final byte[] paintingImage = new byte[] {
            2, 3, 4, 5
    };

    @Inject
    protected ObjectContext context;

    protected Artist newArtist() {
        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName(artistName);
        return a1;
    }

    protected Painting newPainting() {
        Painting p1 = context.newObject(Painting.class);
        p1.setPaintingTitle(paintingName);
        return p1;
    }

    protected PaintingInfo newPaintingInfo() {
        PaintingInfo p1 = context.newObject(PaintingInfo.class);
        p1.setTextReview(textReview);
        p1.setImageBlob(paintingImage);
        return p1;
    }

    protected Painting fetchPainting() {
        List<Painting> pts = ObjectSelect.query(Painting.class, Painting.PAINTING_TITLE.eq(paintingName)).select(context);
        return (pts.size() > 0) ? pts.get(0) : null;
    }

    protected PaintingInfo fetchPaintingInfo() {
        // we are using "LIKE" comparison, since Sybase does not allow
        // "=" comparisons on "text" columns
        List<PaintingInfo> pts = ObjectSelect.query(PaintingInfo.class, PaintingInfo.TEXT_REVIEW.like(textReview)).select(context);
        return (pts.size() > 0) ? pts.get(0) : null;
    }
}
