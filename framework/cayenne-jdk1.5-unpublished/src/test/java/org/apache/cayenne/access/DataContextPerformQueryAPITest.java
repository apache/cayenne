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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.art.Artist;
import org.apache.art.Painting;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class DataContextPerformQueryAPITest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testObjectQueryStringBoolean() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);
        getAccessStack().createTestData(DataContextCase.class, "testPaintings", null);

        List paintings = createDataContext().performQuery("ObjectQuery", true);
        assertNotNull(paintings);
        assertEquals(25, paintings.size());
    }

    public void testObjectQueryStringMapBoolean() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);
        getAccessStack().createTestData(DataContextCase.class, "testPaintings", null);

        // fetch artist
        DataContext context = createDataContext();
        Artist a = (Artist) context.localObject(new ObjectId(
                "Artist",
                Artist.ARTIST_ID_PK_COLUMN,
                33018), null);
        Map parameters = Collections.singletonMap("artist", a);

        List paintings = createDataContext()
                .performQuery("ObjectQuery", parameters, true);
        assertNotNull(paintings);
        assertEquals(1, paintings.size());
    }

    public void testProcedureQueryStringMapBoolean() throws Exception {
        // Don't run this on MySQL
        if (!getAccessStackAdapter().supportsStoredProcedures()) {
            return;
        }

        if (!getAccessStackAdapter().canMakeObjectsOutOfProcedures()) {
            return;
        }

        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);
        getAccessStack().createTestData(DataContextCase.class, "testPaintings", null);

        // fetch artist
        Map parameters = Collections.singletonMap("aName", "artist2");
        DataContext context = createDataContext();
        List artists;

        // Sybase blows whenever a transaction wraps a SP, so turn of transactions
        boolean transactionsFlag = context
                .getParentDataDomain()
                .isUsingExternalTransactions();

        context.getParentDataDomain().setUsingExternalTransactions(true);
        try {
            artists = context.performQuery("ProcedureQuery", parameters, true);
        }
        finally {
            context.getParentDataDomain().setUsingExternalTransactions(transactionsFlag);
        }

        assertNotNull(artists);
        assertEquals(1, artists.size());

        Artist artist = (Artist) artists.get(0);
        assertEquals(33002, ((Number) artist.getObjectId().getIdSnapshot().get(
                Artist.ARTIST_ID_PK_COLUMN)).intValue());
    }

    public void testNonSelectingQueryString() throws Exception {
        DataContext context = createDataContext();

        int[] counts = context.performNonSelectingQuery("NonSelectingQuery");

        assertNotNull(counts);
        assertEquals(1, counts.length);
        assertEquals(1, counts[0]);

        Painting p = (Painting) context.localObject(new ObjectId(
                "Painting",
                Painting.PAINTING_ID_PK_COLUMN,
                512), null);
        assertEquals("No Painting Like This", p.getPaintingTitle());
    }

    public void testNonSelectingQueryStringMap() throws Exception {
        DataContext context = createDataContext();

        Map parameters = new HashMap();
        parameters.put("id", new Integer(300));
        parameters.put("title", "Go Figure");
        parameters.put("price", new BigDecimal("22.01"));

        int[] counts = context.performNonSelectingQuery(
                "ParameterizedNonSelectingQuery",
                parameters);

        assertNotNull(counts);
        assertEquals(1, counts.length);
        assertEquals(1, counts[0]);

        Painting p = (Painting) context.localObject(new ObjectId(
                "Painting",
                Painting.PAINTING_ID_PK_COLUMN,
                300), null);
        assertEquals("Go Figure", p.getPaintingTitle());
    }

    public void testPerfomQueryNonSelecting() throws Exception {
        DataContext context = createDataContext();
        Artist a = context.newObject(Artist.class);
        a.setArtistName("aa");
        context.commitChanges();

        SQLTemplate q = new SQLTemplate(Artist.class, "DELETE FROM ARTIST");

        // this way of executing a query makes no sense, but it shouldn't blow either...
        List result = context.performQuery(q);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    public void testObjectQueryWithLocalCache() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        DataContext context = createDataContext();
        List artists = context.performQuery("QueryWithLocalCache", true);
        assertEquals(25, artists.size());

        blockQueries();

        try {
            List artists1 = context.performQuery("QueryWithLocalCache", false);
            assertEquals(25, artists1.size());
        }
        finally {
            unblockQueries();
        }
    }

    public void testObjectQueryWithSharedCache() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        DataContext context = createDataContext();
        List artists = context.performQuery("QueryWithSharedCache", true);
        assertEquals(25, artists.size());

        blockQueries();

        try {
            List artists1 = context
                    .getParentDataDomain()
                    .createDataContext()
                    .performQuery("QueryWithSharedCache", false);
            assertEquals(25, artists1.size());
        }
        finally {
            unblockQueries();
        }
    }
}
