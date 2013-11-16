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
import java.util.List;
import java.util.Set;
import java.sql.Types;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.ArtGroup;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class FlattenedPrefetchTest extends ServerCase {

    @Inject
    protected DataChannelInterceptor queryInterceptor;

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tArtist;
    protected TableHelper tPainting;
    protected TableHelper tArtgroup;
    protected TableHelper tArtistGroup;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("PAINTING1");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");
        dbHelper.update("ARTGROUP").set("PARENT_GROUP_ID", null, Types.INTEGER).execute();
        dbHelper.deleteAll("ARTGROUP");

        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns(
                "PAINTING_ID",
                "PAINTING_TITLE",
                "ARTIST_ID",
                "ESTIMATED_PRICE");

        tArtgroup = new TableHelper(dbHelper, "ARTGROUP");
        tArtgroup.setColumns("GROUP_ID", "NAME");

        tArtistGroup = new TableHelper(dbHelper, "ARTIST_GROUP");
        tArtistGroup.setColumns("ARTIST_ID", "GROUP_ID");
    }

    protected void createPrefetchDataSet1() throws Exception {
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
        tArtist.insert(33003, "artist3");

        tArtgroup.insert(33001, "group1");
        tArtgroup.insert(33002, "group2");

        tArtistGroup.insert(33001, 33001);
        tArtistGroup.insert(33001, 33002);
        tArtistGroup.insert(33002, 33002);
        tArtistGroup.insert(33003, 33002);
    }

    protected void createPrefetchDataSet2() throws Exception {
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
        tArtist.insert(33003, "artist3");
        
        tArtgroup.insert(33001, "group1");
        tArtgroup.insert(33002, "group2");
      
        tArtistGroup.insert(33001, 33001);
        tArtistGroup.insert(33001, 33002);
        tArtistGroup.insert(33002, 33002);
        tArtistGroup.insert(33003, 33002);
        tPainting.insert(33001, "P_artist11", 33001, 1000);
        tPainting.insert(33002, "P_artist12", 33001, 2000);
        tPainting.insert(33003, "P_artist21", 33002, 3000);
    }

    public void testManyToMany() throws Exception {
        createPrefetchDataSet1();

        SelectQuery q = new SelectQuery(Artist.class);
        q.addPrefetch(Artist.GROUP_ARRAY_PROPERTY);

        final List<Artist> objects = context.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals(3, objects.size());

                for (Artist a : objects) {
                    List<ArtGroup> list = a.getGroupArray();

                    assertNotNull(list);
                    assertFalse(
                            "artist's groups not resolved: " + a,
                            ((ValueHolder) list).isFault());
                    assertTrue(list.size() > 0);

                    for (ArtGroup g : list) {
                        assertEquals(PersistenceState.COMMITTED, g.getPersistenceState());
                    }

                    // assert no duplicates
                    Set<ArtGroup> s = new HashSet<ArtGroup>(list);
                    assertEquals(s.size(), list.size());
                }
            }
        });
    }

    public void testMultiPrefetch() throws Exception {
        createPrefetchDataSet2();

        SelectQuery q = new SelectQuery(Painting.class);
        q.addPrefetch(Painting.TO_ARTIST_PROPERTY);
        q.addPrefetch(Painting.TO_ARTIST_PROPERTY + '.' + Artist.GROUP_ARRAY_PROPERTY);

        final List<Painting> objects = context.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals(3, objects.size());

                for (Painting p : objects) {
                    Artist a = p.getToArtist();
                    assertEquals(PersistenceState.COMMITTED, a.getPersistenceState());

                    List<ArtGroup> list = a.getGroupArray();
                    assertNotNull(list);
                    assertFalse(
                            "artist's groups not resolved: " + a,
                            ((ValueHolder) list).isFault());
                    assertTrue(list.size() > 0);

                    for (ArtGroup g : list) {
                        assertEquals(PersistenceState.COMMITTED, g.getPersistenceState());
                    }

                    // assert no duplicates
                    Set<ArtGroup> s = new HashSet<ArtGroup>(list);
                    assertEquals(s.size(), list.size());
                }
            }
        });
    }

    public void testJointManyToMany() throws Exception {
        createPrefetchDataSet1();

        SelectQuery q = new SelectQuery(Artist.class);
        q.addPrefetch(Artist.GROUP_ARRAY_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        final List<Artist> objects = context.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals(3, objects.size());

                for (Artist a : objects) {
                    List<ArtGroup> list = a.getGroupArray();

                    assertNotNull(list);
                    assertFalse(
                            "artist's groups not resolved: " + a,
                            ((ValueHolder) list).isFault());
                    assertTrue(list.size() > 0);

                    for (ArtGroup g : list) {
                        assertEquals(PersistenceState.COMMITTED, g.getPersistenceState());
                    }

                    // assert no duplicates
                    Set<ArtGroup> s = new HashSet<ArtGroup>(list);
                    assertEquals(s.size(), list.size());
                }
            }
        });

    }

    public void testJointMultiPrefetch() throws Exception {
        createPrefetchDataSet2();

        SelectQuery q = new SelectQuery(Painting.class);
        q.addPrefetch(Painting.TO_ARTIST_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
        q
                .addPrefetch(
                        Painting.TO_ARTIST_PROPERTY + '.' + Artist.GROUP_ARRAY_PROPERTY)
                .setSemantics(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        final List<Painting> objects = context.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals(3, objects.size());

                for (Painting p : objects) {
                    Artist a = p.getToArtist();
                    assertEquals(PersistenceState.COMMITTED, a.getPersistenceState());

                    List<ArtGroup> list = a.getGroupArray();
                    assertNotNull(list);
                    assertFalse(
                            "artist's groups not resolved: " + a,
                            ((ValueHolder) list).isFault());
                    assertTrue(list.size() > 0);

                    for (ArtGroup g : list) {
                        assertEquals(PersistenceState.COMMITTED, g.getPersistenceState());
                    }

                    // assert no duplicates

                    Set<ArtGroup> s = new HashSet<ArtGroup>(list);
                    assertEquals(s.size(), list.size());
                }
            }
        });
    }
}
