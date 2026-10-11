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

import org.apache.cayenne.access.SnapshotVersion;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class DataRowTest {

    @Test
    public void version() throws Exception {
        DataRow s1 = new DataRow("E", new HashMap<>(10), SnapshotVersion.next());
        DataRow s2 = new DataRow("E", new HashMap<>(10), SnapshotVersion.next());
        DataRow s3 = new DataRow("E", new HashMap<>(10), SnapshotVersion.next());
        assertFalse(s1.getVersion() == s2.getVersion());
        assertFalse(s2.getVersion() == s3.getVersion());
        assertFalse(s3.getVersion() == s1.getVersion());
    }

    @Test
    public void explicitVersion() {
        long version = SnapshotVersion.next();
        DataRow s1 = new DataRow("E", new HashMap<>(10), version);
        s1.put("a", 1);
        DataRow s2 = new DataRow("E2", new HashMap<>(s1), version);
        assertEquals(version, s1.getVersion());
        assertEquals(version, s2.getVersion());
        assertEquals("E", s1.getEntityName());
        assertEquals("E2", s2.getEntityName());
        assertEquals(1, s2.get("a"));
    }
}
