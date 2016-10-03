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
package org.apache.cayenne.dbsync.merge;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactoryProvider;
import org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator;
import org.apache.cayenne.dbsync.reverse.db.DbLoader;
import org.apache.cayenne.dbsync.reverse.db.DbLoaderConfiguration;
import org.apache.cayenne.dbsync.reverse.db.LoggingDbLoaderDelegate;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.apache.cayenne.dbsync.reverse.filters.PatternFilter;
import org.apache.cayenne.dbsync.reverse.filters.TableFilter;
import org.apache.cayenne.dbsync.unit.DbSyncCase;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCaseDataSourceFactory;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public abstract class MergeCase extends DbSyncCase {

    @Inject
    protected EntityResolver resolver;
    @Inject
    protected DataNode node;
    protected DataMap map;
    private Log logger = LogFactory.getLog(MergeCase.class);
    @Inject
    private DBHelper dbHelper;
    @Inject
    private ServerRuntime runtime;
    @Inject
    private UnitDbAdapter accessStackAdapter;
    @Inject
    private ServerCaseDataSourceFactory dataSourceFactory;

    @Override
    public void cleanUpDB() throws Exception {
        dbHelper.update("ARTGROUP").set("PARENT_GROUP_ID", null, Types.INTEGER).execute();
        super.cleanUpDB();
    }

    @Before
    public void setUp() throws Exception {

        // this map can't be safely modified in this test, as it is reset by DI
        // container
        // on every test
        map = runtime.getDataDomain().getDataMap("testmap");

        filterDataMap();

        List<MergerToken> tokens = createMergeTokens();
        execute(tokens);

        assertTokensAndExecute(0, 0);
    }

    protected DbMerger.Builder merger() {
        return DbMerger.builder(mergerFactory());
    }

    protected List<MergerToken> createMergeTokens() {

        FiltersConfig filters = FiltersConfig.create(null, null,
                TableFilter.include("ARTIST|GALLERY|PAINTING|NEW_TABLE2?"), PatternFilter.INCLUDE_NOTHING);

        DbLoaderConfiguration loaderConfiguration = new DbLoaderConfiguration();
        loaderConfiguration.setFiltersConfig(filters);

        DataMap dbImport = new DataMap();
        try (Connection conn = node.getDataSource().getConnection();) {
            new DbLoader(conn,
                    node.getAdapter(),
                    new LoggingDbLoaderDelegate(LogFactory.getLog(DbLoader.class)),
                    new DefaultObjectNameGenerator())
                    .load(dbImport, loaderConfiguration);

        } catch (SQLException e) {
            throw new CayenneRuntimeException("Can't doLoad dataMap from db.", e);
        }

        return merger().filters(filters).build().createMergeTokens(map, dbImport);
    }

    /**
     * Remote binary pk {@link DbEntity} for {@link DbAdapter} not supporting
     * that and so on.
     */
    private void filterDataMap() {
        // copied from AbstractAccessStack.dbEntitiesInInsertOrder
        boolean excludeBinPK = accessStackAdapter.supportsBinaryPK();

        if (!excludeBinPK) {
            return;
        }

        List<DbEntity> entitiesToRemove = new ArrayList<DbEntity>();

        for (DbEntity ent : map.getDbEntities()) {
            for (DbAttribute attr : ent.getAttributes()) {
                // check for BIN PK or FK to BIN Pk
                if (attr.getType() == Types.BINARY || attr.getType() == Types.VARBINARY
                        || attr.getType() == Types.LONGVARBINARY) {

                    if (attr.isPrimaryKey() || attr.isForeignKey()) {
                        entitiesToRemove.add(ent);
                        break;
                    }
                }
            }
        }

        for (DbEntity e : entitiesToRemove) {
            map.removeDbEntity(e.getName(), true);
        }
    }

    protected void execute(List<MergerToken> tokens) {
        MergerContext mergerContext = MergerContext.builder(map).dataNode(node).build();
        for (MergerToken tok : tokens) {
            tok.execute(mergerContext);
        }
    }

    protected void execute(MergerToken token) throws Exception {
        MergerContext mergerContext = MergerContext.builder(map).dataNode(node).build();
        token.execute(mergerContext);
    }

    private void executeSql(String sql) throws Exception {

        try (Connection conn = dataSourceFactory.getSharedDataSource().getConnection();) {

            try (Statement st = conn.createStatement();) {
                st.execute(sql);
            }
        }
    }

    protected void assertTokens(List<MergerToken> tokens, int expectedToDb, int expectedToModel) {
        int actualToDb = 0;
        int actualToModel = 0;
        for (MergerToken token : tokens) {
            if (token.getDirection().isToDb()) {
                actualToDb++;
            } else if (token.getDirection().isToModel()) {
                actualToModel++;
            }
        }

        assertEquals("tokens to db", expectedToDb, actualToDb);
        assertEquals("tokens to model", expectedToModel, actualToModel);
    }

    protected void assertTokensAndExecute(int expectedToDb, int expectedToModel) {
        List<MergerToken> tokens = createMergeTokens();
        assertTokens(tokens, expectedToDb, expectedToModel);
        execute(tokens);
    }

    protected MergerTokenFactory mergerFactory() {
        return runtime.getInjector().getInstance(MergerTokenFactoryProvider.class).get(node.getAdapter());
    }

    protected void dropTableIfPresent(String tableName) throws Exception {

        // must have a dummy datamap for the dummy table for the downstream code
        // to work
        DataMap map = new DataMap("dummy");
        map.setQuotingSQLIdentifiers(map.isQuotingSQLIdentifiers());
        DbEntity entity = new DbEntity(tableName);
        map.addDbEntity(entity);

        AbstractToDbToken t = (AbstractToDbToken) mergerFactory().createDropTableToDb(entity);

        for (String sql : t.createSql(node.getAdapter())) {

            try {
                executeSql(sql);
            } catch (Exception e) {
                logger.info("Exception dropping table " + tableName + ", probably abscent..");
            }
        }
    }
}
