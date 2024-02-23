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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.map_to_many.MapToMany;
import org.apache.cayenne.testdo.relationships_set_to_many.SetToMany;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Set;

import static org.junit.Assert.*;

@UseCayenneRuntime(CayenneProjects.RELATIONSHIPS_SET_TO_MANY_PROJECT)
public class CayennePersistentObjectSetToManySetIT extends RuntimeCase {

	@Inject
	protected ObjectContext context;

	@Inject
	protected DBHelper dbHelper;

	protected TableHelper tSetToMany;
	protected TableHelper tSetToManyTarget;

	@Before
	public void setUp() throws Exception {
		tSetToMany = new TableHelper(dbHelper, "SET_TO_MANY");
		tSetToMany.setColumns("ID");

		tSetToManyTarget = new TableHelper(dbHelper, "SET_TO_MANY_TARGET");
		tSetToManyTarget.setColumns("ID", "SET_TO_MANY_ID");

		createTestDataSet();
	}

	protected void createTestDataSet() throws Exception {
		tSetToMany.insert(1);
		tSetToMany.insert(2);
		tSetToManyTarget.insert(1, 1);
		tSetToManyTarget.insert(2, 1);
		tSetToManyTarget.insert(3, 1);
		tSetToManyTarget.insert(4, 2);
	}

	/**
	 * Testing if collection type is set, everything should work fine without a runtime exception
	 */
	@Test
	public void testRelationCollectionTypeMap() throws Exception {


		SetToMany o1 = Cayenne.objectForPK(context, SetToMany.class, 1);
		assertTrue(o1.readProperty(SetToMany.TARGETS.getName()) instanceof Set);
		try {
			o1.setToManyTarget(SetToMany.TARGETS.getName(), new ArrayList<MapToMany>(0), true);
		} catch (RuntimeException e) {
			fail();
		}
		assertEquals(o1.getTargets().size(), 0);
	}
}
