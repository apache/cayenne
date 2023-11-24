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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.tx.BaseTransaction;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextIteratedQueryIT extends RuntimeCase {

    @Inject
    protected DBHelper dbHelper;
    @Inject
    private DataContext context;
    private TableHelper tArtist;
    private TableHelper tExhibit;
    private TableHelper tGallery;
    private TableHelper tPainting;

    @Before
    public void before() throws Exception {
        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tExhibit = new TableHelper(dbHelper, "EXHIBIT");
        tExhibit.setColumns("EXHIBIT_ID", "GALLERY_ID", "OPENING_DATE", "CLOSING_DATE");

        tGallery = new TableHelper(dbHelper, "GALLERY");
        tGallery.setColumns("GALLERY_ID", "GALLERY_NAME");

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns("PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "ESTIMATED_PRICE");
    }

    private void createArtistsDataSet() throws Exception {
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
        tArtist.insert(33003, "artist3");
        tArtist.insert(33004, "artist4");
        tArtist.insert(33005, "artist5");
        tArtist.insert(33006, "artist11");
        tArtist.insert(33007, "artist21");
    }

    protected void createArtistsAndPaintingsDataSet() throws Exception {
        createArtistsDataSet();

        tPainting.insert(33001, "P_artist1", 33001, 1000);
        tPainting.insert(33002, "P_artist2", 33002, 2000);
        tPainting.insert(33003, "P_artist3", 33003, 3000);
        tPainting.insert(33004, "P_artist4", 33004, 4000);
        tPainting.insert(33005, "P_artist5", 33005, 5000);
        tPainting.insert(33006, "P_artist11", 33006, 11000);
        tPainting.insert(33007, "P_artist21", 33007, 21000);
    }

    private void createLargeArtistsDataSet() throws Exception {
        for (int i = 1; i <= 20; i++) {
            tArtist.insert(i, "artist" + i);
        }
    }

    @Test
    public void testIterate() throws Exception {
        createArtistsDataSet();

        final int[] count = new int[1];
        ObjectSelect.query(Artist.class).iterate(context, object -> {
            assertNotNull(object.getArtistName());
            count[0]++;
        });

        assertEquals(7, count[0]);
    }

    @Test
    public void testIterateDataRows() throws Exception {
        createArtistsDataSet();

        final int[] count = new int[1];
        ObjectSelect.dataRowQuery(Artist.class).iterate(context, object -> {
            assertNotNull(object.get("ARTIST_ID"));
            count[0]++;
        });

        assertEquals(7, count[0]);
    }

    @Test
    public void testIterator() throws Exception {
        createArtistsDataSet();

        try (ResultIterator<Artist> it = ObjectSelect.query(Artist.class).iterator(context)) {
            int count = 0;

            for (Artist a : it) {
                count++;
            }

            assertEquals(7, count);
        }
    }

    @Test
    public void testBatchIterator() throws Exception {
        createLargeArtistsDataSet();

        try (ResultBatchIterator<Artist> it = ObjectSelect.query(Artist.class).batchIterator(context, 5)) {
            int count = 0;

            for (List<Artist> artistList : it) {
                count++;
                assertEquals(5, artistList.size());
            }

            assertEquals(4, count);
        }
    }


    @Test
    public void testPerformIteratedQuery_Count() throws Exception {
        createArtistsDataSet();

        try (ResultIterator<?> it = context.performIteratedQuery(ObjectSelect.query(Artist.class))) {
            int count = 0;
            while (it.hasNextRow()) {
                it.nextRow();
                count++;
            }

            assertEquals(7, count);
        }
    }

    @Test
    public void testPerformIteratedQuery_resolve() throws Exception {
        createArtistsAndPaintingsDataSet();

        try (ResultIterator<?> it = context.performIteratedQuery(ObjectSelect.query(Artist.class))) {
            while (it.hasNextRow()) {
                DataRow row = (DataRow) it.nextRow();

                // try instantiating an object and fetching its relationships
                Artist artist = context.objectFromDataRow(Artist.class, row);
                List<Painting> paintings = artist.getPaintingArray();
                assertNotNull(paintings);
                assertEquals("Expected one painting for artist: " + artist, 1, paintings.size());
            }
        }
    }

    @Test
    public void testContextIterator() throws Exception {
        createArtistsAndPaintingsDataSet();
        try (ResultIterator<Artist> it = context
                .iterator(ObjectSelect.query(Artist.class))) {
            while (it.hasNextRow()) {
                Artist artist =  it.nextRow();
                List<Painting> paintings = artist.getPaintingArray();
                assertNotNull(paintings);
                assertEquals("Expected one painting for artist: " + artist, 1, paintings.size());
            }
        }
    }

    @Test
    public void testPerformIteratedQuery_CommitWithinIterator() throws Exception {
        createArtistsAndPaintingsDataSet();

        assertEquals(7, tPainting.getRowCount());

        try (ResultIterator<?> it = context.performIteratedQuery(ObjectSelect.query(Artist.class))) {
            while (it.hasNextRow()) {
                DataRow row = (DataRow) it.nextRow();

                Artist artist = context.objectFromDataRow(Artist.class, row);

                Painting painting = context.newObject(Painting.class);
                painting.setPaintingTitle("P_" + artist.getArtistName());
                painting.setToArtist(artist);
                context.commitChanges();
            }
        }

        assertEquals(14, tPainting.getRowCount());
    }

    @Test
    public void testPerformIteratedQuery_Transaction() throws Exception {
        createArtistsDataSet();

        try (ResultIterator<?> it = context.performIteratedQuery(ObjectSelect.query(Artist.class))) {
            assertNull("Iterator transaction was not unbound from thread", BaseTransaction.getThreadTransaction());
        }

        // TODO: how do we test that transaction unbound from the thread is closed/committed at the end?
    }
}
