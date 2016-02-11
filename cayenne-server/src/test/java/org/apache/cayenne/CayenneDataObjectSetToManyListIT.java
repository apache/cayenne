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

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.testdo.testmap.ROPainting;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class CayenneDataObjectSetToManyListIT extends ServerCase {

	@Inject
	private ServerRuntime runtime;

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

	}

	private void createArtistWithPaintingDataSet() throws Exception {
		tArtist.insert(8, "artist 8");
		tPainting.insert(6, "painting 6", 8);
		tPainting.insert(7, "painting 7", 8);
		tPainting.insert(8, "painting 8", 8);
	}

	@Test
	public void testReadRO1() throws Exception {

		createArtistWithPaintingDataSet();

		Artist a1 = Cayenne.objectForPK(context, Artist.class, 8);

		assertEquals(a1 != null, true);

		List<ROPainting> paints = ObjectSelect.query(ROPainting.class).where(ROPainting.TO_ARTIST.eq(a1))
				.select(context);

		assertEquals(3, paints.size());

		ROPainting rop1 = paints.get(0);
		assertSame(a1, rop1.getToArtist());
	}

	@Test
	public void testSetEmptyList1() throws Exception {
		createArtistWithPaintingDataSet();
		Artist artist = Cayenne.objectForPK(context, Artist.class, 8);
		artist.setToManyTarget(Artist.PAINTING_ARRAY.getName(), new ArrayList<ROPainting>(0), true);
		List<Painting> paints = artist.getPaintingArray();
		assertEquals(0, paints.size());
	}

	@Test
	public void testSetEmptyList2() throws Exception {
		createArtistWithPaintingDataSet();
		Artist artist = Cayenne.objectForPK(context, Artist.class, 8);
		boolean thrown = false;
		try {
			artist.setToManyTarget(Artist.PAINTING_ARRAY.getName(), null, true);
		} catch (IllegalArgumentException e) {
			thrown = true;
		}
		assertEquals("should throw a IllegalArgumentException", thrown, true);
	}

	@Test
	public void testWrongRelName() throws Exception {
		createArtistWithPaintingDataSet();

		Artist artist = Cayenne.objectForPK(context, Artist.class, 8);
		boolean thrown = false;
		try {
			artist.setToManyTarget("doesnotexist", new ArrayList<ROPainting>(0), true);
		} catch (IllegalArgumentException e) {
			thrown = true;
		}
		assertEquals("should throw a IllegalArgumentException, because the relName does not exist", thrown, true);

		thrown = false;
		try {
			artist.setToManyTarget("", new ArrayList<ROPainting>(0), true);
		} catch (IllegalArgumentException e) {
			thrown = true;
		}
		assertEquals("should throw a IllegalArgumentException, because the relName is an empty string", thrown, true);

		thrown = false;
		try {
			artist.setToManyTarget(null, new ArrayList<ROPainting>(0), true);
		} catch (IllegalArgumentException e) {
			thrown = true;
		}
		assertEquals("should throw a IllegalArgumentException, because the relName is NULL", thrown, true);

	}

	@Test
	public void testTotalDifferentPaintings() throws Exception {
		createArtistWithPaintingDataSet();

		Artist artist = Cayenne.objectForPK(context, Artist.class, 8);

		// copy the paintings list. Replacing paintings wont change the copy
		List<Painting> oldPaints = new ArrayList<Painting>(artist.getPaintingArray());
		System.out.println("class:" + oldPaints.getClass());

		Painting paintX = new Painting();
		paintX.setPaintingTitle("pantingX");
		Painting paintY = new Painting();
		paintY.setPaintingTitle("paintingY");
		Painting paintZ = new Painting();
		paintZ.setPaintingTitle("paintingZ");

		List<? extends DataObject> returnList = artist.setToManyTarget(Artist.PAINTING_ARRAY.getName(),
				Arrays.asList(paintX, paintY, paintZ), true);

		assertEquals(returnList.size(), 3);
		assertEquals(returnList.containsAll(oldPaints), true);

		List<Painting> newPaints = artist.getPaintingArray();

		assertEquals(newPaints.size(), 3);
		for (Painting oldPaint : oldPaints) {
			// no element of oldPaints should exist in the newPaints
			assertEquals(newPaints.contains(oldPaint), false);
		}
	}

	@Test
	public void testSamePaintings() throws Exception {
		createArtistWithPaintingDataSet();

		Artist artist = Cayenne.objectForPK(context, Artist.class, 8);
		List<Painting> oldPaints = new ArrayList<Painting>(artist.getPaintingArray());

		Painting paint6 = Cayenne.objectForPK(context, Painting.class, 6);
		Painting paint7 = Cayenne.objectForPK(context, Painting.class, 7);
		Painting paint8 = Cayenne.objectForPK(context, Painting.class, 8);

		List<Painting> newPaints = Arrays.asList(paint6, paint7, paint8);
		List<? extends DataObject> returnList = artist.setToManyTarget(Artist.PAINTING_ARRAY.getName(), newPaints,
				true);

		assertEquals(returnList.size(), 0);

		newPaints = artist.getPaintingArray();
		// testing if oldPaints and newPaints contain the same objects
		assertEquals(newPaints.size(), 3);
		assertEquals(oldPaints.size(), 3);
		assertEquals(newPaints.containsAll(oldPaints), true);
	}

	@Test
	public void testOldPlusNewPaintings() throws Exception {
		createArtistWithPaintingDataSet();

		Artist artist = Cayenne.objectForPK(context, Artist.class, 8);
		List<Painting> oldPaints = artist.getPaintingArray();

		List<Painting> newPaints = new ArrayList<Painting>(6);
		for (int i = 0; i < oldPaints.size(); i++) {
			newPaints.add(oldPaints.get(i));
		}

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

		assertEquals(newPaints2.size(), 6);
		assertEquals(newPaints2.contains(paintX), true);
		assertEquals(newPaints2.contains(paintY), true);
		assertEquals(newPaints2.contains(paintZ), true);
		assertEquals(newPaints2.contains(paint6), true);
		assertEquals(newPaints2.contains(paint7), true);
		assertEquals(newPaints2.contains(paint8), true);
	}

	@Test
	public void testRemoveOneOldAndAddOneNewPaintings() throws Exception {
		createArtistWithPaintingDataSet();

		Artist artist = Cayenne.objectForPK(context, Artist.class, 8);

		List<Painting> newPaints = new ArrayList<Painting>();

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

		List<? extends DataObject> returnList = artist.setToManyTarget(Artist.PAINTING_ARRAY.getName(), newPaints,
				true);

		assertEquals(returnList.size(), 1);
		assertEquals(returnList.get(0) == paint8, true);

		List<Painting> newPaints2 = artist.getPaintingArray();

		assertEquals(newPaints2.size(), 4);
		assertEquals(newPaints2.contains(paintX), true);
		assertEquals(newPaints2.contains(paintY), true);
		assertEquals(newPaints2.contains(paint6), true);
		assertEquals(newPaints2.contains(paint7), true);
	}

	/**
	 * Testing if collection type is list, everything should work fine without an
	 * runtimexception
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRelationCollectionTypeList() throws Exception {
		createArtistWithPaintingDataSet();

		Artist artist = Cayenne.objectForPK(context, Artist.class, 8);
		assertTrue(artist.readProperty(Artist.PAINTING_ARRAY.getName()) instanceof List);
		boolean catchedSomething = false;
		try {
			artist.setToManyTarget(Artist.PAINTING_ARRAY.getName(), new ArrayList<Painting>(0), true);
		} catch (UnsupportedOperationException e) {
			catchedSomething = true;
		}
		assertEquals(catchedSomething, false);
		assertEquals(artist.getPaintingArray().size(), 0);
	}
}