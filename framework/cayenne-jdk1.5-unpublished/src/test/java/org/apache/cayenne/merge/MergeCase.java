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

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.QueryLogger;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;

public class MergeCase extends CayenneCase {

    protected DataDomain dom;
    protected DataNode node;
    protected DataMap map;

    private final Log logObj = LogFactory.getLog(getClass());

    private static List<String> tableNames = Arrays.asList(
            "ARTIST",
            "PAINTING",
            "NEW_TABLE",
            "NEW_TABLE2");

    protected DbMerger createMerger() {
        return new DbMerger() {

            @Override
            public boolean includeTableName(String tableName) {
                return tableNames.contains(tableName.toUpperCase());
            }
        };
    }

    protected List<MergerToken> createMergeTokens() {
        return createMerger().createMergeTokens(node, map);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();
        createTestData("testArtists");
        DataNode orgNode = getDomain().getDataNodes().iterator().next();
        
        
        // clone DataMap by saving and loading from XML as to avoid modifying shared test
        // DataMap
        DataMap originalMap = getDomain().getMap("testmap");
        StringWriter out = new StringWriter();
        PrintWriter outWriter = new PrintWriter(out);
        originalMap.encodeAsXML(outWriter);
        outWriter.flush();
        StringReader in = new StringReader(out.toString());
        map = new MapLoader().loadDataMap(new InputSource(in));
        
        // map must operate in an EntityResolve namespace...
        EntityResolver testResolver = new EntityResolver();
        testResolver.addDataMap(map);

        node = new DataNode("mergenode");
        node.setAdapter(orgNode.getAdapter());
        node.setDataSource(orgNode.getDataSource());
        node.addDataMap(map);
        
        dom = new DataDomain("mergetestdomain");
        dom.addNode(node);

        filterDataMap(node, map);

        List<MergerToken> tokens = createMergeTokens();
        execute(tokens);

        assertTokensAndExecute(0, 0);
    }

    /**
     * Remote binary pk {@link DbEntity} for {@link DbAdapter} not supporting that and so
     * on.
     */
    private void filterDataMap(DataNode node, DataMap map) {
        // copied from AbstractAccessStack.dbEntitiesInInsertOrder
        boolean excludeBinPK = getAccessStackAdapter().supportsBinaryPK();

        if (!excludeBinPK) { 
            return;
        }

        List<DbEntity> entitiesToRemove = new ArrayList<DbEntity>();

        for (DbEntity ent : map.getDbEntities()) {
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

        for (DbEntity e : entitiesToRemove) {
            logObj.info("filter away " + e.getName());
            map.removeDbEntity(e.getName(), true);
        }

    }

    protected void execute(List<MergerToken> tokens) throws Exception {
        MergerContext mergerContext = new ExecutingMergerContext(map, node);
        for (MergerToken tok : tokens) {
            tok.execute(mergerContext);
        }
    }

    protected void execute(MergerToken token) throws Exception {
        MergerContext mergerContext = new ExecutingMergerContext(map, node);
        token.execute(mergerContext);
    }

    protected void executeSql(String sql) throws Exception {
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

    protected void assertTokens(
            List<MergerToken> tokens,
            int expectedToDb,
            int expectedToModel) {
        int actualToDb = 0;
        int actualToModel = 0;
        for (MergerToken token : tokens) {
            if (token.getDirection().isToDb()) {
                actualToDb++;
            }
            else if (token.getDirection().isToModel()) {
                actualToModel++;
            }
        }
        logTokens(tokens);
        assertEquals("tokens to db", expectedToDb, actualToDb);
        assertEquals("tokens to model", expectedToModel, actualToModel);
    }

    protected void assertTokensAndExecute(
            DataNode node,
            DataMap map,
            int expectedToDb,
            int expectedToModel) {
        List<MergerToken> tokens = createMergeTokens();

        assertTokens(tokens, expectedToDb, expectedToModel);
        if (!tokens.isEmpty()) {
            try {
                execute(tokens);
            }
            catch (Exception e) {
                fail(e.getMessage());
            }
        }
    }

    protected void assertTokensAndExecute(int expectedToDb, int expectedToModel) {
        List<MergerToken> tokens = createMergeTokens();

        assertTokens(tokens, expectedToDb, expectedToModel);
        if (!tokens.isEmpty()) {
            try {
                execute(tokens);
            }
            catch (Exception e) {
                fail(e.getMessage());
            }
        }
    }

    protected void logTokens(List<MergerToken> tokens) {
        for (MergerToken token : tokens) {
            logObj.info("token: " + token.toString());
        }
    }

    protected MergerFactory mergerFactory() {
        return node.getAdapter().mergerFactory();
    }

    protected void dropTableIfPresent(DataNode node, String tableName) {
        DbEntity entity = new DbEntity(tableName);
        AbstractToDbToken t = (AbstractToDbToken) mergerFactory().createDropTableToDb(
                entity);
        try {
            for (String sql : t.createSql(node.getAdapter())) {
                executeSql(sql);
            }
        }
        catch (Exception e) {
        }
    }
    
    @Override
    protected DataContext createDataContext() {
        return dom.createDataContext();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        deleteTestData();
    }

}
