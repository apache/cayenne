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
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.filter.NameFilter;
import org.apache.cayenne.dbsync.filter.NamePatternMatcher;
import org.apache.cayenne.dbsync.merge.DefaultModelMergeDelegate;
import org.apache.cayenne.dbsync.merge.ModelMergeDelegate;
import org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator;
import org.apache.cayenne.dbsync.naming.ObjectNameGenerator;
import org.apache.cayenne.dbsync.reverse.db.DbLoader;
import org.apache.cayenne.dbsync.reverse.db.DbLoaderConfiguration;
import org.apache.cayenne.dbsync.reverse.db.DbLoaderDelegate;
import org.apache.cayenne.dbsync.reverse.db.DefaultDbLoaderDelegate;
import org.apache.cayenne.dbsync.reverse.db.LoggingDbLoaderDelegate;
import org.apache.cayenne.dbsync.reverse.filters.CatalogFilter;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.resource.URLResource;
import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.util.Collections;
import java.util.regex.Pattern;

/**
 * @since 4.0
 */
public class DbImportConfiguration {

    private static final String DATA_MAP_LOCATION_SUFFIX = ".map.xml";
    private final DataSourceInfo dataSourceInfo = new DataSourceInfo();
    /**
     * DB schema to use for DB importing.
     */
    private final DbLoaderConfiguration dbLoaderConfiguration = new DbLoaderConfiguration();
    /**
     * DataMap XML file to use as a base for DB importing.
     */
    private File dataMapFile;
    /**
     * A default package for ObjEntity Java classes.
     */
    private String defaultPackage;
    private String meaningfulPkTables;
    /**
     * Java class implementing org.apache.cayenne.dba.DbAdapter. This attribute
     * is optional, the default is AutoAdapter, i.e. Cayenne would try to guess
     * the DB type.
     */
    private String adapter;
    private boolean usePrimitives;
    private Log logger;
    private String namingStrategy;

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

    public String getNamingStrategy() {
        return namingStrategy;
    }

    public void setNamingStrategy(String namingStrategy) {
        this.namingStrategy = namingStrategy;
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
        return new DbLoader(connection, adapter, loaderDelegate, getNameGenerator());
    }

    public NameFilter getMeaningfulPKFilter() {

        if (meaningfulPkTables == null) {
            return NamePatternMatcher.EXCLUDE_ALL;
        }

        // TODO: this filter can't handle table names with comma in them
        String[] patternStrings = meaningfulPkTables.split(",");
        Pattern[] patterns = new Pattern[patternStrings.length];
        for (int i = 0; i < patterns.length; i++) {
            patterns[i] = Pattern.compile(patternStrings[i]);
        }

        return new NamePatternMatcher(patterns, new Pattern[0]);
    }

    public ObjectNameGenerator getNameGenerator() {

        // TODO: load via DI AdhocObjectFactory

        // TODO: not a singleton; called from different places...

        String namingStrategy = getNamingStrategy();
        if (namingStrategy != null) {
            try {
                return (ObjectNameGenerator) Class.forName(namingStrategy).newInstance();
            } catch (Exception e) {
                throw new CayenneRuntimeException("Error creating name generator: " + namingStrategy, e);
            }
        }

        return new DefaultObjectNameGenerator();
    }

    public String getDriver() {
        return dataSourceInfo.getJdbcDriver();
    }

    public void setDriver(String jdbcDriver) {
        dataSourceInfo.setJdbcDriver(jdbcDriver);
    }

    public String getPassword() {
        return dataSourceInfo.getPassword();
    }

    public void setPassword(String password) {
        dataSourceInfo.setPassword(password);
    }

    public String getUsername() {
        return dataSourceInfo.getUserName();
    }

    public void setUsername(String userName) {
        dataSourceInfo.setUserName(userName);
    }

    public String getUrl() {
        return dataSourceInfo.getDataSourceUrl();
    }

    public void setUrl(String dataSourceUrl) {
        dataSourceInfo.setDataSourceUrl(dataSourceUrl);
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

        DataMap dataMap = new DataMap();
        initializeDataMap(dataMap);
        return dataMap;
    }

    protected void initializeDataMap(DataMap dataMap) throws MalformedURLException {
        dataMap.setName(getDataMapName());
        dataMap.setConfigurationSource(new URLResource(dataMapFile.toURI().toURL()));
        dataMap.setNamespace(new EntityResolver(Collections.singleton(dataMap)));

        // update map defaults

        // do not override default package of existing DataMap unless it is
        // explicitly requested by the plugin caller
        String defaultPackage = getDefaultPackage();
        if (defaultPackage != null && defaultPackage.length() > 0) {
            dataMap.setDefaultPackage(defaultPackage);
        }

        CatalogFilter[] catalogs = dbLoaderConfiguration.getFiltersConfig().getCatalogs();
        if (catalogs.length > 0) {
            // do not override default catalog of existing DataMap unless it is
            // explicitly requested by the plugin caller, and the provided catalog is
            // not a pattern
            String catalog = catalogs[0].name;
            if (catalog != null && catalog.length() > 0 && catalog.indexOf('%') < 0) {
                dataMap.setDefaultCatalog(catalog);
            }

            // do not override default schema of existing DataMap unless it is
            // explicitly requested by the plugin caller, and the provided schema is
            // not a pattern
            String schema = catalogs[0].schemas[0].name;
            if (schema != null && schema.length() > 0 && schema.indexOf('%') < 0) {
                dataMap.setDefaultSchema(schema);
            }
        }
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

    public void setSkipPrimaryKeyLoading(Boolean skipPrimaryKeyLoading) {
        this.dbLoaderConfiguration.setSkipPrimaryKeyLoading(skipPrimaryKeyLoading);
    }

    public void setTableTypes(String[] tableTypes) {
        dbLoaderConfiguration.setTableTypes(tableTypes);
    }
}
