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

import java.lang.annotation.Annotation;
import java.util.Arrays;

import javax.persistence.Entity;
import javax.persistence.IdClass;
import javax.persistence.NamedQuery;

import junit.framework.TestCase;

import org.apache.cayenne.jpa.MockPersistenceUnitInfo;
import org.apache.cayenne.jpa.entity.MockEmbed1;
import org.apache.cayenne.jpa.entity.MockEmbed2;
import org.apache.cayenne.jpa.entity.MockEntity1;
import org.apache.cayenne.jpa.entity.MockEntity2;
import org.apache.cayenne.jpa.entity.MockEntity3;
import org.apache.cayenne.jpa.entity.MockEntity4;
import org.apache.cayenne.jpa.entity.MockEntity5;
import org.apache.cayenne.jpa.entity.MockEntityMap1;
import org.apache.cayenne.jpa.entity.MockEntityMap2;
import org.apache.cayenne.jpa.entity.MockMappedSuperclass1;
import org.apache.cayenne.jpa.entity.MockMappedSuperclass2;
import org.apache.cayenne.jpa.entity.MockMappedSuperclass3;
import org.apache.cayenne.jpa.map.JpaAttributeOverride;
import org.apache.cayenne.jpa.map.JpaBasic;
import org.apache.cayenne.jpa.map.JpaEntity;
import org.apache.cayenne.jpa.map.JpaEntityMap;

public class EntityMapAnnotationLoaderTest extends TestCase {

    public void testSortAnnotations1() throws Exception {

        EntityMapAnnotationLoader loader = new EntityMapAnnotationLoader(
                new EntityMapLoaderContext(new MockPersistenceUnitInfo()));

        Annotation[] a1 = new Annotation[3];
        a1[0] = MockAnnotatedBean1.class.getAnnotation(NamedQuery.class);
        a1[1] = MockAnnotatedBean1.class.getAnnotation(IdClass.class);
        a1[2] = MockAnnotatedBean1.class.getAnnotation(Entity.class);

        Arrays.sort(a1, loader.typeAnnotationsSorter);

        assertEquals(Entity.class, a1[0].annotationType());
        assertEquals(NamedQuery.class, a1[1].annotationType());
        assertEquals(IdClass.class, a1[2].annotationType());
    }

    public void testSortAnnotations2() throws Exception {
        EntityMapLoaderContext context = new EntityMapLoaderContext(
                new MockPersistenceUnitInfo());
        EntityMapAnnotationLoader loader = new EntityMapAnnotationLoader(context);
        loader.loadClassMapping(MockAnnotatedBeanOrdering.class);

        JpaEntityMap map = context.getEntityMap();
        JpaEntity entity = map.getEntities().iterator().next();

        // regardless of the ordering of annotations, we should get the same result for
        // both attributes
        assertEquals(3, entity.getAttributes().getBasicAttributes().size());
        JpaBasic a1 = entity.getAttributes().getBasicAttribute("attribute1");
        assertTrue(a1.isLob());

        JpaBasic a2 = entity.getAttributes().getBasicAttribute("attribute2");
        assertTrue(a2.isLob());

        JpaBasic a3 = entity.getAttributes().getBasicAttribute("attribute3");
        assertTrue(a3.isLob());
    }

    /**
     * Checks that class-level AttributeOverride and embedded property AttributeOverride
     * are both processed correctly.
     */
    public void testAttributeOverride() {

        EntityMapLoaderContext context = new EntityMapLoaderContext(
                new MockPersistenceUnitInfo());
        EntityMapAnnotationLoader loader = new EntityMapAnnotationLoader(context);
        loader.loadClassMapping(MockAnnotatedBean2.class);

        JpaEntityMap map = context.getEntityMap();
        assertEquals(1, map.getEntities().size());
        JpaEntity entity = map.getEntities().iterator().next();
        assertEquals(1, entity.getAttributeOverrides().size());
        JpaAttributeOverride entityOverride = entity
                .getAttributeOverrides()
                .iterator()
                .next();
        assertEquals("entityAttribute", entityOverride.getName());
    }

    /**
     * Tests loading of all supported annotations. Uses mock annotated objects from the
     * "entity" package that roughly correspond to the XML mapping tested under
     * {@link EntityMapXMLLoaderTest#testDetails()}.
     */
    public void testLoadClassMapping() throws Exception {

        EntityMapLoaderContext context = new EntityMapLoaderContext(
                new MockPersistenceUnitInfo());
        EntityMapAnnotationLoader loader = new EntityMapAnnotationLoader(context);

        loader.loadClassMapping(MockEntity1.class);
        loader.loadClassMapping(MockEntity2.class);
        loader.loadClassMapping(MockEntity3.class);
        loader.loadClassMapping(MockEntity4.class);
        loader.loadClassMapping(MockEntity5.class);

        loader.loadClassMapping(MockEmbed1.class);
        loader.loadClassMapping(MockEmbed2.class);

        loader.loadClassMapping(MockMappedSuperclass1.class);
        loader.loadClassMapping(MockMappedSuperclass2.class);
        loader.loadClassMapping(MockMappedSuperclass3.class);

        loader.loadClassMapping(MockEntityMap1.class);
        loader.loadClassMapping(MockEntityMap2.class);

        assertFalse("Found conflicts: " + context.getConflicts(), context
                .getConflicts()
                .hasFailures());

        new MappingAssertion().testEntityMap(context.getEntityMap());
    }
}
