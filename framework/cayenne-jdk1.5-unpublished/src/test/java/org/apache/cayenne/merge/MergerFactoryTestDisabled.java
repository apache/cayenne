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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.QueryLogger;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.util.Util;

public class MergerFactoryTestDisabled extends CayenneCase {

    private DataNode node;
    private DataMap map;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();
        createTestData("testArtists");
        node = getDomain().getDataNodes().iterator().next();
        map = getDomain().getMap("testmap");

        // clone the DataMap so that we can manipulate it without worries
        map = (DataMap) Util.cloneViaSerialization(map);

        filterDataMap(node, map);

        DbMerger merger = new DbMerger();
        List<MergerToken> tokens = merger.createMergeTokens(node, map);
        execute(map, node, tokens);

        assertTokensAndExecute(node, map, 0, 0);
    }

    public void XXtestAddAndDropColumnToDb() throws Exception {
        DbEntity dbEntity = map.getDbEntity("PAINTING");
        assertNotNull(dbEntity);

        // create and add new column to model and db
        DbAttribute column = new DbAttribute("NEWCOL1", Types.VARCHAR, dbEntity);
        column.setMandatory(false);
        column.setMaxLength(10);
        dbEntity.addAttribute(column);
        assertTokensAndExecute(node, map, 1, 0);

        // try merge once more to check that is was merged
        assertTokensAndExecute(node, map, 0, 0);

        // remove it from model and db
        dbEntity.removeAttribute(column.getName());
        assertTokensAndExecute(node, map, 1, 0);
        assertTokensAndExecute(node, map, 0, 0);
    }

    public void XXtestChangeVarcharSizeToDb() throws Exception {
        DbEntity dbEntity = map.getDbEntity("PAINTING");
        assertNotNull(dbEntity);

        // create and add new column to model and db
        DbAttribute column = new DbAttribute("NEWCOL2", Types.VARCHAR, dbEntity);
        column.setMandatory(false);
        column.setMaxLength(10);
        dbEntity.addAttribute(column);
        assertTokensAndExecute(node, map, 1, 0);

        // check that is was merged
        assertTokensAndExecute(node, map, 0, 0);

        // change size
        column.setMaxLength(20);

        // merge to db
        assertTokensAndExecute(node, map, 1, 0);

        // check that is was merged
        assertTokensAndExecute(node, map, 0, 0);

        // clean up
        dbEntity.removeAttribute(column.getName());
        assertTokensAndExecute(node, map, 1, 0);
        assertTokensAndExecute(node, map, 0, 0);
    }

    public void XXtestMultipleTokensToDb() throws Exception {
        DbEntity dbEntity = map.getDbEntity("PAINTING");
        assertNotNull(dbEntity);

        DbAttribute column1 = new DbAttribute("NEWCOL3", Types.VARCHAR, dbEntity);
        column1.setMandatory(false);
        column1.setMaxLength(10);
        dbEntity.addAttribute(column1);
        DbAttribute column2 = new DbAttribute("NEWCOL4", Types.VARCHAR, dbEntity);
        column2.setMandatory(false);
        column2.setMaxLength(10);
        dbEntity.addAttribute(column2);
        assertTokensAndExecute(node, map, 2, 0);

        // check that is was merged
        assertTokensAndExecute(node, map, 0, 0);

        // change size
        column1.setMaxLength(20);
        column2.setMaxLength(30);

        // merge to db
        assertTokensAndExecute(node, map, 2, 0);

        // check that is was merged
        assertTokensAndExecute(node, map, 0, 0);

        // clean up
        dbEntity.removeAttribute(column1.getName());
        dbEntity.removeAttribute(column2.getName());
        assertTokensAndExecute(node, map, 2, 0);
        assertTokensAndExecute(node, map, 0, 0);
    }

    public void testAddTableToDb() throws Exception {
        dropTableIfPresent(node, "NEW_TABLE");

        assertTokensAndExecute(node, map, 0, 0);

        DbEntity dbEntity = new DbEntity("NEW_TABLE");

        DbAttribute column1 = new DbAttribute("ID", Types.INTEGER, dbEntity);
        column1.setMandatory(true);
        column1.setPrimaryKey(true);
        dbEntity.addAttribute(column1);

        DbAttribute column2 = new DbAttribute("NAME", Types.VARCHAR, dbEntity);
        column2.setMaxLength(10);
        column2.setMandatory(false);
        dbEntity.addAttribute(column2);

        map.addDbEntity(dbEntity);

        assertTokensAndExecute(node, map, 1, 0);
        assertTokensAndExecute(node, map, 0, 0);

        ObjEntity objEntity = new ObjEntity("NewTable");
        objEntity.setDbEntity(dbEntity);
        ObjAttribute oatr1 = new ObjAttribute("name");
        oatr1.setDbAttribute(column2);
        oatr1.setType("java.lang.String");
        objEntity.addAttribute(oatr1);
        map.addObjEntity(objEntity);

        // try to insert some rows to check that pk stuff is working
        DataContext ctxt = createDataContext();
        for (int i = 0; i < 5; i++) {
            CayenneDataObject dao = (CayenneDataObject) ctxt.newObject(objEntity
                    .getName());
            dao.writeProperty(oatr1.getName(), "test " + i);
        }
        ctxt.commitChanges();

        // clear up
        map.removeObjEntity(objEntity.getName(), true);
        map.removeDbEntity(dbEntity.getName(), true);
        ctxt.getEntityResolver().clearCache();
        assertNull(map.getObjEntity(objEntity.getName()));
        assertNull(map.getDbEntity(dbEntity.getName()));
        assertFalse(map.getDbEntities().contains(dbEntity));
        assertTokensAndExecute(node, map, 0, 0);
    }

    public void XXtestAddForeignKeyWithTable() throws Exception {
        dropTableIfPresent(node, "NEW_TABLE");

        assertTokensAndExecute(node, map, 0, 0);

        DbEntity dbEntity = new DbEntity("NEW_TABLE");

        DbAttribute column1 = new DbAttribute("ID", Types.INTEGER, dbEntity);
        column1.setMandatory(true);
        column1.setPrimaryKey(true);
        dbEntity.addAttribute(column1);

        DbAttribute column2 = new DbAttribute("NAME", Types.VARCHAR, dbEntity);
        column2.setMaxLength(10);
        column2.setMandatory(false);
        dbEntity.addAttribute(column2);

        DbAttribute column3 = new DbAttribute("ARTIST_ID", Types.INTEGER, dbEntity);
        column3.setMandatory(false);
        dbEntity.addAttribute(column3);

        map.addDbEntity(dbEntity);

        DbEntity artistDbEntity = map.getDbEntity("ARTIST");
        assertNotNull(artistDbEntity);

        // relation from new_table to artist
        DbRelationship r1 = new DbRelationship("toArtistR1");
        r1.setSourceEntity(dbEntity);
        r1.setTargetEntity(artistDbEntity);
        r1.setToMany(false);
        r1.addJoin(new DbJoin(r1, "ARTIST_ID", "ARTIST_ID"));
        dbEntity.addRelationship(r1);

        // relation from artist to new_table
        DbRelationship r2 = new DbRelationship("toNewTableR2");
        r2.setSourceEntity(artistDbEntity);
        r2.setTargetEntity(dbEntity);
        r2.setToMany(true);
        r2.addJoin(new DbJoin(r2, "ARTIST_ID", "ARTIST_ID"));
        artistDbEntity.addRelationship(r2);

        assertTokensAndExecute(node, map, 2, 0);
        assertTokensAndExecute(node, map, 0, 0);

        DataContext ctxt = createDataContext();

        // remove relationships
        dbEntity.removeRelationship(r1.getName());
        artistDbEntity.removeRelationship(r2.getName());
        ctxt.getEntityResolver().clearCache();
        assertTokensAndExecute(node, map, 1, 1);
        assertTokensAndExecute(node, map, 0, 0);

        // clear up
        // map.removeObjEntity(objEntity.getName(), true);
        map.removeDbEntity(dbEntity.getName(), true);
        ctxt.getEntityResolver().clearCache();
        // assertNull(map.getObjEntity(objEntity.getName()));
        assertNull(map.getDbEntity(dbEntity.getName()));
        assertFalse(map.getDbEntities().contains(dbEntity));
        assertTokensAndExecute(node, map, 0, 0);
    }

    public void XXtestAddForeignKeyAfterTable() throws Exception {
        dropTableIfPresent(node, "NEW_TABLE");

        assertTokensAndExecute(node, map, 0, 0);

        DbEntity dbEntity = new DbEntity("NEW_TABLE");

        DbAttribute column1 = new DbAttribute("ID", Types.INTEGER, dbEntity);
        column1.setMandatory(true);
        column1.setPrimaryKey(true);
        dbEntity.addAttribute(column1);

        DbAttribute column2 = new DbAttribute("NAME", Types.VARCHAR, dbEntity);
        column2.setMaxLength(10);
        column2.setMandatory(false);
        dbEntity.addAttribute(column2);

        DbAttribute column3 = new DbAttribute("ARTIST_ID", Types.INTEGER, dbEntity);
        column3.setMandatory(false);
        dbEntity.addAttribute(column3);

        map.addDbEntity(dbEntity);

        DbEntity artistDbEntity = map.getDbEntity("ARTIST");
        assertNotNull(artistDbEntity);

        assertTokensAndExecute(node, map, 1, 0);
        assertTokensAndExecute(node, map, 0, 0);

        // relation from new_table to artist
        DbRelationship r1 = new DbRelationship("toArtistR1");
        r1.setSourceEntity(dbEntity);
        r1.setTargetEntity(artistDbEntity);
        r1.setToMany(false);
        r1.addJoin(new DbJoin(r1, "ARTIST_ID", "ARTIST_ID"));
        dbEntity.addRelationship(r1);

        // relation from artist to new_table
        DbRelationship r2 = new DbRelationship("toNewTableR2");
        r2.setSourceEntity(artistDbEntity);
        r2.setTargetEntity(dbEntity);
        r2.setToMany(true);
        r2.addJoin(new DbJoin(r2, "ARTIST_ID", "ARTIST_ID"));
        artistDbEntity.addRelationship(r2);

        assertTokensAndExecute(node, map, 1, 0);
        assertTokensAndExecute(node, map, 0, 0);

        DataContext ctxt = createDataContext();

        // remove relationships
        dbEntity.removeRelationship(r1.getName());
        artistDbEntity.removeRelationship(r2.getName());
        ctxt.getEntityResolver().clearCache();
        assertTokensAndExecute(node, map, 1, 1);
        assertTokensAndExecute(node, map, 0, 0);

        // clear up
        // map.removeObjEntity(objEntity.getName(), true);
        map.removeDbEntity(dbEntity.getName(), true);
        ctxt.getEntityResolver().clearCache();
        // assertNull(map.getObjEntity(objEntity.getName()));
        assertNull(map.getDbEntity(dbEntity.getName()));
        assertFalse(map.getDbEntities().contains(dbEntity));
        assertTokensAndExecute(node, map, 0, 0);
    }

    private void assertTokensAndExecute(
            DataNode node,
            DataMap map,
            int expectedToDb,
            int expectedToModel) throws Exception {
        DbMerger merger = new DbMerger();
        List<MergerToken> tokens = merger.createMergeTokens(node, map);

        assertTokens(tokens, expectedToDb, expectedToModel);
        if (!tokens.isEmpty()) {
            execute(map, node, tokens);
        }
    }

    private void assertTokens(
            List<MergerToken> tokens,
            int expectedToDb,
            int expectedToModel) {
        int actualToDb = 0;
        int actualToModel = 0;
        for (MergerToken token : tokens) {
            if (token.getDirection().equals(MergeDirection.TO_DB)) {
                actualToDb++;
            }
            else if (token.getDirection().equals(MergeDirection.TO_MODEL)) {
                actualToModel++;
            }
        }
        logTokens(tokens);
        assertEquals("tokens to db", expectedToDb, actualToDb);
        assertEquals("tokens to model", expectedToModel, actualToModel);
    }

    private void logTokens(List<MergerToken> tokens) {
        for (MergerToken token : tokens) {
            QueryLogger.log("token: " + token.toString());
            if (token instanceof AbstractToDbToken) {
                QueryLogger.log("  \\-->  "
                        + ((AbstractToDbToken) token).createSql(node.getAdapter()));
            }
        }
    }

    private void dropTableIfPresent(DataNode node, String tableName) {
        DbEntity entity = new DbEntity(tableName);
        AbstractToDbToken t = (AbstractToDbToken) node
                .getAdapter()
                .mergerFactory()
                .createDropTableToDb(entity);
        try {
            for (String sql : t.createSql(node.getAdapter())) {
                executeSql(sql);
            }
        }
        catch (Exception e) {
        }
    }

    /**
     * Remote binary pk {@link DbEntity} for {@link DbAdapter} not supporting that and so
     * on.
     */
    private void filterDataMap(DataNode node, DataMap map) {
        // copied from AbstractAccessStack.dbEntitiesInInsertOrder
        boolean excludeLOB = !getAccessStackAdapter().supportsLobs();
        boolean excludeBinPK = !getAccessStackAdapter().supportsBinaryPK();

        if (!(excludeLOB || excludeBinPK)) {
            return;
        }

        List<DbEntity> entitiesToRemove = new ArrayList<DbEntity>();

        for (DbEntity ent : map.getDbEntities()) {

            if (excludeBinPK) {
                for (DbAttribute attr : ent.getAttributes()) {
                    // check for BIN PK or FK to BIN Pk
                    if (attr.getType() == Types.BINARY
                            || attr.getType() == Types.VARBINARY
                            || attr.getType() == Types.LONGVARBINARY) {

                        if (attr.isPrimaryKey() || attr.isForeignKey()) {
                            entitiesToRemove.add(ent);
                            break;
                        }
                    }
                }

            }
        }

        for (DbEntity e : entitiesToRemove) {
            QueryLogger.log("filter away " + e.getName());
            map.removeDbEntity(e.getName(), true);
        }

    }

    private void execute(DataMap map, DataNode node, List<MergerToken> tokens)
            throws Exception {
        MergerContext mergerContext = new ExecutingMergerContext(map, node);
        for (MergerToken tok : tokens) {
            tok.execute(mergerContext);
        }

    }

    private void executeSql(String sql) throws Exception {
        Connection conn = null;
        Statement st = null;
        try {
            QueryLogger.log(sql);
            conn = getConnection();
            st = conn.createStatement();
            st.execute(sql);
        }
        catch (SQLException e) {
            QueryLogger.logQueryError(e);
            throw e;
        }
        finally {
            if (st != null) {
                st.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        deleteTestData();
    }

}
