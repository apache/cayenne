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
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import junit.framework.Assert;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.jpa.entity.cayenne.MockCayenneEntity1;
import org.apache.cayenne.jpa.entity.cayenne.MockCayenneEntity2;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

public class DataMapMappingAssertion extends Assert {

    public void testDataMap(DataMap dataMap) {
        assertNotNull(dataMap);
        assertEquals("n1", dataMap.getName());

        assertNotNull(dataMap.getObjEntities());
        assertEquals(4, dataMap.getObjEntities().size());
        Iterator<ObjEntity> entityIt = dataMap.getObjEntities().iterator();
        assertEntity1(entityIt.next());
        assertEntity2(entityIt.next());

        assertEquals(4, dataMap.getQueries().size());
    }

    protected void assertEntity1(ObjEntity entity1) {

        assertNotNull(entity1);
        assertEquals("MockCayenneEntity1", entity1.getName());
        assertEquals(MockCayenneEntity1.class.getName(), entity1.getClassName());

        assertObjAttributes(entity1);
        assertObjRelationships(entity1);

        assertDbEntity(entity1.getDbEntity());
    }

    protected void assertEntity2(ObjEntity entity2) {

        assertNotNull(entity2);
        assertEquals("MockCayenneEntity2", entity2.getName());
        assertEquals(MockCayenneEntity2.class.getName(), entity2.getClassName());
    }

    protected void assertDbEntity(DbEntity table) {
        assertNotNull(table);
        assertEquals("mock_persistent_1", table.getName());
        assertEquals("catalog1", table.getCatalog());
        assertEquals("schema1", table.getSchema());

        Collection<DbAttribute> pks = table.getPrimaryKeys();
        assertEquals(1, pks.size());
        DbAttribute pk = (DbAttribute) pks.iterator().next();
        assertEquals("id", pk.getName());
        assertTrue(pk.isPrimaryKey());
        assertTrue(pk.isMandatory());
        assertEquals(
                "Invalid SQL type: " + TypesMapping.getSqlNameByType(pk.getType()),
                Types.INTEGER,
                pk.getType());

        DbAttribute column9 = (DbAttribute) table.getAttribute("column9");
        assertNotNull(column9);
        assertEquals(Types.DATE, column9.getType());
        assertFalse(column9.isPrimaryKey());

        assertDbRelationship(table);
    }

    protected void assertObjAttributes(ObjEntity entity1) {
        assertEquals(4, entity1.getAttributeMap().size());

        ObjAttribute a1 = (ObjAttribute) entity1.getAttribute("attribute1");
        assertNotNull(a1);
        assertEquals("attribute1", a1.getName());
        assertEquals("attribute1", a1.getDbAttributeName());
        assertEquals(String.class.getName(), a1.getType());

        ObjAttribute a2 = (ObjAttribute) entity1.getAttribute("attribute2");
        assertNotNull(a2);
        assertEquals("attribute2", a2.getName());
        assertEquals("attribute2", a2.getDbAttributeName());
        assertEquals(Integer.TYPE.getName(), a2.getType());

        ObjAttribute a3 = (ObjAttribute) entity1.getAttribute("attribute9");
        assertNotNull(a3);
        assertEquals("attribute9", a3.getName());
        assertEquals("column9", a3.getDbAttributeName());
        assertEquals(Date.class.getName(), a3.getType());
        
        // PK must also be mapped
        ObjAttribute id = (ObjAttribute) entity1.getAttribute("id");
        assertNotNull(id);
    }

    protected void assertObjRelationships(ObjEntity entity1) {
        assertEquals(4, entity1.getRelationshipMap().size());

        ObjRelationship attribute4 = (ObjRelationship) entity1
                .getRelationship("attribute4");
        assertNotNull(attribute4);
        assertTrue(attribute4.isToMany());
        assertEquals("attribute4", attribute4.getDbRelationshipPath());
        assertEquals("MockCayenneTargetEntity2", attribute4.getTargetEntityName());

        ObjRelationship attribute5 = (ObjRelationship) entity1
                .getRelationship("attribute5");
        assertNotNull(attribute5);
        assertFalse(attribute5.isToMany());
        assertEquals("attribute5", attribute5.getDbRelationshipPath());
        assertEquals("MockCayenneTargetEntity2", attribute5.getTargetEntityName());
    }

    protected void assertDbRelationship(DbEntity entity1) {
        assertEquals(4, entity1.getRelationshipMap().size());

        DbRelationship attribute4 = (DbRelationship) entity1
                .getRelationship("attribute4");
        assertNotNull(attribute4);
        assertTrue(attribute4.isToMany());
        assertEquals("MockCayenneTargetEntity2", attribute4.getTargetEntityName());
        assertEquals(1, attribute4.getJoins().size());

        DbRelationship attribute5 = (DbRelationship) entity1
                .getRelationship("attribute5");
        assertNotNull(attribute5);
        assertFalse(attribute5.isToMany());
        assertEquals("MockCayenneTargetEntity2", attribute5.getTargetEntityName());
        assertEquals(1, attribute5.getJoins().size());
    }
}
