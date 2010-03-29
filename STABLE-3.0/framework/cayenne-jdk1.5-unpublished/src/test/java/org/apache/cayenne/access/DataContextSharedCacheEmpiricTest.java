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

import java.util.Collections;
import java.util.List;

import org.apache.art.Artist;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.util.ThreadedTestHelper;

/**
 */
public class DataContextSharedCacheEmpiricTest extends CayenneCase {

    private static final String NEW_NAME = "versionX";

    protected DataContext c1;
    protected DataContext c2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();

        DataRowStore cache = new DataRowStore(
                "cacheTest",
                Collections.EMPTY_MAP,
                new EventManager());

        c1 = new DataContext(getDomain(), new ObjectStore(cache));
        c2 = new DataContext(getDomain(), new ObjectStore(cache));

        // prepare a single artist record
        SQLTemplate insert = new SQLTemplate(
                Artist.class,
                "insert into ARTIST (ARTIST_ID, ARTIST_NAME) values (1, 'version1')");
        c1.performNonSelectingQuery(insert);
    }

    public void testSelectSelectCommitRefresh() throws Exception {

        SelectQuery query = new SelectQuery(Artist.class);

        // select both, a2 should go second...
        List artists = c1.performQuery(query);
        Artist a1 = (Artist) artists.get(0);

        List altArtists = c2.performQuery(query);
        final Artist a2 = (Artist) altArtists.get(0);
        assertNotNull(a2);
        assertFalse(a2 == a1);

        // Update Artist
        a1.setArtistName(NEW_NAME);
        c1.commitChanges();

        assertOnCommit(a2);
    }

    public void testSelectSelectCommitRefreshReverse() throws Exception {

        SelectQuery query = new SelectQuery(Artist.class);

        List altArtists = c2.performQuery(query);
        final Artist a2 = (Artist) altArtists.get(0);

        List artists = c1.performQuery(query);
        Artist a1 = (Artist) artists.get(0);

        assertFalse(a2 == a1);

        // Update Artist
        a1.setArtistName(NEW_NAME);
        c1.commitChanges();

        assertOnCommit(a2);
    }

    public void testSelectUpdateSelectCommitRefresh() throws Exception {

        SelectQuery query = new SelectQuery(Artist.class);

        List artists = c1.performQuery(query);
        Artist a1 = (Artist) artists.get(0);

        // Update Artist
        a1.setArtistName(NEW_NAME);

        List altArtists = c2.performQuery(query);
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
        ThreadedTestHelper helper = new ThreadedTestHelper() {

            @Override
            protected void assertResult() throws Exception {
                assertEquals(
                        "Snapshot change is not propagated: " + freshSnapshot,
                        NEW_NAME,
                        a2.getArtistName());
            }
        };
        helper.assertWithTimeout(3000);
    }
}
