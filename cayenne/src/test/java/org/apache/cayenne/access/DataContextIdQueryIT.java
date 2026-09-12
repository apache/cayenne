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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.FaultFailureException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DataContextIdQueryIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private DataContext context;
    private TableHelper tArtist;

    @BeforeEach
    public void setUp() {
        context = env.context();
        tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME");
    }

    private static ObjectId artistId(int id) {
        return ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, id);
    }

    @Test
    public void registeredObject_NoQuery() throws Exception {
        tArtist.insert(1, "a1");

        Artist a = context.objectForPK(Artist.class, 1);
        assertEquals(1, tArtist.update().set("ARTIST_NAME", "a2").where("ARTIST_ID", 1).execute());

        env.runWithQueriesBlocked(() -> assertSame(a, context.objectForPK(artistId(1))));

        // a resolved object is returned as is, without a refresh
        assertEquals("a1", a.getArtistName());
    }

    @Test
    public void newObject() {
        Artist a = context.newObject(Artist.class);
        a.setArtistName("a1");

        env.runWithQueriesBlocked(() -> assertSame(a, context.objectForPK(a.getObjectId())));
    }

    @Test
    public void temporaryId() {
        env.runWithQueriesBlocked(() -> assertNull(context.objectForPK(ObjectId.of("Artist"))));
    }

    @Test
    public void unknownEntity() {
        assertThrows(CayenneRuntimeException.class, () -> context.objectForPK(ObjectId.of("Bogus", "ID", 1)));
        assertThrows(CayenneRuntimeException.class, () -> context.objectForPK("Bogus", Map.of("ID", 1)));
        assertThrows(CayenneRuntimeException.class, () -> context.objectForPK("Bogus", 1));
    }

    @Test
    public void noMatchingRow() {
        assertNull(context.objectForPK(artistId(1)));
    }

    @Test
    public void fromDB() throws Exception {
        tArtist.insert(1, "a1");

        Artist a = (Artist) context.objectForPK(artistId(1));
        assertNotNull(a);
        assertSame(context, a.getObjectContext());
        assertEquals(PersistenceState.COMMITTED, a.getPersistenceState());
        assertEquals("a1", a.getArtistName());
    }

    @Test
    public void fromSharedCache() throws Exception {
        tArtist.insert(1, "a1");

        // resolve in one context, so that the snapshot ends up in the shared cache
        context.objectForPK(Artist.class, 1);

        ObjectContext context2 = env.runtime().newContext();
        env.runWithQueriesBlocked(() -> {
            Artist a = (Artist) context2.objectForPK(artistId(1));
            assertNotNull(a);
            assertSame(context2, a.getObjectContext());
            assertEquals("a1", a.getArtistName());
        });
    }

    @Test
    public void hollowObject_FromDB() throws Exception {
        tArtist.insert(1, "a1");

        Artist a = (Artist) context.findOrCreateObject(artistId(1));
        assertEquals(PersistenceState.HOLLOW, a.getPersistenceState());

        assertSame(a, context.objectForPK(artistId(1)));
        assertEquals(PersistenceState.COMMITTED, a.getPersistenceState());
        assertEquals("a1", a.getArtistName());
    }

    @Test
    public void hollowObject_FromSharedCache() throws Exception {
        tArtist.insert(1, "a1");
        context.objectForPK(Artist.class, 1);

        DataContext context2 = (DataContext) env.runtime().newContext();
        Artist a = (Artist) context2.findOrCreateObject(artistId(1));
        assertEquals(PersistenceState.HOLLOW, a.getPersistenceState());

        env.runWithQueriesBlocked(() -> {
            // fault resolution goes via 'onIdQuery' as well
            assertEquals("a1", a.getArtistName());
            assertEquals(PersistenceState.COMMITTED, a.getPersistenceState());
        });
    }

    @Test
    public void hollowObject_NoMatchingRow() throws Exception {
        Artist a = (Artist) context.findOrCreateObject(artistId(1));
        assertEquals(PersistenceState.HOLLOW, a.getPersistenceState());

        assertNull(context.objectForPK(artistId(1)));
        assertThrows(FaultFailureException.class, a::getArtistName);
    }

    @Test
    public void nestedContext_FromDB() throws Exception {
        tArtist.insert(1, "a1");

        ObjectContext child = env.runtime().newContext(context);
        Artist a = (Artist) child.objectForPK(artistId(1));
        assertNotNull(a);
        assertSame(child, a.getObjectContext());
        assertEquals(PersistenceState.COMMITTED, a.getPersistenceState());
        assertEquals("a1", a.getArtistName());

        // the object was resolved via the parent, so the parent has it too
        assertNotNull(context.getGraphManager().getNode(artistId(1)));
    }

    @Test
    public void nestedContext_ParentState() throws Exception {
        tArtist.insert(1, "a1");

        Artist parentA = context.objectForPK(Artist.class, 1);
        parentA.setArtistName("a2");

        ObjectContext child = env.runtime().newContext(context);
        env.runWithQueriesBlocked(() -> {
            Artist a = (Artist) child.objectForPK(artistId(1));
            assertNotNull(a);
            assertSame(child, a.getObjectContext());

            // uncommitted parent state is the committed state from the child perspective
            assertEquals("a2", a.getArtistName());
            assertEquals(PersistenceState.COMMITTED, a.getPersistenceState());
        });
    }

    @Test
    public void nestedContext_NoMatchingRow() {
        ObjectContext child = env.runtime().newContext(context);
        assertNull(child.objectForPK(artistId(1)));
    }

    @Test
    public void refreshFromDB() throws Exception {
        tArtist.insert(1, "a1");

        Artist a = context.objectForPK(Artist.class, 1);
        assertEquals(1, tArtist.update().set("ARTIST_NAME", "a2").where("ARTIST_ID", 1).execute());

        // an id lookup never refreshes a registered object, an explicit select does
        Artist a1 = ObjectSelect.query(Artist.class).where(Artist.SELF.eqId(artistId(1))).selectOne(context);
        assertSame(a, a1);
        assertEquals("a2", a.getArtistName());
    }
}
