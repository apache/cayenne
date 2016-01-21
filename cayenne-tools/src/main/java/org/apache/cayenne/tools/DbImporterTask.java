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

import org.apache.cayenne.access.loader.filters.OldFilterConfigBridge;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.server.DataSourceFactory;
import org.apache.cayenne.configuration.server.DbAdapterFactory;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbimport.AntNestedElement;
import org.apache.cayenne.dbimport.Catalog;
import org.apache.cayenne.dbimport.ExcludeColumn;
import org.apache.cayenne.dbimport.ExcludeProcedure;
import org.apache.cayenne.dbimport.ExcludeTable;
import org.apache.cayenne.dbimport.FiltersConfigBuilder;
import org.apache.cayenne.dbimport.IncludeColumn;
import org.apache.cayenne.dbimport.IncludeProcedure;
import org.apache.cayenne.dbimport.IncludeTable;
import org.apache.cayenne.dbimport.ReverseEngineering;
import org.apache.cayenne.dbimport.Schema;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.naming.DefaultNameGenerator;
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

    private final OldFilterConfigBridge filterBuilder = new OldFilterConfigBridge();

    public DbImporterTask() {
        config = new DbImportConfiguration();
        config.setOverwrite(true);
        config.setUsePrimitives(true);
        config.setNamingStrategy(DefaultNameGenerator.class.getName());
    }

    @Override
    public void execute() {

        config.setFiltersConfig(new FiltersConfigBuilder(reverseEngineering)
                .add(filterBuilder)
                .filtersConfig());

        validateAttributes();

        Log logger = new AntLogger(this);
        config.setLogger(logger);
        config.setSkipRelationshipsLoading(reverseEngineering.getSkipRelationshipsLoading());
        config.setSkipPrimaryKeyLoading(reverseEngineering.getSkipPrimaryKeyLoading());
        config.setTableTypes(reverseEngineering.getTableTypes());

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
        this.setSchema(schemaName);
    }

    /**
     * @since 4.0
     */
    public void setSchema(String schema) {
        filterBuilder.schema(schema);
    }

    /**
     * @since 4.0
     */
    public void setDefaultPackage(String defaultPackage) {
        config.setDefaultPackage(defaultPackage);
    }

    public void setTablePattern(String tablePattern) {
        filterBuilder.includeTables(tablePattern);
    }

    public void setImportProcedures(boolean importProcedures) {
        filterBuilder.setProceduresFilters(importProcedures);
    }

    public void setProcedurePattern(String procedurePattern) {
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
        filterBuilder.includeTables(includeTables);
    }

    /**
     * @since 4.0
     */
    public void setExcludeTables(String excludeTables) {
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
        reverseEngineering.addIncludeColumn(includeColumn);
    }

    public void addConfiguredExcludeColumn(ExcludeColumn excludeColumn) {
        reverseEngineering.addExcludeColumn(excludeColumn);
    }

    public void addConfiguredIncludeTable(IncludeTable includeTable) {
        reverseEngineering.addIncludeTable(includeTable);
    }

    public void addConfiguredExcludeTable(ExcludeTable excludeTable) {
        reverseEngineering.addExcludeTable(excludeTable);
    }

    public void addConfiguredIncludeProcedure(IncludeProcedure includeProcedure) {
        reverseEngineering.addIncludeProcedure(includeProcedure);
    }

    public void addConfiguredExcludeProcedure(ExcludeProcedure excludeProcedure) {
        reverseEngineering.addExcludeProcedure(excludeProcedure);
    }

    public void addConfiguredSchema(Schema schema) {
        reverseEngineering.addSchema(schema);
    }

    public void addConfiguredCatalog(Catalog catalog) {
        reverseEngineering.addCatalog(catalog);
    }

    public void addConfiguredTableType(AntNestedElement type) {
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
