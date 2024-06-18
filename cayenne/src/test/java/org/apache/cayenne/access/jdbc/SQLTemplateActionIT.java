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

package org.apache.cayenne.access.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.MockOperationObserver;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.RuntimeCaseDataSourceFactory;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.apache.cayenne.unit.util.SQLTemplateCustomizer;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("deprecation")
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class SQLTemplateActionIT extends RuntimeCase {

	@Inject
	protected RuntimeCaseDataSourceFactory dataSourceFactory;

	@Inject
	protected DataNode node;

	@Inject
	protected JdbcAdapter adapter;

	@Inject
	protected UnitDbAdapter unitDbAdapter;

	@Inject
	protected ObjectContext objectContext;

	@Inject
	protected DBHelper dbHelper;

	@Inject
	protected SQLTemplateCustomizer sqlTemplateCustomizer;

	protected TableHelper tArtist;

	@Before
	public void setUp() throws Exception {
		tArtist = new TableHelper(dbHelper, "ARTIST");
		tArtist.setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");
	}

	protected void createFourArtists() throws Exception {

		Date date = new Date(System.currentTimeMillis());

		tArtist.insert(11, "artist2", date);
		tArtist.insert(101, "artist3", date);
		tArtist.insert(201, "artist4", date);
		tArtist.insert(3001, "artist5", date);
	}

	@Test
	public void testProperties() {
		SQLTemplate template = new SQLTemplate(Object.class, "AAAAA");

		SQLTemplateAction action = new SQLTemplateAction(template, node);
		assertSame(template, action.getQuery());
		assertSame(node, action.dataNode);
	}

	@Test
	public void testExecuteSelect() throws Exception {
		createFourArtists();

		String templateString = "SELECT * FROM ARTIST WHERE ARTIST_ID = #bind($id)";
		SQLTemplate template = new SQLTemplate(Object.class, templateString);
		sqlTemplateCustomizer.updateSQLTemplate(template);

		Map<String, Object> bindings = new HashMap<>();
		bindings.put("id", 201L);
		template.setParameters(bindings);

		// must ensure the right SQLTemplateAction is created

		SQLAction plan = adapter.getAction(template, node);
		assertTrue(plan instanceof SQLTemplateAction);

		MockOperationObserver observer = new MockOperationObserver();

		try (Connection c = dataSourceFactory.getSharedDataSource().getConnection();) {
			plan.performAction(c, observer);
		}

		List<DataRow> rows = observer.rowsForQuery(template);
		assertNotNull(rows);
		assertEquals(1, rows.size());
		DataRow row = rows.get(0);

		// In the absence of ObjEntity most DB's return a Long here, except for
		// Oracle
		// that has no BIGINT type and
		// returns BigDecimal, so do a Number comparison
		Number id = (Number) row.get("ARTIST_ID");
		assertNotNull(id);
		assertEquals(((Number) bindings.get("id")).longValue(), id.longValue());
		assertEquals("artist4", row.get("ARTIST_NAME"));
		assertTrue(row.containsKey("DATE_OF_BIRTH"));
	}

	@Test
	public void selectObjects() throws Exception {
		createFourArtists();

		String templateString = "SELECT * FROM ARTIST";
		SQLTemplate sqlTemplate = new SQLTemplate(Artist.class, templateString);

		if(unitDbAdapter.isLowerCaseNames()) {
			sqlTemplate.setColumnNamesCapitalization(CapsStrategy.UPPER);
		}

		@SuppressWarnings("unchecked")
		List<Artist> artists = (List<Artist>)objectContext.performQuery(sqlTemplate);

		assertEquals(4, artists.size());
		for(Artist artist : artists){
			assertTrue(artist.getArtistName().startsWith("artist"));
		}
	}

	@Test
	public void testSelectUtilDate() throws Exception {
		createFourArtists();

		String templateString = "SELECT #result('DATE_OF_BIRTH' 'java.util.Date' 'DOB') "
				+ "FROM ARTIST WHERE ARTIST_ID = #bind($id)";
		SQLTemplate template = new SQLTemplate(Object.class, templateString);
		sqlTemplateCustomizer.updateSQLTemplate(template);

		Map<String, Object> bindings = new HashMap<>();
		bindings.put("id", 101);
		template.setParameters(bindings);

		SQLAction plan = adapter.getAction(template, node);

		MockOperationObserver observer = new MockOperationObserver();

		try (Connection c = dataSourceFactory.getSharedDataSource().getConnection();) {
			plan.performAction(c, observer);
		}

		List<DataRow> rows = observer.rowsForQuery(template);
		assertNotNull(rows);
		assertEquals(1, rows.size());
		DataRow row = rows.get(0);

		assertNotNull(row.get("DOB"));
		assertEquals(java.util.Date.class, row.get("DOB").getClass());
	}

	@Test
	public void testSelectSQLDate() throws Exception {
		createFourArtists();

		String templateString = "SELECT #result('DATE_OF_BIRTH' 'java.sql.Date' 'DOB') "
				+ "FROM ARTIST WHERE ARTIST_ID = #bind($id)";
		SQLTemplate template = new SQLTemplate(Object.class, templateString);
		sqlTemplateCustomizer.updateSQLTemplate(template);

		Map<String, Object> bindings = new HashMap<>();
		bindings.put("id", 101);
		template.setParameters(bindings);

		SQLAction plan = adapter.getAction(template, node);

		MockOperationObserver observer = new MockOperationObserver();

		try (Connection c = dataSourceFactory.getSharedDataSource().getConnection();) {
			plan.performAction(c, observer);
		}

		List<DataRow> rows = observer.rowsForQuery(template);
		assertNotNull(rows);
		assertEquals(1, rows.size());
		DataRow row = rows.get(0);

		assertNotNull(row.get("DOB"));
		assertEquals(java.sql.Date.class, row.get("DOB").getClass());
	}

	@Test
	public void testSelectSQLTimestamp() throws Exception {
		createFourArtists();

		String templateString = "SELECT #result('DATE_OF_BIRTH' 'java.sql.Timestamp' 'DOB') "
				+ "FROM ARTIST WHERE ARTIST_ID = #bind($id)";
		SQLTemplate template = new SQLTemplate(Object.class, templateString);
		sqlTemplateCustomizer.updateSQLTemplate(template);

		Map<String, Object> bindings = new HashMap<>();
		bindings.put("id", 201);
		template.setParameters(bindings);

		SQLAction plan = adapter.getAction(template, node);

		MockOperationObserver observer = new MockOperationObserver();

		try (Connection c = dataSourceFactory.getSharedDataSource().getConnection();) {
			plan.performAction(c, observer);
		}

		List<DataRow> rows = observer.rowsForQuery(template);
		assertNotNull(rows);
		assertEquals(1, rows.size());
		DataRow row = rows.get(0);

		assertNotNull(row.get("DOB"));
		// Sybase returns a Timestamp subclass... so can't test equality
		assertTrue(java.sql.Timestamp.class.isAssignableFrom(row.get("DOB").getClass()));
	}

	@Test
	public void testExecuteUpdate() throws Exception {
		String templateString = "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) "
				+ "VALUES (#bind($id), #bind($name), #bind($dob 'DATE'))";
		SQLTemplate template = new SQLTemplate(Object.class, templateString);

		Map<String, Object> bindings = new HashMap<>();
		bindings.put("id", 1L);
		bindings.put("name", "a1");
		bindings.put("dob", new Date(System.currentTimeMillis()));
		template.setParameters(bindings);

		SQLAction action = adapter.getAction(template, node);

		try (Connection c = dataSourceFactory.getSharedDataSource().getConnection();) {
			MockOperationObserver observer = new MockOperationObserver();
			action.performAction(c, observer);

			int[] batches = observer.countsForQuery(template);
			assertNotNull(batches);
			assertEquals(1, batches.length);
			assertEquals(1, batches[0]);
		}
		assertEquals(1, tArtist.getRowCount());
		assertEquals(1L, tArtist.getLong("ARTIST_ID"));
		assertEquals("a1", tArtist.getString("ARTIST_NAME").trim());
	}

	@Test
	public void testExecuteUpdateNoParameters() throws Exception {
		createFourArtists();

		SQLTemplate template = new SQLTemplate(Object.class, "delete from ARTIST where ARTIST_NAME like 'a%'");

		SQLAction action = adapter.getAction(template, node);

		try (Connection c = dataSourceFactory.getSharedDataSource().getConnection();) {
			MockOperationObserver observer = new MockOperationObserver();
			action.performAction(c, observer);

			int[] batches = observer.countsForQuery(template);
			assertNotNull(batches);
			assertEquals(1, batches.length);
			assertEquals(4, batches[0]);
		}
	}

	@Test
	public void testExecuteUpdateBatch() throws Exception {
		String templateString = "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) "
				+ "VALUES (#bind($id), #bind($name), #bind($dob 'DATE'))";
		SQLTemplate template = new SQLTemplate(Object.class, templateString);

		Map<String, Object> bindings1 = new HashMap<>();
		bindings1.put("id", 1L);
		bindings1.put("name", "a1");
		bindings1.put("dob", new Date(System.currentTimeMillis()));

		Map<String, Object> bindings2 = new HashMap<>();
		bindings2.put("id", 33L);
		bindings2.put("name", "a$$$$$");
		bindings2.put("dob", new Date(System.currentTimeMillis()));
		template.setParameters(new Map[] { bindings1, bindings2 });

		SQLAction genericAction = adapter.getAction(template, node);
		assertTrue(genericAction instanceof SQLTemplateAction);
		SQLTemplateAction action = (SQLTemplateAction) genericAction;

		assertSame(node, action.dataNode);
		assertSame(template, action.getQuery());

		try (Connection c = dataSourceFactory.getSharedDataSource().getConnection();) {
			MockOperationObserver observer = new MockOperationObserver();
			action.performAction(c, observer);

			int[] batches = observer.countsForQuery(template);
			assertNotNull(batches);
			assertEquals(2, batches.length);
			assertEquals(1, batches[0]);
			assertEquals(1, batches[1]);
		}

		MockOperationObserver observer = new MockOperationObserver();
		ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
				.orderBy("db:ARTIST_ID", SortOrder.ASCENDING);
		node.performQueries(Collections.singletonList(query), observer);

		@SuppressWarnings("unchecked")
		List<DataRow> data = observer.rowsForQuery(query);
		assertEquals(2, data.size());
		DataRow row1 = data.get(0);
		assertEquals(bindings1.get("id"), row1.get("ARTIST_ID"));
		assertEquals(bindings1.get("name"), row1.get("ARTIST_NAME"));
		// to compare dates we need to create the binding correctly
		// assertEquals(bindings1.get("dob"), row.get("DATE_OF_BIRTH"));

		DataRow row2 = data.get(1);
		assertEquals(bindings2.get("id"), row2.get("ARTIST_ID"));
		assertEquals(bindings2.get("name"), row2.get("ARTIST_NAME"));
		// to compare dates we need to create the binding correctly
		// assertEquals(bindings2.get("dob"), row2.get("DATE_OF_BIRTH"));
	}

	@Test
	public void testExtractTemplateString() {
		SQLTemplate template = new SQLTemplate(Artist.class, "A\nBC");
		SQLTemplateAction action = new SQLTemplateAction(template, node);

		assertEquals("A BC", action.extractTemplateString());
	}

}
