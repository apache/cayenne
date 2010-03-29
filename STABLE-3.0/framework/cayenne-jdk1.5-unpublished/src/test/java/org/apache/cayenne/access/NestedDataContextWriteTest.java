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

import org.apache.art.ArtGroup;
import org.apache.art.Artist;
import org.apache.art.Painting;
import org.apache.art.PaintingInfo;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

public class NestedDataContextWriteTest extends CayenneCase {

    /**
     * Overrides super implementation to ensure that created DataContext's ObjectStore
     * retains unreferenced registered objects.
     */
    @Override
    protected DataContext createDataContext() {
        DataContext context = super.createDataContext();
        context.getObjectStore().objectMap = new HashMap();
        return context;
    }

    public void testDeleteNew() throws Exception {
        deleteTestData();
        createTestData("testDeleteNew");

        DataContext context = createDataContext();
        ObjectContext childContext = context.createChildContext();

        Artist a = DataObjectUtils.objectForPK(childContext, Artist.class, 33001);
        Painting p = childContext.newObject(Painting.class);
        p.setPaintingTitle("X");
        a.addToPaintingArray(p);

        childContext.commitChangesToParent();

        childContext.deleteObject(p);
        a.removeFromPaintingArray(p);

        childContext.commitChangesToParent();
    }

    /**
     * A test case for CAY-698 bug.
     */
    public void testNullifyToOne() throws Exception {
        deleteTestData();
        createTestData("testNullifyToOne");

        DataContext context = createDataContext();
        ObjectContext childContext = context.createChildContext();
        ObjectContext childContextPeer = context.createChildContext();

        Painting childP1 = DataObjectUtils.objectForPK(
                childContext,
                Painting.class,
                33001);

        // trigger object creation in the peer nested DC
        DataObjectUtils.objectForPK(childContextPeer, Painting.class, 33001);
        childP1.setToArtist(null);

        blockQueries();

        try {
            childContext.commitChangesToParent();
            assertEquals(PersistenceState.COMMITTED, childP1.getPersistenceState());

            Painting parentP1 = (Painting) context.getGraphManager().getNode(
                    childP1.getObjectId());

            assertNotNull(parentP1);
            assertEquals(PersistenceState.MODIFIED, parentP1.getPersistenceState());
            assertNull(parentP1.getToArtist());
        }
        finally {
            unblockQueries();
        }
    }

    public void testCommitChangesToParent() throws Exception {
        deleteTestData();
        createTestData("testFlushChanges");

        DataContext context = createDataContext();
        ObjectContext childContext = context.createChildContext();

        // make sure we fetch in predictable order
        SelectQuery query = new SelectQuery(Artist.class);
        query.addOrdering(Artist.ARTIST_NAME_PROPERTY, true);
        List objects = childContext.performQuery(query);

        assertEquals(4, objects.size());

        Artist childNew = childContext.newObject(Artist.class);
        childNew.setArtistName("NNN");

        Artist childModified = (Artist) objects.get(0);
        childModified.setArtistName("MMM");

        Artist childCommitted = (Artist) objects.get(1);

        Artist childHollow = (Artist) objects.get(3);
        childContext.invalidateObjects(Collections.singleton(childHollow));

        blockQueries();

        try {
            childContext.commitChangesToParent();

            // * all modified child objects must be in committed state now
            // * all modifications should be propagated to the parent
            // * no actual commit should occur.

            assertEquals(PersistenceState.COMMITTED, childNew.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, childModified.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, childCommitted.getPersistenceState());
            assertEquals(PersistenceState.HOLLOW, childHollow.getPersistenceState());

            Artist parentNew = (Artist) context.getGraphManager().getNode(
                    childNew.getObjectId());
            Artist parentModified = (Artist) context.getGraphManager().getNode(
                    childModified.getObjectId());
            Artist parentCommitted = (Artist) context.getGraphManager().getNode(
                    childCommitted.getObjectId());
            Artist parentHollow = (Artist) context.getGraphManager().getNode(
                    childHollow.getObjectId());

            assertNotNull(parentNew);
            assertEquals(PersistenceState.NEW, parentNew.getPersistenceState());
            assertEquals("NNN", parentNew.getArtistName());

            assertNotNull(parentModified);
            assertEquals(PersistenceState.MODIFIED, parentModified.getPersistenceState());
            assertEquals("MMM", parentModified.getArtistName());
            assertNotNull(context.getObjectStore().getChangesByObjectId().get(
                    parentModified.getObjectId()));

            assertNotNull(parentCommitted);
            assertEquals(PersistenceState.COMMITTED, parentCommitted
                    .getPersistenceState());

            assertNotNull(parentHollow);
            // TODO: we can assert that when we figure out how nested "invalidate" should
            // work
            // assertEquals(PersistenceState.HOLLOW, parentHollow.getPersistenceState());
        }
        finally {
            unblockQueries();
        }
    }

