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
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NestedDataContextRollbackIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void rollbackChanges() {
        ObjectContext child1 = env.runtime().newContext(env.context());

        assertFalse(env.context().hasChanges());
        assertFalse(child1.hasChanges());

        env.context().newObject(Artist.class);
        child1.newObject(Artist.class);

        assertTrue(env.context().hasChanges());
        assertTrue(child1.hasChanges());

        child1.rollbackChanges();
        assertFalse(env.context().hasChanges());
        assertFalse(child1.hasChanges());
    }

    @Test
    public void rollbackChangesLocally() {
        ObjectContext child1 = env.runtime().newContext(env.context());

        assertFalse(env.context().hasChanges());
        assertFalse(child1.hasChanges());

        env.context().newObject(Artist.class);
        child1.newObject(Artist.class);

        assertTrue(env.context().hasChanges());
        assertTrue(child1.hasChanges());

        child1.rollbackChangesLocally();
        assertTrue(env.context().hasChanges());
        assertFalse(child1.hasChanges());
    }
}
