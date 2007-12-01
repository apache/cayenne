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

package org.apache.cayenne.access;

import java.util.Collections;

import org.apache.art.Artist;
import org.apache.art.Painting;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.unit.CayenneCase;

public class ToManyListTest extends CayenneCase {

    protected DataContext context;

    protected void setUp() throws Exception {
        super.setUp();

        context = createDataContext();
    }

    private ToManyList createForNewArtist() {
        Artist artist = context.newObject(Artist.class);
        return new ToManyList(artist, Artist.PAINTING_ARRAY_PROPERTY);
    }

    private ToManyList createForExistingArtist() {
        Artist artist = context.newObject(Artist.class);
        artist.setArtistName("aa");
        context.commitChanges();
        return new ToManyList(artist, Artist.PAINTING_ARRAY_PROPERTY);
    }

    public void testNewAddRemove() throws Exception {
        ToManyList list = createForNewArtist();
        assertFalse("Expected resolved list when created with a new object", list
                .isFault());
        assertEquals(0, list.size());

        Painting p1 = context.newObject(Painting.class);
        list.add(p1);
        assertEquals(1, list.size());

        Painting p2 = context.newObject(Painting.class);
        list.add(p2);
        assertEquals(2, list.size());

        list.remove(p1);
        assertEquals(1, list.size());
    }

    public void testSavedUnresolvedAddRemove() throws Exception {
        ToManyList list = createForExistingArtist();

        // immediately tag Artist as MODIFIED, since we are messing up with relationship
        // bypassing normal CayenneDataObject methods
        ((Artist) list.getRelationshipOwner())
                .setPersistenceState(PersistenceState.MODIFIED);

        assertTrue("List must be unresolved for an existing object", list.isFault());

        Painting p1 = context.newObject(Painting.class);
        list.add(p1);
        assertTrue("List must be unresolved when adding an object...", list.isFault());
        assertTrue(list.addedToUnresolved.contains(p1));

        Painting p2 = context.newObject(Painting.class);
        list.add(p2);
        assertTrue("List must be unresolved when adding an object...", list.isFault());
        assertTrue(list.addedToUnresolved.contains(p2));

        list.remove(p1);
        assertTrue("List must be unresolved when removing an object...", list.isFault());
        assertFalse(list.addedToUnresolved.contains(p1));

        // now resolve
        int size = list.size();
        assertFalse("List must be resolved after checking a size...", list.isFault());
        assertEquals(1, size);
        assertTrue(list.objectList.contains(p2));
    }

    public void testSavedUnresolvedMerge() throws Exception {
        ToManyList list = createForExistingArtist();

        Painting p1 = context.newObject(Painting.class);
        p1.setPaintingTitle("p1");

        // list being tested is a separate copy from
        // the relationship list that Artist has, so adding a painting
        // here will not add the painting to the array being tested
        ((Artist) list.getRelationshipOwner()).addToPaintingArray(p1);
        context.commitChanges();

        // immediately tag Artist as MODIFIED, since we are messing up with relationship
        // bypassing normal CayenneDataObject methods
        ((Artist) list.getRelationshipOwner())
                .setPersistenceState(PersistenceState.MODIFIED);

        assertTrue("List must be unresolved...", list.isFault());
        list.add(p1);
        assertTrue("List must be unresolved when adding an object...", list.isFault());
        assertTrue(list.addedToUnresolved.contains(p1));

        Painting p2 = context.newObject(Painting.class);
        list.add(p2);
        assertTrue("List must be unresolved when adding an object...", list.isFault());
        assertTrue(list.addedToUnresolved.contains(p2));

        // now resolve the list and see how merge worked
        int size = list.size();
        assertFalse("List must be resolved after checking a size...", list.isFault());
        assertEquals(2, size);
        assertTrue(list.objectList.contains(p2));
        assertTrue(list.objectList.contains(p1));
    }

