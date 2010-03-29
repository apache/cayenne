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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.art.ArtGroup;
import org.apache.art.Artist;
import org.apache.art.ArtistExhibit;
import org.apache.art.Painting;
import org.apache.art.PaintingInfo;
import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SelectQuery;

/**
 */
public class DataContextPrefetchTest extends DataContextCase {

    /**
     * Test that a to-many relationship is initialized.
     */
    public void testPrefetchToMany() throws Exception {
        createTestData("testPaintings");

        Map params = new HashMap();
        params.put("name1", "artist2");
        params.put("name2", "artist3");
        Expression e = Expression
                .fromString("artistName = $name1 or artistName = $name2");
        SelectQuery q = new SelectQuery("Artist", e.expWithParameters(params));
        q.addPrefetch("paintingArray");

        List artists = context.performQuery(q);

        blockQueries();

        try {
            assertEquals(2, artists.size());

            Artist a1 = (Artist) artists.get(0);
            List toMany = (List) a1.readPropertyDirectly("paintingArray");
            assertNotNull(toMany);
            assertFalse(((ValueHolder) toMany).isFault());
            assertEquals(1, toMany.size());

            Painting p1 = (Painting) toMany.get(0);
            assertEquals("P_" + a1.getArtistName(), p1.getPaintingTitle());

            Artist a2 = (Artist) artists.get(1);
            List toMany2 = (List) a2.readPropertyDirectly("paintingArray");
            assertNotNull(toMany2);
            assertFalse(((ValueHolder) toMany2).isFault());
            assertEquals(1, toMany2.size());

            Painting p2 = (Painting) toMany2.get(0);
            assertEquals("P_" + a2.getArtistName(), p2.getPaintingTitle());
        }
        finally {
            unblockQueries();
        }
    }

    /**
     * Test that a to-many relationship is initialized.
     */
    public void testPrefetchToManyNoQualifier() throws Exception {
        createTestData("testPaintings");
        SelectQuery q = new SelectQuery(Artist.class);
        q.addPrefetch("paintingArray");

        List artists = context.performQuery(q);

        blockQueries();
        try {
            assertEquals(artistCount, artists.size());

            for (int i = 0; i < artistCount; i++) {
                Artist a = (Artist) artists.get(i);
                List toMany = (List) a.readPropertyDirectly("paintingArray");
                assertNotNull(toMany);
                assertFalse(((ValueHolder) toMany).isFault());
                assertEquals(1, toMany.size());

                Painting p = (Painting) toMany.get(0);
                assertEquals(
                        "Invalid prefetched painting:" + p,
                        "P_" + a.getArtistName(),
                        p.getPaintingTitle());
            }
        }
        finally {
            unblockQueries();
        }
    }

    /**
     * Test that a to-many relationship is initialized when a target entity has a compound
     * PK only partially involved in relationship.
     */
    public void testPrefetchToManyOnJoinTableDisjoinedPrefetch() throws Exception {
        // setup data
        createTestData("testGalleries");
        populateExhibits();
        createTestData("testArtistExhibits");

        SelectQuery q = new SelectQuery(Artist.class);
        q.addPrefetch("artistExhibitArray").setSemantics(
                PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);
        q.addOrdering(Artist.ARTIST_NAME_PROPERTY, Ordering.ASC);

        List artists = context.performQuery(q);

        blockQueries();
        try {

            assertEquals(artistCount, artists.size());

            Artist a1 = (Artist) artists.get(0);
            assertEquals("artist1", a1.getArtistName());
            List toMany = (List) a1.readPropertyDirectly("artistExhibitArray");
            assertNotNull(toMany);
            assertFalse(((ValueHolder) toMany).isFault());
            assertEquals(2, toMany.size());

            ArtistExhibit artistExhibit = (ArtistExhibit) toMany.get(0);
            assertEquals(PersistenceState.COMMITTED, artistExhibit.getPersistenceState());
            assertSame(a1, artistExhibit.getToArtist());
        }
        finally {
            unblockQueries();
        }
    }

