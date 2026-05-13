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
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.OracleUnitDbAdapter;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class ObjectSelect_RunIT {

	@RegisterExtension
	static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

	private UnitDbAdapter unitDbAdapter;
	private DataContext context;

	@BeforeEach
	public void createArtistsDataSet() throws Exception {
		unitDbAdapter = env.getInstance(UnitDbAdapter.class);
		context = env.dataContext();
		TableHelper tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");

		long dateBase = System.currentTimeMillis();
		for (int i = 1; i <= 20; i++) {
			tArtist.insert(i, "artist" + i, new java.sql.Date(dateBase + 10000 * i));
		}

		TableHelper tGallery = env.table("GALLERY", "GALLERY_ID", "GALLERY_NAME");
		tGallery.insert(1, "tate modern");

		TableHelper tPaintings = env.table("PAINTING", "PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "GALLERY_ID");
		for (int i = 1; i <= 20; i++) {
			tPaintings.insert(i, "painting" + i, i % 5 + 1, 1);
		}
	}

	@Test
	public void selectObjects() {
		List<Artist> result = ObjectSelect.query(Artist.class).select(context);
		assertEquals(20, result.size());
		assertInstanceOf(Artist.class, result.get(0));

		Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.in("artist14", "at1", "12", "asdas")).selectOne(context);
		assertNotNull(a);
		assertEquals("artist14", a.getArtistName());
	}

	@Test
	public void iterate() {
		final int[] count = new int[1];
		ObjectSelect.query(Artist.class).iterate(context, object -> {
			assertNotNull(object.getArtistName());
			count[0]++;
		});

		assertEquals(20, count[0]);
	}

	@Test
	public void iterator() {
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
	public void batchIterator() {
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
	public void selectDataRows() {
		List<DataRow> result = ObjectSelect.dataRowQuery(Artist.class).select(context);
		assertEquals(20, result.size());
		assertInstanceOf(DataRow.class, result.get(0));

		DataRow a = ObjectSelect.dataRowQuery(Artist.class).where(Artist.ARTIST_NAME.eq("artist14")).selectOne(context);
		assertNotNull(a);
		assertEquals("artist14", a.get("ARTIST_NAME"));
	}

	@Test
	public void selectOne() {
		Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("artist13")).selectOne(context);
		assertNotNull(a);
		assertEquals("artist13", a.getArtistName());
	}

	@Test
	public void selectOne_NoMatch() {
		Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("artist33")).selectOne(context);
		assertNull(a);
	}

	@Test
	public void selectOne_MoreThanOneMatch() {
		assertThrows(CayenneRuntimeException.class,
				() -> ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.like("artist%")).selectOne(context));
	}

	@Test
	public void selectFirst() {
		Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("artist13")).selectFirst(context);
		assertNotNull(a);
		assertEquals("artist13", a.getArtistName());
	}

	@Test
	public void selectFirstByContext() {
		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("artist13"));
		Artist a = context.selectFirst(q);
		assertNotNull(a);
		assertEquals("artist13", a.getArtistName());
	}

	@Test
	public void selectFirst_NoMatch() {
		Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("artist33")).selectFirst(context);
		assertNull(a);
	}

	@Test
	public void selectFirst_MoreThanOneMatch() {
		Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.like("artist%"))
				.orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc()).selectFirst(context);
		assertNotNull(a);
		assertEquals("artist1", a.getArtistName());
	}

	@Test
	public void selectFirst_TrimInWhere() {
		Artist a = ObjectSelect.query(Artist.class)
				.where(Artist.ARTIST_NAME.trim().likeIgnoreCase("artist%"))
				.orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc()).selectFirst(context);
		assertNotNull(a);
		assertEquals("artist1", a.getArtistName());
	}

	@Test
	public void selectFirst_SubstringInWhere() {
		Artist a = ObjectSelect.query(Artist.class)
				.where(Artist.ARTIST_NAME.substring(2, 3).eq("rti"))
				.orderBy(Artist.ARTIST_ID_PK_PROPERTY.asc()).selectFirst(context);
		assertNotNull(a);
		assertEquals("artist1", a.getArtistName());
	}

	@Test
	public void select_CustomFunction() {
		// TODO: This will fail for Oracle, so skip for now.
		//       It is necessary to provide connection with "fixedString=true" property somehow.
		//       Also see CAY-1470.
		assumeFalse(unitDbAdapter instanceof OracleUnitDbAdapter);
		Artist a = ObjectSelect.query(Artist.class)
				.where(Artist.ARTIST_NAME.function("UPPER", String.class).eq("ARTIST1"))
				.selectOne(context);
		assertNotNull(a);
		assertEquals("artist1", a.getArtistName());
	}

	@Test
	public void select_Having() {
		List<Artist> artists = ObjectSelect.query(Artist.class)
				.having(Artist.PAINTING_ARRAY.count().gt(3L))
				.select(context);

		assertEquals(5, artists.size());
	}

	@Test
	public void select_Where_Having() {
		List<Artist> artists = ObjectSelect.query(Artist.class)
				.where(Artist.ARTIST_NAME.eq("artist1"))
				.having(Artist.PAINTING_ARRAY.count().gt(3L))
				.select(context);

		assertEquals(1, artists.size());
	}

	@Test
	public void cay_2092() {
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
	public void cay_2836_countWithOrdering() {
		long count = ObjectSelect.query(Artist.class)
				.orderBy(Artist.ARTIST_NAME.asc())
				.selectCount(context);
		assertEquals(20, count);
	}
}
