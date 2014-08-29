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
package org.apache.cayenne.merge;

import org.apache.cayenne.dba.hsqldb.HSQLMergerFactory;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.merge.builders.DbEntityBuilder;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.apache.cayenne.merge.builders.ObjectMother.dataMap;
import static org.apache.cayenne.merge.builders.ObjectMother.dbAttr;
import static org.apache.cayenne.merge.builders.ObjectMother.dbEntity;
import static org.junit.Assert.assertEquals;

public class DbMergerTest {

    @Test
    public void testEmptyDataMap() throws Exception {
        assertEquals(0, dbMerger().createMergeTokens(new ArrayList<DbEntity>(0),
                new ArrayList<DbEntity>(0)).size());
    }

    @Test
    public void testAddTable() throws Exception {
        DbEntityBuilder dbEntity =
            dbEntity("table1").attributes(
                dbAttr("attr01").typeInt()
        );
        DataMap existing = dataMap().with(dbEntity).build();

        List<MergerToken> tokens = dbMerger().createMergeTokens(existing.getDbEntities(),
                new ArrayList<DbEntity>(0));

        assertEquals(1, tokens.size());
        assertEquals(factory().createCreateTableToDb(dbEntity.build()).getTokenValue(),
                     tokens.get(0).getTokenValue());
    }

    @Test
    public void testRemoveTable() throws Exception {
        DataMap db = dataMap().with(
            dbEntity("table1").attributes(
                dbAttr("attr01").typeInt()
        )).build();

        List<MergerToken> tokens = dbMerger().createMergeTokens(new ArrayList<DbEntity>(0), db.getDbEntities());

        assertEquals(1, tokens.size());
        assertEquals(factory().createDropTableToDb(db.getDbEntity("table1")).getTokenValue(),
                     tokens.get(0).getTokenValue());
    }

    @Test
    public void testAddColumn() throws Exception {
        DataMap existing = dataMap().with(
            dbEntity("table1").attributes(
                dbAttr("attr01").typeInt(),
                dbAttr("attr02").typeInt()
        )).build();

        DataMap db = dataMap().with(
            dbEntity("table1").attributes(
                dbAttr("attr01").typeInt()
        )).build();

        List<MergerToken> tokens = dbMerger().createMergeTokens(existing.getDbEntities(), db.getDbEntities());

        assertEquals(1, tokens.size());

        DbEntity entity = existing.getDbEntity("table1");
        assertEquals(factory().createAddColumnToDb(entity, entity.getAttribute("attr02")).getTokenValue(),
                     tokens.get(0).getTokenValue());
    }

    @Test
    public void testAddRelationship() throws Exception {
        DataMap existing = dataMap().with(
            dbEntity("table1").attributes(
                dbAttr("attr01").typeInt(),
                dbAttr("attr02").typeInt()),

            dbEntity("table2").attributes(
                dbAttr("attr01").typeInt().primaryKey(),
                dbAttr("attr02").typeInt())
        ).join("rel", "table1.attr01", "table2.attr01")
         .build();

        DataMap db = dataMap().with(
            dbEntity("table1").attributes(
                dbAttr("attr01").typeInt(),
                dbAttr("attr02").typeInt()),

            dbEntity("table2").attributes(
                dbAttr("attr01").typeInt().primaryKey(),
                dbAttr("attr02").typeInt())
        )//.join("table1.attr01", "table2.attr01")
         .build();


        List<MergerToken> tokens = dbMerger().createMergeTokens(existing.getDbEntities(), db.getDbEntities());

        assertEquals(1, tokens.size());

        DbEntity entity = existing.getDbEntity("table1");
        assertEquals(factory().createAddRelationshipToDb(entity, entity.getRelationship("rel")).getTokenValue(),
                     tokens.get(0).getTokenValue());
    }