    /**
     * Test that a to-many relationship is initialized when a target entity has a compound
     * PK only partially involved in relationship.
     */
    public void testPrefetchToManyOnJoinTableJoinedPrefetch() throws Exception {
        // setup data
        createTestData("testGalleries");
        populateExhibits();
        createTestData("testArtistExhibits");

        SelectQuery q = new SelectQuery(Artist.class);
        q.addPrefetch("artistExhibitArray").setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
        q.addOrdering(Artist.ARTIST_NAME_PROPERTY, Ordering.ASC);

        List artists = context.performQuery(q);

        blockQueries();
        try {

            assertEquals(artistCount, artists.size());

            Artist a1 = (Artist) artists.get(0);
            assertEquals("artist1", a1.getArtistName());
            List toMany = (List) a1.readPropertyDirectly("artistExhibitArray");
            assertNotNull(toMany);
            assertFalse(((ValueHolder) toMany).isFault());
            assertEquals(2, toMany.size());

            ArtistExhibit artistExhibit = (ArtistExhibit) toMany.get(0);
            assertEquals(PersistenceState.COMMITTED, artistExhibit.getPersistenceState());
            assertSame(a1, artistExhibit.getToArtist());
        }
        finally {
            unblockQueries();
        }
    }

    /**
     * Test that a to-many relationship is initialized when there is no inverse
     * relationship
     */
    public void testPrefetch3a() throws Exception {
        createTestData("testPaintings");

        ObjEntity paintingEntity = context.getEntityResolver().lookupObjEntity(
                Painting.class);
        ObjRelationship relationship = (ObjRelationship) paintingEntity
                .getRelationship("toArtist");
        paintingEntity.removeRelationship("toArtist");

        SelectQuery q = new SelectQuery("Artist");
        q.addPrefetch("paintingArray");

        try {
            List result = context.performQuery(q);

            blockQueries();
            try {
                assertFalse(result.isEmpty());
                CayenneDataObject a1 = (CayenneDataObject) result.get(0);
                List toMany = (List) a1.readPropertyDirectly("paintingArray");
                assertNotNull(toMany);
                assertFalse(((ValueHolder) toMany).isFault());
            }
            finally {
                unblockQueries();
            }
        }
        finally {
            // Fix it up again, so other tests do not fail
            paintingEntity.addRelationship(relationship);
        }

    }

    /**
     * Test that a to-many relationship is initialized when there is no inverse
     * relationship and the root query is qualified
     */
    public void testPrefetchOneWayToMany() throws Exception {
        createTestData("testPaintings");

        ObjEntity paintingEntity = context.getEntityResolver().lookupObjEntity(
                Painting.class);
        ObjRelationship relationship = (ObjRelationship) paintingEntity
                .getRelationship("toArtist");
        paintingEntity.removeRelationship("toArtist");

        SelectQuery q = new SelectQuery("Artist");
        q.setQualifier(ExpressionFactory.matchExp("artistName", "artist1"));
        q.addPrefetch("paintingArray");

        try {
            List result = context.performQuery(q);

            blockQueries();

            try {
                assertFalse(result.isEmpty());

                CayenneDataObject a1 = (CayenneDataObject) result.get(0);
                List toMany = (List) a1.readPropertyDirectly("paintingArray");
                assertNotNull(toMany);
                assertFalse(((ValueHolder) toMany).isFault());
            }
            finally {
                unblockQueries();
            }
        }
        finally {
            // Fix it up again, so other tests do not fail
            paintingEntity.addRelationship(relationship);
        }

    }

    /**
     * Test that a to-one relationship is initialized.
     */
    public void testPrefetch4() throws Exception {
        createTestData("testPaintings");

        SelectQuery q = new SelectQuery("Painting");
        q.addPrefetch("toArtist");

        List result = context.performQuery(q);

        blockQueries();
        try {
            assertFalse(result.isEmpty());
            DataObject p1 = (DataObject) result.get(0);

            Object toOnePrefetch = p1.readNestedProperty("toArtist");
            assertNotNull(toOnePrefetch);
            assertTrue(
                    "Expected DataObject, got: " + toOnePrefetch.getClass().getName(),
                    toOnePrefetch instanceof DataObject);

            DataObject a1 = (DataObject) toOnePrefetch;
            assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
        }
        finally {
            unblockQueries();
        }
    }

