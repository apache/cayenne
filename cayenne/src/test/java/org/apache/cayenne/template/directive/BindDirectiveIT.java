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
package org.apache.cayenne.template.directive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.MockOperationObserver;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.oracle.OracleAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

/**
 * Tests BindDirective for passed null parameters and for not passed parameters
 */
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class BindDirectiveIT extends RuntimeCase {

	private static String INSERT_TEMPLATE = "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) "
			+ "VALUES (#bind($id), #bind($name), #bind($dob))";
	private static String INSERT_TEMPLATE_WITH_TYPES = "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) "
			+ "VALUES (#bind($id), #bind($name), #bind($dob 'DATE'))";

	@Inject
	private JdbcAdapter adapter;

	@Inject
	private ObjectContext context;

	@Inject
	private JdbcEventLogger logger;

	@Inject
	private DataNode node;

	@Inject
	private DBHelper dbHelper;

	@Test
	public void testBind_Timestamp() throws Exception {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("id", 1);
		parameters.put("name", "ArtistWithDOB");
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(2010, 2, 8);
		parameters.put("dob", new Timestamp(cal.getTime().getTime()));

		// without JDBC usage
		Map<String, ?> row = performInsertForParameters(parameters, INSERT_TEMPLATE);
		assertEquals(parameters.get("name"), row.get("ARTIST_NAME"));
		assertEquals(cal.getTime(), row.get("DATE_OF_BIRTH"));
		assertNotNull(row.get("DATE_OF_BIRTH"));
		assertEquals(Date.class, row.get("DATE_OF_BIRTH").getClass());
	}

	@Test
	public void testBind_SQLDate() throws Exception {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("id", 1);
		parameters.put("name", "ArtistWithDOB");
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(2010, 2, 8);
		parameters.put("dob", new java.sql.Date(cal.getTime().getTime()));

		// without JDBC usage
		Map<String, ?> row = performInsertForParameters(parameters, INSERT_TEMPLATE);
		assertEquals(parameters.get("name"), row.get("ARTIST_NAME"));
		assertEquals(parameters.get("dob"), row.get("DATE_OF_BIRTH"));
		assertNotNull(row.get("DATE_OF_BIRTH"));
		assertEquals(Date.class, row.get("DATE_OF_BIRTH").getClass());
	}

	@Test
	public void testBind_UtilDate() throws Exception {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("id", 1);
		parameters.put("name", "ArtistWithDOB");
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(2010, 2, 8);
		parameters.put("dob", cal.getTime());

		// without JDBC usage
		Map<String, ?> row = performInsertForParameters(parameters, INSERT_TEMPLATE);
		assertEquals(parameters.get("name"), row.get("ARTIST_NAME"));
		assertEquals(parameters.get("dob"), row.get("DATE_OF_BIRTH"));
		assertNotNull(row.get("DATE_OF_BIRTH"));
		assertEquals(Date.class, row.get("DATE_OF_BIRTH").getClass());
	}

	@Test
	public void testBind_Collection() throws Exception {

		TableHelper tArtist = new TableHelper(dbHelper, "ARTIST").setColumns("ARTIST_ID", "ARTIST_NAME");

		// insert 3 artists
		for (int i = 1; i < 4; i++) {
			tArtist.insert((long) i, "Artist" + i);
		}

		// now select only with names: Artist1 and Artist3
		Set<String> artistNames = new HashSet<String>();
		artistNames.add("Artist1");
		artistNames.add("Artist3");
		String sql = "SELECT * FROM ARTIST WHERE ARTIST_NAME in (#bind($ARTISTNAMES))";
		SQLTemplate query = new SQLTemplate(Artist.class, sql);

		// customize for DB's that require trimming CHAR spaces
		query.setTemplate(OracleAdapter.class.getName(),
				"SELECT * FROM ARTIST WHERE RTRIM(ARTIST_NAME) in (#bind($ARTISTNAMES))");

		query.setColumnNamesCapitalization(CapsStrategy.UPPER);
		query.setParams(Collections.singletonMap("ARTISTNAMES", artistNames));
		List<?> result = context.performQuery(query);
		assertEquals(2, result.size());
	}

	@Test
	public void testBind_NullParam() throws Exception {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("id", 1L);
		parameters.put("name", "ArtistWithoutDOB");
		// passing null in parameter
		parameters.put("dob", null);

		// without JDBC usage
		Map<String, ?> row = performInsertForParameters(parameters, INSERT_TEMPLATE);
		assertEquals(parameters.get("id"), row.get("ARTIST_ID"));
		assertEquals(parameters.get("name"), row.get("ARTIST_NAME"));
		assertEquals(parameters.get("dob"), row.get("DATE_OF_BIRTH"));
		assertNull(row.get("DATE_OF_BIRTH"));
	}

	@Test
	public void testBind_NullParam_JDBCTypes() throws Exception {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("id", 1L);
		parameters.put("name", "ArtistWithoutDOB");
		// passing null in parameter
		parameters.put("dob", null);

		// use JDBC
		Map<String, ?> row = performInsertForParameters(parameters, INSERT_TEMPLATE_WITH_TYPES);
		assertEquals(parameters.get("id"), row.get("ARTIST_ID"));
		assertEquals(parameters.get("name"), row.get("ARTIST_NAME"));
		assertEquals(parameters.get("dob"), row.get("DATE_OF_BIRTH"));
		assertNull(row.get("DATE_OF_BIRTH"));
	}

	@Test
	public void testBind_SkippedParam() throws Exception {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("id", 1L);
		parameters.put("name", "ArtistWithoutDOB");
		// skipping "dob"

		// without JDBC usage
		Map<String, ?> row = performInsertForParameters(parameters, INSERT_TEMPLATE);
		assertEquals(parameters.get("id"), row.get("ARTIST_ID"));
		assertEquals(parameters.get("name"), row.get("ARTIST_NAME"));
		// parameter should be passed as null
		assertNull(row.get("DATE_OF_BIRTH"));
	}

	@Test
	public void testBind_SkippedParam_JDBCTypes() throws Exception {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("id", 1L);
		parameters.put("name", "ArtistWithoutDOB");
		// skipping "dob"

		// use JDBC
		Map<String, ?> row = performInsertForParameters(parameters, INSERT_TEMPLATE_WITH_TYPES);
		assertEquals(parameters.get("id"), row.get("ARTIST_ID"));
		assertEquals(parameters.get("name"), row.get("ARTIST_NAME"));
		// parameter should be passed as null
		assertNull(row.get("DATE_OF_BIRTH"));
	}

	/**
	 * Inserts row for given parameters
	 * 
	 * @return inserted row
	 */
	private Map<String, ?> performInsertForParameters(Map<String, Object> parameters, String templateString)
			throws Exception {

		// TODO: do we really care if an inserting SQLTemplate is executed via
		// ObjectContext?
		SQLTemplate template = new SQLTemplate(Object.class, templateString);
		template.setParams(parameters);
		MockOperationObserver observer = new MockOperationObserver();
		node.performQueries(Collections.singletonList(template), observer);

		return ObjectSelect.dataRowQuery(Artist.class).selectOne(context);
	}
}
