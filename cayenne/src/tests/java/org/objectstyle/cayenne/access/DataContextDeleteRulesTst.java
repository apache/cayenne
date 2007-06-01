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
package org.objectstyle.cayenne.access;

import org.objectstyle.art.ArtGroup;
import org.objectstyle.art.Artist;
import org.objectstyle.art.ArtistExhibit;
import org.objectstyle.art.Exhibit;
import org.objectstyle.art.Gallery;
import org.objectstyle.art.Painting;
import org.objectstyle.art.PaintingInfo;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.unit.CayenneTestCase;

// TODO: redefine all test cases in terms of entities in "relationships" map
// and merge this test case with DeleteRulesTst that inherits 
// from RelationshipTestCase.
public class DataContextDeleteRulesTst extends CayenneTestCase {

    private DataContext context;

    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();
        context = getDomain().createDataContext();
    }

    public void testNullifyToOne() {
        //ArtGroup toParentGroup
        ArtGroup parentGroup = (ArtGroup) context.createAndRegisterNewObject("ArtGroup");
        parentGroup.setName("Parent");

        ArtGroup childGroup = (ArtGroup) context.createAndRegisterNewObject("ArtGroup");
        childGroup.setName("Child");
        parentGroup.addToChildGroupsArray(childGroup);

        //Check to make sure that the relationships are both exactly correct
        // before starting.  We're not really testing this, but it is imperative
        // that it is correct before testing the real details.
        assertEquals(parentGroup, childGroup.getToParentGroup());
        assertTrue(parentGroup.getChildGroupsArray().contains(childGroup));

        //Always good to commit before deleting... bad things happen otherwise
        context.commitChanges();

        context.deleteObject(childGroup);

        //The things we are testing.
        assertFalse(parentGroup.getChildGroupsArray().contains(childGroup));
        //Although deleted, the property should be null (good cleanup policy)
        //assertNull(childGroup.getToParentGroup());

        //And be sure that the commit works afterwards, just for sanity
        context.commitChanges();
    }

    /**
     * Tests that deleting a source of a flattened relationship with
     * CASCADE rule results in deleting a join and a target.
     */
    public void testCascadeToManyFlattened() {
        // testing Artist.groupArray relationship
        ArtGroup aGroup = (ArtGroup) context.createAndRegisterNewObject(ArtGroup.class);
        aGroup.setName("Group Name");
        Artist anArtist = (Artist) context.createAndRegisterNewObject(Artist.class);
        anArtist.setArtistName("A Name");
        anArtist.addToGroupArray(aGroup);
        assertTrue(anArtist.getGroupArray().contains(aGroup));

        context.commitChanges();

        assertEquals(0, context.getObjectStore().flattenedDeletes.size());

        context.deleteObject(anArtist);

        assertEquals(PersistenceState.DELETED, aGroup.getPersistenceState());
        assertFalse(anArtist.getGroupArray().contains(aGroup));
        assertEquals(1, context.getObjectStore().flattenedDeletes.size());
        context.commitChanges();
    }

    /**
     * Tests that deleting a source of a flattened relationship with
     * NULLIFY rule results in deleting a join together with the object
     * deleted. 
     */
    public void testNullifyToManyFlattened() {
        // testing ArtGroup.artistArray relationship
        ArtGroup aGroup = (ArtGroup) context.createAndRegisterNewObject(ArtGroup.class);
        aGroup.setName("Group Name");
        Artist anArtist = (Artist) context.createAndRegisterNewObject(Artist.class);
        anArtist.setArtistName("A Name");
        aGroup.addToArtistArray(anArtist);

        context.commitChanges();

        // Preconditions 
        assertTrue(aGroup.getArtistArray().contains(anArtist));
        assertTrue(anArtist.getGroupArray().contains(aGroup));
        assertEquals(0, context.getObjectStore().flattenedDeletes.size());

        context.deleteObject(aGroup);

        //The things to test
        assertFalse(anArtist.getGroupArray().contains(aGroup));
        assertEquals(1, context.getObjectStore().flattenedDeletes.size());

        context.commitChanges();
    }

    public void testNullifyToMany() {
        //ArtGroup childGroupsArray
        ArtGroup parentGroup = (ArtGroup) context.createAndRegisterNewObject("ArtGroup");
        parentGroup.setName("Parent");

        ArtGroup childGroup = (ArtGroup) context.createAndRegisterNewObject("ArtGroup");
        childGroup.setName("Child");
        parentGroup.addToChildGroupsArray(childGroup);

        //Preconditions - good to check to be sure
        assertEquals(parentGroup, childGroup.getToParentGroup());
        assertTrue(parentGroup.getChildGroupsArray().contains(childGroup));

        context.commitChanges();

        context.deleteObject(parentGroup);

        //The things we are testing.
        assertNull(childGroup.getToParentGroup());

        //Although deleted, the property should be null (good cleanup policy)
        //assertFalse(parentGroup.getChildGroupsArray().contains(childGroup));
        context.commitChanges();
    }

    public void testCascadeToOne() {
        //Painting toPaintingInfo
        Painting painting = (Painting) context.createAndRegisterNewObject("Painting");
        painting.setPaintingTitle("A Title");

        PaintingInfo info =
            (PaintingInfo) context.createAndRegisterNewObject("PaintingInfo");
        painting.setToPaintingInfo(info);

        //Must commit before deleting.. this relationship is dependent,
        // and everything must be committed for certain things to work.
        context.commitChanges();

        context.deleteObject(painting);

        //info must also be deleted
        assertEquals(PersistenceState.DELETED, info.getPersistenceState());
        assertNull(info.getPainting());
        assertNull(painting.getToPaintingInfo());
        context.commitChanges();
    }

    public void testCascadeToMany() {
        //Artist artistExhibitArray
        Artist anArtist = (Artist) context.createAndRegisterNewObject("Artist");
        anArtist.setArtistName("A Name");
        Exhibit anExhibit = (Exhibit) context.createAndRegisterNewObject("Exhibit");
        anExhibit.setClosingDate(new java.sql.Timestamp(System.currentTimeMillis()));
        anExhibit.setOpeningDate(new java.sql.Timestamp(System.currentTimeMillis()));

        //Needs a gallery... required for data integrity
        Gallery gallery = (Gallery) context.createAndRegisterNewObject("Gallery");
        gallery.setGalleryName("A Name");

        anExhibit.setToGallery(gallery);

        ArtistExhibit artistExhibit =
            (ArtistExhibit) context.createAndRegisterNewObject("ArtistExhibit");

        artistExhibit.setToArtist(anArtist);
        artistExhibit.setToExhibit(anExhibit);
        context.commitChanges();

        context.deleteObject(anArtist);

        //Test that the link record was deleted, and removed from the relationship
        assertEquals(PersistenceState.DELETED, artistExhibit.getPersistenceState());
        assertFalse(anArtist.getArtistExhibitArray().contains(artistExhibit));
        context.commitChanges();
    }

    public void testDenyToMany() {
        //Gallery paintingArray
        Gallery gallery = (Gallery) context.createAndRegisterNewObject("Gallery");
        gallery.setGalleryName("A Name");
        Painting painting = (Painting) context.createAndRegisterNewObject("Painting");
        painting.setPaintingTitle("A Title");
        gallery.addToPaintingArray(painting);
        context.commitChanges();

        try {
            context.deleteObject(gallery);
            fail("Should have thrown an exception");
        }
        catch (Exception e) {
            //GOOD!
        }
        context.commitChanges();
    }
}