    public void testCommitChangesToParentDeleted() throws Exception {
        deleteTestData();
        createTestData("testFlushChanges");

        DataContext context = createDataContext();
        ObjectContext childContext = context.createChildContext();

        // make sure we fetch in predictable order
        SelectQuery query = new SelectQuery(Artist.class);
        query.addOrdering(Artist.ARTIST_NAME_PROPERTY, true);
        List objects = childContext.performQuery(query);

        assertEquals(4, objects.size());

        // delete AND modify
        Artist childDeleted = (Artist) objects.get(2);
        childContext.deleteObject(childDeleted);
        childDeleted.setArtistName("DDD");

        // don't block queries - on delete Cayenne may need to resolve delete rules via
        // fetch
        childContext.commitChangesToParent();

        // * all modified child objects must be in committed state now
        // * all modifications should be propagated to the parent
        // * no actual commit should occur.

        assertEquals(PersistenceState.TRANSIENT, childDeleted.getPersistenceState());

        Artist parentDeleted = (Artist) context.getGraphManager().getNode(
                childDeleted.getObjectId());

        assertNotNull(parentDeleted);
        assertEquals(PersistenceState.DELETED, parentDeleted.getPersistenceState());
        assertEquals("DDD", parentDeleted.getArtistName());
    }

    public void testCommitChanges() throws Exception {
        deleteTestData();
        createTestData("testFlushChanges");

        DataContext context = createDataContext();
        ObjectContext childContext = context.createChildContext();

        // make sure we fetch in predictable order
        SelectQuery query = new SelectQuery(Artist.class);
        query.addOrdering(Artist.ARTIST_NAME_PROPERTY, true);
        List objects = childContext.performQuery(query);

        assertEquals(4, objects.size());

        Artist childNew = childContext.newObject(Artist.class);
        childNew.setArtistName("NNN");

        Artist childModified = (Artist) objects.get(0);
        childModified.setArtistName("MMM");

        Artist childCommitted = (Artist) objects.get(1);

        // delete AND modify
        Artist childDeleted = (Artist) objects.get(2);
        childContext.deleteObject(childDeleted);
        childDeleted.setArtistName("DDD");

        Artist childHollow = (Artist) objects.get(3);
        childContext.invalidateObjects(Collections.singleton(childHollow));

        childContext.commitChanges();

        assertEquals(PersistenceState.COMMITTED, childNew.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, childModified.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, childCommitted.getPersistenceState());
        assertEquals(PersistenceState.TRANSIENT, childDeleted.getPersistenceState());
        assertEquals(PersistenceState.HOLLOW, childHollow.getPersistenceState());

        Artist parentNew = (Artist) context.getGraphManager().getNode(
                childNew.getObjectId());
        Artist parentModified = (Artist) context.getGraphManager().getNode(
                childModified.getObjectId());
        Artist parentCommitted = (Artist) context.getGraphManager().getNode(
                childCommitted.getObjectId());
        Artist parentDeleted = (Artist) context.getGraphManager().getNode(
                childDeleted.getObjectId());
        Artist parentHollow = (Artist) context.getGraphManager().getNode(
                childHollow.getObjectId());

        assertNotNull(parentNew);
        assertEquals(PersistenceState.COMMITTED, parentNew.getPersistenceState());
        assertEquals("NNN", parentNew.getArtistName());

        assertNotNull(parentModified);
        assertEquals(PersistenceState.COMMITTED, parentModified.getPersistenceState());
        assertEquals("MMM", parentModified.getArtistName());
        assertNull(context.getObjectStore().getChangesByObjectId().get(
                parentModified.getObjectId()));

        assertNull("Deleted object should not be registered.", parentDeleted);

        assertNotNull(parentCommitted);
        assertEquals(PersistenceState.COMMITTED, parentCommitted.getPersistenceState());

        assertNotNull(parentHollow);
    }

