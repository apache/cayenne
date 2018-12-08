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

package org.apache.cayenne;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class DataRowTest {

    @Test
    public void testVersion() throws Exception {
        DataRow s1 = new DataRow(10);
        DataRow s2 = new DataRow(10);
        DataRow s3 = new DataRow(10);
        assertFalse(s1.getVersion() == s2.getVersion());
        assertFalse(s2.getVersion() == s3.getVersion());
        assertFalse(s3.getVersion() == s1.getVersion());
    }

    @Test
    public void testEquals(){
        DataRow d1 = new DataRow(1);
        d1.put("FIELD", "test".getBytes());

        assertTrue(d1.equals(d1));

        DataRow d2 = new DataRow(1);
        d2.put("FIELD", "test".getBytes());

        assertTrue(d1.equals(d2));
        assertTrue(d2.equals(d1));

        DataRow d3 = new DataRow(1);
        d3.put("FIELD", "test".getBytes());

        assertTrue(d2.equals(d3));
        assertTrue(d1.equals(d3));

        assertFalse(d1.equals(null));

        DataRow d4 = new DataRow(1);
        d4.put("FIELD1", "test".getBytes());

        for(int i = 0; i < 5; i++) {
            assertFalse(d3.equals(d4));
        }

        DataRow d5 = new DataRow(1);
        d5.put("FIELD", "test1".getBytes());

        DataRow d6 = new DataRow(1);
        d6.put("FIELD", "test".getBytes());

        assertFalse(d5.equals(d6));
    }

    @Test
    public void testHashCode(){
        DataRow d1 = new DataRow(1);
        d1.put("FIELD", "test".getBytes());

        assertEquals(d1.hashCode(), d1.hashCode());

        DataRow d2 = new DataRow(1);
        d2.put("FIELD", "test".getBytes());

        assertTrue(d1.equals(d2));
        assertEquals(d1.hashCode(), d2.hashCode());
    }
}
