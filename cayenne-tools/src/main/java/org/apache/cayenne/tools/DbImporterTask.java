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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.cayenne.access.loader.filters.OldFilterConfigBridge;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.XMLDataMapLoader;
import org.apache.cayenne.configuration.server.DataSourceFactory;
import org.apache.cayenne.configuration.server.DbAdapterFactory;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbimport.*;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.naming.DefaultNameGenerator;
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

public class DbImporterTask extends Task {

    private final DbImportConfiguration config;

    private final ReverseEngineering reverseEngineering = new ReverseEngineering();
    private boolean isReverseEngineeringDefined = false;

    private final OldFilterConfigBridge filterBuilder = new OldFilterConfigBridge();

    public DbImporterTask() {
        config = new DbImportConfiguration();
        config.setOverwrite(true);
        config.setUsePrimitives(true);
        config.setNamingStrategy(DefaultNameGenerator.class.getName());
    }

    @Override
    public void execute() {
        File dataMapFile = config.getDataMapFile();
        config.setFiltersConfig(new FiltersConfigBuilder(reverseEngineering)
                .add(filterBuilder)
                .filtersConfig());

        validateAttributes();

        Log logger = new AntLogger(this);
        config.setLogger(logger);
        config.setSkipRelationshipsLoading(reverseEngineering.getSkipRelationshipsLoading());
        config.setSkipPrimaryKeyLoading(reverseEngineering.getSkipPrimaryKeyLoading());
        config.setTableTypes(reverseEngineering.getTableTypes());

        if (isReverseEngineeringDefined) {
            Injector injector = DIBootstrap.createInjector(new ToolsModule(logger), new DbImportModule());

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
            }
            finally {
                injector.shutdown();
            }
        }
        else {
            if (dataMapFile.exists()) {
                try {
                    URL url = dataMapFile.toURI().toURL();
                    URLResource resource = new URLResource(url);

                    XMLDataMapLoader xmlDataMapLoader = new XMLDataMapLoader();
                    DataMap dataMap = xmlDataMapLoader.load(resource);
                    if (dataMap.getReverseEngineering() != null) {
                        Resource reverseEngineeringResource = new URLResource(dataMapFile.toURL()).getRelativeResource(dataMap.getReverseEngineering().getName() + ".reverseEngineering.xml");

                        DefaultReverseEngineeringLoader reverseEngineeringLoader = new DefaultReverseEngineeringLoader();
                        ReverseEngineering reverseEngineering = reverseEngineeringLoader.load(reverseEngineeringResource.getURL().openStream());
                        reverseEngineering.setName(dataMap.getReverseEngineering().getName());
                        reverseEngineering.setConfigurationSource(reverseEngineeringResource);
                        dataMap.setReverseEngineering(reverseEngineering);

                        FiltersConfigBuilder filtersConfigBuilder = new FiltersConfigBuilder(dataMap.getReverseEngineering());
                        config.getDbLoaderConfig().setFiltersConfig(filtersConfigBuilder.filtersConfig());
                        Injector injector = DIBootstrap.createInjector(new ToolsModule(logger), new DbImportModule());

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
                        }
                        finally {
                            injector.shutdown();
                        }
                    }
                } catch (MalformedURLException e) {
                    log(e.getMessage(), Project.MSG_ERR);
                    throw new BuildException(e.getMessage(), e);
                } catch (IOException e) {
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
    public void setOverwrite(boolean overwrite) {
        config.setOverwrite(overwrite);
    }

    /**
     * @deprecated since 4.0 use {@link #setSchema(String)}
     */
    @Deprecated
    public void setSchemaName(String schemaName) {
        isReverseEngineeringDefined = true;
        this.setSchema(schemaName);
    }

    /**
     * @since 4.0
     */
    public void setSchema(String schema) {
        isReverseEngineeringDefined = true;
        filterBuilder.schema(schema);
    }

    /**
     * @since 4.0
     */
    public void setDefaultPackage(String defaultPackage) {
        config.setDefaultPackage(defaultPackage);
    }

    public void setTablePattern(String tablePattern) {
        isReverseEngineeringDefined = true;
        filterBuilder.includeTables(tablePattern);
    }

    public void setImportProcedures(boolean importProcedures) {
        isReverseEngineeringDefined = true;
        filterBuilder.setProceduresFilters(importProcedures);
    }

    public void setProcedurePattern(String procedurePattern) {
        isReverseEngineeringDefined = true;
        filterBuilder.includeProcedures(procedurePattern);
    }

    /**
     * @deprecated since 4.0 use {@link #setMeaningfulPkTables(String)}
     */
    public void setMeaningfulPk(boolean meaningfulPk) {
        log("'meaningfulPk' property is deprecated. Use 'meaningfulPkTables' pattern instead", Project.MSG_WARN);

        if (meaningfulPk) {
            setMeaningfulPkTables("*");
        }
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

    public void setMap(File map) {
        config.setDataMapFile(map);
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

    /**
     * @since 4.0
     */
    public void setIncludeTables(String includeTables) {
        isReverseEngineeringDefined = true;
        filterBuilder.includeTables(includeTables);
    }

    /**
     * @since 4.0
     */
    public void setExcludeTables(String excludeTables) {
        isReverseEngineeringDefined = true;
        filterBuilder.excludeTables(excludeTables);
    }

    /**
     * @since 4.0
     */
    public void setUsePrimitives(boolean usePrimitives) {
        config.setUsePrimitives(usePrimitives);
    }

    public void setSkipRelationshipsLoading(Boolean skipRelationshipsLoading) {
        reverseEngineering.setSkipRelationshipsLoading(skipRelationshipsLoading);
    }

    public void addConfiguredIncludeColumn(IncludeColumn includeColumn) {
        isReverseEngineeringDefined = true;
        reverseEngineering.addIncludeColumn(includeColumn);
    }

    public void addConfiguredExcludeColumn(ExcludeColumn excludeColumn) {
        isReverseEngineeringDefined = true;
        reverseEngineering.addExcludeColumn(excludeColumn);
    }

    public void addConfiguredIncludeTable(IncludeTable includeTable) {
        isReverseEngineeringDefined = true;
        reverseEngineering.addIncludeTable(includeTable);
    }

    public void addConfiguredExcludeTable(ExcludeTable excludeTable) {
        isReverseEngineeringDefined = true;
        reverseEngineering.addExcludeTable(excludeTable);
    }

    public void addConfiguredIncludeProcedure(IncludeProcedure includeProcedure) {
        isReverseEngineeringDefined = true;
        reverseEngineering.addIncludeProcedure(includeProcedure);
    }

    public void addConfiguredExcludeProcedure(ExcludeProcedure excludeProcedure) {
        isReverseEngineeringDefined = true;
        reverseEngineering.addExcludeProcedure(excludeProcedure);
    }

    public void addConfiguredSchema(Schema schema) {
        reverseEngineering.addSchema(schema);
    }

    public void addConfiguredCatalog(Catalog catalog) {
        isReverseEngineeringDefined = true;
        reverseEngineering.addCatalog(catalog);
    }

    public void addConfiguredTableType(AntNestedElement type) {
        isReverseEngineeringDefined = true;
        reverseEngineering.addTableType(type.getName());
    }

    public ReverseEngineering getReverseEngineering() {
        return reverseEngineering;
    }

    public File getMap() {
        return config.getDataMapFile();
    }

    public DbImportConfiguration toParameters() {
        return config;
    }
}
