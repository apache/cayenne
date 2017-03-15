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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.configuration.DefaultRuntimeProperties;
import org.apache.cayenne.configuration.ObjectStoreFactory;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.event.DefaultEventManager;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.test.parallel.ParallelTestContainer;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextSharedCacheEmpiricIT extends ServerCase {

    private static final String NEW_NAME = "versionX";

    @Inject
    private ServerRuntime runtime;
    
    @Inject
    private ObjectStoreFactory objectStoreFactory;

    @Inject
    private DBHelper dbHelper;

    private DataContext c1;
    private DataContext c2;

    private DefaultEventManager eventManager;

    @Before
    public void setUp() throws Exception {
        eventManager = new DefaultEventManager();
        DataRowStore cache = new DataRowStore(
                "cacheTest",
                new DefaultRuntimeProperties(Collections.<String, String>emptyMap()),
                eventManager);

        c1 = new DataContext(runtime.getDataDomain(), 
                objectStoreFactory.createObjectStore(cache));
        c2 = new DataContext(runtime.getDataDomain(), 
                objectStoreFactory.createObjectStore(cache));

        // prepare a single artist record
        TableHelper tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");
        tArtist.insert(1, "version1");
    }

    @After
    public void tearDown() {
        if(eventManager != null) {
            eventManager.shutdown();
        }
    }

    @Test
    public void testSelectSelectCommitRefresh() throws Exception {

        SelectQuery<Artist> query = new SelectQuery<>(Artist.class);

        // select both, a2 should go second...
        List<?> artists = c1.performQuery(query);
        Artist a1 = (Artist) artists.get(0);

        List<?> altArtists = c2.performQuery(query);
        final Artist a2 = (Artist) altArtists.get(0);
        assertNotNull(a2);
        assertFalse(a2 == a1);

        // Update Artist
        a1.setArtistName(NEW_NAME);
        c1.commitChanges();

        assertOnCommit(a2);
    }

    @Test
    public void testSelectSelectCommitRefreshReverse() throws Exception {

        SelectQuery<Artist> query = new SelectQuery<>(Artist.class);

        List<?> altArtists = c2.performQuery(query);
        final Artist a2 = (Artist) altArtists.get(0);

        List<?> artists = c1.performQuery(query);
        Artist a1 = (Artist) artists.get(0);

        assertFalse(a2 == a1);

        // Update Artist
        a1.setArtistName(NEW_NAME);
        c1.commitChanges();

        assertOnCommit(a2);
    }

    @Test
    public void testSelectUpdateSelectCommitRefresh() throws Exception {

        SelectQuery<Artist> query = new SelectQuery<>(Artist.class);

        List<?> artists = c1.performQuery(query);
        Artist a1 = (Artist) artists.get(0);

        // Update Artist
        a1.setArtistName(NEW_NAME);

        List<?> altArtists = c2.performQuery(query);
        final Artist a2 = (Artist) altArtists.get(0);
        assertNotNull(a2);
        assertFalse(a2 == a1);

        c1.commitChanges();
        assertOnCommit(a2);
    }

    private void assertOnCommit(final Artist a2) throws Exception {
        // check underlying cache
        final DataRow freshSnapshot = c2
                .getObjectStore()
                .getDataRowCache()
                .getCachedSnapshot(a2.getObjectId());
        assertNotNull("No snapshot for artist", freshSnapshot);
        assertEquals(NEW_NAME, freshSnapshot.get("ARTIST_NAME"));

        // check peer artist
        ParallelTestContainer helper = new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertEquals(
                        "Snapshot change is not propagated: " + freshSnapshot,
                        NEW_NAME,
                        a2.getArtistName());
            }
        };
        helper.runTest(3000);
    }
}
