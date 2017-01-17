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
package org.apache.cayenne.query;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.ResultIteratorCallback;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.FunctionExpressionFactory;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.exp.parser.ASTScalar;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

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
	public void test_SelectObjects() throws Exception {
		List<Artist> result = ObjectSelect.query(Artist.class).select(context);
		assertEquals(20, result.size());
		assertThat(result.get(0), instanceOf(Artist.class));

		Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("artist14")).selectOne(context);
		assertNotNull(a);
		assertEquals("artist14", a.getArtistName());
	}

	@Test
	public void test_Iterate() throws Exception {
		final int[] count = new int[1];
		ObjectSelect.query(Artist.class).iterate(context, new ResultIteratorCallback<Artist>() {

			@Override
			public void next(Artist object) {
				assertNotNull(object.getArtistName());
				count[0]++;
			}
		});

		assertEquals(20, count[0]);
	}

	@Test
	public void test_Iterator() throws Exception {
		try (ResultIterator<Artist> it = ObjectSelect.query(Artist.class).iterator(context)) {
			int count = 0;

			for (Artist a : it) {
				count++;
			}

			assertEquals(20, count);
		}
	}

	@Test
	public void test_BatchIterator() throws Exception {
		try (ResultBatchIterator<Artist> it = ObjectSelect.query(Artist.class).batchIterator(context, 5);) {
			int count = 0;

			for (List<Artist> artistList : it) {
				count++;
				assertEquals(5, artistList.size());
			}

			assertEquals(4, count);
		}
	}

	@Test
	public void test_SelectDataRows() throws Exception {
		List<DataRow> result = ObjectSelect.dataRowQuery(Artist.class).select(context);
		assertEquals(20, result.size());
		assertThat(result.get(0), instanceOf(DataRow.class));

		DataRow a = ObjectSelect.dataRowQuery(Artist.class).where(Artist.ARTIST_NAME.eq("artist14")).selectOne(context);
		assertNotNull(a);
		assertEquals("artist14", a.get("ARTIST_NAME"));
	}

	@Test
	public void test_SelectOne() throws Exception {
		Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("artist13")).selectOne(context);
		assertNotNull(a);
		assertEquals("artist13", a.getArtistName());
	}

	@Test
	public void test_SelectOne_NoMatch() throws Exception {
		Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("artist33")).selectOne(context);
		assertNull(a);
	}

	@Test(expected = CayenneRuntimeException.class)
	public void test_SelectOne_MoreThanOneMatch() throws Exception {
		ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.like("artist%")).selectOne(context);
	}

	@Test
	public void test_SelectFirst() throws Exception {
		Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("artist13")).selectFirst(context);
		assertNotNull(a);
		assertEquals("artist13", a.getArtistName());
	}

	@Test
	public void test_SelectFirstByContext() throws Exception {
		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("artist13"));
		Artist a = context.selectFirst(q);
		assertNotNull(a);
		assertEquals("artist13", a.getArtistName());
	}

	@Test
	public void test_SelectFirst_NoMatch() throws Exception {
		Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("artist33")).selectFirst(context);
		assertNull(a);
	}

	@Test
	public void test_SelectFirst_MoreThanOneMatch() throws Exception {
		Artist a = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.like("artist%"))
				.orderBy("db:ARTIST_ID").selectFirst(context);
		assertNotNull(a);
		assertEquals("artist1", a.getArtistName());
	}

	@Test
	public void test_SelectFirst_TrimInWhere() throws Exception {
		Expression exp = FunctionExpressionFactory.trimExp(Artist.ARTIST_NAME.path());
		Property<String> trimmedName = Property.create("trimmed", exp, String.class);
		Artist a = ObjectSelect.query(Artist.class).where(trimmedName.likeIgnoreCase("artist%"))
				.orderBy("db:ARTIST_ID").selectFirst(context);
		assertNotNull(a);
		assertEquals("artist1", a.getArtistName());
	}

	@Test
	public void test_SelectFirst_SubstringInWhere() throws Exception {
		Expression exp = FunctionExpressionFactory.substringExp(Artist.ARTIST_NAME.path(), 2, 3);
		Property<String> substrName = Property.create("substr", exp, String.class);
		Artist a = ObjectSelect.query(Artist.class).where(substrName.eq("rti"))
				.orderBy("db:ARTIST_ID").selectFirst(context);
		assertNotNull(a);
		assertEquals("artist1", a.getArtistName());
	}

	@Test
	public void test_SelectFirst_MultiColumns() throws Exception {
		Object[] a = ObjectSelect.query(Artist.class)
				.columns(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH)
				.columns(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH)
				.columns(Artist.ARTIST_NAME.alias("newName"))
				.where(Artist.ARTIST_NAME.like("artist%"))
				.orderBy("db:ARTIST_ID")
				.selectFirst(context);
		assertNotNull(a);
		assertEquals("artist1", a[0]);
		assertEquals("artist1", a[4]);
	}

	@Test
	public void test_SelectFirst_SingleValueInColumns() throws Exception {
		Object[] a = ObjectSelect.query(Artist.class)
				.columns(Artist.ARTIST_NAME)
				.where(Artist.ARTIST_NAME.like("artist%"))
				.orderBy("db:ARTIST_ID")
				.selectFirst(context);
		assertNotNull(a);
		assertEquals("artist1", a[0]);
	}

	@Test
	public void test_SelectFirst_SubstringName() throws Exception {
		Expression exp = FunctionExpressionFactory.substringExp(Artist.ARTIST_NAME.path(), 5, 3);
		Property<String> substrName = Property.create("substrName", exp, String.class);
		Object[] a = ObjectSelect.query(Artist.class)
				.columns(Artist.ARTIST_NAME, substrName)
				.where(substrName.eq("st3"))
				.selectFirst(context);

		assertNotNull(a);
		assertEquals("artist3", a[0]);
		assertEquals("st3", a[1]);
	}

	@Test
	public void test_SelectFirst_RelColumns() throws Exception {
		// set shorter than painting_array.paintingTitle alias as some DBs doesn't support dot in alias
		Property<String> paintingTitle = Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).alias("paintingTitle");

		Object[] a = ObjectSelect.query(Artist.class)
				.columns(Artist.ARTIST_NAME, paintingTitle)
				.orderBy(paintingTitle.asc())
				.selectFirst(context);
		assertNotNull(a);
		assertEquals("painting1", a[1]);
	}

	@Test
	public void test_SelectFirst_RelColumn() throws Exception {
		// set shorter than painting_array.paintingTitle alias as some DBs doesn't support dot in alias
		Property<String> paintingTitle = Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).alias("paintingTitle");

		String a = ObjectSelect.query(Artist.class)
				.column(paintingTitle)
				.orderBy(paintingTitle.asc())
				.selectFirst(context);
		assertNotNull(a);
		assertEquals("painting1", a);
	}

	@Test
	public void test_SelectFirst_RelColumnWithFunction() throws Exception {
		Property<String> paintingTitle = Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE);
		Expression exp = FunctionExpressionFactory.substringExp(paintingTitle.path(), 7, 3);
		exp = FunctionExpressionFactory.concatExp(exp, new ASTScalar(" "), Artist.ARTIST_NAME.path());
		Property<String> altTitle = Property.create("altTitle", exp, String.class);

		String a = ObjectSelect.query(Artist.class)
				.column(altTitle)
				.where(altTitle.like("ng1%"))
				.and(Artist.ARTIST_NAME.like("%ist1"))
//				.orderBy(altTitle.asc()) // unsupported for now
				.selectFirst(context);
		assertNotNull(a);
		assertEquals("ng1 artist1", a);
	}
}
