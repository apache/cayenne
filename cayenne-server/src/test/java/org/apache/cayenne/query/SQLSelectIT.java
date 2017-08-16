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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.ResultIteratorCallback;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class SQLSelectIT extends ServerCase {

	@Inject
	private DataContext context;

	@Inject
	private DBHelper dbHelper;

	private TableHelper tPainting;

	@Before
	public void before() {

		tPainting = new TableHelper(dbHelper, "PAINTING")
				.setColumns("PAINTING_ID", "PAINTING_TITLE", "ESTIMATED_PRICE").setColumnTypes(Types.INTEGER,
						Types.VARCHAR, Types.DECIMAL);
	}

	protected void createPaintingsDataSet() throws Exception {
		for (int i = 1; i <= 20; i++) {
			tPainting.insert(i, "painting" + i, 10000. * i);
		}
	}

	@Test
	public void test_DataRows_DataMapNameRoot() throws Exception {

		createPaintingsDataSet();

		SQLSelect<DataRow> q1 = SQLSelect.dataRowQuery("testmap", "SELECT * FROM PAINTING");
		assertTrue(q1.isFetchingDataRows());

		List<DataRow> result = context.select(q1);
		assertEquals(20, result.size());
		assertTrue(result.get(0) instanceof DataRow);
	}

	@Test
	public void test_DataRows_DefaultRoot() throws Exception {

		createPaintingsDataSet();

		SQLSelect<DataRow> q1 = SQLSelect.dataRowQuery("SELECT * FROM PAINTING");
		assertTrue(q1.isFetchingDataRows());

		List<DataRow> result = context.select(q1);
		assertEquals(20, result.size());
		assertTrue(result.get(0) instanceof DataRow);
	}

	@Test
	public void test_DataRows_ClassRoot() throws Exception {

		createPaintingsDataSet();

		SQLSelect<Painting> q1 = SQLSelect.query(Painting.class, "SELECT * FROM PAINTING").columnNameCaps(
				CapsStrategy.UPPER);
		assertFalse(q1.isFetchingDataRows());
		List<Painting> result = context.select(q1);
		assertEquals(20, result.size());
		assertTrue(result.get(0) instanceof Painting);
	}

	@Test
	public void test_DataRows_ClassRoot_Parameters() throws Exception {

		createPaintingsDataSet();

		SQLSelect<Painting> q1 = SQLSelect.query(Painting.class,
				"SELECT * FROM PAINTING WHERE PAINTING_TITLE = #bind($a)");
		q1.params("a", "painting3").columnNameCaps(CapsStrategy.UPPER);

		assertFalse(q1.isFetchingDataRows());
		Painting a = context.selectOne(q1);
		assertEquals("painting3", a.getPaintingTitle());
	}

	@Test
	public void test_DataRows_ClassRoot_Bind() throws Exception {

		createPaintingsDataSet();

		SQLSelect<Painting> q1 = SQLSelect.query(Painting.class,
				"SELECT * FROM PAINTING WHERE PAINTING_TITLE = #bind($a) OR PAINTING_TITLE = #bind($b)")
				.columnNameCaps(CapsStrategy.UPPER);
		q1.params("a", "painting3").params("b", "painting4");

		List<Painting> result = context.select(q1);
		assertEquals(2, result.size());
	}

	@Test
	public void test_DataRows_ColumnNameCaps() throws Exception {

		SQLSelect<DataRow> q1 = SQLSelect.dataRowQuery("SELECT * FROM PAINTING WHERE PAINTING_TITLE = 'painting2'");
		q1.upperColumnNames();

		SQLTemplate r1 = (SQLTemplate) q1.getReplacementQuery(context.getEntityResolver());
		assertEquals(CapsStrategy.UPPER, r1.getColumnNamesCapitalization());

		q1.lowerColumnNames();
		SQLTemplate r2 = (SQLTemplate) q1.getReplacementQuery(context.getEntityResolver());
		assertEquals(CapsStrategy.LOWER, r2.getColumnNamesCapitalization());
	}

	@Test
	public void test_DataRows_FetchLimit() throws Exception {

		createPaintingsDataSet();

		SQLSelect<DataRow> q1 = SQLSelect.dataRowQuery("SELECT * FROM PAINTING");
		q1.limit(5);

		assertEquals(5, context.select(q1).size());
	}

	@Test
	public void test_DataRows_FetchOffset() throws Exception {

		createPaintingsDataSet();

		SQLSelect<DataRow> q1 = SQLSelect.dataRowQuery("SELECT * FROM PAINTING");
		q1.offset(4);

		assertEquals(16, context.select(q1).size());
	}

	@Test
	public void test_Append() throws Exception {

		createPaintingsDataSet();

		SQLSelect<Painting> q1 = SQLSelect.query(Painting.class, "SELECT * FROM PAINTING")
				.append(" WHERE PAINTING_TITLE = #bind($a)").params("a", "painting3")
				.columnNameCaps(CapsStrategy.UPPER);

		List<Painting> result = context.select(q1);
		assertEquals(1, result.size());
	}

	@Test
	public void test_Select() throws Exception {

		createPaintingsDataSet();

		List<Painting> result = SQLSelect
				.query(Painting.class, "SELECT * FROM PAINTING WHERE PAINTING_TITLE = #bind($a)")
				.params("a", "painting3").columnNameCaps(CapsStrategy.UPPER).select(context);

		assertEquals(1, result.size());
	}

	@Test
	public void test_SelectOne() throws Exception {

		createPaintingsDataSet();

		Painting a = SQLSelect.query(Painting.class, "SELECT * FROM PAINTING WHERE PAINTING_TITLE = #bind($a)")
				.params("a", "painting3").columnNameCaps(CapsStrategy.UPPER).selectOne(context);

		assertEquals("painting3", a.getPaintingTitle());
	}

	@Test
	public void test_SelectFirst() throws Exception {
		createPaintingsDataSet();

		Painting p = SQLSelect.query(Painting.class, "SELECT * FROM PAINTING ORDER BY PAINTING_TITLE").columnNameCaps(CapsStrategy.UPPER).selectFirst(
				context);

		assertNotNull(p);
		assertEquals("painting1", p.getPaintingTitle());
	}

	@Test
	public void test_SelectFirstByContext() throws Exception {
		createPaintingsDataSet();

		SQLSelect<Painting> q = SQLSelect.query(Painting.class, "SELECT * FROM PAINTING ORDER BY PAINTING_TITLE").columnNameCaps(CapsStrategy.UPPER);
		Painting p = context.selectFirst(q);

		assertNotNull(p);
		assertEquals("painting1", p.getPaintingTitle());
	}

	@Test
	public void test_Iterate() throws Exception {
		createPaintingsDataSet();

		final int[] count = new int[1];
		SQLSelect.query(Painting.class, "SELECT * FROM PAINTING").columnNameCaps(CapsStrategy.UPPER)
				.iterate(context, new ResultIteratorCallback<Painting>() {
					@Override
					public void next(Painting object) {
						assertNotNull(object.getPaintingTitle());
						count[0]++;
					}
				});

		assertEquals(20, count[0]);
	}

	@Test
	public void test_Iterator() throws Exception {
		createPaintingsDataSet();

		try (ResultIterator<Painting> it = SQLSelect.query(Painting.class, "SELECT * FROM PAINTING")
				.columnNameCaps(CapsStrategy.UPPER).iterator(context);) {
			int count = 0;

			for (Painting p : it) {
				count++;
			}

			assertEquals(20, count);
		}
	}

	@Test
	public void test_BatchIterator() throws Exception {
		createPaintingsDataSet();

		try (ResultBatchIterator<Painting> it = SQLSelect.query(Painting.class, "SELECT * FROM PAINTING")
				.columnNameCaps(CapsStrategy.UPPER).batchIterator(context, 5);) {
			int count = 0;

			for (List<Painting> paintingList : it) {
				count++;
				assertEquals(5, paintingList.size());
			}

			assertEquals(4, count);
		}
	}

	@Test
	public void test_SelectLong() throws Exception {

		createPaintingsDataSet();

		long id = SQLSelect
				.scalarQuery(Integer.class, "SELECT PAINTING_ID FROM PAINTING WHERE PAINTING_TITLE = #bind($a)")
				.params("a", "painting3").selectOne(context);

		assertEquals(3l, id);
	}

	@Test
	public void test_SelectLongArray() throws Exception {

		createPaintingsDataSet();

		List<Integer> ids = SQLSelect.scalarQuery(Integer.class,
				"SELECT PAINTING_ID FROM PAINTING ORDER BY PAINTING_ID").select(context);

		assertEquals(20, ids.size());
		assertEquals(2l, ids.get(1).intValue());
	}

	@Test
	public void test_SelectCount() throws Exception {

		createPaintingsDataSet();

		int c = SQLSelect.scalarQuery(Integer.class, "SELECT #result('COUNT(*)' 'int') FROM PAINTING").selectOne(
				context);

		assertEquals(20, c);
	}

	@Test
	public void test_ParamsArray_Single() throws Exception {

		createPaintingsDataSet();

		Integer id = SQLSelect
				.scalarQuery(Integer.class, "SELECT PAINTING_ID FROM PAINTING WHERE PAINTING_TITLE = #bind($a)")
				.paramsArray("painting3").selectOne(context);

		assertEquals(3l, id.intValue());
	}

	@Test
	public void test_ParamsArray_Multiple() throws Exception {

		createPaintingsDataSet();

		List<Integer> ids = SQLSelect
				.scalarQuery(Integer.class,
						"SELECT PAINTING_ID FROM PAINTING WHERE PAINTING_TITLE = #bind($a) OR PAINTING_TITLE = #bind($b) ORDER BY PAINTING_ID")
				.paramsArray("painting3", "painting2").select(context);

		assertEquals(2l, ids.get(0).intValue());
		assertEquals(3l, ids.get(1).intValue());
	}

	@Test
	@Ignore("This is supported by Velocity only")
	// TODO: move this test to new cayenne-velocity module
	public void test_ParamsArray_Multiple_OptionalChunks() throws Exception {

		tPainting.insert(1, "painting1", 1.0);
		tPainting.insert(2, "painting2", null);

		List<Integer> ids = SQLSelect
				.scalarQuery(
						Integer.class,
						"SELECT PAINTING_ID FROM PAINTING #chain('OR' 'WHERE') "
								+ "#chunk($a) ESTIMATED_PRICE #bindEqual($a) #end "
								+ "#chunk($b) PAINTING_TITLE #bindEqual($b) #end #end ORDER BY PAINTING_ID")
				.paramsArray(null, "painting1").select(context);

		assertEquals(1, ids.size());
		assertEquals(1l, ids.get(0).longValue());
	}

	@Test
	@Ignore("This is supported by Velocity only")
	// TODO: move this test to new cayenne-velocity module
	public void test_Params_Multiple_OptionalChunks() throws Exception {

		tPainting.insert(1, "painting1", 1.0);
		tPainting.insert(2, "painting2", null);

		Map<String, Object> params = new HashMap<>();
		params.put("a", null);
		params.put("b", "painting1");

		List<Integer> ids = SQLSelect
				.scalarQuery(
						Integer.class,
						"SELECT PAINTING_ID FROM PAINTING #chain('OR' 'WHERE') "
								+ "#chunk($a) ESTIMATED_PRICE #bindEqual($a) #end "
								+ "#chunk($b) PAINTING_TITLE #bindEqual($b) #end #end ORDER BY PAINTING_ID")
				.params(params).select(context);

		assertEquals(1, ids.size());
		assertEquals(1l, ids.get(0).longValue());
	}
}
