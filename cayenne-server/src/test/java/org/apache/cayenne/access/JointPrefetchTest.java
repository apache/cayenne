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

import java.sql.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

/**
 * Tests joint prefetch handling by Cayenne access stack.
 */
@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class JointPrefetchTest extends ServerCase {

    @Inject
    protected DataContext context;

    @Inject
    protected ServerRuntime runtime;

    @Inject
    protected DataChannelInterceptor queryInterceptor;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tArtist;
    protected TableHelper tGallery;
    protected TableHelper tPainting;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT"); // table artist_exhibit depends on artist and exhibit
        dbHelper.deleteAll("ARTIST");
        dbHelper.deleteAll("EXHIBIT"); // table exhibit depends on gallery
        dbHelper.deleteAll("GALLERY");

        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tGallery = new TableHelper(dbHelper, "GALLERY");
        tGallery.setColumns("GALLERY_ID", "GALLERY_NAME");

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns(
                "PAINTING_ID",
                "PAINTING_TITLE",
                "ARTIST_ID",
                "ESTIMATED_PRICE",
                "GALLERY_ID");
    }

    protected void createJointPrefetchDataSet1() throws Exception {
        tGallery.insert(33001, "G1");
        tGallery.insert(33002, "G2");
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
        tArtist.insert(33003, "artist3");
        tPainting.insert(33001, "P_artist11", 33001, 1000, 33001);
        tPainting.insert(33002, "P_artist12", 33001, 2000, 33001);
        tPainting.insert(33003, "P_artist21", 33002, 3000, 33002);
    }

    protected void createJointPrefetchDataSet2() throws Exception {
        tGallery.insert(33001, "G1");
        tGallery.insert(33002, "G2");
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
        tArtist.insert(33003, "artist3");
        tPainting.insert(33001, "P_artist11", 33001, 1000, 33001);
        tPainting.insert(33002, "P_artist12", 33001, 2000, 33001);
        tPainting.insert(33003, "P_artist21", 33002, 3000, 33002);
    }

    public void testJointPrefetch_ToOne_FetchLimit() throws Exception {
        createJointPrefetchDataSet1();

        SelectQuery q = new SelectQuery(Painting.class);
        q.setFetchLimit(2);
        q.setFetchOffset(0);
        q.addOrdering("db:PAINTING_ID", SortOrder.ASCENDING);
        q.addPrefetch(Painting.TO_ARTIST_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        final List<?> objects = context.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals(2, objects.size());

                Iterator<?> it = objects.iterator();
                while (it.hasNext()) {
                    Painting p = (Painting) it.next();
                    Artist target = p.getToArtist();
                    assertNotNull(target);
                    assertEquals(PersistenceState.COMMITTED, target.getPersistenceState());
                }
            }
        });
    }

    public void testJointPrefetch_ToMany_FetchLimit() throws Exception {
        createJointPrefetchDataSet1();

        SelectQuery q = new SelectQuery(Artist.class);
        q.setFetchLimit(2);
        q.setFetchOffset(0);
        q.addOrdering("db:ARTIST_ID", SortOrder.ASCENDING);
        q.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        final List<?> objects = context.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                // herein lies the limitation of prefetching combined with fetch limit -
                // we got fewer artists than we wanted
                assertEquals(1, objects.size());

                Iterator<?> it = objects.iterator();
                while (it.hasNext()) {
                    Artist a = (Artist) it.next();
                    List<Painting> targets = a.getPaintingArray();
                    assertNotNull(targets);
                    for (Painting p : targets) {
                        assertEquals(PersistenceState.COMMITTED, p.getPersistenceState());
                    }
                }
            }
        });
    }

    public void testJointPrefetchDataRows() throws Exception {
        createJointPrefetchDataSet1();

        // query with to-many joint prefetches
        SelectQuery q = new SelectQuery(Painting.class);
        q.addOrdering("db:PAINTING_ID", SortOrder.ASCENDING);
        q.setFetchingDataRows(true);
        q.addPrefetch(Painting.TO_ARTIST_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        final List<?> rows = context.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals(3, rows.size());

                // row should contain columns from both entities minus those duplicated in
                // a
                // join...

                int rowWidth = context
                        .getEntityResolver()
                        .getDbEntity("ARTIST")
                        .getAttributes()
                        .size()
                        + context
                                .getEntityResolver()
                                .getDbEntity("PAINTING")
                                .getAttributes()
                                .size();
                Iterator<?> it = rows.iterator();
                while (it.hasNext()) {
                    DataRow row = (DataRow) it.next();
                    assertEquals("" + row, rowWidth, row.size());

                    // assert columns presence
                    assertTrue(row + "", row.containsKey("PAINTING_ID"));
                    assertTrue(row + "", row.containsKey("ARTIST_ID"));
                    assertTrue(row + "", row.containsKey("GALLERY_ID"));
                    assertTrue(row + "", row.containsKey("PAINTING_TITLE"));
                    assertTrue(row + "", row.containsKey("ESTIMATED_PRICE"));
                    assertTrue(row + "", row.containsKey("toArtist.ARTIST_NAME"));
                    assertTrue(row + "", row.containsKey("toArtist.DATE_OF_BIRTH"));
                }
            }
        });
    }

    public void testJointPrefetchSQLTemplate() throws Exception {
        createJointPrefetchDataSet1();

        // correctly naming columns is the key..
        SQLTemplate q = new SQLTemplate(
                Artist.class,
                "SELECT distinct "
                        + "#result('ESTIMATED_PRICE' 'BigDecimal' '' 'paintingArray.ESTIMATED_PRICE'), "
                        + "#result('PAINTING_TITLE' 'String' '' 'paintingArray.PAINTING_TITLE'), "
                        + "#result('GALLERY_ID' 'int' '' 'paintingArray.GALLERY_ID'), "
                        + "#result('PAINTING_ID' 'int' '' 'paintingArray.PAINTING_ID'), "
                        + "#result('ARTIST_NAME' 'String'), "
                        + "#result('DATE_OF_BIRTH' 'java.util.Date'), "
                        + "#result('t0.ARTIST_ID' 'int' '' 'ARTIST_ID') "
                        + "FROM ARTIST t0, PAINTING t1 "
                        + "WHERE t0.ARTIST_ID = t1.ARTIST_ID");

        PrefetchTreeNode prefetch = q.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY);
        assertEquals(
                "Default semantics for SQLTemplate is assumed to be joint.",
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS,
                prefetch.getSemantics());
        q.setFetchingDataRows(false);

        final List<?> objects = context.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                // without OUTER join we will get fewer objects...
                assertEquals(2, objects.size());

                Iterator<?> it = objects.iterator();
                while (it.hasNext()) {
                    Artist a = (Artist) it.next();
                    List<?> list = a.getPaintingArray();

                    assertNotNull(list);
                    assertFalse(((ValueHolder) list).isFault());
                    assertTrue(list.size() > 0);

                    Iterator<?> children = list.iterator();
                    while (children.hasNext()) {
                        Painting p = (Painting) children.next();
                        assertEquals(PersistenceState.COMMITTED, p.getPersistenceState());

                        // make sure properties are not null..
                        assertNotNull(p.getPaintingTitle());
                    }
                }
            }
        });
    }

    public void testJointPrefetchToOne() throws Exception {
        createJointPrefetchDataSet1();

        // query with to-many joint prefetches
        SelectQuery q = new SelectQuery(Painting.class);
        q.addOrdering("db:PAINTING_ID", SortOrder.ASCENDING);
        q.addPrefetch(Painting.TO_ARTIST_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        final List<?> objects = context.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals(3, objects.size());

                Iterator<?> it = objects.iterator();
                while (it.hasNext()) {
                    Painting p = (Painting) it.next();
                    Artist target = p.getToArtist();
                    assertNotNull(target);
                    assertEquals(PersistenceState.COMMITTED, target.getPersistenceState());
                }
            }
        });
    }

    /**
     * Tests that joined entities can have non-standard type mappings.
     */
    public void testJointPrefetchDataTypes() throws Exception {
        // prepare... can't load from XML, as it doesn't yet support dates..
        SQLTemplate artistSQL = new SQLTemplate(
                Artist.class,
                "insert into ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) "
                        + "values (33001, 'a1', #bind($date 'DATE'))");
        artistSQL.setParameters(Collections.singletonMap(
                "date",
                new Date(System.currentTimeMillis())));
        SQLTemplate paintingSQL = new SQLTemplate(
                Painting.class,
                "INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ARTIST_ID, ESTIMATED_PRICE) "
                        + "VALUES (33001, 'p1', 33001, 1000)");

        context.performNonSelectingQuery(artistSQL);
        context.performNonSelectingQuery(paintingSQL);

        // test
        SelectQuery q = new SelectQuery(Painting.class);
        q.addPrefetch(Painting.TO_ARTIST_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        ObjEntity artistE = context.getEntityResolver().getObjEntity("Artist");
        ObjAttribute dateOfBirth = artistE.getAttribute("dateOfBirth");
        assertEquals("java.util.Date", dateOfBirth.getType());
        dateOfBirth.setType("java.sql.Date");
        try {
            final List<?> objects = context.performQuery(q);

            queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

                public void execute() {
                    assertEquals(1, objects.size());

                    Iterator<?> it = objects.iterator();
                    while (it.hasNext()) {
                        Painting p = (Painting) it.next();
                        Artist a = p.getToArtist();
                        assertNotNull(a);
                        assertNotNull(a.getDateOfBirth());
                        assertTrue(a.getDateOfBirth().getClass().getName(), Date.class
                                .isAssignableFrom(a.getDateOfBirth().getClass()));
                    }
                }
            });
        }
        finally {
            dateOfBirth.setType("java.util.Date");
        }
    }

    public void testJointPrefetchToMany() throws Exception {
        createJointPrefetchDataSet1();

        // query with to-many joint prefetches
        SelectQuery q = new SelectQuery(Artist.class);
        q.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        final List<?> objects = context.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals(3, objects.size());

                Iterator<?> it = objects.iterator();
                while (it.hasNext()) {
                    Artist a = (Artist) it.next();
                    List<?> list = a.getPaintingArray();

                    assertNotNull(list);
                    assertFalse(((ValueHolder) list).isFault());

                    Iterator<?> children = list.iterator();
                    while (children.hasNext()) {
                        Painting p = (Painting) children.next();
                        assertEquals(PersistenceState.COMMITTED, p.getPersistenceState());
                        // make sure properties are not null..
                        assertNotNull(p.getPaintingTitle());
                    }
                }
            }
        });
    }

    public void testJointPrefetchToManyNonConflictingQualifier() throws Exception {
        createJointPrefetchDataSet1();

        // query with to-many joint prefetches and qualifier that doesn't match
        // prefetch....
        Expression qualifier = ExpressionFactory.matchExp(
                Artist.ARTIST_NAME_PROPERTY,
                "artist1");
        SelectQuery q = new SelectQuery(Artist.class, qualifier);
        q.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        final List<?> objects = context.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals(1, objects.size());

                Artist a = (Artist) objects.get(0);
                List<?> list = a.getPaintingArray();

                assertNotNull(list);
                assertFalse(((ValueHolder) list).isFault());
                assertEquals(2, list.size());

                Iterator<?> children = list.iterator();
                while (children.hasNext()) {
                    Painting p = (Painting) children.next();
                    assertEquals(PersistenceState.COMMITTED, p.getPersistenceState());
                    // make sure properties are not null..
                    assertNotNull(p.getPaintingTitle());
                }

                // assert no duplicates
                Set s = new HashSet(list);
                assertEquals(s.size(), list.size());
            }
        });
    }

    public void testJointPrefetchMultiStep() throws Exception {
        createJointPrefetchDataSet2();

        // query with to-many joint prefetches
        SelectQuery q = new SelectQuery(Artist.class);
        q
                .addPrefetch(
                        Artist.PAINTING_ARRAY_PROPERTY
                                + "."
                                + Painting.TO_GALLERY_PROPERTY)
                .setSemantics(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        final DataContext context = this.context;

        // make sure phantomly prefetched objects are not deallocated
        context.getObjectStore().objectMap = new HashMap<Object, Persistent>();

        // sanity check...
        DataObject g1 = (DataObject) context.getGraphManager().getNode(
                new ObjectId("Gallery", Gallery.GALLERY_ID_PK_COLUMN, 33001));
        assertNull(g1);

        final List<?> objects = context.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals(3, objects.size());

                Iterator<?> it = objects.iterator();
                while (it.hasNext()) {
                    Artist a = (Artist) it.next();
                    ValueHolder list = (ValueHolder) a.getPaintingArray();

                    assertNotNull(list);

                    // intermediate relationship is not fetched...
                    assertTrue(list.isFault());
                }

                // however both galleries must be in memory...
                DataObject g1 = (DataObject) context.getGraphManager().getNode(
                        new ObjectId("Gallery", Gallery.GALLERY_ID_PK_COLUMN, 33001));
                assertNotNull(g1);
                assertEquals(PersistenceState.COMMITTED, g1.getPersistenceState());
                DataObject g2 = (DataObject) context.getGraphManager().getNode(
                        new ObjectId("Gallery", Gallery.GALLERY_ID_PK_COLUMN, 33002));
                assertNotNull(g2);
                assertEquals(PersistenceState.COMMITTED, g2.getPersistenceState());
            }
        });
    }
}
