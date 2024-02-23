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

import org.apache.cayenne.access.ToManyList;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.ArtGroup;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.testdo.testmap.PaintingInfo;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class CayennePersistentObjectRelationshipsIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Inject
    private CayenneRuntime runtime;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tArtist;
    private TableHelper tPaintingInfo;
    private TableHelper tPainting;

    @Before
    public void setUp() throws Exception {
        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns(
                "PAINTING_ID",
                "PAINTING_TITLE",
                "ARTIST_ID");

        tPaintingInfo = new TableHelper(dbHelper, "PAINTING_INFO");
        tPaintingInfo.setColumns("PAINTING_ID", "TEXT_REVIEW");
    }

    private void createArtistWithPaintingDataSet() throws Exception {
        tArtist.insert(8, "aX");
        tPainting.insert(6, "pW", 8);
    }

    private void createArtistWithPaintingAndInfoDataSet() throws Exception {
        tArtist.insert(8, "aX");
        tPainting.insert(6, "pW", 8);
        tPaintingInfo.insert(6, "mE");
    }

    @Test
    public void testReadNestedProperty1() throws Exception {
        createArtistWithPaintingDataSet();

        Painting p1 = Cayenne.objectForPK(context, Painting.class, 6);
        assertEquals("aX", p1.readNestedProperty("toArtist.artistName"));
    }

    @Test
    public void testReadNestedProperty2() throws Exception {
        createArtistWithPaintingDataSet();

        Painting p1 = Cayenne.objectForPK(context, Painting.class, 6);
        assertTrue(p1.getToArtist().readNestedProperty("paintingArray") instanceof List<?>);
    }

    @Test
    public void testReciprocalRel1() throws Exception {
        createArtistWithPaintingDataSet();

        Painting p1 = Cayenne.objectForPK(context, Painting.class, 6);
        Artist a1 = p1.getToArtist();

        assertNotNull(a1);
        assertEquals("aX", a1.getArtistName());

        List<Painting> paintings = a1.getPaintingArray();
        assertEquals(1, paintings.size());
        Painting p2 = paintings.get(0);
        assertSame(p1, p2);
    }

    @Test
    public void testReadToOneRel1() throws Exception {
        createArtistWithPaintingDataSet();

        Painting p1 = Cayenne.objectForPK(context, Painting.class, 6);
        Artist a1 = p1.getToArtist();

        assertNotNull(a1);
        assertEquals(PersistenceState.HOLLOW, a1.getPersistenceState());
        assertEquals("aX", a1.getArtistName());
        assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
    }

    @Test
    public void testReadToOneRel2() throws Exception {
        // test chained calls to read relationships
        createArtistWithPaintingAndInfoDataSet();

        PaintingInfo pi1 = Cayenne.objectForPK(context, PaintingInfo.class, 6);
        Painting p1 = pi1.getPainting();
        p1.getPaintingTitle();

        Artist a1 = p1.getToArtist();

        assertNotNull(a1);
        assertEquals(PersistenceState.HOLLOW, a1.getPersistenceState());
        assertEquals("aX", a1.getArtistName());
        assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
    }

    @Test
    public void testReadToOneRel3() throws Exception {
        createArtistWithPaintingDataSet();

        Painting p1 = Cayenne.objectForPK(context, Painting.class, 6);
        Gallery g1 = p1.getToGallery();
        assertNull(g1);
    }

    @Test
    public void testReadToManyRel1() throws Exception {
        createArtistWithPaintingDataSet();

        Artist a1 = Cayenne.objectForPK(context, Artist.class, 8);
        List<Painting> plist = a1.getPaintingArray();

        assertNotNull(plist);
        assertEquals(1, plist.size());
        assertEquals(PersistenceState.COMMITTED, plist.get(0).getPersistenceState());
        assertEquals("pW", plist.get(0).getPaintingTitle());
    }

    @Test
    public void testReadToManyRel2() throws Exception {
        // test empty relationship
        tArtist.insert(11, "aX");

        Artist a1 = Cayenne.objectForPK(context, Artist.class, 11);
        List<Painting> plist = a1.getPaintingArray();

        assertNotNull(plist);
        assertEquals(0, plist.size());
    }

    @Test
    public void testReflexiveRelationshipInsertOrder1() {

        ArtGroup parentGroup = context.newObject(ArtGroup.class);
        parentGroup.setName("parent");

        ArtGroup childGroup1 = context.newObject(ArtGroup.class);
        childGroup1.setName("child1");
        childGroup1.setToParentGroup(parentGroup);
        context.commitChanges();

        childGroup1.setToParentGroup(null);
        context.commitChanges();
    }

    @Test
    public void testReflexiveRelationshipInsertOrder2() {

        ArtGroup childGroup1 = context.newObject(ArtGroup.class);
        childGroup1.setName("child1");

        ArtGroup parentGroup = context.newObject(ArtGroup.class);
        parentGroup.setName("parent");

        childGroup1.setToParentGroup(parentGroup);

        context.commitChanges();

        childGroup1.setToParentGroup(null);
        context.commitChanges();
    }

    @Test
    public void testReflexiveRelationshipInsertOrder3() {
        // multiple children, one created before parent, one after

        ArtGroup childGroup1 = context.newObject(ArtGroup.class);
        childGroup1.setName("child1");

        ArtGroup parentGroup = context.newObject(ArtGroup.class);
        parentGroup.setName("parent");

        childGroup1.setToParentGroup(parentGroup);

        ArtGroup childGroup2 = context.newObject(ArtGroup.class);
        childGroup2.setName("child2");
        childGroup2.setToParentGroup(parentGroup);

        context.commitChanges();

        childGroup1.setToParentGroup(null);
        context.commitChanges();

        childGroup2.setToParentGroup(null);
        context.commitChanges();
    }

    @Test
    public void testReflexiveRelationshipInsertOrder4() {
        // multiple children, one created before parent, one after

        ArtGroup childGroup1 = context.newObject(ArtGroup.class);
        childGroup1.setName("child1");

        ArtGroup parentGroup = context.newObject(ArtGroup.class);
        parentGroup.setName("parent");

        childGroup1.setToParentGroup(parentGroup);

        ArtGroup childGroup2 = context.newObject(ArtGroup.class);
        childGroup2.setName("subchild");
        childGroup2.setToParentGroup(childGroup1);

        context.commitChanges();

        childGroup1.setToParentGroup(null);
        context.commitChanges();

        childGroup2.setToParentGroup(null);
        context.commitChanges();
    }

    @Test
    public void testCrossContextRelationshipException() {

        // Create this object in one context...
        Artist artist = context.newObject(Artist.class);

        // ...and this object in another context
        Painting painting = runtime.newContext().newObject(Painting.class);

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

    @Test
    public void testComplexInsertUpdateOrdering() {
        Artist artist = context.newObject(Artist.class);
        artist.setArtistName("a name");

        context.commitChanges();

        // Cause an update and an insert that need correct ordering
        Painting painting = context.newObject(Painting.class);
        painting.setPaintingTitle("a painting");
        artist.addToPaintingArray(painting);

        context.commitChanges();

        context.deleteObjects(artist);
        context.commitChanges();
    }

    @Test
    public void testNewToMany() throws Exception {
        Artist artist = context.newObject(Artist.class);
        artist.setArtistName("test");
        assertTrue(artist.readPropertyDirectly("paintingArray") instanceof ToManyList);

        ToManyList list = (ToManyList) artist.readPropertyDirectly("paintingArray");
        assertFalse(list.isFault());

        context.commitChanges();

        assertFalse(list.isFault());
    }

    @Test
    public void testTransientInsertAndDeleteOfToManyRelationship() throws Exception {
        createArtistWithPaintingDataSet();

        Artist artist = ObjectSelect.query(Artist.class).selectOne(context);

        // create and then immediately delete a to-many relationship value
        Painting object2 = context.newObject(Painting.class);
        artist.addToPaintingArray(object2);
        artist.removeFromPaintingArray(object2);
        context.deleteObject(object2);
        assertEquals(1, artist.getPaintingArray().size());

        artist.setArtistName("updated artist name"); // this will force the commit to actually execute some SQL
        context.commitChanges();
    }
    
    @Test
    public void testTransientSetAndNullOfToOneRelationship() throws Exception {
        createArtistWithPaintingDataSet();

        Artist artist = ObjectSelect.query(Artist.class).selectOne(context);

        Painting object2 = context.newObject(Painting.class);
        object2.setPaintingTitle("Title");
        object2.setToArtist(artist);
        object2.setToArtist(null);
        context.commitChanges();
        
        context.invalidateObjects(object2);
        assertNull(object2.getToArtist());
    }
    
}
