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

package org.apache.cayenne.dbsync.reverse.dbimport;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.dbsync.xml.DbImportExtension;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * @since 4.0
 */
public class ReverseEngineering extends SchemaContainer implements Serializable, XMLSerializable {

    private boolean skipRelationshipsLoading;

    private boolean skipPrimaryKeyLoading;

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
    private final List<String> tableTypes = new ArrayList<>();

    private final List<Catalog> catalogCollection = new ArrayList<>();

    public ReverseEngineering() {
    }

    public ReverseEngineering(ReverseEngineering original) {
        super(original);
        this.setDefaultPackage(original.getDefaultPackage());
        this.setStripFromTableNames(original.getStripFromTableNames());
        this.setNamingStrategy(original.getNamingStrategy());
        this.setMeaningfulPkTables(original.getMeaningfulPkTables());
        this.setSkipPrimaryKeyLoading(original.getSkipPrimaryKeyLoading());
        this.setSkipRelationshipsLoading(original.getSkipRelationshipsLoading());
        this.setForceDataMapSchema(original.isForceDataMapSchema());
        this.setForceDataMapCatalog(original.isForceDataMapCatalog());
        this.setUseJava7Types(original.isUseJava7Types());
        this.setTableTypes(Arrays.asList(original.getTableTypes()));
        this.setName(original.getName());
        for (Catalog catalog : original.getCatalogs()) {
            this.addCatalog(new Catalog(catalog));
        }
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

    public List<Catalog> getCatalogs() {
        return catalogCollection;
    }

    public String[] getTableTypes() {
        return tableTypes.toArray(new String[tableTypes.size()]);
    }

    public Collection<String> getTableTypesCollection() {
        return tableTypes;
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

        super.toString(res, "  ");

        if (skipRelationshipsLoading) {
            res.append("\n  Skip Relationships Loading");
        }
        if (skipPrimaryKeyLoading) {
            res.append("\n  Skip PrimaryKey Loading");
        }
        if (forceDataMapCatalog) {
            res.append("\n  Force DataMap catalog");
        }
        if (forceDataMapSchema) {
            res.append("\n  Force DataMap schema");
        }
        if (useJava7Types) {
            res.append("\n  Use Java 7 types");
        }
        return res.toString();
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

    /**
     * @return false
     * @deprecated since 5.0
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public boolean isUsePrimitives() {
        return false;
    }

    public boolean isUseJava7Types() {
        return useJava7Types;
    }

    public void setDefaultPackage(String defaultPackage) {
        this.defaultPackage = defaultPackage;
    }

    public void setForceDataMapCatalog(boolean forceDataMapCatalog) {
        this.forceDataMapCatalog = forceDataMapCatalog;
    }

    public void setForceDataMapSchema(boolean forceDataMapSchema) {
        this.forceDataMapSchema = forceDataMapSchema;
    }

    public void setMeaningfulPkTables(String meaningfulPkTables) {
        this.meaningfulPkTables = meaningfulPkTables;
    }

    public void setNamingStrategy(String namingStrategy) {
        this.namingStrategy = namingStrategy;
    }

    public void setStripFromTableNames(String stripFromTableNames) {
        this.stripFromTableNames = stripFromTableNames;
    }

    /**
     * does nothing
     * @deprecated since 5.0
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public void setUsePrimitives(boolean usePrimitives) {
    }

    public void setUseJava7Types(boolean useJava7Types) {
        this.useJava7Types = useJava7Types;
    }

    @Override
    public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor delegate) {
        encoder.start("dbImport")
                .attribute("xmlns", DbImportExtension.NAMESPACE)
                .nested(this.getIncludeTables(), delegate)
                .nested(this.getExcludeTables(), delegate)
                .nested(this.getIncludeColumns(), delegate)
                .nested(this.getExcludeColumns(), delegate)
                .nested(this.getIncludeProcedures(), delegate)
                .nested(this.getExcludeProcedures(), delegate)
                .nested(this.getCatalogs(), delegate)
                .nested(this.getSchemas(), delegate);
        String[] tableTypes = this.getTableTypes();
        if(tableTypes.length != 0) {
            encoder.start("tableTypes");
            for(String type : tableTypes) {
                encoder.simpleTag("tableType", type);
            }
            encoder.end();
        }
        encoder.simpleTag("defaultPackage", this.getDefaultPackage())
                .simpleTag("forceDataMapCatalog", Boolean.toString(this.isForceDataMapCatalog()))
                .simpleTag("forceDataMapSchema", Boolean.toString(this.isForceDataMapSchema()))
                .simpleTag("meaningfulPkTables", this.getMeaningfulPkTables())
                .simpleTag("namingStrategy", this.getNamingStrategy())
                .simpleTag("skipPrimaryKeyLoading", this.getSkipPrimaryKeyLoading().toString())
                .simpleTag("skipRelationshipsLoading", this.getSkipRelationshipsLoading().toString())
                .simpleTag("stripFromTableNames", this.getStripFromTableNames())
                .simpleTag("useJava7Types", Boolean.toString(this.isUseJava7Types()))
                .end();
    }

}
