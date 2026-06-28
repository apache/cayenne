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

package org.apache.cayenne.access.jdbc;

import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RSColumnTest {

    @Test
    public void name() {
        RSColumn column = new RSColumn("abc", null, 0, null, null);
        assertEquals("abc", column.name());
    }

    @Test
    public void label() {
        RSColumn column = new RSColumn(null, "abc", 0, null, null);
        assertEquals("abc", column.dataRowKey());
    }

    @Test
    public void equals() {
        // type should be ignored in the comparison
        RSColumn column1 = new RSColumn("n1", "k1", Types.VARCHAR, null, null);
        RSColumn column2 = new RSColumn("n1", "k1", Types.BOOLEAN, null, null);
        RSColumn column3 = new RSColumn("n1", "k3", 0, null, null);

        assertEquals(column1, column2);
        assertFalse(column1.equals(column3));
        assertFalse(column3.equals(column2));
    }

    @Test
    public void hashCodeTest() {
        // type should be ignored in the comparison
        RSColumn column1 = new RSColumn("n1", "k1", Types.VARCHAR, null, null);
        RSColumn column2 = new RSColumn("n1", "k1", Types.BOOLEAN, null, null);
        RSColumn column3 = new RSColumn("n1", "k3", 0, null, null);

        assertEquals(column1.hashCode(), column2.hashCode());

        // this is not really required by the hashcode contract... but just to see that
        // different columns generally end up in different buckets..
        assertTrue(column1.hashCode() != column3.hashCode());
    }
}
