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
import org.apache.cayenne.QueryResult;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.SelectResult;
import org.apache.cayenne.UpdateResult;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.frontbase.FrontBaseAdapter;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.*;

public class SQLQueryIT {

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

		SQLQuery<DataRow> q1 = SQLQuery.dataRowQuery("testmap", "SELECT * FROM PAINTING");
		assertTrue(q1.isFetchingDataRows());

		List<DataRow> result = context.select(q1);
		assertEquals(20, result.size());
		assertTrue(result.get(0) instanceof DataRow);
	}

	@Test
	public void dataRows_DefaultRoot() throws Exception {
		createPaintingsDataSet();

		SQLQuery<DataRow> q1 = SQLQuery.dataRowQuery("SELECT * FROM PAINTING");
		assertTrue(q1.isFetchingDataRows());

		List<DataRow> result = context.select(q1);
		assertEquals(20, result.size());
		assertTrue(result.get(0) instanceof DataRow);
	}

	@Test
	public void dataNode() throws Exception {
		createPaintingsDataSet();

		List<DataRow> result = SQLQuery.dataRowQuery("SELECT * FROM PAINTING")
				.dataNode(env.dataNode().getName())
				.select(context);
		assertEquals(20, result.size());
	}

	@Test
	public void dataNode_Unknown() throws Exception {
		SQLQuery<DataRow> query = SQLQuery.dataRowQuery("SELECT * FROM PAINTING").dataNode("no_such_node");
		CayenneRuntimeException e = assertThrows(CayenneRuntimeException.class, () -> query.select(context));
		assertTrue(e.getMessage().contains("no_such_node"), e.getMessage());
	}

	@Test
	public void dialect() throws Exception {
		createPaintingsDataSet();
		Class<? extends DbAdapter> adapterType = env.runtime().getDataDomain().getDefaultNode().getAdapter().getClass();

		// the dialect of the current adapter replaces the default SQL
		List<DataRow> matching = SQLQuery.dataRowQuery("SELECT * FROM PAINTING")
				.dialect(adapterType, "SELECT * FROM PAINTING WHERE PAINTING_ID <= 5")
				.select(context);
		assertEquals(5, matching.size());

		// the dialect of another adapter is ignored
		List<DataRow> notMatching = SQLQuery.dataRowQuery("SELECT * FROM PAINTING")
				.dialect(FrontBaseAdapter.class, "SELECT * FROM PAINTING WHERE PAINTING_ID <= 5")
				.select(context);
		assertEquals(20, notMatching.size());
	}

	@Test
	public void dialect_Append() throws Exception {
		createPaintingsDataSet();
		Class<? extends DbAdapter> adapterType = env.runtime().getDataDomain().getDefaultNode().getAdapter().getClass();

		List<DataRow> result = SQLQuery.dataRowQuery("SELECT * FROM PAINTING")
				.append(" WHERE PAINTING_ID <= 3")
				.dialect(adapterType, "SELECT * FROM PAINTING")
				.dialectAppend(adapterType, " WHERE PAINTING_ID <= 5")
				.select(context);
		assertEquals(5, result.size());
	}

	@Test
	public void dataRows_ClassRoot() throws Exception {
		createPaintingsDataSet();

		SQLQuery<Painting> q1 = SQLQuery.query(Painting.class, "SELECT * FROM PAINTING").columnNameCaps(
				CapsStrategy.UPPER);
		assertFalse(q1.isFetchingDataRows());
		List<Painting> result = context.select(q1);
		assertEquals(20, result.size());
		assertTrue(result.get(0) instanceof Painting);
	}

	@Test
	public void dataRowWithTypes() throws Exception {
		createArtistDataSet();

		List<DataRow> result = SQLQuery.dataRowQuery("SELECT * FROM ARTIST_CT", Integer.class, String.class, LocalDateTime.class)
				.columnNameCaps(CapsStrategy.UPPER)
				.select(context);
		assertEquals(2, result.size());
		assertTrue(result.get(0) instanceof DataRow);
		assertInstanceOf(LocalDateTime.class, result.get(0).get("DATE_OF_BIRTH"));
	}

	@Test
	public void dataRowWithDirectives() throws Exception {
		createArtistDataSet();

		List<DataRow> result = SQLQuery.dataRowQuery("SELECT #result('ARTIST_ID' 'java.lang.Double'), #result('ARTIST_NAME' 'java.lang.String') FROM ARTIST_CT")
				.select(context);
		assertEquals(2, result.size());
		assertTrue(result.get(0) instanceof DataRow);
		assertTrue(result.get(0).get("ARTIST_ID") instanceof Double);
	}

	@Test
	public void dataRowWithTypesException() throws Exception {
		createArtistDataSet();

		assertThrows(CayenneRuntimeException.class, () ->
				SQLQuery.dataRowQuery("SELECT * FROM ARTIST_CT", Integer.class, String.class)
						.select(context));
	}

	@Test
	public void objectArrayWithDefaultTypesReturnAndDirectives() throws Exception {
		createArtistDataSet();

		List<Object[]> result = SQLQuery.columnQuery("SELECT #result('ARTIST_ID' 'java.lang.Long'), #result('ARTIST_NAME' 'java.lang.String') FROM ARTIST_CT")
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

		List<ArtistDataWrapper> result = SQLQuery
				.columnQuery("SELECT #result('ARTIST_ID' 'java.lang.Long'), #result('ARTIST_NAME' 'java.lang.String') FROM ARTIST_CT")
				.select(context)
				.stream()
				.map(ArtistDataWrapper::new)
				.toList();

		assertEquals(2, result.size());
		assertTrue(result.get(0).id > 0);
		assertNotNull(result.get(0).name);
	}

	@Test
	public void objectArrayReturnAndDirectives() throws Exception {
		createArtistDataSet();

		assertThrows(CayenneRuntimeException.class, () ->
				SQLQuery.columnQuery("SELECT #result('ARTIST_ID' 'java.lang.Long'), #result('ARTIST_NAME' 'java.lang.String') FROM ARTIST_CT",
						Integer.class, String.class).select(context));
	}

	@Test
	public void objectArrayWithOneObjectDefaultTypesReturnAndDirectives() throws Exception {
		createArtistDataSet();

		List<Object[]> result = SQLQuery.columnQuery("SELECT #result('ARTIST_ID' 'java.lang.Long') FROM ARTIST_CT")
				.select(context);

		assertEquals(2, result.size());
		assertTrue(result.get(0) instanceof Object[]);
		assertEquals(1, result.get(0).length);
		assertTrue(result.get(0)[0] instanceof Long);
	}

	@Test
	public void objectArrayQueryWithDefaultTypes() throws Exception {
		createPaintingsDataSet();

		List<Object[]> result = SQLQuery.columnQuery("SELECT PAINTING_ID, PAINTING_TITLE, ESTIMATED_PRICE FROM PAINTING")
				.select(context);

		assertEquals(20, result.size());
		assertEquals(3, result.get(0).length);
	}

	@Test
	public void objectQueryWithDefaultType() throws Exception {
		createPaintingsDataSet();

		List<Object[]> result = SQLQuery.columnQuery("SELECT PAINTING_ID FROM PAINTING")
				.select(context);
		assertEquals(20, result.size());
		assertTrue(result.get(0) instanceof Object[]);
		assertTrue(result.get(0)[0] instanceof Integer);
	}

	@Test
	public void objectArrayQueryException() throws Exception {
		createPaintingsDataSet();

		assertThrows(CayenneRuntimeException.class, () -> {
			SQLQuery<Object[]> query = SQLQuery.columnQuery("SELECT PAINTING_ID, PAINTING_TITLE, ESTIMATED_PRICE FROM PAINTING", Integer.class, String.class);
			context.select(query);
		});
	}

	@Test
	public void singleObjectQuery() throws Exception {
		createPaintingsDataSet();

		List<Integer> result = SQLQuery.scalarQuery("SELECT PAINTING_ID FROM PAINTING", Integer.class)
				.select(context);
		assertEquals(20, result.size());
		assertTrue(result.get(0) instanceof Integer);
	}

	@Test
	public void objectArrayWithCustomType() throws SQLException {
		createArtistDataSet();

		List<Object[]> results = SQLQuery.columnQuery("SELECT * FROM ARTIST_CT",
				Integer.class, String.class, LocalDateTime.class).select(context);

		assertEquals(2, results.size());
		assertTrue(results.get(0) instanceof Object[]);
		assertEquals(3, results.get(0).length);
		assertTrue(results.get(0)[2] instanceof LocalDateTime);
	}

	@Test
	public void objectArrayWithCustomTypeMappedToPojo() throws SQLException {
		createArtistDataSet();

		List<ArtistDataWrapper> result = SQLQuery.columnQuery("SELECT * FROM ARTIST_CT",
				Integer.class, String.class, LocalDateTime.class)
				.select(context)
				.stream()
				.map(ArtistDataWrapper::new)
				.toList();

		assertEquals(2, result.size());
		assertTrue(result.get(0).id > 0);
		assertNotNull(result.get(0).name);
		assertNotNull(result.get(0).date);
	}

	@Test
	public void dataRows_ClassRoot_Parameters() throws Exception {
		createPaintingsDataSet();

		SQLQuery<Painting> q1 = SQLQuery.query(Painting.class,
				"SELECT * FROM PAINTING WHERE PAINTING_TITLE = #bind($a)");
		q1.param("a", "painting3").columnNameCaps(CapsStrategy.UPPER);

		assertFalse(q1.isFetchingDataRows());
		Painting a = context.selectOne(q1);
		assertEquals("painting3", a.getPaintingTitle());
	}

	@Test
	public void dataRows_ClassRoot_Bind() throws Exception {
		createPaintingsDataSet();

		SQLQuery<Painting> q1 = SQLQuery.query(Painting.class,
				"SELECT * FROM PAINTING WHERE PAINTING_TITLE = #bind($a) OR PAINTING_TITLE = #bind($b)")
				.columnNameCaps(CapsStrategy.UPPER);
		q1.param("a", "painting3").param("b", "painting4");

		List<Painting> result = context.select(q1);
		assertEquals(2, result.size());
	}

	@Test
	public void dataRows_ColumnNameCaps() {
		SQLQuery<DataRow> q1 = SQLQuery.dataRowQuery("SELECT * FROM PAINTING WHERE PAINTING_TITLE = 'painting2'");
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

		SQLQuery<DataRow> q1 = SQLQuery.dataRowQuery("SELECT * FROM PAINTING");
		q1.limit(5);

		assertEquals(5, context.select(q1).size());
	}

	@Test
	public void dataRows_FetchOffset() throws Exception {
		createPaintingsDataSet();

		SQLQuery<DataRow> q1 = SQLQuery.dataRowQuery("SELECT * FROM PAINTING");
		q1.offset(4);

		assertEquals(16, context.select(q1).size());
	}

	@Test
	public void append() throws Exception {
		createPaintingsDataSet();

		SQLQuery<Painting> q1 = SQLQuery.query(Painting.class, "SELECT * FROM PAINTING")
				.append(" WHERE PAINTING_TITLE = #bind($a)").param("a", "painting3")
				.columnNameCaps(CapsStrategy.UPPER);

		List<Painting> result = context.select(q1);
		assertEquals(1, result.size());
	}

	@Test
	public void select() throws Exception {
		createPaintingsDataSet();

		List<Painting> result = SQLQuery
				.query(Painting.class, "SELECT * FROM PAINTING WHERE PAINTING_TITLE = #bind($a)")
				.param("a", "painting3")
				.columnNameCaps(CapsStrategy.UPPER)
				.select(context);

		assertEquals(1, result.size());
	}

	@Test
	public void selectOne() throws Exception {
		createPaintingsDataSet();

		Painting a = SQLQuery.query(Painting.class, "SELECT * FROM PAINTING WHERE PAINTING_TITLE = #bind($a)")
				.param("a", "painting3").columnNameCaps(CapsStrategy.UPPER).selectOne(context);

		assertEquals("painting3", a.getPaintingTitle());
	}

	@Test
	public void selectFirst() throws Exception {
		createPaintingsDataSet();

		Painting p = SQLQuery.query(Painting.class, "SELECT * FROM PAINTING ORDER BY PAINTING_TITLE").columnNameCaps(CapsStrategy.UPPER).selectFirst(
				context);

		assertNotNull(p);
		assertEquals("painting1", p.getPaintingTitle());
	}

	@Test
	public void selectFirstByContext() throws Exception {
		createPaintingsDataSet();

		SQLQuery<Painting> q = SQLQuery.query(Painting.class, "SELECT * FROM PAINTING ORDER BY PAINTING_TITLE").columnNameCaps(CapsStrategy.UPPER);
		Painting p = context.selectFirst(q);

		assertNotNull(p);
		assertEquals("painting1", p.getPaintingTitle());
	}

	@Test
	public void iterate() throws Exception {
		createPaintingsDataSet();

		final int[] count = new int[1];
		SQLQuery.query(Painting.class, "SELECT * FROM PAINTING").columnNameCaps(CapsStrategy.UPPER)
				.iterate(context, object -> {
					assertNotNull(object.getPaintingTitle());
					count[0]++;
				});

		assertEquals(20, count[0]);
	}

	@Test
	public void iterator() throws Exception {
		createPaintingsDataSet();

		try (ResultIterator<Painting> it = SQLQuery.query(Painting.class, "SELECT * FROM PAINTING")
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

		try (ResultBatchIterator<Painting> it = SQLQuery.query(Painting.class, "SELECT * FROM PAINTING")
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

		long id = SQLQuery
				.scalarQuery( "SELECT PAINTING_ID FROM PAINTING WHERE PAINTING_TITLE = #bind($a)",
						Integer.class)
				.param("a", "painting3").selectOne(context);

		assertEquals(3L, id);
	}

	@Test
	public void selectLongArray() throws Exception {
		createPaintingsDataSet();

		List<Integer> ids = SQLQuery.scalarQuery("SELECT PAINTING_ID FROM PAINTING ORDER BY PAINTING_ID",
				Integer.class).select(context);

		assertEquals(20, ids.size());
		assertEquals(2L, ids.get(1).intValue());
	}

	@Test
	public void selectCount() throws Exception {
		createPaintingsDataSet();

		int c = SQLQuery.scalarQuery("SELECT COUNT(*) FROM PAINTING", Integer.class).selectOne(context);

		assertEquals(20, c);
	}

	@Test
	public void paramsArray_Single() throws Exception {
		createPaintingsDataSet();

		Integer id = SQLQuery
				.scalarQuery( "SELECT PAINTING_ID FROM PAINTING WHERE PAINTING_TITLE = #bind($a)",
						Integer.class)
				.paramsArray("painting3").selectOne(context);

		assertEquals(3L, id.intValue());
	}

	@Test
	public void paramsArray_Multiple() throws Exception {
		createPaintingsDataSet();

		List<Integer> ids = SQLQuery
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

		List<Integer> ids = SQLQuery
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

		List<Integer> ids = SQLQuery
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

		byte[] bytes = SQLQuery
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

	// --- non-selecting SQL, formerly tested via SQLExec ---

	private int update(SQLQuery<?> query) {
		List<QueryResult> results = query.execute(context);
		assertEquals(1, results.size());
		return ((UpdateResult) results.getFirst()).count();
	}

	@Test
	public void execute_Update_DataMapNameRoot() throws Exception {
		int inserted = update(SQLQuery.query("testmap", "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (1, 'a')"));
		assertEquals(1, inserted);
	}

	@Test
	public void execute_Update_DefaultRoot() throws Exception {
		int inserted = update(SQLQuery.query("INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (1, 'a')"));
		assertEquals(1, inserted);
	}

	@Test
	public void execute_Update_DataNode() throws Exception {
		int inserted = update(SQLQuery.query("INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (1, 'a')")
				.dataNode(env.dataNode().getName()));
		assertEquals(1, inserted);
		assertEquals(1L, env.table("ARTIST").getLong("ARTIST_ID"));
	}

	@Test
	public void execute_Update_DataNode_Unknown() throws Exception {
		SQLQuery<DataRow> query = SQLQuery.query("INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (1, 'a')")
				.dataNode("no_such_node");
		CayenneRuntimeException e = assertThrows(CayenneRuntimeException.class, () -> query.execute(context));
		assertTrue(e.getMessage().contains("no_such_node"), e.getMessage());
	}

	@Test
	public void execute_Update_Dialect() throws Exception {
		Class<? extends DbAdapter> adapterType = env.runtime().getDataDomain().getDefaultNode().getAdapter().getClass();

		int inserted = update(SQLQuery.query("INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (1, 'default')")
				.dialect(adapterType, "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (2, 'dialect')"));

		assertEquals(1, inserted);
		assertEquals(2L, env.table("ARTIST").getLong("ARTIST_ID"));
		assertEquals("dialect", env.table("ARTIST").getString("ARTIST_NAME").trim());
	}

	@Test
	public void execute_Update_Dialect_OtherAdapter() throws Exception {
		int inserted = update(SQLQuery.query("INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (1, 'default')")
				.dialect(FrontBaseAdapter.class, "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (2, 'dialect')"));

		assertEquals(1, inserted);
		assertEquals(1L, env.table("ARTIST").getLong("ARTIST_ID"));
		assertEquals("default", env.table("ARTIST").getString("ARTIST_NAME").trim());
	}

	@Test
	public void execute_Update_Dialect_Append() throws Exception {
		Class<? extends DbAdapter> adapterType = env.runtime().getDataDomain().getDefaultNode().getAdapter().getClass();

		int inserted = update(SQLQuery.query("INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME)")
				.append(" VALUES (1, 'default')")
				.dialect(adapterType, "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME)")
				.dialectAppend(adapterType, " VALUES (2, 'dialect')"));

		assertEquals(1, inserted);
		assertEquals(2L, env.table("ARTIST").getLong("ARTIST_ID"));
	}

	@Test
	public void execute_ReturnGeneratedKeys() {
		if (env.testDbAdapter().supportsGeneratedKeys()) {
			List<QueryResult> response = SQLQuery.query("testmap", "INSERT INTO GENERATED_COLUMN (NAME) VALUES ('Surikov')")
					.returnGeneratedKeys(true)
					.execute(context);
			assertEquals(2, response.size());

			List<QueryResult> response1 = SQLQuery.query("testmap", "INSERT INTO GENERATED_COLUMN (NAME) VALUES ('Sidorov')")
					.returnGeneratedKeys(false)
					.execute(context);
			assertEquals(1, response1.size());
		}
	}

	@Test
	public void execute_Update_ParamsArray_Single() throws Exception {
		int inserted = update(SQLQuery.query("INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (1, #bind($name))")
				.paramsArray("a3"));

		assertEquals(1, inserted);
		assertEquals("a3", env.table("ARTIST").getString("ARTIST_NAME").trim());
	}

	@Test
	public void execute_Update_ParamsArray_Multiple() throws Exception {
		int inserted = update(SQLQuery.query("INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (#bind($id), #bind($name))")
				.paramsArray(55, "a3"));

		assertEquals(1, inserted);
		assertEquals(55L, env.table("ARTIST").getLong("ARTIST_ID"));
		assertEquals("a3", env.table("ARTIST").getString("ARTIST_NAME").trim());
	}

	@Test
	public void execute_Select() throws Exception {
		update(SQLQuery.query("INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (1, 'a')"));

		// the untyped query selects data rows
		List<QueryResult> result = SQLQuery.query("SELECT * FROM ARTIST").execute(context);
		assertEquals(2, result.size());
		List<?> rows = ((SelectResult<?>) result.getFirst()).objects();
		assertEquals(1, rows.size());

		DataRow row = (DataRow) rows.getFirst();
		if (env.testDbAdapter().isLowerCaseNames()) {
			assertTrue(row.containsKey("artist_id"));
			assertEquals(1L, ((Number) row.get("artist_id")).longValue());
			assertEquals("a", row.get("artist_name"));
		} else {
			assertTrue(row.containsKey("ARTIST_ID"));
			assertEquals(1L, ((Number) row.get("ARTIST_ID")).longValue());
			assertEquals("a", row.get("ARTIST_NAME"));
		}
	}

	@Test
	public void select_UntypedQuery() throws Exception {
		update(SQLQuery.query("INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (1, 'a')"));

		List<DataRow> rows = SQLQuery.query("SELECT * FROM ARTIST").select(context);
		assertEquals(1, rows.size());
		assertEquals("a", rows.getFirst().get(env.testDbAdapter().isLowerCaseNames() ? "artist_name" : "ARTIST_NAME"));
	}

	@Test
	public void select_Update_NoResultSets() throws Exception {
		// a non-selecting query has no rows to select
		List<DataRow> rows = SQLQuery.query("INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (1, 'a')").select(context);
		assertTrue(rows.isEmpty());
		assertEquals(1L, env.table("ARTIST").getLong("ARTIST_ID"));
	}

	@Test
	public void execute_MultipleArrayBind() throws Exception {
		SQLQuery<DataRow> inserter = SQLQuery.query("INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (#bind($id), #bind($name))");
		for (int i = 0; i < 2; i++) {
			assertEquals(1, update(inserter.paramsArray(i, "artist " + i)));
		}
		assertEquals(2, env.table("ARTIST").getRowCount());
	}

	@Test
	public void execute_MultipleMapBind() throws Exception {
		SQLQuery<DataRow> inserter = SQLQuery.query("INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (#bind($id), #bind($name))");
		for (int i = 0; i < 2; i++) {
			Map<String, Object> params = new HashMap<>();
			params.put("id", i);
			params.put("name", "artist " + i);
			assertEquals(1, update(inserter.params(params)));
		}
		assertEquals(2, env.table("ARTIST").getRowCount());
	}
}
