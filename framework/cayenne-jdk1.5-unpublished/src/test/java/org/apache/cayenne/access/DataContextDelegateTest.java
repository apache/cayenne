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

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.MockQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

/**
 * Tests various DataContextDelegate methods invocation and consequences on DataContext
 * behavior.
 */
@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextDelegateTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {

        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");
        dbHelper.deleteAll("EXHIBIT");
        dbHelper.deleteAll("GALLERY");

        // prepare a single gallery record
        Gallery gallery = (Gallery) context.newObject("Gallery");
        gallery.setGalleryName("version1");

        // prepare a single artist record
        Artist artist = (Artist) context.newObject("Artist");
        artist.setArtistName("version1");

        context.commitChanges();
    }

    public void testWillPerformGenericQuery() throws Exception {

        final List<Query> queriesPerformed = new ArrayList<Query>(1);
        DataContextDelegate delegate = new MockDataContextDelegate() {

            @Override
            public Query willPerformGenericQuery(DataContext context, Query query) {
                queriesPerformed.add(query);
                return query;
            }
        };
        context.setDelegate(delegate);

        // test that delegate is consulted before select
        MockQuery query = new MockQuery();
        context.performGenericQuery(query);

        assertTrue("Delegate is not notified of a query being run.", queriesPerformed
                .contains(query));
        assertEquals(1, queriesPerformed.size());
        assertTrue("Delegate unexpectedly blocked the query.", query.isRouteCalled());
    }

    public void testWillPerformGenericQueryBlocked() throws Exception {

        final List<Query> queriesPerformed = new ArrayList<Query>(1);
        DataContextDelegate delegate = new MockDataContextDelegate() {

            @Override
            public Query willPerformGenericQuery(DataContext context, Query query) {
                queriesPerformed.add(query);
                return null;
            }
        };

        context.setDelegate(delegate);
        MockQuery query = new MockQuery();
        context.performGenericQuery(query);

        assertTrue("Delegate is not notified of a query being run.", queriesPerformed
                .contains(query));
        assertEquals(1, queriesPerformed.size());
        assertFalse("Delegate couldn't block the query.", query.isRouteCalled());
    }

    public void testWillPerformQuery() throws Exception {

        final List<Query> queriesPerformed = new ArrayList<Query>(1);
        DataContextDelegate delegate = new MockDataContextDelegate() {

            @Override
            public Query willPerformQuery(DataContext context, Query query) {
                queriesPerformed.add(query);
                return query;
            }
        };
        context.setDelegate(delegate);

        // test that delegate is consulted before select
        SelectQuery query = new SelectQuery(Gallery.class);
        List<?> results = context.performQuery(query);

        assertTrue("Delegate is not notified of a query being run.", queriesPerformed
                .contains(query));
        assertEquals(1, queriesPerformed.size());
        assertNotNull(results);
    }

    public void testWillPerformQueryBlocked() throws Exception {

        final List<Query> queriesPerformed = new ArrayList<Query>(1);
        DataContextDelegate delegate = new MockDataContextDelegate() {

            @Override
            public Query willPerformQuery(DataContext context, Query query) {
                queriesPerformed.add(query);
                return null;
            }
        };

        context.setDelegate(delegate);
        SelectQuery query = new SelectQuery(Gallery.class);
        List<?> results = context.performQuery(query);

        assertTrue("Delegate is not notified of a query being run.", queriesPerformed
                .contains(query));
        assertEquals(1, queriesPerformed.size());

        assertNotNull(results);

        // blocked
        assertEquals("Delegate couldn't block the query.", 0, results.size());
    }
}
