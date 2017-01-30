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

import org.apache.cayenne.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.DbSyncModule;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfigBuilder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.tools.configuration.ToolsModule;
import org.apache.cayenne.tools.dbimport.DbImportAction;
import org.apache.cayenne.tools.dbimport.DbImportConfiguration;
import org.apache.cayenne.tools.dbimport.DbImportModule;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * Maven mojo to reverse engineer datamap from DB.
 *
 * @since 3.0
 */
@Mojo(name = "cdbimport", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
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
    @Parameter(required = true)
    private DbImportDataSourceConfig dataSource = new DbImportDataSourceConfig();

    /**
     * DataMap XML file to use as a base for DB importing.
     */
    @Parameter(required = true)
    private File map;

    /**
     * An object that contains reverse engineering rules.
     */
    @Parameter(property = "dbimport", alias = "dbImport")
    private ReverseEngineering reverseEngineering = new ReverseEngineering();

    public void execute() throws MojoExecutionException, MojoFailureException {

        Log logger = new MavenLogger(this);

        DbImportConfiguration config = createConfig(logger);
        Injector injector = DIBootstrap.createInjector(
                new DbSyncModule(), new ToolsModule(logger), new DbImportModule());

        DbImportConfigurationValidator validator = new DbImportConfigurationValidator(
                reverseEngineering, config, injector);
        try {
            validator.validate();
        } catch (Exception ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }

        try {
            injector.getInstance(DbImportAction.class).execute(config);
        } catch (Exception ex) {
            Throwable th = Util.unwindException(ex);

            String message = "Error importing database schema";

            if (th.getLocalizedMessage() != null) {
                message += ": " + th.getLocalizedMessage();
            }

            getLog().error(message);
            throw new MojoExecutionException(message, th);
        }
    }

    DbImportConfiguration createConfig(Log logger) {

        DbImportConfiguration config = new DbImportConfiguration();
        config.setAdapter(adapter);
        config.setDefaultPackage(reverseEngineering.getDefaultPackage());
        config.setDriver(dataSource.getDriver());
        config.setFiltersConfig(new FiltersConfigBuilder(reverseEngineering).build());
        config.setForceDataMapCatalog(reverseEngineering.isForceDataMapCatalog());
        config.setForceDataMapSchema(reverseEngineering.isForceDataMapSchema());
        config.setLogger(logger);
        config.setMeaningfulPkTables(reverseEngineering.getMeaningfulPkTables());
        config.setNamingStrategy(reverseEngineering.getNamingStrategy());
        config.setPassword(dataSource.getPassword());
        config.setSkipRelationshipsLoading(reverseEngineering.getSkipRelationshipsLoading());
        config.setSkipPrimaryKeyLoading(reverseEngineering.getSkipPrimaryKeyLoading());
        config.setStripFromTableNames(reverseEngineering.getStripFromTableNames());
        config.setTableTypes(reverseEngineering.getTableTypes());
        config.setTargetDataMap(map);
        config.setUrl(dataSource.getUrl());
        config.setUsername(dataSource.getUsername());
        config.setUsePrimitives(reverseEngineering.isUsePrimitives());

        return config;
    }

    public File getMap() {
        return map;
    }

    public void setMap(File map) {
        this.map = map;
    }

    /**
     * This setter is used by Maven
     */
    public void setDbimport(ReverseEngineering reverseEngineering) {
        this.reverseEngineering = reverseEngineering;
    }

    /**
     * This setter is used by Maven
     */
    public void setDbImport(ReverseEngineering reverseEngineering) {
        this.reverseEngineering = reverseEngineering;
    }

    public ReverseEngineering getReverseEngineering() {
        return reverseEngineering;
    }

    // ⬇⬇⬇ All following setters should be removed in 4.0.BETA ⬇⬇⬇ //

    /**
     * Setter to catch old styled configuration
     * @deprecated to be removed in 4.0.BETA
     */
    @Deprecated
    public void setUrl(String url) {
        throw new UnsupportedOperationException("Connection properties were replaced with <dataSource> tag since 4.0.M5.\n\tFor additional information see http://cayenne.apache.org/docs/4.0/cayenne-guide/including-cayenne-in-project.html#maven-projects");
    }

    /**
     * Setter to catch old styled configuration
     * @deprecated to be removed in 4.0.BETA
     */
    @Deprecated
    public void setUser(String user) {
        throw new UnsupportedOperationException("Connection properties were replaced with <dataSource> tag since 4.0.M5.\n\tFor additional information see http://cayenne.apache.org/docs/4.0/cayenne-guide/including-cayenne-in-project.html#maven-projects");
    }

    /**
     * Setter to catch old styled configuration
     * @deprecated to be removed in 4.0.BETA
     */
    @Deprecated
    public void setPassword(String password) {
        throw new UnsupportedOperationException("Connection properties were replaced with <dataSource> tag since 4.0.M5.\n\tFor additional information see http://cayenne.apache.org/docs/4.0/cayenne-guide/including-cayenne-in-project.html#maven-projects");
    }

    /**
     * Setter to catch old styled configuration
     * @deprecated to be removed in 4.0.BETA
     */
    @Deprecated
    public void setDriver(String driver) {
        throw new UnsupportedOperationException("Connection properties were replaced with <dataSource> tag since 4.0.M5.\n\tFor additional information see http://cayenne.apache.org/docs/4.0/cayenne-guide/including-cayenne-in-project.html#maven-projects");
    }

    /**
     * Setter to catch old styled configuration
     * @deprecated to be removed in 4.0.BETA
     */
    @Deprecated
    public void setForceDataMapCatalog(boolean forceDataMapCatalog) {
        throw new UnsupportedOperationException("forceDataMapCatalog property has been moved to <dbimport> tag since 4.0.M5.\n\tFor additional information see http://cayenne.apache.org/docs/4.0/cayenne-guide/including-cayenne-in-project.html#maven-projects");
    }

    /**
     * Setter to catch old styled configuration
     * @deprecated to be removed in 4.0.BETA
     */
    @Deprecated
    public void setForceDataMapSchema(boolean forceDataMapSchema) {
        throw new UnsupportedOperationException("forceDataMapSchema property has been moved to <dbimport> tag since 4.0.M5.\n\tFor additional information see http://cayenne.apache.org/docs/4.0/cayenne-guide/including-cayenne-in-project.html#maven-projects");
    }

    /**
     * Setter to catch old styled configuration
     * @deprecated to be removed in 4.0.BETA
     */
    @Deprecated
    public void setMeaningfulPkTables(String meaningfulPkTables) {
        throw new UnsupportedOperationException("meaningfulPkTables property has been moved to <dbimport> tag since 4.0.M5.\n\tFor additional information see http://cayenne.apache.org/docs/4.0/cayenne-guide/including-cayenne-in-project.html#maven-projects");
    }

    /**
     * Setter to catch old styled configuration
     * @deprecated to be removed in 4.0.BETA
     */
    @Deprecated
    public void setNamingStrategy(String namingStrategy) {
        throw new UnsupportedOperationException("namingStrategy property has been moved to <dbimport> tag since 4.0.M5.\n\tFor additional information see http://cayenne.apache.org/docs/4.0/cayenne-guide/including-cayenne-in-project.html#maven-projects");
    }

    /**
     * Setter to catch old styled configuration
     * @deprecated to be removed in 4.0.BETA
     */
    @Deprecated
    public void setDefaultPackage(String defaultPackage) {
        throw new UnsupportedOperationException("defaultPackage property has been deprecated since 4.0.M5.\n\tFor additional information see http://cayenne.apache.org/docs/4.0/cayenne-guide/including-cayenne-in-project.html#maven-projects");
    }

    /**
     * Setter to catch old styled configuration
     * @deprecated to be removed in 4.0.BETA
     */
    @Deprecated
    public void setStripFromTableNames(String stripFromTableNames) {
        throw new UnsupportedOperationException("stripFromTableNames property has been deprecated since 4.0.M5.\n\tFor additional information see http://cayenne.apache.org/docs/4.0/cayenne-guide/including-cayenne-in-project.html#maven-projects");
    }

    /**
     * Setter to catch old styled configuration
     * @deprecated to be removed in 4.0.BETA
     */
    @Deprecated
    public void setUsePrimitives(boolean usePrimitives) {
        throw new UnsupportedOperationException("usePrimitives property has been deprecated since 4.0.M5.\n\tFor additional information see http://cayenne.apache.org/docs/4.0/cayenne-guide/including-cayenne-in-project.html#maven-projects");
    }

    /**
     * Setter to catch old styled configuration
     * @deprecated to be removed in 4.0.BETA
     */
    @Deprecated
    public void setReverseEngineering(ReverseEngineering reverseEngineering) {
        throw new UnsupportedOperationException("<reverseEngineering> tag has been replaced with <dbimport> since 4.0.M5.\n\tFor additional information see http://cayenne.apache.org/docs/4.0/cayenne-guide/including-cayenne-in-project.html#maven-projects");
    }
}


