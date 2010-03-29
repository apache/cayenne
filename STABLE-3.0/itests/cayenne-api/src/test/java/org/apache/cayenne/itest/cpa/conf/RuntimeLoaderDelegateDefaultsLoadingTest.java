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
package org.apache.cayenne.itest.cpa.conf;

import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.itest.cpa.CPAContextCase;
import org.apache.cayenne.itest.cpa.defaults.DefaultsTable2;
import org.apache.cayenne.itest.cpa.defaults.DefaultsTable3;
import org.apache.cayenne.itest.cpa.defaults.DefaultsTable4;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.RefreshQuery;

public class RuntimeLoaderDelegateDefaultsLoadingTest extends CPAContextCase {

	public void testLoadedReverseDb() {

		DbEntity table1 = getContext().getEntityResolver().getDbEntity(
				"defaults_table1");
		DbEntity table2 = getContext().getEntityResolver().getDbEntity(
				"defaults_table2");
		assertNotNull(table1.getAnyRelationship(table2));
		assertTrue(table1.getAnyRelationship(table2).isRuntime());
		assertFalse(table2.getAnyRelationship(table1).isRuntime());
	}

	public void testLoadedReverseObj() {

		ObjEntity class1 = getContext().getEntityResolver().getObjEntity(
				"DefaultsTable1");
		ObjEntity class2 = getContext().getEntityResolver().getObjEntity(
				"DefaultsTable2");
		assertNotNull(class1.getAnyRelationship(class2));
		assertTrue(class1.getAnyRelationship(class2).isRuntime());
		assertFalse(class2.getAnyRelationship(class1).isRuntime());
	}

	public void testResolveRelationship() throws Exception {
		getDbHelper().deleteAll("defaults_table2");
		getDbHelper().deleteAll("defaults_table1");
		getDbHelper().insert("defaults_table1", new String[] { "id", "name" },
				new Object[] { 1, "X" });
		getDbHelper().insert("defaults_table2",
				new String[] { "id", "defaults_table1_id" },
				new Object[] { 1, 1 });

		DefaultsTable2 o = (DefaultsTable2) DataObjectUtils.objectForPK(
				getContext(), DefaultsTable2.class, 1);
		assertNotNull(o.getToTable1());
		assertEquals("X", o.getToTable1().getName());
	}

	public void testUpdateImplicitToOne() throws Exception {
		getDbHelper().deleteAll("defaults_table4");
		getDbHelper().deleteAll("defaults_table3");
		getDbHelper().insert("defaults_table3", new String[] { "id", "name" },
				new Object[] { 1, "X" });
		getDbHelper().insert("defaults_table3", new String[] { "id", "name" },
				new Object[] { 2, "Y" });
		getDbHelper().insert("defaults_table4",
				new String[] { "id", "defaults_table3_id" },
				new Object[] { 1, 1 });

		DefaultsTable4 o = (DefaultsTable4) DataObjectUtils.objectForPK(
				getContext(), DefaultsTable4.class, 1);
		DefaultsTable3 o1 = (DefaultsTable3) DataObjectUtils.objectForPK(
				getContext(), DefaultsTable3.class, 1);
		DefaultsTable3 o2 = (DefaultsTable3) DataObjectUtils.objectForPK(
				getContext(), DefaultsTable3.class, 2);

		assertEquals(1, o1.getDefaultTable4s().size());
		assertEquals(0, o2.getDefaultTable4s().size());

		o2.addToDefaultTable4s(o);

		assertEquals(1, o2.getDefaultTable4s().size());
		getContext().commitChanges();
		assertEquals(1, o2.getDefaultTable4s().size());

		getContext().performQuery(new RefreshQuery());

		// note that the old to-many is only refreshed after invalidation with
		// RefreshQuery... should this be treated as a bug?
		assertEquals(0, o1.getDefaultTable4s().size());
		assertEquals(1, o2.getDefaultTable4s().size());
	}
}
