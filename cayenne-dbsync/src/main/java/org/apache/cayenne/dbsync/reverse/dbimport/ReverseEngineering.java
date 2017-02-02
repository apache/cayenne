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
package org.apache.cayenne.dbsync.reverse.dbimport;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;

/**
 * @since 4.0
 */
public class ReverseEngineering extends SchemaContainer implements Serializable {

    private Boolean skipRelationshipsLoading;

    private Boolean skipPrimaryKeyLoading;

    /*
     * <p>
     * A default package for ObjEntity Java classes.
     * </p>
     * <p>
     * If not specified, and the existing DataMap already has the default package,
     * the existing package will be used.
     * </p>
     */
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
    private final Collection<String> tableTypes = new LinkedList<>();

    private final Collection<Catalog> catalogCollection = new LinkedList<>();

    public ReverseEngineering() {
    }

    public Boolean getSkipRelationshipsLoading() {
        return skipRelationshipsLoading;
    }

    public void setSkipRelationshipsLoading(Boolean skipRelationshipsLoading) {
        this.skipRelationshipsLoading = skipRelationshipsLoading;
    }

    public Boolean getSkipPrimaryKeyLoading() {
        return skipPrimaryKeyLoading;
    }

    public void setSkipPrimaryKeyLoading(Boolean skipPrimaryKeyLoading) {
        this.skipPrimaryKeyLoading = skipPrimaryKeyLoading;
    }

    public Collection<Catalog> getCatalogs() {
        return catalogCollection;
    }

    public String[] getTableTypes() {
        return tableTypes.toArray(new String[tableTypes.size()]);
    }

    /*
     * Typical types are "TABLE",
     * "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY",
     * "LOCAL TEMPORARY", "ALIAS", "SYNONYM"., etc.
     */
    public void setTableTypes(Collection<String> tableTypes) {
        this.tableTypes.addAll(tableTypes);
    }

    /*
     * Typical types are "TABLE",
     * "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY",
     * "LOCAL TEMPORARY", "ALIAS", "SYNONYM"., etc.
     */
    public void addTableType(String type) {
        this.tableTypes.add(type);
    }

    public void addCatalog(Catalog catalog) {
        this.catalogCollection.add(catalog);
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        res.append("ReverseEngineering: ").append("\n");

        if (!isBlank(catalogCollection)) {
            for (Catalog catalog : catalogCollection) {
                catalog.toString(res, "  ");
            }
        }

        if (skipRelationshipsLoading != null && skipRelationshipsLoading) {
            res.append("\n        Skip Relationships Loading");
        }
        if (skipPrimaryKeyLoading != null && skipPrimaryKeyLoading) {
            res.append("\n        Skip PrimaryKey Loading");
        }

        return super.toString(res, "  ").toString();
    }

    public String getDefaultPackage() {
        return defaultPackage;
    }

    public boolean isForceDataMapCatalog() {
        return forceDataMapCatalog;
    }

    public boolean isForceDataMapSchema() {
        return forceDataMapSchema;
    }

    public String getMeaningfulPkTables() {
        return meaningfulPkTables;
    }

    public String getNamingStrategy() {
        return namingStrategy;
    }

    public String getStripFromTableNames() {
        return stripFromTableNames;
    }

    public boolean isUsePrimitives() {
        return usePrimitives;
    }

    public boolean isUseJava7Types() {
        return useJava7Types;
    }
}
