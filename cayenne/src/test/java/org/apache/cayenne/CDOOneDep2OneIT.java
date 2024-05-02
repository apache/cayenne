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

import org.apache.cayenne.access.types.ByteArrayTypeTest;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.ArtistExhibit;
import org.apache.cayenne.testdo.testmap.Exhibit;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.testdo.testmap.PaintingInfo;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import java.sql.Timestamp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class CDOOneDep2OneIT extends CayenneDOTestBase {

    @Inject
    private ObjectContext context1;

    @Test
    public void testNewAdd1() throws Exception {
        Artist a1 = newArtist();
        PaintingInfo pi1 = newPaintingInfo();
        Painting p1 = newPainting();

        // needed to save without errors
        p1.setToArtist(a1);

        // *** TESTING THIS ***
        pi1.setPainting(p1);

        // test before save
        assertSame(pi1, p1.getToPaintingInfo());
        assertSame(p1, pi1.getPainting());

        // do save
        context.commitChanges();
        context = context1;

        // test database data
        PaintingInfo pi2 = fetchPaintingInfo();
        Painting p2 = pi2.getPainting();
        assertNotNull(p2);
        assertEquals(paintingName, p2.getPaintingTitle());
    }

    /** Tests how primary key is propagated from one new object to another. */
    @Test
    public void testNewAdd2() throws Exception {
        Artist a1 = this.newArtist();
        Gallery g1 = context.newObject(Gallery.class);
        g1.setGalleryName(galleryName);

        Exhibit e1 = context.newObject(Exhibit.class);
        e1.setOpeningDate(new Timestamp(System.currentTimeMillis()));
        e1.setClosingDate(new Timestamp(System.currentTimeMillis()));
        e1.setToGallery(g1);

        ArtistExhibit ae1 = context.newObject(ArtistExhibit.class);
        ae1.setToArtist(a1);
        ae1.setToExhibit(e1);

        // *** TESTING THIS ***
        context.commitChanges();
    }

    @Test
    public void testReplace() throws Exception {
        String altPaintingName = "alt painting";

        PaintingInfo pi1 = newPaintingInfo();
        Painting p1 = newPainting();
        p1.setPaintingTitle(altPaintingName);

        pi1.setPainting(p1);

        assertTrue(context.hasChanges());

        // do save
        context.commitChanges();
        context = context1;

        // test database data
        PaintingInfo pi2 = fetchPaintingInfo();
        Painting p21 = pi2.getPainting();
        assertNotNull(p21);
        assertEquals(altPaintingName, p21.getPaintingTitle());
        assertSame(pi2, p21.getToPaintingInfo());
        ByteArrayTypeTest.assertByteArraysEqual(paintingImage, p21
                .getToPaintingInfo()
                .getImageBlob());

        Painting p22 = newPainting();

        // *** TESTING THIS ***
        pi2.setPainting(p22);

        // test before save
        assertNull(p21.getToPaintingInfo());
        assertSame(pi2, p22.getToPaintingInfo());
        assertSame(p22, pi2.getPainting());
        assertEquals(PersistenceState.MODIFIED, pi2.getPersistenceState());

        // do save II
        context.commitChanges();
        ObjectId pi2oid = pi2.getObjectId();
        context = context1;

        PaintingInfo pi3 = fetchPaintingInfo();
        Painting p3 = pi3.getPainting();
        assertNotNull(p3);
        assertEquals(paintingName, p3.getPaintingTitle());
        assertSame(pi3, p3.getToPaintingInfo());

        // test that object id was updated.
        assertEquals(pi2oid, pi3.getObjectId());

    }

    @Test
    public void testReplaceOtherSide() throws Exception {
        String altPaintingName = "alt painting";

        PaintingInfo pi1 = newPaintingInfo();
        Painting p1 = newPainting();
        p1.setPaintingTitle(altPaintingName);

        pi1.setPainting(p1);

        assertTrue(context.hasChanges());

        // do save
        context.commitChanges();
        context = context1;

        // test database data
        PaintingInfo pi2 = fetchPaintingInfo();
        Painting p21 = pi2.getPainting();
        assertNotNull(p21);
        assertEquals(altPaintingName, p21.getPaintingTitle());
        assertSame(pi2, p21.getToPaintingInfo());
        ByteArrayTypeTest.assertByteArraysEqual(paintingImage, p21
                .getToPaintingInfo()
                .getImageBlob());

        Painting p22 = newPainting();

        // *** TESTING THIS ***
        p22.setToPaintingInfo(pi2);

        // test before save
        assertNull(p21.getToPaintingInfo());
        assertSame(pi2, p22.getToPaintingInfo());
        assertSame(p22, pi2.getPainting());
        assertEquals(PersistenceState.MODIFIED, pi2.getPersistenceState());

        // do save II
        context.commitChanges();
        ObjectId pi2oid = pi2.getObjectId();
        context = context1;

        PaintingInfo pi3 = fetchPaintingInfo();
        Painting p3 = pi3.getPainting();
        assertNotNull(p3);
        assertEquals(paintingName, p3.getPaintingTitle());
        assertSame(pi3, p3.getToPaintingInfo());

        // test that object id was updated.
        assertEquals(pi2oid, pi3.getObjectId());

    }
}
