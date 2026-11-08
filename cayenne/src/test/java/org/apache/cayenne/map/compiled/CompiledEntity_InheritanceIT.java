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
package org.apache.cayenne.map.compiled;

import org.apache.cayenne.PeopleTestBase;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The single-table inheritance hierarchy of the "people" map: AbstractPerson with CustomerRepresentative and
 * Employee, Employee with Manager.
 */
public class CompiledEntity_InheritanceIT extends PeopleTestBase {

    @Test
    public void abstractPersonTree() {
        CompiledEntity abstractPerson = env.mapping().entity("AbstractPerson");
        assertNotNull(abstractPerson);
        assertTrue(abstractPerson.isAbstract());
        assertNull(abstractPerson.superEntity());
        assertTrue(abstractPerson.hasSubEntities());

        assertEquals(2, abstractPerson.subEntities().size());
        assertEquals(Set.of("CustomerRepresentative", "Employee"),
                Set.copyOf(abstractPerson.subEntities().stream().map(CompiledEntity::name).toList()));
        // the entity itself first, then all its sub-entities
        assertSame(abstractPerson, abstractPerson.allSubEntities().getFirst());
        assertEquals(Set.of("AbstractPerson", "CustomerRepresentative", "Employee", "Manager"),
                Set.copyOf(abstractPerson.allSubEntities().stream().map(CompiledEntity::name).toList()));
    }

    @Test
    public void employeeTree() {
        CompiledEntity employee = env.mapping().entity("Employee");
        assertNotNull(employee);
        assertSame(env.mapping().entity("AbstractPerson"), employee.superEntity());
        assertEquals(1, employee.subEntities().size());
        assertSame(env.mapping().entity("Manager"), employee.subEntity("Manager"));
        assertSame(env.mapping().entity("Manager"), employee.subEntities().getFirst());
        // a name that is not of a sub-entity resolves to the entity itself
        assertSame(employee, employee.subEntity("CustomerRepresentative"));
        assertSame(employee, employee.subEntity("Employee"));
    }

    @Test
    public void managerTree() {
        CompiledEntity manager = env.mapping().entity("Manager");
        assertNotNull(manager);
        assertFalse(manager.hasSubEntities());
        assertTrue(manager.subEntities().isEmpty());
        assertSame(env.mapping().entity("Employee"), manager.superEntity());
        assertTrue(manager.isSubEntityOf(env.mapping().entity("AbstractPerson")));
        assertTrue(manager.isSubEntityOf(env.mapping().entity("Employee")));
        assertFalse(manager.isSubEntityOf(env.mapping().entity("CustomerRepresentative")));
    }

    @Test
    public void sharedTable() {
        CompiledTable person = env.mapping().schema().table("PERSON");
        assertSame(person, env.mapping().entity("AbstractPerson").table());
        assertSame(person, env.mapping().entity("Manager").table());
        assertEquals(Set.of("AbstractPerson", "CustomerRepresentative", "Employee", "Manager"),
                Set.copyOf(person.entities().stream().map(CompiledEntity::name).toList()));
    }
}
