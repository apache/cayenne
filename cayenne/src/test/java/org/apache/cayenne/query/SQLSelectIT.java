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

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.*;

public class SQLSelectIT {

	@RegisterExtension
	static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

	private DataContext context;

	private TableHelper tPainting;
	private TableHelper tArtistCt;
	private TableHelper tPaintingInfo;

	@BeforeEach
	public void before() {
		context = env.context();
		tPainting = env.table("PAINTING")
				.setColumns("PAINTING_ID", "PAINTING_TITLE", "ESTIMATED_PRICE")
				.setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.DECIMAL);

		tArtistCt = env.table("ARTIST_CT")
				.setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");

		tPaintingInfo = env.table("PAINTING_INFO")
				.setColumns("PAINTING_ID", "IMAGE_BLOB")
				.setColumnTypes(Types.INTEGER, Types.LONGVARBINARY);
	}

	private void createPaintingsDataSet() throws Exception {
		for (int i = 1; i <= 20; i++) {
			tPainting.insert(i, "painting" + i, 10000. * i);
		}
	}

	private void createArtistDataSet() throws SQLException {
		tArtistCt.insert(1, "Test", new Date(System.currentTimeMillis()));
		tArtistCt.insert(2, "Test1", new Date(System.currentTimeMillis()));
	}

	@Test
	public void dataRows_DataMapNameRoot() throws Exception {
		createPaintingsDataSet();

		SQLSelect<DataRow> q1 = SQLSelect.dataRowQuery("testmap", "SELECT * FROM PAINTING");
		assertTrue(q1.isFetchingDataRows());

		List<DataRow> result = context.select(q1);
		assertEquals(20, result.size());
		assertTrue(result.get(0) instanceof DataRow);
	}

	@Test
	public void dataRows_DefaultRoot() throws Exception {
		createPaintingsDataSet();

		SQLSelect<DataRow> q1 = SQLSelect.dataRowQuery("SELECT * FROM PAINTING");
		assertTrue(q1.isFetchingDataRows());

		List<DataRow> result = context.select(q1);
		assertEquals(20, result.size());
		assertTrue(result.get(0) instanceof DataRow);
	}

	@Test
	public void dataRows_ClassRoot() throws Exception {
		createPaintingsDataSet();

		SQLSelect<Painting> q1 = SQLSelect.query(Painting.class, "SELECT * FROM PAINTING").columnNameCaps(
				CapsStrategy.UPPER);
		assertFalse(q1.isFetchingDataRows());
		List<Painting> result = context.select(q1);
		assertEquals(20, result.size());
		assertTrue(result.get(0) instanceof Painting);
	}

	@Test
	public void dataRowWithTypes() throws Exception {
		createArtistDataSet();

		List<DataRow> result = SQLSelect.dataRowQuery("SELECT * FROM ARTIST_CT", Integer.class, String.class, LocalDateTime.class)
				.columnNameCaps(CapsStrategy.UPPER)
				.select(context);
		assertEquals(2, result.size());
		assertTrue(result.get(0) instanceof DataRow);
		assertInstanceOf(LocalDateTime.class, result.get(0).get("DATE_OF_BIRTH"));
	}

	@Test
	public void dataRowWithTypesMapped() throws Exception {
		createArtistDataSet();

		List<Object> result = SQLSelect.dataRowQuery("SELECT * FROM ARTIST_CT", Integer.class, String.class, LocalDateTime.class)
				.columnNameCaps(CapsStrategy.UPPER)
				.map(dataRow -> dataRow.get("ARTIST_ID"))
				.select(context);
		assertEquals(2, result.size());
		assertTrue(result.get(0) instanceof Integer);
	}

	@Test
	public void dataRowWithDirectives() throws Exception {
		createArtistDataSet();

		List<DataRow> result = SQLSelect.dataRowQuery("SELECT #result('ARTIST_ID' 'java.lang.Double'), #result('ARTIST_NAME' 'java.lang.String') FROM ARTIST_CT")
				.select(context);
		assertEquals(2, result.size());
		assertTrue(result.get(0) instanceof DataRow);
		assertTrue(result.get(0).get("ARTIST_ID") instanceof Double);
	}

	@Test
	public void dataRowWithTypesException() throws Exception {
		createArtistDataSet();

		assertThrows(CayenneRuntimeException.class, () ->
				SQLSelect.dataRowQuery("SELECT * FROM ARTIST_CT", Integer.class, String.class)
						.select(context));
	}

	@Test
	public void objectArrayWithDefaultTypesReturnAndDirectives() throws Exception {
		createArtistDataSet();

		List<Object[]> result = SQLSelect.columnQuery("SELECT #result('ARTIST_ID' 'java.lang.Long'), #result('ARTIST_NAME' 'java.lang.String') FROM ARTIST_CT")
				.select(context);

		assertEquals(2, result.size());
		assertTrue(result.get(0) instanceof Object[]);
		assertEquals(2, result.get(0).length);
		assertTrue(result.get(0)[0] instanceof Long);
		assertTrue(result.get(0)[1] instanceof String);
	}

	@Test
	public void objectArrayWithDefaultTypesReturnAndDirectivesMappedToPojo() throws Exception {
		createArtistDataSet();

		List<ArtistDataWrapper> result = SQLSelect
				.columnQuery("SELECT #result('ARTIST_ID' 'java.lang.Long'), #result('ARTIST_NAME' 'java.lang.String') FROM ARTIST_CT")
				.map(ArtistDataWrapper::new)
				.select(context);

		assertEquals(2, result.size());
		assertTrue(result.get(0).id > 0);
		assertNotNull(result.get(0).name);
	}

	@Test
	public void objectArrayReturnAndDirectives() throws Exception {
		createArtistDataSet();

		assertThrows(CayenneRuntimeException.class, () ->
				SQLSelect.columnQuery("SELECT #result('ARTIST_ID' 'java.lang.Long'), #result('ARTIST_NAME' 'java.lang.String') FROM ARTIST_CT",
						Integer.class, String.class).select(context));
	}

	@Test
	public void objectArrayWithOneObjectDefaultTypesReturnAndDirectives() throws Exception {
		createArtistDataSet();

		List<Object[]> result = SQLSelect.columnQuery("SELECT #result('ARTIST_ID' 'java.lang.Long') FROM ARTIST_CT")
				.select(context);

		assertEquals(2, result.size());
		assertTrue(result.get(0) instanceof Object[]);
		assertEquals(1, result.get(0).length);
		assertTrue(result.get(0)[0] instanceof Long);
	}

	@Test
	public void objectArrayQueryWithDefaultTypes() throws Exception {
		createPaintingsDataSet();

		List<Object[]> result = SQLSelect.columnQuery("SELECT PAINTING_ID, PAINTING_TITLE, ESTIMATED_PRICE FROM PAINTING")
				.select(context);

		assertEquals(20, result.size());
		assertEquals(3, result.get(0).length);
	}

	@Test
	public void objectQueryWithDefaultType() throws Exception {
		createPaintingsDataSet();

		List<Object[]> result = SQLSelect.columnQuery("SELECT PAINTING_ID FROM PAINTING")
				.select(context);
		assertEquals(20, result.size());
		assertTrue(result.get(0) instanceof Object[]);
		assertTrue(result.get(0)[0] instanceof Integer);
	}

	@Test
	public void objectArrayQueryException() throws Exception {
		createPaintingsDataSet();

		assertThrows(CayenneRuntimeException.class, () -> {
			SQLSelect<Object[]> query = SQLSelect.columnQuery("SELECT PAINTING_ID, PAINTING_TITLE, ESTIMATED_PRICE FROM PAINTING", Integer.class, String.class);
			context.performQuery(query);
		});
	}

	@Test
	public void singleObjectQuery() throws Exception {
		createPaintingsDataSet();

		List<Integer> result = SQLSelect.scalarQuery("SELECT PAINTING_ID FROM PAINTING", Integer.class)
				.select(context);
		assertEquals(20, result.size());
		assertTrue(result.get(0) instanceof Integer);
	}

	@Test
	public void objectArrayWithCustomType() throws SQLException {
		createArtistDataSet();

		List<Object[]> results = SQLSelect.columnQuery("SELECT * FROM ARTIST_CT",
				Integer.class, String.class, LocalDateTime.class).select(context);

		assertEquals(2, results.size());
		assertTrue(results.get(0) instanceof Object[]);
		assertEquals(3, results.get(0).length);
		assertTrue(results.get(0)[2] instanceof LocalDateTime);
	}

	@Test
	public void objectArrayWithCustomTypeMappedToPojo() throws SQLException {
		createArtistDataSet();

		List<ArtistDataWrapper> result = SQLSelect.columnQuery("SELECT * FROM ARTIST_CT",
				Integer.class, String.class, LocalDateTime.class)
				.map(ArtistDataWrapper::new)
				.select(context);

		assertEquals(2, result.size());
		assertTrue(result.get(0).id > 0);
		assertNotNull(result.get(0).name);
		assertNotNull(result.get(0).date);
	}

	@Test
	public void dataRows_ClassRoot_Parameters() throws Exception {
		createPaintingsDataSet();

		SQLSelect<Painting> q1 = SQLSelect.query(Painting.class,
				"SELECT * FROM PAINTING WHERE PAINTING_TITLE = #bind($a)");
		q1.param("a", "painting3").columnNameCaps(CapsStrategy.UPPER);

		assertFalse(q1.isFetchingDataRows());
		Painting a = context.selectOne(q1);
		assertEquals("painting3", a.getPaintingTitle());
	}

	@Test
	public void dataRows_ClassRoot_Bind() throws Exception {
		createPaintingsDataSet();

		SQLSelect<Painting> q1 = SQLSelect.query(Painting.class,
				"SELECT * FROM PAINTING WHERE PAINTING_TITLE = #bind($a) OR PAINTING_TITLE = #bind($b)")
				.columnNameCaps(CapsStrategy.UPPER);
		q1.param("a", "painting3").param("b", "painting4");

		List<Painting> result = context.select(q1);
		assertEquals(2, result.size());
	}

	@Test
	public void dataRows_ColumnNameCaps() {
		SQLSelect<DataRow> q1 = SQLSelect.dataRowQuery("SELECT * FROM PAINTING WHERE PAINTING_TITLE = 'painting2'");
		q1.upperColumnNames();

		SQLTemplate r1 = (SQLTemplate) q1.getReplacementQuery(context.getEntityResolver());
		assertEquals(CapsStrategy.UPPER, r1.getColumnNamesCapitalization());

		q1.lowerColumnNames();
		SQLTemplate r2 = (SQLTemplate) q1.getReplacementQuery(context.getEntityResolver());
		assertEquals(CapsStrategy.LOWER, r2.getColumnNamesCapitalization());
	}

	@Test
	public void dataRows_FetchLimit() throws Exception {
		createPaintingsDataSet();

		SQLSelect<DataRow> q1 = SQLSelect.dataRowQuery("SELECT * FROM PAINTING");
		q1.limit(5);

		assertEquals(5, context.select(q1).size());
	}

	@Test
	public void dataRows_FetchOffset() throws Exception {
		createPaintingsDataSet();

		SQLSelect<DataRow> q1 = SQLSelect.dataRowQuery("SELECT * FROM PAINTING");
		q1.offset(4);

		assertEquals(16, context.select(q1).size());
	}

	@Test
	public void append() throws Exception {
		createPaintingsDataSet();

		SQLSelect<Painting> q1 = SQLSelect.query(Painting.class, "SELECT * FROM PAINTING")
				.append(" WHERE PAINTING_TITLE = #bind($a)").param("a", "painting3")
				.columnNameCaps(CapsStrategy.UPPER);

		List<Painting> result = context.select(q1);
		assertEquals(1, result.size());
	}

	@Test
	public void select() throws Exception {
		createPaintingsDataSet();

		List<Painting> result = SQLSelect
				.query(Painting.class, "SELECT * FROM PAINTING WHERE PAINTING_TITLE = #bind($a)")
				.param("a", "painting3")
				.columnNameCaps(CapsStrategy.UPPER)
				.select(context);

		assertEquals(1, result.size());
	}

	@Test
	public void selectOne() throws Exception {
		createPaintingsDataSet();

		Painting a = SQLSelect.query(Painting.class, "SELECT * FROM PAINTING WHERE PAINTING_TITLE = #bind($a)")
				.param("a", "painting3").columnNameCaps(CapsStrategy.UPPER).selectOne(context);

		assertEquals("painting3", a.getPaintingTitle());
	}

	@Test
	public void selectFirst() throws Exception {
		createPaintingsDataSet();

		Painting p = SQLSelect.query(Painting.class, "SELECT * FROM PAINTING ORDER BY PAINTING_TITLE").columnNameCaps(CapsStrategy.UPPER).selectFirst(
				context);

		assertNotNull(p);
		assertEquals("painting1", p.getPaintingTitle());
	}

	@Test
	public void selectFirstByContext() throws Exception {
		createPaintingsDataSet();

		SQLSelect<Painting> q = SQLSelect.query(Painting.class, "SELECT * FROM PAINTING ORDER BY PAINTING_TITLE").columnNameCaps(CapsStrategy.UPPER);
		Painting p = context.selectFirst(q);

		assertNotNull(p);
		assertEquals("painting1", p.getPaintingTitle());
	}

	@Test
	public void iterate() throws Exception {
		createPaintingsDataSet();

		final int[] count = new int[1];
		SQLSelect.query(Painting.class, "SELECT * FROM PAINTING").columnNameCaps(CapsStrategy.UPPER)
				.iterate(context, object -> {
					assertNotNull(object.getPaintingTitle());
					count[0]++;
				});

		assertEquals(20, count[0]);
	}

	@Test
	public void iterator() throws Exception {
		createPaintingsDataSet();

		try (ResultIterator<Painting> it = SQLSelect.query(Painting.class, "SELECT * FROM PAINTING")
				.columnNameCaps(CapsStrategy.UPPER).iterator(context)) {
			int count = 0;

			for (Painting p : it) {
				count++;
			}

			assertEquals(20, count);
		}
	}

	@Test
	public void batchIterator() throws Exception {
		createPaintingsDataSet();

		try (ResultBatchIterator<Painting> it = SQLSelect.query(Painting.class, "SELECT * FROM PAINTING")
				.columnNameCaps(CapsStrategy.UPPER).batchIterator(context, 5)) {
			int count = 0;

			for (List<Painting> paintingList : it) {
				count++;
				assertEquals(5, paintingList.size());
			}

			assertEquals(4, count);
		}
	}

	@Test
	public void selectLong() throws Exception {
		createPaintingsDataSet();

		long id = SQLSelect
				.scalarQuery( "SELECT PAINTING_ID FROM PAINTING WHERE PAINTING_TITLE = #bind($a)",
						Integer.class)
				.param("a", "painting3").selectOne(context);

		assertEquals(3L, id);
	}

	@Test
	public void selectLongArray() throws Exception {
		createPaintingsDataSet();

		List<Integer> ids = SQLSelect.scalarQuery("SELECT PAINTING_ID FROM PAINTING ORDER BY PAINTING_ID",
				Integer.class).select(context);

		assertEquals(20, ids.size());
		assertEquals(2L, ids.get(1).intValue());
	}

	@Test
	public void selectCount() throws Exception {
		createPaintingsDataSet();

		int c = SQLSelect.scalarQuery("SELECT COUNT(*) FROM PAINTING", Integer.class).selectOne(context);

		assertEquals(20, c);
	}

	@Test
	public void paramsArray_Single() throws Exception {
		createPaintingsDataSet();

		Integer id = SQLSelect
				.scalarQuery( "SELECT PAINTING_ID FROM PAINTING WHERE PAINTING_TITLE = #bind($a)",
						Integer.class)
				.paramsArray("painting3").selectOne(context);

		assertEquals(3L, id.intValue());
	}

	@Test
	public void paramsArray_Multiple() throws Exception {
		createPaintingsDataSet();

		List<Integer> ids = SQLSelect
				.scalarQuery("SELECT PAINTING_ID FROM PAINTING WHERE PAINTING_TITLE = #bind($a) " +
						"OR PAINTING_TITLE = #bind($b) ORDER BY PAINTING_ID",
						Integer.class)
				.paramsArray("painting3", "painting2").select(context);

		assertEquals(2L, ids.get(0).intValue());
		assertEquals(3L, ids.get(1).intValue());
	}

	@Test
	@Disabled("This is supported by Velocity only")
	// TODO: move this test to new cayenne-velocity module
	public void paramsArray_Multiple_OptionalChunks() throws Exception {
		tPainting.insert(1, "painting1", 1.0);
		tPainting.insert(2, "painting2", null);

		List<Integer> ids = SQLSelect
				.scalarQuery(
						"SELECT PAINTING_ID FROM PAINTING #chain('OR' 'WHERE') "
								+ "#chunk($a) ESTIMATED_PRICE #bindEqual($a) #end "
								+ "#chunk($b) PAINTING_TITLE #bindEqual($b) #end #end ORDER BY PAINTING_ID",
						Integer.class)
				.paramsArray(null, "painting1").select(context);

		assertEquals(1, ids.size());
		assertEquals(1L, ids.get(0).longValue());
	}

	@Test
	@Disabled("This is supported by Velocity only")
	// TODO: move this test to new cayenne-velocity module
	public void params_Multiple_OptionalChunks() throws Exception {
		tPainting.insert(1, "painting1", 1.0);
		tPainting.insert(2, "painting2", null);

		Map<String, Object> params = new HashMap<>();
		params.put("a", null);
		params.put("b", "painting1");

		List<Integer> ids = SQLSelect
				.scalarQuery(
						"SELECT PAINTING_ID FROM PAINTING #chain('OR' 'WHERE') "
								+ "#chunk($a) ESTIMATED_PRICE #bindEqual($a) #end "
								+ "#chunk($b) PAINTING_TITLE #bindEqual($b) #end #end ORDER BY PAINTING_ID",
						Integer.class)
				.params(params).select(context);

		assertEquals(1, ids.size());
		assertEquals(1L, ids.get(0).longValue());
	}

	@Test
	public void byteArray() throws Exception {
		byte[] data = {1, 2, 3};
		tPainting.insert(1, "test", 0);
		tPaintingInfo.insert(1, data);

		byte[] bytes = SQLSelect
				.scalarQuery("SELECT IMAGE_BLOB FROM PAINTING_INFO", byte[].class)
				.selectOne(context);
		assertArrayEquals(data, bytes);
	}

	static class ArtistDataWrapper {
		long id;
		String name;
		LocalDateTime date;
		ArtistDataWrapper(Object[] data) {
			id = ((Number)data[0]).longValue();
			name = (String)data[1];
			if(data.length > 2) {
				date = (LocalDateTime)data[2];
			}
		}
	}
}