    @Test
    public void testAddRelationship1() throws Exception {
        DataMap existing = dataMap().with(
            dbEntity("table1").attributes(
                dbAttr("attr01").typeInt(),
                dbAttr("attr02").typeInt()),

            dbEntity("table2").attributes(
                dbAttr("attr01").typeInt().primaryKey(),
                dbAttr("attr02").typeInt().primaryKey(),
                dbAttr("attr03").typeInt().primaryKey())
        ).join("rel", "table1.attr01", "table2.attr01")
         .join("rel1", "table1.attr01", "table2.attr03")
         .build();

        DataMap db = dataMap().with(
            dbEntity("table1").attributes(
                dbAttr("attr01").typeInt(),
                dbAttr("attr02").typeInt()),

            dbEntity("table2").attributes(
                dbAttr("attr01").typeInt().primaryKey(),
                dbAttr("attr02").typeInt().primaryKey(),
                dbAttr("attr03").typeInt().primaryKey())
        ).join("rel", "table1.attr01", "table2.attr02")
         .join("rel1", "table1.attr01", "table2.attr03")
         .build();


        List<MergerToken> tokens = dbMerger().createMergeTokens(existing.getDbEntities(), db.getDbEntities());

        assertEquals(2, tokens.size());

        DbEntity entity = existing.getDbEntity("table1");
        assertEquals(factory().createDropRelationshipToDb(entity, entity.getRelationship("rel")).getTokenValue(),
                     tokens.get(0).getTokenValue());

        entity = db.getDbEntity("table1");
        assertEquals(factory().createAddRelationshipToDb(entity, entity.getRelationship("rel")).getTokenValue(),
                     tokens.get(0).getTokenValue());
    }

    @Test
    public void testRemoveRelationship() throws Exception {
        DataMap existing = dataMap().with(
            dbEntity("table1").attributes(
                dbAttr("attr01").typeInt(),
                dbAttr("attr02").typeInt()),

            dbEntity("table2").attributes(
                dbAttr("attr01").typeInt().primaryKey(),
                dbAttr("attr02").typeInt())
        )
         .build();

        DataMap db = dataMap().with(
            dbEntity("table1").attributes(
                dbAttr("attr01").typeInt(),
                dbAttr("attr02").typeInt()),

            dbEntity("table2").attributes(
                dbAttr("attr01").typeInt().primaryKey(),
                dbAttr("attr02").typeInt())
        ).join("rel", "table1.attr01", "table2.attr01")
         .build();


        List<MergerToken> tokens = dbMerger().createMergeTokens(existing.getDbEntities(), db.getDbEntities());

        assertEquals(1, tokens.size());

        DbEntity entity = db.getDbEntity("table1");
        assertEquals(factory().createDropRelationshipToDb(entity, entity.getRelationship("rel")).getTokenValue(),
                     tokens.get(0).getTokenValue());
    }

    @Test
    public void testRemoveColumn() throws Exception {
        DataMap existing = dataMap().with(
            dbEntity("table1").attributes(
                dbAttr("attr01").typeInt()
        )).build();

        DataMap db = dataMap().with(
            dbEntity("table1").attributes(
                dbAttr("attr01").typeInt(),
                dbAttr("attr02").typeInt()
        )).build();

        List<MergerToken> tokens = dbMerger().createMergeTokens(existing.getDbEntities(), db.getDbEntities());

        assertEquals(1, tokens.size());

        DbEntity entity = db.getDbEntity("table1");
        assertEquals(factory().createDropColumnToModel(entity, entity.getAttribute("attr02")).getTokenValue(),
                     tokens.get(0).getTokenValue());
    }

    @Test
    public void testNoChanges() throws Exception {
        DataMap dataMap1 = dataMap().with(
                dbEntity("table1").attributes(
                        dbAttr("attr01").typeInt(),
                        dbAttr("attr02").typeInt(),
                        dbAttr("attr03").typeInt()
                )).build();

        DataMap dataMap2 = dataMap().with(
            dbEntity("table1").attributes(
                dbAttr("attr01").typeInt(),
                dbAttr("attr02").typeInt(),
                dbAttr("attr03").typeInt()
        )).build();


        assertEquals(0, dbMerger().createMergeTokens(dataMap1.getDbEntities(), dataMap2.getDbEntities()).size());
    }

    private DbMerger dbMerger() {
        return new DbMerger(factory());
    }

    private HSQLMergerFactory factory() {
        return new HSQLMergerFactory();
    }
}