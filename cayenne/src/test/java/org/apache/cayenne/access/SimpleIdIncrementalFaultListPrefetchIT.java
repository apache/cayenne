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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class SimpleIdIncrementalFaultListPrefetchIT extends RuntimeCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected DataChannelInterceptor queryInterceptor;

    protected TableHelper tArtist;
    protected TableHelper tPaining;

    @Before
    public void setUp() throws Exception {
        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tPaining = new TableHelper(dbHelper, "PAINTING");
        tPaining.setColumns(
                "PAINTING_ID",
                "PAINTING_TITLE",
                "ARTIST_ID",
                "ESTIMATED_PRICE");
    }

    protected void createArtistsDataSet() throws Exception {
        tArtist.insert(33001, "artist11");
        tArtist.insert(33002, "artist12");
        tArtist.insert(33003, "artist13");
        tArtist.insert(33004, "artist14");
        tArtist.insert(33005, "artist15");
        tArtist.insert(33006, "artist16");
        tArtist.insert(33007, "artist21");
    }

    protected void createArtistsAndPaintingsDataSet() throws Exception {
        createArtistsDataSet();

        tPaining.insert(33001, "P_artist11", 33001, 1000);
        tPaining.insert(33002, "P_artist12", 33002, 2000);
        tPaining.insert(33003, "P_artist13", 33003, 3000);
        tPaining.insert(33004, "P_artist14", 33004, 4000);
        tPaining.insert(33005, "P_artist15", 33005, 5000);
        tPaining.insert(33006, "P_artist16", 33006, 11000);
        tPaining.insert(33007, "P_artist21", 33007, 21000);
    }

    @Test
    public void testListType() throws Exception {
        createArtistsDataSet();

        List<?> result = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.like("artist1%"))
                .pageSize(4)
                .select(context);
        assertTrue(result instanceof SimpleIdIncrementalFaultList);
    }

    /**
     * Test that all queries specified in prefetch are executed with a single prefetch
     * path.
     */
    @Test
    public void testPrefetch1() throws Exception {
        createArtistsAndPaintingsDataSet();

        ObjectSelect<Artist> q = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.like("artist1%"))
                .prefetch("paintingArray", PrefetchTreeNode.UNDEFINED_SEMANTICS)
                .pageSize(3);

        final IncrementalFaultList<?> result = (IncrementalFaultList) context
                .performQuery(q);

        assertEquals(6, result.size());

        // currently queries with prefetch do not resolve their first page
        assertEquals(result.size(), result.getUnfetchedObjects());

        int count = queryInterceptor.runWithQueryCounter(() -> {
            // go through the second page objects and count queries
            for (int i = 3; i < 6; i++) {
                result.get(i);
            }
        });

        // within the same page only one query should've been executed
        // we expect the second one for the prefetch
        assertEquals(2, count);
    }

    /**
     * Test that a to-many relationship is initialized.
     */
    @Test
    public void testPrefetch3() throws Exception {
        createArtistsAndPaintingsDataSet();

        ObjectSelect<Artist> q = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.like("artist1%"))
                .prefetch("paintingArray", PrefetchTreeNode.UNDEFINED_SEMANTICS)
                .pageSize(3);

        IncrementalFaultList result = (IncrementalFaultList) context.performQuery(q);

        assertEquals(6, result.size());

        // currently queries with prefetch do not resolve their first page
        assertEquals(result.size(), result.getUnfetchedObjects());

        // go through the second page objects and check their to many
        for (int i = 3; i < 6; i++) {
            Artist a = (Artist) result.get(i);

            List paintings = a.getPaintingArray();
            assertFalse(((ValueHolder) paintings).isFault());
            assertEquals(1, paintings.size());
        }
    }

    /**
     * Test that a to-one relationship is initialized.
     */
    @Test
    public void testPrefetch4() throws Exception {
        createArtistsAndPaintingsDataSet();

        ObjectSelect<Painting> q = ObjectSelect.query(Painting.class)
                .pageSize(3)
                .prefetch("toArtist", PrefetchTreeNode.UNDEFINED_SEMANTICS);

        IncrementalFaultList<?> result = (IncrementalFaultList<Painting>) context.performQuery(q);

        // get an objects from the second page
        final Persistent p1 = (Persistent) result.get(q.getPageSize());

        queryInterceptor.runWithQueriesBlocked(() -> {
            Object toOnePrefetch = p1.readNestedProperty("toArtist");
            assertNotNull(toOnePrefetch);
            assertTrue(
                    "Expected Persistent, got: " + toOnePrefetch.getClass().getName(),
                    toOnePrefetch instanceof Persistent);

            Persistent a1 = (Persistent) toOnePrefetch;
            assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
        });
    }

}