    public void testCommitChangesToParent_MergeProperties() throws Exception {
        deleteTestData();
        createTestData("testCommitChangesToParent_MergeProperties");

        DataContext context = createDataContext();
        ObjectContext childContext = context.createChildContext();

        // make sure we fetch in predictable order
        SelectQuery query = new SelectQuery(Painting.class);
        query.addOrdering(Painting.PAINTING_TITLE_PROPERTY, true);
        List objects = childContext.performQuery(query);

        assertEquals(6, objects.size());

        Painting childModifiedSimple = (Painting) objects.get(0);
        childModifiedSimple.setPaintingTitle("C_PT");

        Painting childModifiedToOne = (Painting) objects.get(1);
        childModifiedToOne.setToArtist(childModifiedSimple.getToArtist());

        Artist childModifiedToMany = ((Painting) objects.get(2)).getToArtist();

        // ensure painting array is fully resolved...
        childModifiedToMany.getPaintingArray().size();
        childModifiedToMany.addToPaintingArray((Painting) objects.get(3));

        blockQueries();

        Painting parentModifiedSimple = null;
        Artist parentModifiedToMany = null;
        try {

            childContext.commitChangesToParent();

            assertEquals(PersistenceState.COMMITTED, childModifiedSimple
                    .getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, childModifiedToOne
                    .getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, childModifiedToMany
                    .getPersistenceState());

            parentModifiedSimple = (Painting) context.getGraphManager().getNode(
                    childModifiedSimple.getObjectId());

            Painting parentModifiedToOne = (Painting) context.getGraphManager().getNode(
                    childModifiedToOne.getObjectId());

            parentModifiedToMany = (Artist) context.getGraphManager().getNode(
                    childModifiedToMany.getObjectId());

            assertNotNull(parentModifiedSimple);
            assertEquals(PersistenceState.MODIFIED, parentModifiedSimple
                    .getPersistenceState());
            assertEquals("C_PT", parentModifiedSimple.getPaintingTitle());
            assertNotNull(context.getObjectStore().getChangesByObjectId().get(
                    parentModifiedSimple.getObjectId()));

            assertNotNull(parentModifiedToOne);
            assertEquals(PersistenceState.MODIFIED, parentModifiedToOne
                    .getPersistenceState());
            assertNotNull(parentModifiedToOne.getToArtist());
            assertEquals(33001, DataObjectUtils.intPKForObject(parentModifiedToOne
                    .getToArtist()));
            assertNotNull(context.getObjectStore().getChangesByObjectId().get(
                    parentModifiedToOne.getObjectId()));

            // indirectly modified....
            assertNotNull(parentModifiedToMany);
            assertEquals(PersistenceState.MODIFIED, parentModifiedToMany
                    .getPersistenceState());
        }
        finally {
            unblockQueries();
        }

        // here query is expected, as the parent was hollow and its to-many relationship
        // is unresolved
        List paintings = parentModifiedToMany.getPaintingArray();
        assertEquals(2, paintings.size());
    }

