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
package org.apache.cayenne.exp.parser;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EvaluatorTest {

    @Test
    public void testEvaluator_Null() {
        Evaluator e = Evaluator.evaluator(null);
        assertNotNull(e);
        assertTrue(e.eq(null, null));
        assertFalse(e.eq(null, new Object()));
    }

    @Test
    public void testEvaluator_Object() {
        Object o = new Object();
        Evaluator e = Evaluator.evaluator(o);
        assertNotNull(e);
        assertTrue(e.eq(o, o));
        assertFalse(e.eq(o, null));
    }

    @Test
    public void testEvaluator_Number() {

        Evaluator e = Evaluator.evaluator(1);
        assertNotNull(e);
        assertTrue(e.eq(1, 1));
        assertFalse(e.eq(1, null));
        assertFalse(e.eq(1, 5));
        assertFalse(e.eq(1, 1.1));
    }

	@Test
    public void testEvaluator_NumberWideningEquals() {
        Evaluator e = Evaluator.evaluator(1);

        assertTrue(e.eq((byte)1, (byte)1));
        assertTrue(e.eq((byte)1, (short)1));
        assertTrue(e.eq((byte)1, (int)1));
        assertTrue(e.eq((byte)1, (long)1));
        assertTrue(e.eq((byte)1, (float)1));
        assertTrue(e.eq((byte)1, (double)1));

        assertTrue(e.eq((short)1, (byte)1));
        assertTrue(e.eq((short)1, (short)1));
        assertTrue(e.eq((short)1, (int)1));
        assertTrue(e.eq((short)1, (long)1));
        assertTrue(e.eq((short)1, (float)1));
        assertTrue(e.eq((short)1, (double)1));

        assertTrue(e.eq((int)1, (byte)1));
        assertTrue(e.eq((int)1, (short)1));
        assertTrue(e.eq((int)1, (int)1));
        assertTrue(e.eq((int)1, (long)1));
        assertTrue(e.eq((int)1, (float)1));
        assertTrue(e.eq((int)1, (double)1));
        
        assertTrue(e.eq((long)1, (byte)1));
        assertTrue(e.eq((long)1, (short)1));
        assertTrue(e.eq((long)1, (int)1));
        assertTrue(e.eq((long)1, (long)1));
        assertTrue(e.eq((long)1, (float)1));
        assertTrue(e.eq((long)1, (double)1));
        
        assertTrue(e.eq((float)1, (byte)1));
        assertTrue(e.eq((float)1, (short)1));
        assertTrue(e.eq((float)1, (int)1));
        assertTrue(e.eq((float)1, (long)1));
        assertTrue(e.eq((float)1, (float)1));
        assertTrue(e.eq((float)1, (double)1));
        
        assertTrue(e.eq((double)1, (byte)1));
        assertTrue(e.eq((double)1, (short)1));
        assertTrue(e.eq((double)1, (int)1));
        assertTrue(e.eq((double)1, (long)1));
        assertTrue(e.eq((double)1, (float)1));
        assertTrue(e.eq((double)1, (double)1));
        
        assertTrue(e.eq((float)1.1, (float)1.1));
        assertTrue(e.eq((float)1.1, (double)1.1));
        
        assertTrue(e.eq(Long.MAX_VALUE, Long.MAX_VALUE));
        assertTrue(e.eq(Double.MAX_VALUE, Double.MAX_VALUE));
        
        assertTrue(e.eq((int)1, new AtomicInteger(1)));
        assertTrue(e.eq(new AtomicInteger(1), (int)1));
        
        assertTrue(e.eq((int)1, new AtomicLong(1)));
        assertTrue(e.eq(new AtomicLong(1), (int)1));
        
        assertTrue(e.eq((int)1, BigInteger.ONE));
        assertTrue(e.eq(BigInteger.ONE, (int)1));
        
        BigInteger bigInt = new BigInteger(Long.valueOf(Long.MAX_VALUE).toString() + "0");
        assertTrue(e.eq(bigInt, bigInt));
    }
    
	@Test
    public void testEvaluator_NumberWideningCompare() {
        Evaluator e = Evaluator.evaluator(1);

        assertTrue(e.compare((byte)1, (byte)1) == 0);
        assertTrue(e.compare((byte)1, (short)1) == 0);
        assertTrue(e.compare((byte)1, (int)1) == 0);
        assertTrue(e.compare((byte)1, (long)1) == 0);
        assertTrue(e.compare((byte)1, (float)1) == 0);
        assertTrue(e.compare((byte)1, (double)1) == 0);

        assertTrue(e.compare((short)1, (byte)1) == 0);
        assertTrue(e.compare((short)1, (short)1) == 0);
        assertTrue(e.compare((short)1, (int)1) == 0);
        assertTrue(e.compare((short)1, (long)1) == 0);
        assertTrue(e.compare((short)1, (float)1) == 0);
        assertTrue(e.compare((short)1, (double)1) == 0);

        assertTrue(e.compare((int)1, (byte)1) == 0);
        assertTrue(e.compare((int)1, (short)1) == 0);
        assertTrue(e.compare((int)1, (int)1) == 0);
        assertTrue(e.compare((int)1, (long)1) == 0);
        assertTrue(e.compare((int)1, (float)1) == 0);
        assertTrue(e.compare((int)1, (double)1) == 0);
        
        assertTrue(e.compare((long)1, (byte)1) == 0);
        assertTrue(e.compare((long)1, (short)1) == 0);
        assertTrue(e.compare((long)1, (int)1) == 0);
        assertTrue(e.compare((long)1, (long)1) == 0);
        assertTrue(e.compare((long)1, (float)1) == 0);
        assertTrue(e.compare((long)1, (double)1) == 0);
        
        assertTrue(e.compare((float)1, (byte)1) == 0);
        assertTrue(e.compare((float)1, (short)1) == 0);
        assertTrue(e.compare((float)1, (int)1) == 0);
        assertTrue(e.compare((float)1, (long)1) == 0);
        assertTrue(e.compare((float)1, (float)1) == 0);
        assertTrue(e.compare((float)1, (double)1) == 0);
        
        assertTrue(e.compare((double)1, (byte)1) == 0);
        assertTrue(e.compare((double)1, (short)1) == 0);
        assertTrue(e.compare((double)1, (int)1) == 0);
        assertTrue(e.compare((double)1, (long)1) == 0);
        assertTrue(e.compare((double)1, (float)1) == 0);
        assertTrue(e.compare((double)1, (double)1) == 0);
        
        assertTrue(e.compare((float)1.1, (float)1.1) == 0);
        assertTrue(e.compare((float)1.1, (double)1.1) == 0);
        
        assertTrue(e.compare(Long.MAX_VALUE, Long.MAX_VALUE) == 0);
        assertTrue(e.compare(Double.MAX_VALUE, Double.MAX_VALUE) == 0);
        
        assertTrue(e.compare((int)1, new AtomicInteger(1)) == 0);
        assertTrue(e.compare(new AtomicInteger(1), (int)1) == 0);
        
        assertTrue(e.compare((int)1, new AtomicLong(1)) == 0);
        assertTrue(e.compare(new AtomicLong(1), (int)1) == 0);
        
        assertTrue(e.compare((int)1, BigInteger.ONE) == 0);
        assertTrue(e.compare(BigInteger.ONE, (int)1) == 0);
        
        BigInteger bigInt = new BigInteger(Long.valueOf(Long.MAX_VALUE).toString() + "0");
        assertTrue(e.compare(bigInt, bigInt) == 0);
    }
    
    @Test
    public void testEvaluator_BigDecimal() {
        Object lhs = new BigDecimal("1.10");
        Evaluator e = Evaluator.evaluator(lhs);
        assertNotNull(e);
        assertTrue(e.eq(lhs, new BigDecimal("1.1")));
        assertFalse(e.eq(lhs, new BigDecimal("1.10001")));

        Integer c = e.compare(lhs, new BigDecimal("1.10001"));
        assertEquals(-1, c.intValue());
    }

    @Test
    public void testEvaluator_Persistent() {

        ObjectId lhsId = ObjectId.of("X", "k", 3);
        Persistent lhs = mock(Persistent.class);
        when(lhs.getObjectId()).thenReturn(lhsId);

        Evaluator e = Evaluator.evaluator(lhs);
        assertNotNull(e);

        ObjectId rhsId1 = ObjectId.of("X", "k", 3);
        Persistent rhs1 = mock(Persistent.class);
        when(rhs1.getObjectId()).thenReturn(rhsId1);

        assertTrue(e.eq(lhs, rhs1));
        assertTrue(e.eq(lhs, rhsId1));
        assertTrue(e.eq(lhs, 3));

        ObjectId rhsId2 = ObjectId.of("X", "k", 4);
        Persistent rhs2 = mock(Persistent.class);
        when(rhs2.getObjectId()).thenReturn(rhsId2);

        assertFalse(e.eq(lhs, rhs2));
        assertFalse(e.eq(lhs, rhsId2));
        assertFalse(e.eq(lhs, 4));
    }

    @Test
    public void testEvaluator_Persistent_StringId() {

        ObjectId lhsId = ObjectId.of("X", "k", "A");
        Persistent lhs = mock(Persistent.class);
        when(lhs.getObjectId()).thenReturn(lhsId);

        Evaluator e = Evaluator.evaluator(lhs);
        assertNotNull(e);

        ObjectId rhsId1 = ObjectId.of("X", "k", "A");
        Persistent rhs1 = mock(Persistent.class);
        when(rhs1.getObjectId()).thenReturn(rhsId1);

        assertTrue(e.eq(lhs, rhs1));
        assertTrue(e.eq(lhs, rhsId1));
        assertTrue(e.eq(lhs, "A"));

        ObjectId rhsId2 = ObjectId.of("X", "k", "B");
        Persistent rhs2 = mock(Persistent.class);
        when(rhs2.getObjectId()).thenReturn(rhsId2);

        assertFalse(e.eq(lhs, rhs2));
        assertFalse(e.eq(lhs, rhsId2));
        assertFalse(e.eq(lhs, "B"));
    }

    @Test
    public void testEvaluator_Persistent_MultiKey() {

        Map<String, Object> lhsIdMap = new HashMap<>();
        lhsIdMap.put("a", 1);
        lhsIdMap.put("b", "B");
        ObjectId lhsId = ObjectId.of("X", lhsIdMap);
        Persistent lhs = mock(Persistent.class);
        when(lhs.getObjectId()).thenReturn(lhsId);

        Evaluator e = Evaluator.evaluator(lhs);
        assertNotNull(e);

        Map<String, Object> rhsId1Map = new HashMap<>();
        rhsId1Map.put("a", 1);
        rhsId1Map.put("b", "B");
        ObjectId rhsId1 = ObjectId.of("X", rhsId1Map);
        Persistent rhs1 = mock(Persistent.class);
        when(rhs1.getObjectId()).thenReturn(rhsId1);

        assertTrue(e.eq(lhs, rhs1));
        assertTrue(e.eq(lhs, rhsId1));
        assertTrue(e.eq(lhs, rhsId1Map));

        Map<String, Object> rhsId2Map = new HashMap<>();
        rhsId2Map.put("a", 1);
        rhsId2Map.put("b", "BX");
        ObjectId rhsId2 = ObjectId.of("X", rhsId2Map);
        Persistent rhs2 = mock(Persistent.class);
        when(rhs2.getObjectId()).thenReturn(rhsId2);

        assertFalse(e.eq(lhs, rhs2));
        assertFalse(e.eq(lhs, rhsId2));
        assertFalse(e.eq(lhs, rhsId2Map));
        assertFalse(e.eq(lhs, "B"));
    }
}
