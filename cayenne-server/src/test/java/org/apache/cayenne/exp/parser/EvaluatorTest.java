/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.exp.parser;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;

import junit.framework.TestCase;

public class EvaluatorTest extends TestCase {

    public void testEvaluator_Null() {
        Evaluator e = Evaluator.evaluator(null);
        assertNotNull(e);
        assertTrue(e.eq(null, null));
        assertFalse(e.eq(null, new Object()));
    }

    public void testEvaluator_Object() {
        Object o = new Object();
        Evaluator e = Evaluator.evaluator(o);
        assertNotNull(e);
        assertTrue(e.eq(o, o));
        assertFalse(e.eq(o, null));
    }

    public void testEvaluator_Number() {

        Evaluator e = Evaluator.evaluator(1);
        assertNotNull(e);
        assertTrue(e.eq(1, 1));
        assertFalse(e.eq(1, null));
        assertFalse(e.eq(1, 5));
        assertFalse(e.eq(1, 1.1));
    }

    public void testEvaluator_BigDecimal() {
        Object lhs = new BigDecimal("1.10");
        Evaluator e = Evaluator.evaluator(lhs);
        assertNotNull(e);
        assertTrue(e.eq(lhs, new BigDecimal("1.1")));
        assertFalse(e.eq(lhs, new BigDecimal("1.10001")));

        Integer c = e.compare(lhs, new BigDecimal("1.10001"));
        assertEquals(-1, c.intValue());
    }

    public void testEvaluator_Persistent() {

        ObjectId lhsId = new ObjectId("X", "k", 3);
        Persistent lhs = mock(Persistent.class);
        when(lhs.getObjectId()).thenReturn(lhsId);

        Evaluator e = Evaluator.evaluator(lhs);
        assertNotNull(e);

        ObjectId rhsId1 = new ObjectId("X", "k", 3);
        Persistent rhs1 = mock(Persistent.class);
        when(rhs1.getObjectId()).thenReturn(rhsId1);

        assertTrue(e.eq(lhs, rhs1));
        assertTrue(e.eq(lhs, rhsId1));
        assertTrue(e.eq(lhs, 3));

        ObjectId rhsId2 = new ObjectId("X", "k", 4);
        Persistent rhs2 = mock(Persistent.class);
        when(rhs2.getObjectId()).thenReturn(rhsId2);

        assertFalse(e.eq(lhs, rhs2));
        assertFalse(e.eq(lhs, rhsId2));
        assertFalse(e.eq(lhs, 4));
    }

    public void testEvaluator_Persistent_StringId() {

        ObjectId lhsId = new ObjectId("X", "k", "A");
        Persistent lhs = mock(Persistent.class);
        when(lhs.getObjectId()).thenReturn(lhsId);

        Evaluator e = Evaluator.evaluator(lhs);
        assertNotNull(e);

        ObjectId rhsId1 = new ObjectId("X", "k", "A");
        Persistent rhs1 = mock(Persistent.class);
        when(rhs1.getObjectId()).thenReturn(rhsId1);

        assertTrue(e.eq(lhs, rhs1));
        assertTrue(e.eq(lhs, rhsId1));
        assertTrue(e.eq(lhs, "A"));

        ObjectId rhsId2 = new ObjectId("X", "k", "B");
        Persistent rhs2 = mock(Persistent.class);
        when(rhs2.getObjectId()).thenReturn(rhsId2);

        assertFalse(e.eq(lhs, rhs2));
        assertFalse(e.eq(lhs, rhsId2));
        assertFalse(e.eq(lhs, "B"));
    }

    public void testEvaluator_Persistent_MultiKey() {

        Map<String, Object> lhsIdMap = new HashMap<String, Object>();
        lhsIdMap.put("a", 1);
        lhsIdMap.put("b", "B");
        ObjectId lhsId = new ObjectId("X", lhsIdMap);
        Persistent lhs = mock(Persistent.class);
        when(lhs.getObjectId()).thenReturn(lhsId);

        Evaluator e = Evaluator.evaluator(lhs);
        assertNotNull(e);

        Map<String, Object> rhsId1Map = new HashMap<String, Object>();
        rhsId1Map.put("a", 1);
        rhsId1Map.put("b", "B");
        ObjectId rhsId1 = new ObjectId("X", rhsId1Map);
        Persistent rhs1 = mock(Persistent.class);
        when(rhs1.getObjectId()).thenReturn(rhsId1);

        assertTrue(e.eq(lhs, rhs1));
        assertTrue(e.eq(lhs, rhsId1));
        assertTrue(e.eq(lhs, rhsId1Map));

        Map<String, Object> rhsId2Map = new HashMap<String, Object>();
        rhsId2Map.put("a", 1);
        rhsId2Map.put("b", "BX");
        ObjectId rhsId2 = new ObjectId("X", rhsId2Map);
        Persistent rhs2 = mock(Persistent.class);
        when(rhs2.getObjectId()).thenReturn(rhsId2);

        assertFalse(e.eq(lhs, rhs2));
        assertFalse(e.eq(lhs, rhsId2));
        assertFalse(e.eq(lhs, rhsId2Map));
        assertFalse(e.eq(lhs, "B"));
    }
}
