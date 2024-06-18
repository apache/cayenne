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

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.Types;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextEJBQLJoinsIT extends RuntimeCase {

	@Inject
	private ObjectContext context;

	@Inject
	protected DBHelper dbHelper;

	protected TableHelper tArtist;
	protected TableHelper tPainting;
	protected TableHelper tGallery;

	@Before
	public void setUp() throws Exception {
		tArtist = new TableHelper(dbHelper, "ARTIST");
		tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

		tPainting = new TableHelper(dbHelper, "PAINTING");
		tPainting.setColumns("PAINTING_ID", "ARTIST_ID", "GALLERY_ID", "PAINTING_TITLE", "ESTIMATED_PRICE")
				.setColumnTypes(Types.INTEGER, Types.BIGINT, Types.INTEGER, Types.VARCHAR, Types.DECIMAL);

		tGallery = new TableHelper(dbHelper, "GALLERY");
		tGallery.setColumns("GALLERY_ID", "GALLERY_NAME");
	}

	private void createFourArtistsFourPaintings() throws Exception {
		tArtist.insert(33001, "AA1");
		tArtist.insert(33002, "AA2");
		tArtist.insert(33003, "BB1");
		tArtist.insert(33004, "BB2");
		tPainting.insert(33001, 33001, null, "P1", 3000);
		tPainting.insert(33002, 33002, null, "P2", 5000);
		tPainting.insert(33003, 33001, null, "AA1", 3000);
		tPainting.insert(33004, 33002, null, "BB2", 3000);
	}

	private void createTwoArtistsOnePainting() throws Exception {
		tArtist.insert(33001, "AA1");
		tArtist.insert(33005, "AA1");
		tPainting.insert(33001, 33001, null, "P1", 3000);
	}

	private void createTwoArtistsTwoPaintingsTwoGalleries() throws Exception {
		tArtist.insert(33001, "AA1");
		tArtist.insert(33002, "AA2");
		tGallery.insert(33001, "gallery1");
		tGallery.insert(33002, "gallery2");
		tPainting.insert(33005, 33001, 33001, "CC1", 5000);
		tPainting.insert(33006, 33002, 33002, "CC2", 5000);
	}

	private void createTwoArtistsThreePaintings() throws Exception {
		tArtist.insert(33001, "AA1");
		tArtist.insert(33002, "AA2");
		tPainting.insert(33001, 33001, null, "P1", 3000);
		tPainting.insert(33002, 33002, null, "P2", 5000);
		tPainting.insert(33007, 33001, null, "P2", 5000);
	}

	@Test
	public void testThetaJoins() throws Exception {
		createFourArtistsFourPaintings();

		String ejbql = "SELECT DISTINCT a FROM Artist a, Painting b " + "WHERE a.artistName = b.paintingTitle";

		EJBQLQuery query = new EJBQLQuery(ejbql);
		List<?> artists = context.performQuery(query);
		assertEquals(2, artists.size());

		Set<String> names = new HashSet<String>(2);
		Iterator<?> it = artists.iterator();
		while (it.hasNext()) {
			Artist a = (Artist) it.next();
			names.add(a.getArtistName());
		}

		assertTrue(names.contains("AA1"));
		assertTrue(names.contains("BB2"));
	}

	@Test
	public void testInnerJoins() throws Exception {
		createTwoArtistsOnePainting();

		String ejbql = "SELECT a FROM Artist a INNER JOIN a.paintingArray p " + "WHERE a.artistName = 'AA1'";

		List<?> artists = context.performQuery(new EJBQLQuery(ejbql));
		assertEquals(1, artists.size());
		assertEquals(33001, Cayenne.intPKForObject((Artist) artists.get(0)));
	}

	@Test
	public void testOuterJoins() throws Exception {
		createTwoArtistsOnePainting();

		String ejbql = "SELECT a FROM Artist a LEFT JOIN a.paintingArray p " + "WHERE a.artistName = 'AA1'";

		List<?> artists = context.performQuery(new EJBQLQuery(ejbql));
		assertEquals(2, artists.size());
		Set<Object> ids = new HashSet<>(2);
		Iterator<?> it = artists.iterator();
		while (it.hasNext()) {
			Artist a = (Artist) it.next();
			ids.add(Cayenne.pkForObject(a));
		}

		assertTrue(ids.contains(33001L));
		assertTrue(ids.contains(33005L));
	}

	@Test
	public void testChainedJoins() throws Exception {
		createTwoArtistsTwoPaintingsTwoGalleries();

		String ejbql = "SELECT a FROM Artist a JOIN a.paintingArray p JOIN p.toGallery g "
				+ "WHERE g.galleryName = 'gallery2'";

		EJBQLQuery query = new EJBQLQuery(ejbql);

		List<?> artists = context.performQuery(query);
		assertEquals(1, artists.size());
		assertEquals(33002, Cayenne.intPKForObject((Artist) artists.get(0)));
	}

	@Test
	public void testImplicitJoins() throws Exception {
		createTwoArtistsTwoPaintingsTwoGalleries();

		String ejbql = "SELECT a FROM Artist a WHERE a.paintingArray.toGallery.galleryName = 'gallery2'";

		EJBQLQuery query = new EJBQLQuery(ejbql);

		List<?> artists = context.performQuery(query);
		assertEquals(1, artists.size());
		assertEquals(33002, Cayenne.intPKForObject((Artist) artists.get(0)));
	}

	@Test
	public void testImplicitJoins_OUTER_LastComponent() throws Exception {

		tArtist.insert(33001, "AA1");
		tArtist.insert(33002, "AA2");
		tPainting.insert(33005, 33001, null, "CC1", 5000);
		tPainting.insert(33006, 33001, null, "CC2", 5000);

		String ejbql = "SELECT a FROM Artist a WHERE a.paintingArray+ is null";

		EJBQLQuery query = new EJBQLQuery(ejbql);

		List<?> artists = context.performQuery(query);
		assertEquals(1, artists.size());
		assertEquals(33002, Cayenne.intPKForObject((Artist) artists.get(0)));
	}

	@Test
	public void testImplicitJoins_OUTER_InTheMiddle() throws Exception {

		tGallery.insert(33001, "gallery1");
		tGallery.insert(33002, "gallery2");
		tArtist.insert(33001, "AA1");
		tArtist.insert(33002, "AA2");
		tPainting.insert(33005, 33001, 33001, "CC1", 5000);
		tPainting.insert(33006, 33001, 33002, "CC2", 5000);

		String ejbql = "SELECT a FROM Artist a WHERE a.paintingArray+.toGallery is null";

		EJBQLQuery query = new EJBQLQuery(ejbql);

		List<?> artists = context.performQuery(query);
		assertEquals(1, artists.size());
		assertEquals(33002, Cayenne.intPKForObject((Artist) artists.get(0)));
	}

	@Test
	public void testPartialImplicitJoins1() throws Exception {
		createTwoArtistsTwoPaintingsTwoGalleries();

		String ejbql = "SELECT a " + "FROM Artist a JOIN a.paintingArray b "
				+ "WHERE a.paintingArray.toGallery.galleryName = 'gallery2'";

		List<?> artists = context.performQuery(new EJBQLQuery(ejbql));
		assertEquals(1, artists.size());
		assertEquals(33002, Cayenne.intPKForObject((Artist) artists.get(0)));
	}

	@Test
	public void testPartialImplicitJoins2() throws Exception {
		createTwoArtistsTwoPaintingsTwoGalleries();

		String ejbql = "SELECT a " + "FROM Artist a JOIN a.paintingArray b "
				+ "WHERE a.paintingArray.paintingTitle = 'CC2'";

		List<?> artists = context.performQuery(new EJBQLQuery(ejbql));
		assertEquals(1, artists.size());
		assertEquals(33002, Cayenne.intPKForObject((Artist) artists.get(0)));
	}

	@Test
	public void testMultipleJoinsToTheSameTable() throws Exception {
		createTwoArtistsThreePaintings();

		String ejbql = "SELECT a " + "FROM Artist a JOIN a.paintingArray b JOIN a.paintingArray c "
				+ "WHERE b.paintingTitle = 'P1' AND c.paintingTitle = 'P2'";

		List<?> artists = context.performQuery(new EJBQLQuery(ejbql));
		assertEquals(1, artists.size());
		assertEquals(33001, Cayenne.intPKForObject((Artist) artists.get(0)));
	}
}
