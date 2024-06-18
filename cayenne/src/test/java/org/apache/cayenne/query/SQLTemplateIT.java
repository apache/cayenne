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
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.SQLResult;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.OracleUnitDbAdapter;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class SQLTemplateIT extends RuntimeCase {

	@Inject
	private DataContext context;

	@Inject
	private DBHelper dbHelper;

	@Inject
	protected DataChannelInterceptor queryInterceptor;

	@Inject
	protected UnitDbAdapter unitDbAdapter;

	private TableHelper tPainting;

	private TableHelper tArtist;

	private TableHelper tArtistCt;

	@Before
	public void setUp() throws Exception {
		tArtist = new TableHelper(dbHelper, "ARTIST");
		tArtist.setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");

		tPainting = new TableHelper(dbHelper, "PAINTING");
		tPainting.setColumns("PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE", "ESTIMATED_PRICE");

		tArtistCt = new TableHelper(dbHelper, "ARTIST_CT");
		tArtistCt.setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");
	}

	private void createArtistDataSet() throws SQLException {
		tArtist.insert(15, "Surikov", new Date(System.currentTimeMillis()));
		tArtist.insert(16, "Ivanov", new Date(System.currentTimeMillis()));
	}

	@Test
	public void testSQLTemplateForDataMap() {
		DataMap testDataMap = context.getEntityResolver().getDataMap("testmap");
		SQLTemplate q1 = new SQLTemplate(testDataMap, "SELECT * FROM ARTIST", true);
		List<DataRow> result = context.performQuery(q1);
		assertEquals(0, result.size());
	}

	@Test
	public void testSQLTemplateForDataMapWithInsert() throws SQLException {
		createArtistDataSet();

		DataMap testDataMap = context.getEntityResolver().getDataMap("testmap");
		SQLTemplate q2 = new SQLTemplate(testDataMap, "SELECT * FROM ARTIST", true);
		List<DataRow> result = context.performQuery(q2);
		assertEquals(2, result.size());
	}

	@Test
	public void testReturnGeneratedKeys() {
		if(unitDbAdapter.supportsGeneratedKeys()) {
			DataMap testDataMap = context.getEntityResolver().getDataMap("testmap");
			String sql = "INSERT INTO GENERATED_COLUMN (NAME) VALUES ('Surikov')";
			SQLTemplate q1 = new SQLTemplate(testDataMap, sql, true);
			q1.setReturnGeneratedKeys(true);
			List<DataRow> response = context.performQuery(q1);
			assertEquals(1, response.size());

			String sql1 = "INSERT INTO GENERATED_COLUMN (NAME) VALUES ('Test')";
			SQLTemplate q2 = new SQLTemplate(testDataMap, sql1, true);
			q2.setReturnGeneratedKeys(false);
			List<DataRow> response1 = context.performQuery(q2);
			assertEquals(0, response1.size());
		}
	}

	@Test
	public void testSQLTemplateForDataMapWithInsertException() throws SQLException {
		createArtistDataSet();

		DataMap testDataMap = context.getEntityResolver().getDataMap("testmap");
		SQLTemplate q2 = new SQLTemplate(testDataMap, "SELECT * FROM ARTIST", false);
		boolean gotRuntimeException = false;
		try {
			context.performQuery(q2);
		} catch (CayenneRuntimeException e) {
			gotRuntimeException = true;
		}
		assertTrue("If fetchingDataRows is false and ObjectEntity not set, should be thrown exception",
				gotRuntimeException);
	}

	@Test(expected = CayenneRuntimeException.class)
	public void testExceptionWhenUsingColumnsTypesAndSQLResult() throws SQLException {
		createArtistDataSet();

		SQLTemplate query = new SQLTemplate("SELECT ARTIST_ID P FROM ARTIST", true);
		query.setResultColumnsTypes(Float.class);
		SQLResult resultDescriptor = new SQLResult();
		resultDescriptor.addColumnResult("P");
		query.setResult(resultDescriptor);
		context.performQuery(query);
	}

	@Test(expected = CayenneRuntimeException.class)
	public void testExceptionWhenUsingColumnsTypesAndSQLResultUsingScalar() throws SQLException {
		createArtistDataSet();

		DataMap testDataMap = context.getEntityResolver().getDataMap("testmap");
		SQLTemplate query = new SQLTemplate(testDataMap, "SELECT ARTIST_ID, ARTIST_NAME P FROM ARTIST", false);
		query.setResultColumnsTypes(Float.class, String.class);
		query.setUseScalar(true);
		SQLResult resultDescriptor = new SQLResult();
		resultDescriptor.addColumnResult("P");
		resultDescriptor.addColumnResult("N");
		query.setResult(resultDescriptor);
		context.performQuery(query);
	}

	@Test
	public void testWithRootUsingScalar() throws SQLException {
		createArtistDataSet();

		SQLTemplate q3 = new SQLTemplate(Artist.class, "SELECT ARTIST_ID, ARTIST_NAME FROM ARTIST");
		q3.setResultColumnsTypes(Double.class, String.class);
		q3.setUseScalar(true);
		List<Object[]> result = context.performQuery(q3);
		assertEquals(2, result.size());
		assertTrue(result.get(0) instanceof Object[]);
		assertTrue(result.get(0)[0] instanceof Double);
	}

	@Test
	public void testWithRootUsingDataRow() throws SQLException {
		createArtistDataSet();

		SQLTemplate q3 = new SQLTemplate(Artist.class, "SELECT ARTIST_ID, ARTIST_NAME FROM ARTIST");
		q3.setResultColumnsTypes(Double.class, String.class);
		q3.setFetchingDataRows(true);
		q3.setColumnNamesCapitalization(CapsStrategy.UPPER);
		List<DataRow> result = context.performQuery(q3);
		assertEquals(2, result.size());
		assertTrue(result.get(0) instanceof DataRow);
		assertThat(result.get(0).get("ARTIST_ID"), instanceOf(Double.class));
	}

	@Test(expected = CayenneRuntimeException.class)
	public void testWithRootException() throws SQLException {
		createArtistDataSet();

		SQLTemplate q3 = new SQLTemplate(Artist.class, "SELECT ARTIST_ID, ARTIST_NAME FROM ARTIST");
		q3.setResultColumnsTypes(Double.class, String.class);
		context.performQuery(q3);
	}

	@Test(expected = CayenneRuntimeException.class)
	public void testUsingScalarAndDataRow() throws SQLException {
		createArtistDataSet();

		SQLTemplate q3 = new SQLTemplate(Artist.class, "SELECT ARTIST_ID, ARTIST_NAME FROM ARTIST");
		q3.setUseScalar(true);
		q3.setFetchingDataRows(true);
		context.performQuery(q3);
	}

	@Test
	public void testDataRowWithTypes() throws SQLException {
		createArtistDataSet();

		DataMap testDataMap = context.getEntityResolver().getDataMap("testmap");
		SQLTemplate q3 = new SQLTemplate(testDataMap, "SELECT ARTIST_ID, ARTIST_NAME FROM ARTIST", true);
		q3.setResultColumnsTypes(Double.class, String.class);
		q3.setColumnNamesCapitalization(CapsStrategy.UPPER);
		List<DataRow> artists = context.performQuery(q3);
		assertEquals(2, artists.size());
		assertTrue(artists.get(0) instanceof DataRow);
		assertThat(artists.get(0).get("ARTIST_ID"), instanceOf(Double.class));
	}

	@Test
	public void testDataRowReturnAndDirectives() throws SQLException {
		createArtistDataSet();

		DataMap testDataMap = context.getEntityResolver().getDataMap("testmap");
		SQLTemplate q3 = new SQLTemplate(testDataMap, "SELECT #result('ARTIST_ID' 'java.lang.Long'), #result('ARTIST_NAME' 'java.lang.String') FROM ARTIST", true);
		List<DataRow> result = context.performQuery(q3);
		assertEquals(2, result.size());
		assertTrue(result.get(0) instanceof DataRow);
		assertEquals(2, result.get(0).size());
		assertTrue(result.get(0).get("ARTIST_ID") instanceof Long);
		assertTrue(result.get(0).get("ARTIST_NAME") instanceof String);
	}

	@Test(expected = CayenneRuntimeException.class)
	public void testDataRowReturnAndDirectivesExc() throws SQLException {
		createArtistDataSet();

		DataMap testDataMap = context.getEntityResolver().getDataMap("testmap");
		SQLTemplate q3 = new SQLTemplate(testDataMap, "SELECT #result('ARTIST_ID' 'java.lang.Long'), #result('ARTIST_NAME' 'java.lang.String') FROM ARTIST", true);
		q3.setResultColumnsTypes(Integer.class, String.class);
		context.performQuery(q3);
	}

	@Test
	public void testObjectArrayReturnAndDirectives() throws SQLException {
		createArtistDataSet();

		DataMap testDataMap = context.getEntityResolver().getDataMap("testmap");
		SQLTemplate q3 = new SQLTemplate(testDataMap, "SELECT #result('ARTIST_ID' 'java.lang.Long'), #result('ARTIST_NAME' 'java.lang.String') FROM ARTIST", false);
		q3.setUseScalar(true);
		List<Object[]> result = context.performQuery(q3);
		assertEquals(2, result.size());
		assertTrue(result.get(0) instanceof Object[]);
		assertEquals(2, result.get(0).length);
		assertTrue(result.get(0)[0] instanceof Long);
		assertTrue(result.get(0)[1] instanceof String);
	}

	@Test(expected = CayenneRuntimeException.class)
	public void testObjectArrayReturnAndDirectivesException() throws SQLException {
		createArtistDataSet();

		DataMap testDataMap = context.getEntityResolver().getDataMap("testmap");
		SQLTemplate q3 = new SQLTemplate(testDataMap, "SELECT #result('ARTIST_ID' 'java.lang.Long'), #result('ARTIST_NAME' 'java.lang.String') FROM ARTIST", false);
		q3.setResultColumnsTypes(Integer.class, String.class);
		q3.setUseScalar(true);
		context.performQuery(q3);
	}

	@Test
	public void testObjectArrayWithSingleObjectReturnAndDirectives() throws SQLException {
		createArtistDataSet();

		DataMap testDataMap = context.getEntityResolver().getDataMap("testmap");
		SQLTemplate q3 = new SQLTemplate(testDataMap, "SELECT #result('ARTIST_ID' 'java.lang.Long') FROM ARTIST", false);
		q3.setUseScalar(true);
		List<Object[]> result = context.performQuery(q3);
		assertEquals(2, result.size());
		assertTrue(result.get(0) instanceof Object[]);
		assertEquals(1, result.get(0).length);
		assertTrue(result.get(0)[0] instanceof Long);
	}

	@Test(expected = CayenneRuntimeException.class)
	public void testObjectArrayReturnWithException() throws SQLException {
		createArtistDataSet();

		DataMap testDataMap = context.getEntityResolver().getDataMap("testmap");
		SQLTemplate q3 = new SQLTemplate(testDataMap, "SELECT ARTIST_ID, ARTIST_NAME FROM ARTIST", false);
		q3.setResultColumnsTypes(Integer.class);
		q3.setUseScalar(true);
		context.performQuery(q3);
	}

	@Test
	public void testObjectArrayWithSingleObjectReturn() throws SQLException {
		createArtistDataSet();

		DataMap testDataMap = context.getEntityResolver().getDataMap("testmap");
		SQLTemplate q3 = new SQLTemplate(testDataMap, "SELECT ARTIST_ID FROM ARTIST", false);
		q3.setUseScalar(true);
		List<Object[]> artists = context.performQuery(q3);
		assertEquals(2, artists.size());
		assertEquals(1, artists.get(0).length);
		assertTrue(artists.get(0) instanceof Object[]);

        // TODO: JDBC's BIGINT matches Oracle's NUMERIC, which matches BigDecimal.
        Class<?> idType = unitDbAdapter instanceof OracleUnitDbAdapter ? BigDecimal.class : Long.class;
        assertThat(artists.get(0)[0], instanceOf(idType));
	}

	@Test
	public void testObjectArrayReturnWithDefaultTypes() throws SQLException {
		createArtistDataSet();

		DataMap testDataMap = context.getEntityResolver().getDataMap("testmap");
		SQLTemplate q3 = new SQLTemplate(testDataMap, "SELECT ARTIST_ID, ARTIST_NAME FROM ARTIST", false);
		q3.setUseScalar(true);
		List<Object[]> artists = context.performQuery(q3);
		assertEquals(2, artists.size());
		assertEquals(2, artists.get(0).length);
		assertTrue(artists.get(0) instanceof Object[]);

        // JDBC's BIGINT matches Oracle's NUMERIC, which matches BigDecimal.
        Class<?> idType = unitDbAdapter instanceof OracleUnitDbAdapter ? BigDecimal.class : Long.class;
		assertThat(artists.get(0)[0], instanceOf(idType));
	}

	@Test
	public void testObjectArrayReturn() throws SQLException {
		createArtistDataSet();

		DataMap testDataMap = context.getEntityResolver().getDataMap("testmap");
		SQLTemplate q5 = new SQLTemplate(testDataMap, "SELECT * FROM ARTIST", false);
		q5.setResultColumnsTypes(Float.class, String.class, LocalDateTime.class);
		q5.setUseScalar(true);
		List dates = context.performQuery(q5);
		assertEquals(2, dates.size());
		assertTrue(dates.get(0) instanceof Object[]);
		assertEquals(3, ((Object[])dates.get(0)).length);
		assertTrue(((Object[])dates.get(0))[2] instanceof LocalDateTime);
	}

	@Test
	public void testSingleObjectReturn() throws SQLException {
		tArtistCt.insert(1, "Test", new Date(System.currentTimeMillis()));

		DataMap testDataMap = context.getEntityResolver().getDataMap("testmap");
		SQLTemplate q5 = new SQLTemplate(testDataMap, "SELECT ARTIST_NAME FROM ARTIST_CT", false);
		q5.setResultColumnsTypes(String.class);
		q5.setUseScalar(true);
		List dates = context.performQuery(q5);
		assertEquals(1, dates.size());
		assertTrue(dates.get(0) instanceof String);
		assertEquals("Test", dates.get(0));
	}

	@Test
	public void testSQLTemplate_PositionalParams() throws SQLException {

		String sql = "INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ESTIMATED_PRICE) "
				+ "VALUES ($b, '$n', #bind($c 'INTEGER'))";

		SQLTemplate q1 = new SQLTemplate(Painting.class, sql);
		q1.setParamsArray(76, "The Fiddler", 10005);
		context.performNonSelectingQuery(q1);

		assertEquals("The Fiddler", tPainting.getString("PAINTING_TITLE"));
		assertEquals(76, tPainting.getInt("PAINTING_ID"));
		assertEquals(10005.d, tPainting.getDouble("ESTIMATED_PRICE"), 0.001);
	}

	@Test
	public void testSQLTemplate_PositionalParams_RepeatingVars() throws SQLException {

		String sql = "INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ESTIMATED_PRICE) "
				+ "VALUES ($b, '$n', #bind($b 'INTEGER'))";

		SQLTemplate q1 = new SQLTemplate(Painting.class, sql);
		q1.setParamsArray(11, "The Fiddler");
		context.performNonSelectingQuery(q1);

		assertEquals("The Fiddler", tPainting.getString("PAINTING_TITLE"));
		assertEquals(11, tPainting.getInt("PAINTING_ID"));
		assertEquals(11.d, tPainting.getDouble("ESTIMATED_PRICE"), 0.001);
	}

	@Test(expected = CayenneRuntimeException.class)
	public void testSQLTemplate_PositionalParams_ToFewParams() {

		String sql = "INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ESTIMATED_PRICE) "
				+ "VALUES ($b, '$n', #bind($c 'INTEGER'))";

		SQLTemplate q1 = new SQLTemplate(Painting.class, sql);
		q1.setParamsArray(11, "The Fiddler");

		context.performNonSelectingQuery(q1);
	}

	@Test
	public void testSQLTemplate_PositionalParams_ToManyParams() throws SQLException {

		String sql = "INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ESTIMATED_PRICE) "
				+ "VALUES ($b, '$n', #bind($b 'INTEGER'))";

		SQLTemplate q1 = new SQLTemplate(Painting.class, sql);
		q1.setParamsArray(11, "The Fiddler", 2345, 333);

		try {
			context.performNonSelectingQuery(q1);
			fail("Exception not thrown on parameter length mismatch");
		} catch (CayenneRuntimeException e) {
			// expected
		}
	}

	@Test
	public void testSQLTemplateSelectNullObjects() throws Exception {
		tPainting.insert(1, null, "p1", 10);

		String sql = "SELECT p.GALLERY_ID FROM PAINTING p";
		SQLTemplate q1 = new SQLTemplate(Gallery.class, sql);
		q1.setColumnNamesCapitalization(CapsStrategy.UPPER);
		List<Gallery> galleries = context.performQuery(q1);

		assertEquals(1, galleries.size());
		assertNull(galleries.get(0));
	}

	@Test(expected = CayenneRuntimeException.class)
	public void testSQLTemplateSelectInvalid() throws Exception {
		tPainting.insert(1, null, "p1", 10);

		String sql = "SELECT p.PAINTING_TITLE FROM PAINTING p";
		SQLTemplate q1 = new SQLTemplate(Gallery.class, sql);
		q1.setColumnNamesCapitalization(CapsStrategy.UPPER);

		// this should fail as result can't be converted to Gallery class
		context.performQuery(q1);
	}

	@Test
	public void testSQLTemplateWithDisjointByIdPrefetch() throws Exception {
		tArtist.insert(1, "artist1", null);
		tArtist.insert(2, "artist2", null);

		tPainting.insert(1, 1, "p1", 10);
		tPainting.insert(2, 2, "p2", 20);

		String sql = "SELECT p.* FROM PAINTING p";
		SQLTemplate q1 = new SQLTemplate(Painting.class, sql);
		q1.addPrefetch(Painting.TO_ARTIST.disjointById());
		q1.setColumnNamesCapitalization(CapsStrategy.UPPER);

		@SuppressWarnings("unchecked")
		List<Painting> paintings = context.performQuery(q1);

		queryInterceptor.runWithQueriesBlocked(() -> {
			for(Painting painting : paintings) {
				assertEquals(PersistenceState.COMMITTED, painting.getToArtist().getPersistenceState());
			}
		});
	}

	@Test(expected = CayenneRuntimeException.class)
	public void testSQLTemplateWithDisjointPrefetch() {
		String sql = "SELECT p.* FROM PAINTING p";
		SQLTemplate q1 = new SQLTemplate(Painting.class, sql);
		q1.addPrefetch(Painting.TO_ARTIST.disjoint());
	}
}
