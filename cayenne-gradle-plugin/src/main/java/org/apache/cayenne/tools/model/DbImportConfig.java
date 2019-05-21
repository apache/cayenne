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

package org.apache.cayenne.tools.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import groovy.lang.Closure;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.gradle.util.ConfigureUtil;

/**
 * @since 4.0
 */
public class DbImportConfig extends SchemaContainer {

    private boolean skipRelationshipsLoading;

    private boolean skipPrimaryKeyLoading;

    private String defaultPackage;

    /**
     * <p>
     * Automatically tagging each DbEntity with the actual DB catalog/schema (default behavior) may sometimes be undesirable.
     * If this is the case then setting forceDataMapCatalog to true will set DbEntity catalog to one in the DataMap.
     * </p>
     * <p>
     * Default value is <b>false</b>.
     * </p>
     */
    private boolean forceDataMapCatalog;

    /**
     * <p>
     * Automatically tagging each DbEntity with the actual DB catalog/schema (default behavior) may sometimes be undesirable.
     * If this is the case then setting forceDataMapSchema to true will set DbEntity schema to one in the DataMap.
     * </p>
     * <p>
     * Default value is <b>false</b>.
     * </p>
     */
    private boolean forceDataMapSchema;

    /**
     * <p>
     * A comma-separated list of Perl5 patterns that defines which imported tables should have their primary key columns
     * mapped as ObjAttributes.
     * </p>
     * <p><b>"*"</b> would indicate all tables.</p>
     */
    private String meaningfulPkTables;

    /**
     * <p>
     * Object layer naming generator implementation.
     * Should be fully qualified Java class name implementing "org.apache.cayenne.dbsync.naming.ObjectNameGenerator".
     * </p>
     * <p>
     * The default is "org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator".
     * </p>
     */
    private String namingStrategy = "org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator";

    /**
     * A regular expression that should match the part of the table name to strip before generating DB names.
     */
    private String stripFromTableNames = "";

    /**
     * <p>If true, would use primitives instead of numeric and boolean classes.</p>
     * <p>Default is <b>"true"</b>, i.e. primitives will be used.</p>
     */
    private boolean usePrimitives = true;

    /**
     * Use old Java 7 date types
     */
    private boolean useJava7Types = false;

    /**
     * Typical types are: <ul>
     * <li> "TABLE"
     * <li> "VIEW"
     * <li> "SYSTEM TABLE"
     * <li> "GLOBAL TEMPORARY",
     * <li> "LOCAL TEMPORARY"
     * <li> "ALIAS"
     * <li> "SYNONYM"
     * </ul>
     */
    private Collection<String> tableTypes = new LinkedList<>();

    private Collection<SchemaContainer> catalogs = new LinkedList<>();

    public void catalog(String name) {
        catalogs.add(new SchemaContainer(name));
    }

    public void catalog(Closure<?> closure) {
        catalogs.add(ConfigureUtil.configure(closure, new SchemaContainer()));
    }

    public void catalog(String name, Closure<?> closure) {
        catalogs.add(ConfigureUtil.configure(closure, new SchemaContainer(name)));
    }

    public ReverseEngineering toReverseEngineering() {
        final ReverseEngineering reverseEngineering = new ReverseEngineering();
        reverseEngineering.setSkipRelationshipsLoading(skipRelationshipsLoading);
        reverseEngineering.setSkipPrimaryKeyLoading(skipPrimaryKeyLoading);
        reverseEngineering.setForceDataMapCatalog(forceDataMapCatalog);
        reverseEngineering.setForceDataMapSchema(forceDataMapSchema);
        reverseEngineering.setDefaultPackage(defaultPackage);
        reverseEngineering.setMeaningfulPkTables(meaningfulPkTables);
        reverseEngineering.setNamingStrategy(namingStrategy);
        reverseEngineering.setStripFromTableNames(stripFromTableNames);
        reverseEngineering.setUsePrimitives(usePrimitives);
        reverseEngineering.setUseJava7Types(useJava7Types);
        reverseEngineering.setTableTypes(tableTypes);

        for(SchemaContainer catalog : catalogs) {
            reverseEngineering.addCatalog(catalog.toCatalog());
        }

        for(FilterContainer schema : schemas) {
            reverseEngineering.addSchema(schema.fillContainer(new Schema()));
        }

        fillContainer(reverseEngineering);

        return reverseEngineering;
    }

