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
package org.apache.cayenne.tools.dbimport;

import java.io.File;

/**
 * @since 3.2
 */
public class DbImportParameters {

    /**
     * DataMap XML file to use as a base for DB importing.
     */
    private File dataMapFile;

    /**
     * A default package for ObjEntity Java classes.
     */
    private String defaultPackage;

    /**
     * Indicates that the old mapping should be completely removed and replaced
     * with the new data based on reverse engineering.
     */
    private boolean overwrite;

    /**
     * DB schema to use for DB importing.
     */
    private String catalog;

    /**
     * DB schema to use for DB importing.
     */
    private String schema;

    /**
     * Pattern for tables to import from DB
     */
    private String tablePattern;

    /**
     * Indicates whether stored procedures should be imported.
     */
    private boolean importProcedures;

    /**
     * Pattern for stored procedures to import from DB. This is only meaningful
     * if <code>importProcedures</code> is set to <code>true</code>.
     */
    private String procedurePattern;

    private String meaningfulPkTables;

    /**
     * Java class implementing org.apache.cayenne.map.naming.NamingStrategy.
     * This is used to specify how ObjEntities will be mapped from the imported
     * DB schema.
     */
    private String namingStrategy;

    /**
     * Java class implementing org.apache.cayenne.dba.DbAdapter. This attribute
     * is optional, the default is AutoAdapter, i.e. Cayenne would try to guess
     * the DB type.
     */
    private String adapter;

    /**
     * A class of JDBC driver to use for the target database.
     */
    private String driver;

    /**
     * JDBC connection URL of a target database.
     */
    private String url;

    /**
     * Database user name.
     */
    private String username;

    /**
     * Database user password.
     */
    private String password;

    private String includeTables;
    private String excludeTables;

    private boolean usePrimitives;

    public File getDataMapFile() {
        return dataMapFile;
    }

    public void setDataMapFile(File map) {
        this.dataMapFile = map;
    }

    public String getDefaultPackage() {
        return defaultPackage;
    }

    public void setDefaultPackage(String defaultPackage) {
        this.defaultPackage = defaultPackage;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getTablePattern() {
        return tablePattern;
    }

    public void setTablePattern(String tablePattern) {
        this.tablePattern = tablePattern;
    }

    public boolean isImportProcedures() {
        return importProcedures;
    }

    public void setImportProcedures(boolean importProcedures) {
        this.importProcedures = importProcedures;
    }

    public String getProcedurePattern() {
        return procedurePattern;
    }

    public void setProcedurePattern(String procedurePattern) {
        this.procedurePattern = procedurePattern;
    }

    public String getNamingStrategy() {
        return namingStrategy;
    }

    public void setNamingStrategy(String namingStrategy) {
        this.namingStrategy = namingStrategy;
    }

    public String getAdapter() {
        return adapter;
    }

    public void setAdapter(String adapter) {
        this.adapter = adapter;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getIncludeTables() {
        return includeTables;
    }

    public void setIncludeTables(String includeTables) {
        this.includeTables = includeTables;
    }

    public String getExcludeTables() {
        return excludeTables;
    }

    public void setExcludeTables(String excludeTables) {
        this.excludeTables = excludeTables;
    }

    /**
     * Returns a comma-separated list of Perl5 regular expressions that match
     * table names for which {@link DbImportAction} should include ObjAttribute
     * for PK.
     */
    public String getMeaningfulPkTables() {
        return meaningfulPkTables;
    }

    public void setMeaningfulPkTables(String meaningfulPkTables) {
        this.meaningfulPkTables = meaningfulPkTables;
    }

    public boolean isUsePrimitives() {
        return usePrimitives;
    }

    public void setUsePrimitives(boolean usePrimitives) {
        this.usePrimitives = usePrimitives;
    }
}
