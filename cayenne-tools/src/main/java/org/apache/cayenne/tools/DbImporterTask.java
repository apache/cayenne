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
package org.apache.cayenne.tools;

import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.XMLDataMapLoader;
import org.apache.cayenne.configuration.server.DataSourceFactory;
import org.apache.cayenne.configuration.server.DbAdapterFactory;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbimport.Catalog;
import org.apache.cayenne.dbimport.DefaultReverseEngineeringLoader;
import org.apache.cayenne.dbimport.ExcludeColumn;
import org.apache.cayenne.dbimport.ExcludeProcedure;
import org.apache.cayenne.dbimport.ExcludeTable;
import org.apache.cayenne.dbimport.IncludeColumn;
import org.apache.cayenne.dbimport.IncludeProcedure;
import org.apache.cayenne.dbimport.IncludeTable;
import org.apache.cayenne.dbimport.ReverseEngineering;
import org.apache.cayenne.dbimport.Schema;
import org.apache.cayenne.dbsync.CayenneDbSyncModule;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfigBuilder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.tools.configuration.ToolsModule;
import org.apache.cayenne.tools.dbimport.DbImportAction;
import org.apache.cayenne.tools.dbimport.DbImportConfiguration;
import org.apache.cayenne.tools.dbimport.DbImportModule;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import javax.sql.DataSource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class DbImporterTask extends Task {

    private final DbImportConfiguration config;
    private ReverseEngineering reverseEngineering;
    private boolean isReverseEngineeringDefined;

    public DbImporterTask() {
        this.config = new DbImportConfiguration();
        this.config.setUsePrimitives(true);
        this.config.setNamingStrategy(DefaultObjectNameGenerator.class.getName());

        // reverse engineering config is flattened into task...
        this.reverseEngineering = new ReverseEngineering();
    }

    public void addIncludeColumn(IncludeColumn includeColumn) {
        isReverseEngineeringDefined = true;
        reverseEngineering.addIncludeColumn(includeColumn);
    }

    public void addExcludeColumn(ExcludeColumn excludeColumn) {
        isReverseEngineeringDefined = true;
        reverseEngineering.addExcludeColumn(excludeColumn);
    }

    public void addIncludeTable(IncludeTable includeTable) {
        isReverseEngineeringDefined = true;
        reverseEngineering.addIncludeTable(includeTable);
    }

    public void addExcludeTable(ExcludeTable excludeTable) {
        isReverseEngineeringDefined = true;
        reverseEngineering.addExcludeTable(excludeTable);
    }

    public void addIncludeProcedure(IncludeProcedure includeProcedure) {
        isReverseEngineeringDefined = true;
        reverseEngineering.addIncludeProcedure(includeProcedure);
    }

    public void addExcludeProcedure(ExcludeProcedure excludeProcedure) {
        isReverseEngineeringDefined = true;
        reverseEngineering.addExcludeProcedure(excludeProcedure);
    }

    public void setSkipRelationshipsLoading(boolean skipRelationshipsLoading) {
        isReverseEngineeringDefined = true;
        reverseEngineering.setSkipRelationshipsLoading(skipRelationshipsLoading);
    }

    public void setSkipPrimaryKeyLoading(boolean skipPrimaryKeyLoading) {
        isReverseEngineeringDefined = true;
        reverseEngineering.setSkipPrimaryKeyLoading(skipPrimaryKeyLoading);
    }

    public void addConfiguredTableType(AntTableType type) {
        isReverseEngineeringDefined = true;
        reverseEngineering.addTableType(type.getName());
    }

    public void addConfiguredSchema(Schema schema) {
        isReverseEngineeringDefined = true;
        reverseEngineering.addSchema(schema);
    }

    public void addCatalog(Catalog catalog) {
        isReverseEngineeringDefined = true;
        reverseEngineering.addCatalog(catalog);
    }

    @Override
    public void execute() {

        File dataMapFile = config.getDataMapFile();
        config.setFiltersConfig(new FiltersConfigBuilder(reverseEngineering).build());

        validateAttributes();

        Log logger = new AntLogger(this);
        config.setLogger(logger);
        config.setSkipRelationshipsLoading(reverseEngineering.getSkipRelationshipsLoading());
        config.setSkipPrimaryKeyLoading(reverseEngineering.getSkipPrimaryKeyLoading());
        config.setTableTypes(reverseEngineering.getTableTypes());

        // TODO: get rid of this fork...
        if (isReverseEngineeringDefined) {
            Injector injector = DIBootstrap.createInjector(new CayenneDbSyncModule(), new ToolsModule(logger), new DbImportModule());

            validateDbImportConfiguration(config, injector);

            try {
                injector.getInstance(DbImportAction.class).execute(config);
            } catch (Exception ex) {
                Throwable th = Util.unwindException(ex);

                String message = "Error importing database schema";

                if (th.getLocalizedMessage() != null) {
                    message += ": " + th.getLocalizedMessage();
                }

                log(message, Project.MSG_ERR);
                throw new BuildException(message, th);
            } finally {
                injector.shutdown();
            }
        } else {
            if (dataMapFile.exists()) {
                try {
                    URL url = dataMapFile.toURI().toURL();
                    URLResource resource = new URLResource(url);

                    XMLDataMapLoader xmlDataMapLoader = new XMLDataMapLoader();
                    DataMap dataMap = xmlDataMapLoader.load(resource);
                    if (dataMap.getReverseEngineering() != null) {
                        Injector injector = DIBootstrap.createInjector(new CayenneDbSyncModule(), new ToolsModule(logger), new DbImportModule());
                        try {
                            ConfigurationNameMapper nameMapper = injector.getInstance(ConfigurationNameMapper.class);
                            String reverseEngineeringLocation = nameMapper.configurationLocation(ReverseEngineering.class, dataMap.getReverseEngineering().getName());
                            Resource reverseEngineeringResource = new URLResource(dataMapFile.toURI().toURL()).getRelativeResource(reverseEngineeringLocation);

                            DefaultReverseEngineeringLoader reverseEngineeringLoader = new DefaultReverseEngineeringLoader();
                            ReverseEngineering reverseEngineering = reverseEngineeringLoader.load(reverseEngineeringResource.getURL().openStream());
                            reverseEngineering.setName(dataMap.getReverseEngineering().getName());
                            reverseEngineering.setConfigurationSource(reverseEngineeringResource);
                            dataMap.setReverseEngineering(reverseEngineering);

                            FiltersConfigBuilder filtersConfigBuilder = new FiltersConfigBuilder(dataMap.getReverseEngineering());
                            config.getDbLoaderConfig().setFiltersConfig(filtersConfigBuilder.build());
                            validateDbImportConfiguration(config, injector);
                            injector.getInstance(DbImportAction.class).execute(config);
                        } catch (Exception ex) {
                            Throwable th = Util.unwindException(ex);

                            String message = "Error importing database schema";

                            if (th.getLocalizedMessage() != null) {
                                message += ": " + th.getLocalizedMessage();
                            }

                            log(message, Project.MSG_ERR);
                            throw new BuildException(message, th);
                        } finally {
                            injector.shutdown();
                        }
                    }
                } catch (MalformedURLException e) {
                    log(e.getMessage(), Project.MSG_ERR);
                    throw new BuildException(e.getMessage(), e);
                }
            }
        }
    }

    private void validateDbImportConfiguration(DbImportConfiguration config, Injector injector) throws BuildException {
        DataNodeDescriptor dataNodeDescriptor = config.createDataNodeDescriptor();
        DataSource dataSource = null;
        DbAdapter adapter = null;

        try {
            dataSource = injector.getInstance(DataSourceFactory.class).getDataSource(dataNodeDescriptor);
            adapter = injector.getInstance(DbAdapterFactory.class).createAdapter(dataNodeDescriptor, dataSource);

            if (!adapter.supportsCatalogsOnReverseEngineering() &&
                    reverseEngineering.getCatalogs() != null && !reverseEngineering.getCatalogs().isEmpty()) {
                String message = "Your database does not support catalogs on reverse engineering. " +
                        "It allows to connect to only one at the moment. Please don't note catalogs as param.";
                throw new BuildException(message);
            }
        } catch (Exception e) {
            throw new BuildException("Error creating DataSource ("
                    + dataSource + ") or DbAdapter (" + adapter + ") for DataNodeDescriptor (" + dataNodeDescriptor + ")", e);
        }
    }

    /**
     * Validates attributes that are not related to internal
     * DefaultClassGenerator. Throws BuildException if attributes are invalid.
     */
    protected void validateAttributes() throws BuildException {
        StringBuilder error = new StringBuilder("");

        if (config.getDataMapFile() == null) {
            error.append("The 'map' attribute must be set.\n");
        }

        DataSourceInfo dataSourceInfo = config.getDataSourceInfo();
        if (dataSourceInfo.getJdbcDriver() == null) {
            error.append("The 'driver' attribute must be set.\n");
        }

        if (dataSourceInfo.getDataSourceUrl() == null) {
            error.append("The 'url' attribute must be set.\n");
        }

        if (error.length() > 0) {
            throw new BuildException(error.toString());
        }
    }

    /**
     * @since 4.0
     */
    public void setDefaultPackage(String defaultPackage) {
        config.setDefaultPackage(defaultPackage);
    }

    /**
     * @since 4.0
     */
    public void setMeaningfulPkTables(String meaningfulPkTables) {
        config.setMeaningfulPkTables(meaningfulPkTables);
    }

    public void setNamingStrategy(String namingStrategy) {
        config.setNamingStrategy(namingStrategy);
    }

    public void setAdapter(String adapter) {
        config.setAdapter(adapter);
    }

    public void setDriver(String driver) {
        config.setDriver(driver);
    }

    public void setPassword(String password) {
        config.setPassword(password);
    }

    public void setUrl(String url) {
        config.setUrl(url);
    }

    public void setUserName(String username) {
        config.setUsername(username);
    }

    public ReverseEngineering getReverseEngineering() {
        return reverseEngineering;
    }

    public File getMap() {
        return config.getDataMapFile();
    }

    public void setMap(File map) {
        config.setDataMapFile(map);
    }

    public DbImportConfiguration toParameters() {
        return config;
    }
}
