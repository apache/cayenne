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

package org.apache.cayenne.access;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Types;
import java.util.List;

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.testdo.testmap.ArtGroup;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.ArtistExhibit;
import org.apache.cayenne.testdo.testmap.Exhibit;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.testdo.testmap.PaintingInfo;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

// TODO: redefine all test cases in terms of entities in "relationships" map
// and merge this test case with DeleteRulesTst that inherits
// from RelationshipTestCase.
public class DataContextDeleteRulesIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private DataContext context;

    @BeforeEach
    public void cleanUpDB() throws Exception {
        // Break ARTGROUP self-referencing FK before DB clean, then re-clean
        env.table("ARTGROUP").update().set("PARENT_GROUP_ID", null, Types.INTEGER).execute();
        env.dbCleaner().clean();
        context = env.context();
    }

    @Test
    public void nullifyToOne() {
        // ArtGroup parentGroup
        ArtGroup parentGroup = (ArtGroup) context.newObject("ArtGroup");
        parentGroup.setName("Parent");

        ArtGroup childGroup = (ArtGroup) context.newObject("ArtGroup");
        childGroup.setName("Child");
        parentGroup.addToChildGroups(childGroup);

        // Check to make sure that the relationships are both exactly correct
        // before starting. We're not really testing this, but it is imperative
        // that it is correct before testing the real details.
        assertEquals(parentGroup, childGroup.getParentGroup());
        assertTrue(parentGroup.getChildGroups().contains(childGroup));

        // Always good to commit before deleting... bad things happen otherwise
        context.commitChanges();

        context.deleteObjects(childGroup);

        // The things we are testing.
        assertFalse(parentGroup.getChildGroups().contains(childGroup));
        // Although deleted, the property should be null (good cleanup policy)
        // assertNull(childGroup.getParentGroup());

        // And be sure that the commit works afterwards, just for sanity
        context.commitChanges();
    }

    /**
     * Tests that deleting a source of a flattened relationship with CASCADE
     * rule results in deleting a join and a target.
     */
    @Test
    public void cascadeToManyFlattened() {
        // testing Artist.groups relationship
        ArtGroup aGroup = context.newObject(ArtGroup.class);
        aGroup.setName("Group Name");
        Artist anArtist = context.newObject(Artist.class);
        anArtist.setArtistName("A Name");
        anArtist.addToGroups(aGroup);
        assertTrue(anArtist.getGroups().contains(aGroup));

        context.commitChanges();

        SQLTemplate checkQuery = new SQLTemplate(Artist.class, "SELECT * FROM ARTIST_GROUP");
        checkQuery.setFetchingDataRows(true);
        List<?> joins1 = context.select(checkQuery);
        assertEquals(1, joins1.size());

        context.deleteObjects(anArtist);

        assertEquals(PersistenceState.DELETED, aGroup.getPersistenceState());
        assertFalse(anArtist.getGroups().contains(aGroup));
        context.commitChanges();

        List<?> joins2 = context.select(checkQuery);
        assertEquals(0, joins2.size());
    }

    /**
     * Tests that deleting a source of a flattened relationship with NULLIFY
     * rule results in deleting a join together with the object deleted.
     */
    @Test
    public void nullifyToManyFlattened() {
        // testing ArtGroup.artists relationship
        ArtGroup aGroup = context.newObject(ArtGroup.class);
        aGroup.setName("Group Name");
        Artist anArtist = context.newObject(Artist.class);
        anArtist.setArtistName("A Name");
        aGroup.addToArtists(anArtist);

        context.commitChanges();

        // Preconditions
        assertTrue(aGroup.getArtists().contains(anArtist));
        assertTrue(anArtist.getGroups().contains(aGroup));

        SQLTemplate checkQuery = new SQLTemplate(Artist.class, "SELECT * FROM ARTIST_GROUP");
        checkQuery.setFetchingDataRows(true);
        List<?> joins1 = context.select(checkQuery);
        assertEquals(1, joins1.size());

        context.deleteObjects(aGroup);
        assertFalse(anArtist.getGroups().contains(aGroup));
        context.commitChanges();

        List<?> joins2 = context.select(checkQuery);
        assertEquals(0, joins2.size());
    }

    @Test
    public void nullifyToMany() {
        // ArtGroup childGroups
        ArtGroup parentGroup = (ArtGroup) context.newObject("ArtGroup");
        parentGroup.setName("Parent");

        ArtGroup childGroup = (ArtGroup) context.newObject("ArtGroup");
        childGroup.setName("Child");
        parentGroup.addToChildGroups(childGroup);

        // Preconditions - good to check to be sure
        assertEquals(parentGroup, childGroup.getParentGroup());
        assertTrue(parentGroup.getChildGroups().contains(childGroup));

        context.commitChanges();

        context.deleteObjects(parentGroup);

        // The things we are testing.
        assertNull(childGroup.getParentGroup());

        // Although deleted, the property should be null (good cleanup policy)
        // assertFalse(parentGroup.getChildGroups().contains(childGroup));
        context.commitChanges();
    }

    @Test
    public void cascadeToOne() {
        // Painting paintingInfo
        Painting painting = (Painting) context.newObject("Painting");
        painting.setPaintingTitle("A Title");

        PaintingInfo info = (PaintingInfo) context.newObject("PaintingInfo");
        painting.setPaintingInfo(info);

        // Must commit before deleting.. this relationship is dependent,
        // and everything must be committed for certain things to work
        context.commitChanges();

        context.deleteObjects(painting);

        // info must also be deleted
        assertEquals(PersistenceState.DELETED, info.getPersistenceState());
        assertNull(info.getPainting());
        assertNull(painting.getPaintingInfo());
        context.commitChanges();
    }

    @Test
    public void cascadeToMany() {
        // Artist artistExhibits
        Artist anArtist = (Artist) context.newObject("Artist");
        anArtist.setArtistName("A Name");
        Exhibit anExhibit = (Exhibit) context.newObject("Exhibit");
        anExhibit.setClosingDate(new java.sql.Timestamp(System.currentTimeMillis()));
        anExhibit.setOpeningDate(new java.sql.Timestamp(System.currentTimeMillis()));

        // Needs a gallery... required for data integrity
        Gallery gallery = (Gallery) context.newObject("Gallery");
        gallery.setGalleryName("A Name");

        anExhibit.setGallery(gallery);

        ArtistExhibit artistExhibit = (ArtistExhibit) context.newObject("ArtistExhibit");

        artistExhibit.setArtist(anArtist);
        artistExhibit.setExhibit(anExhibit);
        context.commitChanges();

        context.deleteObjects(anArtist);

        // Test that the link record was deleted, and removed from the
        // relationship
        assertEquals(PersistenceState.DELETED, artistExhibit.getPersistenceState());
        assertFalse(anArtist.getArtistExhibits().contains(artistExhibit));
        context.commitChanges();
    }

    @Test
    public void denyToMany() {
        // Gallery paintings
        Gallery gallery = (Gallery) context.newObject("Gallery");
        gallery.setGalleryName("A Name");
        Painting painting = (Painting) context.newObject("Painting");
        painting.setPaintingTitle("A Title");
        gallery.addToPaintings(painting);
        context.commitChanges();

        assertThrows(Exception.class, () -> context.deleteObjects(gallery));
        context.commitChanges();
    }
}
