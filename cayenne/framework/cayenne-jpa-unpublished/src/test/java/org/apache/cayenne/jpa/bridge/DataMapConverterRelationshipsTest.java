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
package org.apache.cayenne.jpa.bridge;

import junit.framework.TestCase;

import org.apache.cayenne.jpa.MockPersistenceUnitInfo;
import org.apache.cayenne.jpa.bridge.entity.Entity1;
import org.apache.cayenne.jpa.bridge.entity.Entity2;
import org.apache.cayenne.jpa.bridge.entity.Entity3;
import org.apache.cayenne.jpa.bridge.entity.Entity4;
import org.apache.cayenne.jpa.conf.EntityMapAnnotationLoader;
import org.apache.cayenne.jpa.conf.EntityMapDefaultsProcessor;
import org.apache.cayenne.jpa.conf.EntityMapLoaderContext;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

public class DataMapConverterRelationshipsTest extends TestCase {

    public void testBidiOM() throws Exception {
        DataMap dataMap2 = load(Entity2.class, Entity1.class);
        bidiOMAssert(dataMap2);

        // note that CAY-860 problem (missing joins) is conditional on the entity load
        // order, so now reverse the order ...
        DataMap dataMap1 = load(Entity1.class, Entity2.class);
        bidiOMAssert(dataMap1);
    }

    public void testMapOM() throws Exception {
        DataMap dataMap = load(Entity3.class, Entity4.class);

        ObjEntity e3 = dataMap.getObjEntity("Entity3");
        ObjEntity e4 = dataMap.getObjEntity("Entity4");

        assertEquals(1, e3.getRelationships().size());
        assertEquals(1, e4.getRelationships().size());

        ObjRelationship or3 = (ObjRelationship) e3.getRelationship("entity4");
        ObjRelationship or4 = (ObjRelationship) e4.getRelationship("entity3s");

        assertEquals("entity4", or3.getDbRelationshipPath());
        assertEquals("entity3s", or4.getDbRelationshipPath());

        assertEquals("Entity4", or3.getTargetEntityName());
        assertEquals("Entity3", or4.getTargetEntityName());

        assertEquals("key", or4.getMapKey());
        assertEquals("java.util.Map", or4.getCollectionType());

        DbEntity db3 = dataMap.getDbEntity("Entity3");
        DbEntity db4 = dataMap.getDbEntity("Entity4");

        assertEquals(3, db3.getAttributes().size());
        assertEquals(1, db4.getAttributes().size());

        assertEquals(1, db3.getRelationships().size());
        assertEquals(1, db4.getRelationships().size());

        DbRelationship dbr3 = (DbRelationship) db3.getRelationship("entity4");
        DbRelationship dbr4 = (DbRelationship) db4.getRelationship("entity3s");

        assertEquals(1, dbr3.getJoins().size());
        assertEquals(1, dbr4.getJoins().size());

        assertSame(dbr4, dbr3.getReverseRelationship());
        assertSame(dbr3, dbr4.getReverseRelationship());
    }

    private void bidiOMAssert(DataMap dataMap) throws Exception {

        ObjEntity e1 = dataMap.getObjEntity("Entity1");
        ObjEntity e2 = dataMap.getObjEntity("Entity2");

        assertEquals(1, e1.getRelationships().size());
        assertEquals(1, e2.getRelationships().size());

        DbEntity db1 = dataMap.getDbEntity("Entity1");
        DbEntity db2 = dataMap.getDbEntity("Entity2");

        assertEquals(2, db1.getAttributes().size());
        assertEquals(1, db2.getAttributes().size());

        assertEquals(1, db1.getRelationships().size());
        assertEquals(1, db2.getRelationships().size());

        DbRelationship dbr1 = (DbRelationship) db1.getRelationship("entity2");
        DbRelationship dbr2 = (DbRelationship) db2.getRelationship("entity1s");

        assertEquals(1, dbr1.getJoins().size());
        assertEquals(1, dbr2.getJoins().size());

        assertSame(dbr2, dbr1.getReverseRelationship());
        assertSame(dbr1, dbr2.getReverseRelationship());
    }

    private DataMap load(Class<?>... classes) {
        EntityMapLoaderContext context = new EntityMapLoaderContext(
                new MockPersistenceUnitInfo());
        EntityMapAnnotationLoader loader = new EntityMapAnnotationLoader(context);

        for (Class<?> c : classes) {
            loader.loadClassMapping(c);
        }

        new EntityMapDefaultsProcessor().applyDefaults(context);
        assertFalse("Found conflicts: " + context.getConflicts(), context
                .getConflicts()
                .hasFailures());

        DataMap dataMap = new DataMapConverter().toDataMap("n1", context);
        assertFalse("Found DataMap conflicts: " + context.getConflicts(), context
                .getConflicts()
                .hasFailures());
        return dataMap;
    }
}
