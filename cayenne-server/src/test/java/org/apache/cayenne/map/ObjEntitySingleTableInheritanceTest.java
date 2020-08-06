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

package org.apache.cayenne.map;

import org.junit.Before;
import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 */
public class ObjEntitySingleTableInheritanceTest {

    protected DataMap map;

    protected DbEntity dbEntity;

    protected DbAttribute dbAttribute1;
    protected DbAttribute dbAttribute2;
    protected DbAttribute dbAttribute3;

    protected DbRelationship dbRelationship1;
    protected DbRelationship dbRelationship2;
    protected DbRelationship dbRelationship3;

    protected ObjEntity entity1;
    protected ObjEntity entity2;
    protected ObjEntity entity3;

    protected ObjAttribute attribute1;
    protected ObjAttribute attribute2;
    protected ObjAttribute attribute3;

    protected ObjRelationship relationship1;
    protected ObjRelationship relationship2;
    protected ObjRelationship relationship3;

    @Before
    public void setUp() throws Exception {
        map = new DataMap();

        // create common DbEntity
        dbEntity = new DbEntity("DB");
        dbAttribute1 = new DbAttribute("ATTRIBUTE1", Types.INTEGER, dbEntity);
        dbAttribute2 = new DbAttribute("ATTRIBUTE2", Types.INTEGER, dbEntity);
        dbAttribute3 = new DbAttribute("ATTRIBUTE3", Types.INTEGER, dbEntity);

        dbEntity.addAttribute(dbAttribute1);
        dbEntity.addAttribute(dbAttribute2);
        dbEntity.addAttribute(dbAttribute3);

        dbRelationship1 = new DbRelationship("DBR1");
        dbRelationship2 = new DbRelationship("DBR2");
        dbRelationship3 = new DbRelationship("DBR3");

        dbEntity.addRelationship(dbRelationship1);
        dbEntity.addRelationship(dbRelationship2);
        dbEntity.addRelationship(dbRelationship3);

        map.addDbEntity(dbEntity);

        entity1 = new ObjEntity("e1");
        entity2 = new ObjEntity("e2");
        entity3 = new ObjEntity("e3");

        attribute1 = new ObjAttribute("a1");
        attribute2 = new ObjAttribute("a2");
        attribute3 = new ObjAttribute("a3");

        entity1.addAttribute(attribute1);
        entity2.addAttribute(attribute2);
        entity3.addAttribute(attribute3);

        relationship1 = new ObjRelationship("r1");
        relationship2 = new ObjRelationship("r2");
        relationship3 = new ObjRelationship("r3");

        entity1.addRelationship(relationship1);
        entity2.addRelationship(relationship2);
        entity3.addRelationship(relationship3);

        map.addObjEntity(entity1);
        map.addObjEntity(entity2);
        map.addObjEntity(entity3);
    }

    @Test
    public void testInheritedAttributes() throws Exception {
        assertSame(attribute1, entity1.getAttribute("a1"));
        assertNull(entity1.getAttribute("a2"));

        entity1.setSuperEntityName("e2");
        assertNotNull(entity1.getAttribute("a2"));
        assertEquals("a2", entity1.getAttribute("a2").getName());
        assertSame(entity1, entity1.getAttribute("a2").getParent());
        assertNull(entity1.getAttribute("a3"));

        entity2.setSuperEntityName("e3");
        assertNotNull(entity1.getAttribute("a3"));
    }

    @Test
    public void testInheritedRelationships() throws Exception {
        assertSame(relationship1, entity1.getRelationship("r1"));
        assertNull(entity1.getRelationship("r2"));

        entity1.setSuperEntityName("e2");
        assertSame(relationship2, entity1.getRelationship("r2"));
        assertNull(entity1.getRelationship("r3"));

        entity2.setSuperEntityName("e3");
        assertSame(relationship3, entity1.getRelationship("r3"));
    }

    @Test
    public void testAttributeForDbAttribute() throws Exception {
        entity1.setSuperEntityName("e2");
        entity2.setDbEntityName(dbEntity.getName());

        attribute1.setDbAttributePath(dbAttribute1.getName());
        attribute2.setDbAttributePath(dbAttribute2.getName());

        assertNull(entity2.getAttributeForDbAttribute(dbAttribute1));
        assertSame(attribute2, entity2.getAttributeForDbAttribute(dbAttribute2));

        assertSame(attribute1, entity1.getAttributeForDbAttribute(dbAttribute1));
        assertNotNull(entity1.getAttributeForDbAttribute(dbAttribute2));
    }

    @Test
    public void testRelationshipForDbRelationship() throws Exception {
        entity1.setSuperEntityName("e2");
        entity2.setDbEntityName(dbEntity.getName());

        relationship1.addDbRelationship(dbRelationship1);
        relationship2.addDbRelationship(dbRelationship2);

        assertNull(entity2.getRelationshipForDbRelationship(dbRelationship1));
        assertSame(relationship2, entity2
                .getRelationshipForDbRelationship(dbRelationship2));

        assertSame(relationship1, entity1
                .getRelationshipForDbRelationship(dbRelationship1));
        assertSame(relationship2, entity1
                .getRelationshipForDbRelationship(dbRelationship2));
    }
}
