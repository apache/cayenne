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
import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.access.loader.filters.EntityFilters;
import org.apache.cayenne.access.loader.filters.FilterFactory;
import org.apache.cayenne.tools.dbimport.config.Catalog;
import org.apache.cayenne.tools.dbimport.config.ExcludeColumn;
import org.apache.cayenne.tools.dbimport.config.ExcludeProcedure;
import org.apache.cayenne.tools.dbimport.config.FiltersConfigBuilder;
import org.apache.cayenne.tools.dbimport.config.IncludeColumn;
import org.apache.cayenne.tools.dbimport.config.IncludeProcedure;
import org.apache.cayenne.tools.dbimport.config.IncludeTable;
import org.apache.cayenne.tools.dbimport.config.ReverseEngineering;
import org.apache.cayenne.tools.dbimport.config.Schema;
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
     * @parameter expression="${cdbimport.map}"
     * @required
     */
    private File map;

    /**
     * A default package for ObjEntity Java classes. If not specified, and the
     * existing DataMap already has the default package, the existing package
     * will be used.
     * 
     * @parameter expression="${cdbimport.defaultPackage}"
     * @since 4.0
     */
    private String defaultPackage;

    /**
     * Indicates that the old mapping should be completely removed and replaced
     * with the new data based on reverse engineering. Default is
     * <code>true</code>.
     * 
     * @parameter expression="${cdbimport.overwrite}" default-value="true"
     */
    private boolean overwrite;

    /**
     * @parameter expression="${cdbimport.meaningfulPkTables}"
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
     * @parameter expression="${cdbimport.namingStrategy}"
     *            default-value="org.apache.cayenne.map.naming.DefaultNameGenerator"
     */
    private String namingStrategy;

    /**
     * Java class implementing org.apache.cayenne.dba.DbAdapter. This attribute
     * is optional, the default is AutoAdapter, i.e. Cayenne would try to guess
     * the DB type.
     * 
     * @parameter expression="${cdbimport.adapter}"
     *            default-value="org.apache.cayenne.dba.AutoAdapter"
     */
    private String adapter;

    /**
     * A class of JDBC driver to use for the target database.
     * 
     * @parameter expression="${cdbimport.driver}"
     * @required
     */
    private String driver;

    /**
     * JDBC connection URL of a target database.
     * 
     * @parameter expression="${cdbimport.url}"
     * @required
     */
    private String url;

    /**
     * Database user name.
     * 
     * @parameter expression="${cdbimport.username}"
     */
    private String username;

    /**
     * Database user password.
     * 
     * @parameter expression="${cdbimport.password}"
     */
    private String password;

    /**
     * If true, would use primitives instead of numeric and boolean classes.
     * 
     * @parameter expression="${cdbimport.usePrimitives}" default-value="true"
     */
    private boolean usePrimitives;

    private final EntityFilters.Builder filterBuilder = new EntityFilters.Builder();

    /**
     * If true, would use primitives instead of numeric and boolean classes.
     *
     * @parameter expression="${cdbimport.reverseEngineering}"
     */
    private ReverseEngineering reverseEngineering = new ReverseEngineering();

    /**
     * DB schema to use for DB importing.
     *
     * @parameter expression="${cdbimport.schemaName}"
     * @deprecated since 4.0 renamed to "schema"
     */
    private String schemaName;

    private void setSchemaName(String schemaName) {
        getLog().warn("'schemaName' property is deprecated. Use 'schema' instead");

        filterBuilder.schema(schemaName);
    }

    /**
     * DB schema to use for DB importing.
     *
     * @parameter expression="${cdbimport.schema}"
     * @since 4.0
     */
    private Schema schema;

    public void setSchema(Schema schema) {
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
     * @parameter expression="${cdbimport.tablePattern}"
     */
    private String tablePattern;

    public void setTablePattern(String tablePattern) {
        filterBuilder.includeTables(tablePattern);
    }

    /**
     * Indicates whether stored procedures should be imported.
     *
     * Default is <code>false</code>.
     *
     * @parameter expression="${cdbimport.importProcedures}"
     *            default-value="false"
     */
    private String importProcedures;

    public void setImportProcedures(boolean importProcedures) {
        filterBuilder.setProceduresFilters(importProcedures ? FilterFactory.TRUE : FilterFactory.NULL);
    }

    /**
     * Pattern for stored procedures to import from DB. This is only meaningful
     * if <code>importProcedures</code> is set to <code>true</code>.
     *
     * The default is to match against all stored procedures.
     *
     * @parameter expression="${cdbimport.procedurePattern}"
     */
    private String procedurePattern;

    public void setProcedurePattern(String procedurePattern) {
        filterBuilder.includeProcedures(procedurePattern);
    }

    /**
     * Indicates whether primary keys should be mapped as meaningful attributes
     * in the object entities.
     *
     * Default is <code>false</code>.
     *
     * @parameter expression="${cdbimport.meaningfulPk}" default-value="false"
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
        Injector injector = DIBootstrap.createInjector(new ToolsModule(logger), new DbImportModule());

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

    DbImportConfiguration toParameters() {
        DbImportConfiguration config = new DbImportConfiguration();
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
                .add(filterBuilder.build()).filtersConfig());
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
     * @parameter expression="${cdbimport.includeTables}"
     */
    private String includeTables;

    public void setIncludeTables(String includeTables) {
        filterBuilder.includeTables(includeTables);
    }

    /**
     * A comma-separated list of Perl5 regex that defines tables that should be
     * skipped from import.
     *
     * @parameter expression="${cdbimport.excludeTables}"
     */
    private String excludeTables;

    public void setExcludeTables(String excludeTables) {
        filterBuilder.excludeTables(excludeTables);
    }

    public void addSchema(Schema schema) {
        reverseEngineering.addSchema(schema);
    }

    /**
     * DB schema to use for DB importing.
     *
     * @parameter expression="${cdbimport.catalog}"
     * @since 4.0
     */
    private Catalog catalog[];

    public void addCatalog(Catalog catalog) {
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
        this.reverseEngineering = reverseEngineering;
    }
}
