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

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.testdo.testmap.PaintingInfo;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class CDOOne2OneDepIT extends CayenneDOTestBase {

    @Inject
    private ObjectContext context1;

    @Test
    public void testRollbackDependent() {
        Artist a1 = newArtist();
        Painting p1 = newPainting();

        // needed to save without errors
        p1.setToArtist(a1);
        context.commitChanges();

        PaintingInfo info = context.newObject(PaintingInfo.class);
        info.setTextReview("XXX");
        p1.setToPaintingInfo(info);

        assertSame(info, p1.getToPaintingInfo());

        context.rollbackChanges();
        assertNull(p1.getToPaintingInfo());
    }

    @Test
    public void test2Null() throws Exception {
        Artist a1 = newArtist();
        Painting p1 = newPainting();

        // needed to save without errors
        p1.setToArtist(a1);
        context.commitChanges();
        context = context1;

        // test database data
        Painting p2 = fetchPainting();

        // *** TESTING THIS ***
        assertNull(p2.getToPaintingInfo());
    }

    @Test
    public void testReplaceNull() throws Exception {
        Artist a1 = newArtist();
        Painting p1 = newPainting();

        // needed to save without errors
        p1.setToArtist(a1);
        context.commitChanges();
        context = context1;

        // test database data
        Painting p2 = fetchPainting();

        // *** TESTING THIS ***
        p2.setToPaintingInfo(null);

        assertNull(p2.getToPaintingInfo());
    }

    @Test
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
        context.commitChanges();
        context = context1;

        // test database data
        Painting p2 = fetchPainting();
        PaintingInfo pi2 = p2.getToPaintingInfo();
        assertNotNull(pi2);
        assertEquals(textReview, pi2.getTextReview());
    }

    @Test
    public void testTakeObjectSnapshotDependentFault() throws Exception {
        // prepare data
        Artist a1 = newArtist();
        PaintingInfo pi1 = newPaintingInfo();
        Painting p1 = newPainting();

        p1.setToArtist(a1);
        p1.setToPaintingInfo(pi1);
        context.commitChanges();

        context = context1;
        Painting painting = fetchPainting();

        assertTrue(painting.readPropertyDirectly("toPaintingInfo") instanceof Fault);

        // test that taking a snapshot does not trigger a fault, and generally works well
        DataRow snapshot = ((DataContext) context).currentSnapshot(painting);

        assertEquals(paintingName, snapshot.get("PAINTING_TITLE"));
        assertTrue(painting.readPropertyDirectly("toPaintingInfo") instanceof Fault);
    }

}
