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

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.apache.cayenne.util.PersistentObjectList;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ToManyListIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private ToManyList createForNewArtist() {
        Artist artist = env.context().newObject(Artist.class);
        return new ToManyList(artist, Artist.PAINTING_ARRAY.getName());
    }

    private ToManyList createForExistingArtist() {
        Artist artist = env.context().newObject(Artist.class);
        artist.setArtistName("aa");
        env.context().commitChanges();
        return new ToManyList(artist, Artist.PAINTING_ARRAY.getName());
    }

    @Test
    public void newAddRemove() throws Exception {
        ToManyList list = createForNewArtist();
        assertFalse(list.isFault(), "Expected resolved list when created with a new object");
        assertEquals(0, list.size());

        Painting p1 = env.context().newObject(Painting.class);
        list.add(p1);
        assertEquals(1, list.size());

        Painting p2 = env.context().newObject(Painting.class);
        list.add(p2);
        assertEquals(2, list.size());

        list.remove(p1);
        assertEquals(1, list.size());
    }

    @Test
    public void savedUnresolvedAddRemove() throws Exception {
        ToManyList list = createForExistingArtist();

        // immediately tag Artist as MODIFIED, since we are messing up with relationship
        // bypassing normal PersistentObject methods
        list.getRelationshipOwner().setPersistenceState(PersistenceState.MODIFIED);

        assertTrue(list.isFault(), "List must be unresolved for an existing object");

        Painting p1 = env.context().newObject(Painting.class);
        list.add(p1);
        assertTrue(list.isFault(), "List must be unresolved when adding an object...");
        assertTrue(addedToUnresolved(list).contains(p1));

        Painting p2 = env.context().newObject(Painting.class);
        list.add(p2);
        assertTrue(list.isFault(), "List must be unresolved when adding an object...");
        assertTrue(addedToUnresolved(list).contains(p2));

        list.remove(p1);
        assertTrue(list.isFault(), "List must be unresolved when removing an object...");
        assertFalse(addedToUnresolved(list).contains(p1));

        // now resolve
        int size = list.size();
        assertFalse(list.isFault(), "List must be resolved after checking a size...");
        assertEquals(1, size);
        assertTrue(getValue(list).contains(p2));
    }

    @Test
    public void savedUnresolvedMerge() throws Exception {
        ToManyList list = createForExistingArtist();

        Painting p1 = env.context().newObject(Painting.class);
        p1.setPaintingTitle("p1");

        // list being tested is a separate copy from
        // the relationship list that Artist has, so adding a painting
        // here will not add the painting to the array being tested
        ((Artist) list.getRelationshipOwner()).addToPaintingArray(p1);
        env.context().commitChanges();

        // immediately tag Artist as MODIFIED, since we are messing up with relationship
        // bypassing normal PersistentObject methods
        list.getRelationshipOwner().setPersistenceState(PersistenceState.MODIFIED);

        assertTrue(list.isFault(), "List must be unresolved...");
        list.add(p1);
        assertTrue(list.isFault(), "List must be unresolved when adding an object...");
        assertTrue(addedToUnresolved(list).contains(p1));

        Painting p2 = env.context().newObject(Painting.class);
        list.add(p2);
        assertTrue(list.isFault(), "List must be unresolved when adding an object...");
        assertTrue(addedToUnresolved(list).contains(p2));

        // now resolve the list and see how merge worked
        int size = list.size();
        assertFalse(list.isFault(), "List must be resolved after checking a size...");
        assertEquals(2, size);
        assertTrue(getValue(list).contains(p2));
        assertTrue(getValue(list).contains(p1));
    }

    @Test
    public void throwOutDeleted() throws Exception {
        ToManyList list = createForExistingArtist();

        Painting p1 = env.context().newObject(Painting.class);
        p1.setPaintingTitle("p1");
        Painting p2 = env.context().newObject(Painting.class);
        p2.setPaintingTitle("p2");

        // list being tested is a separate copy from
        // the relationship list that Artist has, so adding a painting
        // here will not add the painting to the array being tested
        ((Artist) list.getRelationshipOwner()).addToPaintingArray(p1);
        ((Artist) list.getRelationshipOwner()).addToPaintingArray(p2);
        env.context().commitChanges();

        // immediately tag Artist as MODIFIED, since we are messing up with relationship
        // bypassing normal PersistentObject methods
        list.getRelationshipOwner().setPersistenceState(PersistenceState.MODIFIED);

        assertTrue(list.isFault(), "List must be unresolved...");
        list.add(p1);
        list.add(p2);
        assertTrue(list.isFault(), "List must be unresolved when adding an object...");
        assertTrue(addedToUnresolved(list).contains(p2));
        assertTrue(addedToUnresolved(list).contains(p1));

        // now delete p2 and resolve list
        ((Artist) list.getRelationshipOwner()).removeFromPaintingArray(p2);
        env.context().deleteObjects(p2);
        env.context().commitChanges();

        assertTrue(list.isFault(), "List must be unresolved...");
        assertTrue(
                list.isFault(),
                "List must be unresolved when an object was deleted externally...");
        assertTrue(addedToUnresolved(list).contains(p2));
        assertTrue(addedToUnresolved(list).contains(p1));

        // now resolve the list and see how merge worked
        int size = list.size();
        assertFalse(list.isFault(), "List must be resolved after checking a size...");
        assertEquals(1, size, "Deleted object must have been purged...");
        assertTrue(getValue(list).contains(p1));
        assertFalse(getValue(list).contains(p2), "Deleted object must have been purged...");
    }

    @Test
    public void realRelationship() throws Exception {
        Artist artist = env.context().newObject(Artist.class);
        artist.setArtistName("aaa");

        Painting p1 = env.context().newObject(Painting.class);
        p1.setPaintingTitle("p1");

        env.context().commitChanges();
        env.context().invalidateObjects(artist);

        ToManyList list = (ToManyList) artist.getPaintingArray();
        assertTrue(list.isFault(), "List must be unresolved...");

        Painting p2 = env.context().newObject(Painting.class);
        p2.setPaintingTitle("p2");

        artist.addToPaintingArray(p1);
        artist.addToPaintingArray(p2);
        assertTrue(list.isFault(), "List must be unresolved...");

        env.context().commitChanges();

        assertTrue(list.isFault(), "List must be unresolved...");

        int size = list.size();
        assertFalse(list.isFault(), "List must be resolved...");
        assertTrue(list.contains(p1));
        assertTrue(list.contains(p2));
        assertEquals(2, size);
    }

    @Test
    public void realRelationshipRollback() throws Exception {
        Artist artist = env.context().newObject(Artist.class);
        artist.setArtistName("aaa");

        Painting p1 = env.context().newObject(Painting.class);
        p1.setPaintingTitle("p1");
        artist.addToPaintingArray(p1);
        env.context().commitChanges();
        env.context().invalidateObjects(artist);

        ToManyList list = (ToManyList) artist.getPaintingArray();
        assertTrue(list.isFault(), "List must be unresolved...");

        Painting p2 = env.context().newObject(Painting.class);

        artist.addToPaintingArray(p2);
        assertTrue(list.isFault(), "List must be unresolved...");
        assertTrue(addedToUnresolved(list).contains(p2));

        env.context().rollbackChanges();

        assertTrue(list.isFault(), "List must be unresolved...");

        // call to "contains" must trigger list resolution
        assertTrue(list.contains(p1));
        assertFalse(list.contains(p2));
        assertFalse(list.isFault(), "List must be resolved...");
    }

    private List<?> getValue(ToManyList list) {
        return (List<?>) list.getValueDirectly();
    }

    private LinkedList<?> addedToUnresolved(ToManyList list) throws Exception {
        Field f = PersistentObjectList.class.getDeclaredField("addedToUnresolved");
        f.setAccessible(true);
        return (LinkedList<?>) f.get(list);
    }
}
