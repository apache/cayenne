/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */

package org.apache.cayenne.tools.dbimport.config;

import org.apache.cayenne.access.loader.filters.DbPath;
import org.apache.cayenne.access.loader.filters.EntityFilters;
import org.apache.cayenne.access.loader.filters.FiltersConfig;
import org.junit.Test;

import static org.apache.cayenne.access.loader.filters.FilterFactory.*;
import static org.apache.cayenne.access.loader.filters.FiltersFactory.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class FiltersConfigBuilderTest {

    @Test
    public void testEmptyDbEntitiesFilters() throws Exception {
        ReverseEngineering engineering = new ReverseEngineering();
        FiltersConfig executions = new FiltersConfigBuilder(engineering).filtersConfig();

        assertEquals("If nothing was configured we have to import everything. Filter %/%/% true/true/true",
                new FiltersConfig(eFilters(path(), TRUE, TRUE, NULL)),
                executions);
    }

    @Test
    public void testOnlyOneCatalogDbEntitiesFilters() throws Exception {
        ReverseEngineering engineering = new ReverseEngineering();
        engineering.addCatalog(new Catalog("catalog_01"));
        FiltersConfig executions = new FiltersConfigBuilder(engineering).filtersConfig();


        assertEquals(new FiltersConfig(eFilters(path("catalog_01", null), TRUE, TRUE, NULL)),
                executions);
    }

    @Test
    public void testCatalogDbEntitiesFilters() throws Exception {
        ReverseEngineering engineering = new ReverseEngineering();
        engineering.addCatalog(new Catalog("catalog_01"));
        engineering.addCatalog(new Catalog("catalog_02").schema(new Schema("schema_01")));
        engineering.addCatalog(new Catalog("catalog_02").schema(new Schema("schema_02")));
        engineering.addCatalog(new Catalog("catalog_02").schema(new Schema("schema_03")));
        engineering.addCatalog(new Catalog("catalog_03").schema(new Schema("schema_01")));
        engineering.addCatalog(new Catalog("catalog_03").schema(new Schema("schema_01")));
        engineering.addCatalog(new Catalog("catalog_03").schema(new Schema("schema_01")));
        engineering.addCatalog(new Catalog("catalog_03").schema(new Schema("schema_01")));
        FiltersConfig executions = new FiltersConfigBuilder(engineering).filtersConfig();


        assertEquals(new FiltersConfig(
                        eFilters(path("catalog_01", null), TRUE, TRUE, NULL),
                        eFilters(path("catalog_02", "schema_01"), TRUE, TRUE, NULL),
                        eFilters(path("catalog_02", "schema_02"), TRUE, TRUE, NULL),
                        eFilters(path("catalog_02", "schema_03"), TRUE, TRUE, NULL),
                        eFilters(path("catalog_03", "schema_01"), TRUE, TRUE, NULL)
                ),
                executions);
    }

    @Test
    public void testSchemaDbEntitiesFilters() throws Exception {
        ReverseEngineering engineering = new ReverseEngineering();
        engineering.addSchema(new Schema("schema_01"));
        engineering.addSchema(new Schema("schema_02"));
        engineering.addSchema(new Schema("schema_03"));
        FiltersConfig executions = new FiltersConfigBuilder(engineering).filtersConfig();


        assertEquals(new FiltersConfig(
                        eFilters(path(null, "schema_01"), TRUE, TRUE, NULL),
                        eFilters(path(null, "schema_02"), TRUE, TRUE, NULL),
                        eFilters(path(null, "schema_03"), TRUE, TRUE, NULL)
                ),
                executions);
    }

    @Test
    public void testFiltersDbEntitiesFilters() throws Exception {
        ReverseEngineering engineering = new ReverseEngineering();
        engineering.addIncludeTable(new IncludeTable("IncludeTable"));
        engineering.addIncludeColumn(new IncludeColumn("IncludeColumn"));
        engineering.addIncludeProcedure(new IncludeProcedure("IncludeProcedure"));
        engineering.addExcludeTable(new ExcludeTable("ExcludeTable"));
        engineering.addExcludeColumn(new ExcludeColumn("ExcludeColumn"));
        engineering.addExcludeProcedure(new ExcludeProcedure("ExcludeProcedure"));

        FiltersConfig executions = new FiltersConfigBuilder(engineering).filtersConfig();

        assertEquals(new FiltersConfig(
                        eFilters(path(),
                            list(include("IncludeTable"), exclude("ExcludeTable")),
                            list(include("IncludeColumn"), exclude("ExcludeColumn")),
                            list(include("IncludeProcedure"), exclude("ExcludeProcedure"))),
                        eFilters(path(null, null, "IncludeTable"), NULL, TRUE, NULL)
                ),
                executions);
    }

    @Test
    public void testComplexConfiguration() throws Exception {
        IncludeTable table = new IncludeTable("table");
        table.addIncludeColumn(new IncludeColumn("column"));

        Schema schema = new Schema("schema");
        schema.addIncludeTable(table);

        Catalog catalog = new Catalog("catalog");
        catalog.addSchema(schema);

        ReverseEngineering engineering = new ReverseEngineering();
        engineering.addCatalog(catalog);

        FiltersConfig executions = new FiltersConfigBuilder(engineering).filtersConfig();

        assertEquals(new FiltersConfig(
                        eFilters(path("catalog", "schema"), include("table"), NULL, NULL),
                        eFilters(path("catalog", "schema", "table"), NULL, include("column"), NULL)
                        ),
                executions);
    }

    @Test
    public void testAddNull() throws Exception {
        FiltersConfigBuilder builder = new FiltersConfigBuilder(new ReverseEngineering());
        DbPath path = new DbPath();
        builder.add(new EntityFilters(path, NULL, NULL, NULL));
        builder.add(new EntityFilters(path, NULL, NULL, NULL));
        builder.add(new EntityFilters(path, NULL, NULL, NULL));
        builder.add(new EntityFilters(path, NULL, NULL, NULL));

        EntityFilters filter = builder.filtersConfig().filter(path);
        assertFalse(filter.isEmpty());
    }
}