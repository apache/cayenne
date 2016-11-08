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

import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.server.DataSourceFactory;
import org.apache.cayenne.configuration.server.DbAdapterFactory;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbimport.*;
import org.apache.cayenne.dbsync.DbSyncModule;
import org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfigBuilder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
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

public class DbImporterTask extends Task {

    private final DbImportConfiguration config;
    private ReverseEngineering reverseEngineering;

    public DbImporterTask() {
        this.config = new DbImportConfiguration();
        this.config.setUsePrimitives(true);
        this.config.setNamingStrategy(DefaultObjectNameGenerator.class.getName());

        // reverse engineering config is flattened into task...
        this.reverseEngineering = new ReverseEngineering();
    }

    public void addIncludeColumn(IncludeColumn includeColumn) {
        reverseEngineering.addIncludeColumn(includeColumn);
    }

    public void addExcludeColumn(ExcludeColumn excludeColumn) {
        reverseEngineering.addExcludeColumn(excludeColumn);
    }

    public void addIncludeTable(IncludeTable includeTable) {
        reverseEngineering.addIncludeTable(includeTable);
    }

    public void addExcludeTable(ExcludeTable excludeTable) {
        reverseEngineering.addExcludeTable(excludeTable);
    }

    public void addIncludeProcedure(IncludeProcedure includeProcedure) {
        reverseEngineering.addIncludeProcedure(includeProcedure);
    }

    public void addExcludeProcedure(ExcludeProcedure excludeProcedure) {
        reverseEngineering.addExcludeProcedure(excludeProcedure);
    }

    public void setSkipRelationshipsLoading(boolean skipRelationshipsLoading) {
        reverseEngineering.setSkipRelationshipsLoading(skipRelationshipsLoading);
    }

    public void setSkipPrimaryKeyLoading(boolean skipPrimaryKeyLoading) {
        reverseEngineering.setSkipPrimaryKeyLoading(skipPrimaryKeyLoading);
    }

    public void addConfiguredTableType(AntTableType type) {
        reverseEngineering.addTableType(type.getName());
    }

    public void addConfiguredSchema(Schema schema) {
        reverseEngineering.addSchema(schema);
    }

    public void addCatalog(Catalog catalog) {
        reverseEngineering.addCatalog(catalog);
    }

    @Override
    public void execute() {
        config.setFiltersConfig(new FiltersConfigBuilder(reverseEngineering).build());
        validateAttributes();

        Log logger = new AntLogger(this);
        config.setLogger(logger);
        config.setSkipRelationshipsLoading(reverseEngineering.getSkipRelationshipsLoading());
        config.setSkipPrimaryKeyLoading(reverseEngineering.getSkipPrimaryKeyLoading());
        config.setTableTypes(reverseEngineering.getTableTypes());

        Injector injector = DIBootstrap.createInjector(new DbSyncModule(), new ToolsModule(logger), new DbImportModule());

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

        if (config.getTargetDataMap() == null) {
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

    /**
     * @since 4.0
     */
    public void setStripFromTableNames(String pattern) {
        config.setStripFromTableNames(pattern);
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

    public void setUsePrimitives(boolean flag) {
        config.setUsePrimitives(flag);
    }

    public void setForceDataMapCatalog(boolean flag) {
        config.setForceDataMapCatalog(flag);
    }

    public void setForceDataMapSchema(boolean flag) {
        config.setForceDataMapSchema(flag);
    }

    public ReverseEngineering getReverseEngineering() {
        return reverseEngineering;
    }

    public File getMap() {
        return config.getTargetDataMap();
    }

    public void setMap(File map) {
        config.setTargetDataMap(map);
    }

    public DbImportConfiguration toParameters() {
        return config;
    }
}