    public void testThrowOutDeleted() throws Exception {
        ToManyList list = createForExistingArtist();

        Painting p1 = context.newObject(Painting.class);
        p1.setPaintingTitle("p1");
        Painting p2 = context.newObject(Painting.class);
        p2.setPaintingTitle("p2");

        // list being tested is a separate copy from
        // the relationship list that Artist has, so adding a painting
        // here will not add the painting to the array being tested
        ((Artist) list.getRelationshipOwner()).addToPaintingArray(p1);
        ((Artist) list.getRelationshipOwner()).addToPaintingArray(p2);
        context.commitChanges();

        // immediately tag Artist as MODIFIED, since we are messing up with relationship
        // bypassing normal CayenneDataObject methods
        ((Artist) list.getRelationshipOwner())
                .setPersistenceState(PersistenceState.MODIFIED);

        assertTrue("List must be unresolved...", list.isFault());
        list.add(p1);
        list.add(p2);
        assertTrue("List must be unresolved when adding an object...", list.isFault());
        assertTrue(list.addedToUnresolved.contains(p2));
        assertTrue(list.addedToUnresolved.contains(p1));

        // now delete p2 and resolve list
        ((Artist) list.getRelationshipOwner()).removeFromPaintingArray(p2);
        context.deleteObject(p2);
        context.commitChanges();

        assertTrue("List must be unresolved...", list.isFault());
        assertTrue(
                "List must be unresolved when an object was deleted externally...",
                list.isFault());
        assertTrue(list.addedToUnresolved.contains(p2));
        assertTrue(list.addedToUnresolved.contains(p1));

        // now resolve the list and see how merge worked
        int size = list.size();
        assertFalse("List must be resolved after checking a size...", list.isFault());
        assertEquals("Deleted object must have been purged...", 1, size);
        assertTrue(list.objectList.contains(p1));
        assertFalse("Deleted object must have been purged...", list.objectList
                .contains(p2));
    }

    public void testRealRelationship() throws Exception {
        Artist artist = context.newObject(Artist.class);
        artist.setArtistName("aaa");

        Painting p1 = context.newObject(Painting.class);
        p1.setPaintingTitle("p1");

        context.commitChanges();
        context.invalidateObjects(Collections.singletonList(artist));

        ToManyList list = (ToManyList) artist.getPaintingArray();
        assertTrue("List must be unresolved...", list.isFault());

        Painting p2 = context.newObject(Painting.class);
        p2.setPaintingTitle("p2");

        artist.addToPaintingArray(p1);
        artist.addToPaintingArray(p2);
        assertTrue("List must be unresolved...", list.isFault());

        context.commitChanges();

        assertTrue("List must be unresolved...", list.isFault());

        int size = list.size();
        assertFalse("List must be resolved...", list.isFault());
        assertTrue(list.contains(p1));
        assertTrue(list.contains(p2));
        assertEquals(2, size);
    }

    public void testRealRelationshipRollback() throws Exception {
        Artist artist = context.newObject(Artist.class);
        artist.setArtistName("aaa");

        Painting p1 = context.newObject(Painting.class);
        p1.setPaintingTitle("p1");
        artist.addToPaintingArray(p1);
        context.commitChanges();
        context.invalidateObjects(Collections.singletonList(artist));

        ToManyList list = (ToManyList) artist.getPaintingArray();
        assertTrue("List must be unresolved...", list.isFault());

        Painting p2 = context.newObject(Painting.class);

        artist.addToPaintingArray(p2);
        assertTrue("List must be unresolved...", list.isFault());
        assertTrue(list.addedToUnresolved.contains(p2));

        context.rollbackChanges();

        assertTrue("List must be unresolved...", list.isFault());

        // call to "contains" must trigger list resolution
        assertTrue(list.contains(p1));
        assertFalse(list.contains(p2));
        assertFalse("List must be resolved...", list.isFault());
    }
}
