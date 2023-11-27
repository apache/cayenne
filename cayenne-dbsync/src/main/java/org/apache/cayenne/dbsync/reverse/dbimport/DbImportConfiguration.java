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
package org.apache.cayenne.dbsync.reverse.dbimport;

import java.io.File;
import java.util.regex.Pattern;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.DataSourceDescriptor;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.filter.NameFilter;
import org.apache.cayenne.dbsync.filter.NamePatternMatcher;
import org.apache.cayenne.dbsync.naming.DbEntityNameStemmer;
import org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator;
import org.apache.cayenne.dbsync.naming.NoStemStemmer;
import org.apache.cayenne.dbsync.naming.ObjectNameGenerator;
import org.apache.cayenne.dbsync.naming.PatternStemmer;
import org.apache.cayenne.dbsync.reverse.dbload.DbLoaderConfiguration;
import org.apache.cayenne.dbsync.reverse.dbload.DbLoaderDelegate;
import org.apache.cayenne.dbsync.reverse.dbload.DefaultDbLoaderDelegate;
import org.apache.cayenne.dbsync.reverse.dbload.DefaultModelMergeDelegate;
import org.apache.cayenne.dbsync.reverse.dbload.LoggingDbLoaderDelegate;
import org.apache.cayenne.dbsync.reverse.dbload.ModelMergeDelegate;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.slf4j.Logger;

/**
 * @since 4.0
 */
public class DbImportConfiguration {

    private static final String DATA_MAP_LOCATION_SUFFIX = ".map.xml";

    private final DataSourceDescriptor dataSourceInfo;
    private final DbLoaderConfiguration dbLoaderConfiguration;
    private File targetDataMap;
    private String defaultPackage;
    private String meaningfulPkTables;
    private String adapter;
    private boolean useJava7Types;
    private Logger logger;
    private String namingStrategy;
    private String stripFromTableNames;
    private boolean forceDataMapCatalog;
    private boolean forceDataMapSchema;
    private boolean useDataMapReverseEngineering;
    private File cayenneProject;

    public DbImportConfiguration() {
        this.dataSourceInfo = new DataSourceDescriptor();
        this.dbLoaderConfiguration = new DbLoaderConfiguration();
    }

    public String getStripFromTableNames() {
        return stripFromTableNames;
    }

    public void setStripFromTableNames(String stripFromTableNames) {
        this.stripFromTableNames = stripFromTableNames;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * Returns DataMap XML file representing the target of the DB import operation.
     */
    public File getTargetDataMap() {
        return targetDataMap;
    }

    public void setTargetDataMap(File map) {
        this.targetDataMap = map;
    }

    /**
     * Returns a default package for ObjEntity Java classes.
     */
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

    /**
     * Returns the name of a Java class implementing {@link DbAdapter}. This attribute is optional, the default is
     * {@link org.apache.cayenne.dba.AutoAdapter}, i.e. Cayenne will try to guess the DB type.
     */
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

    /**
     * @deprecated since 5.0
     * @return false
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public boolean isUsePrimitives() {
        return false;
    }

    /**
     * does nothing
     * @param usePrimitives not used
     * @deprecated since 5.0
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public void setUsePrimitives(boolean usePrimitives) {
    }

    public boolean isUseJava7Types() {
        return useJava7Types;
    }

    public void setUseJava7Types(boolean useJava7Types) {
        this.useJava7Types = useJava7Types;
    }

    public File getCayenneProject() {
        return cayenneProject;
    }

    public void setCayenneProject(File cayenneProject) {
        this.cayenneProject = cayenneProject;
    }

    public NameFilter createMeaningfulPKFilter() {

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

    public ObjectNameGenerator createNameGenerator() {

        // TODO: not a singleton; called from different places...

        // custom name generator
        // TODO: support stemmer in non-standard generators...
        // TODO: load via DI AdhocObjectFactory
        String namingStrategy = getNamingStrategy();
        if (namingStrategy != null && !namingStrategy.equals(DefaultObjectNameGenerator.class.getName())) {
            try {
                Class<?> generatorClass = Thread.currentThread().getContextClassLoader().loadClass(namingStrategy);
                return (ObjectNameGenerator) generatorClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new CayenneRuntimeException("Error creating name generator: " + namingStrategy, e);
            }
        }

        return new DefaultObjectNameGenerator(createStemmer());
    }

    protected DbEntityNameStemmer createStemmer() {
        return (stripFromTableNames == null || stripFromTableNames.length() == 0)
                ? NoStemStemmer.getInstance()
                : new PatternStemmer(stripFromTableNames, false);
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

    public String getDataMapName() {
        String name = targetDataMap.getName();
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

    /**
     * Returns configuration that should be used for DB import stage when the schema is loaded from the database.
     */
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

    public DataSourceDescriptor getDataSourceInfo() {
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

    public void setForceDataMapCatalog(boolean forceDataMapCatalog) {
        this.forceDataMapCatalog = forceDataMapCatalog;
    }

    public boolean isForceDataMapCatalog() {
        return forceDataMapCatalog;
    }

    public void setForceDataMapSchema(boolean forceDataMapSchema) {
        this.forceDataMapSchema = forceDataMapSchema;
    }

    public boolean isForceDataMapSchema() {
        return forceDataMapSchema;
    }

    public boolean isUseDataMapReverseEngineering() {
        return useDataMapReverseEngineering;
    }

    public void setUseDataMapReverseEngineering(boolean useDataMapReverseEngineering) {
        this.useDataMapReverseEngineering = useDataMapReverseEngineering;
    }
}
