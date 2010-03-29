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

package org.apache.cayenne;

import java.util.List;

import org.apache.art.ArtGroup;
import org.apache.art.Artist;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.MockDataNode;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

/**
 * Test case for objects with flattened relationships.
 * 
 */
// TODO: redefine all test cases in terms of entities in "relationships" map
// and merge this test case with FlattenedRelationshipsTst that inherits
// from RelationshipTestCase.
public class CayenneDataObjectFlattenedRelTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testReadFlattenedRelationship() throws Exception {
        DataContext context = createDataContext();

        createTestData("testReadFlattenedRelationship");
        Artist a1 = DataObjectUtils.objectForPK(context, Artist.class, 33001);
        List groupList = a1.getGroupArray();
        assertNotNull(groupList);
        assertEquals(0, groupList.size());
    }

    public void testReadFlattenedRelationship2() throws Exception {
        DataContext context = createDataContext();

        createTestData("testReadFlattenedRelationship2");

        Artist a1 = DataObjectUtils.objectForPK(context, Artist.class, 33001);
        List groupList = a1.getGroupArray();
        assertNotNull(groupList);
        assertEquals(1, groupList.size());
        assertEquals(PersistenceState.COMMITTED, ((ArtGroup) groupList.get(0))
                .getPersistenceState());
        assertEquals("g1", ((ArtGroup) groupList.get(0)).getName());
    }

    public void testAddToFlattenedRelationship() throws Exception {

        createTestData("testAddToFlattenedRelationship");
        DataContext context = createDataContext();

        Artist a1 = DataObjectUtils.objectForPK(context, Artist.class, 33001);
        assertEquals(0, a1.getGroupArray().size());

        SelectQuery q = new SelectQuery(ArtGroup.class, ExpressionFactory.matchExp(
                "name",
                "g1"));
        List results = context.performQuery(q);
        assertEquals(1, results.size());

        assertFalse(context.hasChanges());
        ArtGroup group = (ArtGroup) results.get(0);
        a1.addToGroupArray(group);
        assertTrue(context.hasChanges());

        List groupList = a1.getGroupArray();
        assertEquals(1, groupList.size());
        assertEquals("g1", ((ArtGroup) groupList.get(0)).getName());

        // Ensure that the commit doesn't fail
        a1.getObjectContext().commitChanges();

        // and check again
        assertFalse(context.hasChanges());

        // refetch artist with a different context
        context = createDataContext();
        a1 = DataObjectUtils.objectForPK(context, Artist.class, 33001);
        groupList = a1.getGroupArray();
        assertEquals(1, groupList.size());
        assertEquals("g1", ((ArtGroup) groupList.get(0)).getName());
    }

    // Test case to show up a bug in committing more than once
    public void testDoubleCommitAddToFlattenedRelationship() throws Exception {
        createTestData("testDoubleCommitAddToFlattenedRelationship");
        DataContext context = createDataContext();

        Artist a1 = DataObjectUtils.objectForPK(context, Artist.class, 33001);

        SelectQuery q = new SelectQuery(ArtGroup.class, ExpressionFactory.matchExp(
                "name",
                "g1"));
        List results = context.performQuery(q);
        assertEquals(1, results.size());

        ArtGroup group = (ArtGroup) results.get(0);
        a1.addToGroupArray(group);

        List groupList = a1.getGroupArray();
        assertEquals(1, groupList.size());
        assertEquals("g1", ((ArtGroup) groupList.get(0)).getName());

        // Ensure that the commit doesn't fail
        a1.getObjectContext().commitChanges();

        try {
            // The bug caused the second commit to fail (the link record
            // was inserted again)
            a1.getObjectContext().commitChanges();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Should not have thrown an exception");
        }

    }

    public void testRemoveFromFlattenedRelationship() throws Exception {
        createTestData("testRemoveFromFlattenedRelationship");
        DataContext context = createDataContext();

        Artist a1 = DataObjectUtils.objectForPK(context, Artist.class, 33001);

        ArtGroup group = a1.getGroupArray().get(0);
        a1.removeFromGroupArray(group);

        List groupList = a1.getGroupArray();
        assertEquals(0, groupList.size());

        // Ensure that the commit doesn't fail
        a1.getObjectContext().commitChanges();

        // and check again
        groupList = a1.getGroupArray();
        assertEquals(0, groupList.size());
    }

    // Shows up a possible bug in ordering of deletes, when a flattened relationships link
    // record is deleted
    // at the same time (same transaction) as one of the record to which it links.
    public void testRemoveFlattenedRelationshipAndRootRecord() throws Exception {
        DataContext context = createDataContext();
        createTestData("testRemoveFlattenedRelationshipAndRootRecord");
        Artist a1 = DataObjectUtils.objectForPK(context, Artist.class, 33001);

        ArtGroup group = a1.getGroupArray().get(0);
        a1.removeFromGroupArray(group); // Cause the delete of the link record

        context.deleteObject(a1); // Cause the deletion of the artist

        try {
            context.commitChanges();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Should not have thrown the exception :" + e.getMessage());
        }
    }

    public void testAddRemoveFlattenedRelationship1() throws Exception {
        DataContext context = createDataContext();
        createTestData("testAddRemoveFlattenedRelationship1");
        Artist a1 = DataObjectUtils.objectForPK(context, Artist.class, 33001);

        SelectQuery q = new SelectQuery(ArtGroup.class, ExpressionFactory.matchExp(
                "name",
                "g1"));
        List results = context.performQuery(q);
        assertEquals(1, results.size());

        ArtGroup group = (ArtGroup) results.get(0);
        a1.addToGroupArray(group);
        group.removeFromArtistArray(a1);

        blockQueries();
        try {
            context.commitChanges();
        }
        finally {
            unblockQueries();
        }
    }

    public void testAddRemoveFlattenedRelationship2() throws Exception {
        createTestData("testAddRemoveFlattenedRelationship2");

        DataContext context = createDataContext();

        Artist a1 = DataObjectUtils.objectForPK(context, Artist.class, 33001);

        SelectQuery q = new SelectQuery(ArtGroup.class);
        List results = context.performQuery(q);
        assertEquals(2, results.size());

        ArtGroup g1 = (ArtGroup) results.get(0);
        ArtGroup g2 = (ArtGroup) results.get(1);
        a1.addToGroupArray(g1);
        a1.addToGroupArray(g2);

        // test that there is no delete query issued when a flattened join is first
        // added
        // and then deleted AND there are some other changes (CAY-548)
        a1.removeFromGroupArray(g1);

        MockDataNode engine = MockDataNode.interceptNode(getDomain(), getNode());
        try {
            context.commitChanges();
            assertEquals(1, engine.getRunCount());
        }
        finally {
            engine.stopInterceptNode();
        }
    }
}
