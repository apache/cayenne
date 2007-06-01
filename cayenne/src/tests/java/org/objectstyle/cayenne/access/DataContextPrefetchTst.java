/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectstyle.art.ArtGroup;
import org.objectstyle.art.Artist;
import org.objectstyle.art.ArtistExhibit;
import org.objectstyle.art.Exhibit;
import org.objectstyle.art.Gallery;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.CayenneDataObject;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.access.util.SelectObserver;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.Ordering;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * @author Andrei Adamchik
 */
public class DataContextPrefetchTst extends DataContextTestBase {

    /**
     * Tests that all queries specified in prefetch are executed in a more complex
     * prefetch scenario.
     */
    public void testQueryWithPrefetches() throws Exception {
        OperationObserver observer = new SelectObserver();
        Expression e = ExpressionFactory.matchExp("artistName", "a");
        SelectQuery q = new SelectQuery("Artist", e);

        assertEquals(1, context.queryWithPrefetches(q, observer).size());
        q.addPrefetch("paintingArray");
        assertEquals(2, context.queryWithPrefetches(q, observer).size());

        q.addPrefetch("paintingArray.toGallery");
        assertEquals(3, context.queryWithPrefetches(q, observer).size());

        q.addPrefetch("artistExhibitArray.toExhibit");
        assertEquals(4, context.queryWithPrefetches(q, observer).size());
    }

    /**
     * Tests that all queries specified in prefetch are executed in a more complex
     * prefetch scenario with no reverse obj relationships.
     */
    //  TODO: this should probably be a part of PrefetchSelectQueryTst...
    public void testQueryWithPrefetchesNoReverse() throws Exception {

        OperationObserver observer = new SelectObserver();

        org.objectstyle.cayenne.map.EntityResolver er = context.getEntityResolver();
        ObjEntity paintingEntity = er.lookupObjEntity(Painting.class);
        ObjEntity galleryEntity = er.lookupObjEntity(Gallery.class);
        ObjEntity artistExhibitEntity = er.lookupObjEntity(ArtistExhibit.class);
        ObjEntity exhibitEntity = er.lookupObjEntity(Exhibit.class);
        ObjRelationship paintingToArtistRel = (ObjRelationship) paintingEntity
                .getRelationship("toArtist");
        paintingEntity.removeRelationship("toArtist");

        ObjRelationship galleryToPaintingRel = (ObjRelationship) galleryEntity
                .getRelationship("paintingArray");
        galleryEntity.removeRelationship("paintingArray");

        ObjRelationship artistExhibitToArtistRel = (ObjRelationship) artistExhibitEntity
                .getRelationship("toArtist");
        artistExhibitEntity.removeRelationship("toArtist");

        ObjRelationship exhibitToArtistExhibitRel = (ObjRelationship) exhibitEntity
                .getRelationship("artistExhibitArray");
        exhibitEntity.removeRelationship("artistExhibitArray");

        Expression e = ExpressionFactory.matchExp("artistName", "artist1");
        SelectQuery q = new SelectQuery("Artist", e);
        q.addPrefetch("paintingArray");
        q.addPrefetch("paintingArray.toGallery");
        q.addPrefetch("artistExhibitArray.toExhibit");

        try {
            assertEquals(4, context.queryWithPrefetches(q, observer).size());
        }
        finally {
            paintingEntity.addRelationship(paintingToArtistRel);
            galleryEntity.addRelationship(galleryToPaintingRel);
            artistExhibitEntity.addRelationship(artistExhibitToArtistRel);
            exhibitEntity.addRelationship(exhibitToArtistExhibitRel);
        }
    }

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
        assertEquals(2, artists.size());

        Artist a1 = (Artist) artists.get(0);
        ToManyList toMany = (ToManyList) a1.readPropertyDirectly("paintingArray");
        assertNotNull(toMany);
        assertFalse(toMany.needsFetch());
        assertEquals(1, toMany.size());

        Painting p1 = (Painting) toMany.get(0);
        assertEquals("P_" + a1.getArtistName(), p1.getPaintingTitle());

        Artist a2 = (Artist) artists.get(1);
        ToManyList toMany2 = (ToManyList) a2.readPropertyDirectly("paintingArray");
        assertNotNull(toMany2);
        assertFalse(toMany2.needsFetch());
        assertEquals(1, toMany2.size());

