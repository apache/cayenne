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

package org.apache.cayenne.map;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 */
public class DbJoinTest {

    @Test
    public void testRelationship() throws Exception {
        DbJoin join = new DbJoin(null);
        assertNull(join.getRelationship());

        DbRelationship relationship = new DbRelationship("abc");
        join.setRelationship(relationship);
        assertSame(relationship, join.getRelationship());
    }

    @Test
    public void testToString() {
        DbJoin join = new DbJoin();
        join.setSourceName("X");
        join.setTargetName("Y");

        String string = join.toString();

        assertTrue(string, string.startsWith("org.apache.cayenne.map.DbJoin@"));
        assertTrue(string, string.endsWith("[source=X,target=Y]"));
    }

}
