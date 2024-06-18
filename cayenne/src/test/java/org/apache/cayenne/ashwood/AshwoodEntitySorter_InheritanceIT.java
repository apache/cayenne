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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.PeopleProjectCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.PEOPLE_PROJECT)
public class AshwoodEntitySorter_InheritanceIT extends PeopleProjectCase {

	@Inject
	protected ObjectContext context;

	private EntityResolver resolver;
	private AshwoodEntitySorter sorter;

	private DbEntity address;
	private DbEntity clientCompany;
	private DbEntity department;
	private DbEntity person;
	private DbEntity personNotes;

	@Before
	public void before() {

		this.resolver = context.getEntityResolver();
		this.sorter = new AshwoodEntitySorter();
		sorter.setEntityResolver(resolver);

		this.address = resolver.getDbEntity("ADDRESS");
		this.clientCompany = resolver.getDbEntity("CLIENT_COMPANY");
		this.department = resolver.getDbEntity("DEPARTMENT");
		this.person = resolver.getDbEntity("PERSON");
		this.personNotes = resolver.getDbEntity("PERSON_NOTES");
	}

	@Test
	public void testSortDbEntities() {

		List<DbEntity> entities = Arrays.asList(address, clientCompany, department, person, personNotes);
		Collections.shuffle(entities);

		sorter.sortDbEntities(entities, false);

		assertTrue(entities.indexOf(person) < entities.indexOf(personNotes));
		assertTrue(entities.indexOf(clientCompany) < entities.indexOf(person));
		assertTrue(entities.indexOf(person) < entities.indexOf(address));

		// this is actually undefined as person depends on department, and
		// department depends on person
		// assertTrue(entities.indexOf(person) < entities.indexOf(department));
	}

}
