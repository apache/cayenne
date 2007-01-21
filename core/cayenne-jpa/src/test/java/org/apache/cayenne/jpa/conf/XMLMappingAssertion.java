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

import org.apache.cayenne.jpa.map.AccessType;
import org.apache.cayenne.jpa.map.JpaEmbeddable;
import org.apache.cayenne.jpa.map.JpaEntity;
import org.apache.cayenne.jpa.map.JpaEntityMap;
import org.apache.cayenne.jpa.map.JpaMappedSuperclass;
import org.apache.cayenne.jpa.map.JpaPersistenceUnitMetadata;

class XMLMappingAssertion extends MappingAssertion {

    @Override
    public void testEntityMap(JpaEntityMap entityMap) throws Exception {

        assertNotNull(entityMap);
        assertEquals("Test Description", entityMap.getDescription());
        assertEquals("default_package", entityMap.getPackageName());
        assertEquals("default_catalog", entityMap.getCatalog());
        assertEquals(AccessType.FIELD, entityMap.getAccess());
        assertUnitMetadata(entityMap.getPersistenceUnitMetadata());

        super.testEntityMap(entityMap);
    }

    protected void assertUnitMetadata(JpaPersistenceUnitMetadata md) {
        assertNotNull(md);
        assertNotNull(md.getPersistenceUnitDefaults());
        assertEquals("dschema", md.getPersistenceUnitDefaults().getSchema());
        assertEquals("dcatalog", md.getPersistenceUnitDefaults().getCatalog());
        assertEntityListeners(md.getPersistenceUnitDefaults().getEntityListeners());
    }

    @Override
    protected void assertEntity1(JpaEntity entity1) {
        super.assertEntity1(entity1);
        assertTrue(entity1.isMetadataComplete());
        assertSame(AccessType.PROPERTY, entity1.getAccess());
    }

    @Override
    protected void assertEntity2(JpaEntity entity2) {
        super.assertEntity2(entity2);
        assertFalse(entity2.isMetadataComplete());
    }

    @Override
    protected void assertEmbeddable1(JpaEmbeddable embeddable1) {
        super.assertEmbeddable1(embeddable1);
        assertTrue(embeddable1.isMetadataComplete());
        assertSame(AccessType.FIELD, embeddable1.getAccess());
    }

    @Override
    protected void assertMappedSuperclass1(JpaMappedSuperclass mappedSuperclass1) {
        super.assertMappedSuperclass1(mappedSuperclass1);
        assertTrue(mappedSuperclass1.isMetadataComplete());
    }
}
