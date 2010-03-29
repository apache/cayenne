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

import java.util.Map;

import org.apache.art.Artist;
import org.apache.art.Painting;
import org.apache.art.PaintingInfo;

public class CDOOne2OneDepTest extends CayenneDOTestBase {
    
    public void testRollbackDependent() {
        Artist a1 = newArtist();
        Painting p1 = newPainting();

        // needed to save without errors
        p1.setToArtist(a1);
        ctxt.commitChanges();
        
        PaintingInfo info = ctxt.newObject(PaintingInfo.class);
        info.setTextReview("XXX");
        p1.setToPaintingInfo(info);
        
        assertSame(info, p1.getToPaintingInfo());
        
        ctxt.rollbackChanges();
        assertNull(p1.getToPaintingInfo());
    }

    public void test2Null() throws Exception {
        Artist a1 = newArtist();
        Painting p1 = newPainting();

        // needed to save without errors
        p1.setToArtist(a1);
        ctxt.commitChanges();
        ctxt = createDataContext();

        // test database data
        Painting p2 = fetchPainting();

        // *** TESTING THIS ***
        assertNull(p2.getToPaintingInfo());
    }

    public void testReplaceNull() throws Exception {
        Artist a1 = newArtist();
        Painting p1 = newPainting();

        // needed to save without errors
        p1.setToArtist(a1);
        ctxt.commitChanges();
        ctxt = createDataContext();

        // test database data
        Painting p2 = fetchPainting();

        // *** TESTING THIS ***
        p2.setToPaintingInfo(null);

        assertNull(p2.getToPaintingInfo());
    }

    public void testNewAdd() throws Exception {
        Artist a1 = newArtist();
        PaintingInfo pi1 = newPaintingInfo();
        Painting p1 = newPainting();

        // needed to save without errors
        p1.setToArtist(a1);

        // *** TESTING THIS *** 
        p1.setToPaintingInfo(pi1);

        // test before save
        assertSame(pi1, p1.getToPaintingInfo());
        assertSame(p1, pi1.getPainting());

        // do save 
        ctxt.commitChanges();
        ctxt = createDataContext();

        // test database data
        Painting p2 = fetchPainting();
        PaintingInfo pi2 = p2.getToPaintingInfo();
        assertNotNull(pi2);
        assertEquals(textReview, pi2.getTextReview());
    }

    public void testTakeObjectSnapshotDependentFault() throws Exception {
        // prepare data
        Artist a1 = newArtist();
        PaintingInfo pi1 = newPaintingInfo();
        Painting p1 = newPainting();
        
        p1.setToArtist(a1);
        p1.setToPaintingInfo(pi1);
        ctxt.commitChanges();
        
        ctxt = createDataContext();
        Painting painting = fetchPainting();

        assertTrue(painting.readPropertyDirectly("toPaintingInfo") instanceof Fault);

        // test that taking a snapshot does not trigger a fault, and generally works well 
        Map snapshot = ctxt.currentSnapshot(painting);

        assertEquals(paintingName, snapshot.get("PAINTING_TITLE"));
        assertTrue(painting.readPropertyDirectly("toPaintingInfo") instanceof Fault);
    }

}
