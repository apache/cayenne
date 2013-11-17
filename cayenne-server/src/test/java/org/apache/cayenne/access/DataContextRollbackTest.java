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

import java.util.List;

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextRollbackTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private ServerRuntime serverRuntime;

    @Inject
    private DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");
    }

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

    public void testRollbackNewObject() {
        String artistName = "revertTestArtist";
        Artist artist = (Artist) context.newObject("Artist");
        artist.setArtistName(artistName);

        context.rollbackChanges();

        assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
        context.commitChanges();
        // The commit should have made no changes, so
        // perform a fetch to ensure that this artist hasn't been persisted to the db

        DataContext freshContext = (DataContext) serverRuntime.newContext();
        assertNotSame(this.context, freshContext);

        SelectQuery query = new SelectQuery(Artist.class);
        query.setQualifier(ExpressionFactory.matchExp("artistName", artistName));
        List<?> queryResults = freshContext.performQuery(query);

        assertEquals(0, queryResults.size());
    }

    // Catches a bug where new objects were unregistered within an object iterator, thus
    // modifying the collection the iterator was iterating over
    // (ConcurrentModificationException)
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

        DataContext freshContext = (DataContext) serverRuntime.newContext();
        assertNotSame(this.context, freshContext);

        SelectQuery query = new SelectQuery(Artist.class);
        query.setQualifier(ExpressionFactory.matchExp("artistName", artistName));
        List<?> queryResults = freshContext.performQuery(query);

        assertEquals(0, queryResults.size());
    }

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

        DataContext freshContext = (DataContext) serverRuntime.newContext();
        assertNotSame(this.context, freshContext);

        SelectQuery query = new SelectQuery(Painting.class);
        query.setQualifier(ExpressionFactory.matchExp("paintingTitle", paintingTitle));
        List<?> queryResults = freshContext.performQuery(query);

        assertEquals(1, queryResults.size());
        Painting queriedPainting = (Painting) queryResults.get(0);

        // NB: This is an easier comparison than manually fetching artist
        assertEquals(artistName, queriedPainting.getToArtist().getArtistName());
    }

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

        DataContext freshContext = (DataContext) serverRuntime.newContext();
        assertNotSame(this.context, freshContext);

        SelectQuery query = new SelectQuery(Artist.class);
        query.setQualifier(ExpressionFactory.matchExp("artistName", artistName));
        List<?> queryResults = freshContext.performQuery(query);

        assertEquals(1, queryResults.size());
    }

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
        DataContext freshContext = (DataContext) serverRuntime.newContext();
        assertNotSame(this.context, freshContext);

        SelectQuery query = new SelectQuery(Artist.class);
        query.setQualifier(ExpressionFactory.matchExp("artistName", artistName));
        List<?> queryResults = freshContext.performQuery(query);

        assertEquals(1, queryResults.size());
    }
}
