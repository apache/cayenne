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

        addDbRelationship(itemTable, "toMaster", "MASTER", "MASTER_ID", "ID", false);
        addDbRelationship(masterTable, "items", "ITEM", "ID", "MASTER_ID", true);

        // a to-one over a non-PK column of the target
        addDbRelationship(itemTable, "toMasterByName", "MASTER", "MASTER_NAME", "NAME", false);

        // the DETAIL PK is also an FK to MASTER
        addDbRelationship(masterTable, "detail", "DETAIL", "ID", "ID", false).setToDependentPK(true);
        addDbRelationship(detailTable, "master", "MASTER", "ID", "ID", false);

        // a PK-to-PK one-to-one with no "to dependent PK" flag on either side
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
        addObjRelationship(item, "toMaster", "Master", "toMaster");
        addObjRelationship(item, "toMasterByName", "Master", "toMasterByName");
        addObjRelationship(item, "toDetail", "Detail", "toMaster.detail");
    }

    @Test
    public void fkOnSource() {
        assertTrue(item.getRelationship("toMaster").isFromFK());
        assertTrue(detail.getRelationship("master").isFromFK());

        // a to-one over a non-PK column, to-many, master to dependent PK and flattened
        assertFalse(item.getRelationship("toMasterByName").isFromFK());
        assertFalse(master.getRelationship("items").isFromFK());
        assertFalse(master.getRelationship("detail").isFromFK());
        assertFalse(item.getRelationship("toDetail").isFromFK());

        // without the flag, both sides of a PK-to-PK pair look like the FK side
        assertTrue(master.getRelationship("twin").isFromFK());
        assertTrue(twin.getRelationship("master").isFromFK());
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
