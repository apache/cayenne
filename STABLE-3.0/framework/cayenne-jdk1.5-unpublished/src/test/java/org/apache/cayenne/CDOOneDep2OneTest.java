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

import org.apache.art.Artist;
import org.apache.art.ArtistExhibit;
import org.apache.art.Exhibit;
import org.apache.art.Gallery;
import org.apache.art.Painting;
import org.apache.art.PaintingInfo;
import org.apache.cayenne.access.types.ByteArrayTypeTest;

public class CDOOneDep2OneTest extends CayenneDOTestBase {

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
        ctxt.commitChanges();
        ctxt = createDataContext();

        // test database data
        PaintingInfo pi2 = fetchPaintingInfo();
        Painting p2 = pi2.getPainting();
        assertNotNull(p2);
        assertEquals(paintingName, p2.getPaintingTitle());
    }

    /** Tests how primary key is propagated from one new object to another. */
    public void testNewAdd2() throws Exception {
        Artist a1 = this.newArtist();
        Gallery g1 = this.newGallery();
        Exhibit e1 = this.newExhibit(g1);

        ArtistExhibit ae1 = this.newArtistExhibit();
        ae1.setToArtist(a1);
        ae1.setToExhibit(e1);

        // do save

        // *** TESTING THIS ***
        ctxt.commitChanges();
    }

    public void testReplace() throws Exception {
        String altPaintingName = "alt painting";

        Artist a1 = newArtist();
        assertEquals(a1.getObjectId(), ctxt
                .localObject(a1.getObjectId(), null)
                .getObjectId());

        PaintingInfo pi1 = newPaintingInfo();
        Painting p1 = newPainting();
        p1.setPaintingTitle(altPaintingName);

        pi1.setPainting(p1);

        assertTrue(ctxt.hasChanges());

        // do save
        ctxt.commitChanges();
        ctxt = createDataContext();

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
        ctxt.commitChanges();
        ObjectId pi2oid = pi2.getObjectId();
        ctxt = createDataContext();

        PaintingInfo pi3 = fetchPaintingInfo();
        Painting p3 = pi3.getPainting();
        assertNotNull(p3);
        assertEquals(paintingName, p3.getPaintingTitle());
        assertSame(pi3, p3.getToPaintingInfo());

        // test that object id was updated.
        assertEquals(pi2oid, pi3.getObjectId());

    }
}
