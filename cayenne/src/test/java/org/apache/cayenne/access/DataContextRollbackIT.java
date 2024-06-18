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
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextRollbackIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Inject
    private CayenneRuntime runtime;

    @Test
    public void testRollbackNew() {
        Artist artist = (Artist) context.newObject("Artist");
        artist.setArtistName("a");

        Painting p1 = (Painting) context.newObject("Painting");
        p1.setPaintingTitle("p1");
        p1.setToArtist(artist);

        Painting p2 = (Painting) context.newObject("Painting");
        p2.setPaintingTitle("p2");
        p2.setToArtist(artist);

        Painting p3 = (Painting) context.newObject("Painting");
        p3.setPaintingTitle("p3");
        p3.setToArtist(artist);

        // before:
        assertEquals(artist, p1.getToArtist());
        assertEquals(3, artist.getPaintingArray().size());

        context.rollbackChanges();

        // after:
        assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
    }

    @Test
    public void testRollbackNewObject() {
        String artistName = "revertTestArtist";
        Artist artist = (Artist) context.newObject("Artist");
        artist.setArtistName(artistName);

        context.rollbackChanges();

        assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
        context.commitChanges();
        // The commit should have made no changes, so
        // perform a fetch to ensure that this artist hasn't been persisted to the db

        DataContext freshContext = (DataContext) runtime.newContext();
        assertNotSame(this.context, freshContext);

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq(artistName));
        List<Artist> queryResults = query.select(freshContext);

        assertEquals(0, queryResults.size());
    }

    // Catches a bug where new objects were unregistered within an object iterator, thus
    // modifying the collection the iterator was iterating over
    // (ConcurrentModificationException)
    @Test
    public void testRollbackWithMultipleNewObjects() {
        String artistName = "rollbackTestArtist";
        String paintingTitle = "rollbackTestPainting";
        Artist artist = (Artist) context.newObject("Artist");
        artist.setArtistName(artistName);

        Painting painting = (Painting) context.newObject("Painting");
        painting.setPaintingTitle(paintingTitle);
        painting.setToArtist(artist);

        context.rollbackChanges();

        assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
        context.commitChanges();

        // The commit should have made no changes, so
        // perform a fetch to ensure that this artist hasn't been persisted to the db

        DataContext freshContext = (DataContext) runtime.newContext();
        assertNotSame(this.context, freshContext);

        List<?> queryResults = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq(artistName))
                .select(freshContext);

        assertEquals(0, queryResults.size());
    }

    @Test
    public void testRollbackRelationshipModification() {
        String artistName = "relationshipModArtist";
        String paintingTitle = "relationshipTestPainting";
        Artist artist = (Artist) context.newObject("Artist");
        artist.setArtistName(artistName);
        Painting painting = (Painting) context.newObject("Painting");
        painting.setPaintingTitle(paintingTitle);
        painting.setToArtist(artist);
        context.commitChanges();

        painting.setToArtist(null);
        assertEquals(0, artist.getPaintingArray().size());
        context.rollbackChanges();

        assertTrue(((ValueHolder) artist.getPaintingArray()).isFault());
        assertEquals(1, artist.getPaintingArray().size());
        assertEquals(artist, painting.getToArtist());

        // Check that the reverse relationship was handled
        assertEquals(1, artist.getPaintingArray().size());
        context.commitChanges();

        DataContext freshContext = (DataContext) runtime.newContext();
        assertNotSame(this.context, freshContext);

        List<?> queryResults = ObjectSelect.query(Painting.class)
                .where(Painting.PAINTING_TITLE.eq(paintingTitle))
                .select(freshContext);

        assertEquals(1, queryResults.size());
        Painting queriedPainting = (Painting) queryResults.get(0);

        // NB: This is an easier comparison than manually fetching artist
        assertEquals(artistName, queriedPainting.getToArtist().getArtistName());
    }

    @Test
    public void testRollbackDeletedObject() {
        String artistName = "deleteTestArtist";
        Artist artist = (Artist) context.newObject("Artist");
        artist.setArtistName(artistName);
        context.commitChanges();

        context.deleteObjects(artist);
        context.rollbackChanges();

        // Now check everything is as it should be
        assertEquals(PersistenceState.HOLLOW, artist.getPersistenceState());

        context.commitChanges();
        // The commit should have made no changes, so
        // perform a fetch to ensure that this artist hasn't been deleted from the db

        DataContext freshContext = (DataContext) runtime.newContext();
        assertNotSame(this.context, freshContext);

        List<?> queryResults = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq(artistName))
                .select(context);

        assertEquals(1, queryResults.size());
    }

    @Test
    public void testRollbackModifiedObject() {
        String artistName = "initialTestArtist";
        Artist artist = (Artist) context.newObject("Artist");
        artist.setArtistName(artistName);
        context.commitChanges();

        artist.setArtistName("a new value");

        context.rollbackChanges();

        // Make sure the inmemory changes have been rolled back
        assertEquals(artistName, artist.getArtistName());

        // Commit what's in memory...
        context.commitChanges();

        // .. and ensure that the correct data is in the db
        DataContext freshContext = (DataContext) runtime.newContext();
        assertNotSame(this.context, freshContext);

        List<?> queryResults = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq(artistName))
                .select(freshContext);

        assertEquals(1, queryResults.size());
    }
}
