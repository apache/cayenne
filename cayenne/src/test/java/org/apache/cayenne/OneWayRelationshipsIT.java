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

import org.apache.cayenne.query.SQLSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.oneway.OnewayTable1;
import org.apache.cayenne.testdo.oneway.OnewayTable2;
import org.apache.cayenne.testdo.oneway.OnewayTable3;
import org.apache.cayenne.testdo.oneway.OnewayTable4;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OneWayRelationshipsIT {

	@RegisterExtension
	static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.ONEWAY_PROJECT);

	private TableHelper t1Helper;
	private TableHelper t2Helper;
	private TableHelper t3Helper;
	private TableHelper t4Helper;

	@BeforeEach
	public void setUp() throws Exception {
		t1Helper = env.table("oneway_table1", "ID");
		t2Helper = env.table("oneway_table2", "ID", "TABLE1_ID");

		t3Helper = env.table("oneway_table3", "ID");
		t4Helper = env.table("oneway_table4").setColumns("ID", "TABLE3_ID").setColumnTypes(Types.INTEGER, Types.INTEGER);
	}

	@Test
	public void testToOne_TwoNew() throws SQLException {

		OnewayTable1 t1 = env.context().newObject(OnewayTable1.class);
		OnewayTable2 t2 = env.context().newObject(OnewayTable2.class);
		t2.setToOneOneWayDb(t1);

		env.context().commitChanges();

		int t1Pk = t1Helper.getInt("ID");
		assertEquals(Cayenne.intPKForObject(t1), t1Pk);
		int t2FK = t2Helper.getInt("TABLE1_ID");
		assertEquals(t1Pk, t2FK);
	}

	@Test
	public void testToOne_Replace() throws SQLException {

		t1Helper.insert(1).insert(2);
		t2Helper.insert(1, 1);

		OnewayTable1 t11 = Cayenne.objectForPK(env.context(), OnewayTable1.class, 1);
		OnewayTable1 t12 = Cayenne.objectForPK(env.context(), OnewayTable1.class, 2);
		OnewayTable2 t2 = Cayenne.objectForPK(env.context(), OnewayTable2.class, 1);

		assertSame(t11, t2.getToOneOneWayDb());

		t2.setToOneOneWayDb(t12);
		env.context().commitChanges();

		assertSame(t12, t2.getToOneOneWayDb());

		int t2FK = t2Helper.getInt("TABLE1_ID");
		assertEquals(2, t2FK);
	}

	@Test
	public void testToOne_ReplaceWithNull() throws SQLException {

		t1Helper.insert(1);
		t2Helper.insert(1, 1);

		OnewayTable1 t11 = Cayenne.objectForPK(env.context(), OnewayTable1.class, 1);
		OnewayTable2 t2 = Cayenne.objectForPK(env.context(), OnewayTable2.class, 1);

		assertSame(t11, t2.getToOneOneWayDb());

		t2.setToOneOneWayDb(null);
		env.context().commitChanges();

		assertNull(t2.getToOneOneWayDb());

		Object t2FK = t2Helper.getObject("TABLE1_ID");
		assertNull(t2FK);
	}

	@Test
	public void testToMany_TwoNew() throws SQLException {

		OnewayTable3 t3 = env.context().newObject(OnewayTable3.class);
		OnewayTable4 t4 = env.context().newObject(OnewayTable4.class);
		t3.addToToManyOneWayDb(t4);

		env.context().commitChanges();

		int t3Pk = t3Helper.getInt("ID");
		assertEquals(Cayenne.intPKForObject(t3), t3Pk);
		int t4FK = t4Helper.getInt("TABLE3_ID");
		assertEquals(t3Pk, t4FK);
	}

	@Test
	public void testToMany_AddNew() throws SQLException {

		t3Helper.insert(1);
		t4Helper.insert(1, 1);

		OnewayTable3 t3 = Cayenne.objectForPK(env.context(), OnewayTable3.class, 1);
		assertEquals(1, t3.getToManyOneWayDb().size());

		OnewayTable4 t41 = Cayenne.objectForPK(env.context(), OnewayTable4.class, 1);
		assertTrue(t3.getToManyOneWayDb().contains(t41));

		OnewayTable4 t42 = env.context().newObject(OnewayTable4.class);
		t3.addToToManyOneWayDb(t42);
		env.context().commitChanges();

		assertEquals(2, t3.getToManyOneWayDb().size());

		SQLSelect<Integer> fksQuery = SQLSelect.scalarQuery("SELECT TABLE3_ID FROM oneway_table4",
				"oneway-rels",Integer.class);

		List<Integer> fks = env.context().select(fksQuery);
		assertEquals(2, fks.size());
		for (Integer fk : fks) {
			assertEquals(Integer.valueOf(1), fk);
		}
	}

	@Test
	public void testToMany_AddExisting() throws SQLException {

		t3Helper.insert(1);
		t4Helper.insert(1, 1).insert(2, null);

		OnewayTable3 t3 = Cayenne.objectForPK(env.context(), OnewayTable3.class, 1);
		assertEquals(1, t3.getToManyOneWayDb().size());

		OnewayTable4 t41 = Cayenne.objectForPK(env.context(), OnewayTable4.class, 1);
		assertTrue(t3.getToManyOneWayDb().contains(t41));

		OnewayTable4 t42 = Cayenne.objectForPK(env.context(), OnewayTable4.class, 2);

		t3.addToToManyOneWayDb(t42);
		env.context().commitChanges();

		assertEquals(2, t3.getToManyOneWayDb().size());

		SQLSelect<Integer> fksQuery = SQLSelect.scalarQuery("SELECT TABLE3_ID FROM oneway_table4",
				"oneway-rels", Integer.class);

		List<Integer> fks = env.context().select(fksQuery);
		assertEquals(2, fks.size());
		for (Integer fk : fks) {
			assertEquals(Integer.valueOf(1), fk);
		}
	}

	@Test
	public void testToMany_RemoveExisting() throws SQLException {

		t3Helper.insert(1);
		t4Helper.insert(1, 1).insert(2, 1);

		OnewayTable3 t3 = Cayenne.objectForPK(env.context(), OnewayTable3.class, 1);
		assertEquals(2, t3.getToManyOneWayDb().size());

		OnewayTable4 t41 = Cayenne.objectForPK(env.context(), OnewayTable4.class, 1);
		assertTrue(t3.getToManyOneWayDb().contains(t41));

		OnewayTable4 t42 = Cayenne.objectForPK(env.context(), OnewayTable4.class, 2);
		assertTrue(t3.getToManyOneWayDb().contains(t42));

		t3.removeFromToManyOneWayDb(t42);
		env.context().commitChanges();

		assertEquals(1, t3.getToManyOneWayDb().size());

		SQLSelect<Integer> fksQuery = SQLSelect.scalarQuery("SELECT TABLE3_ID FROM oneway_table4",
				"oneway-rels", Integer.class);

		List<Integer> fks = env.context().select(fksQuery);
		assertEquals(2, fks.size());
		assertTrue(fks.contains(1));
		assertTrue(fks.contains(null));
	}
}
