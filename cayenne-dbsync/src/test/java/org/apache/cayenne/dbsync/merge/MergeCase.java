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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.dbsync.DbSyncModule;
import org.apache.cayenne.dbsync.merge.context.MergerContext;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactoryProvider;
import org.apache.cayenne.dbsync.merge.token.db.AbstractToDbToken;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.dbsync.merge.token.db.SetColumnTypeToDb;
import org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator;
import org.apache.cayenne.dbsync.naming.NoStemStemmer;
import org.apache.cayenne.dbsync.reverse.dbload.DbLoader;
import org.apache.cayenne.dbsync.reverse.dbload.DbLoaderConfiguration;
import org.apache.cayenne.dbsync.reverse.dbload.LoggingDbLoaderDelegate;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.apache.cayenne.dbsync.reverse.filters.PatternFilter;
import org.apache.cayenne.dbsync.reverse.filters.TableFilter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.ExtraModules;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.RuntimeCaseDataSourceFactory;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.slf4j.Logger;
import org.junit.Before;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
@ExtraModules(DbSyncModule.class)
public abstract class MergeCase extends RuntimeCase {

    @Inject
    protected EntityResolver resolver;
    @Inject
    protected DataNode node;
    protected DataMap map;
    private final Logger logger = LoggerFactory.getLogger(MergeCase.class);
    @Inject
    private DBHelper dbHelper;
    @Inject
    private CayenneRuntime runtime;
    @Inject
    protected UnitDbAdapter accessStackAdapter;
    @Inject
    private RuntimeCaseDataSourceFactory dataSourceFactory;

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

    protected DataMapMerger.Builder merger() {
        return DataMapMerger.builder(mergerFactory());
    }

    protected List<MergerToken> createMergeTokens() {
        return createMergeTokens("ARTIST|GALLERY|PAINTING|NEW_TABLE2?");
    }

    protected List<MergerToken> createMergeTokens(String tableFilterInclude) {

        FiltersConfig filters = FiltersConfig.create(null, null,
                TableFilter.include(tableFilterInclude), PatternFilter.INCLUDE_NOTHING);

        DbLoaderConfiguration loaderConfiguration = new DbLoaderConfiguration();
        loaderConfiguration.setFiltersConfig(filters);

        DataMap dbImport;
        try (Connection conn = node.getDataSource().getConnection();) {
            dbImport = new DbLoader(node.getAdapter(), conn,
                    loaderConfiguration,
                    new LoggingDbLoaderDelegate(LoggerFactory.getLogger(DbLoader.class)),
                    new DefaultObjectNameGenerator(NoStemStemmer.getInstance()))
                    .load();

        } catch (SQLException e) {
            throw new CayenneRuntimeException("Can't doLoad dataMap from db.", e);
        }

        List<MergerToken> tokens = merger().filters(filters).build().createMergeTokens(map, dbImport);
        return filter(tokens);
    }

    private List<MergerToken> filter(List<MergerToken> tokens) {
        return filterEmptyTypeChange(filterEmpty(tokens));
    }

    /**
     * Filter out tokens for db attribute type change when types is same for specific DB
     */
    private List<MergerToken> filterEmptyTypeChange(List<MergerToken> tokens) {
        List<MergerToken> tokensOut = new ArrayList<>();
        for(MergerToken token : tokens) {
            if(!(token instanceof SetColumnTypeToDb)) {
                tokensOut.add(token);
                continue;
            }
            SetColumnTypeToDb setColumnToDb = (SetColumnTypeToDb)token;
            int toType = setColumnToDb.getColumnNew().getType();
            int fromType = setColumnToDb.getColumnOriginal().getType();
            // filter out conversions between date/time types
            if(accessStackAdapter.onlyGenericDateType()) {
                if(isDateTimeType(toType) && isDateTimeType(fromType)){
                    continue;
                }
            }
            // filter out conversions between numeric types
            if(accessStackAdapter.onlyGenericNumberType()) {
                if(TypesMapping.isNumeric(toType) && TypesMapping.isNumeric(fromType)) {
                    continue;
                }
            }
            tokensOut.add(token);
        }

        return tokensOut;
    }

    private static boolean isDateTimeType(int type) {
        return type == Types.DATE || type == Types.TIME || type == Types.TIMESTAMP;
    }

    private List<MergerToken> filterEmpty(List<MergerToken> tokens) {
        List<MergerToken> tokensOut = new ArrayList<>();
        for(MergerToken token : tokens) {
            if(!token.isEmpty()) {
                tokensOut.add(token);
            }
        }
        return tokensOut;
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
