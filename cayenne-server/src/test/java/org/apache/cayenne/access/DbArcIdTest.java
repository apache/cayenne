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
package org.apache.cayenne.access;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.map.DbRelationship;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class DbArcIdTest {

    @Test
    public void testHashCode() {

        DbArcId id1 = new DbArcId(ObjectId.of("x", "k", "v"),
                new DbRelationship("r1"));
        int h1 = id1.hashCode();
        assertEquals(h1, id1.hashCode());
        assertEquals(h1, id1.hashCode());

        DbArcId id1_eq = new DbArcId(ObjectId.of("x", "k", "v"),
                new DbRelationship("r1"));
        assertEquals(h1, id1_eq.hashCode());

        DbArcId id2 = new DbArcId(ObjectId.of("x", "k", "v"),
                new DbRelationship("r2"));
        assertNotEquals(h1, id2.hashCode());

        DbArcId id3 = new DbArcId(ObjectId.of("y", "k", "v"),
                new DbRelationship("r1"));
        assertNotEquals(h1, id3.hashCode());
    }

    @Test
    public void testEquals() {

        DbArcId id1 = new DbArcId(ObjectId.of("x", "k", "v"),
                new DbRelationship("r1"));
        assertEquals(id1, id1);

        DbArcId id1_eq = new DbArcId(ObjectId.of("x", "k", "v"),
                new DbRelationship("r1"));
        assertEquals(id1, id1_eq);
        assertEquals(id1_eq, id1);

        DbArcId id2 = new DbArcId(ObjectId.of("x", "k", "v"),
                new DbRelationship("r2"));
        assertNotEquals(id1, id2);

        DbArcId id3 = new DbArcId(ObjectId.of("y", "k", "v"),
                new DbRelationship("r1"));
        assertNotEquals(id1, id3);

        assertNotEquals(id1, new Object());
    }
}
