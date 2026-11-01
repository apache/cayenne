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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.dba.OracleTestDbAdapter;
import org.apache.cayenne.unit.dba.TestDbAdapter;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SQLQueryResultIT {

	@RegisterExtension
	static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

	private DataContext context;
	protected TestDbAdapter testDbAdapter;

	private TableHelper tPainting;
	private TableHelper tArtist;
	private TableHelper tArtistCt;

	@BeforeEach
	public void setUp() throws Exception {
		context = env.context();
		testDbAdapter = env.testDbAdapter();
		tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");

		tPainting = env.table("PAINTING", "PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE", "ESTIMATED_PRICE");

		tArtistCt = env.table("ARTIST_CT", "ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");
	}

	private void createArtistDataSet() throws SQLException {
		tArtist.insert(15, "Surikov", new Date(System.currentTimeMillis()));
		tArtist.insert(16, "Ivanov", new Date(System.currentTimeMillis()));
	}

	@Test
	public void testSQLTemplateForDataMap() {
		SQLQuery<DataRow> q1 = SQLQuery.query("SELECT * FROM ARTIST").dataMap("testmap");
		List<DataRow> result = context.select(q1);
		assertEquals(0, result.size());
	}

	@Test
	public void testSQLTemplateForDataMapWithInsert() throws SQLException {
		createArtistDataSet();

		SQLQuery<DataRow> q2 = SQLQuery.query("SELECT * FROM ARTIST").dataMap("testmap");
		List<DataRow> result = context.select(q2);
		assertEquals(2, result.size());
	}

	@Test
	public void testReturnGeneratedKeys() {
		if(testDbAdapter.supportsGeneratedKeys()) {
			String sql = "INSERT INTO GENERATED_COLUMN (NAME) VALUES ('Surikov')";
			SQLQuery<DataRow> q1 = SQLQuery.query(sql).dataMap("testmap");
			q1.returnGeneratedKeys(true);
			List<DataRow> response = context.select(q1);
			assertEquals(1, response.size());

			String sql1 = "INSERT INTO GENERATED_COLUMN (NAME) VALUES ('Test')";
			SQLQuery<DataRow> q2 = SQLQuery.query(sql1).dataMap("testmap");
			q2.returnGeneratedKeys(false);
			List<DataRow> response1 = context.select(q2);
			assertEquals(0, response1.size());
		}
	}

	@Test
	public void testWithRootUsingScalar() throws SQLException {
		createArtistDataSet();

		SQLQuery<Object[]> q3 = SQLQuery.query(Artist.class, "SELECT ARTIST_ID, ARTIST_NAME FROM ARTIST").useScalar();
		q3.resultColumnsTypes(Double.class, String.class);
		List<Object[]> result = context.select(q3);
		assertEquals(2, result.size());
		assertTrue(result.get(0) instanceof Object[]);
		assertTrue(result.get(0)[0] instanceof Double);
	}

	@Test
	public void testWithRootUsingDataRow() throws SQLException {
		createArtistDataSet();

		SQLQuery<DataRow> q3 = SQLQuery.query(Artist.class, "SELECT ARTIST_ID, ARTIST_NAME FROM ARTIST").fetchDataRows();
		q3.resultColumnsTypes(Double.class, String.class);
		q3.columnNameCaps(CapsStrategy.UPPER);
		List<DataRow> result = context.select(q3);
		assertEquals(2, result.size());
		assertTrue(result.get(0) instanceof DataRow);
		assertInstanceOf(Double.class, result.get(0).get("ARTIST_ID"));
	}

	@Test
	public void withRootException() throws SQLException {
		createArtistDataSet();

		assertThrows(CayenneRuntimeException.class, () -> {
			SQLQuery<Artist> q3 = SQLQuery.query(Artist.class, "SELECT ARTIST_ID, ARTIST_NAME FROM ARTIST");
			q3.resultColumnsTypes(Double.class, String.class);
			context.select(q3);
		});
	}

	@Test
	public void usingScalarAndDataRow() throws SQLException {
		createArtistDataSet();

		assertThrows(CayenneRuntimeException.class, () -> {
			SQLQuery<Object[]> q3 = SQLQuery.query(Artist.class, "SELECT ARTIST_ID, ARTIST_NAME FROM ARTIST").fetchDataRows().useScalar();
			context.select(q3);
		});
	}

	@Test
	public void testDataRowWithTypes() throws SQLException {
		createArtistDataSet();

		SQLQuery<DataRow> q3 = SQLQuery.query("SELECT ARTIST_ID, ARTIST_NAME FROM ARTIST").dataMap("testmap");
		q3.resultColumnsTypes(Double.class, String.class);
		q3.columnNameCaps(CapsStrategy.UPPER);
		List<DataRow> artists = context.select(q3);
		assertEquals(2, artists.size());
		assertTrue(artists.get(0) instanceof DataRow);
		assertInstanceOf(Double.class, artists.get(0).get("ARTIST_ID"));
	}

	@Test
	public void testDataRowReturnAndDirectives() throws SQLException {
		createArtistDataSet();

		SQLQuery<DataRow> q3 = SQLQuery.query("SELECT #result('ARTIST_ID' 'java.lang.Long'), #result('ARTIST_NAME' 'java.lang.String') FROM ARTIST").dataMap("testmap");
		List<DataRow> result = context.select(q3);
		assertEquals(2, result.size());
		assertTrue(result.get(0) instanceof DataRow);
		assertEquals(2, result.get(0).size());
		assertTrue(result.get(0).get("ARTIST_ID") instanceof Long);
		assertTrue(result.get(0).get("ARTIST_NAME") instanceof String);
	}

	@Test
	public void dataRowReturnAndDirectivesExc() throws SQLException {
		createArtistDataSet();

		assertThrows(CayenneRuntimeException.class, () -> {
			SQLQuery<DataRow> q3 = SQLQuery.query("SELECT #result('ARTIST_ID' 'java.lang.Long'), #result('ARTIST_NAME' 'java.lang.String') FROM ARTIST").dataMap("testmap");
			q3.resultColumnsTypes(Integer.class, String.class);
			context.select(q3);
		});
	}

	@Test
	public void testObjectArrayReturnAndDirectives() throws SQLException {
		createArtistDataSet();

		SQLQuery<Object[]> q3 = SQLQuery.columnQuery("SELECT #result('ARTIST_ID' 'java.lang.Long'), #result('ARTIST_NAME' 'java.lang.String') FROM ARTIST");
		List<Object[]> result = context.select(q3);
		assertEquals(2, result.size());
		assertTrue(result.get(0) instanceof Object[]);
		assertEquals(2, result.get(0).length);
		assertTrue(result.get(0)[0] instanceof Long);
		assertTrue(result.get(0)[1] instanceof String);
	}

	@Test
	public void objectArrayReturnAndDirectivesException() throws SQLException {
		createArtistDataSet();

		assertThrows(CayenneRuntimeException.class, () -> {
			SQLQuery<Object[]> q3 = SQLQuery.columnQuery("SELECT #result('ARTIST_ID' 'java.lang.Long'), #result('ARTIST_NAME' 'java.lang.String') FROM ARTIST");
			q3.resultColumnsTypes(Integer.class, String.class);
			context.select(q3);
		});
	}

	@Test
	public void testObjectArrayWithSingleObjectReturnAndDirectives() throws SQLException {
		createArtistDataSet();

		SQLQuery<Object[]> q3 = SQLQuery.columnQuery("SELECT #result('ARTIST_ID' 'java.lang.Long') FROM ARTIST");
		List<Object[]> result = context.select(q3);
		assertEquals(2, result.size());
		assertTrue(result.get(0) instanceof Object[]);
		assertEquals(1, result.get(0).length);
		assertTrue(result.get(0)[0] instanceof Long);
	}

	@Test
	public void objectArrayReturnWithException() throws SQLException {
		createArtistDataSet();

		assertThrows(CayenneRuntimeException.class, () -> {
			SQLQuery<Object[]> q3 = SQLQuery.columnQuery("SELECT ARTIST_ID, ARTIST_NAME FROM ARTIST");
			q3.resultColumnsTypes(Integer.class);
			context.select(q3);
		});
	}

	@Test
	public void testObjectArrayWithSingleObjectReturn() throws SQLException {
		createArtistDataSet();

		SQLQuery<Object[]> q3 = SQLQuery.columnQuery("SELECT ARTIST_ID FROM ARTIST");
		List<Object[]> artists = context.select(q3);
		assertEquals(2, artists.size());
		assertEquals(1, artists.get(0).length);
		assertTrue(artists.get(0) instanceof Object[]);

        // TODO: JDBC's BIGINT matches Oracle's NUMERIC, which matches BigDecimal.
        Class<?> idType = testDbAdapter instanceof OracleTestDbAdapter ? BigDecimal.class : Long.class;
        assertInstanceOf(idType, artists.get(0)[0]);
	}

	@Test
	public void testObjectArrayReturnWithDefaultTypes() throws SQLException {
		createArtistDataSet();

		SQLQuery<Object[]> q3 = SQLQuery.columnQuery("SELECT ARTIST_ID, ARTIST_NAME FROM ARTIST");
		List<Object[]> artists = context.select(q3);
		assertEquals(2, artists.size());
		assertEquals(2, artists.get(0).length);
		assertTrue(artists.get(0) instanceof Object[]);

        // JDBC's BIGINT matches Oracle's NUMERIC, which matches BigDecimal.
        Class<?> idType = testDbAdapter instanceof OracleTestDbAdapter ? BigDecimal.class : Long.class;
		assertInstanceOf(idType, artists.get(0)[0]);
	}

	@Test
	public void testObjectArrayReturn() throws SQLException {
		createArtistDataSet();

		SQLQuery<Object[]> q5 = SQLQuery.columnQuery("SELECT * FROM ARTIST");
		q5.resultColumnsTypes(Float.class, String.class, LocalDateTime.class);
		List dates = context.select(q5);
		assertEquals(2, dates.size());
		assertTrue(dates.get(0) instanceof Object[]);
		assertEquals(3, ((Object[])dates.get(0)).length);
		assertTrue(((Object[])dates.get(0))[2] instanceof LocalDateTime);
	}

	@Test
	public void testSingleObjectReturn() throws SQLException {
		tArtistCt.insert(1, "Test", new Date(System.currentTimeMillis()));

		SQLQuery<Object[]> q5 = SQLQuery.columnQuery("SELECT ARTIST_NAME FROM ARTIST_CT");
		q5.resultColumnsTypes(String.class);
		List dates = context.select(q5);
		assertEquals(1, dates.size());
		assertTrue(dates.get(0) instanceof String);
		assertEquals("Test", dates.get(0));
	}

	@Test
	public void testSQLTemplate_PositionalParams() throws SQLException {

		String sql = "INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ESTIMATED_PRICE) "
				+ "VALUES ($b, '$n', #bind($c 'INTEGER'))";

		SQLQuery<Painting> q1 = SQLQuery.query(Painting.class, sql);
		q1.paramsArray(76, "The Fiddler", 10005);
		context.execute(q1);

		assertEquals("The Fiddler", tPainting.getString("PAINTING_TITLE"));
		assertEquals(76, tPainting.getInt("PAINTING_ID"));
		assertEquals(10005.d, tPainting.getDouble("ESTIMATED_PRICE"), 0.001);
	}

	@Test
	public void testSQLTemplate_PositionalParams_RepeatingVars() throws SQLException {

		String sql = "INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ESTIMATED_PRICE) "
				+ "VALUES ($b, '$n', #bind($b 'INTEGER'))";

		SQLQuery<Painting> q1 = SQLQuery.query(Painting.class, sql);
		q1.paramsArray(11, "The Fiddler");
		context.execute(q1);

		assertEquals("The Fiddler", tPainting.getString("PAINTING_TITLE"));
		assertEquals(11, tPainting.getInt("PAINTING_ID"));
		assertEquals(11.d, tPainting.getDouble("ESTIMATED_PRICE"), 0.001);
	}

	@Test
	public void sqlTemplate_PositionalParams_ToFewParams() {

		String sql = "INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ESTIMATED_PRICE) "
				+ "VALUES ($b, '$n', #bind($c 'INTEGER'))";

		SQLQuery<Painting> q1 = SQLQuery.query(Painting.class, sql);
		q1.paramsArray(11, "The Fiddler");

		assertThrows(CayenneRuntimeException.class, () -> context.execute(q1));
	}

	@Test
	public void sqlTemplate_PositionalParams_ToManyParams() throws SQLException {

		String sql = "INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ESTIMATED_PRICE) "
				+ "VALUES ($b, '$n', #bind($b 'INTEGER'))";

		SQLQuery<Painting> q1 = SQLQuery.query(Painting.class, sql);
		q1.paramsArray(11, "The Fiddler", 2345, 333);

		assertThrows(CayenneRuntimeException.class, () -> context.execute(q1),
				"Exception not thrown on parameter length mismatch");
	}

	@Test
	public void testSQLTemplateSelectNullObjects() throws Exception {
		tPainting.insert(1, null, "p1", 10);

		String sql = "SELECT p.GALLERY_ID FROM PAINTING p";
		SQLQuery<Gallery> q1 = SQLQuery.query(Gallery.class, sql);
		q1.columnNameCaps(CapsStrategy.UPPER);
		List<Gallery> galleries = context.select(q1);

		assertEquals(1, galleries.size());
		assertNull(galleries.get(0));
	}

	@Test
	public void sqlTemplateSelectInvalid() throws Exception {
		tPainting.insert(1, null, "p1", 10);

		assertThrows(CayenneRuntimeException.class, () -> {
			String sql = "SELECT p.PAINTING_TITLE FROM PAINTING p";
			SQLQuery<Gallery> q1 = SQLQuery.query(Gallery.class, sql);
			q1.columnNameCaps(CapsStrategy.UPPER);

			// this should fail as result can't be converted to Gallery class
			context.select(q1);
		});
	}

	@Test
	public void testSQLTemplateWithDisjointByIdPrefetch() throws Exception {
		tArtist.insert(1, "artist1", null);
		tArtist.insert(2, "artist2", null);

		tPainting.insert(1, 1, "p1", 10);
		tPainting.insert(2, 2, "p2", 20);

		String sql = "SELECT p.* FROM PAINTING p";
		SQLQuery<Painting> q1 = SQLQuery.query(Painting.class, sql);
		q1.addPrefetch(Painting.ARTIST.disjointById());
		q1.columnNameCaps(CapsStrategy.UPPER);

		@SuppressWarnings("unchecked")
		List<Painting> paintings = context.select(q1);

		env.runWithQueriesBlocked(() -> {
			for(Painting painting : paintings) {
				assertEquals(PersistenceState.COMMITTED, painting.getArtist().getPersistenceState());
			}
		});
	}

	@Test
	public void sqlTemplateWithDisjointPrefetch() {
		assertThrows(CayenneRuntimeException.class, () -> {
			String sql = "SELECT p.* FROM PAINTING p";
			SQLQuery<Painting> q1 = SQLQuery.query(Painting.class, sql);
			q1.addPrefetch(Painting.ARTIST.disjoint());
		});
	}
}
