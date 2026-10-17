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

import org.apache.cayenne.exp.ExpressionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the key semantics of an ObjRelationship over every relationship shape: FK to PK, master to dependent PK
 * and back, to-many, flattened, a qualified target and a target with subclasses.
 */
public class ObjRelationshipTest {

    private EntityResolver resolver;
    private ObjEntity item;
    private ObjEntity detail;
    private ObjEntity itemTagEntity;
    private DbRelationship itemTags;

    @BeforeEach
    public void buildModel() {

        DataMap map = new DataMap("M");

        DbEntity master = addDbEntity(map, "MASTER");
        DbEntity detailTable = addDbEntity(map, "DETAIL");
        DbEntity twin = addDbEntity(map, "TWIN");
        DbEntity category = addDbEntity(map, "CATEGORY");
        DbEntity itemTable = addDbEntity(map, "ITEM");
        itemTable.addAttribute(new DbAttribute("MASTER_ID"));
        itemTable.addAttribute(new DbAttribute("CATEGORY_ID"));

        // a join table whose FK to ITEM is a part of its compound PK
        DbEntity itemTag = addDbEntity(map, "ITEM_TAG");
        DbAttribute tagId = new DbAttribute("TAG_ID");
        tagId.setPrimaryKey(true);
        itemTag.addAttribute(tagId);
        addDbRelationship(itemTag, "toItem", "ITEM", "ID", "ID", false);
        itemTags = addDbRelationship(itemTable, "itemTags", "ITEM_TAG", "ID", "ID", true);

        addDbRelationship(itemTable, "toMaster", "MASTER", "MASTER_ID", "ID", false);
        addDbRelationship(master, "items", "ITEM", "ID", "MASTER_ID", true);
        addDbRelationship(itemTable, "toCategory", "CATEGORY", "CATEGORY_ID", "ID", false);

        // the DETAIL PK is also an FK to MASTER
        addDbRelationship(master, "detail", "DETAIL", "ID", "ID", false).setToDependentPK(true);
        addDbRelationship(detailTable, "master", "MASTER", "ID", "ID", false);

        // a PK-to-PK one-to-one with no "to dependent PK" flag on either side
        addDbRelationship(master, "twin", "TWIN", "ID", "ID", false);
        addDbRelationship(twin, "master", "MASTER", "ID", "ID", false);

        ObjEntity masterEntity = addObjEntity(map, "Master", "MASTER");
        ObjEntity subMaster = addObjEntity(map, "SubMaster", null);
        subMaster.setSuperEntityName("Master");
        subMaster.setDeclaredQualifier(ExpressionFactory.matchDbExp("ID", 1));

        addObjEntity(map, "Category", "CATEGORY");
        ObjEntity qualifiedCategory = addObjEntity(map, "QualifiedCategory", "CATEGORY");
        qualifiedCategory.setDeclaredQualifier(ExpressionFactory.matchDbExp("ID", 1));

        detail = addObjEntity(map, "Detail", "DETAIL");
        addObjRelationship(detail, "master", "Master", "master");

        ObjEntity twinEntity = addObjEntity(map, "Twin", "TWIN");
        addObjRelationship(twinEntity, "master", "Master", "master");

        itemTagEntity = addObjEntity(map, "ItemTag", "ITEM_TAG");
        addObjRelationship(itemTagEntity, "toItem", "Item", "toItem");

        item = addObjEntity(map, "Item", "ITEM");
        addObjRelationship(item, "toMaster", "Master", "toMaster");
        addObjRelationship(item, "toCategory", "Category", "toCategory");
        addObjRelationship(item, "toQualifiedCategory", "QualifiedCategory", "toCategory");
        addObjRelationship(item, "toDetail", "Detail", "toMaster.detail");

        addObjRelationship(masterEntity, "items", "Item", "items");
        addObjRelationship(masterEntity, "detail", "Detail", "detail");
        addObjRelationship(masterEntity, "twin", "Twin", "twin");

        resolver = new EntityResolver(List.of(map));
    }

    @Test
    public void fkOnSource() {
        assertTrue(item.getRelationship("toMaster").isFromFK());
        assertTrue(item.getRelationship("toCategory").isFromFK());
        assertTrue(detail.getRelationship("master").isFromFK());

        // a qualifier on the target doesn't change where the key lives
        assertTrue(item.getRelationship("toQualifiedCategory").isFromFK());

        ObjEntity master = resolver.getObjEntity("Master");
        assertFalse(master.getRelationship("items").isFromFK());
        assertFalse(master.getRelationship("detail").isFromFK());
        assertFalse(item.getRelationship("toDetail").isFromFK());

        // without the flag, both sides of a PK-to-PK pair look like the FK side
        assertTrue(master.getRelationship("twin").isFromFK());
        assertTrue(resolver.getObjEntity("Twin").getRelationship("master").isFromFK());
    }

    @Test
    public void targetDeterminedByFK() {
        assertTrue(item.getRelationship("toCategory").isTargetDeterminedByFK(resolver));

        // the FK says that a Master row exists, but not whether it is a Master or a SubMaster
        assertFalse(item.getRelationship("toMaster").isTargetDeterminedByFK(resolver));
        assertFalse(detail.getRelationship("master").isTargetDeterminedByFK(resolver));

        // an existing target row may still be filtered out by the target qualifier
        assertFalse(item.getRelationship("toQualifiedCategory").isTargetDeterminedByFK(resolver));

        // no FK on the source
        ObjEntity master = resolver.getObjEntity("Master");
        assertFalse(master.getRelationship("items").isTargetDeterminedByFK(resolver));
        assertFalse(master.getRelationship("detail").isTargetDeterminedByFK(resolver));
        assertFalse(item.getRelationship("toDetail").isTargetDeterminedByFK(resolver));

        // one side of an unflagged PK-to-PK pair is the master, so neither side guarantees the target
        assertFalse(master.getRelationship("twin").isTargetDeterminedByFK(resolver));
        assertFalse(resolver.getObjEntity("Twin").getRelationship("master").isTargetDeterminedByFK(resolver));

        // an FK that is a part of the source PK is trusted only once the reverse marks the source as dependent
        ObjRelationship toItem = itemTagEntity.getRelationship("toItem");
        assertTrue(toItem.isFromFK());
        assertFalse(toItem.isTargetDeterminedByFK(resolver));
        itemTags.setToDependentPK(true);
        assertTrue(toItem.isTargetDeterminedByFK(resolver));
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
