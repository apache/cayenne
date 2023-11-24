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
package org.apache.cayenne;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SQLSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.oneway.OnewayTable1;
import org.apache.cayenne.testdo.oneway.OnewayTable2;
import org.apache.cayenne.testdo.oneway.OnewayTable3;
import org.apache.cayenne.testdo.oneway.OnewayTable4;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.ONEWAY_PROJECT)
public class OneWayRelationshipsIT extends RuntimeCase {

	@Inject
	private ObjectContext context;

	@Inject
	private DBHelper dbHelper;

	private TableHelper t1Helper;
	private TableHelper t2Helper;
	private TableHelper t3Helper;
	private TableHelper t4Helper;

	@Before
	public void setUp() throws Exception {
		t1Helper = new TableHelper(dbHelper, "oneway_table1");
		t1Helper.setColumns("ID");
		t2Helper = new TableHelper(dbHelper, "oneway_table2");
		t2Helper.setColumns("ID", "TABLE1_ID");

		t3Helper = new TableHelper(dbHelper, "oneway_table3");
		t3Helper.setColumns("ID");
		t4Helper = new TableHelper(dbHelper, "oneway_table4");
		t4Helper.setColumns("ID", "TABLE3_ID").setColumnTypes(Types.INTEGER, Types.INTEGER);
	}

	@Test
	public void testToOne_TwoNew() throws SQLException {

		OnewayTable1 t1 = context.newObject(OnewayTable1.class);
		OnewayTable2 t2 = context.newObject(OnewayTable2.class);
		t2.setToOneOneWayDb(t1);

		context.commitChanges();

		int t1Pk = t1Helper.getInt("ID");
		assertEquals(Cayenne.intPKForObject(t1), t1Pk);
		int t2FK = t2Helper.getInt("TABLE1_ID");
		assertEquals(t1Pk, t2FK);
	}

	@Test
	public void testToOne_Replace() throws SQLException {

		t1Helper.insert(1).insert(2);
		t2Helper.insert(1, 1);

		OnewayTable1 t11 = Cayenne.objectForPK(context, OnewayTable1.class, 1);
		OnewayTable1 t12 = Cayenne.objectForPK(context, OnewayTable1.class, 2);
		OnewayTable2 t2 = Cayenne.objectForPK(context, OnewayTable2.class, 1);

		assertSame(t11, t2.getToOneOneWayDb());

		t2.setToOneOneWayDb(t12);
		context.commitChanges();

		assertSame(t12, t2.getToOneOneWayDb());

		int t2FK = t2Helper.getInt("TABLE1_ID");
		assertEquals(2, t2FK);
	}

	@Test
	public void testToOne_ReplaceWithNull() throws SQLException {

		t1Helper.insert(1);
		t2Helper.insert(1, 1);

		OnewayTable1 t11 = Cayenne.objectForPK(context, OnewayTable1.class, 1);
		OnewayTable2 t2 = Cayenne.objectForPK(context, OnewayTable2.class, 1);

		assertSame(t11, t2.getToOneOneWayDb());

		t2.setToOneOneWayDb(null);
		context.commitChanges();

		assertNull(t2.getToOneOneWayDb());

		Object t2FK = t2Helper.getObject("TABLE1_ID");
		assertNull(t2FK);
	}

	@Test
	public void testToMany_TwoNew() throws SQLException {

		OnewayTable3 t3 = context.newObject(OnewayTable3.class);
		OnewayTable4 t4 = context.newObject(OnewayTable4.class);
		t3.addToToManyOneWayDb(t4);

		context.commitChanges();

		int t3Pk = t3Helper.getInt("ID");
		assertEquals(Cayenne.intPKForObject(t3), t3Pk);
		int t4FK = t4Helper.getInt("TABLE3_ID");
		assertEquals(t3Pk, t4FK);
	}

	@Test
	public void testToMany_AddNew() throws SQLException {

		t3Helper.insert(1);
		t4Helper.insert(1, 1);

		OnewayTable3 t3 = Cayenne.objectForPK(context, OnewayTable3.class, 1);
		assertEquals(1, t3.getToManyOneWayDb().size());

		OnewayTable4 t41 = Cayenne.objectForPK(context, OnewayTable4.class, 1);
		assertTrue(t3.getToManyOneWayDb().contains(t41));

		OnewayTable4 t42 = context.newObject(OnewayTable4.class);
		t3.addToToManyOneWayDb(t42);
		context.commitChanges();

		assertEquals(2, t3.getToManyOneWayDb().size());

		SQLSelect<Integer> fksQuery = SQLSelect.scalarQuery("SELECT TABLE3_ID FROM oneway_table4",
				"oneway-rels",Integer.class);

		List<Integer> fks = context.select(fksQuery);
		assertEquals(2, fks.size());
		for (Integer fk : fks) {
			assertEquals(Integer.valueOf(1), fk);
		}
	}

	@Test
	public void testToMany_AddExisting() throws SQLException {

		t3Helper.insert(1);
		t4Helper.insert(1, 1).insert(2, null);

		OnewayTable3 t3 = Cayenne.objectForPK(context, OnewayTable3.class, 1);
		assertEquals(1, t3.getToManyOneWayDb().size());

		OnewayTable4 t41 = Cayenne.objectForPK(context, OnewayTable4.class, 1);
		assertTrue(t3.getToManyOneWayDb().contains(t41));

		OnewayTable4 t42 = Cayenne.objectForPK(context, OnewayTable4.class, 2);

		t3.addToToManyOneWayDb(t42);
		context.commitChanges();

		assertEquals(2, t3.getToManyOneWayDb().size());

		SQLSelect<Integer> fksQuery = SQLSelect.scalarQuery("SELECT TABLE3_ID FROM oneway_table4",
				"oneway-rels", Integer.class);

		List<Integer> fks = context.select(fksQuery);
		assertEquals(2, fks.size());
		for (Integer fk : fks) {
			assertEquals(Integer.valueOf(1), fk);
		}
	}

	@Test
	public void testToMany_RemoveExisting() throws SQLException {

		t3Helper.insert(1);
		t4Helper.insert(1, 1).insert(2, 1);

		OnewayTable3 t3 = Cayenne.objectForPK(context, OnewayTable3.class, 1);
		assertEquals(2, t3.getToManyOneWayDb().size());

		OnewayTable4 t41 = Cayenne.objectForPK(context, OnewayTable4.class, 1);
		assertTrue(t3.getToManyOneWayDb().contains(t41));

		OnewayTable4 t42 = Cayenne.objectForPK(context, OnewayTable4.class, 2);
		assertTrue(t3.getToManyOneWayDb().contains(t42));

		t3.removeFromToManyOneWayDb(t42);
		context.commitChanges();

		assertEquals(1, t3.getToManyOneWayDb().size());

		SQLSelect<Integer> fksQuery = SQLSelect.scalarQuery("SELECT TABLE3_ID FROM oneway_table4",
				"oneway-rels", Integer.class);

		List<Integer> fks = context.select(fksQuery);
		assertEquals(2, fks.size());
		assertTrue(fks.contains(1));
		assertTrue(fks.contains(null));
	}
}
