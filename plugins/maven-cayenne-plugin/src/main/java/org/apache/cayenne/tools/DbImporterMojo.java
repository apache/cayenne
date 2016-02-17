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

import org.apache.cayenne.access.loader.filters.*;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.XMLDataMapLoader;
import org.apache.cayenne.configuration.server.DataSourceFactory;
import org.apache.cayenne.configuration.server.DbAdapterFactory;
import org.apache.cayenne.dba.DbAdapter;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.cayenne.dbimport.*;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.tools.configuration.ToolsModule;
import org.apache.cayenne.tools.dbimport.DbImportAction;
import org.apache.cayenne.tools.dbimport.DbImportConfiguration;
import org.apache.cayenne.tools.dbimport.DbImportModule;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import javax.sql.DataSource;

/**
 * Maven mojo to reverse engineer datamap from DB.
 * 
 * @since 3.0
 * 
 * @phase generate-sources
 * @goal cdbimport
 */
public class DbImporterMojo extends AbstractMojo {
    /**
     * DataMap XML file to use as a base for DB importing.
     * 
     * @parameter map="map"
     * @required
     */
    private File map;

    /**
     * A default package for ObjEntity Java classes. If not specified, and the
     * existing DataMap already has the default package, the existing package
     * will be used.
     * 
     * @parameter defaultPackage="defaultPackage"
     * @since 4.0
     */
    private String defaultPackage;

    /**
     * Indicates that the old mapping should be completely removed and replaced
     * with the new data based on reverse engineering. Default is
     * <code>true</code>.
     * 
     * @parameter overwrite="overwrite" default-value="true"
     */
    private boolean overwrite;

    /**
     * @parameter meaningfulPkTables="meaningfulPkTables"
     * @since 4.0
     */
    private String meaningfulPkTables;

    /**
     * Java class implementing org.apache.cayenne.map.naming.NamingStrategy.
     * This is used to specify how ObjEntities will be mapped from the imported
     * DB schema.
     * 
     * The default is a basic naming strategy.
     * 
     * @parameter namingStrategy="namingStrategy"
     *            default-value="org.apache.cayenne.map.naming.DefaultNameGenerator"
     */
    private String namingStrategy;

    /**
     * Java class implementing org.apache.cayenne.dba.DbAdapter. This attribute
     * is optional, the default is AutoAdapter, i.e. Cayenne would try to guess
     * the DB type.
     * 
     * @parameter adapter="adapter"
     *            default-value="org.apache.cayenne.dba.AutoAdapter"
     */
    private String adapter;

    /**
     * A class of JDBC driver to use for the target database.
     * 
     * @parameter driver="driver"
     * @required
     */
    private String driver;

    /**
     * JDBC connection URL of a target database.
     * 
     * @parameter url="url"
     * @required
     */
    private String url;

    /**
     * Database user name.
     * 
     * @parameter username="username"
     */
    private String username;

    /**
     * Database user password.
     * 
     * @parameter password="password"
     */
    private String password;

    /**
     * If true, would use primitives instead of numeric and boolean classes.
     * 
     * @parameter usePrimitives="usePrimitives" default-value="true"
     */
    private boolean usePrimitives;

    private final OldFilterConfigBridge filterBuilder = new OldFilterConfigBridge();

    /**
     * If true, would use primitives instead of numeric and boolean classes.
     *
     * @parameter reverseEngineering="reverseEngineering"
     */
    private ReverseEngineering reverseEngineering = new ReverseEngineering();

    private boolean isReverseEngineeringDefined = false;

    public void setIsReverseEngineeringDefined(boolean isReverseEngineeringDefined) {
        this.isReverseEngineeringDefined = isReverseEngineeringDefined;
    }

    /**
     * DB schema to use for DB importing.
     *
     * @parameter schemaName="schemaName"
     * @deprecated since 4.0 renamed to "schema"
     */
    private String schemaName;
    private DbImportConfiguration config;

    private void setSchemaName(String schemaName) {
        isReverseEngineeringDefined = true;
        getLog().warn("'schemaName' property is deprecated. Use 'schema' instead");

        filterBuilder.schema(schemaName);
    }

    /**
     * DB schema to use for DB importing.
     *
     * @parameter schema="schema"
     * @since 4.0
     */
    private Schema schema;

    public void setSchema(Schema schema) {
        isReverseEngineeringDefined = true;
        if (schema.isEmptyContainer()) {
            filterBuilder.schema(schema.getName());
        } else {
            reverseEngineering.addSchema(schema);
        }
    }

    /**
     * Pattern for tables to import from DB.
     *
     * The default is to match against all tables.
     *
     * @parameter tablePattern="tablePattern"
     */
    private String tablePattern;

    public void setTablePattern(String tablePattern) {
        isReverseEngineeringDefined = true;
        filterBuilder.includeTables(tablePattern);
    }

    /**
     * Indicates whether stored procedures should be imported.
     *
     * Default is <code>false</code>.
     *
     * @parameter importProcedures="importProcedures"
     *            default-value="false"
     */
    private String importProcedures;

