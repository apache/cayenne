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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.art.ArtGroup;
import org.apache.art.Artist;
import org.apache.art.Painting;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class FlattenedPrefetchTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();
    }

    public void testManyToMany() throws Exception {

        createTestData("testPrefetch1");

        SelectQuery q = new SelectQuery(Artist.class);
        q.addPrefetch(Artist.GROUP_ARRAY_PROPERTY);

        DataContext context = createDataContext();

        List objects = context.performQuery(q);

        blockQueries();
        try {

            assertEquals(3, objects.size());

            Iterator it = objects.iterator();
            while (it.hasNext()) {
                Artist a = (Artist) it.next();
                List list = a.getGroupArray();

                assertNotNull(list);
                assertFalse("artist's groups not resolved: " + a, ((ValueHolder) list).isFault());
                assertTrue(list.size() > 0);

                Iterator children = list.iterator();
                while (children.hasNext()) {
                    ArtGroup g = (ArtGroup) children.next();
                    assertEquals(PersistenceState.COMMITTED, g.getPersistenceState());
                }

                // assert no duplicates
                Set s = new HashSet(list);
                assertEquals(s.size(), list.size());
            }
        }
        finally {
            unblockQueries();
        }
    }

    public void testMultiPrefetch() throws Exception {
        createTestData("testPrefetch2");

        SelectQuery q = new SelectQuery(Painting.class);
        q.addPrefetch(Painting.TO_ARTIST_PROPERTY);
        q.addPrefetch(Painting.TO_ARTIST_PROPERTY + '.' + Artist.GROUP_ARRAY_PROPERTY);

        DataContext context = createDataContext();

        List objects = context.performQuery(q);

        blockQueries();

        try {
            assertEquals(3, objects.size());

            Iterator it = objects.iterator();
            while (it.hasNext()) {
                Painting p = (Painting) it.next();
                Artist a = p.getToArtist();
                assertEquals(PersistenceState.COMMITTED, a.getPersistenceState());

                List list = a.getGroupArray();
                assertNotNull(list);
                assertFalse("artist's groups not resolved: " + a, ((ValueHolder)list).isFault());
                assertTrue(list.size() > 0);

                Iterator children = list.iterator();
                while (children.hasNext()) {
                    ArtGroup g = (ArtGroup) children.next();
                    assertEquals(PersistenceState.COMMITTED, g.getPersistenceState());
                }

                // assert no duplicates
                Set s = new HashSet(list);
                assertEquals(s.size(), list.size());
            }
        }
        finally {
            unblockQueries();
        }
    }

    public void testJointManyToMany() throws Exception {
        createTestData("testPrefetch1");

        SelectQuery q = new SelectQuery(Artist.class);
        q.addPrefetch(Artist.GROUP_ARRAY_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        DataContext context = createDataContext();

        List objects = context.performQuery(q);

        blockQueries();

        try {

            assertEquals(3, objects.size());

            Iterator it = objects.iterator();
            while (it.hasNext()) {
                Artist a = (Artist) it.next();
                List list = a.getGroupArray();

                assertNotNull(list);
                assertFalse("artist's groups not resolved: " + a, ((ValueHolder) list).isFault());
                assertTrue(list.size() > 0);

                Iterator children = list.iterator();
                while (children.hasNext()) {
                    ArtGroup g = (ArtGroup) children.next();
                    assertEquals(PersistenceState.COMMITTED, g.getPersistenceState());
                }

                // assert no duplicates
                Set s = new HashSet(list);
                assertEquals(s.size(), list.size());
            }
        }
        finally {
            unblockQueries();
        }
    }

    public void testJointMultiPrefetch() throws Exception {
        createTestData("testPrefetch2");

        SelectQuery q = new SelectQuery(Painting.class);
        q.addPrefetch(Painting.TO_ARTIST_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
        q
                .addPrefetch(
                        Painting.TO_ARTIST_PROPERTY + '.' + Artist.GROUP_ARRAY_PROPERTY)
                .setSemantics(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        DataContext context = createDataContext();

        List objects = context.performQuery(q);

        blockQueries();
        try {
            assertEquals(3, objects.size());

            Iterator it = objects.iterator();
            while (it.hasNext()) {
                Painting p = (Painting) it.next();
                Artist a = p.getToArtist();
                assertEquals(PersistenceState.COMMITTED, a.getPersistenceState());

                List list = a.getGroupArray();
                assertNotNull(list);
                assertFalse("artist's groups not resolved: " + a, ((ValueHolder) list).isFault());
                assertTrue(list.size() > 0);

                Iterator children = list.iterator();
                while (children.hasNext()) {
                    ArtGroup g = (ArtGroup) children.next();
                    assertEquals(PersistenceState.COMMITTED, g.getPersistenceState());
                }

                // assert no duplicates

                Set s = new HashSet(list);
                assertEquals(s.size(), list.size());
            }
        }
        finally {
            unblockQueries();
        }
    }
}
