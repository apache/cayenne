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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DbLoader;
import org.apache.cayenne.access.DbLoaderConfiguration;
import org.apache.cayenne.access.DbLoaderDelegate;
import org.apache.cayenne.access.DefaultDbLoaderDelegate;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.map.naming.ObjectNameGenerator;
import org.apache.cayenne.merge.DefaultModelMergeDelegate;
import org.apache.cayenne.merge.ModelMergeDelegate;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.tools.NamePatternMatcher;
import org.apache.cayenne.util.EntityMergeSupport;
import org.apache.commons.logging.Log;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.util.Collections;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

/**
 * @since 3.2
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

    /**
     * DB schema to use for DB importing.
     */
    private DbLoaderConfiguration dbLoaderConfiguration = new DbLoaderConfiguration();

    /**
     * Pattern for tables to import from DB
     */
    private String tablePattern;

    /**
     * Indicates whether stored procedures should be imported.
     */
    private boolean importProcedures;

    /**
     * Pattern for stored procedures to import from DB. This is only meaningful
     * if <code>importProcedures</code> is set to <code>true</code>.
     */
    private String procedurePattern;

    private String meaningfulPkTables;

    /**
     * Java class implementing org.apache.cayenne.map.naming.NamingStrategy.
     * This is used to specify how ObjEntities will be mapped from the imported
     * DB schema.
     */
    private String namingStrategy;

    /**
     * Java class implementing org.apache.cayenne.dba.DbAdapter. This attribute
     * is optional, the default is AutoAdapter, i.e. Cayenne would try to guess
     * the DB type.
     */
    private String adapter;

    /**
     * A class of JDBC driver to use for the target database.
     */
    private String driver;

    /**
     * JDBC connection URL of a target database.
     */
    private String url;

    /**
     * Database user name.
     */
    private String username;

    /**
     * Database user password.
     */
    private String password;

    private String includeTables;
    private String excludeTables;

    private boolean usePrimitives;

    private Log logger;

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

    public String getCatalog() {
        return dbLoaderConfiguration.getCatalog();
    }

    public void setCatalog(String catalog) {
        this.dbLoaderConfiguration.setCatalog(catalog);
    }

    public void setSchema(String schema) {
        dbLoaderConfiguration.setSchema(schema);
    }

    public String getSchema() {
        return dbLoaderConfiguration.getSchema();
    }

    public String getTablePattern() {
        return tablePattern;
    }

    public void setTablePattern(String tablePattern) {
        this.tablePattern = tablePattern;
    }

    public boolean isImportProcedures() {
        return importProcedures;
    }

    public void setImportProcedures(boolean importProcedures) {
        this.importProcedures = importProcedures;
    }

    public String getProcedurePattern() {
        return procedurePattern;
    }

    public void setProcedurePattern(String procedurePattern) {
        this.procedurePattern = procedurePattern;
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

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getIncludeTables() {
        return includeTables;
    }

    public void setIncludeTables(String includeTables) {
        this.includeTables = includeTables;
    }

    public String getExcludeTables() {
        return excludeTables;
    }

    public void setExcludeTables(String excludeTables) {
        this.excludeTables = excludeTables;
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

        final NamePatternMatcher nameFilter = new NamePatternMatcher(logger, getIncludeTables(), getExcludeTables());
        final NamePatternMatcher meaningfulPkFilter = new NamePatternMatcher(logger, getMeaningfulPkTables(),
                getMeaningfulPkTables() != null ? null : "*");

        DbLoader loader = new DbLoader(connection, adapter, loaderDelegate) {
            @Override
            public boolean includeTableName(String tableName) {
                return nameFilter.isIncluded(tableName);
            }

            @Override
            protected EntityMergeSupport createEntityMerger(DataMap map) {
                EntityMergeSupport emSupport = new EntityMergeSupport(map, nameGenerator, true) {

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

    public DataSourceInfo createDataSourceInfo() {
        DataSourceInfo dataSourceInfo = new DataSourceInfo();
        dataSourceInfo.setDataSourceUrl(getUrl());
        dataSourceInfo.setJdbcDriver(getDriver());
        dataSourceInfo.setUserName(getUsername());
        dataSourceInfo.setPassword(getPassword());

        return dataSourceInfo;
    }

    public DataNodeDescriptor createDataNodeDescriptor() {
        DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor();
        nodeDescriptor.setAdapterType(getAdapter());
        nodeDescriptor.setDataSourceDescriptor(createDataSourceInfo());

        return nodeDescriptor;
    }

    public DataMap createDataMap() throws IOException {
        if (dataMapFile == null) {
            throw new NullPointerException("Null DataMap File.");
        }

        String name = dataMapFile.getName();
        if (!name.endsWith(DATA_MAP_LOCATION_SUFFIX)) {
            throw new CayenneRuntimeException("DataMap file name must end with '%s': '%s'", DATA_MAP_LOCATION_SUFFIX,
                    name);
        }

        String dataMapName = name.substring(0, name.length() - DATA_MAP_LOCATION_SUFFIX.length());
        DataMap dataMap = new DataMap(dataMapName);
        dataMap.setConfigurationSource(new URLResource(dataMapFile.toURI().toURL()));
        dataMap.setNamespace(new EntityResolver(Collections.singleton(dataMap)));

        // update map defaults

        // do not override default package of existing DataMap unless it is
        // explicitly requested by the plugin caller
        String defaultPackage = getDefaultPackage();
        if (isNotEmpty(defaultPackage)) {
            dataMap.setDefaultPackage(defaultPackage);
        }

        // do not override default catalog of existing DataMap unless it is
        // explicitly requested by the plugin caller, and the provided catalog is
        // not a pattern
        String catalog = getCatalog();
        if (isNotEmpty(catalog) && catalog.indexOf('%') < 0) {
            dataMap.setDefaultCatalog(catalog);
        }

        // do not override default schema of existing DataMap unless it is
        // explicitly requested by the plugin caller, and the provided schema is
        // not a pattern
        String schema = getSchema();
        if (isNotEmpty(schema) && schema.indexOf('%') < 0) {
            dataMap.setDefaultSchema(schema);
        }

        return dataMap;
    }

    public ModelMergeDelegate createMergeDelegate() {
        return new DefaultModelMergeDelegate();
    }

    public DbLoaderDelegate createLoaderDelegate() {
        return new DefaultDbLoaderDelegate();
    }

    public void log() {
        logger.debug("Importer options - map: " + getDataMapFile());
        logger.debug("Importer options - overwrite: " + isOverwrite());
        logger.debug("Importer options - adapter: " + getAdapter());
        logger.debug("Importer options - catalog: " + getCatalog());
        logger.debug("Importer options - schema: " + getSchema());
        logger.debug("Importer options - defaultPackage: " + getDefaultPackage());
        logger.debug("Importer options - tablePattern: " + getTablePattern());
        logger.debug("Importer options - importProcedures: " + isImportProcedures());
        logger.debug("Importer options - procedurePattern: " + getProcedurePattern());
        logger.debug("Importer options - meaningfulPkTables: " + getMeaningfulPkTables());
        logger.debug("Importer options - namingStrategy: " + getNamingStrategy());
        logger.debug("Importer options - includeTables: " + getIncludeTables());
        logger.debug("Importer options - excludeTables: " + getExcludeTables());
    }
}
