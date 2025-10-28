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
package org.apache.cayenne.query;

import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ObjectSelect_RunIT extends ServerCase {

	@Inject
	private DataContext context;

	@Inject
	private DBHelper dbHelper;

	@Before
	public void createArtistsDataSet() throws Exception {
		TableHelper tArtist = new TableHelper(dbHelper, "ARTIST");
		tArtist.setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");

		long dateBase = System.currentTimeMillis();
		for (int i = 1; i <= 20; i++) {
			tArtist.insert(i, "artist" + i, new java.sql.Date(dateBase + 10000 * i));
		}

		TableHelper tGallery = new TableHelper(dbHelper, "GALLERY");
		tGallery.setColumns("GALLERY_ID", "GALLERY_NAME");
		tGallery.insert(1, "tate modern");

		TableHelper tPaintings = new TableHelper(dbHelper, "PAINTING");
		tPaintings.setColumns("PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "GALLERY_ID");
		for (int i = 1; i <= 20; i++) {
			tPaintings.insert(i, "painting" + i, i % 5 + 1, 1);
		}
	}

	@Test
	public void test_SelectObjects() {
		List<Artist> result = ObjectSelect.query(Artist.class).select(context);
		assertEquals(20, result.size());
		assertThat(result.get(0), instanceOf(Artist.class));

		Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.in("artist14", "at1", "12", "asdas")).selectOne(context);
		assertNotNull(a);
		assertEquals("artist14", a.getArtistName());
	}

	@Test
	public void test_Iterate() {
		final int[] count = new int[1];
		ObjectSelect.query(Artist.class).iterate(context, object -> {
			assertNotNull(object.getArtistName());
			count[0]++;
		});

		assertEquals(20, count[0]);
	}

	@Test
	public void test_Iterator() {
		try (ResultIterator<Artist> it = ObjectSelect.query(Artist.class).iterator(context)) {
			int count = 0;

			while(it.hasNextRow()) {
				it.nextRow();
				count++;
			}
			assertEquals(20, count);
		}
	}

	@Test
	public void test_BatchIterator() {
		try (ResultBatchIterator<Artist> it = ObjectSelect.query(Artist.class).batchIterator(context, 5)) {
			int count = 0;

			for (List<Artist> artistList : it) {
				count++;
				assertEquals(5, artistList.size());
			}

			assertEquals(4, count);
		}
	}

	@Test
	public void test_SelectDataRows() {
		List<DataRow> result = ObjectSelect.dataRowQuery(Artist.class).select(context);
		assertEquals(20, result.size());
		assertThat(result.get(0), instanceOf(DataRow.class));

		DataRow a = ObjectSelect.dataRowQuery(Artist.class).where(Artist.ARTIST_NAME.eq("artist14")).selectOne(context);
		assertNotNull(a);
		assertEquals("artist14", a.get("ARTIST_NAME"));
	}

	@Test
	public void test_SelectOne() {
		Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("artist13")).selectOne(context);
		assertNotNull(a);
		assertEquals("artist13", a.getArtistName());
	}

	@Test
	public void test_SelectOne_NoMatch() {
		Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("artist33")).selectOne(context);
		assertNull(a);
	}

	@Test(expected = CayenneRuntimeException.class)
	public void test_SelectOne_MoreThanOneMatch() {
		ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.like("artist%")).selectOne(context);
	}

	@Test
	public void test_SelectFirst() {
		Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("artist13")).selectFirst(context);
		assertNotNull(a);
		assertEquals("artist13", a.getArtistName());
	}

	@Test
	public void test_SelectFirstByContext() {
		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("artist13"));
		Artist a = context.selectFirst(q);
		assertNotNull(a);
		assertEquals("artist13", a.getArtistName());
	}

	@Test
	public void test_SelectFirst_NoMatch() {
		Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("artist33")).selectFirst(context);
		assertNull(a);
	}

	@Test
	public void test_SelectFirst_MoreThanOneMatch() {
		Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.like("artist%"))
				.orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc()).selectFirst(context);
		assertNotNull(a);
		assertEquals("artist1", a.getArtistName());
	}

	@Test
	public void test_SelectFirst_TrimInWhere() {
		Artist a = ObjectSelect.query(Artist.class)
				.where(Artist.ARTIST_NAME.trim().likeIgnoreCase("artist%"))
				.orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc()).selectFirst(context);
		assertNotNull(a);
		assertEquals("artist1", a.getArtistName());
	}

	@Test
	public void test_SelectFirst_SubstringInWhere() {
		Artist a = ObjectSelect.query(Artist.class)
				.where(Artist.ARTIST_NAME.substring(2, 3).eq("rti"))
				.orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc()).selectFirst(context);
		assertNotNull(a);
		assertEquals("artist1", a.getArtistName());
	}

	@Test
	public void test_Select_CustomFunction() {
		Artist a = ObjectSelect.query(Artist.class)
				.where(Artist.ARTIST_NAME.function("UPPER", String.class).eq("ARTIST1"))
				.selectOne(context);
		assertNotNull(a);
		assertEquals("artist1", a.getArtistName());
	}

	@Test
	public void test_Select_Having() {
		List<Artist> artists = ObjectSelect.query(Artist.class)
				.having(Artist.PAINTING_ARRAY.count().gt(3L))
				.select(context);

		assertEquals(5, artists.size());
	}

	@Test
	public void test_Select_Where_Having() {
		List<Artist> artists = ObjectSelect.query(Artist.class)
				.where(Artist.ARTIST_NAME.eq("artist1"))
				.having(Artist.PAINTING_ARRAY.count().gt(3L))
				.select(context);

		assertEquals(1, artists.size());
	}

	@Test
	public void test_CAY_2092() {
		List<Artist> artists = ObjectSelect.query(Artist.class)
				.orderBy(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).asc())
				.pageSize(5)
				.select(context);
		// just read everything to trigger page resolving
		for (Artist artist : artists) {
			assertNotNull(artist);
		}
	}

	@Test
	public void test_CAY_2836_countWithOrdering() {
		long count = ObjectSelect.query(Artist.class)
				.orderBy(Artist.ARTIST_NAME.asc())
				.selectCount(context);
		assertEquals(20, count);
	}
}
