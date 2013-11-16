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

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.testdo.relationship.FlattenedTest1;
import org.apache.cayenne.testdo.relationship.FlattenedTest3;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.RELATIONSHIPS_PROJECT)
public class FlattenedArcKeyTest extends ServerCase {

    @Inject
    private EntityResolver entityResolver;

    public void testAttributes() {
        ObjectId src = new ObjectId("X");
        ObjectId target = new ObjectId("Y");
        ObjRelationship r1 = entityResolver.getObjEntity(FlattenedTest3.class).getRelationship(
                FlattenedTest3.TO_FT1_PROPERTY);

        FlattenedArcKey update = new FlattenedArcKey(src, target, r1);

        assertSame(src, update.id1.getSourceId());
        assertSame(target, update.id2.getSourceId());
        assertSame(r1, update.relationship);
    }

    public void testHashCode() {
        ObjectId src = new ObjectId("X");
        ObjectId target = new ObjectId("Y");
        ObjRelationship r1 = entityResolver.getObjEntity(FlattenedTest3.class).getRelationship(
                FlattenedTest3.TO_FT1_PROPERTY);

        FlattenedArcKey update = new FlattenedArcKey(src, target, r1);
        FlattenedArcKey update1 = new FlattenedArcKey(target, src, r1.getReverseRelationship());

        ObjRelationship r3 = entityResolver.getObjEntity(FlattenedTest1.class).getRelationship(
                FlattenedTest1.FT3OVER_COMPLEX_PROPERTY);

        FlattenedArcKey update2 = new FlattenedArcKey(target, src, r3);

        int h = update.hashCode();
        int h1 = update1.hashCode();
        int h2 = update2.hashCode();
        assertTrue(h == h1);
        assertTrue(h == update.hashCode());
        assertFalse(h == h2);
    }

    public void testEquals() {
        ObjectId src = new ObjectId("X");
        ObjectId target = new ObjectId("Y");
        ObjRelationship r1 = entityResolver.getObjEntity(FlattenedTest3.class).getRelationship(
                FlattenedTest3.TO_FT1_PROPERTY);

        FlattenedArcKey update = new FlattenedArcKey(src, target, r1);
        FlattenedArcKey update1 = new FlattenedArcKey(target, src, r1.getReverseRelationship());

        ObjRelationship r3 = entityResolver.getObjEntity(FlattenedTest1.class).getRelationship(
                FlattenedTest1.FT3OVER_COMPLEX_PROPERTY);

        FlattenedArcKey update2 = new FlattenedArcKey(target, src, r3);

        assertTrue(update.equals(update1));
        assertFalse(update.equals(update2));
    }
}
