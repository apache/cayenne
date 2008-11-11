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

import org.apache.art.Artist;
import org.apache.art.Gallery;
import org.apache.cayenne.query.MockQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

/**
 * Tests various DataContextDelegate methods invocation and consequences on DataContext
 * behavior.
 * 
 */
public class DataContextDelegateTest extends CayenneCase {

    protected DataContext context;
    protected Gallery gallery;
    protected Artist artist;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        context = createDataContextWithSharedCache();

        // prepare a single gallery record
        gallery = (Gallery) context.newObject("Gallery");
        gallery.setGalleryName("version1");

        // prepare a single artist record
        artist = (Artist) context.newObject("Artist");
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
