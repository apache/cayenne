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

import java.util.List;

import org.apache.cayenne.Fault;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.relationships_flattened.FlattenedTest3;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.RELATIONSHIPS_FLATTENED_PROJECT)
public class FlattenedRelationshipInContextIT extends RuntimeCase {

    @Inject
    protected DataContext context;

    @Inject
    protected DBHelper dbHelper;

    private TableHelper tFlattenedTest1;
    private TableHelper tFlattenedTest2;
    private TableHelper tFlattenedTest3;

    @Before
    public void setUp() throws Exception {
        tFlattenedTest1 = new TableHelper(dbHelper, "FLATTENED_TEST_1");
        tFlattenedTest1.setColumns("FT1_ID", "NAME");

        tFlattenedTest2 = new TableHelper(dbHelper, "FLATTENED_TEST_2");
        tFlattenedTest2.setColumns("FT2_ID", "FT1_ID", "NAME");

        tFlattenedTest3 = new TableHelper(dbHelper, "FLATTENED_TEST_3");
        tFlattenedTest3.setColumns("FT3_ID", "FT2_ID", "NAME");
    }

    protected void createFlattenedTestDataSet() throws Exception {
        tFlattenedTest1.insert(1, "ft1");
        tFlattenedTest1.insert(2, "ft12");
        tFlattenedTest2.insert(1, 1, "ft2");
        tFlattenedTest3.insert(1, 1, "ft3");
    }

    @Test
    public void testIsToOneTargetModifiedFlattenedFault1() throws Exception {

        createFlattenedTestDataSet();

        // fetch
        List<FlattenedTest3> ft3s = ObjectSelect.query(FlattenedTest3.class).select(context);
        assertEquals(1, ft3s.size());
        FlattenedTest3 ft3 = ft3s.get(0);

        // mark as dirty for the purpose of the test...
        ft3.setPersistenceState(PersistenceState.MODIFIED);

        assertTrue(ft3.readPropertyDirectly("toFT1") instanceof Fault);

        // test that checking for modifications does not trigger a fault, and generally
        // works well

        ClassDescriptor d = context.getEntityResolver().getClassDescriptor(
                "FlattenedTest3");
        ArcProperty flattenedRel = (ArcProperty) d.getProperty("toFT1");

        ObjectDiff diff = context.getObjectStore().registerDiff(ft3.getObjectId(), null);
        assertFalse(DataRowUtils.isToOneTargetModified(flattenedRel, ft3, diff));
        assertTrue(ft3.readPropertyDirectly("toFT1") instanceof Fault);
    }

}
