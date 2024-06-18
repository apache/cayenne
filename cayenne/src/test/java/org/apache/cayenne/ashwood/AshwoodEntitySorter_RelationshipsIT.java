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

package org.apache.cayenne.ashwood;

import static org.junit.Assert.assertEquals;

import java.sql.Types;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.relationships.ReflexiveAndToOne;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

@UseCayenneRuntime(CayenneProjects.RELATIONSHIPS_PROJECT)
public class AshwoodEntitySorter_RelationshipsIT extends RuntimeCase {

	@Inject
	protected DBHelper dbHelper;

	@Inject
	protected ObjectContext context;

	private TableHelper tRelationshipHelper;
	private TableHelper tReflexiveAndToOne;

	private AshwoodEntitySorter sorter;

	@Before
	public void setUp() throws Exception {
		this.sorter = new AshwoodEntitySorter();
		sorter.setEntityResolver(context.getEntityResolver());

		tRelationshipHelper = new TableHelper(dbHelper, "RELATIONSHIP_HELPER");
		tRelationshipHelper.setColumns("RELATIONSHIP_HELPER_ID", "NAME");

		tReflexiveAndToOne = new TableHelper(dbHelper, "REFLEXIVE_AND_TO_ONE");
		tReflexiveAndToOne.setColumns("REFLEXIVE_AND_TO_ONE_ID", "PARENT_ID", "RELATIONSHIP_HELPER_ID", "NAME")
				.setColumnTypes(Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.VARCHAR);
	}

	@Test
	public void testSortObjectsForEntityReflexiveWithFaults() throws Exception {

		tRelationshipHelper.insert(1, "rh1");
		tReflexiveAndToOne.insert(1, null, 1, "r1");
		tReflexiveAndToOne.insert(2, 1, 1, "r2");
		tReflexiveAndToOne.insert(3, 2, 1, "r3");

		ObjEntity entity = context.getEntityResolver().getObjEntity(ReflexiveAndToOne.class);

		List<ReflexiveAndToOne> objects = ObjectSelect.query(ReflexiveAndToOne.class).select(context);
		Collections.shuffle(objects);
		assertEquals(3, objects.size());

		sorter.sortObjectsForEntity(entity, objects, true);

		assertEquals("r3", objects.get(0).getName());
		assertEquals("r2", objects.get(1).getName());
		assertEquals("r1", objects.get(2).getName());

		tReflexiveAndToOne.delete().where("PARENT_ID", 2).execute();
		tReflexiveAndToOne.delete().where("PARENT_ID", 1).execute();
		tReflexiveAndToOne.deleteAll();
	}
}