    /**
     * Test prefetching with queries using DB_PATH.
     */
    public void testPrefetch5() throws Exception {
        createTestData("testPaintings");

        SelectQuery q = new SelectQuery("Painting");
        q.andQualifier(ExpressionFactory.matchDbExp("toArtist.ARTIST_NAME", "artist2"));
        q.addPrefetch("toArtist");

        List results = context.performQuery(q);

        assertEquals(1, results.size());
    }

    /**
     * Test prefetching with queries using OBJ_PATH.
     */
    public void testPrefetch6() throws Exception {
        createTestData("testPaintings");

        SelectQuery q = new SelectQuery("Painting");
        q.andQualifier(ExpressionFactory.matchExp("toArtist.artistName", "artist2"));
        q.addPrefetch("toArtist");

        List results = context.performQuery(q);
        assertEquals(1, results.size());
    }

    /**
     * Test prefetching with the prefetch on a reflexive relationship
     */
    public void testPrefetch7() throws Exception {
        ArtGroup parent = (ArtGroup) context.newObject("ArtGroup");
        parent.setName("parent");
        ArtGroup child = (ArtGroup) context.newObject("ArtGroup");
        child.setName("child");
        child.setToParentGroup(parent);
        context.commitChanges();

        SelectQuery q = new SelectQuery("ArtGroup");
        q.setQualifier(ExpressionFactory.matchExp("name", "child"));
        q.addPrefetch("toParentGroup");

        List results = context.performQuery(q);

        blockQueries();

        try {
            assertEquals(1, results.size());

            ArtGroup fetchedChild = (ArtGroup) results.get(0);
            // The parent must be fully fetched, not just HOLLOW (a fault)
            assertEquals(PersistenceState.COMMITTED, fetchedChild
                    .getToParentGroup()
                    .getPersistenceState());
        }
        finally {
            unblockQueries();
        }
    }

    /**
     * Test prefetching with qualifier on the root query containing the path to the
     * prefetch.
     */
    public void testPrefetch8() throws Exception {
        createTestData("testPaintings");
        Expression exp = ExpressionFactory.matchExp("toArtist.artistName", "artist1");

        SelectQuery q = new SelectQuery(Painting.class, exp);
        q.addPrefetch("toArtist");

        List results = context.performQuery(q);

        blockQueries();

        try {
            assertEquals(1, results.size());

            Painting painting = (Painting) results.get(0);

            // The parent must be fully fetched, not just HOLLOW (a fault)
            assertEquals(PersistenceState.COMMITTED, painting
                    .getToArtist()
                    .getPersistenceState());
        }
        finally {
            unblockQueries();
        }
    }

    /**
     * Test prefetching with qualifier on the root query being the path to the prefetch.
     */
    public void testPrefetch9() throws Exception {
        createTestData("testPaintings");
        Expression artistExp = ExpressionFactory.matchExp("artistName", "artist1");
        SelectQuery artistQuery = new SelectQuery(Artist.class, artistExp);
        Artist artist1 = (Artist) context.performQuery(artistQuery).get(0);

        // find the painting not matching the artist (this is the case where such prefetch
        // at least makes sense)
        Expression exp = ExpressionFactory.noMatchExp("toArtist", artist1);

        SelectQuery q = new SelectQuery(Painting.class, exp);
        q.addPrefetch("toArtist");

        // run the query ... see that it doesn't blow
        List paintings = context.performQuery(q);

        blockQueries();
        try {
            assertEquals(24, paintings.size());

            // see that artists are resolved...

            Painting px = (Painting) paintings.get(3);
            Artist ax = (Artist) px.readProperty(Painting.TO_ARTIST_PROPERTY);
            assertEquals(PersistenceState.COMMITTED, ax.getPersistenceState());
        }
        finally {
            unblockQueries();
        }

    }

