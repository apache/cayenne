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

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataRowTest {

    @Test
    public void version() throws Exception {
        DataRow s1 = new DataRow(10);
        DataRow s2 = new DataRow(10);
        DataRow s3 = new DataRow(10);
        assertFalse(s1.getVersion() == s2.getVersion());
        assertFalse(s2.getVersion() == s3.getVersion());
        assertFalse(s3.getVersion() == s1.getVersion());
    }

    @Test
    public void nextVersionGrows() {
        long v1 = DataRow.nextVersion();
        long v2 = DataRow.nextVersion();
        assertTrue(v2 > v1);
        assertTrue(new DataRow(10).getVersion() > v2);
    }

    @Test
    public void explicitVersion() {
        long version = DataRow.nextVersion();
        DataRow s1 = new DataRow(10, version);
        s1.setEntityName("E");
        DataRow s2 = new DataRow(s1, version);
        assertEquals(version, s1.getVersion());
        assertEquals(version, s2.getVersion());
        assertNull(s2.getEntityName());
    }

    @Test
    public void diffHasNewerRowVersion() {
        DataRow old = new DataRow(Map.of("a", 1, "b", 2), DataRow.nextVersion());
        DataRow newer = new DataRow(Map.of("a", 1, "b", 3), DataRow.nextVersion());
        DataRow diff = old.createDiff(newer);
        assertEquals(Map.of("b", 3), diff);
        assertEquals(newer.getVersion(), diff.getVersion());
        assertNull(old.createDiff(new DataRow(old, DataRow.nextVersion())));
    }

}
