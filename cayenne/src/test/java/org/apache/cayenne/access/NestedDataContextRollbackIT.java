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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsExt;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NestedDataContextRollbackIT {

    @RegisterExtension
    static final CayenneTestsExt env = CayenneTestsExt.forProject(CayenneProjects.TESTMAP_PROJECT);

    protected CayenneRuntime runtime;
    private DataContext context;

    @BeforeEach
    public void setUp() {
        runtime = env.runtime();
        context = env.dataContext();
    }


    @Test
    public void rollbackChanges() {
        ObjectContext child1 = runtime.newContext(context);

        assertFalse(context.hasChanges());
        assertFalse(child1.hasChanges());

        context.newObject(Artist.class);
        child1.newObject(Artist.class);

        assertTrue(context.hasChanges());
        assertTrue(child1.hasChanges());

        child1.rollbackChanges();
        assertFalse(context.hasChanges());
        assertFalse(child1.hasChanges());
    }

    @Test
    public void rollbackChangesLocally() {
        ObjectContext child1 = runtime.newContext(context);

        assertFalse(context.hasChanges());
        assertFalse(child1.hasChanges());

        context.newObject(Artist.class);
        child1.newObject(Artist.class);

        assertTrue(context.hasChanges());
        assertTrue(child1.hasChanges());

        child1.rollbackChangesLocally();
        assertTrue(context.hasChanges());
        assertFalse(child1.hasChanges());
    }
}
