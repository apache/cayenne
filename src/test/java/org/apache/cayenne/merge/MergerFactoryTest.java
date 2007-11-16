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
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.QueryLogger;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.unit.CayenneCase;

public class MergerFactoryTest extends CayenneCase {

    /**
     * Check that an up to date database are detected as up to date.
     */
    public void testUpToDateToDb() throws Exception {
        deleteTestData();
        createTestData("testArtists");
        DataNode node = (DataNode) getDomain().getDataNodes().iterator().next();
        DataMap map = getDomain().getMap("testmap");
        filterDataMap(node, map);

        assertInSync(node, map);
    }

    public void testAddAndDropColumnToDb() throws Exception {
        deleteTestData();
        createTestData("testArtists");
        DataNode node = (DataNode) getDomain().getDataNodes().iterator().next();
        DataMap map = getDomain().getMap("testmap");
        filterDataMap(node, map);

        DbEntity dbEntity = map.getDbEntity("PAINTING");
        assertNotNull(dbEntity);

        // create and add new column to model and db
        DbAttribute column = new DbAttribute("NEWCOL1", Types.VARCHAR, dbEntity);
        column.setMandatory(false);
        column.setMaxLength(10);
        dbEntity.addAttribute(column);
        mergeToDb(node, map, 1);

        // try merge once more to check that is was merged
        assertInSync(node, map);

        // remove it from model and db
        dbEntity.removeAttribute(column.getName());
        mergeToDb(node, map, 1);

        assertInSync(node, map);
    }

    public void testChangeVarcharSizeToDb() throws Exception {
        deleteTestData();
        createTestData("testArtists");
        DataNode node = (DataNode) getDomain().getDataNodes().iterator().next();
        DataMap map = getDomain().getMap("testmap");
        filterDataMap(node, map);

        DbEntity dbEntity = map.getDbEntity("PAINTING");
        assertNotNull(dbEntity);

        // create and add new column to model and db
        DbAttribute column = new DbAttribute("NEWCOL2", Types.VARCHAR, dbEntity);
        column.setMandatory(false);
        column.setMaxLength(10);
        dbEntity.addAttribute(column);
        mergeToDb(node, map, 1);

        // check that is was merged
        assertInSync(node, map);

        // change size
        column.setMaxLength(20);

        // merge to db
        mergeToDb(node, map, 1);

        // check that is was merged
        assertInSync(node, map);

        // clean up
        dbEntity.removeAttribute(column.getName());
        mergeToDb(node, map, 1);
        assertInSync(node, map);
    }

    public void testMultipleTokensToDb() throws Exception {
        deleteTestData();
        createTestData("testArtists");
        DataNode node = (DataNode) getDomain().getDataNodes().iterator().next();
        DataMap map = getDomain().getMap("testmap");
        filterDataMap(node, map);

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
        mergeToDb(node, map, 2);

        // check that is was merged
        assertInSync(node, map);

        // change size
        column1.setMaxLength(20);
        column2.setMaxLength(30);

        // merge to db
        mergeToDb(node, map, 2);

        // check that is was merged
        assertInSync(node, map);

        // clean up
        dbEntity.removeAttribute(column1.getName());
        dbEntity.removeAttribute(column2.getName());
        mergeToDb(node, map, 2);
        assertInSync(node, map);
    }

    public void testAddTableToDb() throws Exception {

        deleteTestData();

        createTestData("testArtists");
        DataNode node = (DataNode) getDomain().getDataNodes().iterator().next();
        DataMap map = getDomain().getMap("testmap");
        filterDataMap(node, map);

        dropTableIfPresent(node, "NEW_TABLE");

        assertInSync(node, map);

        DbEntity dbEntity = new DbEntity("NEW_TABLE");

        DbAttribute column1 = new DbAttribute("ID", Types.INTEGER, dbEntity);
        column1.setMandatory(true);
        dbEntity.addAttribute(column1);

        DbAttribute column2 = new DbAttribute("NAME", Types.VARCHAR, dbEntity);
        column2.setMaxLength(10);
        column2.setMandatory(false);
        dbEntity.addAttribute(column2);

        map.addDbEntity(dbEntity);

        mergeToDb(node, map, 1);
        assertInSync(node, map);

        // clear up
        map.removeDbEntity(dbEntity.getName(), true);
        assertNull(map.getDbEntity(dbEntity.getName()));
        assertFalse(map.getDbEntities().contains(dbEntity));
        // TODO: mergeToDb(node, map, 1);
        assertInSync(node, map);
    }

    private void assertInSync(DataNode node, DataMap map) {
        DbMerger merger = new DbMerger();
        List tokens = merger.createMergeTokens(node, map);
        /*
         * if (tokens.size() > 0) { QueryLogger.log("should be up to date, but missing:\n" +
         * tokens.createSql(node.getAdapter())); }
         */
        assertEquals(0, tokens.size());
    }

    private void mergeToDb(DataNode node, DataMap map, int expectedChanges)
            throws Exception {
        DbMerger merger = new DbMerger();
        List tokens = merger.createMergeTokens(node, map);
        /*
         * if (expectedChanges != tokens.size()) {
         * QueryLogger.log(tokens.createSql(node.getAdapter())); }
         */
        assertEquals(expectedChanges, tokens.size());
        if (tokens.size() > 0) {
            execute(map, node, tokens);
        }
    }

    private void dropTableIfPresent(DataNode node, String tableName) {
        DbEntity entity = new DbEntity(tableName);
        AbstractToDbToken t = (AbstractToDbToken) node
                .getAdapter()
                .mergerFactory()
                .createDropTableToDb(entity);
        try {
            executeSql(t.createSql(node.getAdapter()));
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

        List entitiesToRemove = new ArrayList();

        for (Iterator it = map.getDbEntities().iterator(); it.hasNext();) {
            DbEntity ent = (DbEntity) it.next();

            if (excludeBinPK) {
                boolean skip = false;
                Iterator attrs = ent.getAttributes().iterator();
                while (attrs.hasNext()) {
                    // check for BIN PK or FK to BIN Pk
                    DbAttribute attr = (DbAttribute) attrs.next();
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

        for (Iterator it = entitiesToRemove.iterator(); it.hasNext();) {
            DbEntity e = (DbEntity) it.next();
            map.removeDbEntity(e.getName(), true);
        }
    }

    private void execute(DataMap map, DataNode node, List tokens) throws Exception {

        MergerContext mergerContext = new ExecutingMergerContext(map, node);

        for (Iterator it = tokens.iterator(); it.hasNext();) {
            MergerToken tok = (MergerToken) it.next();
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

    /*
     * protected void tearDown() throws Exception { super.tearDown(); deleteTestData(); }
     */

}
