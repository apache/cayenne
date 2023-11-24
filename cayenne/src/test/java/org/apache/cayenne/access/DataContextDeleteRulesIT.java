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

package org.apache.cayenne.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Types;
import java.util.List;

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.testmap.ArtGroup;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.ArtistExhibit;
import org.apache.cayenne.testdo.testmap.Exhibit;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.testdo.testmap.PaintingInfo;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

// TODO: redefine all test cases in terms of entities in "relationships" map
// and merge this test case with DeleteRulesTst that inherits
// from RelationshipTestCase.
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextDeleteRulesIT extends RuntimeCase {

	@Inject
	private DataContext context;

	@Inject
	private DBHelper dbHelper;

	@Override
	public void cleanUpDB() throws Exception {
		dbHelper.update("ARTGROUP").set("PARENT_GROUP_ID", null, Types.INTEGER).execute();
		super.cleanUpDB();
	}

	@Test
	public void testNullifyToOne() {
		// ArtGroup toParentGroup
		ArtGroup parentGroup = (ArtGroup) context.newObject("ArtGroup");
		parentGroup.setName("Parent");

		ArtGroup childGroup = (ArtGroup) context.newObject("ArtGroup");
		childGroup.setName("Child");
		parentGroup.addToChildGroupsArray(childGroup);

		// Check to make sure that the relationships are both exactly correct
		// before starting. We're not really testing this, but it is imperative
		// that it is correct before testing the real details.
		assertEquals(parentGroup, childGroup.getToParentGroup());
		assertTrue(parentGroup.getChildGroupsArray().contains(childGroup));

		// Always good to commit before deleting... bad things happen otherwise
		context.commitChanges();

		context.deleteObjects(childGroup);

		// The things we are testing.
		assertFalse(parentGroup.getChildGroupsArray().contains(childGroup));
		// Although deleted, the property should be null (good cleanup policy)
		// assertNull(childGroup.getToParentGroup());

		// And be sure that the commit works afterwards, just for sanity
		context.commitChanges();
	}

	/**
	 * Tests that deleting a source of a flattened relationship with CASCADE
	 * rule results in deleting a join and a target.
	 */
	@Test
	public void testCascadeToManyFlattened() {
		// testing Artist.groupArray relationship
		ArtGroup aGroup = context.newObject(ArtGroup.class);
		aGroup.setName("Group Name");
		Artist anArtist = context.newObject(Artist.class);
		anArtist.setArtistName("A Name");
		anArtist.addToGroupArray(aGroup);
		assertTrue(anArtist.getGroupArray().contains(aGroup));

		context.commitChanges();

		SQLTemplate checkQuery = new SQLTemplate(Artist.class, "SELECT * FROM ARTIST_GROUP");
		checkQuery.setFetchingDataRows(true);
		List<?> joins1 = context.performQuery(checkQuery);
		assertEquals(1, joins1.size());

		context.deleteObjects(anArtist);

		assertEquals(PersistenceState.DELETED, aGroup.getPersistenceState());
		assertFalse(anArtist.getGroupArray().contains(aGroup));
		context.commitChanges();

		List<?> joins2 = context.performQuery(checkQuery);
		assertEquals(0, joins2.size());
	}

	/**
	 * Tests that deleting a source of a flattened relationship with NULLIFY
	 * rule results in deleting a join together with the object deleted.
	 */
	@Test
	public void testNullifyToManyFlattened() {
		// testing ArtGroup.artistArray relationship
		ArtGroup aGroup = context.newObject(ArtGroup.class);
		aGroup.setName("Group Name");
		Artist anArtist = context.newObject(Artist.class);
		anArtist.setArtistName("A Name");
		aGroup.addToArtistArray(anArtist);

		context.commitChanges();

		// Preconditions
		assertTrue(aGroup.getArtistArray().contains(anArtist));
		assertTrue(anArtist.getGroupArray().contains(aGroup));

		SQLTemplate checkQuery = new SQLTemplate(Artist.class, "SELECT * FROM ARTIST_GROUP");
		checkQuery.setFetchingDataRows(true);
		List<?> joins1 = context.performQuery(checkQuery);
		assertEquals(1, joins1.size());

		context.deleteObjects(aGroup);
		assertFalse(anArtist.getGroupArray().contains(aGroup));
		context.commitChanges();

		List<?> joins2 = context.performQuery(checkQuery);
		assertEquals(0, joins2.size());
	}

	@Test
	public void testNullifyToMany() {
		// ArtGroup childGroupsArray
		ArtGroup parentGroup = (ArtGroup) context.newObject("ArtGroup");
		parentGroup.setName("Parent");

		ArtGroup childGroup = (ArtGroup) context.newObject("ArtGroup");
		childGroup.setName("Child");
		parentGroup.addToChildGroupsArray(childGroup);

		// Preconditions - good to check to be sure
		assertEquals(parentGroup, childGroup.getToParentGroup());
		assertTrue(parentGroup.getChildGroupsArray().contains(childGroup));

		context.commitChanges();

		context.deleteObjects(parentGroup);

		// The things we are testing.
		assertNull(childGroup.getToParentGroup());

		// Although deleted, the property should be null (good cleanup policy)
		// assertFalse(parentGroup.getChildGroupsArray().contains(childGroup));
		context.commitChanges();
	}

	@Test
	public void testCascadeToOne() {
		// Painting toPaintingInfo
		Painting painting = (Painting) context.newObject("Painting");
		painting.setPaintingTitle("A Title");

		PaintingInfo info = (PaintingInfo) context.newObject("PaintingInfo");
		painting.setToPaintingInfo(info);

		// Must commit before deleting.. this relationship is dependent,
		// and everything must be committed for certain things to work
		context.commitChanges();

		context.deleteObjects(painting);

		// info must also be deleted
		assertEquals(PersistenceState.DELETED, info.getPersistenceState());
		assertNull(info.getPainting());
		assertNull(painting.getToPaintingInfo());
		context.commitChanges();
	}

	@Test
	public void testCascadeToMany() {
		// Artist artistExhibitArray
		Artist anArtist = (Artist) context.newObject("Artist");
		anArtist.setArtistName("A Name");
		Exhibit anExhibit = (Exhibit) context.newObject("Exhibit");
		anExhibit.setClosingDate(new java.sql.Timestamp(System.currentTimeMillis()));
		anExhibit.setOpeningDate(new java.sql.Timestamp(System.currentTimeMillis()));

		// Needs a gallery... required for data integrity
		Gallery gallery = (Gallery) context.newObject("Gallery");
		gallery.setGalleryName("A Name");

		anExhibit.setToGallery(gallery);

		ArtistExhibit artistExhibit = (ArtistExhibit) context.newObject("ArtistExhibit");

		artistExhibit.setToArtist(anArtist);
		artistExhibit.setToExhibit(anExhibit);
		context.commitChanges();

		context.deleteObjects(anArtist);

		// Test that the link record was deleted, and removed from the
		// relationship
		assertEquals(PersistenceState.DELETED, artistExhibit.getPersistenceState());
		assertFalse(anArtist.getArtistExhibitArray().contains(artistExhibit));
		context.commitChanges();
	}

	@Test
	public void testDenyToMany() {
		// Gallery paintingArray
		Gallery gallery = (Gallery) context.newObject("Gallery");
		gallery.setGalleryName("A Name");
		Painting painting = (Painting) context.newObject("Painting");
		painting.setPaintingTitle("A Title");
		gallery.addToPaintingArray(painting);
		context.commitChanges();

		try {
			context.deleteObjects(gallery);
			fail("Should have thrown an exception");
		} catch (Exception e) {
			// GOOD!
		}
		context.commitChanges();
	}
}
