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
package org.apache.cayenne.access;

import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.inheritance.vertical.Iv1Root;
import org.apache.cayenne.testdo.inheritance.vertical.Iv1Sub1;
import org.apache.cayenne.testdo.inheritance.vertical.Iv2Sub1;
import org.apache.cayenne.testdo.inheritance.vertical.Iv2X;
import org.apache.cayenne.testdo.inheritance.vertical.IvRoot;
import org.apache.cayenne.testdo.inheritance.vertical.IvSub1;
import org.apache.cayenne.testdo.inheritance.vertical.IvSub1Sub1;
import org.apache.cayenne.testdo.inheritance.vertical.IvSub2;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.INHERTITANCE_VERTICAL_PROJECT)
public class VerticalInheritanceTest extends ServerCase {

	@Inject
	protected ObjectContext context;

	@Inject
	protected DBHelper dbHelper;

	@Override
	protected void setUpAfterInjection() throws Exception {

		dbHelper.deleteAll("IV_SUB1_SUB1");
		dbHelper.deleteAll("IV_SUB1");
		dbHelper.deleteAll("IV_SUB2");
		dbHelper.deleteAll("IV_ROOT");

		dbHelper.deleteAll("IV1_SUB1");
		dbHelper.deleteAll("IV1_ROOT");

		dbHelper.deleteAll("IV2_SUB1");
		dbHelper.deleteAll("IV2_ROOT");
		dbHelper.deleteAll("IV2_X");
	}

	public void testInsert_Root() throws Exception {

		TableHelper ivRootTable = new TableHelper(dbHelper, "IV_ROOT");
		ivRootTable.setColumns("ID", "NAME", "DISCRIMINATOR");

		assertEquals(0, ivRootTable.getRowCount());

		IvRoot root = context.newObject(IvRoot.class);
		root.setName("XyZ");
		root.getObjectContext().commitChanges();

		assertEquals(1, ivRootTable.getRowCount());

		Object[] rootData = ivRootTable.select();
		assertEquals(3, rootData.length);
		assertTrue(rootData[0] instanceof Number);
		assertTrue(((Number) rootData[0]).intValue() > 0);
		assertEquals("XyZ", rootData[1]);
		assertNull(rootData[2]);
	}

	public void testInsert_Sub1() throws Exception {

		TableHelper ivRootTable = new TableHelper(dbHelper, "IV_ROOT");
		ivRootTable.setColumns("ID", "NAME", "DISCRIMINATOR");

		TableHelper ivSub1Table = new TableHelper(dbHelper, "IV_SUB1");
		ivSub1Table.setColumns("ID", "SUB1_NAME");

		IvSub1 sub1 = context.newObject(IvSub1.class);
		sub1.setName("XyZX");
		sub1.getObjectContext().commitChanges();

		assertEquals(1, ivRootTable.getRowCount());
		assertEquals(1, ivSub1Table.getRowCount());

		Object[] data = ivRootTable.select();
		assertEquals(3, data.length);
		assertTrue(data[0] instanceof Number);
		assertTrue(((Number) data[0]).intValue() > 0);
		assertEquals("XyZX", data[1]);
		assertEquals("IvSub1", data[2]);

		Object[] subdata = ivSub1Table.select();
		assertEquals(2, subdata.length);
		assertEquals(data[0], subdata[0]);
		assertNull(subdata[1]);

		ivSub1Table.deleteAll();
		ivRootTable.deleteAll();

		IvSub1 sub11 = context.newObject(IvSub1.class);
		sub11.setName("XyZXY");
		sub11.setSub1Name("BdE2");
		sub11.getObjectContext().commitChanges();

		data = ivRootTable.select();
		assertEquals(3, data.length);
		assertTrue(data[0] instanceof Number);
		assertTrue(((Number) data[0]).intValue() > 0);
		assertEquals("XyZXY", data[1]);
		assertEquals("IvSub1", data[2]);

		subdata = ivSub1Table.select();
		assertEquals(2, subdata.length);
		assertEquals(data[0], subdata[0]);
		assertEquals("BdE2", subdata[1]);
	}

