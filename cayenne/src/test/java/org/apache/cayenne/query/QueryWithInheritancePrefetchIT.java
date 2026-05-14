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

package org.apache.cayenne.query;

import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.inheritance_with_enum.Dependent;
import org.apache.cayenne.testdo.inheritance_with_enum.Root;
import org.apache.cayenne.testdo.inheritance_with_enum.Sub;
import org.apache.cayenne.testdo.inheritance_with_enum.Type;
import org.apache.cayenne.unit.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * This one is about https://issues.apache.org/jira/browse/CAY-2405
 */
public class QueryWithInheritancePrefetchIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.INHERITANCE_WITH_ENUM_PROJECT);

    private CayenneRuntime runtime;

    @BeforeEach
    public void createTestData() throws Exception {
        runtime = env.runtime();
        TableHelper tRoot = env.table("iwe_root", "id", "type", "name", "enum");

        tRoot.insert(1, 0, "root1", null);
        tRoot.insert(2, 1, "enum1", Type.type1.ordinal());
        tRoot.insert(3, 1, "enum2", Type.type2.ordinal());

        TableHelper tDependent = env.table("iwe_dependent", "id", "root_id", "name");

        tDependent.insert(1, 1, "test1");
        tDependent.insert(2, 2, "test2");
        tDependent.insert(3, 3, "test3");
    }

    /**
     * Validate that direct select of objects works
     */
    @Test
    public void directQuery() {
        List<Root> result = ObjectSelect.query(Root.class)
                .orderBy("db:" + Root.ID_PK_COLUMN)
                .select(runtime.newContext());

        assertEquals(3, result.size());

        assertNotNull(result.get(0));
        assertFalse(result.get(0) instanceof Sub);
        assertInstanceOf(Sub.class, result.get(1));
        assertInstanceOf(Sub.class, result.get(2));

        assertEquals(Type.type1, ((Sub)result.get(1)).getEnum());
        assertEquals(Type.type2, ((Sub)result.get(2)).getEnum());
    }

    @Test
    public void queryWithJointPrefetch() {
        List<Dependent> result = ObjectSelect.query(Dependent.class)
                .orderBy("db:" + Dependent.ID_PK_COLUMN)
                .prefetch(Dependent.ROOT.joint())
                .select(runtime.newContext());

        assertPrefetchResult(result);
    }

    @Test
    public void queryWithDisjointPrefetch() {
        List<Dependent> result = ObjectSelect.query(Dependent.class)
                .orderBy("db:" + Dependent.ID_PK_COLUMN)
                .prefetch(Dependent.ROOT.disjoint())
                .select(runtime.newContext());

        assertPrefetchResult(result);
    }

    @Test
    public void queryWithDisjointByIdPrefetch() {
        List<Dependent> result = ObjectSelect.query(Dependent.class)
                .orderBy("db:" + Dependent.ID_PK_COLUMN)
                .prefetch(Dependent.ROOT.disjointById())
                .select(runtime.newContext());

        assertPrefetchResult(result);
    }

    private void assertPrefetchResult(final List<Dependent> result) {
        assertEquals(3, result.size());

        env.runWithQueriesBlocked(() -> {
            assertNotNull(result.get(0).getRoot());
            assertFalse(result.get(0).getRoot() instanceof Sub);
            assertInstanceOf(Sub.class, result.get(1).getRoot());
            assertInstanceOf(Sub.class, result.get(2).getRoot());

            assertEquals(Type.type1, ((Sub) result.get(1).getRoot()).getEnum());
            assertEquals(Type.type2, ((Sub) result.get(2).getRoot()).getEnum());
        });
    }

}
