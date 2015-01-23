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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class SQLSelectIT extends ServerCase {

	@Inject
	private DataContext context;

	@Inject
	private DBHelper dbHelper;

	private TableHelper tArtist;

	@Before
	public void before() {
		tArtist = new TableHelper(dbHelper, "ARTIST").setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");
	}

	protected void createArtistsDataSet() throws Exception {

		long dateBase = System.currentTimeMillis();

		for (int i = 1; i <= 20; i++) {
			tArtist.insert(i, "artist" + i, new java.sql.Date(dateBase + 10000 * i));
		}
	}

	@Test
	public void test_DataRows_DataMapNameRoot() throws Exception {

		createArtistsDataSet();

		SQLSelect<DataRow> q1 = SQLSelect.dataRowQuery("testmap", "SELECT * FROM ARTIST");
		assertTrue(q1.isFetchingDataRows());

		List<DataRow> result = context.select(q1);
		assertEquals(20, result.size());
		assertTrue(result.get(0) instanceof DataRow);
	}

	@Test
	public void test_DataRows_DefaultRoot() throws Exception {

		createArtistsDataSet();

		SQLSelect<DataRow> q1 = SQLSelect.dataRowQuery("SELECT * FROM ARTIST");
		assertTrue(q1.isFetchingDataRows());

		List<DataRow> result = context.select(q1);
		assertEquals(20, result.size());
		assertTrue(result.get(0) instanceof DataRow);
	}

	@Test
	public void test_DataRows_ClassRoot() throws Exception {

		createArtistsDataSet();

		SQLSelect<Artist> q1 = SQLSelect.query(Artist.class, "SELECT * FROM ARTIST");
		assertFalse(q1.isFetchingDataRows());
		List<Artist> result = context.select(q1);
		assertEquals(20, result.size());
		assertTrue(result.get(0) instanceof Artist);
	}

	@Test
	public void test_DataRows_ClassRoot_Parameters() throws Exception {

		createArtistsDataSet();

		SQLSelect<Artist> q1 = SQLSelect.query(Artist.class, "SELECT * FROM ARTIST WHERE ARTIST_NAME = #bind($a)");
		q1.params("a", "artist3");

		assertFalse(q1.isFetchingDataRows());
		Artist a = context.selectOne(q1);
		assertEquals("artist3", a.getArtistName());
	}

	@Test
	public void test_DataRows_ClassRoot_Bind() throws Exception {

		createArtistsDataSet();

		SQLSelect<Artist> q1 = SQLSelect.query(Artist.class,
				"SELECT * FROM ARTIST WHERE ARTIST_NAME = #bind($a) OR ARTIST_NAME = #bind($b)");
		q1.params("a", "artist3").params("b", "artist4");

		List<Artist> result = context.select(q1);
		assertEquals(2, result.size());
	}

	@Test
	public void test_DataRows_ColumnNameCaps() throws Exception {

		SQLSelect<DataRow> q1 = SQLSelect.dataRowQuery("SELECT * FROM ARTIST WHERE ARTIST_NAME = 'artist2'");
		q1.upperColumnNames();

		SQLTemplate r1 = (SQLTemplate) q1.getReplacementQuery(context.getEntityResolver());
		assertEquals(CapsStrategy.UPPER, r1.getColumnNamesCapitalization());

		q1.lowerColumnNames();
		SQLTemplate r2 = (SQLTemplate) q1.getReplacementQuery(context.getEntityResolver());
		assertEquals(CapsStrategy.LOWER, r2.getColumnNamesCapitalization());
	}

	@Test
	public void test_DataRows_FetchLimit() throws Exception {

		createArtistsDataSet();

		SQLSelect<DataRow> q1 = SQLSelect.dataRowQuery("SELECT * FROM ARTIST");
		q1.limit(5);

		assertEquals(5, context.select(q1).size());
	}

	@Test
	public void test_DataRows_FetchOffset() throws Exception {

		createArtistsDataSet();

		SQLSelect<DataRow> q1 = SQLSelect.dataRowQuery("SELECT * FROM ARTIST");
		q1.offset(4);

		assertEquals(16, context.select(q1).size());
	}

	@Test
	public void test_Append() throws Exception {

		createArtistsDataSet();

		SQLSelect<Artist> q1 = SQLSelect.query(Artist.class, "SELECT * FROM ARTIST")
				.append(" WHERE ARTIST_NAME = #bind($a)").params("a", "artist3");

		List<Artist> result = context.select(q1);
		assertEquals(1, result.size());
	}

	@Test
	public void test_Select() throws Exception {

		createArtistsDataSet();

		List<Artist> result = SQLSelect.query(Artist.class, "SELECT * FROM ARTIST WHERE ARTIST_NAME = #bind($a)")
				.params("a", "artist3").select(context);

		assertEquals(1, result.size());
	}

	@Test
	public void test_SelectOne() throws Exception {

		createArtistsDataSet();

		Artist a = SQLSelect.query(Artist.class, "SELECT * FROM ARTIST WHERE ARTIST_NAME = #bind($a)")
				.params("a", "artist3").selectOne(context);

		assertEquals("artist3", a.getArtistName());
	}

	@Test
	public void test_SelectLong() throws Exception {

		createArtistsDataSet();

		long id = SQLSelect.scalarQuery(Long.class, "SELECT ARTIST_ID FROM ARTIST WHERE ARTIST_NAME = #bind($a)")
				.params("a", "artist3").selectOne(context);

		assertEquals(3l, id);
	}

	@Test
	public void test_SelectLongArray() throws Exception {

		createArtistsDataSet();

		List<Long> ids = SQLSelect.scalarQuery(Long.class, "SELECT ARTIST_ID FROM ARTIST ORDER BY ARTIST_ID").select(
				context);

		assertEquals(20, ids.size());
		assertEquals(2l, ids.get(1).longValue());
	}

	@Test
	public void test_SelectCount() throws Exception {

		createArtistsDataSet();

		int c = SQLSelect.scalarQuery(Integer.class, "SELECT #result('COUNT(*)' 'int') FROM ARTIST").selectOne(context);

		assertEquals(20, c);
	}

	@Test
	public void test_ParamsArray_Single() throws Exception {

		createArtistsDataSet();

		Long id = SQLSelect.scalarQuery(Long.class, "SELECT ARTIST_ID FROM ARTIST WHERE ARTIST_NAME = #bind($a)")
				.paramsArray("artist3").selectOne(context);

		assertEquals(3l, id.longValue());
	}

	@Test
	public void test_ParamsArray_Multiple() throws Exception {

		createArtistsDataSet();

		List<Long> ids = SQLSelect
				.scalarQuery(Long.class,
						"SELECT ARTIST_ID FROM ARTIST WHERE ARTIST_NAME = #bind($a) OR ARTIST_NAME = #bind($b) ORDER BY ARTIST_ID")
				.paramsArray("artist3", "artist2").select(context);

		assertEquals(2l, ids.get(0).longValue());
		assertEquals(3l, ids.get(1).longValue());
	}

	@Test
	public void test_ParamsArray_Multiple_OptionalChunks() throws Exception {

		Date dob = new java.sql.Date(System.currentTimeMillis());

		tArtist.insert(1, "artist1", dob);
		tArtist.insert(2, "artist2", null);

		List<Long> ids = SQLSelect
				.scalarQuery(
						Long.class,
						"SELECT ARTIST_ID FROM ARTIST #chain('OR' 'WHERE') "
								+ "#chunk($a) DATE_OF_BIRTH #bindEqual($a) #end "
								+ "#chunk($b) ARTIST_NAME #bindEqual($b) #end #end ORDER BY ARTIST_ID")
				.paramsArray(null, "artist1").select(context);

		assertEquals(1, ids.size());
		assertEquals(1l, ids.get(0).longValue());
	}

	@Test
	public void test_Params_Multiple_OptionalChunks() throws Exception {

		Date dob = new java.sql.Date(System.currentTimeMillis());

		tArtist.insert(1, "artist1", dob);
		tArtist.insert(2, "artist2", null);

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("a", null);
		params.put("b", "artist1");

		List<Long> ids = SQLSelect
				.scalarQuery(
						Long.class,
						"SELECT ARTIST_ID FROM ARTIST #chain('OR' 'WHERE') "
								+ "#chunk($a) DATE_OF_BIRTH #bindEqual($a) #end "
								+ "#chunk($b) ARTIST_NAME #bindEqual($b) #end #end ORDER BY ARTIST_ID").params(params)
				.select(context);

		assertEquals(1, ids.size());
		assertEquals(1l, ids.get(0).longValue());
	}
}
