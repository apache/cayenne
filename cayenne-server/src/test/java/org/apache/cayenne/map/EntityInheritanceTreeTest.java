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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.exp.ExpressionFactory;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;


public class EntityInheritanceTreeTest {

    @Test
    public void testEntityMatchingRow_NoInheritance() {
        DataMap dataMap = new DataMap("map");

        DbEntity dbEntity = new DbEntity("e1");
        dbEntity.addAttribute(new DbAttribute("x"));
        dataMap.addDbEntity(dbEntity);

        ObjEntity entity = new ObjEntity("E1");
        entity.setDbEntityName(dbEntity.getName());
        dataMap.addObjEntity(entity);

        // no inheritance
        EntityInheritanceTree t1 = new EntityInheritanceTree(entity);

        DataRow row11 = new DataRow(5);
        row11.put("x", 1);
        assertSame(entity, t1.entityMatchingRow(row11));

        entity.setDeclaredQualifier(ExpressionFactory.matchDbExp("x", 2));
        assertNull(t1.entityMatchingRow(row11));

        DataRow row12 = new DataRow(5);
        row12.put("x", 2);
        assertSame(entity, t1.entityMatchingRow(row12));

    }

    @Test
    public void testEntityMatchingRow_SingleTableInheritance() {
        DataMap dataMap = new DataMap("map");

        DbEntity dbEntity = new DbEntity("e1");
        dbEntity.addAttribute(new DbAttribute("x"));
        dataMap.addDbEntity(dbEntity);

        ObjEntity entity = new ObjEntity("E1");
        entity.setDbEntityName(dbEntity.getName());
        entity.setDeclaredQualifier(ExpressionFactory.matchDbExp("x", 2));
        dataMap.addObjEntity(entity);

        ObjEntity subEntity = new ObjEntity("E2");
        subEntity.setSuperEntityName("E1");
        subEntity.setDeclaredQualifier(ExpressionFactory.matchDbExp("x", 1));
        dataMap.addObjEntity(subEntity);

        // creating EntityInheritanceTree via EntityResolver to ensure the entities are indexed
        EntityResolver resolver = new EntityResolver(Collections.singleton(dataMap));
        EntityInheritanceTree t1 = resolver.getInheritanceTree("E1");

        DataRow row11 = new DataRow(5);
        row11.put("x", 1);

        DataRow row12 = new DataRow(5);
        row12.put("x", 2);

        DataRow row13 = new DataRow(5);
        row13.put("x", 3);

        assertSame(subEntity, t1.entityMatchingRow(row11));
        assertSame(entity, t1.entityMatchingRow(row12));
        assertNull(t1.entityMatchingRow(row13));
    }

    @Test
    public void testEntityMatchingRow_CAY_2693() {
        DataMap dataMap = new DataMap("map");

        DbEntity dbEntity = new DbEntity("a");
        dbEntity.addAttribute(new DbAttribute("type"));
        dataMap.addDbEntity(dbEntity);

        ObjEntity entityA = new ObjEntity("A");
        entityA.setAbstract(true);
        entityA.setDbEntityName(dbEntity.getName());
        dataMap.addObjEntity(entityA);

        ObjEntity subEntityC = new ObjEntity("AC"); // name it AC so it would be sorted before B
        subEntityC.setSuperEntityName("A");
        subEntityC.setAbstract(true);
        dataMap.addObjEntity(subEntityC);

        ObjEntity subEntityB = new ObjEntity("B");
        subEntityB.setSuperEntityName("A");
        subEntityB.setDeclaredQualifier(ExpressionFactory.matchDbExp("type", 0));
        dataMap.addObjEntity(subEntityB);

        ObjEntity subEntityD = new ObjEntity("D");
        subEntityD.setSuperEntityName("AC");
        subEntityD.setDeclaredQualifier(ExpressionFactory.matchDbExp("type", 1));
        dataMap.addObjEntity(subEntityD);

        ObjEntity subEntityE = new ObjEntity("E");
        subEntityE.setSuperEntityName("AC");
        subEntityE.setDeclaredQualifier(ExpressionFactory.matchDbExp("type", 2));
        dataMap.addObjEntity(subEntityE);

        // creating EntityInheritanceTree via EntityResolver to ensure the entities are indexed
        EntityResolver resolver = new EntityResolver(Collections.singleton(dataMap));
        EntityInheritanceTree treeA = resolver.getInheritanceTree("A");
        EntityInheritanceTree treeC = resolver.getInheritanceTree("AC");

        DataRow row11 = new DataRow(5);
        row11.put("type", 0);

        DataRow row12 = new DataRow(5);
        row12.put("type", 1);

        DataRow row13 = new DataRow(5);
        row13.put("type", 2);

        assertSame(subEntityB, treeA.entityMatchingRow(row11));
        assertSame(subEntityD, treeA.entityMatchingRow(row12));
        assertSame(subEntityE, treeA.entityMatchingRow(row13));

        assertSame(subEntityD, treeC.entityMatchingRow(row12));
        assertSame(subEntityE, treeC.entityMatchingRow(row13));

        assertNull(treeA.qualifierForEntityAndSubclasses());
        assertEquals(ExpressionFactory.exp("(db:type = 1) or (db:type = 2)"), treeC.qualifierForEntityAndSubclasses());
    }
}
