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

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.testdo.hybrid.HybridEntity1;
import org.apache.cayenne.testdo.hybrid.HybridEntity2;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.apache.cayenne.util.Util;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @since 4.1
 */
@UseCayenneRuntime(CayenneProjects.HYBRID_DATA_OBJECT_PROJECT)
public class HybridPersistentObjectIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Inject
    private CayenneRuntime runtime;

    @Test
    public void testCreateNew() {
        HybridEntity1 entity1 = context.newObject(HybridEntity1.class);
        HybridEntity2 entity2 = context.newObject(HybridEntity2.class);
        context.commitChanges();

        assertNull(entity1.values);
        assertNull(entity2.values);

        HybridEntity1 selectEntity1 = ObjectSelect.query(HybridEntity1.class).selectOne(context);
        assertEquals(0, selectEntity1.getIntField());
        assertEquals(null, selectEntity1.getStrField());

        HybridEntity2 selectEntity2 = ObjectSelect.query(HybridEntity2.class).selectOne(context);
        assertEquals(0, selectEntity2.getIntField());
        assertEquals(null, selectEntity2.getStrField());
    }

    @Test
    public void testSetFieldAttributes() {
        HybridEntity1 entity1 = context.newObject(HybridEntity1.class);
        entity1.setIntField(123);
        entity1.setStrField("abc");

        HybridEntity2 entity2 = context.newObject(HybridEntity2.class);
        entity2.setIntField(321);
        entity2.setStrField("cba");
        entity2.setHybridEntity1(entity1);

        assertNull(entity1.values);
        assertNull(entity2.values);

        context.commitChanges();

        HybridEntity1 selectEntity1 = ObjectSelect.query(HybridEntity1.class).selectOne(context);
        assertEquals(123, selectEntity1.getIntField());
        assertEquals("abc", selectEntity1.getStrField());

        HybridEntity2 selectEntity2 = ObjectSelect.query(HybridEntity2.class).selectOne(context);
        assertEquals(321, selectEntity2.getIntField());
        assertEquals("cba", selectEntity2.getStrField());
        assertEquals(selectEntity1, selectEntity2.getHybridEntity1());
    }

    @Test
    public void testSetDynamicDbAttributes() {
        // add attributes that in DbEntity but not mapped yet
        addRuntimeAttribute(HybridEntity1.class, "FLOAT_FIELD", "double");
        addRuntimeAttribute(HybridEntity2.class, "BOOLEAN_FIELD", "boolean");

        try {
            HybridEntity1 entity1 = context.newObject(HybridEntity1.class);
            entity1.writeProperty("FLOAT_FIELD", 3.14);

            HybridEntity2 entity2 = context.newObject(HybridEntity2.class);
            entity2.writeProperty("BOOLEAN_FIELD", true);

            assertNotNull(entity1.values);
            assertNotNull(entity2.values);

            context.commitChanges();

            entity1.writeProperty("FLOAT_FIELD", 2.17);
            entity2.writeProperty("BOOLEAN_FIELD", false);

            // attributes should be merged with context cache
            HybridEntity1 selectEntity1 = ObjectSelect.query(HybridEntity1.class).selectOne(context);
            assertEquals(2.17, selectEntity1.readProperty("FLOAT_FIELD"));

            HybridEntity2 selectEntity2 = ObjectSelect.query(HybridEntity2.class).selectOne(context);
            assertEquals(false, selectEntity2.readProperty("BOOLEAN_FIELD"));

            // attributes should be read from DB
            ObjectContext cleanContext = runtime.newContext();
            HybridEntity1 selectCleanEntity1 = ObjectSelect.query(HybridEntity1.class).selectOne(cleanContext);
            assertEquals(3.14, selectCleanEntity1.readProperty("FLOAT_FIELD"));

            HybridEntity2 selectCleanEntity2 = ObjectSelect.query(HybridEntity2.class).selectOne(cleanContext);
            assertEquals(true, selectCleanEntity2.readProperty("BOOLEAN_FIELD"));
        } finally {
            removeRuntimeAttribute(HybridEntity1.class, "FLOAT_FIELD");
            removeRuntimeAttribute(HybridEntity2.class, "BOOLEAN_FIELD");
        }
    }

    @Test
    public void testSetDynamicNonDbAttributes() {
        // test write arbitrary data into object
        HybridEntity1 entity1 = context.newObject(HybridEntity1.class);
        entity1.writeProperty("CUSTOM_NON_DB_ATTRIBUTE", 42L);
        assertEquals(42L, entity1.readProperty("CUSTOM_NON_DB_ATTRIBUTE"));
        assertNotNull(entity1.values);

        context.commitChanges();

        entity1.writeProperty("CUSTOM_NON_DB_ATTRIBUTE", 12L);

        HybridEntity1 selectEntity1 = ObjectSelect.query(HybridEntity1.class).selectOne(context);
        // this will be restored from context cache
        assertEquals(12L, selectEntity1.readProperty("CUSTOM_NON_DB_ATTRIBUTE"));

        ObjectContext cleanContext = runtime.newContext();

        HybridEntity1 selectCleanEntity1 = ObjectSelect.query(HybridEntity1.class).selectOne(cleanContext);
        // this will be read from db only
        assertEquals(null, selectCleanEntity1.readProperty("CUSTOM_NON_DB_ATTRIBUTE"));
    }

    @Test
    public void testSerialization() throws Exception {

        HybridEntity1 entity1 = new HybridEntity1();
        entity1.setIntField(123);
        entity1.setStrField("abc");
        entity1.writeProperty("CUSTOM_PROPERTY", 3.14);

        HybridEntity1 clonedEntity1 = Util.cloneViaSerialization(entity1);

        assertEquals(123, clonedEntity1.getIntField());
        assertEquals("abc", clonedEntity1.getStrField());
        assertEquals(3.14, clonedEntity1.readProperty("CUSTOM_PROPERTY"));
    }

    @Test
    public void testDirectPropertyWrite() throws Exception {
        HybridEntity1 entity1 = new HybridEntity1();

        HybridEntity2 entity2 = new HybridEntity2();
        entity2.writePropertyDirectly("intField", 123);
        entity2.writePropertyDirectly("strField", "abc");
        assertNull(entity2.values);

        entity2.writePropertyDirectly("CUSTOM_PROPERTY", 3.14);
        entity2.writePropertyDirectly("hybridEntity1", entity1);
        assertNotNull(entity2.values);

        assertEquals(123, entity2.readPropertyDirectly("intField"));
        assertEquals("abc", entity2.readPropertyDirectly("strField"));
        assertEquals(3.14, entity2.readPropertyDirectly("CUSTOM_PROPERTY"));
        assertEquals(entity1, entity2.readPropertyDirectly("hybridEntity1"));
    }

    private void addRuntimeAttribute(Class<?> entityClass, String attributeName, String attributeType) {
        ObjEntity entity = runtime.getDataDomain().getEntityResolver().getObjEntity(entityClass);
        ObjAttribute attribute = new ObjAttribute();
        attribute.setName(attributeName);
        attribute.setDbAttributePath(attributeName);
        attribute.setType(attributeType);
        entity.addAttribute(attribute);
    }

    private void removeRuntimeAttribute(Class<?> entityClass, String attributeName) {
        ObjEntity entity = runtime.getDataDomain().getEntityResolver().getObjEntity(entityClass);
        entity.removeAttribute(attributeName);
    }
}
