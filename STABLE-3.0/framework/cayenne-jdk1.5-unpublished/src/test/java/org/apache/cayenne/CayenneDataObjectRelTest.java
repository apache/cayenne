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

import org.apache.art.ArtGroup;
import org.apache.art.Artist;
import org.apache.art.Gallery;
import org.apache.art.Painting;
import org.apache.art.PaintingInfo;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.ToManyList;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CaseDataFactory;

public class CayenneDataObjectRelTest extends CayenneDOTestBase {

    private void prepareNestedProperties() throws Exception {
        Artist a1 = super.newArtist();
        Painting p1 = super.newPainting();
        PaintingInfo pi1 = super.newPaintingInfo();
        Gallery g1 = super.newGallery();

        p1.setToArtist(a1);
        p1.setToPaintingInfo(pi1);
        p1.setToGallery(g1);
        ctxt.commitChanges();
        ctxt = createDataContext();
    }

    public void testReadNestedProperty1() throws Exception {
        prepareNestedProperties();

        Painting p1 = fetchPainting();
        assertEquals(artistName, p1.readNestedProperty("toArtist.artistName"));
    }

    public void testReadNestedProperty2() throws Exception {
        prepareNestedProperties();

        Painting p1 = fetchPainting();
        assertTrue(p1.getToArtist().readNestedProperty("paintingArray") instanceof List);
    }

    public void testReciprocalRel1() throws Exception {
        CaseDataFactory.createArtistWithPainting(artistName, new String[] {
            paintingName
        }, false);

        Painting p1 = fetchPainting();
        Artist a1 = p1.getToArtist();

        assertNotNull(a1);
        assertEquals(artistName, a1.getArtistName());

        List paintings = a1.getPaintingArray();
        assertEquals(1, paintings.size());
        Painting p2 = (Painting) paintings.get(0);
        assertSame(p1, p2);
    }

    public void testReadToOneRel1() throws Exception {
        // read to-one relationship
        CaseDataFactory.createArtistWithPainting(artistName, new String[] {
            paintingName
        }, false);

        Painting p1 = fetchPainting();
        Artist a1 = p1.getToArtist();

        assertNotNull(a1);
        assertEquals(PersistenceState.HOLLOW, a1.getPersistenceState());
        assertEquals(artistName, a1.getArtistName());
        assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
    }

    public void testReadToOneRel2() throws Exception {
        // test chained calls to read relationships
        CaseDataFactory.createArtistWithPainting(artistName, new String[] {
            paintingName
        }, true);

        PaintingInfo pi1 = fetchPaintingInfo(paintingName);
        Painting p1 = pi1.getPainting();
        p1.getPaintingTitle();

        Artist a1 = p1.getToArtist();

        assertNotNull(a1);
        assertEquals(PersistenceState.HOLLOW, a1.getPersistenceState());
        assertEquals(artistName, a1.getArtistName());
        assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
    }

    public void testReadToOneRel3() throws Exception {
        // test null relationship destination
        CaseDataFactory.createArtistWithPainting(artistName, new String[] {
            paintingName
        }, false);

        Painting p1 = fetchPainting();
        Gallery g1 = p1.getToGallery();
        assertNull(g1);
    }

    public void testReadToManyRel1() throws Exception {
        CaseDataFactory.createArtistWithPainting(artistName, new String[] {
            paintingName
        }, false);

        Artist a1 = fetchArtist();
        List plist = a1.getPaintingArray();

        assertNotNull(plist);
        assertEquals(1, plist.size());
        assertEquals(PersistenceState.COMMITTED, ((Painting) plist.get(0))
                .getPersistenceState());
        assertEquals(paintingName, ((Painting) plist.get(0)).getPaintingTitle());
    }

    public void testReadToManyRel2() throws Exception {
        // test empty relationship
        CaseDataFactory.createArtistWithPainting(artistName, new String[] {}, false);

        Artist a1 = fetchArtist();
        List plist = a1.getPaintingArray();

        assertNotNull(plist);
        assertEquals(0, plist.size());
    }

