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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class SQLTemplateIT extends ServerCase {

	@Inject
	private DataContext context;

	@Inject
	private DBHelper dbHelper;

	private TableHelper tPainting;

	@Before
	public void setUp() throws Exception {
		tPainting = new TableHelper(dbHelper, "PAINTING");
		tPainting.setColumns("PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE", "ESTIMATED_PRICE").setColumnTypes(
				Types.INTEGER, Types.BIGINT, Types.VARCHAR, Types.DECIMAL);
	}

	@Test
	public void testSQLTemplateForDataMap() {
		DataMap testDataMap = context.getEntityResolver().getDataMap("testmap");
		SQLTemplate q1 = new SQLTemplate(testDataMap, "SELECT * FROM ARTIST", true);
		List<DataRow> result = context.performQuery(q1);
		assertEquals(0, result.size());
	}

	@Test
	public void testSQLTemplateForDataMapWithInsert() {
		DataMap testDataMap = context.getEntityResolver().getDataMap("testmap");
		String sql = "INSERT INTO ARTIST VALUES (15, 'Surikov', null)";
		SQLTemplate q1 = new SQLTemplate(testDataMap, sql, true);
		context.performNonSelectingQuery(q1);

		SQLTemplate q2 = new SQLTemplate(testDataMap, "SELECT * FROM ARTIST", true);
		List<DataRow> result = context.performQuery(q2);
		assertEquals(1, result.size());
	}

	@Test
	public void testSQLTemplateForDataMapWithInsertException() {
		DataMap testDataMap = context.getEntityResolver().getDataMap("testmap");
		String sql = "INSERT INTO ARTIST VALUES (15, 'Surikov', null)";
		SQLTemplate q1 = new SQLTemplate(testDataMap, sql, true);
		context.performNonSelectingQuery(q1);

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
	public void testSQLTemplate_PositionalParams_ToFewParams() throws SQLException {

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
}
