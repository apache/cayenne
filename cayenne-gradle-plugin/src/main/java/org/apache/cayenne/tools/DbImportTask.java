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

package org.apache.cayenne.tools;

import javax.sql.DataSource;
import java.io.File;

import groovy.lang.Closure;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.runtime.DataSourceFactory;
import org.apache.cayenne.configuration.runtime.DbAdapterFactory;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.DbSyncModule;
import org.apache.cayenne.dbsync.reverse.configuration.ToolsModule;
import org.apache.cayenne.dbsync.reverse.dbimport.DbImportAction;
import org.apache.cayenne.dbsync.reverse.dbimport.DbImportConfiguration;
import org.apache.cayenne.dbsync.reverse.dbimport.DbImportConfigurationValidator;
import org.apache.cayenne.dbsync.reverse.dbimport.DbImportModule;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfigBuilder;
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.tools.model.DataSourceConfig;
import org.apache.cayenne.tools.model.DbImportConfig;
import org.apache.cayenne.util.Util;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.apache.cayenne.di.spi.DefaultClassLoaderManager;

/**
 * @since 4.0
 */
public class DbImportTask extends BaseCayenneTask {

    /**
     * Java class implementing org.apache.cayenne.dba.DbAdapter. This attribute
     * is optional, the default is AutoAdapter, i.e. Cayenne would try to guess
     * the DB type.
     */
    @Input
    @Optional
    private String adapter;

    /**
     * Connection properties.
     *
     * @since 4.0
     */
    @Internal
    private DataSourceConfig dataSource = new DataSourceConfig();

    @Internal
    private DbImportConfig config = new DbImportConfig();

    /**
     * An object that contains reverse engineering rules.
     */
    @Internal
    private ReverseEngineering reverseEngineering;

    private File cayenneProject;

    public DbImportTask() {
        // this task should be executed every invocation, so it is never up to date.
        getOutputs().upToDateWhen(task -> false);
    }

    @TaskAction
    public void runImport() {
        // check missing data source parameters
        dataSource.validate();

        final Injector injector = DIBootstrap.createInjector(new DbSyncModule(), new ToolsModule(getLogger()), new DbImportModule(),
                binder -> binder.bind(ClassLoaderManager.class).toInstance(new DefaultClassLoaderManager()));

        final DbImportConfiguration config = createConfig();

        DataSourceFactory dataSourceFactory = injector.getInstance(DataSourceFactory.class);
        DbAdapterFactory dbAdapterFactory = injector.getInstance(DbAdapterFactory.class);
        DataNodeDescriptor dataNodeDescriptor = config.createDataNodeDescriptor();
        try {
            DataSource dataSource = dataSourceFactory.getDataSource(dataNodeDescriptor);
            DbAdapter dbAdapter = dbAdapterFactory.createAdapter(dataNodeDescriptor, dataSource);
            config.setFiltersConfig(new FiltersConfigBuilder(reverseEngineering)
                    .dataSource(dataSource)
                    .dbAdapter(dbAdapter)
                    .build());
        } catch (Exception e) {
            throw new TaskExecutionException(this, e);
        }

        final DbImportConfigurationValidator validator = new DbImportConfigurationValidator(reverseEngineering, config, injector);
        try {
            validator.validate();
        } catch (Exception ex) {
            throw new TaskExecutionException(this, ex);
        }

        try {
            injector.getInstance(DbImportAction.class).execute(config);
        } catch (Exception ex) {
            final Throwable th = Util.unwindException(ex);

            String message = "Error importing database schema";

            if (th.getLocalizedMessage() != null) {
                message += ": " + th.getLocalizedMessage();
            }

            getLogger().error(message);
            throw new TaskExecutionException(this, th);
        }
    }

    DbImportConfiguration createConfig() {

        reverseEngineering = config.toReverseEngineering();

        DbImportConfiguration config = new DbImportConfiguration();
        if (reverseEngineering.getCatalogs().size() == 0 && reverseEngineering.isEmptyContainer()) {
            config.setUseDataMapReverseEngineering(true);
        }
        config.setAdapter(adapter);
        config.setDriver(dataSource.getDriver());
        config.setLogger(getLogger());
        config.setPassword(dataSource.getPassword());
        config.setTargetDataMap(getDataMapFile());
        config.setUrl(dataSource.getUrl());
        config.setUsername(dataSource.getUsername());
        config.setSkipRelationshipsLoading(reverseEngineering.getSkipRelationshipsLoading());
        config.setSkipPrimaryKeyLoading(reverseEngineering.getSkipPrimaryKeyLoading());
        config.setStripFromTableNames(reverseEngineering.getStripFromTableNames());
        config.setTableTypes(reverseEngineering.getTableTypes());
        config.setMeaningfulPkTables(reverseEngineering.getMeaningfulPkTables());
        config.setNamingStrategy(reverseEngineering.getNamingStrategy());
        config.setForceDataMapCatalog(reverseEngineering.isForceDataMapCatalog());
        config.setForceDataMapSchema(reverseEngineering.isForceDataMapSchema());
        config.setDefaultPackage(reverseEngineering.getDefaultPackage());
        config.setUsePrimitives(reverseEngineering.isUsePrimitives());
        config.setUseJava7Types(reverseEngineering.isUseJava7Types());
        config.setCayenneProject(cayenneProject);

        return config;
    }

    @OutputFile
    public File getDataMapFile() {
        return super.getDataMapFile();
    }

    public void dbImport(final Closure<?> closure) {
        getProject().configure(config, closure);
    }

    public void dbimport(final Closure<?> closure) {
        dbImport(closure);
    }

    public void dataSource(final Closure<?> closure) {
        getProject().configure(dataSource, closure);
    }

    public DataSourceConfig getDataSource() {
        return dataSource;
    }

    public DbImportConfig getConfig() {
        return config;
    }

    public String getAdapter() {
        return adapter;
    }

    public void setAdapter(final String adapter) {
        this.adapter = adapter;
    }

    public void adapter(final String adapter) {
        setAdapter(adapter);
    }

    public ReverseEngineering getReverseEngineering() {
        return reverseEngineering;
    }

    @OutputFile
    @Optional
    public File getCayenneProject() {
        return cayenneProject;
    }

    public void setCayenneProject(final File cayenneProject) {
        this.cayenneProject = cayenneProject;
    }

    public void cayenneProject(final File cayenneProject) {
        this.cayenneProject = cayenneProject;
    }

    public void cayenneProject(final String cayenneProjectFileName) {
        this.cayenneProject = getProject().file(cayenneProjectFileName);
    }
}