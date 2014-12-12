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
package org.apache.cayenne.tools.dbimport.config;


import java.util.Collection;
import java.util.LinkedList;

/**
 * @since 4.0.
 */
public class ReverseEngineering extends FilterContainer {

    private Boolean skipRelationshipsLoading;

    private Boolean skipPrimaryKeyLoading;

    /*
     * Typical types are "TABLE",
     * "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY",
     * "LOCAL TEMPORARY", "ALIAS", "SYNONYM"., etc.
     */
    private Collection<String> tableTypes = new LinkedList<String>();

    private Collection<Catalog> catalogs = new LinkedList<Catalog>();
    private Collection<Schema> schemas = new LinkedList<Schema>();

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
        return catalogs;
    }

    public void setCatalogs(Collection<Catalog> catalogs) {
        this.catalogs = catalogs;
    }

    public Collection<Schema> getSchemas() {
        return schemas;
    }

    public void setSchemas(Collection<Schema> schemas) {
        this.schemas = schemas;
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
        this.tableTypes = tableTypes;
    }

    /*
     * Typical types are "TABLE",
     * "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY",
     * "LOCAL TEMPORARY", "ALIAS", "SYNONYM"., etc.
     */
    public void addTableType(String type) {
        this.tableTypes.add(type);
    }

    public void addSchema(Schema schema) {
        this.schemas.add(schema);
    }

    public void addCatalog(Catalog catalog) {
        this.catalogs.add(catalog);
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("    ReverseEngineering: ");
        for (Catalog catalog : catalogs) {
            res.append("\n").append(catalog.toString("        "));
        }
        for (Schema schema : schemas) {
            res.append("\n").append(schema.toString("        "));
        }

        if (skipRelationshipsLoading != null && skipRelationshipsLoading) {
            res.append("\n").append("        Skip Relationships Loading");
        }
        if (skipPrimaryKeyLoading != null && skipPrimaryKeyLoading) {
            res.append("\n").append("        Skip PrimaryKey Loading");
        }
        res.append(super.toString("    "));

        return res.toString();
    }
}
