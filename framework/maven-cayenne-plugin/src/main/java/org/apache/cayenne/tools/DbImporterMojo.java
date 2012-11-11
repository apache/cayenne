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

import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.tools.configuration.ToolsModule;
import org.apache.cayenne.tools.dbimport.DbImportAction;
import org.apache.cayenne.tools.dbimport.DbImportModule;
import org.apache.cayenne.tools.dbimport.DbImportParameters;
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
     * @since 3.2
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
     * DB schema to use for DB importing.
     * 
     * @parameter expression="${cdbimport.schemaName}"
     * @deprecated since 3.2 renamed to "schema"
     */
    private String schemaName;

    /**
     * DB schema to use for DB importing.
     * 
     * @parameter expression="${cdbimport.catalog}"
     * @since 3.2
     */
    private String catalog;

    /**
     * DB schema to use for DB importing.
     * 
     * @parameter expression="${cdbimport.schema}"
     * @since 3.2
     */
    private String schema;

    /**
     * Pattern for tables to import from DB.
     * 
     * The default is to match against all tables.
     * 
     * @parameter expression="${cdbimport.tablePattern}"
     */
    private String tablePattern;

    /**
     * A comma-separated list of Perl5 regex that defines tables that should be
     * included in import.
     * 
     * @parameter expression="${cdbimport.includeTables}"
     */
    private String includeTables;

    /**
     * A comma-separated list of Perl5 regex that defines tables that should be
     * skipped from import.
     * 
     * @parameter expression="${cdbimport.excludeTables}"
     */
    private String excludeTables;

    /**
     * Indicates whether stored procedures should be imported.
     * 
     * Default is <code>false</code>.
     * 
     * @parameter expression="${cdbimport.importProcedures}"
     *            default-value="false"
     */
    private boolean importProcedures;

    /**
     * Pattern for stored procedures to import from DB. This is only meaningful
     * if <code>importProcedures</code> is set to <code>true</code>.
     * 
     * The default is to match against all stored procedures.
     * 
     * @parameter expression="${cdbimport.procedurePattern}"
     */
    private String procedurePattern;

    /**
     * Indicates whether primary keys should be mapped as meaningful attributes
     * in the object entities.
     * 
     * Default is <code>false</code>.
     * 
     * @parameter expression="${cdbimport.meaningfulPk}" default-value="false"
     * @deprecated since 3.2 use meaningfulPkTables
     */
    private boolean meaningfulPk;

    /**
     * @parameter expression="${cdbimport.meaningfulPkTables}"
     * @since 3.2
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
     *            default-value="org.apache.cayenne.map.naming.SmartNamingStrategy"
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

    public void execute() throws MojoExecutionException, MojoFailureException {

        Log logger = new MavenLogger(this);

        DbImportParameters parameters = toParameters();
        Injector injector = DIBootstrap.createInjector(new ToolsModule(logger), new DbImportModule());

        try {
            injector.getInstance(DbImportAction.class).execute(parameters);
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

    DbImportParameters toParameters() {
        DbImportParameters parameters = new DbImportParameters();
        parameters.setAdapter(adapter);
        parameters.setCatalog(catalog);
        parameters.setDefaultPackage(defaultPackage);
        parameters.setDriver(driver);
        parameters.setImportProcedures(importProcedures);
        parameters.setDataMapFile(map);
        parameters.setMeaningfulPkTables(getMeaningfulPkTables());
        parameters.setNamingStrategy(namingStrategy);
        parameters.setOverwrite(overwrite);
        parameters.setPassword(password);
        parameters.setProcedurePattern(procedurePattern);
        parameters.setSchema(getSchema());
        parameters.setTablePattern(tablePattern);
        parameters.setUrl(url);
        parameters.setUsername(username);
        parameters.setIncludeTables(includeTables);
        parameters.setExcludeTables(excludeTables);
        parameters.setUsePrimitives(usePrimitives);
        return parameters;
    }

    private String getSchema() {
        if (schemaName != null) {
            getLog().warn("'schemaName' property is deprecated. Use 'schema' instead");
        }

        return schema != null ? schema : schemaName;
    }

    private String getMeaningfulPkTables() {
        if (meaningfulPk) {
            getLog().warn("'meaningfulPk' property is deprecated. Use 'meaningfulPkTables' pattern instead");
        }

        if (meaningfulPkTables != null) {
            return meaningfulPkTables;
        }

        return meaningfulPk ? "*" : null;
    }

}