        Painting p2 = (Painting) toMany2.get(0);
        assertEquals("P_" + a2.getArtistName(), p2.getPaintingTitle());
    }

    /**
     * Test that a to-many relationship is initialized.
     */
    public void testPrefetchToManyNoQualifier() throws Exception {
        createTestData("testPaintings");
        SelectQuery q = new SelectQuery(Artist.class);
        q.addPrefetch("paintingArray");

        List artists = context.performQuery(q);
        assertEquals(artistCount, artists.size());

        for (int i = 0; i < artistCount; i++) {
            Artist a = (Artist) artists.get(i);
            ToManyList toMany = (ToManyList) a.readPropertyDirectly("paintingArray");
            assertNotNull(toMany);
            assertFalse(toMany.needsFetch());
            assertEquals(1, toMany.size());

            Painting p = (Painting) toMany.get(0);
            assertEquals("Invalid prefetched painting:" + p, "P_" + a.getArtistName(), p
                    .getPaintingTitle());
        }
    }

    /**
     * Test that a to-many relationship is initialized when a target entity has a compound
     * PK only partially involved in relationmship.
     */
    public void testPrefetchToManyOnJoinTable() throws Exception {
        // setup data
        createTestData("testGalleries");
        populateExhibits();
        createTestData("testArtistExhibits");

        SelectQuery q = new SelectQuery(Artist.class);
        q.addPrefetch("artistExhibitArray");
        q.addOrdering(Artist.ARTIST_NAME_PROPERTY, Ordering.ASC);

        List artists = context.performQuery(q);
        assertEquals(artistCount, artists.size());

        Artist a1 = (Artist) artists.get(0);
        assertEquals("artist1", a1.getArtistName());
        ToManyList toMany = (ToManyList) a1.readPropertyDirectly("artistExhibitArray");
        assertNotNull(toMany);
        assertFalse(toMany.needsFetch());
        assertEquals(2, toMany.size());

        ArtistExhibit artistExhibit = (ArtistExhibit) toMany.get(0);
        assertEquals(PersistenceState.COMMITTED, artistExhibit.getPersistenceState());
        assertSame(a1, artistExhibit.getToArtist());
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
            CayenneDataObject a1 = (CayenneDataObject) context.performQuery(q).get(0);
            ToManyList toMany = (ToManyList) a1.readPropertyDirectly("paintingArray");
            assertNotNull(toMany);
            assertFalse(toMany.needsFetch());
        }
        finally {
            //Fix it up again, so other tests do not fail
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
            CayenneDataObject a1 = (CayenneDataObject) context.performQuery(q).get(0);
            ToManyList toMany = (ToManyList) a1.readPropertyDirectly("paintingArray");
            assertNotNull(toMany);
            assertFalse(toMany.needsFetch());
        }
        finally {
            //Fix it up again, so other tests do not fail
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

        DataObject p1 = (DataObject) context.performQuery(q).get(0);

        // resolving the fault must not result in extra queries, since
        // artist must have been prefetched
        DataContextDelegate delegate = new DefaultDataContextDelegate() {

            public GenericSelectQuery willPerformSelect(
                    DataContext context,
                    GenericSelectQuery query) {
                throw new CayenneRuntimeException("No query expected.. attempt to run: "
                        + query);
            }
        };

        p1.getDataContext().setDelegate(delegate);

        Object toOnePrefetch = p1.readNestedProperty("toArtist");
        assertNotNull(toOnePrefetch);
        assertTrue(
                "Expected DataObject, got: " + toOnePrefetch.getClass().getName(),
                toOnePrefetch instanceof DataObject);

        DataObject a1 = (DataObject) toOnePrefetch;
        assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
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
        ArtGroup parent = (ArtGroup) context.createAndRegisterNewObject("ArtGroup");
        parent.setName("parent");
        ArtGroup child = (ArtGroup) context.createAndRegisterNewObject("ArtGroup");
        child.setName("child");
        child.setToParentGroup(parent);
        context.commitChanges();

        SelectQuery q = new SelectQuery("ArtGroup");
        q.setQualifier(ExpressionFactory.matchExp("name", "child"));
        q.addPrefetch("toParentGroup");

        List results = context.performQuery(q);
        assertEquals(1, results.size());

        ArtGroup fetchedChild = (ArtGroup) results.get(0);
        //The parent must be fully fetched, not just HOLLOW (a fault)
        assertEquals(PersistenceState.COMMITTED, fetchedChild
                .getToParentGroup()
                .getPersistenceState());
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
        assertEquals(1, results.size());

        Painting painting = (Painting) results.get(0);

        // The parent must be fully fetched, not just HOLLOW (a fault)
        assertEquals(PersistenceState.COMMITTED, painting
                .getToArtist()
                .getPersistenceState());
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

        // test how prefetches are resolved in this case - this was a stumbling block for
        // a while
        Collection prefetches = context.queryWithPrefetches(q, new SelectObserver());
        assertEquals(2, prefetches.size());

        // run the query ... see that it doesn't blow
        List paintings = context.performQuery(q);
        assertEquals(24, paintings.size());

        // see that artists are resolved...

        Painting px = (Painting) paintings.get(3);
        Artist ax = (Artist) px.readProperty(Painting.TO_ARTIST_PROPERTY);
        assertEquals(PersistenceState.COMMITTED, ax.getPersistenceState());
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
}