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
package org.apache.cayenne.dbsync.merge;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.dbsync.merge.token.db.DropRelationshipToDb;
import org.apache.cayenne.dbsync.merge.token.db.DropTableToDb;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.junit.Test;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DbEntityMergerIT extends MergeCase {

    @Inject
    private DataContext context;

    @Test
    public void testCreateTokensForMissingImported() throws Exception {
        dropTableIfPresent("NEW_TABLE");
        dropTableIfPresent("NEW_TABLE2");
        assertTokensAndExecute(0, 0);

        DbEntity dbEntity1 = new DbEntity("NEW_TABLE");

        DbAttribute column1 = new DbAttribute("ID", Types.INTEGER, dbEntity1);
        column1.setMandatory(true);
        column1.setPrimaryKey(true);
        dbEntity1.addAttribute(column1);

        DbAttribute column2 = new DbAttribute("NAME", Types.VARCHAR, dbEntity1);
        column2.setMaxLength(10);
        column2.setMandatory(false);
        column2.setPrimaryKey(false);
        dbEntity1.addAttribute(column2);

        //to prevent postgresql from creating pk_table
        setPrimaryKeyGeneratorDBGenerate(dbEntity1);
        map.addDbEntity(dbEntity1);

        DbEntity dbEntity2 = new DbEntity("NEW_TABLE2");

        DbAttribute column3 = new DbAttribute("ID", Types.INTEGER, dbEntity2);
        column3.setMandatory(false);
        column3.setPrimaryKey(false);
        dbEntity2.addAttribute(column3);

        DbAttribute column4 = new DbAttribute("NUMBER_ID", Types.INTEGER, dbEntity2);
        column4.setMandatory(false);
        column4.setPrimaryKey(false);
        column4.setMaxLength(10);
        dbEntity2.addAttribute(column4);

        //to prevent postgresql from creating pk_table
        setPrimaryKeyGeneratorDBGenerate(dbEntity2);
        map.addDbEntity(dbEntity2);

        // create db relationships
        DbRelationship rel1To2 = new DbRelationship("rel1To2");
        rel1To2.setSourceEntity(dbEntity1);
        rel1To2.setTargetEntityName(dbEntity2);
        rel1To2.setToMany(true);
        rel1To2.addJoin(new DbJoin(rel1To2, column1.getName(), column3.getName()));
        dbEntity1.addRelationship(rel1To2);

        DbRelationship rel2To1 = new DbRelationship("rel2To1");
        rel2To1.setSourceEntity(dbEntity2);
        rel2To1.setTargetEntityName(dbEntity1);
        rel2To1.setToMany(false);
        rel2To1.addJoin(new DbJoin(rel2To1, column3.getName(), column1.getName()));
        dbEntity2.addRelationship(rel2To1);

        // for the new entity to the db
        execute(mergerFactory().createCreateTableToDb(dbEntity1));
        execute(mergerFactory().createCreateTableToDb(dbEntity2));
        execute(mergerFactory().createDropRelationshipToModel(dbEntity1, rel1To2).createReverse(mergerFactory()));
        execute(mergerFactory().createDropRelationshipToModel(dbEntity2, rel2To1).createReverse(mergerFactory()));
        map.removeDbEntity(dbEntity1.getName());
        map.removeDbEntity(dbEntity2.getName());

        List<MergerToken> tokens = createMergeTokensWithoutEmptyFilter(false);
        List<MergerToken> reverseTokens = new ArrayList<>();
        for (MergerToken token : tokens) {
            if (token instanceof DropRelationshipToDb || token instanceof DropTableToDb) {
                reverseTokens.add(token.createReverse(mergerFactory()));
            }
        }

        Collections.sort(reverseTokens);
        assertEquals(4, reverseTokens.size());
        execute(reverseTokens);

        ObjEntity objEntity1 = null;
        ObjEntity objEntity2 = null;
        for (ObjEntity candidate : map.getObjEntities()) {
            if (dbEntity1.getName().equalsIgnoreCase(candidate.getDbEntityName())) {
                objEntity1 = candidate;
                continue;
            }
            if (dbEntity2.getName().equalsIgnoreCase(candidate.getDbEntityName())) {
                objEntity2 = candidate;
            }
            if(objEntity1 != null && objEntity2 != null) {
                break;
            }
        }

        assertNotNull(objEntity1);
        assertNotNull(objEntity2);
        Iterator<ObjRelationship> iterator1 = objEntity1.getRelationships().iterator();
        Iterator<ObjRelationship> iterator2 = objEntity2.getRelationships().iterator();
        assertTrue(iterator2.hasNext());
        ObjRelationship objRelationship2 = iterator2.next();
        assertFalse(objRelationship2.isToMany());
        assertTrue(iterator1.hasNext());
        ObjRelationship objRelationship1 = iterator1.next();
        assertTrue(objRelationship1.isToMany());

        dropTableIfPresent(objEntity2.getDbEntity().getName());
        dropTableIfPresent(objEntity1.getDbEntity().getName());
        //clear entity
        map.removeDbEntity(objEntity1.getDbEntity().getName());
        map.removeDbEntity(objEntity2.getDbEntity().getName());
        map.removeObjEntity(objEntity1.getName(), true);
        map.removeObjEntity(objEntity2.getName(), true);

        assertTokensAndExecute(0, 0);

    }
}
