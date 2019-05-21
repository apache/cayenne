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
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 4.2
 */
public class QualifierTest {

    @Test
    public void testScalarObjectIdQualifier() {
        ObjectId id = ObjectId.of("test", "id", 123);
        Persistent persistent = mockObject(id);
        DbRowOp row = mockRow(persistent);

        Qualifier qualifier = new Qualifier(row);
        Map<String, Object> qualifierSnapshot = qualifier.getSnapshot();

        assertEquals(Collections.singletonMap("id", 123), qualifierSnapshot);
        assertFalse(qualifier.isUsingOptimisticLocking());

        qualifierSnapshot = qualifier.getSnapshot();
        assertEquals(Collections.singletonMap("id", 123), qualifierSnapshot);
    }

    @Test
    public void testMapObjectIdQualifier() {
        Map<String, Object> idMap = new HashMap<>();
        idMap.put("id1", 123);
        idMap.put("id2", 321);
        ObjectId id = ObjectId.of("test", idMap);

        Persistent persistent = mockObject(id);
        DbRowOp row = mockRow(persistent);

        Qualifier qualifier = new Qualifier(row);
        Map<String, Object> qualifierSnapshot = qualifier.getSnapshot();
        assertEquals(idMap, qualifierSnapshot);

        qualifierSnapshot = qualifier.getSnapshot();
        assertEquals(idMap, qualifierSnapshot);
    }

    @Test
    public void testAdditionalQualifier() {
        ObjectId id = ObjectId.of("test", "id", 123);
        Persistent persistent = mockObject(id);
        DbRowOp row = mockRow(persistent);

        Qualifier qualifier = new Qualifier(row);
        qualifier.addAdditionalQualifier(new DbAttribute("attr"), 42, true);

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
    public void testOptimisticQualifier() {
        ObjectId id = ObjectId.of("test", "id", 123);
        Persistent persistent = mockObject(id);
        DbRowOp row = mockRow(persistent);

        Qualifier qualifier = new Qualifier(row);
        qualifier.addAdditionalQualifier(new DbAttribute("attr"), 42, true);

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
    public void testSameBatch() {
        ObjectId id1 = ObjectId.of("test", "id", 123);
        Persistent persistent1 = mockObject(id1);
        DbRowOp row1 = mockRow(persistent1);

        Qualifier qualifier1 = new Qualifier(row1);

        ObjectId id2 = ObjectId.of("test", "id", 321);
        Persistent persistent2 = mockObject(id2);
        DbRowOp row2 = mockRow(persistent2);

        Qualifier qualifier2 = new Qualifier(row2);

        assertTrue(qualifier1.isSameBatch(qualifier2));

        ObjectId id3 = ObjectId.of("test", "id", 321);
        Persistent persistent3 = mockObject(id3);
        DbRowOp row3 = mockRow(persistent3);

        Qualifier qualifier3 = new Qualifier(row3);
        qualifier3.addAdditionalQualifier(new DbAttribute("attr"), 42);

        assertFalse(qualifier1.isSameBatch(qualifier3));
    }

    private DbRowOp mockRow(Persistent persistent) {
        DbRowOp row = mock(DbRowOp.class);
        ObjectId objectId = persistent.getObjectId();
        when(row.getChangeId()).thenReturn(objectId);
        when(row.getObject()).thenReturn(persistent);
        when(row.getEntity()).thenReturn(mockEntity());
        return row;
    }

    private Persistent mockObject(ObjectId id) {
        Persistent persistent = mock(Persistent.class);
        when(persistent.getObjectId()).thenReturn(id);
        when(persistent.getPersistenceState()).thenReturn(PersistenceState.MODIFIED);
        return persistent;
    }

    private DbEntity mockEntity() {
        DbAttribute attribute1 = new DbAttribute("id");
        attribute1.setPrimaryKey(true);
        DbAttribute attribute2 = new DbAttribute("attr");
        DbEntity testEntity = new DbEntity("TEST");
        testEntity.addAttribute(attribute1);
        testEntity.addAttribute(attribute2);
        return testEntity;
    }
}