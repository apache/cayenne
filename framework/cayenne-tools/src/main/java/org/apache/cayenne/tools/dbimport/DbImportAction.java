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
package org.apache.cayenne.tools.dbimport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;

import javax.sql.DataSource;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DbLoader;
import org.apache.cayenne.access.DbLoaderDelegate;
import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.server.DataSourceFactory;
import org.apache.cayenne.configuration.server.DbAdapterFactory;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.naming.NamingStrategy;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectSaver;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.tools.NamePatternMatcher;
import org.apache.cayenne.util.DeleteRuleUpdater;
import org.apache.cayenne.util.EntityMergeSupport;
import org.apache.commons.logging.Log;
import org.xml.sax.InputSource;

/**
 * A thin wrapper around {@link DbLoader} that encapsulates DB import logic for
 * the benefit of Ant and Maven db importers.
 * 
 * @since 3.2
 */
public class DbImportAction {

    private static final String DATA_MAP_LOCATION_SUFFIX = ".map.xml";

    private ProjectSaver projectSaver;
    private DataSourceFactory dataSourceFactory;
    private DbAdapterFactory adapterFactory;
    private Log logger;

    public DbImportAction(@Inject Log logger, @Inject DbAdapterFactory adapterFactory,
            @Inject DataSourceFactory dataSourceFactory, @Inject ProjectSaver projectSaver) {
        this.logger = logger;
        this.adapterFactory = adapterFactory;
        this.dataSourceFactory = dataSourceFactory;
        this.projectSaver = projectSaver;
    }

    public void execute(DbImportParameters parameters) throws Exception {

        if (logger.isInfoEnabled()) {
            logger.debug(String.format("DB connection - [driver: %s, url: %s, username: %s, password: %s]",
                    parameters.getDriver(), parameters.getUrl(), parameters.getUsername(), "XXXXX"));
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Importer options - map: " + parameters.getDataMapFile());
            logger.debug("Importer options - overwrite: " + parameters.isOverwrite());
            logger.debug("Importer options - adapter: " + parameters.getAdapter());
            logger.debug("Importer options - catalog: " + parameters.getCatalog());
            logger.debug("Importer options - schema: " + parameters.getSchema());
            logger.debug("Importer options - defaultPackage: " + parameters.getDefaultPackage());
            logger.debug("Importer options - tablePattern: " + parameters.getTablePattern());
            logger.debug("Importer options - importProcedures: " + parameters.isImportProcedures());
            logger.debug("Importer options - procedurePattern: " + parameters.getProcedurePattern());
            logger.debug("Importer options - meaningfulPkTables: " + parameters.getMeaningfulPkTables());
            logger.debug("Importer options - namingStrategy: " + parameters.getNamingStrategy());
            logger.debug("Importer options - includeTables: " + parameters.getIncludeTables());
            logger.debug("Importer options - excludeTables: " + parameters.getExcludeTables());
        }

        DataSourceInfo dataSourceInfo = new DataSourceInfo();
        dataSourceInfo.setDataSourceUrl(parameters.getUrl());
        dataSourceInfo.setJdbcDriver(parameters.getDriver());
        dataSourceInfo.setUserName(parameters.getUsername());
        dataSourceInfo.setPassword(parameters.getPassword());

        DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor();
        nodeDescriptor.setAdapterType(parameters.getAdapter());
        nodeDescriptor.setDataSourceDescriptor(dataSourceInfo);

        DataMap dataMap = load(parameters, nodeDescriptor);

        saveLoaded(dataMap, parameters.getDataMapFile());
    }

    void saveLoaded(DataMap dataMap, File dataMapFile) throws FileNotFoundException {

        ConfigurationTree<DataMap> projectRoot = new ConfigurationTree<DataMap>(dataMap);
        Project project = new Project(projectRoot);
        projectSaver.save(project);
    }

    DataMap load(DbImportParameters parameters, DataNodeDescriptor nodeDescriptor) throws Exception {
        DataSource dataSource = dataSourceFactory.getDataSource(nodeDescriptor);
        DbAdapter adapter = adapterFactory.createAdapter(nodeDescriptor, dataSource);

        DataMap dataMap = createDataMap(parameters);

        DbImportDbLoaderDelegate loaderDelegate = new DbImportDbLoaderDelegate();

        Connection connection = dataSource.getConnection();

        try {
            DbLoader loader = createLoader(parameters, adapter, connection, loaderDelegate);

            String[] types = loader.getDefaultTableTypes();
            loader.load(dataMap, parameters.getCatalog(), parameters.getSchema(), parameters.getTablePattern(), types);

            for (ObjEntity addedObjEntity : loaderDelegate.getAddedObjEntities()) {
                DeleteRuleUpdater.updateObjEntity(addedObjEntity);
            }

            if (parameters.isImportProcedures()) {
                loader.loadProcedures(dataMap, parameters.getCatalog(), parameters.getSchema(),
                        parameters.getProcedurePattern());
            }
        } finally {
            connection.close();
        }

        return dataMap;
    }