    public void testCommitChangesToParentPropagatedKey() throws Exception {
        deleteTestData();

        DataContext context = createDataContext();
        ObjectContext childContext = context.createChildContext();

        Painting childMaster = childContext.newObject(Painting.class);
        childMaster.setPaintingTitle("Master");

        PaintingInfo childDetail1 = childContext.newObject(PaintingInfo.class);
        childDetail1.setTextReview("Detail1");
        childDetail1.setPainting(childMaster);

        try {
            childContext.commitChangesToParent();

            assertEquals(PersistenceState.COMMITTED, childMaster.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, childDetail1.getPersistenceState());

            Painting parentMaster = (Painting) context.getGraphManager().getNode(
                    childMaster.getObjectId());

            assertNotNull(parentMaster);
            assertEquals(PersistenceState.NEW, parentMaster.getPersistenceState());

            PaintingInfo parentDetail1 = (PaintingInfo) context
                    .getGraphManager()
                    .getNode(childDetail1.getObjectId());

            assertNotNull(parentDetail1);
            assertEquals(PersistenceState.NEW, parentDetail1.getPersistenceState());

            assertSame(parentMaster, parentDetail1.getPainting());
            assertSame(parentDetail1, parentMaster.getToPaintingInfo());
        }
        finally {
            unblockQueries();
        }
    }

    public void testCommitChangesToParentFlattened() throws Exception {
        deleteTestData();

        DataContext context = createDataContext();
        ObjectContext childContext = context.createChildContext();

        Artist childO1 = childContext.newObject(Artist.class);
        childO1.setArtistName("Master");

        ArtGroup childO2 = childContext.newObject(ArtGroup.class);
        childO2.setName("Detail1");
        childO2.addToArtistArray(childO1);

        assertEquals(1, childO1.getGroupArray().size());
        assertEquals(1, childO2.getArtistArray().size());

        try {
            childContext.commitChangesToParent();

            assertEquals(PersistenceState.COMMITTED, childO1.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, childO2.getPersistenceState());

            Artist parentO1 = (Artist) context.getGraphManager().getNode(
                    childO1.getObjectId());

            assertNotNull(parentO1);
            assertEquals(PersistenceState.NEW, parentO1.getPersistenceState());

            ArtGroup parentO2 = (ArtGroup) context.getGraphManager().getNode(
                    childO2.getObjectId());

            assertNotNull(parentO2);
            assertEquals(PersistenceState.NEW, parentO2.getPersistenceState());

            assertEquals(1, parentO1.getGroupArray().size());
            assertEquals(1, parentO2.getArtistArray().size());
            assertTrue(parentO2.getArtistArray().contains(parentO1));
            assertTrue(parentO1.getGroupArray().contains(parentO2));
        }
        finally {
            unblockQueries();
        }
    }