    public void testPrefetchOneToOne() throws Exception {
        createTestData("testPaintingInfos");

        Expression e = ExpressionFactory.likeExp("toArtist.artistName", "a%");
        SelectQuery q = new SelectQuery(Painting.class, e);
        q.addPrefetch(Painting.TO_PAINTING_INFO_PROPERTY);
        q.addOrdering(Painting.PAINTING_TITLE_PROPERTY, true);

        List results = context.performQuery(q);

        blockQueries();

        try {
            assertEquals(4, results.size());

            // testing non-null to-one target
            Painting p2 = (Painting) results.get(1);
            Object o2 = p2.readPropertyDirectly(Painting.TO_PAINTING_INFO_PROPERTY);
            assertTrue(o2 instanceof PaintingInfo);
            PaintingInfo pi2 = (PaintingInfo) o2;
            assertEquals(PersistenceState.COMMITTED, pi2.getPersistenceState());
            assertEquals(DataObjectUtils.intPKForObject(p2), DataObjectUtils
                    .intPKForObject(pi2));

            // testing null to-one target
            Painting p4 = (Painting) results.get(3);
            assertNull(p4.readPropertyDirectly(Painting.TO_PAINTING_INFO_PROPERTY));

            // there was a bug marking an object as dirty when clearing the relationships
            assertEquals(PersistenceState.COMMITTED, p4.getPersistenceState());
        }
        finally {
            unblockQueries();
        }
    }

    public void testCAY119() throws Exception {
        createTestData("testPaintings");

        Expression e = ExpressionFactory.matchExp("dateOfBirth", new Date());
        SelectQuery q = new SelectQuery(Artist.class, e);
        q.addPrefetch("paintingArray");

        // prefetch with query using date in qualifier used to fail on SQL Server
        // see CAY-119 for details
        context.performQuery(q);
    }

    public void testPrefetchingToOneNull() throws Exception {

        Painting p1 = context.newObject(Painting.class);
        p1.setPaintingTitle("aaaa");

        context.commitChanges();
        context.invalidateObjects(Collections.singleton(p1));

        SelectQuery q = new SelectQuery(Painting.class);
        q.addPrefetch("toArtist");

        // run the query ... see that it doesn't blow
        List paintings = context.performQuery(q);

        blockQueries();
        try {
            assertEquals(1, paintings.size());

            Painting p2 = (Painting) paintings.get(0);
            assertNull(p2.readProperty(Painting.TO_ARTIST_PROPERTY));
        }
        finally {
            unblockQueries();
        }
    }

    public void testPrefetchToOneSharedCache() throws Exception {
        createTestData("testPaintings");

        SelectQuery q = new SelectQuery("Painting");
        q.addPrefetch("toArtist");
        q.setName("__testPrefetchToOneSharedCache__" + System.currentTimeMillis());
        q.setCachePolicy(QueryMetadata.SHARED_CACHE);

        context.performQuery(q);

        blockQueries();
        try {
            // per CAY-499 second run of a cached query with prefetches (i.e. when the
            // result is served from cache) used to throw an exception...

            List cachedResult = context.performQuery(q);

            assertFalse(cachedResult.isEmpty());
            DataObject p1 = (DataObject) cachedResult.get(0);

            Object toOnePrefetch = p1.readNestedProperty("toArtist");
            assertNotNull(toOnePrefetch);
            assertTrue(
                    "Expected DataObject, got: " + toOnePrefetch.getClass().getName(),
                    toOnePrefetch instanceof DataObject);

            DataObject a1 = (DataObject) toOnePrefetch;
            assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());

            // and just in case - run one more time...
            context.performQuery(q);
        }
        finally {
            unblockQueries();
        }
    }

    public void testPrefetchToOneLocalCache() throws Exception {
        createTestData("testPaintings");

        SelectQuery q = new SelectQuery("Painting");
        q.addPrefetch("toArtist");
        q.setName("__testPrefetchToOneLocalCache__" + System.currentTimeMillis());
        q.setCachePolicy(QueryMetadata.LOCAL_CACHE);

        context.performQuery(q);

        blockQueries();
        try {
            // per CAY-499 second run of a cached query with prefetches (i.e. when the
            // result is served from cache) used to throw an exception...

            List cachedResult = context.performQuery(q);

            assertFalse(cachedResult.isEmpty());
            DataObject p1 = (DataObject) cachedResult.get(0);

            Object toOnePrefetch = p1.readNestedProperty("toArtist");
            assertNotNull(toOnePrefetch);
            assertTrue(
                    "Expected DataObject, got: " + toOnePrefetch.getClass().getName(),
                    toOnePrefetch instanceof DataObject);

            DataObject a1 = (DataObject) toOnePrefetch;
            assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());

            // and just in case - run one more time...
            context.performQuery(q);
        }
        finally {
            unblockQueries();
        }
    }
}
