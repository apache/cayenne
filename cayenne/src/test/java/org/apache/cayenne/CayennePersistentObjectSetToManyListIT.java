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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.testdo.testmap.ROPainting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class CayennePersistentObjectSetToManyListIT extends RuntimeCase {

	@Inject
	private CayenneRuntime runtime;

	@Inject
	private ObjectContext context;

	@Inject
	private DBHelper dbHelper;

	protected TableHelper tArtist;
	protected TableHelper tPainting;

	@Before
	public void setUp() throws Exception {
		tArtist = new TableHelper(dbHelper, "ARTIST");
		tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

		tPainting = new TableHelper(dbHelper, "PAINTING");
		tPainting.setColumns("PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID").setColumnTypes(Types.INTEGER, Types.VARCHAR,
				Types.BIGINT);

		createArtistWithPaintingDataSet();
	}

	private void createArtistWithPaintingDataSet() throws Exception {
		tArtist.insert(8, "artist 8");
		tPainting.insert(6, "painting 6", 8);
		tPainting.insert(7, "painting 7", 8);
		tPainting.insert(8, "painting 8", 8);
	}

	@Test
	public void testReadRO1() {
		Artist a1 = Cayenne.objectForPK(context, Artist.class, 8);
		assertNotNull(a1);

		List<ROPainting> paints = ObjectSelect.query(ROPainting.class).where(ROPainting.TO_ARTIST.eq(a1))
				.select(context);

		assertEquals(3, paints.size());

		ROPainting rop1 = paints.get(0);
		assertSame(a1, rop1.getToArtist());
	}

	@Test
	public void testSetEmptyList1() {
		Artist artist = Cayenne.objectForPK(context, Artist.class, 8);
		artist.setToManyTarget(Artist.PAINTING_ARRAY.getName(), new ArrayList<ROPainting>(0), true);
		List<Painting> paints = artist.getPaintingArray();
		assertEquals(0, paints.size());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetEmptyList2() {
		Artist artist = Cayenne.objectForPK(context, Artist.class, 8);
		artist.setToManyTarget(Artist.PAINTING_ARRAY.getName(), null, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNonExistentRelName() {
		Artist artist = Cayenne.objectForPK(context, Artist.class, 8);
		artist.setToManyTarget("doesnotexist", new ArrayList<ROPainting>(0), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyRelName() {
		Artist artist = Cayenne.objectForPK(context, Artist.class, 8);
		artist.setToManyTarget("", new ArrayList<ROPainting>(0), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullRelName() {
		Artist artist = Cayenne.objectForPK(context, Artist.class, 8);
		artist.setToManyTarget(null, new ArrayList<ROPainting>(0), true);
	}

	@Test
	public void testTotalDifferentPaintings() {
		Artist artist = Cayenne.objectForPK(context, Artist.class, 8);

		// copy the paintings list. Replacing paintings wont change the copy
		List<Painting> oldPaints = new ArrayList<>(artist.getPaintingArray());

		Painting paintX = new Painting();
		paintX.setPaintingTitle("pantingX");
		Painting paintY = new Painting();
		paintY.setPaintingTitle("paintingY");
		Painting paintZ = new Painting();
		paintZ.setPaintingTitle("paintingZ");

		List<? extends Persistent> returnList = artist.setToManyTarget(Artist.PAINTING_ARRAY.getName(),
				Arrays.asList(paintX, paintY, paintZ), true);

		assertEquals(3, returnList.size());
		assertTrue(returnList.containsAll(oldPaints));

		List<Painting> newPaints = artist.getPaintingArray();

		assertEquals(3, newPaints.size());
		for (Painting oldPaint : oldPaints) {
			// no element of oldPaints should exist in the newPaints
			assertFalse(newPaints.contains(oldPaint));
		}
	}

	@Test
	public void testSamePaintings() {
		Artist artist = Cayenne.objectForPK(context, Artist.class, 8);
		List<Painting> oldPaints = new ArrayList<>(artist.getPaintingArray());

		Painting paint6 = Cayenne.objectForPK(context, Painting.class, 6);
		Painting paint7 = Cayenne.objectForPK(context, Painting.class, 7);
		Painting paint8 = Cayenne.objectForPK(context, Painting.class, 8);

		List<Painting> newPaints = Arrays.asList(paint6, paint7, paint8);
		List<? extends Persistent> returnList = artist.setToManyTarget(Artist.PAINTING_ARRAY.getName(), newPaints,
				true);

		assertEquals(0, returnList.size());

		newPaints = artist.getPaintingArray();
		// testing if oldPaints and newPaints contain the same objects
		assertEquals(3, newPaints.size());
		assertEquals(3, oldPaints.size());
		assertTrue(newPaints.containsAll(oldPaints));
	}

	@Test
	public void testOldPlusNewPaintings() {
		Artist artist = Cayenne.objectForPK(context, Artist.class, 8);
		List<Painting> oldPaints = artist.getPaintingArray();

		List<Painting> newPaints = new ArrayList<>(6);
		newPaints.addAll(oldPaints);

		Painting paintX = new Painting();
		paintX.setPaintingTitle("pantingX");
		Painting paintY = new Painting();
		paintY.setPaintingTitle("paintingY");
		Painting paintZ = new Painting();
		paintZ.setPaintingTitle("paintingZ");

		newPaints.add(paintX);
		newPaints.add(paintY);
		newPaints.add(paintZ);

		artist.setToManyTarget(Artist.PAINTING_ARRAY.getName(), newPaints, true);

		List<Painting> newPaints2 = artist.getPaintingArray();
		Painting paint6 = Cayenne.objectForPK(context, Painting.class, 6);
		Painting paint7 = Cayenne.objectForPK(context, Painting.class, 7);
		Painting paint8 = Cayenne.objectForPK(context, Painting.class, 8);

		assertEquals(6, newPaints2.size());
		assertTrue(newPaints2.contains(paintX));
		assertTrue(newPaints2.contains(paintY));
		assertTrue(newPaints2.contains(paintZ));
		assertTrue(newPaints2.contains(paint6));
		assertTrue(newPaints2.contains(paint7));
		assertTrue(newPaints2.contains(paint8));
	}

	@Test
	public void testRemoveOneOldAndAddOneNewPaintings() {
		Artist artist = Cayenne.objectForPK(context, Artist.class, 8);

		List<Painting> newPaints = new ArrayList<>();

		Painting paint6 = artist.getPaintingArray().get(0);
		Painting paint7 = artist.getPaintingArray().get(1);
		Painting paint8 = artist.getPaintingArray().get(2);
		Painting paintX = new Painting();
		paintX.setPaintingTitle("pantingX");
		Painting paintY = new Painting();
		paintY.setPaintingTitle("paintingY");

		newPaints.add(paint6);
		newPaints.add(paint7);
		newPaints.add(paintX);
		newPaints.add(paintY);

		List<? extends Persistent> returnList = artist
				.setToManyTarget(Artist.PAINTING_ARRAY.getName(), newPaints, true);

		assertEquals(1, returnList.size());
		assertSame(paint8, returnList.get(0));

		List<Painting> newPaints2 = artist.getPaintingArray();

		assertEquals(4, newPaints2.size());
		assertTrue(newPaints2.contains(paintX));
		assertTrue(newPaints2.contains(paintY));
		assertTrue(newPaints2.contains(paint6));
		assertTrue(newPaints2.contains(paint7));
	}

	/**
	 * Testing if collection type is list, everything should work fine without a RuntimeException
	 */
	@Test
	public void testRelationCollectionTypeList() {
		Artist artist = Cayenne.objectForPK(context, Artist.class, 8);
		assertTrue(artist.readProperty(Artist.PAINTING_ARRAY.getName()) instanceof List);
		try {
			artist.setToManyTarget(Artist.PAINTING_ARRAY.getName(), new ArrayList<Painting>(0), true);
		} catch (UnsupportedOperationException e) {
			fail();
		}
		assertEquals(0, artist.getPaintingArray().size());
	}
}