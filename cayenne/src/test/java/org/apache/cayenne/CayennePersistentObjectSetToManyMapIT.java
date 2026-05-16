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

import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.map_to_many.MapToMany;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CayennePersistentObjectSetToManyMapIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.MAP_TO_MANY_PROJECT);

    protected TableHelper tMapToMany;
    protected TableHelper tMapToManyTarget;
    protected TableHelper tIdMapToMany;
    protected TableHelper tIdMapToManyTarget;

    @BeforeEach
    public void setUp() throws Exception {
        tMapToMany = env.table("MAP_TO_MANY", "ID");

        tMapToManyTarget = env.table("MAP_TO_MANY_TARGET", "ID", "MAP_TO_MANY_ID", "NAME");

        tIdMapToMany = env.table("ID_MAP_TO_MANY", "ID");

        tIdMapToManyTarget = env.table("ID_MAP_TO_MANY_TARGET", "ID", "MAP_TO_MANY_ID");

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
    public void relationCollectionTypeMap() {
        MapToMany o1 = Cayenne.objectForPK(env.context(), MapToMany.class, 1);
        assertTrue(o1.readProperty(MapToMany.TARGETS.getName()) instanceof Map);
        assertDoesNotThrow(() -> o1.setToManyTarget(MapToMany.TARGETS.getName(), new ArrayList<MapToMany>(0), true));
        assertEquals(0, o1.getTargets().size());
    }
}