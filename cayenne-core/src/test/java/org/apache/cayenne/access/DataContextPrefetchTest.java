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

import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.ArtGroup;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.ArtistExhibit;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.testdo.testmap.PaintingInfo;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextPrefetchTest extends ServerCase {

    @Inject
    protected DataContext context;

    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected DataChannelInterceptor queryInterceptor;

    protected TableHelper tArtist;
    protected TableHelper tPainting;
    protected TableHelper tPaintingInfo;
    protected TableHelper tExhibit;
    protected TableHelper tGallery;
    protected TableHelper tArtistExhibit;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");
        dbHelper.deleteAll("EXHIBIT");
        dbHelper.deleteAll("GALLERY");

        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns("PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "ESTIMATED_PRICE").setColumnTypes(
                Types.INTEGER, Types.VARCHAR, Types.BIGINT, Types.DECIMAL);

        tPaintingInfo = new TableHelper(dbHelper, "PAINTING_INFO");
        tPaintingInfo.setColumns("PAINTING_ID", "TEXT_REVIEW");

        tExhibit = new TableHelper(dbHelper, "EXHIBIT");
        tExhibit.setColumns("EXHIBIT_ID", "GALLERY_ID", "OPENING_DATE", "CLOSING_DATE");

        tArtistExhibit = new TableHelper(dbHelper, "ARTIST_EXHIBIT");
        tArtistExhibit.setColumns("ARTIST_ID", "EXHIBIT_ID");

        tGallery = new TableHelper(dbHelper, "GALLERY");
        tGallery.setColumns("GALLERY_ID", "GALLERY_NAME");
    }

    protected void createTwoArtistsAndTwoPaintingsDataSet() throws Exception {
        tArtist.insert(11, "artist2");
        tArtist.insert(101, "artist3");
        tPainting.insert(6, "p_artist3", 101, 1000);
        tPainting.insert(7, "p_artist2", 11, 2000);
    }

    protected void createArtistWithTwoPaintingsAndTwoInfosDataSet() throws Exception {
        tArtist.insert(11, "artist2");

        tPainting.insert(6, "p_artist2", 11, 1000);
        tPainting.insert(7, "p_artist3", 11, 2000);

        tPaintingInfo.insert(6, "xYs");
    }

    protected void createTwoArtistsWithExhibitsDataSet() throws Exception {
        tArtist.insert(11, "artist2");
        tArtist.insert(101, "artist3");

        tGallery.insert(25, "gallery1");
        tGallery.insert(31, "gallery2");
        tGallery.insert(45, "gallery3");

        Timestamp now = new Timestamp(System.currentTimeMillis());

        tExhibit.insert(1, 25, now, now);
        tExhibit.insert(2, 31, now, now);
        tExhibit.insert(3, 45, now, now);
        tExhibit.insert(4, 25, now, now);

        tArtistExhibit.insert(11, 2);
        tArtistExhibit.insert(11, 4);
        tArtistExhibit.insert(101, 1);
        tArtistExhibit.insert(101, 3);
        tArtistExhibit.insert(101, 4);
    }

    public void testPrefetchToMany_ViaProperty() throws Exception {
        createTwoArtistsAndTwoPaintingsDataSet();

        SelectQuery<Artist> q = new SelectQuery<Artist>(Artist.class);
        q.addPrefetch(Artist.PAINTING_ARRAY.disjoint());

        final List<Artist> artists = context.select(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {

                assertEquals(2, artists.size());

                for (int i = 0; i < 2; i++) {
                    Artist a = artists.get(i);
                    List<?> toMany = (List<?>) a.readPropertyDirectly("paintingArray");
                    assertNotNull(toMany);
                    assertFalse(((ValueHolder) toMany).isFault());
                    assertEquals(1, toMany.size());

                    Painting p = (Painting) toMany.get(0);
                    assertEquals("Invalid prefetched painting:" + p, "p_" + a.getArtistName(), p.getPaintingTitle());
                }
            }
        });
    }

    public void testPrefetchToMany_WithQualfier() throws Exception {
        createTwoArtistsAndTwoPaintingsDataSet();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("name1", "artist2");
        params.put("name2", "artist3");
        Expression e = Expression.fromString("artistName = $name1 or artistName = $name2");
        SelectQuery q = new SelectQuery("Artist", e.expWithParameters(params));
        q.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY);

        final List<Artist> artists = context.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {

                assertEquals(2, artists.size());

                Artist a1 = artists.get(0);
                List<?> toMany = (List<?>) a1.readPropertyDirectly(Artist.PAINTING_ARRAY_PROPERTY);
                assertNotNull(toMany);
                assertFalse(((ValueHolder) toMany).isFault());
                assertEquals(1, toMany.size());

                Painting p1 = (Painting) toMany.get(0);
                assertEquals("p_" + a1.getArtistName(), p1.getPaintingTitle());

                Artist a2 = artists.get(1);
                List<?> toMany2 = (List<?>) a2.readPropertyDirectly(Artist.PAINTING_ARRAY_PROPERTY);
                assertNotNull(toMany2);
                assertFalse(((ValueHolder) toMany2).isFault());
                assertEquals(1, toMany2.size());

                Painting p2 = (Painting) toMany2.get(0);
                assertEquals("p_" + a2.getArtistName(), p2.getPaintingTitle());
            }
        });
    }

    public void testPrefetchToManyNoQualifier() throws Exception {
        createTwoArtistsAndTwoPaintingsDataSet();

        SelectQuery q = new SelectQuery(Artist.class);
        q.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY);

        final List<Artist> artists = context.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {

                assertEquals(2, artists.size());

                for (int i = 0; i < 2; i++) {
                    Artist a = artists.get(i);
                    List<?> toMany = (List<?>) a.readPropertyDirectly("paintingArray");
                    assertNotNull(toMany);
                    assertFalse(((ValueHolder) toMany).isFault());
                    assertEquals(1, toMany.size());

                    Painting p = (Painting) toMany.get(0);
                    assertEquals("Invalid prefetched painting:" + p, "p_" + a.getArtistName(), p.getPaintingTitle());
                }
            }
        });
    }

    /**
     * Test that a to-many relationship is initialized when a target entity has
     * a compound PK only partially involved in relationship.
     */
    public void testPrefetchToMany_OnJoinTableDisjoinedPrefetch() throws Exception {

        createTwoArtistsWithExhibitsDataSet();

        SelectQuery q = new SelectQuery(Artist.class);
        q.addPrefetch(Artist.ARTIST_EXHIBIT_ARRAY_PROPERTY).setSemantics(PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);
        q.addOrdering(Artist.ARTIST_NAME_PROPERTY, SortOrder.ASCENDING);

        final List<Artist> artists = context.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals(2, artists.size());

                Artist a1 = artists.get(0);
                assertEquals("artist2", a1.getArtistName());
                List<?> toMany = (List<?>) a1.readPropertyDirectly(Artist.ARTIST_EXHIBIT_ARRAY_PROPERTY);
                assertNotNull(toMany);
                assertFalse(((ValueHolder) toMany).isFault());
                assertEquals(2, toMany.size());

                ArtistExhibit artistExhibit = (ArtistExhibit) toMany.get(0);
                assertEquals(PersistenceState.COMMITTED, artistExhibit.getPersistenceState());
                assertSame(a1, artistExhibit.getToArtist());

                Artist a2 = artists.get(1);
                assertEquals("artist3", a2.getArtistName());
                List<?> toMany2 = (List<?>) a2.readPropertyDirectly(Artist.ARTIST_EXHIBIT_ARRAY_PROPERTY);
                assertNotNull(toMany2);
                assertFalse(((ValueHolder) toMany2).isFault());
                assertEquals(3, toMany2.size());

                ArtistExhibit artistExhibit2 = (ArtistExhibit) toMany2.get(0);
                assertEquals(PersistenceState.COMMITTED, artistExhibit2.getPersistenceState());
                assertSame(a2, artistExhibit2.getToArtist());
            }
        });
    }

    public void testPrefetchToManyOnJoinTableJoinedPrefetch_ViaProperty() throws Exception {
        createTwoArtistsWithExhibitsDataSet();

        SelectQuery<Artist> q = new SelectQuery<Artist>(Artist.class);
        q.addPrefetch(Artist.ARTIST_EXHIBIT_ARRAY.joint());
        q.addOrdering(Artist.ARTIST_NAME.asc());

        final List<Artist> artists = context.select(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {

                assertEquals(2, artists.size());

                Artist a1 = artists.get(0);
                assertEquals("artist2", a1.getArtistName());
                List<?> toMany = (List<?>) a1.readPropertyDirectly(Artist.ARTIST_EXHIBIT_ARRAY.getName());
                assertNotNull(toMany);
                assertFalse(((ValueHolder) toMany).isFault());
                assertEquals(2, toMany.size());

                ArtistExhibit artistExhibit = (ArtistExhibit) toMany.get(0);
                assertEquals(PersistenceState.COMMITTED, artistExhibit.getPersistenceState());
                assertSame(a1, artistExhibit.getToArtist());

                Artist a2 = artists.get(1);
                assertEquals("artist3", a2.getArtistName());
                List<?> toMany2 = (List<?>) a2.readPropertyDirectly(Artist.ARTIST_EXHIBIT_ARRAY.getName());
                assertNotNull(toMany2);
                assertFalse(((ValueHolder) toMany2).isFault());
                assertEquals(3, toMany2.size());

                ArtistExhibit artistExhibit2 = (ArtistExhibit) toMany2.get(0);
                assertEquals(PersistenceState.COMMITTED, artistExhibit2.getPersistenceState());
                assertSame(a2, artistExhibit2.getToArtist());
            }
        });
    }

    /**
     * Test that a to-many relationship is initialized when a target entity has
     * a compound PK only partially involved in relationship.
     */
    public void testPrefetchToManyOnJoinTableJoinedPrefetch() throws Exception {
        createTwoArtistsWithExhibitsDataSet();

        SelectQuery q = new SelectQuery(Artist.class);
        q.addPrefetch("artistExhibitArray").setSemantics(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
        q.addOrdering(Artist.ARTIST_NAME_PROPERTY, SortOrder.ASCENDING);

        final List<Artist> artists = context.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {

                assertEquals(2, artists.size());

                Artist a1 = artists.get(0);
                assertEquals("artist2", a1.getArtistName());
                List<?> toMany = (List<?>) a1.readPropertyDirectly("artistExhibitArray");
                assertNotNull(toMany);
                assertFalse(((ValueHolder) toMany).isFault());
                assertEquals(2, toMany.size());

                ArtistExhibit artistExhibit = (ArtistExhibit) toMany.get(0);
                assertEquals(PersistenceState.COMMITTED, artistExhibit.getPersistenceState());
                assertSame(a1, artistExhibit.getToArtist());

                Artist a2 = artists.get(1);
                assertEquals("artist3", a2.getArtistName());
                List<?> toMany2 = (List<?>) a2.readPropertyDirectly(Artist.ARTIST_EXHIBIT_ARRAY_PROPERTY);
                assertNotNull(toMany2);
                assertFalse(((ValueHolder) toMany2).isFault());
                assertEquals(3, toMany2.size());

                ArtistExhibit artistExhibit2 = (ArtistExhibit) toMany2.get(0);
                assertEquals(PersistenceState.COMMITTED, artistExhibit2.getPersistenceState());
                assertSame(a2, artistExhibit2.getToArtist());
            }
        });
    }

    /**
     * Test that a to-many relationship is initialized when there is no inverse
     * relationship
     */
    public void testPrefetch_ToManyNoReverse() throws Exception {
        createTwoArtistsAndTwoPaintingsDataSet();

        ObjEntity paintingEntity = context.getEntityResolver().getObjEntity(Painting.class);
        ObjRelationship relationship = paintingEntity.getRelationship("toArtist");
        paintingEntity.removeRelationship("toArtist");

        try {
            SelectQuery q = new SelectQuery(Artist.class);
            q.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY);
            final List<Artist> result = context.performQuery(q);

            queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

                public void execute() {
                    assertFalse(result.isEmpty());
                    Artist a1 = result.get(0);
                    List<?> toMany = (List<?>) a1.readPropertyDirectly("paintingArray");
                    assertNotNull(toMany);
                    assertFalse(((ValueHolder) toMany).isFault());
                }
            });
        } finally {
            paintingEntity.addRelationship(relationship);
        }
    }

    public void testPrefetch_ToManyNoReverseWithQualifier() throws Exception {
        createTwoArtistsAndTwoPaintingsDataSet();

        ObjEntity paintingEntity = context.getEntityResolver().getObjEntity(Painting.class);
        ObjRelationship relationship = paintingEntity.getRelationship("toArtist");
        paintingEntity.removeRelationship("toArtist");

        try {

            SelectQuery q = new SelectQuery(Artist.class);
            q.setQualifier(ExpressionFactory.matchExp("artistName", "artist2"));
            q.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY);

            final List<Artist> result = context.performQuery(q);

            queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

                public void execute() {
                    assertFalse(result.isEmpty());
                    Artist a1 = result.get(0);
                    List<?> toMany = (List<?>) a1.readPropertyDirectly("paintingArray");
                    assertNotNull(toMany);
                    assertFalse(((ValueHolder) toMany).isFault());
                }
            });

        } finally {
            paintingEntity.addRelationship(relationship);
        }
    }

    public void testPrefetch_ToOne() throws Exception {
        createTwoArtistsAndTwoPaintingsDataSet();

        SelectQuery q = new SelectQuery(Painting.class);
        q.addPrefetch(Painting.TO_ARTIST_PROPERTY);

        final List<Painting> result = context.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertFalse(result.isEmpty());
                Painting p1 = result.get(0);

                Object toOnePrefetch = p1.readNestedProperty("toArtist");
                assertNotNull(toOnePrefetch);
                assertTrue("Expected Artist, got: " + toOnePrefetch.getClass().getName(),
                        toOnePrefetch instanceof Artist);

                Artist a1 = (Artist) toOnePrefetch;
                assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
            }
        });
    }

    public void testPrefetch_ToOne_DbPath() throws Exception {
        createTwoArtistsAndTwoPaintingsDataSet();

        SelectQuery q = new SelectQuery(Painting.class);
        q.addPrefetch(Painting.TO_ARTIST_PROPERTY);
        q.andQualifier(ExpressionFactory.matchDbExp("toArtist.ARTIST_NAME", "artist2"));

        List<Painting> results = context.performQuery(q);

        assertEquals(1, results.size());
    }

    public void testPrefetch_ToOne_ObjPath() throws Exception {
        createTwoArtistsAndTwoPaintingsDataSet();

        SelectQuery q = new SelectQuery(Painting.class);
        q.addPrefetch(Painting.TO_ARTIST_PROPERTY);
        q.andQualifier(ExpressionFactory.matchExp("toArtist.artistName", "artist2"));

        List<Painting> results = context.performQuery(q);
        assertEquals(1, results.size());
    }

    public void testPrefetch_ReflexiveRelationship() throws Exception {
        ArtGroup parent = (ArtGroup) context.newObject("ArtGroup");
        parent.setName("parent");
        ArtGroup child = (ArtGroup) context.newObject("ArtGroup");
        child.setName("child");
        child.setToParentGroup(parent);
        context.commitChanges();

        SelectQuery q = new SelectQuery("ArtGroup");
        q.setQualifier(ExpressionFactory.matchExp("name", "child"));
        q.addPrefetch("toParentGroup");

        final List<ArtGroup> results = context.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals(1, results.size());

                ArtGroup fetchedChild = results.get(0);
                // The parent must be fully fetched, not just HOLLOW (a fault)
                assertEquals(PersistenceState.COMMITTED, fetchedChild.getToParentGroup().getPersistenceState());
            }
        });
    }

    public void testPrefetch_ToOneWithQualifierOverlappingPrefetchPath() throws Exception {
        createTwoArtistsAndTwoPaintingsDataSet();

        Expression exp = ExpressionFactory.matchExp("toArtist.artistName", "artist3");

        SelectQuery q = new SelectQuery(Painting.class, exp);
        q.addPrefetch(Painting.TO_ARTIST_PROPERTY);

        final List<Painting> results = context.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals(1, results.size());

                Painting painting = results.get(0);

                // The parent must be fully fetched, not just HOLLOW (a fault)
                assertEquals(PersistenceState.COMMITTED, painting.getToArtist().getPersistenceState());
            }
        });
    }

    public void testPrefetch9() throws Exception {
        createTwoArtistsAndTwoPaintingsDataSet();

        Expression artistExp = ExpressionFactory.matchExp("artistName", "artist3");
        SelectQuery artistQuery = new SelectQuery(Artist.class, artistExp);
        Artist artist1 = (Artist) context.performQuery(artistQuery).get(0);

        // find the painting not matching the artist (this is the case where
        // such prefetch
        // at least makes sense)
        Expression exp = ExpressionFactory.noMatchExp("toArtist", artist1);

        SelectQuery q = new SelectQuery(Painting.class, exp);
        q.addPrefetch("toArtist");

        final List<Painting> results = context.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals(1, results.size());

                // see that artists are resolved...

                Painting px = results.get(0);
                Artist ax = (Artist) px.readProperty(Painting.TO_ARTIST_PROPERTY);
                assertEquals(PersistenceState.COMMITTED, ax.getPersistenceState());
            }
        });
    }

    public void testPrefetch_OneToOneWithQualifier() throws Exception {
        createArtistWithTwoPaintingsAndTwoInfosDataSet();

        Expression e = ExpressionFactory.likeExp("toArtist.artistName", "a%");
        SelectQuery q = new SelectQuery(Painting.class, e);
        q.addPrefetch(Painting.TO_PAINTING_INFO_PROPERTY);
        q.addOrdering(Painting.PAINTING_TITLE_PROPERTY, SortOrder.ASCENDING);

        final List<Painting> results = context.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals(2, results.size());

                // testing non-null to-one target
                Painting p0 = results.get(0);
                Object o2 = p0.readPropertyDirectly(Painting.TO_PAINTING_INFO_PROPERTY);
                assertTrue(o2 instanceof PaintingInfo);
                PaintingInfo pi2 = (PaintingInfo) o2;
                assertEquals(PersistenceState.COMMITTED, pi2.getPersistenceState());
                assertEquals(Cayenne.intPKForObject(p0), Cayenne.intPKForObject(pi2));

                // testing null to-one target
                Painting p1 = results.get(1);
                assertNull(p1.readPropertyDirectly(Painting.TO_PAINTING_INFO_PROPERTY));

                // there was a bug marking an object as dirty when clearing the
                // relationships
                assertEquals(PersistenceState.COMMITTED, p1.getPersistenceState());
            }
        });
    }

    public void testPrefetchToMany_DateInQualifier() throws Exception {
        createTwoArtistsAndTwoPaintingsDataSet();

        Expression e = ExpressionFactory.matchExp("dateOfBirth", new Date());
        SelectQuery q = new SelectQuery(Artist.class, e);
        q.addPrefetch("paintingArray");

        // prefetch with query using date in qualifier used to fail on SQL
        // Server
        // see CAY-119 for details
        context.performQuery(q);
    }

    public void testPrefetchingToOneNull() throws Exception {

        tPainting.insert(6, "p_Xty", null, 1000);

        SelectQuery q = new SelectQuery(Painting.class);
        q.addPrefetch(Painting.TO_ARTIST_PROPERTY);

        final List<Painting> paintings = context.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals(1, paintings.size());

                Painting p2 = paintings.get(0);
                assertNull(p2.readProperty(Painting.TO_ARTIST_PROPERTY));
            }
        });
    }

    public void testPrefetchToOneSharedCache() throws Exception {
        createTwoArtistsAndTwoPaintingsDataSet();

        final SelectQuery q = new SelectQuery(Painting.class);
        q.addPrefetch(Painting.TO_ARTIST_PROPERTY);
        q.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);

        context.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                // per CAY-499 second run of a cached query with prefetches
                // (i.e. when the
                // result is served from cache) used to throw an exception...

                List<Painting> cachedResult = context.performQuery(q);

                assertFalse(cachedResult.isEmpty());
                Painting p1 = cachedResult.get(0);

                Object toOnePrefetch = p1.readNestedProperty("toArtist");
                assertNotNull(toOnePrefetch);
                assertTrue("Expected Artist, got: " + toOnePrefetch.getClass().getName(),
                        toOnePrefetch instanceof Artist);

                Artist a1 = (Artist) toOnePrefetch;
                assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());

                // and just in case - run one more time...
                context.performQuery(q);
            }
        });
    }

    public void testPrefetchToOneLocalCache() throws Exception {
        createTwoArtistsAndTwoPaintingsDataSet();

        final SelectQuery q = new SelectQuery(Painting.class);
        q.addPrefetch(Painting.TO_ARTIST_PROPERTY);
        q.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);

        context.performQuery(q);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                // per CAY-499 second run of a cached query with prefetches
                // (i.e. when the
                // result is served from cache) used to throw an exception...

                List<Painting> cachedResult = context.performQuery(q);

                assertFalse(cachedResult.isEmpty());
                Painting p1 = cachedResult.get(0);

                Object toOnePrefetch = p1.readNestedProperty("toArtist");
                assertNotNull(toOnePrefetch);
                assertTrue("Expected Artist, got: " + toOnePrefetch.getClass().getName(),
                        toOnePrefetch instanceof Artist);

                Artist a1 = (Artist) toOnePrefetch;
                assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());

                // and just in case - run one more time...
                context.performQuery(q);
            }
        });
    }

    public void testPrefetchToOneWithBackRelationship() throws Exception {
        createArtistWithTwoPaintingsAndTwoInfosDataSet();

        SelectQuery<Painting> query = new SelectQuery<Painting>(Painting.class);
        query.andQualifier(Painting.PAINTING_TITLE.eq("p_artist2"));
        query.addPrefetch(Painting.TO_PAINTING_INFO.disjoint());
        query.addPrefetch(Painting.TO_PAINTING_INFO.dot(PaintingInfo.PAINTING).disjoint());
        final List<Painting> results = context.select(query);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals(1, results.size());

                Painting p0 = results.get(0);
                PaintingInfo pi0 = (PaintingInfo) p0.readPropertyDirectly(Painting.TO_PAINTING_INFO.getName());
                assertNotNull(pi0);
                assertNotNull(pi0.readPropertyDirectly(PaintingInfo.PAINTING.getName()));
            }
        });
    }

    public void testPrefetchPaintingOverToOneAndToMany() throws Exception {
        createArtistWithTwoPaintingsAndTwoInfosDataSet();

        SelectQuery<Painting> query = new SelectQuery<Painting>(Painting.class);
        query.andQualifier(Painting.PAINTING_TITLE.eq("p_artist2"));
        query.addPrefetch(Painting.TO_ARTIST.disjoint());
        query.addPrefetch(Painting.TO_ARTIST.dot(Artist.PAINTING_ARRAY).disjoint());
        final List<Painting> results = context.select(query);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals(1, results.size());

                Painting p0 = results.get(0);
                Artist a0 = (Artist) p0.readPropertyDirectly(Painting.TO_ARTIST.getName());
                assertNotNull(a0);
                List<?> paintings = (List<?>) a0.readPropertyDirectly(Artist.PAINTING_ARRAY.getName());
                assertEquals(2, paintings.size());
            }
        });
    }

    public void testPrefetchToOneWithBackRelationship_Joint() throws Exception {
        createArtistWithTwoPaintingsAndTwoInfosDataSet();

        SelectQuery<Painting> query = new SelectQuery<Painting>(Painting.class);
        query.andQualifier(Painting.PAINTING_TITLE.eq("p_artist2"));
        query.addPrefetch(Painting.TO_PAINTING_INFO.joint());
        query.addPrefetch(Painting.TO_PAINTING_INFO.dot(PaintingInfo.PAINTING).joint());
        final List<Painting> results = context.select(query);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals(1, results.size());

                Painting p0 = results.get(0);
                PaintingInfo pi0 = (PaintingInfo) p0.readPropertyDirectly(Painting.TO_PAINTING_INFO.getName());
                assertNotNull(pi0);
                assertNotNull(pi0.readPropertyDirectly(PaintingInfo.PAINTING.getName()));
            }
        });
    }
}
