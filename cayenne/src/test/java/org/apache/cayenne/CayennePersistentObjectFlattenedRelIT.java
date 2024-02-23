/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne;

import org.apache.cayenne.access.MockDataNode;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.ArtGroup;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.Types;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class CayennePersistentObjectFlattenedRelIT extends RuntimeCase {

    @Inject
    private CayenneRuntime runtime;

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    @Inject
    private DataChannelInterceptor queryInterceptor;

    private TableHelper tArtist;

    private TableHelper tArtGroup;

    private TableHelper tArtistGroup;
    
	@Override
	public void cleanUpDB() throws Exception {
		dbHelper.update("ARTGROUP").set("PARENT_GROUP_ID", null, Types.INTEGER).execute();
		super.cleanUpDB();
	}

    @Before
    public void setUp() throws Exception {
        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tArtGroup = new TableHelper(dbHelper, "ARTGROUP");
        tArtGroup.setColumns("GROUP_ID", "NAME");

        tArtistGroup = new TableHelper(dbHelper, "ARTIST_GROUP");
        tArtistGroup.setColumns("ARTIST_ID", "GROUP_ID");
    }

    private void create1Artist1ArtGroupDataSet() throws Exception {
        tArtist.insert(33001, "artist1");
        tArtGroup.insert(1, "g1");
    }

    private void create1Artist2ArtGroupDataSet() throws Exception {
        create1Artist1ArtGroupDataSet();
        tArtGroup.insert(2, "g2");
    }

    private void create1Artist1ArtGroup1ArtistGroupDataSet() throws Exception {
        create1Artist1ArtGroupDataSet();
        tArtistGroup.insert(33001, 1);
    }

    @Test
    public void testReadFlattenedRelationship() throws Exception {
        create1Artist1ArtGroupDataSet();

        Artist a1 = Cayenne.objectForPK(context, Artist.class, 33001);
        List<ArtGroup> groupList = a1.getGroupArray();
        assertNotNull(groupList);
        assertEquals(0, groupList.size());
    }

    @Test
    public void testReadFlattenedRelationship2() throws Exception {

        create1Artist1ArtGroup1ArtistGroupDataSet();

        Artist a1 = Cayenne.objectForPK(context, Artist.class, 33001);
        List<ArtGroup> groupList = a1.getGroupArray();
        assertNotNull(groupList);
        assertEquals(1, groupList.size());
        assertEquals(PersistenceState.COMMITTED, groupList.get(0).getPersistenceState());
        assertEquals("g1", groupList.get(0).getName());
    }

    @Test
    public void testAddToFlattenedRelationship() throws Exception {

        create1Artist1ArtGroupDataSet();

        Artist a1 = Cayenne.objectForPK(context, Artist.class, 33001);
        assertEquals(0, a1.getGroupArray().size());

        List<ArtGroup> results = ObjectSelect.query(ArtGroup.class, ArtGroup.NAME.eq("g1")).select(context);
        assertEquals(1, results.size());

        assertFalse(context.hasChanges());
        ArtGroup group = results.get(0);
        a1.addToGroupArray(group);
        assertTrue(context.hasChanges());

        List<?> groupList = a1.getGroupArray();
        assertEquals(1, groupList.size());
        assertEquals("g1", ((ArtGroup) groupList.get(0)).getName());

        // Ensure that the commit doesn't fail
        a1.getObjectContext().commitChanges();

        // and check again
        assertFalse(context.hasChanges());

        // refetch artist with a different context
        ObjectContext context2 = runtime.newContext();
        a1 = Cayenne.objectForPK(context2, Artist.class, 33001);
        groupList = a1.getGroupArray();
        assertEquals(1, groupList.size());
        assertEquals("g1", ((ArtGroup) groupList.get(0)).getName());
    }

    // Test case to show up a bug in committing more than once
    @Test
    public void testDoubleCommitAddToFlattenedRelationship() throws Exception {
        create1Artist1ArtGroupDataSet();

        Artist a1 = Cayenne.objectForPK(context, Artist.class, 33001);

        List<ArtGroup> results = ObjectSelect.query(ArtGroup.class, ArtGroup.NAME.eq("g1")).select(context);
        assertEquals(1, results.size());

        ArtGroup group = results.get(0);
        a1.addToGroupArray(group);

        List<?> groupList = a1.getGroupArray();
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

    @Test
    public void testRemoveFromFlattenedRelationship() throws Exception {
        create1Artist1ArtGroup1ArtistGroupDataSet();

        Artist a1 = Cayenne.objectForPK(context, Artist.class, 33001);

        ArtGroup group = a1.getGroupArray().get(0);
        a1.removeFromGroupArray(group);

        List<ArtGroup> groupList = a1.getGroupArray();
        assertEquals(0, groupList.size());

        // Ensure that the commit doesn't fail
        a1.getObjectContext().commitChanges();

        // and check again
        groupList = a1.getGroupArray();
        assertEquals(0, groupList.size());
    }

    // Demonstrates a possible bug in ordering of deletes, when a flattened relationships
    // link record is deleted at the same time (same transaction) as one of the record to
    // which it links.
    @Test
    public void testRemoveFlattenedRelationshipAndRootRecord() throws Exception {
        create1Artist1ArtGroup1ArtistGroupDataSet();
        Artist a1 = Cayenne.objectForPK(context, Artist.class, 33001);

        ArtGroup group = a1.getGroupArray().get(0);
        a1.removeFromGroupArray(group); // Cause the delete of the link record

        context.deleteObjects(a1); // Cause the deletion of the artist

        try {
            context.commitChanges();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Should not have thrown the exception :" + e.getMessage());
        }
    }

    @Test
    public void testAddRemoveFlattenedRelationship1() throws Exception {
        create1Artist1ArtGroupDataSet();

        Artist a1 = Cayenne.objectForPK(context, Artist.class, 33001);

        List<ArtGroup> results = ObjectSelect.query(ArtGroup.class, ArtGroup.NAME.eq("g1")).select(context);;
        assertEquals(1, results.size());

        ArtGroup group = results.get(0);
        a1.addToGroupArray(group);
        group.removeFromArtistArray(a1);

        queryInterceptor.runWithQueriesBlocked(() -> context.commitChanges());
    }

    @Test
    public void testAddRemoveFlattenedRelationship2() throws Exception {
        create1Artist2ArtGroupDataSet();

        Artist a1 = Cayenne.objectForPK(context, Artist.class, 33001);

        List<?> results = ObjectSelect.query(ArtGroup.class).select(context);
        assertEquals(2, results.size());

        ArtGroup g1 = (ArtGroup) results.get(0);
        ArtGroup g2 = (ArtGroup) results.get(1);
        a1.addToGroupArray(g1);
        a1.addToGroupArray(g2);

        // test that there is no delete query issued when a flattened join is first
        // added and then deleted AND there are some other changes (CAY-548)
        a1.removeFromGroupArray(g1);

        MockDataNode nodeWrapper = MockDataNode.interceptNode(
                runtime.getDataDomain(),
                runtime.getDataDomain().getDataNodes().iterator().next());
        try {
            context.commitChanges();
        } finally {
            nodeWrapper.stopInterceptNode();
        }

        assertEquals(1, nodeWrapper.getRunCount());
    }
}