    public void setImportProcedures(boolean importProcedures) {
        filterBuilder.setProceduresFilters(importProcedures);
    }

    /**
     * Pattern for stored procedures to import from DB. This is only meaningful
     * if <code>importProcedures</code> is set to <code>true</code>.
     *
     * The default is to match against all stored procedures.
     *
     * @parameter procedurePattern="procedurePattern"
     */
    private String procedurePattern;

    public void setProcedurePattern(String procedurePattern) {
        isReverseEngineeringDefined = true;
        filterBuilder.includeProcedures(procedurePattern);
    }

    /**
     * Indicates whether primary keys should be mapped as meaningful attributes
     * in the object entities.
     *
     * Default is <code>false</code>.
     *
     * @parameter meaningfulPk="meaningfulPk"
     * @deprecated since 4.0 use meaningfulPkTables
     */
    private boolean meaningfulPk;

    public void setMeaningfulPk(boolean meaningfulPk) {
        getLog().warn("'meaningfulPk' property is deprecated. Use 'meaningfulPkTables' pattern instead");

        this.meaningfulPkTables = meaningfulPk ? "*" : null;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {

        Log logger = new MavenLogger(this);

        DbImportConfiguration config = toParameters();
        config.setLogger(logger);
        File dataMapFile = config.getDataMapFile();

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

                getLog().error(message);
                throw new MojoExecutionException(message, th);
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
                        Resource reverseEngineeringResource = new URLResource(dataMapFile.toURI().toURL()).getRelativeResource(dataMap.getReverseEngineering().getName() + ".reverseEngineering.xml");

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

                            getLog().error(message);
                            throw new MojoExecutionException(message, th);
                        }
                    }
                } catch (MalformedURLException e) {
                    getLog().error(e);
                    throw new MojoExecutionException(e.getMessage(), e);
                } catch (IOException e) {
                    getLog().error(e);
                    throw new MojoExecutionException(e.getMessage(), e);
                }
            }
        }
    }

    private void validateDbImportConfiguration(DbImportConfiguration config, Injector injector) throws MojoExecutionException {
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
                throw new MojoExecutionException(message);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error creating DataSource ("
                    + dataSource + ") or DbAdapter (" + adapter + ") for DataNodeDescriptor (" + dataNodeDescriptor + ")", e);
        }
    }

    DbImportConfiguration toParameters() {
        if (config != null) {
            return config;
        }

        config = new DbImportConfiguration();
        config.setAdapter(adapter);
        config.setDefaultPackage(defaultPackage);
        config.setDriver(driver);
        config.setDataMapFile(map);
        config.setMeaningfulPkTables(meaningfulPkTables);
        config.setNamingStrategy(namingStrategy);
        config.setOverwrite(overwrite);
        config.setPassword(password);
        config.setUrl(url);
        config.setUsername(username);
        config.setUsePrimitives(usePrimitives);
        config.setFiltersConfig(new FiltersConfigBuilder(reverseEngineering)
                .add(filterBuilder).filtersConfig());
        config.setSkipRelationshipsLoading(reverseEngineering.getSkipRelationshipsLoading());
        config.setSkipPrimaryKeyLoading(reverseEngineering.getSkipPrimaryKeyLoading());
        config.setTableTypes(reverseEngineering.getTableTypes());

        return config;
    }

    public File getMap() {
        return map;
    }

    public void setMap(File map) {
        this.map = map;
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


    /**
     * A comma-separated list of Perl5 regex that defines tables that should be
     * included in import.
     *
     * @parameter includeTables="includeTables"
     */
    private String includeTables;

    public void setIncludeTables(String includeTables) {
        isReverseEngineeringDefined = true;
        filterBuilder.includeTables(includeTables);
    }

    /**
     * A comma-separated list of Perl5 regex that defines tables that should be
     * skipped from import.
     *
     * @parameter excludeTables="excludeTables"
     */
    private String excludeTables;

    public void setExcludeTables(String excludeTables) {
        isReverseEngineeringDefined = true;
        filterBuilder.excludeTables(excludeTables);
    }

    public void addSchema(Schema schema) {
        isReverseEngineeringDefined = true;
        reverseEngineering.addSchema(schema);
    }

    /**
     * DB schema to use for DB importing.
     *
     * @parameter catalog="catalog"
     * @since 4.0
     */
    private Catalog catalog[];

    public void addCatalog(Catalog catalog) {
        isReverseEngineeringDefined = true;

        if (catalog != null) {
            if (catalog.isEmptyContainer()) {
                filterBuilder.catalog(catalog.getName());
            } else {
                reverseEngineering.addCatalog(catalog);
            }
        }
    }

    public ReverseEngineering getReverseEngineering() {
        return reverseEngineering;
    }

    public void setReverseEngineering(ReverseEngineering reverseEngineering) {
        isReverseEngineeringDefined = true;
        this.reverseEngineering = reverseEngineering;
    }
}


