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
package org.apache.cayenne.access;

import junit.framework.TestCase;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.map.DbRelationship;

public class DbArcIdTest extends TestCase {

    public void testHashCode() {

        DbArcId id1 = new DbArcId(new ObjectId("x", "k", "v"),
                new DbRelationship("r1"));
        int h1 = id1.hashCode();
        assertEquals(h1, id1.hashCode());
        assertEquals(h1, id1.hashCode());

        DbArcId id1_eq = new DbArcId(new ObjectId("x", "k", "v"),
                new DbRelationship("r1"));
        assertEquals(h1, id1_eq.hashCode());

        DbArcId id2 = new DbArcId(new ObjectId("x", "k", "v"),
                new DbRelationship("r2"));
        assertFalse(h1 == id2.hashCode());

        DbArcId id3 = new DbArcId(new ObjectId("y", "k", "v"),
                new DbRelationship("r1"));
        assertFalse(h1 == id3.hashCode());
    }

    public void testEquals() {

        DbArcId id1 = new DbArcId(new ObjectId("x", "k", "v"),
                new DbRelationship("r1"));
        assertTrue(id1.equals(id1));

        DbArcId id1_eq = new DbArcId(new ObjectId("x", "k", "v"),
                new DbRelationship("r1"));
        assertTrue(id1.equals(id1_eq));
        assertTrue(id1_eq.equals(id1));

        DbArcId id2 = new DbArcId(new ObjectId("x", "k", "v"),
                new DbRelationship("r2"));
        assertFalse(id1.equals(id2));

        DbArcId id3 = new DbArcId(new ObjectId("y", "k", "v"),
                new DbRelationship("r1"));
        assertFalse(id1.equals(id3));

        assertFalse(id1.equals(new Object()));
    }
}
