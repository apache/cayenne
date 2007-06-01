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
package org.objectstyle.cayenne;

import java.util.List;

import org.objectstyle.art.ArtGroup;
import org.objectstyle.art.Artist;
import org.objectstyle.art.Gallery;
import org.objectstyle.art.Painting;
import org.objectstyle.art.PaintingInfo;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;

public class CayenneDataObjectRelTst extends CayenneDOTestBase {

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
        TestCaseDataFactory.createArtistWithPainting(
            artistName,
            new String[] { paintingName },
            false);

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
        TestCaseDataFactory.createArtistWithPainting(
            artistName,
            new String[] { paintingName },
            false);

        Painting p1 = fetchPainting();
        Artist a1 = p1.getToArtist();

        assertNotNull(a1);
        assertEquals(PersistenceState.HOLLOW, a1.getPersistenceState());
        assertEquals(artistName, a1.getArtistName());
        assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
    }

    public void testReadToOneRel2() throws Exception {
        // test chained calls to read relationships
        TestCaseDataFactory.createArtistWithPainting(
            artistName,
            new String[] { paintingName },
            true);

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
        TestCaseDataFactory.createArtistWithPainting(
            artistName,
            new String[] { paintingName },
            false);

        Painting p1 = fetchPainting();
        Gallery g1 = p1.getToGallery();
        assertNull(g1);
    }

    public void testReadToManyRel1() throws Exception {
        TestCaseDataFactory.createArtistWithPainting(
            artistName,
            new String[] { paintingName },
            false);

        Artist a1 = fetchArtist();
        List plist = a1.getPaintingArray();

        assertNotNull(plist);
        assertEquals(1, plist.size());
        assertEquals(
            PersistenceState.COMMITTED,
            ((Painting) plist.get(0)).getPersistenceState());
        assertEquals(paintingName, ((Painting) plist.get(0)).getPaintingTitle());
    }

    public void testReadToManyRel2() throws Exception {
        // test empty relationship
        TestCaseDataFactory.createArtistWithPainting(artistName, new String[] {
        }, false);

        Artist a1 = fetchArtist();
        List plist = a1.getPaintingArray();

        assertNotNull(plist);
        assertEquals(0, plist.size());
    }

    public void testReflexiveRelationshipInsertOrder1() {
        DataContext dc = this.createDataContext();
        ArtGroup parentGroup = (ArtGroup) dc.createAndRegisterNewObject("ArtGroup");
        parentGroup.setName("parent");

        ArtGroup childGroup1 = (ArtGroup) dc.createAndRegisterNewObject("ArtGroup");
        childGroup1.setName("child1");
        childGroup1.setToParentGroup(parentGroup);
        dc.commitChanges();
    }

    public void testReflexiveRelationshipInsertOrder2() {
        //Create in a different order and see what happens
        DataContext dc = this.createDataContext();
        ArtGroup childGroup1 = (ArtGroup) dc.createAndRegisterNewObject("ArtGroup");
        childGroup1.setName("child1");

        ArtGroup parentGroup = (ArtGroup) dc.createAndRegisterNewObject("ArtGroup");
        parentGroup.setName("parent");

        childGroup1.setToParentGroup(parentGroup);

        dc.commitChanges();
    }

    public void testReflexiveRelationshipInsertOrder3() {
        //Tey multiple children, one created before parent, one after
        DataContext dc = this.createDataContext();
        ArtGroup childGroup1 = (ArtGroup) dc.createAndRegisterNewObject("ArtGroup");
        childGroup1.setName("child1");

        ArtGroup parentGroup = (ArtGroup) dc.createAndRegisterNewObject("ArtGroup");
        parentGroup.setName("parent");

        childGroup1.setToParentGroup(parentGroup);

        ArtGroup childGroup2 = (ArtGroup) dc.createAndRegisterNewObject("ArtGroup");
        childGroup2.setName("child2");
        childGroup2.setToParentGroup(parentGroup);

        dc.commitChanges();
    }

    public void testReflexiveRelationshipInsertOrder4() {
        //Tey multiple children, one created before parent, one after
        DataContext dc = this.createDataContext();
        ArtGroup childGroup1 = (ArtGroup) dc.createAndRegisterNewObject("ArtGroup");
        childGroup1.setName("child1");

        ArtGroup parentGroup = (ArtGroup) dc.createAndRegisterNewObject("ArtGroup");
        parentGroup.setName("parent");

        childGroup1.setToParentGroup(parentGroup);

        ArtGroup childGroup2 = (ArtGroup) dc.createAndRegisterNewObject("ArtGroup");
        childGroup2.setName("subchild");
        childGroup2.setToParentGroup(childGroup1);

        dc.commitChanges();
    }

    public void testCrossContextRelationshipException() {
        DataContext otherContext = getDomain().createDataContext();
        //Create this object in one context...
        Artist artist = (Artist) ctxt.createAndRegisterNewObject("Artist");
        //...and this object in another context
        Painting painting =
            (Painting) otherContext.createAndRegisterNewObject("Painting");

        //Check setting a toOne relationship
        try {
            painting.setToArtist(artist);
            fail("Should have failed to set a cross-context relationship");
        }
        catch (CayenneRuntimeException e) {
            //Fine.. it should  throw an exception
        }

        assertNull(painting.getToArtist()); //Make sure it wasn't set

        //Now try the reverse (toMany) relationship
        try {
            artist.addToPaintingArray(painting);
            fail("Should have failed to add a cross-context relationship");
        }
        catch (CayenneRuntimeException e) {
            //Fine.. it should  throw an exception
        }

        assertEquals(0, artist.getPaintingArray().size());

    }

    public void testComplexInsertUpdateOrdering() {
        Artist artist = (Artist) ctxt.createAndRegisterNewObject("Artist");
        artist.setArtistName("a name");

        ctxt.commitChanges();

        //Cause an update and an insert that need correct ordering
        Painting painting = (Painting) ctxt.createAndRegisterNewObject("Painting");
        painting.setPaintingTitle("a painting");
        artist.addToPaintingArray(painting);

        ctxt.commitChanges();

        ctxt.deleteObject(artist);
        ctxt.commitChanges();
    }

    private PaintingInfo fetchPaintingInfo(String name) {
        SelectQuery q =
            new SelectQuery(
                "PaintingInfo",
                ExpressionFactory.matchExp("painting.paintingTitle", name));
        List pts = ctxt.performQuery(q);
        return (pts.size() > 0) ? (PaintingInfo) pts.get(0) : null;
    }

    public void testToManyUnfaulted() throws Exception {
        Artist artist = (Artist) ctxt.createAndRegisterNewObject(Artist.class);
        artist.setArtistName("test");
        assertTrue(artist.readPropertyDirectly("paintingArray") instanceof Fault);

        ctxt.commitChanges();

        // check that to-many is still fault
        assertTrue(artist.readPropertyDirectly("paintingArray") instanceof Fault);
    }
}
