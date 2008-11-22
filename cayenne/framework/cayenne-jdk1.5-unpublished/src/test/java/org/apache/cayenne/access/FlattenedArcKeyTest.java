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
import org.apache.cayenne.map.MockObjRelationship;

/**
 */
public class FlattenedArcKeyTest extends TestCase {

    public void testAttributes() {
        ObjectId src = new ObjectId("X");
        ObjectId target = new ObjectId("Y");
        MockObjRelationship r1 = new MockObjRelationship("r1");
        r1.setReverseRelationship(new MockObjRelationship("r2"));

        FlattenedArcKey update = new FlattenedArcKey(src, target, r1);

        assertSame(src, update.sourceId);
        assertSame(target, update.destinationId);
        assertSame(r1, update.relationship);
        assertSame(r1.getReverseRelationship(), update.reverseRelationship);
        assertTrue(update.isBidirectional());
    }

    public void testEquals() {
        ObjectId src = new ObjectId("X");
        ObjectId target = new ObjectId("Y");
        MockObjRelationship r1 = new MockObjRelationship("r1");
        r1.setReverseRelationship(new MockObjRelationship("r2"));

        FlattenedArcKey update = new FlattenedArcKey(src, target, r1);
        FlattenedArcKey update1 = new FlattenedArcKey(target, src, r1
                .getReverseRelationship());

        FlattenedArcKey update2 = new FlattenedArcKey(
                target,
                src,
                new MockObjRelationship("r3"));

        assertTrue(update.equals(update1));
        assertFalse(update.equals(update2));
    }
}
