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

package org.apache.cayenne.jpa.conf;

import java.io.Serializable;

import javax.persistence.TemporalType;

import junit.framework.TestCase;

import org.apache.cayenne.jpa.MockPersistenceUnitInfo;
import org.apache.cayenne.jpa.map.JpaBasic;
import org.apache.cayenne.jpa.map.JpaEntity;
import org.apache.cayenne.jpa.map.JpaEntityMap;
import org.apache.cayenne.jpa.map.JpaId;
import org.apache.cayenne.jpa.map.JpaManyToOne;
import org.apache.cayenne.jpa.map.JpaOneToMany;

public class EntityMapDefaultsProcessorTest extends TestCase {

    protected JpaEntity entity;
    protected EntityMapLoaderContext context;

    @Override
    protected void setUp() throws Exception {
        // sanity check - test object must not be serializable to be rejected...
        assertFalse(Serializable.class.isAssignableFrom(MockAnnotatedBean3.class));

        context = new EntityMapLoaderContext(new MockPersistenceUnitInfo());
        EntityMapAnnotationLoader loader = new EntityMapAnnotationLoader(context);
        loader.loadClassMapping(MockAnnotatedBean1.class);
        loader.loadClassMapping(MockAnnotatedBean3.class);
        loader.loadClassMapping(MockAnnotatedBean5.class);

        // apply defaults
        EntityMapDefaultsProcessor defaultsProcessor = new EntityMapDefaultsProcessor();
        defaultsProcessor.applyDefaults(context);
        JpaEntityMap map = context.getEntityMap();
        entity = map.entityForClass(MockAnnotatedBean3.class);
        assertNotNull(entity);
    }

    public void testMissingAttributeAnnotation() throws Exception {

        assertTrue(context.getConflicts().hasFailures());

        assertTrue(context.getConflicts().getFailures(
                MockAnnotatedBean3.class.getDeclaredField("attribute1")).isEmpty());

        assertFalse(context.getConflicts().getFailures(
                MockAnnotatedBean3.class.getDeclaredField("attribute2")).isEmpty());

        assertNotNull(entity.getAttributes().getBasicAttribute("attribute1"));
        assertNull(entity.getAttributes().getBasicAttribute("attribute2"));
    }

    public void testSkipCayennePersistentProperties() throws Exception {

        JpaEntity e5 = context.getEntityMap().entityForClass(MockAnnotatedBean5.class);
        assertNotNull(e5);

        assertNotNull(e5.getAttributes().getBasicAttribute("attribute1"));
        assertNull(e5.getAttributes().getBasicAttribute("objectId"));
    }

    public void testTargetEntityNameToOne() {
        JpaManyToOne toBean2 = entity.getAttributes().getManyToOneRelationship("toBean2");
        assertNotNull(toBean2);
        assertEquals(MockAnnotatedBean1.class.getName(), toBean2.getTargetEntityName());
    }

    public void testTargetEntityNameCollection() throws Exception {

        assertTrue(context.getConflicts().getFailures(
                MockAnnotatedBean3.class.getDeclaredField("toBean2s1")).isEmpty());
        JpaOneToMany toBean2s1 = entity.getAttributes().getOneToManyRelationship(
                "toBean2s1");
        assertNotNull(toBean2s1);
        assertEquals(MockAnnotatedBean1.class.getName(), toBean2s1.getTargetEntityName());

        assertFalse("Expected failure", context.getConflicts().getFailures(
                MockAnnotatedBean3.class.getDeclaredField("toBean2s2")).isEmpty());
        JpaOneToMany toBean2s2 = entity.getAttributes().getOneToManyRelationship(
                "toBean2s2");
        assertNotNull(toBean2s2);
        assertNull(toBean2s2.getTargetEntityName());
    }

    public void testId() {
        assertEquals(1, entity.getAttributes().getIds().size());
        JpaId id = entity.getAttributes().getIds().iterator().next();
        assertEquals("pk", id.getName());
        assertNotNull(id.getColumn());
        assertEquals("pk", id.getColumn().getName());
    }

    public void testDate() throws Exception {
        assertTrue(context.getConflicts().getFailures(
                MockAnnotatedBean3.class.getDeclaredField("date")).isEmpty());
        JpaBasic date = entity.getAttributes().getBasicAttribute("date");
        assertEquals(TemporalType.TIMESTAMP, date.getTemporal());
    }
}