    public void testCommitChangesToParentFlattenedMultipleFlush() throws Exception {
        deleteTestData();

        DataContext context = createDataContext();
        ObjectContext childContext = context.createChildContext();

        Artist childO1 = childContext.newObject(Artist.class);
        childO1.setArtistName("o1");

        ArtGroup childO2 = childContext.newObject(ArtGroup.class);
        childO2.setName("o2");
        childO2.addToArtistArray(childO1);

        childContext.commitChangesToParent();

        ArtGroup childO3 = childContext.newObject(ArtGroup.class);
        childO3.setName("o3");
        childO1.addToGroupArray(childO3);

        assertEquals(2, childO1.getGroupArray().size());
        assertEquals(1, childO2.getArtistArray().size());
        assertEquals(1, childO3.getArtistArray().size());

        try {
            childContext.commitChangesToParent();

            assertEquals(PersistenceState.COMMITTED, childO1.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, childO2.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, childO3.getPersistenceState());

            Artist parentO1 = (Artist) context.getGraphManager().getNode(
                    childO1.getObjectId());

            assertNotNull(parentO1);
            assertEquals(PersistenceState.NEW, parentO1.getPersistenceState());

            ArtGroup parentO2 = (ArtGroup) context.getGraphManager().getNode(
                    childO2.getObjectId());

            assertNotNull(parentO2);
            assertEquals(PersistenceState.NEW, parentO2.getPersistenceState());

            ArtGroup parentO3 = (ArtGroup) context.getGraphManager().getNode(
                    childO3.getObjectId());

            assertNotNull(parentO3);
            assertEquals(PersistenceState.NEW, parentO3.getPersistenceState());

            assertEquals(2, parentO1.getGroupArray().size());
            assertEquals(1, parentO2.getArtistArray().size());
            assertEquals(1, parentO3.getArtistArray().size());
            assertTrue(parentO2.getArtistArray().contains(parentO1));
            assertTrue(parentO3.getArtistArray().contains(parentO1));
            assertTrue(parentO1.getGroupArray().contains(parentO2));
            assertTrue(parentO1.getGroupArray().contains(parentO3));
        }
        finally {
            unblockQueries();
        }

        childO1.removeFromGroupArray(childO2);

        try {
            childContext.commitChangesToParent();

            assertEquals(PersistenceState.COMMITTED, childO1.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, childO2.getPersistenceState());
            assertEquals(PersistenceState.COMMITTED, childO3.getPersistenceState());

            Artist parentO1 = (Artist) context.getGraphManager().getNode(
                    childO1.getObjectId());

            assertNotNull(parentO1);
            assertEquals(PersistenceState.NEW, parentO1.getPersistenceState());

            ArtGroup parentO2 = (ArtGroup) context.getGraphManager().getNode(
                    childO2.getObjectId());

            assertNotNull(parentO2);
            assertEquals(PersistenceState.NEW, parentO2.getPersistenceState());

            ArtGroup parentO3 = (ArtGroup) context.getGraphManager().getNode(
                    childO3.getObjectId());

            assertNotNull(parentO3);
            assertEquals(PersistenceState.NEW, parentO3.getPersistenceState());

            assertEquals(1, parentO1.getGroupArray().size());
            assertEquals(0, parentO2.getArtistArray().size());
            assertEquals(1, parentO3.getArtistArray().size());

            assertTrue(parentO3.getArtistArray().contains(parentO1));
            assertTrue(parentO1.getGroupArray().contains(parentO3));
        }
        finally {
            unblockQueries();
        }
    }

    public void testAddRemove() {

        DataContext context = createDataContext();
        ObjectContext child = context.createChildContext();

        Artist a = child.newObject(Artist.class);
        a.setArtistName("X");
        child.commitChanges();

        Painting p1 = child.newObject(Painting.class);
        p1.setPaintingTitle("P1");
        a.addToPaintingArray(p1);

        Painting p2 = child.newObject(Painting.class);
        p2.setPaintingTitle("P2");
        a.addToPaintingArray(p2);

        a.removeFromPaintingArray(p2);

        // this causes an error on commit
        child.deleteObject(p2);

        child.commitChangesToParent();

    }
    
    public void testCAY1194() throws Exception {
        deleteTestData();
        
        DataContext context = createDataContext();
        
        Artist artist = context.newObject(Artist.class);
        artist.setArtistName("111");
        ObjectContext child = context.createChildContext();
        
        Painting painting = child.newObject(Painting.class);
        painting.setPaintingTitle("222");
        
        Artist localParentMt = (Artist) child.localObject(artist.getObjectId(), null);
        assertEquals(0, artist.getPaintingArray().size());
        assertEquals(0, localParentMt.getPaintingArray().size());
        
        painting.setToArtist(localParentMt);
        
        assertEquals(0, artist.getPaintingArray().size());
        assertEquals(1, localParentMt.getPaintingArray().size());
        assertEquals(localParentMt.getPaintingArray().get(0).getObjectContext(), child);
        
        child.commitChangesToParent();
        assertEquals(1, artist.getPaintingArray().size());
        assertEquals(artist.getPaintingArray().get(0).getObjectContext(), context);
        
    }
}