	public void testInsert_Sub2() throws Exception {

		TableHelper ivRootTable = new TableHelper(dbHelper, "IV_ROOT");
		ivRootTable.setColumns("ID", "NAME", "DISCRIMINATOR");

		TableHelper ivSub2Table = new TableHelper(dbHelper, "IV_SUB2");
		ivSub2Table.setColumns("ID", "SUB2_NAME", "SUB2_ATTR");

		IvSub2 sub2 = context.newObject(IvSub2.class);
		sub2.setName("XyZX");
		sub2.getObjectContext().commitChanges();

		assertEquals(1, ivRootTable.getRowCount());
		assertEquals(1, ivSub2Table.getRowCount());

		Object[] data = ivRootTable.select();
		assertEquals(3, data.length);
		assertTrue(data[0] instanceof Number);
		assertTrue(((Number) data[0]).intValue() > 0);
		assertEquals("XyZX", data[1]);
		assertEquals("IvSub2", data[2]);

		Object[] subdata = ivSub2Table.select();
		assertEquals(3, subdata.length);
		assertEquals(data[0], subdata[0]);
		assertNull(subdata[1]);
		assertNull(subdata[2]);

		ivSub2Table.deleteAll();
		ivRootTable.deleteAll();

		IvSub2 sub21 = context.newObject(IvSub2.class);
		sub21.setName("XyZXY");
		sub21.setSub2Name("BdE2");
		sub21.setSub2Attr("aTtR");
		sub21.getObjectContext().commitChanges();

		data = ivRootTable.select();
		assertEquals(3, data.length);
		assertTrue(data[0] instanceof Number);
		assertTrue(((Number) data[0]).intValue() > 0);
		assertEquals("XyZXY", data[1]);
		assertEquals("IvSub2", data[2]);

		subdata = ivSub2Table.select();
		assertEquals(3, subdata.length);
		assertEquals(data[0], subdata[0]);
		assertEquals("BdE2", subdata[1]);
		assertEquals("aTtR", subdata[2]);

		sub21.setSub2Attr("BUuT");
		sub21.getObjectContext().commitChanges();

		subdata = ivSub2Table.select();
		assertEquals(3, subdata.length);
		assertEquals(data[0], subdata[0]);
		assertEquals("BdE2", subdata[1]);
		assertEquals("BUuT", subdata[2]);

		sub21.getObjectContext().deleteObjects(sub21);
		sub21.getObjectContext().commitChanges();

		assertEquals(0, ivRootTable.getRowCount());
		assertEquals(0, ivSub2Table.getRowCount());
	}

	public void testInsert_Sub1Sub1() throws Exception {

		TableHelper ivRootTable = new TableHelper(dbHelper, "IV_ROOT");
		ivRootTable.setColumns("ID", "NAME", "DISCRIMINATOR");

		TableHelper ivSub1Table = new TableHelper(dbHelper, "IV_SUB1");
		ivSub1Table.setColumns("ID", "SUB1_NAME");

		TableHelper ivSub1Sub1Table = new TableHelper(dbHelper, "IV_SUB1_SUB1");
		ivSub1Sub1Table.setColumns("ID", "SUB1_SUB1_NAME");

		IvSub1Sub1 sub1Sub1 = context.newObject(IvSub1Sub1.class);
		sub1Sub1.setName("XyZN");
		sub1Sub1.setSub1Name("mDA");
		sub1Sub1.setSub1Sub1Name("3DQa");
		sub1Sub1.getObjectContext().commitChanges();

		assertEquals(1, ivRootTable.getRowCount());
		assertEquals(1, ivSub1Table.getRowCount());
		assertEquals(1, ivSub1Sub1Table.getRowCount());

		Object[] data = ivRootTable.select();
		assertEquals(3, data.length);
		assertTrue(data[0] instanceof Number);
		assertTrue(((Number) data[0]).intValue() > 0);
		assertEquals("XyZN", data[1]);
		assertEquals("IvSub1Sub1", data[2]);

		Object[] subdata = ivSub1Table.select();
		assertEquals(2, subdata.length);
		assertEquals(data[0], subdata[0]);
		assertEquals("mDA", subdata[1]);

		Object[] subsubdata = ivSub1Sub1Table.select();
		assertEquals(2, subsubdata.length);
		assertEquals(data[0], subsubdata[0]);
		assertEquals("3DQa", subsubdata[1]);
	}

