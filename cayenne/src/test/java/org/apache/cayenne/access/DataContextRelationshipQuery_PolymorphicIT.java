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
import org.apache.cayenne.testdo.inheritance_people.Employee;
import org.apache.cayenne.testdo.inheritance_people.PersonNotes;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.runtime.PeopleProjectCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class DataContextRelationshipQuery_PolymorphicIT extends PeopleProjectCase {

        private DataContext context1;
        private DataContext context2;
        private DataChannelInterceptor queryInterceptor;

    @BeforeEach
    public void before() {
        context1 = env.context();
        context2 = (DataContext) env.runtime().newContext();
        queryInterceptor = env.dataChannelInterceptor();
    }

    @Test
    public void polymorphicSharedCache() {


        // see CAY-2101... we are trying to get a snapshot from a new object in the shared cache, and then read this
        // object via a relationship, so that shared cache is consulted
        Employee e = context1.newObject(Employee.class);
        e.setName("E1");
        e.setSalary(1234.01f);
        PersonNotes n = context1.newObject(PersonNotes.class);
        n.setNotes("N1");
        n.setPerson(e);

        context1.commitChanges();

        // use different context to ensure we hit shared cache for relationship resolving
        final PersonNotes nPeer = Cayenne.objectForPK(context2, PersonNotes.class, Cayenne.intPKForObject(n));

        queryInterceptor.runWithQueriesBlocked(() -> assertInstanceOf(Employee.class, nPeer.getPerson()));
    }
}
