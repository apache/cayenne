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

import java.util.List;

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataContextRollbackIT  {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void rollbackNew() {
        Artist artist = (Artist) env.context().newObject("Artist");
        artist.setArtistName("a");

        Painting p1 = (Painting) env.context().newObject("Painting");
        p1.setPaintingTitle("p1");
        p1.setToArtist(artist);

        Painting p2 = (Painting) env.context().newObject("Painting");
        p2.setPaintingTitle("p2");
        p2.setToArtist(artist);

        Painting p3 = (Painting) env.context().newObject("Painting");
        p3.setPaintingTitle("p3");
        p3.setToArtist(artist);

        // before:
        assertEquals(artist, p1.getToArtist());
        assertEquals(3, artist.getPaintingArray().size());

        env.context().rollbackChanges();

        // after:
        assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
    }

    @Test
    public void rollbackNewObject() {
        String artistName = "revertTestArtist";
        Artist artist = (Artist) env.context().newObject("Artist");
        artist.setArtistName(artistName);

        env.context().rollbackChanges();

        assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
        env.context().commitChanges();
        // The commit should have made no changes, so
        // perform a fetch to ensure that this artist hasn't been persisted to the db

        DataContext freshContext = (DataContext) env.runtime().newContext();
        assertNotSame(env.context(), freshContext);

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq(artistName));
        List<Artist> queryResults = query.select(freshContext);

        assertEquals(0, queryResults.size());
    }

    // Catches a bug where new objects were unregistered within an object iterator, thus
    // modifying the collection the iterator was iterating over
    // (ConcurrentModificationException)
    @Test
    public void rollbackWithMultipleNewObjects() {
        String artistName = "rollbackTestArtist";
        String paintingTitle = "rollbackTestPainting";
        Artist artist = (Artist) env.context().newObject("Artist");
        artist.setArtistName(artistName);

        Painting painting = (Painting) env.context().newObject("Painting");
        painting.setPaintingTitle(paintingTitle);
        painting.setToArtist(artist);

        env.context().rollbackChanges();

        assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
        env.context().commitChanges();

        // The commit should have made no changes, so
        // perform a fetch to ensure that this artist hasn't been persisted to the db

        DataContext freshContext = (DataContext) env.runtime().newContext();
        assertNotSame(env.context(), freshContext);

        List<?> queryResults = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq(artistName))
                .select(freshContext);

        assertEquals(0, queryResults.size());
    }

    @Test
    public void rollbackRelationshipModification() {
        String artistName = "relationshipModArtist";
        String paintingTitle = "relationshipTestPainting";
        Artist artist = (Artist) env.context().newObject("Artist");
        artist.setArtistName(artistName);
        Painting painting = (Painting) env.context().newObject("Painting");
        painting.setPaintingTitle(paintingTitle);
        painting.setToArtist(artist);
        env.context().commitChanges();

        painting.setToArtist(null);
        assertEquals(0, artist.getPaintingArray().size());
        env.context().rollbackChanges();

        assertTrue(((ValueHolder) artist.getPaintingArray()).isFault());
        assertEquals(1, artist.getPaintingArray().size());
        assertEquals(artist, painting.getToArtist());

        // Check that the reverse relationship was handled
        assertEquals(1, artist.getPaintingArray().size());
        env.context().commitChanges();

        DataContext freshContext = (DataContext) env.runtime().newContext();
        assertNotSame(env.context(), freshContext);

        List<?> queryResults = ObjectSelect.query(Painting.class)
                .where(Painting.PAINTING_TITLE.eq(paintingTitle))
                .select(freshContext);

        assertEquals(1, queryResults.size());
        Painting queriedPainting = (Painting) queryResults.get(0);

        // NB: This is an easier comparison than manually fetching artist
        assertEquals(artistName, queriedPainting.getToArtist().getArtistName());
    }

    @Test
    public void rollbackDeletedObject() {
        String artistName = "deleteTestArtist";
        Artist artist = (Artist) env.context().newObject("Artist");
        artist.setArtistName(artistName);
        env.context().commitChanges();

        env.context().deleteObjects(artist);
        env.context().rollbackChanges();

        // Now check everything is as it should be
        assertEquals(PersistenceState.HOLLOW, artist.getPersistenceState());

        env.context().commitChanges();
        // The commit should have made no changes, so
        // perform a fetch to ensure that this artist hasn't been deleted from the db

        DataContext freshContext = (DataContext) env.runtime().newContext();
        assertNotSame(env.context(), freshContext);

        List<?> queryResults = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq(artistName))
                .select(env.context());

        assertEquals(1, queryResults.size());
    }

    @Test
    public void rollbackModifiedObject() {
        String artistName = "initialTestArtist";
        Artist artist = (Artist) env.context().newObject("Artist");
        artist.setArtistName(artistName);
        env.context().commitChanges();

        artist.setArtistName("a new value");

        env.context().rollbackChanges();

        // Make sure the inmemory changes have been rolled back
        assertEquals(artistName, artist.getArtistName());

        // Commit what's in memory...
        env.context().commitChanges();

        // .. and ensure that the correct data is in the db
        DataContext freshContext = (DataContext) env.runtime().newContext();
        assertNotSame(env.context(), freshContext);

        List<?> queryResults = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq(artistName))
                .select(freshContext);

        assertEquals(1, queryResults.size());
    }
}
