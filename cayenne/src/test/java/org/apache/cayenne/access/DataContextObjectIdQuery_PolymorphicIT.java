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
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.inheritance_people.AbstractPerson;
import org.apache.cayenne.testdo.inheritance_people.Employee;
import org.apache.cayenne.testdo.inheritance_people.Manager;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.runtime.PeopleProjectCase;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Types;

import static org.junit.Assert.assertTrue;

public class DataContextObjectIdQuery_PolymorphicIT extends PeopleProjectCase {

	@Inject
	private DataContext context1;

	@Inject
	private DataContext context2;

	@Inject
	private DataChannelInterceptor queryInterceptor;

	private TableHelper tPerson;

	@Before
	public void before() {
		tPerson = new TableHelper(dbHelper, "PERSON").setColumns("PERSON_ID", "NAME", "PERSON_TYPE")
				.setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.CHAR);
	}

	@Test
	public void testPolymorphicSharedCache() throws SQLException {

		tPerson.insert(1, "P1", "EM");

		final ObjectIdQuery q1 = new ObjectIdQuery(ObjectId.of("AbstractPerson", "PERSON_ID", 1), false,
				ObjectIdQuery.CACHE);

		AbstractPerson ap1 = (AbstractPerson) Cayenne.objectForQuery(context1, q1);
		assertTrue(ap1 instanceof Manager);

		queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

			@Override
			public void execute() {
				// use different context to ensure we hit shared cache
				AbstractPerson ap2 = (AbstractPerson) Cayenne.objectForQuery(context2, q1);
				assertTrue(ap2 instanceof Manager);
			}
		});
	}

	@Test
	public void testPolymorphicSharedCache_AfterCayenneInsert() throws SQLException {


		// see CAY-2101... we are trying to get a snapshot from a new object in the shared cache, and then read this
		// object via a relationship, so that shared cache is consulted
		Employee e = context1.newObject(Employee.class);
		e.setName("E1");
		e.setSalary(1234.01f);

		context1.commitChanges();


		final ObjectIdQuery q1 = new ObjectIdQuery(
				ObjectId.of("AbstractPerson", "PERSON_ID", Cayenne.intPKForObject(e)),
				false,
				ObjectIdQuery.CACHE);


		queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

			@Override
			public void execute() {
				// use different context to ensure we hit shared cache
				AbstractPerson ap1 = (AbstractPerson) Cayenne.objectForQuery(context2, q1);
				assertTrue(ap1 instanceof Employee);
			}
		});
	}

	@Test
	public void testPolymorphicLocalCache() throws SQLException {

		tPerson.insert(1, "P1", "EM");

		final ObjectIdQuery q1 = new ObjectIdQuery(ObjectId.of("AbstractPerson", "PERSON_ID", 1), false,
				ObjectIdQuery.CACHE);

		AbstractPerson ap1 = (AbstractPerson) Cayenne.objectForQuery(context1, q1);
		assertTrue(ap1 instanceof Manager);

		queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

			@Override
			public void execute() {
				// use same context to ensure we hit local cache
				// note that this does not guarantee test correctness. If local
				// cache polymorphic ID lookup is broken, shared cache will pick
				// it up
				AbstractPerson ap2 = (AbstractPerson) Cayenne.objectForQuery(context1, q1);
				assertTrue(ap2 instanceof Manager);
			}
		});
	}
}