	public void testSelectQuery_SuperSub() throws Exception {

		TableHelper ivRootTable = new TableHelper(dbHelper, "IV_ROOT");
		ivRootTable.setColumns("ID", "NAME", "DISCRIMINATOR").setColumnTypes(
				Types.INTEGER, Types.VARCHAR, Types.VARCHAR);

		TableHelper ivSub1Table = new TableHelper(dbHelper, "IV_SUB1");
		ivSub1Table.setColumns("ID", "SUB1_NAME");

		// insert
		ivRootTable.insert(1, "xROOT", null);
		ivRootTable.insert(2, "xSUB1_ROOT", "IvSub1");
		ivSub1Table.insert(2, "xSUB1");

		SelectQuery query = new SelectQuery(IvRoot.class);
		List<IvRoot> results = context.performQuery(query);

		assertEquals(2, results.size());

		// since we don't have ordering, need to analyze results in an order
		// agnostic
		// fashion
		Map<String, IvRoot> resultTypes = new HashMap<String, IvRoot>();

		for (IvRoot result : results) {
			resultTypes.put(result.getClass().getName(), result);
		}

		assertEquals(2, resultTypes.size());

		IvRoot root = resultTypes.get(IvRoot.class.getName());
		assertNotNull(root);
		assertEquals("xROOT", root.getName());
		assertNull(root.getDiscriminator());

		IvSub1 sub1 = (IvSub1) resultTypes.get(IvSub1.class.getName());
		assertNotNull(sub1);
		assertEquals("xSUB1_ROOT", sub1.getName());
		assertEquals("IvSub1", sub1.getDiscriminator());
	}

	public void testSelectQuery_DeepAndWide() throws Exception {

		TableHelper ivRootTable = new TableHelper(dbHelper, "IV_ROOT");
		ivRootTable.setColumns("ID", "NAME", "DISCRIMINATOR").setColumnTypes(
				Types.INTEGER, Types.VARCHAR, Types.VARCHAR);

		TableHelper ivSub1Table = new TableHelper(dbHelper, "IV_SUB1");
		ivSub1Table.setColumns("ID", "SUB1_NAME");

		TableHelper ivSub2Table = new TableHelper(dbHelper, "IV_SUB2");
		ivSub2Table.setColumns("ID", "SUB2_NAME");

		TableHelper ivSub1Sub1Table = new TableHelper(dbHelper, "IV_SUB1_SUB1");
		ivSub1Sub1Table.setColumns("ID", "SUB1_SUB1_NAME");

		// insert
		ivRootTable.insert(1, "xROOT", null);

		ivRootTable.insert(2, "xSUB1_ROOT", "IvSub1");
		ivSub1Table.insert(2, "xSUB1");

		ivRootTable.insert(3, "xSUB1_SUB1_ROOT", "IvSub1Sub1");
		ivSub1Table.insert(3, "xSUB1_SUB1_SUBROOT");
		ivSub1Sub1Table.insert(3, "xSUB1_SUB1");

		ivRootTable.insert(4, "xROOT_SUB2", "IvSub2");
		ivSub2Table.insert(4, "xSUB2");

		SelectQuery query = new SelectQuery(IvRoot.class);
		List<IvRoot> results = context.performQuery(query);

		assertEquals(4, results.size());

		// since we don't have ordering, need to analyze results in an order
		// agnostic
		// fashion
		Map<String, IvRoot> resultTypes = new HashMap<String, IvRoot>();

		for (IvRoot result : results) {
			resultTypes.put(result.getClass().getName(), result);
		}

		assertEquals(4, resultTypes.size());

		IvRoot root = resultTypes.get(IvRoot.class.getName());
		assertNotNull(root);
		assertEquals("xROOT", root.getName());
		assertNull(root.getDiscriminator());

		IvSub1 sub1 = (IvSub1) resultTypes.get(IvSub1.class.getName());
		assertNotNull(sub1);
		assertEquals("xSUB1_ROOT", sub1.getName());
		assertEquals("IvSub1", sub1.getDiscriminator());

		IvSub1Sub1 sub1Sub1 = (IvSub1Sub1) resultTypes.get(IvSub1Sub1.class
				.getName());
		assertNotNull(sub1Sub1);
		assertEquals("xSUB1_SUB1_ROOT", sub1Sub1.getName());
		assertEquals("IvSub1Sub1", sub1Sub1.getDiscriminator());
		assertEquals("xSUB1_SUB1_SUBROOT", sub1Sub1.getSub1Name());
		assertEquals("xSUB1_SUB1", sub1Sub1.getSub1Sub1Name());

		IvSub2 sub2 = (IvSub2) resultTypes.get(IvSub2.class.getName());
		assertNotNull(sub2);
		assertEquals("xROOT_SUB2", sub2.getName());
		assertEquals("IvSub2", sub2.getDiscriminator());
		assertEquals("xSUB2", sub2.getSub2Name());
	}

