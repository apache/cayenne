/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.tools.dbimport;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DbLoader;
import org.apache.cayenne.access.loader.DbLoaderConfiguration;
import org.apache.cayenne.access.DbLoaderDelegate;
import org.apache.cayenne.access.loader.DefaultDbLoaderDelegate;
import org.apache.cayenne.access.loader.LoggingDbLoaderDelegate;
import org.apache.cayenne.access.loader.NameFilter;
import org.apache.cayenne.access.loader.filters.DbPath;
import org.apache.cayenne.access.loader.filters.FiltersConfig;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.naming.ObjectNameGenerator;
import org.apache.cayenne.merge.DefaultModelMergeDelegate;
import org.apache.cayenne.merge.ModelMergeDelegate;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.access.loader.NamePatternMatcher;
import org.apache.cayenne.util.EntityMergeSupport;
import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

/**
 * @since 4.0
 */
public class DbImportConfiguration {

    private static final String DATA_MAP_LOCATION_SUFFIX = ".map.xml";

    /**
     * DataMap XML file to use as a base for DB importing.
     */
    private File dataMapFile;

    /**
     * A default package for ObjEntity Java classes.
     */
    private String defaultPackage;

    /**
     * Indicates that the old mapping should be completely removed and replaced
     * with the new data based on reverse engineering.
     */
    private boolean overwrite;

    private String meaningfulPkTables;

    /**
     * Java class implementing org.apache.cayenne.dba.DbAdapter. This attribute
     * is optional, the default is AutoAdapter, i.e. Cayenne would try to guess
     * the DB type.
     */
    private String adapter;

    private boolean usePrimitives;

    private Log logger;

    private final DataSourceInfo dataSourceInfo = new DataSourceInfo();

    /**
     * DB schema to use for DB importing.
     */
    private final DbLoaderConfiguration dbLoaderConfiguration = new DbLoaderConfiguration();

    public Log getLogger() {
        return logger;
    }

    public void setLogger(Log logger) {
        this.logger = logger;
    }

    public File getDataMapFile() {
        return dataMapFile;
    }

    public void setDataMapFile(File map) {
        this.dataMapFile = map;
    }

    public String getDefaultPackage() {
        return defaultPackage;
    }

