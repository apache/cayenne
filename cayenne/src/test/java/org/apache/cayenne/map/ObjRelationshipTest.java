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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks which side of an ObjRelationship holds the FK over every relationship shape: FK to PK, FK to a non-PK
 * column, master to dependent PK and back, to-many and flattened.
 */
public class ObjRelationshipTest {

    private ObjEntity master;
    private ObjEntity item;
    private ObjEntity detail;
    private ObjEntity twin;

    @BeforeEach
    public void buildModel() {

        DataMap map = new DataMap("M");

        DbEntity masterTable = addDbEntity(map, "MASTER");
        masterTable.addAttribute(new DbAttribute("NAME"));
        DbEntity detailTable = addDbEntity(map, "DETAIL");
        DbEntity twinTable = addDbEntity(map, "TWIN");
        DbEntity itemTable = addDbEntity(map, "ITEM");
        itemTable.addAttribute(new DbAttribute("MASTER_ID"));
        itemTable.addAttribute(new DbAttribute("MASTER_NAME"));

        addDbRelationship(itemTable, "master", "MASTER", "MASTER_ID", "ID", false).setForeignKey(true);
        addDbRelationship(masterTable, "items", "ITEM", "ID", "MASTER_ID", true);

        // an FK to a non-PK column of the target
        addDbRelationship(itemTable, "toMasterByName", "MASTER", "MASTER_NAME", "NAME", false).setForeignKey(true);

        // the DETAIL PK is also an FK to MASTER
        addDbRelationship(masterTable, "detail", "DETAIL", "ID", "ID", false);
        addDbRelationship(detailTable, "master", "MASTER", "ID", "ID", false).setForeignKey(true);

        // a PK-to-PK one-to-one with the FK flag on neither side
        addDbRelationship(masterTable, "twin", "TWIN", "ID", "ID", false);
        addDbRelationship(twinTable, "master", "MASTER", "ID", "ID", false);

        master = addObjEntity(map, "Master", "MASTER");
        addObjRelationship(master, "items", "Item", "items");
        addObjRelationship(master, "detail", "Detail", "detail");
        addObjRelationship(master, "twin", "Twin", "twin");

        detail = addObjEntity(map, "Detail", "DETAIL");
        addObjRelationship(detail, "master", "Master", "master");

        twin = addObjEntity(map, "Twin", "TWIN");
        addObjRelationship(twin, "master", "Master", "master");

        item = addObjEntity(map, "Item", "ITEM");
        addObjRelationship(item, "master", "Master", "master");
        addObjRelationship(item, "toMasterByName", "Master", "toMasterByName");
        addObjRelationship(item, "detail", "Detail", "master.detail");
    }

    @Test
    public void foreignKey() {
        assertTrue(item.getRelationship("master").isForeignKey());
        assertTrue(detail.getRelationship("master").isForeignKey());

        // the flag decides, not the target columns
        assertTrue(item.getRelationship("toMasterByName").isForeignKey());

        // to-many, master to dependent PK and flattened
        assertFalse(master.getRelationship("items").isForeignKey());
        assertFalse(master.getRelationship("detail").isForeignKey());
        assertFalse(item.getRelationship("detail").isForeignKey());

        // a PK-to-PK pair with no flag has no FK side
        assertFalse(master.getRelationship("twin").isForeignKey());
        assertFalse(twin.getRelationship("master").isForeignKey());
    }

    @Test
    public void toPrimaryKey() {
        assertTrue(item.getRelationship("master").isToPrimaryKey());
        assertTrue(detail.getRelationship("master").isToPrimaryKey());

        // the target columns decide, not the FK flag: both sides of a PK-to-PK one-to-one qualify
        assertTrue(master.getRelationship("detail").isToPrimaryKey());
        assertTrue(master.getRelationship("twin").isToPrimaryKey());
        assertTrue(twin.getRelationship("master").isToPrimaryKey());

        // an FK to a non-PK column, to-many and flattened
        assertFalse(item.getRelationship("toMasterByName").isToPrimaryKey());
        assertFalse(master.getRelationship("items").isToPrimaryKey());
        assertFalse(item.getRelationship("detail").isToPrimaryKey());
    }

    private static DbEntity addDbEntity(DataMap map, String name) {
        DbEntity entity = new DbEntity(name);
        DbAttribute pk = new DbAttribute("ID");
        pk.setPrimaryKey(true);
        entity.addAttribute(pk);
        map.addDbEntity(entity);
        return entity;
    }

    private static DbRelationship addDbRelationship(DbEntity source, String name, String target, String from,
                                                    String to, boolean toMany) {
        DbRelationship relationship = new DbRelationship(name);
        source.addRelationship(relationship);
        relationship.setTargetEntityName(target);
        relationship.setToMany(toMany);
        relationship.addJoin(new DbJoin(relationship, from, to));
        return relationship;
    }

    private static ObjEntity addObjEntity(DataMap map, String name, String dbEntityName) {
        ObjEntity entity = new ObjEntity(name);
        entity.setDbEntityName(dbEntityName);
        map.addObjEntity(entity);
        return entity;
    }

    private static void addObjRelationship(ObjEntity source, String name, String target, String dbPath) {
        ObjRelationship relationship = new ObjRelationship(name);
        source.addRelationship(relationship);
        relationship.setTargetEntityName(target);
        relationship.setDbRelationshipPath(dbPath);
    }
}