	public void testSelectQuery_MiddleLeaf() throws Exception {

		TableHelper ivRootTable = new TableHelper(dbHelper, "IV_ROOT");
		ivRootTable.setColumns("ID", "NAME", "DISCRIMINATOR").setColumnTypes(
				Types.INTEGER, Types.VARCHAR, Types.VARCHAR);

		TableHelper ivSub1Table = new TableHelper(dbHelper, "IV_SUB1");
		ivSub1Table.setColumns("ID", "SUB1_NAME");

		TableHelper ivSub2Table = new TableHelper(dbHelper, "IV_SUB2");
		ivSub2Table.setColumns("ID", "SUB2_NAME");

		TableHelper ivSub1Sub1Table = new TableHelper(dbHelper, "IV_SUB1_SUB1");
		ivSub1Sub1Table.setColumns("ID", "SUB1_SUB1_NAME");

		// insert
		ivRootTable.insert(1, "xROOT", null);

		ivRootTable.insert(2, "xSUB1_ROOT", "IvSub1");
		ivSub1Table.insert(2, "xSUB1");

		ivRootTable.insert(3, "xSUB1_SUB1_ROOT", "IvSub1Sub1");
		ivSub1Table.insert(3, "xSUB1_SUB1_SUBROOT");
		ivSub1Sub1Table.insert(3, "xSUB1_SUB1");

		ivRootTable.insert(4, "xROOT_SUB2", "IvSub2");
		ivSub2Table.insert(4, "xSUB2");

		SelectQuery query = new SelectQuery(IvSub1.class);
		List<IvRoot> results = context.performQuery(query);

		assertEquals(2, results.size());

		// since we don't have ordering, need to analyze results in an order
		// agnostic
		// fashion
		Map<String, IvRoot> resultTypes = new HashMap<String, IvRoot>();

		for (IvRoot result : results) {
			resultTypes.put(result.getClass().getName(), result);
		}

		assertEquals(2, resultTypes.size());

		IvSub1 sub1 = (IvSub1) resultTypes.get(IvSub1.class.getName());
		assertNotNull(sub1);
		assertEquals("xSUB1_ROOT", sub1.getName());
		assertEquals("IvSub1", sub1.getDiscriminator());

		IvSub1Sub1 sub1Sub1 = (IvSub1Sub1) resultTypes.get(IvSub1Sub1.class
				.getName());
		assertNotNull(sub1Sub1);
		assertEquals("xSUB1_SUB1_ROOT", sub1Sub1.getName());
		assertEquals("IvSub1Sub1", sub1Sub1.getDiscriminator());
		assertEquals("xSUB1_SUB1_SUBROOT", sub1Sub1.getSub1Name());
		assertEquals("xSUB1_SUB1", sub1Sub1.getSub1Sub1Name());
	}

