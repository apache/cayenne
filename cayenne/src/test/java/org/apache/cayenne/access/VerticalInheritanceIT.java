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
package org.apache.cayenne.access;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.query.ColumnSelect;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SelectById;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.inheritance_vertical.*;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Default sorter fails to properly sort all the relationships in the test schema used
public class VerticalInheritanceIT {

	@RegisterExtension
	static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.INHERITANCE_VERTICAL_PROJECT)
	        .withExtraModules(GraphSorterModule.class);

	protected CayenneRuntime runtime;

	TableHelper ivAbstractTable;

	TableHelper ivConcreteTable;

	@BeforeEach
	public void setup() {
		runtime = env.runtime();
		ivAbstractTable = env.table("IV_ABSTRACT").setColumns("ID", "PARENT_ID", "TYPE")
				.setColumnTypes(Types.INTEGER, Types.INTEGER, Types.CHAR);
		ivConcreteTable = env.table("IV_CONCRETE").setColumns("ID", "NAME", "RELATED_ABSTRACT_ID")
				.setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.INTEGER);
	}

	@AfterEach
	public void cleanUpConcrete() throws SQLException {
		ivConcreteTable.deleteAll();
		ivAbstractTable.deleteAll();

		assertEquals(0, ivAbstractTable.getRowCount());
		assertEquals(0, ivConcreteTable.getRowCount());
	}

    @Test
	public void insertRoot() throws Exception {

		TableHelper ivRootTable = env.table("IV_ROOT", "ID", "NAME", "DISCRIMINATOR");

		assertEquals(0, ivRootTable.getRowCount());

		IvRoot root = env.context().newObject(IvRoot.class);
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

    @Test
	public void insertSub1() throws Exception {

		TableHelper ivRootTable = env.table("IV_ROOT", "ID", "NAME", "DISCRIMINATOR");

		TableHelper ivSub1Table = env.table("IV_SUB1", "ID", "SUB1_NAME");

		IvSub1 sub1 = env.context().newObject(IvSub1.class);
		sub1.setName("XyZX");
		sub1.getObjectContext().commitChanges();

		assertEquals(1, ivRootTable.getRowCount());
		assertEquals(0, ivSub1Table.getRowCount());

		Object[] data = ivRootTable.select();
		assertEquals(3, data.length);
		assertTrue(data[0] instanceof Number);
		assertTrue(((Number) data[0]).intValue() > 0);
		assertEquals("XyZX", data[1]);
		assertEquals("IvSub1", data[2]);

		ivSub1Table.deleteAll();
		ivRootTable.deleteAll();

		IvSub1 sub11 = env.context().newObject(IvSub1.class);
		sub11.setName("XyZXY");
		sub11.setSub1Name("BdE2");
		sub11.getObjectContext().commitChanges();

		data = ivRootTable.select();
		assertEquals(3, data.length);
		assertTrue(data[0] instanceof Number);
		assertTrue(((Number) data[0]).intValue() > 0);
		assertEquals("XyZXY", data[1]);
		assertEquals("IvSub1", data[2]);

		Object[] subdata = ivSub1Table.select();
		assertEquals(2, subdata.length);
		assertEquals(data[0], subdata[0]);
		assertEquals("BdE2", subdata[1]);
	}

    @Test
	public void insertSub2() throws Exception {

		TableHelper ivRootTable = env.table("IV_ROOT", "ID", "NAME", "DISCRIMINATOR");

		TableHelper ivSub2Table = env.table("IV_SUB2", "ID", "SUB2_NAME", "SUB2_ATTR");

		IvSub2 sub2 = env.context().newObject(IvSub2.class);
		sub2.setName("XyZX");
		sub2.getObjectContext().commitChanges();

		assertEquals(1, ivRootTable.getRowCount());
		assertEquals(0, ivSub2Table.getRowCount());

		Object[] data = ivRootTable.select();
		assertEquals(3, data.length);
		assertTrue(data[0] instanceof Number);
		assertTrue(((Number) data[0]).intValue() > 0);
		assertEquals("XyZX", data[1]);
		assertEquals("IvSub2", data[2]);

		ivSub2Table.deleteAll();
		ivRootTable.deleteAll();

		IvSub2 sub21 = env.context().newObject(IvSub2.class);
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

		Object[] subdata = ivSub2Table.select();
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

	/**
	 * @link https://issues.apache.org/jira/browse/CAY-2146
	 */
    @Test
    public void validationOnInsertSub3Exception() {
        IvSub3 sub3 = env.context().newObject(IvSub3.class);
        sub3.setName("XyZX");
        assertThrows(org.apache.cayenne.validation.ValidationException.class, env.context()::commitChanges);
    }

	/**
	 * @link https://issues.apache.org/jira/browse/CAY-2146
	 */
	@Test
	public void validationOnInsertSub3Ok() throws Exception {

		TableHelper ivRootTable = env.table("IV_ROOT", "ID", "NAME", "DISCRIMINATOR");

		TableHelper ivSub3Table = env.table("IV_SUB3", "ID", "IV_ROOT_ID");

		IvSub3 sub3 = env.context().newObject(IvSub3.class);
		sub3.setName("XyZX");
		sub3.setIvRoot(sub3);
		env.context().commitChanges();

		assertEquals(1, ivRootTable.getRowCount());
		assertEquals(1, ivSub3Table.getRowCount());
	}

	/**
	 * @link https://issues.apache.org/jira/browse/CAY-2282
	 */
	@Test
	public void updateRelationSub3() throws Exception {
		TableHelper ivRootTable = env.table("IV_ROOT", "ID", "NAME", "DISCRIMINATOR");
		ivRootTable.insert(1, "root1", null);
		ivRootTable.insert(2, "root2", null);
		ivRootTable.insert(3, "name", "IvSub3");

		TableHelper ivSub3Table = env.table("IV_SUB3", "ID", "IV_ROOT_ID");
		ivSub3Table.insert(3, 1);

		IvRoot root = SelectById.query(IvRoot.class, 2).selectOne(env.context());
		IvSub3 sub3 = SelectById.query(IvSub3.class, 3).selectOne(env.context());
		sub3.setName("new name");
		sub3.setIvRoot(root);

		// this will create 3 queries...
		// update for name, insert for new relationship, delete for old relationship
		env.context().commitChanges();

		ObjectContext cleanContext = runtime.newContext();
		IvSub3 sub3Clean = SelectById.query(IvSub3.class, 3).selectOne(cleanContext);

		assertNotNull(sub3Clean);
		assertNotSame(sub3, sub3Clean);

		assertEquals("new name", sub3.getName());
		assertNotNull(sub3Clean.getIvRoot());
		assertEquals("root2", sub3Clean.getIvRoot().getName());

	}

    @Test
	public void insertSub1Sub1() throws Exception {

		TableHelper ivRootTable = env.table("IV_ROOT", "ID", "NAME", "DISCRIMINATOR");

		TableHelper ivSub1Table = env.table("IV_SUB1", "ID", "SUB1_NAME", "SUB1_PRICE");

		TableHelper ivSub1Sub1Table = env.table("IV_SUB1_SUB1", "ID", "SUB1_SUB1_NAME", "SUB1_SUB1_PRICE");

		IvSub1Sub1 sub1Sub1 = env.context().newObject(IvSub1Sub1.class);
		sub1Sub1.setName("XyZN");
		sub1Sub1.setSub1Name("mDA");
		sub1Sub1.setPrice(42.0);
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
		assertEquals(3, subdata.length);
		assertEquals(data[0], subdata[0]);
		assertEquals("mDA", subdata[1]);
		assertNull(subdata[2]);

		Object[] subsubdata = ivSub1Sub1Table.select();
		assertEquals(3, subsubdata.length);
		assertEquals(data[0], subsubdata[0]);
		assertEquals("3DQa", subsubdata[1]);
		assertNull(subdata[2]);
	}

    @Test
	public void selectQuerySuperSub() throws Exception {

		TableHelper ivRootTable = env.table("IV_ROOT").setColumns("ID", "NAME", "DISCRIMINATOR").setColumnTypes(
				Types.INTEGER, Types.VARCHAR, Types.VARCHAR);

		TableHelper ivSub1Table = env.table("IV_SUB1", "ID", "SUB1_NAME");

		// insert
		ivRootTable.insert(1, "xROOT", null);
		ivRootTable.insert(2, "xSUB1_ROOT", "IvSub1");
		ivSub1Table.insert(2, "xSUB1");

		ObjectSelect<IvRoot> query = ObjectSelect.query(IvRoot.class);
		List<IvRoot> results = env.context().select(query);

		assertEquals(2, results.size());

		// since we don't have ordering, need to analyze results in an order
		// agnostic
		// fashion
		Map<String, IvRoot> resultTypes = new HashMap<>();

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

    @Test
	public void selectQueryDeepAndWide() throws Exception {

		TableHelper ivRootTable = env.table("IV_ROOT").setColumns("ID", "NAME", "DISCRIMINATOR").setColumnTypes(
				Types.INTEGER, Types.VARCHAR, Types.VARCHAR);

		TableHelper ivSub1Table = env.table("IV_SUB1", "ID", "SUB1_NAME");

		TableHelper ivSub2Table = env.table("IV_SUB2", "ID", "SUB2_NAME");

		TableHelper ivSub1Sub1Table = env.table("IV_SUB1_SUB1", "ID", "SUB1_SUB1_NAME");

		// insert
		ivRootTable.insert(1, "xROOT", null);

		ivRootTable.insert(2, "xSUB1_ROOT", "IvSub1");
		ivSub1Table.insert(2, "xSUB1");

		ivRootTable.insert(3, "xSUB1_SUB1_ROOT", "IvSub1Sub1");
		ivSub1Table.insert(3, "xSUB1_SUB1_SUBROOT");
		ivSub1Sub1Table.insert(3, "xSUB1_SUB1");

		ivRootTable.insert(4, "xROOT_SUB2", "IvSub2");
		ivSub2Table.insert(4, "xSUB2");

		ObjectSelect<IvRoot> query = ObjectSelect.query(IvRoot.class);
		List<IvRoot> results = env.context().select(query);

		assertEquals(4, results.size());

		// since we don't have ordering, need to analyze results in an order
		// agnostic
		// fashion
		Map<String, IvRoot> resultTypes = new HashMap<>();

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

    @Test
	public void selectQueryMiddleLeaf() throws Exception {

		TableHelper ivRootTable = env.table("IV_ROOT").setColumns("ID", "NAME", "DISCRIMINATOR").setColumnTypes(
				Types.INTEGER, Types.VARCHAR, Types.VARCHAR);

		TableHelper ivSub1Table = env.table("IV_SUB1", "ID", "SUB1_NAME");

		TableHelper ivSub2Table = env.table("IV_SUB2", "ID", "SUB2_NAME");

		TableHelper ivSub1Sub1Table = env.table("IV_SUB1_SUB1", "ID", "SUB1_SUB1_NAME");

		// insert
		ivRootTable.insert(1, "xROOT", null);

		ivRootTable.insert(2, "xSUB1_ROOT", "IvSub1");
		ivSub1Table.insert(2, "xSUB1");

		ivRootTable.insert(3, "xSUB1_SUB1_ROOT", "IvSub1Sub1");
		ivSub1Table.insert(3, "xSUB1_SUB1_SUBROOT");
		ivSub1Sub1Table.insert(3, "xSUB1_SUB1");

		ivRootTable.insert(4, "xROOT_SUB2", "IvSub2");
		ivSub2Table.insert(4, "xSUB2");

		ObjectSelect<IvSub1> query = ObjectSelect.query(IvSub1.class);
		List<IvSub1> results = env.context().select(query);

		assertEquals(2, results.size());

		// since we don't have ordering, need to analyze results in an order
		// agnostic
		// fashion
		Map<String, IvRoot> resultTypes = new HashMap<>();

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

    @Test
	public void deleteMix() throws Exception {

		TableHelper ivRootTable = env.table("IV_ROOT").setColumns("ID", "NAME", "DISCRIMINATOR").setColumnTypes(
				Types.INTEGER, Types.VARCHAR, Types.VARCHAR);

		TableHelper ivSub1Table = env.table("IV_SUB1", "ID", "SUB1_NAME");

		TableHelper ivSub2Table = env.table("IV_SUB2", "ID", "SUB2_NAME");

		TableHelper ivSub1Sub1Table = env.table("IV_SUB1_SUB1", "ID", "SUB1_SUB1_NAME");

		// insert
		ivRootTable.insert(1, "xROOT", null);

		ivRootTable.insert(2, "xSUB1_ROOT", "IvSub1");
		ivSub1Table.insert(2, "xSUB1");

		ivRootTable.insert(3, "xSUB1_SUB1_ROOT", "IvSub1Sub1");
		ivSub1Table.insert(3, "xSUB1_SUB1_SUBROOT");
		ivSub1Sub1Table.insert(3, "xSUB1_SUB1");

		ivRootTable.insert(4, "xROOT_SUB2", "IvSub2");
		ivSub2Table.insert(4, "xSUB2");

		ObjectSelect<IvRoot> query = ObjectSelect.query(IvRoot.class);

		List<IvRoot> results = query.select(env.context());

		assertEquals(4, results.size());
		Map<String, IvRoot> resultTypes = new HashMap<>();

		for (IvRoot result : results) {
			resultTypes.put(result.getClass().getName(), result);
		}

		assertEquals(4, resultTypes.size());

		IvRoot root = resultTypes.get(IvRoot.class.getName());
		env.context().deleteObjects(root);

		IvSub1 sub1 = (IvSub1) resultTypes.get(IvSub1.class.getName());
		env.context().deleteObjects(sub1);

		env.context().commitChanges();

		assertEquals(2, ivRootTable.getRowCount());
		assertEquals(1, ivSub1Table.getRowCount());
		assertEquals(1, ivSub1Sub1Table.getRowCount());
		assertEquals(1, ivSub2Table.getRowCount());

		results = env.context().select(query);
		assertEquals(2, results.size());
	}

    @Test
	public void selectQueryAttributeOverrides() throws Exception {

		TableHelper iv1RootTable = env.table("IV1_ROOT").setColumns("ID", "NAME", "DISCRIMINATOR").setColumnTypes(
				Types.INTEGER, Types.VARCHAR, Types.VARCHAR);

		TableHelper iv1Sub1Table = env.table("IV1_SUB1", "ID", "SUB1_NAME");

		// insert
		iv1RootTable.insert(1, "xROOT", null);
		iv1RootTable.insert(2, "xSUB1_ROOT", "Iv1Sub1");
		iv1Sub1Table.insert(2, "xSUB1");

		ObjectSelect<Iv1Root> query = ObjectSelect.query(Iv1Root.class);
		List<Iv1Root> results = env.context().select(query);

		assertEquals(2, results.size());

		// since we don't have ordering, need to analyze results in an order
		// agnostic
		// fashion
		Map<String, Iv1Root> resultTypes = new HashMap<>();

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

    @Test
	public void insertWithRelationship() throws SQLException {
		TableHelper xTable = env.table("IV2_X");
		TableHelper rootTable = env.table("IV2_ROOT");
		TableHelper sub1Table = env.table("IV2_SUB1");

		assertEquals(0, xTable.getRowCount());
		assertEquals(0, rootTable.getRowCount());
		assertEquals(0, sub1Table.getRowCount());

		Iv2Sub1 root = env.context().newObject(Iv2Sub1.class);
		Iv2X x = env.context().newObject(Iv2X.class);
		root.setX(x);

		env.context().commitChanges();

		assertEquals(1, xTable.getRowCount());
		assertEquals(1, rootTable.getRowCount());
		assertEquals(1, sub1Table.getRowCount());
	}

	@Test
	public void updateWithRelationship() throws SQLException {
		IvConcrete parent1 = env.context().newObject(IvConcrete.class);
		parent1.setName("Parent1");
		env.context().commitChanges();

		IvConcrete parent2 = env.context().newObject(IvConcrete.class);
		parent2.setName("Parent2");
		env.context().commitChanges();

		IvConcrete child = env.context().newObject(IvConcrete.class);
		child.setName("Child");
		child.setParent(parent1);
		env.context().commitChanges();

		child.setParent(parent2);
		env.context().commitChanges();

		assertEquals(parent2, child.getParent());

		// Manually delete child to prevent a foreign key constraint failure while cleaning MySQL db
		env.context().deleteObject(child);
		env.context().commitChanges();
	}

	/**
     * @link https://issues.apache.org/jira/browse/CAY-2838
     */
	@Test
	public void nullifyFlattenedAttribute() throws SQLException {
		IvConcrete concrete = env.context().newObject(IvConcrete.class);
		concrete.setName("Concrete");
		env.context().commitChanges();

		concrete.setName(null);
		env.context().commitChanges();

		assertNull(concrete.getName());

		long id = Cayenne.longPKForObject(concrete);
		{
			ObjectContext cleanContext = runtime.newContext();
			IvConcrete concreteFetched = SelectById.query(IvConcrete.class, id).selectOne(cleanContext);
			assertNull(concreteFetched.getName());
		}
	}

	@Test
	public void nullifyFlattenedRelationship() {
		IvOther other = env.context().newObject(IvOther.class);
		other.setName("other");

		IvImpl impl = env.context().newObject(IvImpl.class);
		impl.setName("Impl 1");
		impl.setOther1(other);
		env.context().commitChanges();

		impl.setOther1(null);
		env.context().commitChanges();

		assertNull(impl.getOther1());

		long id = Cayenne.longPKForObject(impl);
		{
			ObjectContext cleanContext = runtime.newContext();
			IvImpl implFetched = SelectById.query(IvImpl.class, id).selectOne(cleanContext);
			assertEquals("Impl 1", implFetched.getName());
			assertNull(implFetched.getOther1());
		}
	}

	@Test
	public void updateFlattenedRelationshipWithInverse() throws SQLException {
		TableHelper ivOtherTable = env.table("IV_OTHER").setColumns("ID", "NAME").setColumnTypes(Types.INTEGER, Types.VARCHAR);

		TableHelper ivBaseTable = env.table("IV_BASE").setColumns("ID", "NAME", "TYPE").setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.CHAR);

		TableHelper ivImplTable = env.table("IV_IMPL").setColumns("ID", "ATTR1", "OTHER3_ID").setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.INTEGER);

		ivOtherTable.insert(1, "other1");
		ivOtherTable.insert(2, "other2");
		ivBaseTable.insert(1, "Impl 1", "I");
		ivImplTable.insert(1, "attr1", 1);

		IvImpl impl = SelectById.query(IvImpl.class, 1).selectOne(env.context());
		IvOther other = SelectById.query(IvOther.class, 2).selectOne(env.context());

		impl.setOther3(other);
		env.context().commitChanges();
		assertEquals("Impl 1", impl.getName());
		assertEquals("attr1", impl.getAttr1());
		assertEquals(impl.getOther3(), other);

		{
			ObjectContext cleanContext = runtime.newContext();
			IvImpl implFetched = SelectById.query(IvImpl.class, 1).selectOne(cleanContext);
			IvOther otherFetched = SelectById.query(IvOther.class, 2).selectOne(cleanContext);
			assertEquals("Impl 1", implFetched.getName());
			assertEquals("attr1", implFetched.getAttr1());
			assertEquals(implFetched.getOther3(), otherFetched);
		}
	}

	@Test
	public void deleteFlattenedNoValues() throws SQLException {
		ivAbstractTable.insert(1, null, "S");

		IvConcrete concrete = SelectById.query(IvConcrete.class, 1).selectOne(env.context());
		assertNotNull(concrete);
		assertNull(concrete.getName());

		env.context().deleteObject(concrete);
		env.context().commitChanges();

		assertEquals(0, ivAbstractTable.getRowCount());
		assertEquals(0, ivConcreteTable.getRowCount());
	}

	@Test
	public void deleteFlattenedNullValues() throws SQLException {
		ivAbstractTable.insert(1, null, "S");
		ivConcreteTable.insert(1, null, null);

		IvConcrete concrete = SelectById.query(IvConcrete.class, 1).selectOne(env.context());
		assertNotNull(concrete);
		assertNull(concrete.getName());

		env.context().deleteObject(concrete);
		env.context().commitChanges();

		assertEquals(0, ivAbstractTable.getRowCount());
		assertEquals(0, ivConcreteTable.getRowCount());
	}

	@Test
	public void deleteFlattenedNullifyValues() throws SQLException {
		ivAbstractTable.insert(1, null, "S");
		ivConcreteTable.insert(1, "test", null);

		IvConcrete concrete = SelectById.query(IvConcrete.class, 1).selectOne(env.context());
		assertNotNull(concrete);
		assertEquals("test", concrete.getName());

		concrete.setName(null);
		env.context().commitChanges();
        assertNull(concrete.getName());

		assertEquals(1, ivAbstractTable.getRowCount());
		assertEquals(1, ivConcreteTable.getRowCount());

		env.context().deleteObject(concrete);
		env.context().commitChanges();

		assertEquals(0, ivAbstractTable.getRowCount());
		assertEquals(0, ivConcreteTable.getRowCount());
	}

	@Test
	public void nullifyFlattenedRelationshipConcreteToAbstract() throws SQLException {
		ivAbstractTable.insert(1, null, "S");
		ivConcreteTable.insert(1, "One", null);
		ivAbstractTable.insert(2, null, "S");
		ivConcreteTable.insert(2, "Two", 1);

		IvConcrete concrete = SelectById.query(IvConcrete.class, 2).selectOne(env.context());
		concrete.setRelatedAbstract(null);

		env.context().commitChanges();
		assertNull(concrete.getRelatedAbstract());

		{
			ObjectContext cleanContext = runtime.newContext();
			IvConcrete concreteFetched = SelectById.query(IvConcrete.class, 2).selectOne(cleanContext);
			assertEquals("Two", concreteFetched.getName());
			assertNull(concreteFetched.getRelatedAbstract());
		}
	}

	@Test//(expected = ValidationException.class) // other2 is not mandatory for now
	public void insertWithAttributeAndRelationship() {
		IvOther other = env.context().newObject(IvOther.class);
		other.setName("other");

		IvImpl impl = env.context().newObject(IvImpl.class);
		impl.setName("Impl 1");
		impl.setAttr1("attr1");
		impl.setOther1(other);

		env.context().commitChanges();
	}

	@Test
	public void insertWithMultipleAttributeAndMultipleRelationship() {
		IvOther other1 = env.context().newObject(IvOther.class);
		other1.setName("other1");

		IvOther other2 = env.context().newObject(IvOther.class);
		other2.setName("other2");

		IvImpl impl = env.context().newObject(IvImpl.class);
		impl.setName("Impl 1");
		impl.setAttr0(new Date());
		impl.setAttr1("attr1");
		impl.setAttr2("attr2");
		impl.setOther1(other1);
		impl.setOther2(other2);

		env.context().commitChanges();

		IvImpl impl2 = ObjectSelect.query(IvImpl.class).selectFirst(env.context());
		assertEquals(other1, impl2.getOther1());
		assertEquals(other2, impl2.getOther2());
	}

	@Test
	public void insertTwoObjectsWithMultipleAttributeAndMultipleRelationship() {
		IvOther other1 = env.context().newObject(IvOther.class);
		other1.setName("other1");

		IvOther other2 = env.context().newObject(IvOther.class);
		other2.setName("other2");

		IvImpl impl1 = env.context().newObject(IvImpl.class);
		impl1.setName("Impl 1");
		impl1.setAttr1("attr1");
		impl1.setAttr2("attr2");
		impl1.setOther1(other1);
		impl1.setOther2(other2);

		IvImpl impl2 = env.context().newObject(IvImpl.class);
		impl2.setName("Impl 2");
		impl2.setAttr1("attr1");
		impl2.setAttr2("attr2");
		impl2.setOther1(other1);
		impl2.setOther2(other2);

		env.context().commitChanges();

		assertEquals(2, ObjectSelect.query(IvImpl.class).selectCount(env.context()));
	}

	/**
	 * @link https://issues.apache.org/jira/browse/CAY-2840
	 */
	@Test
	public void baseJointPrefetchBelongsTo() throws SQLException {
		TableHelper ivOtherTable = env.table("IV_OTHER").setColumns("ID", "NAME", "BASE_ID").setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.INTEGER);

		TableHelper ivBaseTable = env.table("IV_BASE").setColumns("ID", "NAME", "TYPE").setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.CHAR);

		TableHelper ivImplTable = env.table("IV_IMPL").setColumns("ID", "ATTR1").setColumnTypes(Types.INTEGER, Types.VARCHAR);

		ivBaseTable.insert(1, "Impl 1", "I");
		ivImplTable.insert(1, "attr1");
		ivOtherTable.insert(1, "other1", 1);

		IvOther other = ObjectSelect.query(IvOther.class).prefetch(IvOther.BASE.joint()).selectOne(env.context());
		assertNotNull(other);
		assertNotNull(other.getBase());
		assertTrue(IvImpl.class.isAssignableFrom(other.getBase().getClass()));

		IvImpl impl = (IvImpl)other.getBase();
		// Ensure that base attributes were prefetched correctly
		assertEquals("Impl 1", impl.getName());
		// Ensure that subclass attributes were prefetched correctly
		assertEquals("attr1", impl.getAttr1());
	}

	/**
	 * @link https://issues.apache.org/jira/browse/CAY-2840
	 */
	@Test
	public void baseDisjointPrefetchBelongsTo() throws SQLException {
		TableHelper ivOtherTable = env.table("IV_OTHER").setColumns("ID", "NAME", "BASE_ID").setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.INTEGER);

		TableHelper ivBaseTable = env.table("IV_BASE").setColumns("ID", "NAME", "TYPE").setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.CHAR);

		TableHelper ivImplTable = env.table("IV_IMPL").setColumns("ID", "ATTR1").setColumnTypes(Types.INTEGER, Types.VARCHAR);

		ivBaseTable.insert(1, "Impl 1", "I");
		ivImplTable.insert(1, "attr1");
		ivOtherTable.insert(1, "other1", 1);

		IvOther other = ObjectSelect.query(IvOther.class).prefetch(IvOther.BASE.disjoint()).selectOne(env.context());
		assertNotNull(other);
		assertNotNull(other.getBase());
		assertTrue(IvImpl.class.isAssignableFrom(other.getBase().getClass()));

		IvImpl impl = (IvImpl)other.getBase();
		// Ensure that base attributes were prefetched correctly
		assertEquals("Impl 1", impl.getName());
		// Ensure that subclass attributes were prefetched correctly
		assertEquals("attr1", impl.getAttr1());
	}

	/**
	 * @link https://issues.apache.org/jira/browse/CAY-2840
	 */
	@Test
	public void baseDisjointByIdPrefetchBelongsTo() throws SQLException {
		TableHelper ivOtherTable = env.table("IV_OTHER").setColumns("ID", "NAME", "BASE_ID").setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.INTEGER);

		TableHelper ivBaseTable = env.table("IV_BASE").setColumns("ID", "NAME", "TYPE").setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.CHAR);

		TableHelper ivImplTable = env.table("IV_IMPL").setColumns("ID", "ATTR1").setColumnTypes(Types.INTEGER, Types.VARCHAR);

		ivBaseTable.insert(1, "Impl 1", "I");
		ivImplTable.insert(1, "attr1");
		ivOtherTable.insert(1, "other1", 1);

		IvOther other = ObjectSelect.query(IvOther.class).prefetch(IvOther.BASE.disjointById()).selectOne(env.context());
		assertNotNull(other);
		assertNotNull(other.getBase());
		assertTrue(IvImpl.class.isAssignableFrom(other.getBase().getClass()));

		IvImpl impl = (IvImpl)other.getBase();
		// Ensure that base attributes were prefetched correctly
		assertEquals("Impl 1", impl.getName());
		// Ensure that subclass attributes were prefetched correctly
		assertEquals("attr1", impl.getAttr1());
	}

	/**
	 * @link https://issues.apache.org/jira/browse/CAY-2855
	 */
	@Test
	public void implJointPrefetchBelongsTo() throws SQLException {
		TableHelper ivOtherTable = env.table("IV_OTHER").setColumns("ID", "NAME", "IMPL_ID").setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.INTEGER);

		TableHelper ivBaseTable = env.table("IV_BASE").setColumns("ID", "NAME", "TYPE").setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.CHAR);

		TableHelper ivImplTable = env.table("IV_IMPL").setColumns("ID", "ATTR1").setColumnTypes(Types.INTEGER, Types.VARCHAR);

		ivBaseTable.insert(1, "Impl 1", "I");
		ivImplTable.insert(1, "attr1");
		ivOtherTable.insert(1, "other1", 1);

		IvOther other = ObjectSelect.query(IvOther.class).prefetch(IvOther.IMPL.joint()).limit(1).selectOne(env.context());
		assertNotNull(other);

		IvImpl impl = other.getImpl();
		assertNotNull(other.getImpl());
		// Ensure that base attributes were prefetched correctly
		assertEquals("Impl 1", impl.getName());
		// Ensure that subclass attributes were prefetched correctly
		assertEquals("attr1", impl.getAttr1());

		impl.setOther1(null);
		impl.setOther2(null);
		impl.setOther3(null);
		env.context().commitChanges();
		ivOtherTable.deleteAll();
	}

	/**
	 * @link https://issues.apache.org/jira/browse/CAY-2855
	 */
	@Test
	public void implDisjointPrefetchBelongsTo() throws SQLException {
		TableHelper ivOtherTable = env.table("IV_OTHER").setColumns("ID", "NAME", "IMPL_ID").setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.INTEGER);

		TableHelper ivBaseTable = env.table("IV_BASE").setColumns("ID", "NAME", "TYPE").setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.CHAR);

		TableHelper ivImplTable = env.table("IV_IMPL").setColumns("ID", "ATTR1").setColumnTypes(Types.INTEGER, Types.VARCHAR);

		ivBaseTable.insert(1, "Impl 1", "I");
		ivImplTable.insert(1, "attr1");
		ivOtherTable.insert(1, "other1", 1);

		IvOther other = ObjectSelect.query(IvOther.class)
				.prefetch(IvOther.IMPL.disjoint())
				.selectOne(env.context());
		assertNotNull(other);

		IvImpl impl = other.getImpl();
		assertNotNull(other.getImpl());
		// Ensure that base attributes were prefetched correctly
		assertEquals("Impl 1", impl.getName());
		// Ensure that subclass attributes were prefetched correctly
		assertEquals("attr1", impl.getAttr1());

		impl.setOther1(null);
		impl.setOther2(null);
		impl.setOther3(null);
		env.context().commitChanges();
		ivOtherTable.deleteAll();
	}

	/**
	 * @link https://issues.apache.org/jira/browse/CAY-2855
	 */
	@Test
	public void implDisjointByIdPrefetchBelongsTo() throws SQLException {
		TableHelper ivOtherTable = env.table("IV_OTHER").setColumns("ID", "NAME", "IMPL_ID").setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.INTEGER);

		TableHelper ivBaseTable = env.table("IV_BASE").setColumns("ID", "NAME", "TYPE").setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.CHAR);

		TableHelper ivImplTable = env.table("IV_IMPL").setColumns("ID", "ATTR1").setColumnTypes(Types.INTEGER, Types.VARCHAR);

		ivBaseTable.insert(1, "Impl 1", "I");
		ivImplTable.insert(1, "attr1");
		ivOtherTable.insert(1, "other1", 1);

		IvOther other = ObjectSelect.query(IvOther.class)
				.prefetch(IvOther.IMPL.disjointById())
				.selectOne(env.context());
		assertNotNull(other);

		IvImpl impl = other.getImpl();
		assertNotNull(other.getImpl());
		// Ensure that base attributes were prefetched correctly
		assertEquals("Impl 1", impl.getName());
		// Ensure that subclass attributes were prefetched correctly
		assertEquals("attr1", impl.getAttr1());

		impl.setOther1(null);
		impl.setOther2(null);
		impl.setOther3(null);
		env.context().commitChanges();
		ivOtherTable.deleteAll();
	}

	/**
	 * @link https://issues.apache.org/jira/browse/CAY-2282
	 */
	@Test
	public void updateWithOptimisticLocks() throws SQLException {
		TableHelper ivOtherTable = env.table("IV_OTHER").setColumns("ID", "NAME").setColumnTypes(Types.INTEGER, Types.VARCHAR);

		TableHelper ivBaseWithLockTable = env.table("IV_BASE_WITH_LOCK").setColumns("ID", "NAME", "TYPE")
				.setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.CHAR);

		TableHelper ivImplWithLockTable = env.table("IV_IMPL_WITH_LOCK").setColumns("ID", "ATTR1", "OTHER1_ID")
				.setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.INTEGER);

		// Insert records we want to update (will end up adding more records for final test)
		ivOtherTable.insert(1, "other1");

		ivBaseWithLockTable.insert(1, "Impl 1", "I");

		ivImplWithLockTable.insert(1, "attr1", 1);

		// Fetch and update the records
		for(IvImplWithLock record : ObjectSelect.query(IvImplWithLock.class).select(env.context())) {
			record.setName(record.getName() + "-Change");
			record.setAttr1(record.getAttr1() + "-Change");
		}

		// commit should pass without any exceptions
		env.context().commitChanges();
	}

	@Test
	public void countEjbqlQuery() throws Exception {
		TableHelper ivRootTable = env.table("IV_ROOT", "ID", "NAME", "DISCRIMINATOR");

		TableHelper ivSub1Table = env.table("IV_SUB1", "ID", "SUB1_NAME");

		TableHelper ivSub2Table = env.table("IV_SUB2", "ID", "SUB2_ATTR", "SUB2_NAME");

		// Root, IvSub1, IvSub2

		ivRootTable.insert(1, "root1", "");

		ivRootTable.insert(2, "sub11", "IvSub1");
		ivSub1Table.insert(2, "sub_name1_1");

		ivRootTable.insert(3, "sub21", "IvSub2");
		ivRootTable.insert(4, "sub22", "IvSub2");
		ivSub2Table.insert(3, "attr1", "sub_name2_1");
		ivSub2Table.insert(4, "attr2", "sub_name2_2");

		EJBQLQuery query1 = new EJBQLQuery("SELECT COUNT(a) FROM IvRoot a");
		assertEquals(Collections.singletonList(4L), env.context().performQuery(query1));

		EJBQLQuery query2 = new EJBQLQuery("SELECT COUNT(a) FROM IvSub1 a");
		assertEquals(Collections.singletonList(1L), env.context().performQuery(query2));

		EJBQLQuery query3 = new EJBQLQuery("SELECT COUNT(a) FROM IvSub2 a");
		assertEquals(Collections.singletonList(2L), env.context().performQuery(query3));
	}

	@Test
	public void propagatedGeneratedPK() {
		IvGenKeySub sub = env.context().newObject(IvGenKeySub.class);
		sub.setName("test");
		env.context().commitChanges();

		assertTrue(Cayenne.intPKForObject(sub) > 0);
	}

	@Test
	public void columnSelectVerticalInheritanceSub1() throws SQLException {
		TableHelper ivRootTable = env.table("IV_ROOT", "ID", "NAME", "DISCRIMINATOR");

		TableHelper ivSub1Table = env.table("IV_SUB1", "ID", "SUB1_NAME", "SUB1_PRICE");

		TableHelper ivSub1Sub1Table = env.table("IV_SUB1_SUB1", "ID", "SUB1_SUB1_NAME", "SUB1_SUB1_PRICE");

		IvSub1Sub1 sub1Sub1 = env.context().newObject(IvSub1Sub1.class);
		sub1Sub1.setName("XyZN");
		sub1Sub1.setSub1Name("mDA");
		sub1Sub1.setPrice(42.0);
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
		assertEquals(3, subdata.length);
		assertEquals(data[0], subdata[0]);
		assertEquals("mDA", subdata[1]);

		Object[] subsubdata = ivSub1Sub1Table.select();
		assertEquals(3, subsubdata.length);
		assertEquals(data[0], subsubdata[0]);
		assertEquals("3DQa", subsubdata[1]);

		ColumnSelect<IvSub1> originalQueryForSub1 = ObjectSelect.query(IvSub1.class)
				.column(IvSub1.SELF);

		IvSub1 result = originalQueryForSub1.selectOne(env.context());
		assertEquals("XyZN", result.getName());
		assertEquals(Double.valueOf(42.0), result.getPrice());
		assertEquals("mDA", result.getSub1Name());
	}

	@Test
	public void columnSelectVerticalInheritanceSub1Sub1() throws SQLException {
		TableHelper ivRootTable = env.table("IV_ROOT", "ID", "NAME", "DISCRIMINATOR");

		TableHelper ivSub1Table = env.table("IV_SUB1", "ID", "SUB1_NAME", "SUB1_PRICE");

		TableHelper ivSub1Sub1Table = env.table("IV_SUB1_SUB1", "ID", "SUB1_SUB1_NAME", "SUB1_SUB1_PRICE");

		IvSub1Sub1 sub1Sub1 = env.context().newObject(IvSub1Sub1.class);
		sub1Sub1.setName("XyZN");
		sub1Sub1.setSub1Name("mDA");
		sub1Sub1.setPrice(42.0);
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
		assertEquals(3, subdata.length);
		assertEquals(data[0], subdata[0]);
		assertEquals("mDA", subdata[1]);

		Object[] subsubdata = ivSub1Sub1Table.select();
		assertEquals(3, subsubdata.length);
		assertEquals(data[0], subsubdata[0]);
		assertEquals("3DQa", subsubdata[1]);

		ColumnSelect<IvSub1Sub1> originalQueryForSub1Sub1 = ObjectSelect.query(IvSub1Sub1.class)
				.column(IvSub1Sub1.SELF);

		IvSub1Sub1 result = originalQueryForSub1Sub1.selectOne(env.context());
		assertEquals("XyZN", result.getName());
		assertEquals(Double.valueOf(42.0), result.getPrice());
		assertEquals("mDA", result.getSub1Name());
		assertEquals("3DQa", result.getSub1Sub1Name());
	}

	@Test
	public void insertTwoGenericVerticalInheritanceObjects() {
		// Generic DataObjects play nicer with a DataContext
		final DataContext dataContext = (DataContext) env.context();

		final Persistent girlEmma = dataContext.newObject("GenGirl");
		final Persistent boyLuke = dataContext.newObject("GenBoy");

		assertEquals(girlEmma.readProperty("type"), "G", "Girl is type G");
		assertEquals(boyLuke.readProperty("type"), "B", "Boy is type B");

		girlEmma.writeProperty("reference", "g1");
		girlEmma.writeProperty("name", "Emma");
		girlEmma.writeProperty("toyDolls", 5);

		boyLuke.writeProperty("reference", "b1");
		boyLuke.writeProperty("name", "Luke");
		boyLuke.writeProperty("toyTrucks", 12);

		env.context().commitChanges();

		assertEquals(2, ObjectSelect.query(Persistent.class, "GenStudent").selectCount(env.context()));

		final List<Persistent> students = ObjectSelect.query(Persistent.class, "GenStudent").select(env.context());
		assertTrue(students.contains(girlEmma));
		assertTrue(students.contains(boyLuke));

		final List<Persistent> girls = ObjectSelect.query(Persistent.class, "GenGirl").select(env.context());
		assertEquals(1, girls.size());
		final List<Persistent> boys = ObjectSelect.query(Persistent.class, "GenBoy").select(env.context());
		assertEquals(1, boys.size());
	}
}
