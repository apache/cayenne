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

import java.sql.Types;

import junit.framework.TestCase;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.jpa.conf.EntityMapAnnotationLoader;
import org.apache.cayenne.jpa.conf.EntityMapDefaultsProcessor;
import org.apache.cayenne.jpa.conf.EntityMapLoaderContext;
import org.apache.cayenne.jpa.entity.MockBasicEntity;
import org.apache.cayenne.jpa.entity.MockIdColumnEntity;
import org.apache.cayenne.jpa.entity.MockTypesEntity;
import org.apache.cayenne.jpa.entity.cayenne.MockCayenneEntity1;
import org.apache.cayenne.jpa.entity.cayenne.MockCayenneEntity2;
import org.apache.cayenne.jpa.entity.cayenne.MockCayenneEntityMap1;
import org.apache.cayenne.jpa.entity.cayenne.MockCayenneTargetEntity1;
import org.apache.cayenne.jpa.entity.cayenne.MockCayenneTargetEntity2;
import org.apache.cayenne.jpa.map.JpaEntityMap;
import org.apache.cayenne.jpa.spi.MockPersistenceUnitInfo;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

public class DataMapConverterTest extends TestCase {

    public void testDataMapDefaults() {
        EntityMapLoaderContext context = new EntityMapLoaderContext(
                new MockPersistenceUnitInfo());
        JpaEntityMap jpaMap = context.getEntityMap();
        jpaMap.setPackageName("p1");
        jpaMap.setSchema("s1");

        // TODO: unsupported by DataMap
        // jpaMap.setCatalog("c1");

        DataMap cayenneMap = new DataMapConverter().toDataMap("n1", context);
        assertEquals("n1", cayenneMap.getName());
        assertEquals("p1", cayenneMap.getDefaultPackage());
        assertEquals("s1", cayenneMap.getDefaultSchema());
    }

    public void testLoadClassMapping() throws Exception {
        EntityMapLoaderContext context = new EntityMapLoaderContext(
                new MockPersistenceUnitInfo());
        EntityMapAnnotationLoader loader = new EntityMapAnnotationLoader(context);

        loader.loadClassMapping(MockCayenneEntity1.class);
        loader.loadClassMapping(MockCayenneEntity2.class);
        loader.loadClassMapping(MockCayenneTargetEntity1.class);
        loader.loadClassMapping(MockCayenneTargetEntity2.class);

        loader.loadClassMapping(MockCayenneEntityMap1.class);

        // apply defaults before conversion
        new EntityMapDefaultsProcessor().applyDefaults(context);

        assertFalse("Found conflicts: " + context.getConflicts(), context
                .getConflicts()
                .hasFailures());

        DataMap dataMap = new DataMapConverter().toDataMap("n1", context);
        assertFalse("Found DataMap conflicts: " + context.getConflicts(), context
                .getConflicts()
                .hasFailures());

        new DataMapMappingAssertion().testDataMap(dataMap);
    }