	public void testDelete_Mix() throws Exception {

		TableHelper ivRootTable = new TableHelper(dbHelper, "IV_ROOT");
		ivRootTable.setColumns("ID", "NAME", "DISCRIMINATOR").setColumnTypes(
				Types.INTEGER, Types.VARCHAR, Types.VARCHAR);

		TableHelper ivSub1Table = new TableHelper(dbHelper, "IV_SUB1");
		ivSub1Table.setColumns("ID", "SUB1_NAME");

		TableHelper ivSub2Table = new TableHelper(dbHelper, "IV_SUB2");
		ivSub2Table.setColumns("ID", "SUB2_NAME");

		TableHelper ivSub1Sub1Table = new TableHelper(dbHelper, "IV_SUB1_SUB1");
		ivSub1Sub1Table.setColumns("ID", "SUB1_SUB1_NAME");

		// insert
		ivRootTable.insert(1, "xROOT", null);

		ivRootTable.insert(2, "xSUB1_ROOT", "IvSub1");
		ivSub1Table.insert(2, "xSUB1");

		ivRootTable.insert(3, "xSUB1_SUB1_ROOT", "IvSub1Sub1");
		ivSub1Table.insert(3, "xSUB1_SUB1_SUBROOT");
		ivSub1Sub1Table.insert(3, "xSUB1_SUB1");

		ivRootTable.insert(4, "xROOT_SUB2", "IvSub2");
		ivSub2Table.insert(4, "xSUB2");

		SelectQuery query = new SelectQuery(IvRoot.class);

		List<IvRoot> results = context.performQuery(query);

		assertEquals(4, results.size());
		Map<String, IvRoot> resultTypes = new HashMap<String, IvRoot>();

		for (IvRoot result : results) {
			resultTypes.put(result.getClass().getName(), result);
		}

		assertEquals(4, resultTypes.size());

		IvRoot root = resultTypes.get(IvRoot.class.getName());
		context.deleteObjects(root);

		IvSub1 sub1 = (IvSub1) resultTypes.get(IvSub1.class.getName());
		context.deleteObjects(sub1);

		context.commitChanges();

		assertEquals(2, ivRootTable.getRowCount());
		assertEquals(1, ivSub1Table.getRowCount());
		assertEquals(1, ivSub1Sub1Table.getRowCount());
		assertEquals(1, ivSub2Table.getRowCount());

		results = context.performQuery(query);
		assertEquals(2, results.size());
	}

	public void testSelectQuery_AttributeOverrides() throws Exception {

		TableHelper iv1RootTable = new TableHelper(dbHelper, "IV1_ROOT");
		iv1RootTable.setColumns("ID", "NAME", "DISCRIMINATOR").setColumnTypes(
				Types.INTEGER, Types.VARCHAR, Types.VARCHAR);

		TableHelper iv1Sub1Table = new TableHelper(dbHelper, "IV1_SUB1");
		iv1Sub1Table.setColumns("ID", "SUB1_NAME");

		// insert
		iv1RootTable.insert(1, "xROOT", null);
		iv1RootTable.insert(2, "xSUB1_ROOT", "Iv1Sub1");
		iv1Sub1Table.insert(2, "xSUB1");

		SelectQuery query = new SelectQuery(Iv1Root.class);
		List<Iv1Root> results = context.performQuery(query);

		assertEquals(2, results.size());

		// since we don't have ordering, need to analyze results in an order
		// agnostic
		// fashion
		Map<String, Iv1Root> resultTypes = new HashMap<String, Iv1Root>();

		for (Iv1Root result : results) {
			resultTypes.put(result.getClass().getName(), result);
		}

		assertEquals(2, resultTypes.size());

		Iv1Root root = resultTypes.get(Iv1Root.class.getName());
		assertNotNull(root);
		assertEquals("xROOT", root.getName());
		assertNull(root.getDiscriminator());

		Iv1Sub1 sub1 = (Iv1Sub1) resultTypes.get(Iv1Sub1.class.getName());
		assertNotNull(sub1);
		assertEquals("xSUB1", sub1.getName());
	}

	public void testInsertWithRelationship() throws SQLException {
		TableHelper xTable = new TableHelper(dbHelper, "IV2_X");
		TableHelper rootTable = new TableHelper(dbHelper, "IV2_ROOT");
		TableHelper sub1Table = new TableHelper(dbHelper, "IV2_SUB1");

		assertEquals(0, xTable.getRowCount());
		assertEquals(0, rootTable.getRowCount());
		assertEquals(0, sub1Table.getRowCount());

		Iv2Sub1 root = context.newObject(Iv2Sub1.class);
		Iv2X x = context.newObject(Iv2X.class);
		root.setX(x);

		context.commitChanges();

		assertEquals(1, xTable.getRowCount());
		assertEquals(1, rootTable.getRowCount());
		assertEquals(1, sub1Table.getRowCount());
	}

}
