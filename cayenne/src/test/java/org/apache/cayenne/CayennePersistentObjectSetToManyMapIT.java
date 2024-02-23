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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.map_to_many.MapToMany;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.*;

@UseCayenneRuntime(CayenneProjects.MAP_TO_MANY_PROJECT)
public class CayennePersistentObjectSetToManyMapIT extends RuntimeCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tMapToMany;
    protected TableHelper tMapToManyTarget;
    protected TableHelper tIdMapToMany;
    protected TableHelper tIdMapToManyTarget;

    @Before
    public void setUp() throws Exception {
        tMapToMany = new TableHelper(dbHelper, "MAP_TO_MANY");
        tMapToMany.setColumns("ID");

        tMapToManyTarget = new TableHelper(dbHelper, "MAP_TO_MANY_TARGET");
        tMapToManyTarget.setColumns("ID", "MAP_TO_MANY_ID", "NAME");

        tIdMapToMany = new TableHelper(dbHelper, "ID_MAP_TO_MANY");
        tIdMapToMany.setColumns("ID");

        tIdMapToManyTarget = new TableHelper(dbHelper, "ID_MAP_TO_MANY_TARGET");
        tIdMapToManyTarget.setColumns("ID", "MAP_TO_MANY_ID");

        createTestDataSet();
    }

    protected void createTestDataSet() throws Exception {
        tMapToMany.insert(1);
        tMapToMany.insert(2);
        tMapToManyTarget.insert(1, 1, "A");
        tMapToManyTarget.insert(2, 1, "B");
        tMapToManyTarget.insert(3, 1, "C");
        tMapToManyTarget.insert(4, 2, "A");
    }

    /**
     * Testing if collection type is map, everything should work fine without a runtime exception
     */
    @Test
    public void testRelationCollectionTypeMap() {
        MapToMany o1 = Cayenne.objectForPK(context, MapToMany.class, 1);
        assertTrue(o1.readProperty(MapToMany.TARGETS.getName()) instanceof Map);
        try {
            o1.setToManyTarget(MapToMany.TARGETS.getName(), new ArrayList<MapToMany>(0), true);
        } catch (RuntimeException e) {
            fail();
        }
        assertEquals(0, o1.getTargets().size(), 0);
    }
}