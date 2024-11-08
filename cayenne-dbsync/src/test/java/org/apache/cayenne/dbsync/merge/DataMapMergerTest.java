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

import java.util.List;

import org.apache.cayenne.dbsync.merge.builders.DbEntityBuilder;
import org.apache.cayenne.dbsync.merge.factory.HSQLMergerTokenFactory;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.dbsync.merge.token.db.SetColumnTypeToDb;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.apache.cayenne.dbsync.reverse.filters.PatternFilter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ProcedureParameter;
import org.junit.Test;

import static org.apache.cayenne.dbsync.merge.builders.ObjectMother.dataMap;
import static org.apache.cayenne.dbsync.merge.builders.ObjectMother.dbAttr;
import static org.apache.cayenne.dbsync.merge.builders.ObjectMother.dbEntity;
import static org.apache.cayenne.dbsync.merge.builders.ObjectMother.procedure;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DataMapMergerTest {

    @Test
    public void testEmptyDataMap() throws Exception {
        DataMap existing = dataMap().build();
        DataMap db = dataMap().build();
        assertEquals(0, dbMerger().createMergeTokens(existing, db).size());
    }

    @Test
    public void testAddTable() throws Exception {
        DbEntityBuilder dbEntity =
            dbEntity("table1").attributes(
                dbAttr("attr01").typeInt()
        );
        DataMap existing = dataMap().with(dbEntity).build();
        DataMap db = dataMap().build();

        List<MergerToken> tokens = dbMerger().createMergeTokens(existing, db);

        assertEquals(1, tokens.size());
        assertEquals(factory().createCreateTableToDb(dbEntity.build()).getTokenValue(),
                     tokens.get(0).getTokenValue());
    }

    @Test
    public void testRemoveTable() throws Exception {
        DataMap existing = dataMap().build();
        DataMap db = dataMap().with(
            dbEntity("table1").attributes(
                dbAttr("attr01").typeInt()
        )).build();

        List<MergerToken> tokens = dbMerger().createMergeTokens(existing, db);

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

        List<MergerToken> tokens = dbMerger().createMergeTokens(existing, db);

        assertEquals(1, tokens.size());

        DbEntity entity = existing.getDbEntity("table1");
        assertEquals(factory().createAddColumnToDb(entity, entity.getAttribute("attr02")).getTokenValue(),
                     tokens.get(0).getTokenValue());
    }

    @Test
    public void testChangeColumnTypeSimple() throws Exception {
        DataMap existing = dataMap().with(
                dbEntity("table1").attributes(
                        dbAttr("attr01").typeInt()
                )).build();

        DataMap db = dataMap().with(
                dbEntity("table1").attributes(
                        dbAttr("attr01").typeVarchar(30)
                )).build();

        List<MergerToken> tokens = dbMerger().createMergeTokens(existing, db);
        assertEquals(1, tokens.size());

        DbEntity entity = existing.getDbEntity("table1");
        DbEntity entityDb = db.getDbEntity("table1");
        assertTrue(tokens.get(0) instanceof SetColumnTypeToDb);

        assertEquals(
                factory()
                        .createSetColumnTypeToDb(entity, entityDb.getAttribute("attr01"), entity.getAttribute("attr01"))
                        .getTokenValue(),
                tokens.get(0)
                        .getTokenValue()
        );
    }

    @Test
    public void testChangeColumnLength() throws Exception {
        DataMap existing = dataMap().with(
                dbEntity("table1").attributes(
                        dbAttr("attr01").typeVarchar(60)
                )).build();

        DataMap db = dataMap().with(
                dbEntity("table1").attributes(
                        dbAttr("attr01").typeVarchar(30)
                )).build();

        List<MergerToken> tokens = dbMerger().createMergeTokens(existing, db);
        assertEquals(1, tokens.size());

        DbEntity entity = existing.getDbEntity("table1");
        DbEntity entityDb = db.getDbEntity("table1");
        assertTrue(tokens.get(0) instanceof SetColumnTypeToDb);

        assertEquals(
                factory()
                        .createSetColumnTypeToDb(entity, entityDb.getAttribute("attr01"), entity.getAttribute("attr01"))
                        .getTokenValue(),
                tokens.get(0)
                        .getTokenValue()
        );
    }

    /**
     * Test unsupported type changes
     */
    @Test
    public void testChangeColumnType() throws Exception {
        DbEntity fromModel = dbEntity("table1").attributes(
                dbAttr("attr01").typeInt(),
                dbAttr("attr02").type("DATE"),
                dbAttr("attr03").type("BOOLEAN"),
                dbAttr("attr04").type("FLOAT")
        ).build();
        DataMap existing = dataMap().with(fromModel).build();

        DbEntity fromDb = dbEntity("table1").attributes(
                dbAttr("attr01").typeBigInt(),
                dbAttr("attr02").type("NUMERIC"),
                dbAttr("attr03").type("BLOB"),
                dbAttr("attr04").type("TIMESTAMP")
        ).build();
        DataMap db = dataMap().with(fromDb).build();

        List<MergerToken> tokens = dbMerger().createMergeTokens(existing, db);
        assertEquals(4, tokens.size());
        for(MergerToken token : tokens) {
            assertTrue(token instanceof SetColumnTypeToDb);
        }

        MergerToken attr02Token = findChangeTypeToken(tokens, "attr02");
        assertNotNull(attr02Token);
        assertEquals(
                factory()
                        .createSetColumnTypeToDb(fromModel, fromDb.getAttribute("attr02"), fromModel.getAttribute("attr02"))
                        .getTokenValue(),
                attr02Token
                        .getTokenValue()
        );


    }

    private MergerToken findChangeTypeToken(List<MergerToken> tokens, String attributeName) {
        for(MergerToken token : tokens) {
            if(token.getTokenValue().contains("." + attributeName)) {
                return token;
            }
        }
        return null;
    }

    @Test
    public void testDropPrimaryKey() throws Exception {
        DataMap existing = dataMap().with(
                dbEntity("table1").attributes(
                        dbAttr("attr01").typeInt()
                )
        ).build();

        DataMap db = dataMap().with(
                dbEntity("table1").attributes(
                        dbAttr("attr01").typeInt().primaryKey()
                )
        ).build();

        List<MergerToken> tokens = dbMerger().createMergeTokens(existing, db);
        assertEquals(1, tokens.size());
    }

    @Test
    public void testAddPrimaryKey() throws Exception {
        DataMap existing = dataMap().with(
                dbEntity("table1").attributes(
                        dbAttr("attr01").typeInt().primaryKey()
                )
        ).build();

        DataMap db = dataMap().with(
                dbEntity("table1").attributes(
                        dbAttr("attr01").typeInt()
                )
        ).build();

        List<MergerToken> tokens = dbMerger().createMergeTokens(existing, db);
        assertEquals(1, tokens.size());
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
        ).join("rel", "table1.attr01", "table2.attr01", true)
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


        List<MergerToken> tokens = dbMerger().createMergeTokens(existing, db);

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
        ).join("rel", "table1.attr01", "table2.attr01",true)
         .join("rel1", "table1.attr01", "table2.attr03",false)
         .build();

        DataMap db = dataMap().with(
            dbEntity("table1").attributes(
                dbAttr("attr01").typeInt(),
                dbAttr("attr02").typeInt()),

            dbEntity("table2").attributes(
                dbAttr("attr01").typeInt().primaryKey(),
                dbAttr("attr02").typeInt().primaryKey(),
                dbAttr("attr03").typeInt().primaryKey())
        ).join("rel", "table1.attr01", "table2.attr02",true)
         .join("rel1", "table1.attr01", "table2.attr03",false)
         .build();


        List<MergerToken> tokens = dbMerger().createMergeTokens(existing, db);

        assertEquals(2, tokens.size());

        DbEntity entity = existing.getDbEntity("table1");
        assertEquals(factory().createDropRelationshipToDb(entity, entity.getRelationship("rel")).getTokenValue(),
                     tokens.get(0).getTokenValue());

        entity = db.getDbEntity("table1");
        assertEquals(factory().createAddRelationshipToDb(entity, entity.getRelationship("rel")).getTokenValue(),
                     tokens.get(0).getTokenValue());
    }

    @Test
    public void testTableNameUppercaseRelationship() throws Exception {
        DataMap existing = dataMap().with(
                dbEntity("TABLE1").attributes(
                        dbAttr("attr01").typeInt(),
                        dbAttr("attr02").typeInt()),

                dbEntity("table2").attributes(
                        dbAttr("attr01").typeInt().primaryKey(),
                        dbAttr("attr02").typeInt().primaryKey(),
                        dbAttr("attr03").typeInt().primaryKey())
        ).join("rel", "TABLE1.attr01", "table2.attr01",true).build();

        DataMap db = dataMap().with(
                dbEntity("table1").attributes(
                        dbAttr("attr01").typeInt(),
                        dbAttr("attr02").typeInt()),

                dbEntity("table2").attributes(
                        dbAttr("attr01").typeInt().primaryKey(),
                        dbAttr("attr02").typeInt().primaryKey(),
                        dbAttr("attr03").typeInt().primaryKey())
        ).join("rel", "table1.attr01", "table2.attr01",false).build();


        List<MergerToken> tokens = dbMerger().createMergeTokens(existing, db);
        assertEquals(0, tokens.size());
    }

    @Test
    public void testAttributeNameUppercaseRelationship() throws Exception {
        DataMap existing = dataMap().with(
                dbEntity("table1").attributes(
                        dbAttr("ATTR01").typeInt(),
                        dbAttr("attr02").typeInt()),

                dbEntity("table2").attributes(
                        dbAttr("attr01").typeInt().primaryKey(),
                        dbAttr("attr02").typeInt().primaryKey(),
                        dbAttr("attr03").typeInt().primaryKey())
        ).join("rel", "table1.ATTR01", "table2.attr01",true).build();

        DataMap db = dataMap().with(
                dbEntity("table1").attributes(
                        dbAttr("attr01").typeInt(),
                        dbAttr("attr02").typeInt()),

                dbEntity("table2").attributes(
                        dbAttr("attr01").typeInt().primaryKey(),
                        dbAttr("attr02").typeInt().primaryKey(),
                        dbAttr("attr03").typeInt().primaryKey())
        ).join("rel", "table1.attr01", "table2.attr01",false).build();


        List<MergerToken> tokens = dbMerger().createMergeTokens(existing, db);
        assertEquals(0, tokens.size());
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
        ).join("rel", "table1.attr01", "table2.attr01",true)
         .build();


        List<MergerToken> tokens = dbMerger().createMergeTokens(existing, db);

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

        List<MergerToken> tokens = dbMerger().createMergeTokens(existing, db);

        assertEquals(1, tokens.size());

        DbEntity entity = db.getDbEntity("table1");
        assertEquals(factory().createDropColumnToModel(entity, entity.getAttribute("attr02")).getTokenValue(),
                     tokens.get(0).getTokenValue());
    }

    @Test
    public void testChangeGeneratedStatus() {
        DataMap existing = dataMap().with(
                dbEntity("table1").attributes(
                        dbAttr("attr01").typeVarchar(10)
                )).build();

        DataMap db = dataMap().with(
                dbEntity("table1").attributes(
                        dbAttr("attr01").typeInt().generated()
                )).build();

        List<MergerToken> tokens = dbMerger().createMergeTokens(existing, db);
        assertEquals(2, tokens.size());
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


        assertEquals(0, dbMerger().createMergeTokens(dataMap1, dataMap2).size());
    }

    @Test
    public void testProcedures() {
        DataMap dataMap1 = dataMap().with(
               procedure("proc1")
                       .callParameters(new ProcedureParameter("test"))
        ).build();

        DataMap dataMap2 = dataMap().build();

        PatternFilter patternFilter = new PatternFilter();
        patternFilter.include("proc1");
        FiltersConfig filtersConfig = FiltersConfig
                .create(null, null, null, patternFilter);
        DataMapMerger merger = DataMapMerger
                .builder(factory())
                .filters(filtersConfig)
                .build();
        assertEquals(1, merger.createMergeTokens(dataMap1, dataMap2).size());
    }

    private DataMapMerger dbMerger() {
        return DataMapMerger.build(factory());
    }

    private HSQLMergerTokenFactory factory() {
        return new HSQLMergerTokenFactory();
    }
}