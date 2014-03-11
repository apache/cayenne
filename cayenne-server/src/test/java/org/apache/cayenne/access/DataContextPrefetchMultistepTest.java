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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.Fault;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.ArtistExhibit;
import org.apache.cayenne.testdo.testmap.Exhibit;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextPrefetchMultistepTest extends ServerCase {

    @Inject
    protected DataContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tArtist;
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

        tExhibit = new TableHelper(dbHelper, "EXHIBIT");
        tExhibit.setColumns("EXHIBIT_ID", "GALLERY_ID", "OPENING_DATE", "CLOSING_DATE");

        tArtistExhibit = new TableHelper(dbHelper, "ARTIST_EXHIBIT");
        tArtistExhibit.setColumns("ARTIST_ID", "EXHIBIT_ID");

        tGallery = new TableHelper(dbHelper, "GALLERY");
        tGallery.setColumns("GALLERY_ID", "GALLERY_NAME");
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
        tArtistExhibit.insert(101, 2);
        tArtistExhibit.insert(101, 4);
    }
    
    protected void createGalleriesAndArtists() throws Exception {
        tArtist.insert(11, "artist2");
        tArtist.insert(101, "artist3");

        tGallery.insert(25, "gallery1");
        tGallery.insert(31, "gallery2");
        tGallery.insert(45, "gallery3");
    }

    public void testToManyToManyFirstStepUnresolved() throws Exception {

        createTwoArtistsWithExhibitsDataSet();

        // since objects for the phantom prefetches are not retained explicitly, they may
        // get garbage collected, and we won't be able to detect them
        // so ensure ObjectStore uses a regular map just for this test

        context.getObjectStore().objectMap = new HashMap<Object, Persistent>();

        // Check the target ArtistExhibit objects do not exist yet

        Map<String, Object> id1 = new HashMap<String, Object>();
        id1.put("ARTIST_ID", 11);
        id1.put("EXHIBIT_ID", 2);
        ObjectId oid1 = new ObjectId("ArtistExhibit", id1);

        Map<String, Object> id2 = new HashMap<String, Object>();
        id2.put("ARTIST_ID", 101);
        id2.put("EXHIBIT_ID", 2);
        ObjectId oid2 = new ObjectId("ArtistExhibit", id2);

        assertNull(context.getGraphManager().getNode(oid1));
        assertNull(context.getGraphManager().getNode(oid2));

        Expression e = Expression.fromString("galleryName = $name");
        SelectQuery<Gallery> q = SelectQuery.query(Gallery.class, e.expWithParameters(Collections
                .singletonMap("name", "gallery2")));
        q.addPrefetch("exhibitArray.artistExhibitArray");

        List<Gallery> galleries = context.select(q);
        assertEquals(1, galleries.size());

        Gallery g2 = galleries.get(0);

        // this relationship wasn't explicitly prefetched....
        Object list = g2.readPropertyDirectly("exhibitArray");
        assertTrue(list instanceof Fault);

        // however the target objects must be resolved
        ArtistExhibit ae1 = (ArtistExhibit) context.getGraphManager().getNode(oid1);
        ArtistExhibit ae2 = (ArtistExhibit) context.getGraphManager().getNode(oid2);

        assertNotNull(ae1);
        assertNotNull(ae2);
        assertEquals(PersistenceState.COMMITTED, ae1.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, ae2.getPersistenceState());
    }

    public void testToManyToManyFirstStepResolved() throws Exception {

        createTwoArtistsWithExhibitsDataSet();

        Expression e = Expression.fromString("galleryName = $name");
        SelectQuery<Gallery> q = SelectQuery.query(Gallery.class, e.expWithParameters(Collections
                .singletonMap("name", "gallery2")));
        q.addPrefetch("exhibitArray");
        q.addPrefetch("exhibitArray.artistExhibitArray");

        List<Gallery> galleries = context.select(q);
        assertEquals(1, galleries.size());

        Gallery g2 = galleries.get(0);

        // this relationship should be resolved
        assertTrue(g2.readPropertyDirectly("exhibitArray") instanceof ValueHolder);
        List<Exhibit> exhibits = (List<Exhibit>) g2.readPropertyDirectly("exhibitArray");
        assertFalse(((ValueHolder) exhibits).isFault());
        assertEquals(1, exhibits.size());

        Exhibit e1 = exhibits.get(0);
        assertEquals(PersistenceState.COMMITTED, e1.getPersistenceState());

        // this to-many must also be resolved
        assertTrue(e1.readPropertyDirectly("artistExhibitArray") instanceof ValueHolder);
        List<ArtistExhibit> aexhibits = (List<ArtistExhibit>) e1
                .readPropertyDirectly("artistExhibitArray");
        assertFalse(((ValueHolder) aexhibits).isFault());
        assertEquals(1, exhibits.size());

        ArtistExhibit ae1 = aexhibits.get(0);
        assertEquals(PersistenceState.COMMITTED, ae1.getPersistenceState());
    }

    public void testMixedPrefetch1() throws Exception {

        createTwoArtistsWithExhibitsDataSet();

        Expression e = Expression.fromString("galleryName = $name");
        SelectQuery<Gallery> q = SelectQuery.query(Gallery.class, e.expWithParameters(Collections
                .singletonMap("name", "gallery2")));
        q.addPrefetch("exhibitArray").setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
        q.addPrefetch("exhibitArray.artistExhibitArray");

        List<Gallery> galleries = context.select(q);
        assertEquals(1, galleries.size());

        Gallery g2 = galleries.get(0);

        // this relationship should be resolved
        assertTrue(g2.readPropertyDirectly("exhibitArray") instanceof ValueHolder);
        List<Exhibit> exhibits = (List<Exhibit>) g2.readPropertyDirectly("exhibitArray");
        assertFalse(((ValueHolder) exhibits).isFault());
        assertEquals(1, exhibits.size());

        Exhibit e1 = exhibits.get(0);
        assertEquals(PersistenceState.COMMITTED, e1.getPersistenceState());

        // this to-many must also be resolved
        assertTrue(e1.readPropertyDirectly("artistExhibitArray") instanceof ValueHolder);
        List<ArtistExhibit> aexhibits = (List<ArtistExhibit>) e1
                .readPropertyDirectly("artistExhibitArray");
        assertFalse(((ValueHolder) aexhibits).isFault());
        assertEquals(2, aexhibits.size());

        ArtistExhibit ae1 = aexhibits.get(0);
        assertEquals(PersistenceState.COMMITTED, ae1.getPersistenceState());
    }

    public void testMixedPrefetch2() throws Exception {

        createTwoArtistsWithExhibitsDataSet();

        Expression e = Expression.fromString("galleryName = $name");
        SelectQuery<Gallery> q = SelectQuery.query(Gallery.class, e.expWithParameters(Collections
                .singletonMap("name", "gallery2")));

        // reverse the order of prefetches compared to the previous test
        q.addPrefetch("exhibitArray");
        q.addPrefetch("exhibitArray.artistExhibitArray").setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        List<Gallery> galleries = context.select(q);
        assertEquals(1, galleries.size());

        Gallery g2 = galleries.get(0);

        // this relationship should be resolved
        assertTrue(g2.readPropertyDirectly("exhibitArray") instanceof ValueHolder);
        List<Exhibit> exhibits = (List<Exhibit>) g2.readPropertyDirectly("exhibitArray");
        assertFalse(((ValueHolder) exhibits).isFault());
        assertEquals(1, exhibits.size());

        Exhibit e1 = exhibits.get(0);
        assertEquals(PersistenceState.COMMITTED, e1.getPersistenceState());

        // this to-many must also be resolved
        assertTrue(e1.readPropertyDirectly("artistExhibitArray") instanceof ValueHolder);
        List<ArtistExhibit> aexhibits = (List<ArtistExhibit>) e1
                .readPropertyDirectly("artistExhibitArray");
        assertFalse(((ValueHolder) aexhibits).isFault());
        assertEquals(2, aexhibits.size());

        ArtistExhibit ae1 = aexhibits.get(0);
        assertEquals(PersistenceState.COMMITTED, ae1.getPersistenceState());
    }
    
    public void testToManyToOne_EmptyToMany() throws Exception {

        createGalleriesAndArtists();

        SelectQuery<Gallery> q = SelectQuery.query(Gallery.class, Gallery.GALLERY_NAME.eq("gallery2"));
        q.addPrefetch(Gallery.PAINTING_ARRAY.disjoint());
        q.addPrefetch(Gallery.PAINTING_ARRAY.dot(Painting.TO_ARTIST).disjoint());

        List<Gallery> galleries = context.select(q);
        assertEquals(1, galleries.size());

        Gallery g2 = galleries.get(0);

        // this relationship should be resolved
        assertTrue(g2.readPropertyDirectly(Gallery.PAINTING_ARRAY.getName()) instanceof ValueHolder);
        List<Painting> exhibits = (List<Painting>) g2.readPropertyDirectly(Gallery.PAINTING_ARRAY.getName());
        assertFalse(((ValueHolder) exhibits).isFault());
        assertEquals(0, exhibits.size());
    }
    
    public void testToManyToOne_EmptyToMany_NoRootQualifier() throws Exception {

        createGalleriesAndArtists();

        SelectQuery<Gallery> q = SelectQuery.query(Gallery.class);
        q.addPrefetch(Gallery.PAINTING_ARRAY.disjoint());
        q.addPrefetch(Gallery.PAINTING_ARRAY.dot(Painting.TO_ARTIST).disjoint());

        List<Gallery> galleries = context.select(q);
        assertEquals(3, galleries.size());

        Gallery g = galleries.get(0);

        // this relationship should be resolved
        assertTrue(g.readPropertyDirectly(Gallery.PAINTING_ARRAY.getName()) instanceof ValueHolder);
        List<Painting> exhibits = (List<Painting>) g.readPropertyDirectly(Gallery.PAINTING_ARRAY.getName());
        assertFalse(((ValueHolder) exhibits).isFault());
        assertEquals(0, exhibits.size());
    }
}
