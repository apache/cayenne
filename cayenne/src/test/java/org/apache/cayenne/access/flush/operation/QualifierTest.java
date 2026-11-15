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

package org.apache.cayenne.access.flush.operation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.unit.CompiledTables;
import org.apache.cayenne.unit.TestObjects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class QualifierTest {

    @Test
    public void scalarObjectIdQualifier() {
        ObjectId id = ObjectId.of("test", "id", 123);
        Persistent persistent = TestObjects.modifiedObject(id);
        DbRowOp row = row(persistent);

        Qualifier qualifier = new Qualifier(row);
        Map<String, Object> qualifierSnapshot = qualifier.getSnapshot();

        assertEquals(Collections.singletonMap("id", 123), qualifierSnapshot);
        assertFalse(qualifier.isUsingOptimisticLocking());

        qualifierSnapshot = qualifier.getSnapshot();
        assertEquals(Collections.singletonMap("id", 123), qualifierSnapshot);
    }

    @Test
    public void mapObjectIdQualifier() {
        Map<String, Object> idMap = new HashMap<>();
        idMap.put("id1", 123);
        idMap.put("id2", 321);
        ObjectId id = ObjectId.of("test", idMap);

        Persistent persistent = TestObjects.modifiedObject(id);
        DbRowOp row = row(persistent);

        Qualifier qualifier = new Qualifier(row);
        Map<String, Object> qualifierSnapshot = qualifier.getSnapshot();
        assertEquals(idMap, qualifierSnapshot);

        qualifierSnapshot = qualifier.getSnapshot();
        assertEquals(idMap, qualifierSnapshot);
    }

    @Test
    public void additionalQualifier() {
        ObjectId id = ObjectId.of("test", "id", 123);
        Persistent persistent = TestObjects.modifiedObject(id);
        DbRowOp row = row(persistent);

        Qualifier qualifier = new Qualifier(row);
        qualifier.addAdditionalQualifier(CompiledTables.column("attr"), 42, true);

        Map<String, Object> qualifierSnapshot = qualifier.getSnapshot();

        Map<String, Object> expectedSnapshot = new HashMap<>();
        expectedSnapshot.put("id", 123);
        expectedSnapshot.put("attr", 42);

        assertEquals(expectedSnapshot, qualifierSnapshot);
        assertTrue(qualifier.isUsingOptimisticLocking());

        qualifierSnapshot = qualifier.getSnapshot();
        assertEquals(expectedSnapshot, qualifierSnapshot);
    }

    @Test
    public void optimisticQualifier() {
        ObjectId id = ObjectId.of("test", "id", 123);
        Persistent persistent = TestObjects.modifiedObject(id);
        DbRowOp row = row(persistent);

        Qualifier qualifier = new Qualifier(row);
        qualifier.addAdditionalQualifier(CompiledTables.column("attr"), 42, true);

        Map<String, Object> qualifierSnapshot = qualifier.getSnapshot();

        Map<String, Object> expectedSnapshot = new HashMap<>();
        expectedSnapshot.put("id", 123);
        expectedSnapshot.put("attr", 42);

        assertEquals(expectedSnapshot, qualifierSnapshot);
        assertTrue(qualifier.isUsingOptimisticLocking());

        qualifierSnapshot = qualifier.getSnapshot();
        assertEquals(expectedSnapshot, qualifierSnapshot);
    }

    private DbRowOp row(Persistent persistent) {
        return new UpdateDbRowOp(persistent, CompiledTables.table("TEST"), persistent.getObjectId());
    }
}