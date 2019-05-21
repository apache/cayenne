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

import java.util.Collection;
import java.util.LinkedList;

import groovy.lang.Closure;
import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.gradle.util.ConfigureUtil;

/**
 * @since 4.0
 */
public class SchemaContainer extends FilterContainer {

    Collection<FilterContainer> schemas = new LinkedList<>();

    SchemaContainer() {
    }

    SchemaContainer(String name) {
        this.setName(name);
    }

    public void schema(String name) {
        schemas.add(new FilterContainer(name));
    }

    public void schema(Closure<?> closure) {
        schemas.add(ConfigureUtil.configure(closure, new FilterContainer()));
    }

    public void schema(String name, Closure<?> closure) {
        schemas.add(ConfigureUtil.configure(closure, new FilterContainer(name)));
    }

    Catalog toCatalog() {
        Catalog catalog = fillContainer(new Catalog());
        for(FilterContainer container : schemas) {
            catalog.addSchema(container.fillContainer(new Schema()));
        }
        return catalog;
    }
}