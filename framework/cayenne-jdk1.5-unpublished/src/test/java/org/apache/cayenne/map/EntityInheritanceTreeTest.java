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
package org.apache.cayenne.map;

import java.util.Collections;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.exp.ExpressionFactory;

import junit.framework.TestCase;

public class EntityInheritanceTreeTest extends TestCase {

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

        // creating EntityInheritanceTree via EntityResolver to ensure the entities are
        // indexed
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
}