    public void testReflexiveRelationshipInsertOrder1() {
        DataContext dc = this.createDataContext();
        ArtGroup parentGroup = (ArtGroup) dc.newObject("ArtGroup");
        parentGroup.setName("parent");

        ArtGroup childGroup1 = (ArtGroup) dc.newObject("ArtGroup");
        childGroup1.setName("child1");
        childGroup1.setToParentGroup(parentGroup);
        dc.commitChanges();
    }

    public void testReflexiveRelationshipInsertOrder2() {
        // Create in a different order and see what happens
        DataContext dc = this.createDataContext();
        ArtGroup childGroup1 = (ArtGroup) dc.newObject("ArtGroup");
        childGroup1.setName("child1");

        ArtGroup parentGroup = (ArtGroup) dc.newObject("ArtGroup");
        parentGroup.setName("parent");

        childGroup1.setToParentGroup(parentGroup);

        dc.commitChanges();
    }

    public void testReflexiveRelationshipInsertOrder3() {
        // Tey multiple children, one created before parent, one after
        DataContext dc = this.createDataContext();
        ArtGroup childGroup1 = (ArtGroup) dc.newObject("ArtGroup");
        childGroup1.setName("child1");

        ArtGroup parentGroup = (ArtGroup) dc.newObject("ArtGroup");
        parentGroup.setName("parent");

        childGroup1.setToParentGroup(parentGroup);

        ArtGroup childGroup2 = (ArtGroup) dc.newObject("ArtGroup");
        childGroup2.setName("child2");
        childGroup2.setToParentGroup(parentGroup);

        dc.commitChanges();
    }

    public void testReflexiveRelationshipInsertOrder4() {
        // Tey multiple children, one created before parent, one after
        DataContext dc = this.createDataContext();
        ArtGroup childGroup1 = (ArtGroup) dc.newObject("ArtGroup");
        childGroup1.setName("child1");

        ArtGroup parentGroup = (ArtGroup) dc.newObject("ArtGroup");
        parentGroup.setName("parent");

        childGroup1.setToParentGroup(parentGroup);

        ArtGroup childGroup2 = (ArtGroup) dc.newObject("ArtGroup");
        childGroup2.setName("subchild");
        childGroup2.setToParentGroup(childGroup1);

        dc.commitChanges();
    }

    public void testCrossContextRelationshipException() {
        DataContext otherContext = getDomain().createDataContext();
        // Create this object in one context...
        Artist artist = (Artist) ctxt.newObject("Artist");
        // ...and this object in another context
        Painting painting = (Painting) otherContext
                .newObject("Painting");

        // Check setting a toOne relationship
        try {
            painting.setToArtist(artist);
            fail("Should have failed to set a cross-context relationship");
        }
        catch (CayenneRuntimeException e) {
            // Fine.. it should throw an exception
        }

        assertNull(painting.getToArtist()); // Make sure it wasn't set

        // Now try the reverse (toMany) relationship
        try {
            artist.addToPaintingArray(painting);
            fail("Should have failed to add a cross-context relationship");
        }
        catch (CayenneRuntimeException e) {
            // Fine.. it should throw an exception
        }

        assertEquals(0, artist.getPaintingArray().size());

    }

    public void testComplexInsertUpdateOrdering() {
        Artist artist = (Artist) ctxt.newObject("Artist");
        artist.setArtistName("a name");

        ctxt.commitChanges();

        // Cause an update and an insert that need correct ordering
        Painting painting = (Painting) ctxt.newObject("Painting");
        painting.setPaintingTitle("a painting");
        artist.addToPaintingArray(painting);

        ctxt.commitChanges();

        ctxt.deleteObject(artist);
        ctxt.commitChanges();
    }

    private PaintingInfo fetchPaintingInfo(String name) {
        SelectQuery q = new SelectQuery("PaintingInfo", ExpressionFactory.matchExp(
                "painting.paintingTitle",
                name));
        List pts = ctxt.performQuery(q);
        return (pts.size() > 0) ? (PaintingInfo) pts.get(0) : null;
    }

    public void testNewToMany() throws Exception {
        Artist artist = ctxt.newObject(Artist.class);
        artist.setArtistName("test");
        assertTrue(artist.readPropertyDirectly("paintingArray") instanceof ToManyList);

        ToManyList list = (ToManyList) artist.readPropertyDirectly("paintingArray");
        assertFalse(list.isFault());

        ctxt.commitChanges();

        assertFalse(list.isFault());
    }
}