    public void testDataMapTypes() {
        EntityMapLoaderContext context = new EntityMapLoaderContext(
                new MockPersistenceUnitInfo());
        EntityMapAnnotationLoader loader = new EntityMapAnnotationLoader(context);

        loader.loadClassMapping(MockTypesEntity.class);

        // apply defaults before conversion
        new EntityMapDefaultsProcessor().applyDefaults(context);

        assertFalse("Found conflicts: " + context.getConflicts(), context
                .getConflicts()
                .hasFailures());

        DataMap dataMap = new DataMapConverter().toDataMap("n1", context);
        assertFalse("Found DataMap conflicts: " + context.getConflicts(), context
                .getConflicts()
                .hasFailures());

        DbEntity typesTable = dataMap.getDbEntity("MockTypesEntity");
        assertNotNull(typesTable);

        DbAttribute defaultCalColumn = (DbAttribute) typesTable
                .getAttribute("defaultCalendar");
        assertNotNull(defaultCalColumn);
        assertEquals(
                "Invalid calendar type: "
                        + TypesMapping.getSqlNameByType(defaultCalColumn.getType()),
                Types.TIMESTAMP,
                defaultCalColumn.getType());

        DbAttribute timeColumn = (DbAttribute) typesTable.getAttribute("timeCalendar");
        assertNotNull(timeColumn);
        assertEquals(Types.TIME, timeColumn.getType());

        DbAttribute dateColumn = (DbAttribute) typesTable.getAttribute("dateCalendar");
        assertNotNull(dateColumn);
        assertEquals(Types.DATE, dateColumn.getType());

        DbAttribute timestampColumn = (DbAttribute) typesTable
                .getAttribute("timestampCalendar");
        assertNotNull(timestampColumn);
        assertEquals(Types.TIMESTAMP, timestampColumn.getType());

        DbAttribute defaultEnumColumn = (DbAttribute) typesTable
                .getAttribute("defaultEnum");
        assertNotNull(defaultEnumColumn);
        assertEquals(Types.INTEGER, defaultEnumColumn.getType());

        DbAttribute ordinalEnumColumn = (DbAttribute) typesTable
                .getAttribute("ordinalEnum");
        assertNotNull(ordinalEnumColumn);
        assertEquals(Types.INTEGER, ordinalEnumColumn.getType());

        DbAttribute stringEnumColumn = (DbAttribute) typesTable
                .getAttribute("stringEnum");
        assertNotNull(stringEnumColumn);
        assertEquals(Types.VARCHAR, stringEnumColumn.getType());

        DbAttribute byteArrayColumn = (DbAttribute) typesTable.getAttribute("byteArray");
        assertNotNull(byteArrayColumn);
        assertEquals(Types.BINARY, byteArrayColumn.getType());
    }

    public void testColumnOverrides() {
        EntityMapLoaderContext context = new EntityMapLoaderContext(
                new MockPersistenceUnitInfo());
        EntityMapAnnotationLoader loader = new EntityMapAnnotationLoader(context);

        loader.loadClassMapping(MockIdColumnEntity.class);

        // apply defaults before conversion
        new EntityMapDefaultsProcessor().applyDefaults(context);

        assertFalse("Found conflicts: " + context.getConflicts(), context
                .getConflicts()
                .hasFailures());

        DataMap dataMap = new DataMapConverter().toDataMap("n1", context);
        assertFalse("Found DataMap conflicts: " + context.getConflicts(), context
                .getConflicts()
                .hasFailures());

        DbEntity table = dataMap.getDbEntity("MockIdColumnEntity");
        assertNotNull(table);

        DbAttribute pk = (DbAttribute) table.getAttribute("pk_column");
        assertNotNull(pk);
        assertTrue(pk.isPrimaryKey());
    }

    public void testBasicOptionality() {
        EntityMapLoaderContext context = new EntityMapLoaderContext(
                new MockPersistenceUnitInfo());
        EntityMapAnnotationLoader loader = new EntityMapAnnotationLoader(context);

        loader.loadClassMapping(MockBasicEntity.class);

        // apply defaults before conversion
        new EntityMapDefaultsProcessor().applyDefaults(context);

        assertFalse("Found conflicts: " + context.getConflicts(), context
                .getConflicts()
                .hasFailures());

        DataMap dataMap = new DataMapConverter().toDataMap("n1", context);
        assertFalse("Found DataMap conflicts: " + context.getConflicts(), context
                .getConflicts()
                .hasFailures());

        DbEntity table = dataMap.getDbEntity("MockBasicEntity");
        assertNotNull(table);

        DbAttribute optional = (DbAttribute) table.getAttribute("optionalBasic");
        assertNotNull(optional);
        assertFalse(optional.isMandatory());
        
        DbAttribute required = (DbAttribute) table.getAttribute("requiredBasic");
        assertNotNull(required);
        assertTrue(required.isMandatory());
    }
}
