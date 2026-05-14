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

import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.relationships_collection_to_many.CollectionToMany;
import org.apache.cayenne.testdo.relationships_collection_to_many.CollectionToManyTarget;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class CayennePersistentObjectSetToManyCollectionIT {

	@RegisterExtension
	static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.RELATIONSHIPS_COLLECTION_TO_MANY_PROJECT);

	@BeforeEach
	public void setUp() throws Exception {
		TableHelper tCollectionToMany = env.table("COLLECTION_TO_MANY", "ID");

		TableHelper tCollectionToManyTarget = env.table("COLLECTION_TO_MANY_TARGET", "ID", "COLLECTION_TO_MANY_ID");

		// single data set for all tests
		tCollectionToMany.insert(1).insert(2);
		tCollectionToManyTarget.insert(1, 1).insert(2, 1).insert(3, 1).insert(4, 2);
	}

	@Test
	public void readToMany() {

		CollectionToMany o1 = Cayenne.objectForPK(env.context(), CollectionToMany.class, 1);

		Collection<?> targets = o1.getTargets();

		assertNotNull(targets);
		assertTrue(((ValueHolder) targets).isFault());

		assertEquals(3, targets.size());

		assertTrue(targets.contains(Cayenne.objectForPK(o1.getObjectContext(), CollectionToManyTarget.class, 1)));
		assertTrue(targets.contains(Cayenne.objectForPK(o1.getObjectContext(), CollectionToManyTarget.class, 2)));
		assertTrue(targets.contains(Cayenne.objectForPK(o1.getObjectContext(), CollectionToManyTarget.class, 3)));
	}

	/**
	 * Testing if collection type is Collection, everything should work fine without a runtime exception
	 */
	@Test
	public void relationCollectionTypeCollection() {
		CollectionToMany o1 = Cayenne.objectForPK(env.context(), CollectionToMany.class, 1);
		assertTrue(o1.readProperty(CollectionToMany.TARGETS.getName()) instanceof Collection);
		assertDoesNotThrow(() -> o1.setToManyTarget(CollectionToMany.TARGETS.getName(), new ArrayList<CollectionToMany>(0), true));
		assertEquals(0, o1.getTargets().size());
	}
}
