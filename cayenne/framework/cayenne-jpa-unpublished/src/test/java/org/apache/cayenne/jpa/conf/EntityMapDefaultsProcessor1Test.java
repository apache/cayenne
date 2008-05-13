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

import junit.framework.TestCase;

import org.apache.cayenne.jpa.MockPersistenceUnitInfo;
import org.apache.cayenne.jpa.map.JpaEntity;
import org.apache.cayenne.jpa.map.JpaEntityMap;
import org.apache.cayenne.jpa.map.JpaVersion;

public class EntityMapDefaultsProcessor1Test extends TestCase {

    public void testVersion() {

        EntityMapLoaderContext context = new EntityMapLoaderContext(
                new MockPersistenceUnitInfo());
        EntityMapAnnotationLoader loader = new EntityMapAnnotationLoader(context);
        loader.loadClassMapping(MockAnnotatedBeanVersion.class);

        // apply defaults
        new EntityMapDefaultsProcessor().applyDefaults(context);

        JpaEntityMap map = context.getEntityMap();
        JpaEntity entity = map.entityForClass(MockAnnotatedBeanVersion.class);
        assertNotNull(entity);

        JpaVersion v = entity.getAttributes().getVersionAttribute("attribute1");
        assertNotNull(v);
        assertNotNull(v.getColumn());
    }

    public void testStaticFields() throws Exception {

        EntityMapLoaderContext context = new EntityMapLoaderContext(
                new MockPersistenceUnitInfo());
        EntityMapAnnotationLoader loader = new EntityMapAnnotationLoader(context);
        loader.loadClassMapping(MockAnnotatedBean4.class);

        // apply defaults
        new EntityMapDefaultsProcessor().applyDefaults(context);

        JpaEntityMap map = context.getEntityMap();
        assertEquals(1, map.getEntities().size());
        JpaEntity entity = map.getEntities().iterator().next();

        // testing that static fields are not loaded as persistent
        assertEquals(1, entity.getAttributes().size());
        assertEquals(1, entity.getAttributes().getIds().size());
    }
}
