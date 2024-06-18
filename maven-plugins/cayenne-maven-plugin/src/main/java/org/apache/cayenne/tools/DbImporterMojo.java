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
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfigBuilder;
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.util.Util;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;

/**
 * Maven mojo to reverse engineer datamap from DB.
 *
 * @since 3.0
 */
@Mojo(name = "cdbimport", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class DbImporterMojo extends AbstractMojo {

    /**
     * Java class implementing org.apache.cayenne.dba.DbAdapter. This attribute
     * is optional, the default is AutoAdapter, i.e. Cayenne would try to guess
     * the DB type.
     */
    @Parameter(defaultValue = "org.apache.cayenne.dba.AutoAdapter")
    private String adapter;

    /**
     * Connection properties.
     *
     * @see DbImportDataSourceConfig
     * @since 4.0
     */
    @Parameter
    private DbImportDataSourceConfig dataSource = new DbImportDataSourceConfig();

    /**
     * DataMap XML file to use as a base for DB importing.
     */
    @Parameter(required = true)
    private File map;

    /**
     * Project XML file to use. If set cayenneProject will be created or updated after DB importing.
     * This is optional parameter.
     * @since 4.1
     */
    @Parameter
    private File cayenneProject;

    /**
     * An object that contains reverse engineering rules.
     */
    @Parameter(name = "dbimport", property = "dbimport", alias = "dbImport")
    private ReverseEngineering dbImportConfig = new ReverseEngineering();

    @Parameter(defaultValue = "${project}" )
    private MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {

        final Logger logger = new MavenLogger(this);

        if (project == null) {
            throw new MojoExecutionException("Can't load MavenProject.");
        }

        // check missing data source parameters
        dataSource.validate();
        final Injector injector = DIBootstrap.createInjector(
                new DbSyncModule(), new ToolsModule(logger), new DbImportModule(),
                binder -> binder.bind(ClassLoaderManager.class).toInstance(new MavenPluginClassLoaderManager(project)));

        final DbImportConfiguration config = createConfig(logger);

        DataSourceFactory dataSourceFactory = injector.getInstance(DataSourceFactory.class);
        DbAdapterFactory dbAdapterFactory = injector.getInstance(DbAdapterFactory.class);
        DataNodeDescriptor dataNodeDescriptor = config.createDataNodeDescriptor();
        try {
            DataSource dataSource = dataSourceFactory.getDataSource(dataNodeDescriptor);
            DbAdapter dbAdapter = dbAdapterFactory.createAdapter(dataNodeDescriptor, dataSource);
            config.setFiltersConfig(new FiltersConfigBuilder(dbImportConfig)
                    .dataSource(dataSource)
                    .dbAdapter(dbAdapter)
                    .build());
        } catch (Exception e) {
            throw new MojoExecutionException("Error getting dataSource", e);
        }

        final DbImportConfigurationValidator validator = new DbImportConfigurationValidator(
                dbImportConfig, config, injector);

        // TODO: "validator.validate()" creates an AutoAdapter (which checks for DB type), then  "DbImportAction.execute()"
        //   does it again. We need to make AutoAdapter a DI singleton to avoid this extra operation

        try {
            validator.validate();
        } catch (Exception ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }

        try {
            injector.getInstance(DbImportAction.class).execute(config);
        } catch (Exception ex) {
           final Throwable th = Util.unwindException(ex);

            String message = "Error importing database schema";

            if (th.getLocalizedMessage() != null) {
                message += ": " + th.getLocalizedMessage();
            }

            getLog().error(message);
            throw new MojoExecutionException(message, th);
        }
    }

    DbImportConfiguration createConfig(final Logger logger) {

        DbImportConfiguration config = new DbImportConfiguration();
        if (dbImportConfig.getCatalogs().size() == 0 && dbImportConfig.isEmptyContainer()) {
            config.setUseDataMapReverseEngineering(true);
        }
        config.setAdapter(adapter);
        config.setDefaultPackage(dbImportConfig.getDefaultPackage());
        config.setDriver(dataSource.getDriver());
        config.setForceDataMapCatalog(dbImportConfig.isForceDataMapCatalog());
        config.setForceDataMapSchema(dbImportConfig.isForceDataMapSchema());
        config.setLogger(logger);
        config.setMeaningfulPkTables(dbImportConfig.getMeaningfulPkTables());
        config.setNamingStrategy(dbImportConfig.getNamingStrategy());
        config.setPassword(dataSource.getPassword());
        config.setSkipRelationshipsLoading(dbImportConfig.getSkipRelationshipsLoading());
        config.setSkipPrimaryKeyLoading(dbImportConfig.getSkipPrimaryKeyLoading());
        config.setStripFromTableNames(dbImportConfig.getStripFromTableNames());
        config.setTableTypes(dbImportConfig.getTableTypes());
        config.setTargetDataMap(map);
        config.setCayenneProject(cayenneProject);
        config.setUrl(dataSource.getUrl());
        config.setUsername(dataSource.getUsername());
        config.setUseJava7Types(dbImportConfig.isUseJava7Types());

        return config;
    }

    public File getMap() {
        return map;
    }

    /**
     * Used only in tests, Maven will inject value directly into the "map" field
     */
    public void setMap(final File map) {
        this.map = map;
    }

    /**
     * This setter is used by Maven when defined {@code <dbimport>} tag
     */
    public void setDbimport(final ReverseEngineering dbImportConfig) {
        this.dbImportConfig = dbImportConfig;
    }

    /**
     * This setter is used by Maven {@code <dbImport>} tag
     */
    public void setDbImport(final ReverseEngineering dbImportConfig) {
        this.dbImportConfig = dbImportConfig;
    }

    public ReverseEngineering getReverseEngineering() {
        return dbImportConfig;
    }

    public DbImportDataSourceConfig getDataSource() {
        return dataSource;
    }
}


