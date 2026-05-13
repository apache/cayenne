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

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.tx.BaseTransaction;
import org.apache.cayenne.tx.ExternalTransaction;
import org.apache.cayenne.tx.Transaction;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DataContextPerformQueryAPIIT  {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

        private DataContext context;

        private DataContext context2;


        private UnitDbAdapter accessStackAdapter;

        private DataChannelInterceptor queryInterceptor;
    
        private JdbcEventLogger jdbcEventLogger;

    private TableHelper tArtist;
    private TableHelper tPainting;

    
    @BeforeEach
    public void setUp() throws Exception {
        context = env.dataContext();
        context2 = (DataContext) env.runtime().newContext();
        accessStackAdapter = env.getInstance(UnitDbAdapter.class);
        queryInterceptor = env.getInstance(DataChannelInterceptor.class);
        jdbcEventLogger = env.getInstance(JdbcEventLogger.class);
        tArtist = env.table("ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tPainting = env.table("PAINTING");
        tPainting.setColumns("PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE", "ESTIMATED_PRICE").setColumnTypes(
                Types.INTEGER, Types.BIGINT, Types.VARCHAR, Types.DECIMAL);
    }

    private void createTwoArtists() throws Exception {
        tArtist.insert(21, "artist2");
        tArtist.insert(201, "artist3");
    }

    private void createTwoArtistsAndTwoPaintingsDataSet() throws Exception {
        tArtist.insert(11, "artist2");
        tArtist.insert(101, "artist3");
        tPainting.insert(6, 101, "p_artist3", 1000);
        tPainting.insert(7, 11, "p_artist2", 2000);
    }

    @Test
    public void objectQueryStringBoolean() throws Exception {
        createTwoArtistsAndTwoPaintingsDataSet();

        List<?> paintings = context.performQuery("ObjectQuery", true);
        assertNotNull(paintings);
        assertEquals(2, paintings.size());
    }

    @Test
    public void objectQueryStringMapBoolean() throws Exception {
        createTwoArtistsAndTwoPaintingsDataSet();

        Artist a = Cayenne.objectForPK(context, Artist.class, 11);
        Map<String, Artist> parameters = Collections.singletonMap("artist", a);

        List<?> paintings = context2.performQuery("ObjectQuery", parameters, true);
        assertNotNull(paintings);
        assertEquals(1, paintings.size());
    }

    @Test
    public void procedureQueryStringMapBoolean() throws Exception {

        if (!accessStackAdapter.supportsStoredProcedures()) {
            return;
        }

        if (!accessStackAdapter.canMakeObjectsOutOfProcedures()) {
            return;
        }

        createTwoArtistsAndTwoPaintingsDataSet();

        // fetch artist
        Map<String, String> parameters = Collections.singletonMap("aName", "artist2");

        List<?> artists;

        // Sybase blows whenever a transaction wraps a SP, so turn of
        // transactions
        Transaction t = new ExternalTransaction(jdbcEventLogger);
        BaseTransaction.bindThreadTransaction(t);
        try {
            artists = context.performQuery("ProcedureQuery", parameters, true);
        } finally {
            BaseTransaction.bindThreadTransaction(null);
            t.commit();
        }

        assertNotNull(artists);
        assertEquals(1, artists.size());

        Artist artist = (Artist) artists.get(0);
        assertEquals(11, ((Number) artist.getObjectId().getIdSnapshot().get(Artist.ARTIST_ID_PK_COLUMN)).intValue());
    }

    @Test
    public void nonSelectingQueryString() throws Exception {

        int[] counts = context.performNonSelectingQuery("NonSelectingQuery");

        assertNotNull(counts);
        assertEquals(1, counts.length);
        assertEquals(1, counts[0]);

        Painting p = Cayenne.objectForPK(context, Painting.class, 512);
        assertEquals("No Painting Like This", p.getPaintingTitle());
    }

    @Test
    public void nonSelectingQueryStringMap() throws Exception {

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", 300);
        parameters.put("title", "Go Figure");
        parameters.put("price", new BigDecimal("22.01"));

        int[] counts = context.performNonSelectingQuery("ParameterizedNonSelectingQuery", parameters);

        assertNotNull(counts);
        assertEquals(1, counts.length);
        assertEquals(1, counts[0]);

        Painting p = Cayenne.objectForPK(context, Painting.class, 300);
        assertEquals("Go Figure", p.getPaintingTitle());
    }

    @Test
    public void perfomQueryNonSelecting() throws Exception {

        Artist a = context.newObject(Artist.class);
        a.setArtistName("aa");
        context.commitChanges();

        SQLTemplate q = new SQLTemplate(Artist.class, "DELETE FROM ARTIST");

        // this way of executing a query makes no sense, but it shouldn't blow
        // either...
        List<?> result = context.performQuery(q);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void objectQueryWithLocalCache() throws Exception {
        createTwoArtists();

        List<?> artists = context.performQuery("QueryWithLocalCache", true);
        assertEquals(2, artists.size());

        queryInterceptor.runWithQueriesBlocked(() -> {
            List<?> artists1 = context.performQuery("QueryWithLocalCache", false);
            assertEquals(2, artists1.size());
        });
    }

    @Test
    public void objectQueryWithSharedCache() throws Exception {
        createTwoArtists();

        List<?> artists = context.performQuery("QueryWithSharedCache", true);
        assertEquals(2, artists.size());

        queryInterceptor.runWithQueriesBlocked(() -> {
            List<?> artists1 = context2.performQuery("QueryWithSharedCache", false);
            assertEquals(2, artists1.size());
        });
    }
}
