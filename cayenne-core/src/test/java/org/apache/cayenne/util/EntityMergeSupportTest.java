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
package org.apache.cayenne.util;

import java.sql.Types;
import java.util.Arrays;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.merge.MergeCase;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class EntityMergeSupportTest extends MergeCase {

    public void testMerging() {
        DbEntity dbEntity1 = new DbEntity("NEW_TABLE");

        DbAttribute e1col1 = new DbAttribute("ID", Types.INTEGER, dbEntity1);
        e1col1.setMandatory(true);
        e1col1.setPrimaryKey(true);
        dbEntity1.addAttribute(e1col1);

        DbAttribute e1col2 = new DbAttribute("NAME", Types.VARCHAR, dbEntity1);
        e1col2.setMaxLength(10);
        e1col2.setMandatory(false);
        dbEntity1.addAttribute(e1col2);

        map.addDbEntity(dbEntity1);

        DbEntity dbEntity2 = new DbEntity("NEW_TABLE2");
        DbAttribute e2col1 = new DbAttribute("ID", Types.INTEGER, dbEntity2);
        e2col1.setMandatory(true);
        e2col1.setPrimaryKey(true);
        dbEntity2.addAttribute(e2col1);
        DbAttribute e2col2 = new DbAttribute("FK", Types.INTEGER, dbEntity2);
        dbEntity2.addAttribute(e2col2);

        map.addDbEntity(dbEntity2);

        // create db relationships
        DbRelationship rel1To2 = new DbRelationship("rel1To2");
        rel1To2.setSourceEntity(dbEntity1);
        rel1To2.setTargetEntity(dbEntity2);
        rel1To2.setToMany(true);
        rel1To2.addJoin(new DbJoin(rel1To2, e1col1.getName(), e2col2.getName()));
        dbEntity1.addRelationship(rel1To2);
        DbRelationship rel2To1 = new DbRelationship("rel2To1");
        rel2To1.setSourceEntity(dbEntity2);
        rel2To1.setTargetEntity(dbEntity1);
        rel2To1.setToMany(false);
        rel2To1.addJoin(new DbJoin(rel2To1, e2col2.getName(), e1col1.getName()));
        dbEntity2.addRelationship(rel2To1);
        assertSame(rel1To2, rel2To1.getReverseRelationship());
        assertSame(rel2To1, rel1To2.getReverseRelationship());

        ObjEntity objEntity1 = new ObjEntity("NewTable");
        objEntity1.setDbEntity(dbEntity1);
        map.addObjEntity(objEntity1);

        ObjEntity objEntity2 = new ObjEntity("NewTable2");
        objEntity2.setDbEntity(dbEntity2);
        map.addObjEntity(objEntity2);

        assertTrue(new EntityMergeSupport(map).synchronizeWithDbEntities(Arrays.asList(objEntity1, objEntity2)));
        assertNotNull(objEntity1.getAttribute("name"));
        assertNotNull(objEntity1.getRelationship("rel1To2"));
        assertNotNull(objEntity2.getRelationship("rel2To1"));

        assertEquals(objEntity1.getRelationship("rel1To2").getDeleteRule(), DeleteRule.DEFAULT_DELETE_RULE_TO_MANY);
        assertEquals(objEntity2.getRelationship("rel2To1").getDeleteRule(), DeleteRule.DEFAULT_DELETE_RULE_TO_ONE);

        map.removeObjEntity(objEntity2.getName());
        map.removeObjEntity(objEntity1.getName());
        map.removeDbEntity(dbEntity2.getName());
        map.removeDbEntity(dbEntity1.getName());
    }
}
