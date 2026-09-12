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

import java.util.Collection;
import java.util.Set;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataContextUncommittedObjectsIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private DataContext context;
    private TableHelper tArtist;

    @BeforeEach
    public void setUp() throws Exception {
        context = env.context();
        tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME");
    }

    @Test
    public void emptyContext() {
        assertTrue(context.newObjects().isEmpty());
        assertTrue(context.deletedObjects().isEmpty());
        assertTrue(context.modifiedObjects().isEmpty());
        assertTrue(context.uncommittedObjects().isEmpty());
    }

    @Test
    public void committedObjectsAreNotReported() throws Exception {
        tArtist.insert(1, "artist1");

        Artist artist = Cayenne.objectForPK(context, Artist.class, 1);
        assertEquals("artist1", artist.getArtistName());

        assertTrue(context.newObjects().isEmpty());
        assertTrue(context.deletedObjects().isEmpty());
        assertTrue(context.modifiedObjects().isEmpty());
        assertTrue(context.uncommittedObjects().isEmpty());
    }

    @Test
    public void newObjects() {
        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("a1");
        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("a2");

        assertContainsExactly(context.newObjects(), a1, a2);
        assertTrue(context.deletedObjects().isEmpty());
        assertTrue(context.modifiedObjects().isEmpty());
        assertContainsExactly(context.uncommittedObjects(), a1, a2);
    }

    @Test
    public void deletedObjects() throws Exception {
        tArtist.insert(1, "artist1");
        tArtist.insert(2, "artist2");

        Artist a1 = Cayenne.objectForPK(context, Artist.class, 1);
        Artist a2 = Cayenne.objectForPK(context, Artist.class, 2);
        context.deleteObject(a1);

        assertTrue(context.newObjects().isEmpty());
        assertContainsExactly(context.deletedObjects(), a1);
        assertTrue(context.modifiedObjects().isEmpty());
        assertContainsExactly(context.uncommittedObjects(), a1);
        assertEquals(PersistenceState.COMMITTED, a2.getPersistenceState());
    }

    @Test
    public void modifiedObjects() throws Exception {
        tArtist.insert(1, "artist1");
        tArtist.insert(2, "artist2");

        Artist a1 = Cayenne.objectForPK(context, Artist.class, 1);
        Cayenne.objectForPK(context, Artist.class, 2);
        a1.setArtistName("changed");

        assertTrue(context.newObjects().isEmpty());
        assertTrue(context.deletedObjects().isEmpty());
        assertContainsExactly(context.modifiedObjects(), a1);
        assertContainsExactly(context.uncommittedObjects(), a1);
    }

    @Test
    public void uncommittedObjectsCombinesAllStates() throws Exception {
        tArtist.insert(1, "artist1");
        tArtist.insert(2, "artist2");
        tArtist.insert(3, "artist3");

        Artist deleted = Cayenne.objectForPK(context, Artist.class, 1);
        Artist modified = Cayenne.objectForPK(context, Artist.class, 2);
        Artist committed = Cayenne.objectForPK(context, Artist.class, 3);
        assertEquals("artist3", committed.getArtistName());

        context.deleteObject(deleted);
        modified.setArtistName("changed");
        Artist created = context.newObject(Artist.class);
        created.setArtistName("new");

        assertContainsExactly(context.newObjects(), created);
        assertContainsExactly(context.deletedObjects(), deleted);
        assertContainsExactly(context.modifiedObjects(), modified);
        assertContainsExactly(context.uncommittedObjects(), created, deleted, modified);
    }

    @Test
    public void emptyAfterCommit() throws Exception {
        tArtist.insert(1, "artist1");

        Artist modified = Cayenne.objectForPK(context, Artist.class, 1);
        modified.setArtistName("changed");
        Artist created = context.newObject(Artist.class);
        created.setArtistName("new");

        assertEquals(2, context.uncommittedObjects().size());

        context.commitChanges();

        assertTrue(context.newObjects().isEmpty());
        assertTrue(context.deletedObjects().isEmpty());
        assertTrue(context.modifiedObjects().isEmpty());
        assertTrue(context.uncommittedObjects().isEmpty());
    }

    @Test
    public void emptyAfterRollback() throws Exception {
        tArtist.insert(1, "artist1");
        tArtist.insert(2, "artist2");

        Artist deleted = Cayenne.objectForPK(context, Artist.class, 1);
        Artist modified = Cayenne.objectForPK(context, Artist.class, 2);
        context.deleteObject(deleted);
        modified.setArtistName("changed");
        Artist created = context.newObject(Artist.class);
        created.setArtistName("new");

        assertEquals(3, context.uncommittedObjects().size());

        context.rollbackChanges();

        assertTrue(context.newObjects().isEmpty());
        assertTrue(context.deletedObjects().isEmpty());
        assertTrue(context.modifiedObjects().isEmpty());
        assertTrue(context.uncommittedObjects().isEmpty());
    }

    private static void assertContainsExactly(Collection<?> actual, Object... expected) {
        assertEquals(Set.of(expected), Set.copyOf(actual));
    }
}