    public void setDefaultPackage(String defaultPackage) {
        this.defaultPackage = defaultPackage;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public String getNamingStrategy() {
        return dbLoaderConfiguration.getNamingStrategy();
    }

    public void setNamingStrategy(String namingStrategy) {
        dbLoaderConfiguration.setNamingStrategy(namingStrategy);
    }

    public String getAdapter() {
        return adapter;
    }

    public void setAdapter(String adapter) {
        this.adapter = adapter;
    }

    /**
     * Returns a comma-separated list of Perl5 regular expressions that match
     * table names for which {@link DbImportAction} should include ObjAttribute
     * for PK.
     */
    public String getMeaningfulPkTables() {
        return meaningfulPkTables;
    }

    public void setMeaningfulPkTables(String meaningfulPkTables) {
        this.meaningfulPkTables = meaningfulPkTables;
    }

    public boolean isUsePrimitives() {
        return usePrimitives;
    }

    public void setUsePrimitives(boolean usePrimitives) {
        this.usePrimitives = usePrimitives;
    }

    public DbLoader createLoader(DbAdapter adapter, Connection connection, DbLoaderDelegate loaderDelegate)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {

        final NameFilter meaningfulPkFilter = NamePatternMatcher.build(logger, getMeaningfulPkTables(),
                getMeaningfulPkTables() != null ? null : "*");

        DbLoader loader = new DbLoader(connection, adapter, loaderDelegate) {

            @Override
            protected EntityMergeSupport createEntityMerger(DataMap map) {
                EntityMergeSupport emSupport = new EntityMergeSupport(map, getNameGenerator(), true) {

                    @Override
                    protected boolean removePK(DbEntity dbEntity) {
                        return !meaningfulPkFilter.isIncluded(dbEntity.getName());
                    }
                };

                emSupport.setUsePrimitives(DbImportConfiguration.this.isUsePrimitives());
                return emSupport;
            }
        };

        // TODO: load via DI AdhocObjectFactory
        String namingStrategy = getNamingStrategy();
        if (namingStrategy != null) {
            ObjectNameGenerator nameGeneratorInst = (ObjectNameGenerator) Class.forName(namingStrategy).newInstance();
            loader.setNameGenerator(nameGeneratorInst);
        }

        return loader;
    }

    public void setDriver(String jdbcDriver) {
        dataSourceInfo.setJdbcDriver(jdbcDriver);
    }

    public String getDriver() {
        return dataSourceInfo.getJdbcDriver();
    }

    public void setPassword(String password) {
        dataSourceInfo.setPassword(password);
    }

    public String getPassword() {
        return dataSourceInfo.getPassword();
    }

    public void setUsername(String userName) {
        dataSourceInfo.setUserName(userName);
    }

    public String getUsername() {
        return dataSourceInfo.getUserName();
    }

    public void setUrl(String dataSourceUrl) {
        dataSourceInfo.setDataSourceUrl(dataSourceUrl);
    }

    public String getUrl() {
        return dataSourceInfo.getDataSourceUrl();
    }

    public DataNodeDescriptor createDataNodeDescriptor() {
        DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor();
        nodeDescriptor.setAdapterType(getAdapter());
        nodeDescriptor.setDataSourceDescriptor(dataSourceInfo);

        return nodeDescriptor;
    }

    public DataMap createDataMap() throws IOException {
        if (dataMapFile == null) {
            throw new NullPointerException("Null DataMap File.");
        }

        return initializeDataMap(new DataMap());
    }

    public DataMap initializeDataMap(DataMap dataMap) throws MalformedURLException {
        dataMap.setName(getDataMapName());
        dataMap.setConfigurationSource(new URLResource(dataMapFile.toURI().toURL()));
        dataMap.setNamespace(new EntityResolver(Collections.singleton(dataMap)));

        // update map defaults

        // do not override default package of existing DataMap unless it is
        // explicitly requested by the plugin caller
        String defaultPackage = getDefaultPackage();
        if (isNotEmpty(defaultPackage)) {
            dataMap.setDefaultPackage(defaultPackage);
        }

        List<DbPath> dbPaths = dbLoaderConfiguration.getFiltersConfig().getDbPaths();
        if (!dbPaths.isEmpty()) {
            // do not override default catalog of existing DataMap unless it is
            // explicitly requested by the plugin caller, and the provided catalog is
            // not a pattern
            String catalog = dbPaths.get(0).catalog;
            if (isNotEmpty(catalog) && catalog.indexOf('%') < 0) {
                dataMap.setDefaultCatalog(catalog);
            }

            // do not override default schema of existing DataMap unless it is
            // explicitly requested by the plugin caller, and the provided schema is
            // not a pattern
            String schema = dbPaths.get(0).schema;
            if (isNotEmpty(schema) && schema.indexOf('%') < 0) {
                dataMap.setDefaultSchema(schema);
            }
        }

        return dataMap;
    }

    public String getDataMapName() {
        String name = dataMapFile.getName();
        if (!name.endsWith(DATA_MAP_LOCATION_SUFFIX)) {
            throw new CayenneRuntimeException("DataMap file name must end with '%s': '%s'", DATA_MAP_LOCATION_SUFFIX,
                    name);
        }
        return name.substring(0, name.length() - DATA_MAP_LOCATION_SUFFIX.length());
    }

    public ModelMergeDelegate createMergeDelegate() {
        return new DefaultModelMergeDelegate();
    }

    public DbLoaderDelegate createLoaderDelegate() {
        if (getLogger() != null) {
            return new LoggingDbLoaderDelegate(getLogger());
        } else {
            return new DefaultDbLoaderDelegate();
        }
    }

    public DbLoaderConfiguration getDbLoaderConfig() {
        return dbLoaderConfiguration;
    }

    public void setFiltersConfig(FiltersConfig filtersConfig) {
        dbLoaderConfiguration.setFiltersConfig(filtersConfig);
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("Importer options:");
        for (String line : dbLoaderConfiguration.toString().split("\n")) {
            res.append("    ").append(line).append("\n");
        }

        return res.toString();
    }

    public DataSourceInfo getDataSourceInfo() {
        return dataSourceInfo;
    }

    public void setSkipRelationshipsLoading(Boolean skipRelationshipsLoading) {
        this.dbLoaderConfiguration.setSkipRelationshipsLoading(skipRelationshipsLoading);
    }
}
