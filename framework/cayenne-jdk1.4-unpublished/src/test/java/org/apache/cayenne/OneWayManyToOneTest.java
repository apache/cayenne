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

import org.apache.art.oneway.Gallery;
import org.apache.art.oneway.Painting;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.OneWayMappingCase;

/**
 * @author Andrus Adamchik
 */
public class OneWayManyToOneTest extends OneWayMappingCase {

    protected DataContext ctxt;

    protected void setUp() throws Exception {
        deleteTestData();
        ctxt = getDomain().createDataContext();
    }

    public void testSavedAdd() throws Exception {
        // prepare and save a gallery
        Gallery g1 = newGallery("g1");
        ctxt.commitChanges();

        Painting p2 = newPainting();

        // *** TESTING THIS ***
        p2.setToGallery(g1);

        // test before save
        assertSame(g1, p2.getToGallery());

        // do save II
        ctxt.commitChanges();
        ctxt = createDataContext();

        Painting p3 = fetchPainting();
        Gallery g3 = p3.getToGallery();
        assertNotNull(g3);
        assertEquals("g1", g3.getGalleryName());
    }

    public void testSavedReplace() throws Exception {
        // prepare and save a gallery
        Gallery g11 = newGallery("g1");
        Gallery g12 = newGallery("g1");
        ctxt.commitChanges();

        Painting p1 = newPainting();
        p1.setToGallery(g11);

        // test before save
        assertSame(g11, p1.getToGallery());
        ctxt.commitChanges();

        p1.setToGallery(g12);
        ctxt.commitChanges();

        ctxt = createDataContext();

        Painting p2 = fetchPainting();
        Gallery g21 = p2.getToGallery();
        assertNotNull(g21);
        assertEquals(g12.getGalleryName(), g21.getGalleryName());
    }

    public void testRevertModification() {
        // prepare and save a gallery
        Gallery g11 = newGallery("g1");
        Gallery g12 = newGallery("g1");
        ctxt.commitChanges();

        Painting p1 = newPainting();
        p1.setToGallery(g11);

        // test before save
        assertSame(g11, p1.getToGallery());
        ctxt.commitChanges();

        p1.setToGallery(g12);
        ctxt.rollbackChanges();

        assertEquals(g11, p1.getToGallery());
        // Expecting the original gallery to be the one

        // And save so we can be sure that the save did the right thing
        ctxt.commitChanges();
        ctxt = createDataContext();

        Painting p2 = fetchPainting();
        Gallery g21 = p2.getToGallery();
        assertNotNull(g21);
        // IT should still be the first one we set
        assertEquals(g11.getGalleryName(), g21.getGalleryName());
    }

    protected Painting newPainting() {
        Painting p1 = (Painting) ctxt.newObject("Painting");
        p1.setPaintingTitle(CayenneDOTestBase.paintingName);
        return p1;
    }

    protected Gallery newGallery(String name) {
        Gallery g1 = (Gallery) ctxt.newObject("Gallery");
        g1.setGalleryName(name);
        return g1;
    }

    protected Painting fetchPainting() {
        SelectQuery q = new SelectQuery("Painting", ExpressionFactory.matchExp(
                "paintingTitle",
                CayenneDOTestBase.paintingName));
        List pts = ctxt.performQuery(q);
        return (pts.size() > 0) ? (Painting) pts.get(0) : null;
    }
}