    DbLoader createLoader(final DbImportParameters parameters, DbAdapter adapter, Connection connection,
            DbLoaderDelegate loaderDelegate) throws InstantiationException, IllegalAccessException,
            ClassNotFoundException {

        final NamePatternMatcher nameFilter = new NamePatternMatcher(logger, parameters.getIncludeTables(),
                parameters.getExcludeTables());

        String meangfulPkExclude = parameters.getMeaningfulPkTables() != null ? null : "*";
        final NamePatternMatcher meaningfulPkFilter = new NamePatternMatcher(logger,
                parameters.getMeaningfulPkTables(), meangfulPkExclude);

        DbLoader loader = new DbLoader(connection, adapter, loaderDelegate) {
            @Override
            public boolean includeTableName(String tableName) {
                return nameFilter.isIncluded(tableName);
            }

            @Override
            protected EntityMergeSupport createEntityMerger(DataMap map) {
                EntityMergeSupport emSupport = new EntityMergeSupport(map, namingStrategy, true) {

                    @Override
                    protected boolean removePK(DbEntity dbEntity) {
                        return !meaningfulPkFilter.isIncluded(dbEntity.getName());
                    }
                };

                emSupport.setUsePrimitives(parameters.isUsePrimitives());
                return emSupport;
            }
        };

        // TODO: load via DI AdhocObjectFactory
        String namingStrategy = parameters.getNamingStrategy();
        if (namingStrategy != null) {
            NamingStrategy namingStrategyInst = (NamingStrategy) Class.forName(namingStrategy).newInstance();
            loader.setNamingStrategy(namingStrategyInst);
        }

        return loader;
    }

    DataMap createDataMap(DbImportParameters parameters) throws IOException {

        File dataMapFile = parameters.getDataMapFile();
        if (dataMapFile == null) {
            throw new NullPointerException("Null DataMap File.");
        }

        String name = dataMapFile.getName();
        if (!name.endsWith(DATA_MAP_LOCATION_SUFFIX)) {
            throw new CayenneRuntimeException("DataMap file name must end with '%s': '%s'", DATA_MAP_LOCATION_SUFFIX,
                    name);
        }

        DataMap dataMap;

        if (dataMapFile.exists()) {
            InputSource in = new InputSource(dataMapFile.getCanonicalPath());
            dataMap = new MapLoader().loadDataMap(in);

            if (parameters.isOverwrite()) {
                dataMap.clearObjEntities();
                dataMap.clearEmbeddables();
                dataMap.clearProcedures();
                dataMap.clearDbEntities();
                dataMap.clearQueries();
                dataMap.clearResultSets();
            }
        } else {
            String dataMapName = name.substring(0, name.length() - DATA_MAP_LOCATION_SUFFIX.length());
            dataMap = new DataMap(dataMapName);
        }

        URL dataMapUrl = dataMapFile.toURI().toURL();
        dataMap.setConfigurationSource(new URLResource(dataMapUrl));

        // update map defaults

        // do not override default package of existing DataMap unless it is
        // explicitly requested by the plugin caller
        String defaultPackage = parameters.getDefaultPackage();
        if (defaultPackage != null && defaultPackage.length() > 0) {
            dataMap.setDefaultPackage(defaultPackage);
        }

        // do not override default catalog of existing DataMap unless it is
        // explicitly requested by the plugin caller, and the provided catalog is
        // not a pattern
        String catalog = parameters.getCatalog();
        if (catalog != null && catalog.length() > 0 && catalog.indexOf('%') < 0) {
            dataMap.setDefaultCatalog(catalog);
        }
        
        // do not override default schema of existing DataMap unless it is
        // explicitly requested by the plugin caller, and the provided schema is
        // not a pattern
        String schema = parameters.getSchema();
        if (schema != null && schema.length() > 0 && schema.indexOf('%') < 0) {
            dataMap.setDefaultSchema(schema);
        }

        return dataMap;
    }
}
