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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.art.ArtistExhibit;
import org.apache.art.Exhibit;
import org.apache.art.Gallery;
import org.apache.cayenne.Fault;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SelectQuery;

/**
 * Testing chained prefetches...
 */
public class DataContextPrefetchMultistepTest extends DataContextCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        createTestData("testGalleries");
        populateExhibits();
        createTestData("testArtistExhibits");
    }

    public void testToManyToManyFirstStepUnresolved() throws Exception {

        // since objects for the phantom prefetches are not retained explicitly, they may
        // get garbage collected, and we won't be able to detect them
        // so ensure ObjectStore uses a regular map just for this test

        context.getObjectStore().objectMap = new HashMap<Object, Persistent>();

        // Check the target ArtistExhibit objects do not exist yet

        Map id1 = new HashMap();
        id1.put("ARTIST_ID", new Integer(33001));
        id1.put("EXHIBIT_ID", new Integer(2));
        ObjectId oid1 = new ObjectId("ArtistExhibit", id1);

        Map id2 = new HashMap();
        id2.put("ARTIST_ID", new Integer(33003));
        id2.put("EXHIBIT_ID", new Integer(2));
        ObjectId oid2 = new ObjectId("ArtistExhibit", id2);

        assertNull(context.getGraphManager().getNode(oid1));
        assertNull(context.getGraphManager().getNode(oid2));

        Expression e = Expression.fromString("galleryName = $name");
        SelectQuery q = new SelectQuery(Gallery.class, e.expWithParameters(Collections
                .singletonMap("name", "gallery2")));
        q.addPrefetch("exhibitArray.artistExhibitArray");

        List galleries = context.performQuery(q);
        assertEquals(1, galleries.size());

        Gallery g2 = (Gallery) galleries.get(0);

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

        Expression e = Expression.fromString("galleryName = $name");
        SelectQuery q = new SelectQuery(Gallery.class, e.expWithParameters(Collections
                .singletonMap("name", "gallery2")));
        q.addPrefetch("exhibitArray");
        q.addPrefetch("exhibitArray.artistExhibitArray");

        List galleries = context.performQuery(q);
        assertEquals(1, galleries.size());

        Gallery g2 = (Gallery) galleries.get(0);

        // this relationship should be resolved
        assertTrue(g2.readPropertyDirectly("exhibitArray") instanceof ValueHolder);
        List exhibits = (List) g2.readPropertyDirectly("exhibitArray");
        assertFalse(((ValueHolder) exhibits).isFault());
        assertEquals(1, exhibits.size());

        Exhibit e1 = (Exhibit) exhibits.get(0);
        assertEquals(PersistenceState.COMMITTED, e1.getPersistenceState());

        // this to-many must also be resolved
        assertTrue(e1.readPropertyDirectly("artistExhibitArray") instanceof ValueHolder);
        List aexhibits = (List) e1.readPropertyDirectly("artistExhibitArray");
        assertFalse(((ValueHolder) aexhibits).isFault());
        assertEquals(1, exhibits.size());

        ArtistExhibit ae1 = (ArtistExhibit) aexhibits.get(0);
        assertEquals(PersistenceState.COMMITTED, ae1.getPersistenceState());
    }

    public void testMixedPrefetch1() {

        Expression e = Expression.fromString("galleryName = $name");
        SelectQuery q = new SelectQuery(Gallery.class, e.expWithParameters(Collections
                .singletonMap("name", "gallery2")));
        q.addPrefetch("exhibitArray").setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
        q.addPrefetch("exhibitArray.artistExhibitArray");

        List galleries = context.performQuery(q);
        assertEquals(1, galleries.size());

        Gallery g2 = (Gallery) galleries.get(0);

        // this relationship should be resolved
        assertTrue(g2.readPropertyDirectly("exhibitArray") instanceof ValueHolder);
        List exhibits = (List) g2.readPropertyDirectly("exhibitArray");
        assertFalse(((ValueHolder) exhibits).isFault());
        assertEquals(1, exhibits.size());

        Exhibit e1 = (Exhibit) exhibits.get(0);
        assertEquals(PersistenceState.COMMITTED, e1.getPersistenceState());

        // this to-many must also be resolved
        assertTrue(e1.readPropertyDirectly("artistExhibitArray") instanceof ValueHolder);
        List aexhibits = (List) e1.readPropertyDirectly("artistExhibitArray");
        assertFalse(((ValueHolder) aexhibits).isFault());
        assertEquals(2, aexhibits.size());

        ArtistExhibit ae1 = (ArtistExhibit) aexhibits.get(0);
        assertEquals(PersistenceState.COMMITTED, ae1.getPersistenceState());
    }

    public void testMixedPrefetch2() {

        Expression e = Expression.fromString("galleryName = $name");
        SelectQuery q = new SelectQuery(Gallery.class, e.expWithParameters(Collections
                .singletonMap("name", "gallery2")));

        // reverse the order of prefetches compared to the previous test
        q.addPrefetch("exhibitArray");
        q.addPrefetch("exhibitArray.artistExhibitArray").setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        List galleries = context.performQuery(q);
        assertEquals(1, galleries.size());

        Gallery g2 = (Gallery) galleries.get(0);

        // this relationship should be resolved
        assertTrue(g2.readPropertyDirectly("exhibitArray") instanceof ValueHolder);
        List exhibits = (List) g2.readPropertyDirectly("exhibitArray");
        assertFalse(((ValueHolder) exhibits).isFault());
        assertEquals(1, exhibits.size());

        Exhibit e1 = (Exhibit) exhibits.get(0);
        assertEquals(PersistenceState.COMMITTED, e1.getPersistenceState());

        // this to-many must also be resolved
        assertTrue(e1.readPropertyDirectly("artistExhibitArray") instanceof ValueHolder);
        List aexhibits = (List) e1.readPropertyDirectly("artistExhibitArray");
        assertFalse(((ValueHolder) aexhibits).isFault());
        assertEquals(2, aexhibits.size());

        ArtistExhibit ae1 = (ArtistExhibit) aexhibits.get(0);
        assertEquals(PersistenceState.COMMITTED, ae1.getPersistenceState());
    }
}
