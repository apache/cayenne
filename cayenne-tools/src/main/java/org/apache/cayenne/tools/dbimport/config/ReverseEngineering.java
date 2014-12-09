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

    private Collection<Catalog> catalogs = new LinkedList<Catalog>();
    private Collection<Schema> schemas = new LinkedList<Schema>();

    public ReverseEngineering() {
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

    public void addSchema(Schema schema) {
        this.schemas.add(schema);
    }

    public void addCatalog(Catalog catalog) {
        this.catalogs.add(catalog);
    }

}
