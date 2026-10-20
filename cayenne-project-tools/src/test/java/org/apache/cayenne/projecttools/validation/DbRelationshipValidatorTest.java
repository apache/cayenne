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

package org.apache.cayenne.projecttools.validation;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DbRelationshipValidatorTest {

    private DbRelationship toDetail;
    private DbRelationship toMaster;
    private DbRelationship itemToMaster;
    private DbRelationship items;

    @BeforeEach
    public void buildModel() {
        DataMap map = new DataMap("M");

        DbEntity master = addDbEntity(map, "MASTER");
        DbEntity detail = addDbEntity(map, "DETAIL");
        DbEntity item = addDbEntity(map, "ITEM");
        item.addAttribute(new DbAttribute("MASTER_ID"));

        // a one-to-one over a shared PK
        toDetail = addDbRelationship(master, "detail", "DETAIL", "ID", "ID");
        toMaster = addDbRelationship(detail, "master", "MASTER", "ID", "ID");

        // a plain FK to PK
        itemToMaster = addDbRelationship(item, "toMaster", "MASTER", "MASTER_ID", "ID");
        itemToMaster.setForeignKey(true);
        items = addDbRelationship(master, "items", "ITEM", "ID", "MASTER_ID");
        items.setToMany(true);
    }

    @Test
    public void oneToOneWithoutForeignKeyIsReported() {
        assertTrue(hasFailure(toDetail, "neither it nor its reverse"));
        assertTrue(hasFailure(toMaster, "neither it nor its reverse"));
    }

    @Test
    public void oneToOneWithForeignKeyIsNotReported() {
        toMaster.setForeignKey(true);
        assertFalse(hasFailure(toDetail, "neither it nor its reverse"));
        assertFalse(hasFailure(toMaster, "neither it nor its reverse"));
    }

    @Test
    public void oneToOneOverPlainFkWithoutForeignKeyIsReported() {
        // the FK side is obvious from the columns, but the flag is required all the same
        items.setToMany(false);
        itemToMaster.setForeignKey(false);
        assertTrue(hasFailure(itemToMaster, "neither it nor its reverse"));
        assertTrue(hasFailure(items, "neither it nor its reverse"));

        itemToMaster.setForeignKey(true);
        assertFalse(hasFailure(itemToMaster, "neither it nor its reverse"));
        assertFalse(hasFailure(items, "neither it nor its reverse"));
    }

    @Test
    public void oneToOneWithForeignKeyOnBothSidesIsReported() {
        toDetail.setForeignKey(true);
        toMaster.setForeignKey(true);
        assertTrue(hasFailure(toDetail, "both marked"));
        assertTrue(hasFailure(toMaster, "both marked"));
    }

    @Test
    public void fkToPkIsNotReported() {
        assertFalse(hasFailure(itemToMaster, "marked"));
        assertFalse(hasFailure(items, "marked"));
    }

    @Test
    public void generatedIdentifyingForeignKeyIsReported() {
        DbEntity item = itemToMaster.getSourceEntity();
        item.getAttribute("ID").setGenerated(true);
        assertFalse(hasFailure(itemToMaster, "can't be database-generated"));

        // the FK becomes a part of the generated PK
        item.getAttribute("MASTER_ID").setPrimaryKey(true);
        assertTrue(hasFailure(itemToMaster, "can't be database-generated"));

        itemToMaster.setForeignKey(false);
        assertFalse(hasFailure(itemToMaster, "can't be database-generated"));
    }

    @Test
    public void foreignKeyMismatchIsReported() {
        itemToMaster.setForeignKey(false);
        assertTrue(hasFailure(itemToMaster, "is not marked as the foreign key"));

        items.setForeignKey(true);
        assertTrue(hasFailure(items, "is to-many, but is marked"));
    }

    private static boolean hasFailure(DbRelationship relationship, String description) {
        ValidationResult result = new ValidationResult();
        new DbRelationshipValidator(ValidationConfig::new).validate(relationship, result);
        return result.getFailures(relationship).stream()
                .anyMatch(failure -> failure.getDescription().contains(description));
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
                                                    String to) {
        DbRelationship relationship = new DbRelationship(name);
        source.addRelationship(relationship);
        relationship.setTargetEntityName(target);
        relationship.addJoin(new DbJoin(relationship, from, to));
        return relationship;
    }
}