    public boolean isSkipRelationshipsLoading() {
        return skipRelationshipsLoading;
    }

    public void setSkipRelationshipsLoading(boolean skipRelationshipsLoading) {
        this.skipRelationshipsLoading = skipRelationshipsLoading;
    }

    public void skipRelationshipsLoading(boolean skipRelationshipsLoading) {
        setSkipRelationshipsLoading(skipRelationshipsLoading);
    }

    public boolean isSkipPrimaryKeyLoading() {
        return skipPrimaryKeyLoading;
    }

    public void setSkipPrimaryKeyLoading(boolean skipPrimaryKeyLoading) {
        this.skipPrimaryKeyLoading = skipPrimaryKeyLoading;
    }

    public void skipPrimaryKeyLoading(boolean skipPrimaryKeyLoading) {
        setSkipPrimaryKeyLoading(skipPrimaryKeyLoading);
    }

    public String getDefaultPackage() {
        return defaultPackage;
    }

    public void setDefaultPackage(String defaultPackage) {
        this.defaultPackage = defaultPackage;
    }

    public void defaultPackage(String defaultPackage) {
        setDefaultPackage(defaultPackage);
    }

    public boolean isForceDataMapCatalog() {
        return forceDataMapCatalog;
    }

    public void setForceDataMapCatalog(boolean forceDataMapCatalog) {
        this.forceDataMapCatalog = forceDataMapCatalog;
    }

    public void forceDataMapCatalog(boolean forceDataMapCatalog) {
        setForceDataMapCatalog(forceDataMapCatalog);
    }

    public boolean isForceDataMapSchema() {
        return forceDataMapSchema;
    }

    public void setForceDataMapSchema(boolean forceDataMapSchema) {
        this.forceDataMapSchema = forceDataMapSchema;
    }

    public void forceDataMapSchema(boolean forceDataMapSchema) {
        setForceDataMapSchema(forceDataMapSchema);
    }

    public String getMeaningfulPkTables() {
        return meaningfulPkTables;
    }

    public void setMeaningfulPkTables(String meaningfulPkTables) {
        this.meaningfulPkTables = meaningfulPkTables;
    }

    public void meaningfulPkTables(String meaningfulPkTables) {
        setMeaningfulPkTables(meaningfulPkTables);
    }

    public String getNamingStrategy() {
        return namingStrategy;
    }

    public void setNamingStrategy(String namingStrategy) {
        this.namingStrategy = namingStrategy;
    }

    public void namingStrategy(String namingStrategy) {
        setNamingStrategy(namingStrategy);
    }

    public String getStripFromTableNames() {
        return stripFromTableNames;
    }

    public void setStripFromTableNames(String stripFromTableNames) {
        this.stripFromTableNames = stripFromTableNames;
    }

    public void stripFromTableNames(String stripFromTableNames) {
        setStripFromTableNames(stripFromTableNames);
    }

    public boolean isUsePrimitives() {
        return usePrimitives;
    }

    public void setUsePrimitives(boolean usePrimitives) {
        this.usePrimitives = usePrimitives;
    }

    public void usePrimitives(boolean usePrimitives) {
        setUsePrimitives(usePrimitives);
    }

    public boolean isUseJava7Types() {
        return useJava7Types;
    }

    public void setUseJava7Types(boolean useJava7Types) {
        this.useJava7Types = useJava7Types;
    }

    public void useJava7Types(boolean useJava7Types) {
        setUseJava7Types(useJava7Types);
    }

    public Collection<String> getTableTypes() {
        return tableTypes;
    }

    public void setTableTypes(Collection<String> tableTypes) {
        this.tableTypes = tableTypes;
    }

    public void tableType(String tableType) {
        tableTypes.add(tableType);
    }

    public void tableTypes(String... tableTypes) {
        this.tableTypes.addAll(Arrays.asList(tableTypes));
    }
}