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

import java.util.List;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.inheritance_with_enum.Dependent;
import org.apache.cayenne.testdo.inheritance_with_enum.Root;
import org.apache.cayenne.testdo.inheritance_with_enum.Sub;
import org.apache.cayenne.testdo.inheritance_with_enum.Type;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 * This one is about https://issues.apache.org/jira/browse/CAY-2405
 *
 * @since 4.1
 */
@UseCayenneRuntime(CayenneProjects.INHERITANCE_WITH_ENUM_PROJECT)
public class QueryWithInheritancePrefetchIT extends RuntimeCase {

    @Inject
    private CayenneRuntime runtime;

    @Inject
    private DBHelper dbHelper;

    @Inject
    private DataChannelInterceptor queryInterceptor;

    @Before
    public void createTestData() throws Exception {
        TableHelper tRoot = new TableHelper(dbHelper, "iwe_root");
        tRoot.setColumns("id", "type", "name", "enum");

        tRoot.insert(1, 0, "root1", null);
        tRoot.insert(2, 1, "enum1", Type.type1.ordinal());
        tRoot.insert(3, 1, "enum2", Type.type2.ordinal());

        TableHelper tDependent = new TableHelper(dbHelper, "iwe_dependent");
        tDependent.setColumns("id", "root_id", "name");

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
        assertTrue(result.get(1) instanceof Sub);
        assertTrue(result.get(2) instanceof Sub);

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

        queryInterceptor.runWithQueriesBlocked(() -> {
            assertNotNull(result.get(0).getRoot());
            assertFalse(result.get(0).getRoot() instanceof Sub);
            assertTrue(result.get(1).getRoot() instanceof Sub);
            assertTrue(result.get(2).getRoot() instanceof Sub);

            assertEquals(Type.type1, ((Sub) result.get(1).getRoot()).getEnum());
            assertEquals(Type.type2, ((Sub) result.get(2).getRoot()).getEnum());
        });
    }

}
