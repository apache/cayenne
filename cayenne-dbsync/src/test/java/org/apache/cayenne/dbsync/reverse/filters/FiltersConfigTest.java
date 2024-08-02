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

package org.apache.cayenne.dbsync.reverse.filters;

import junit.framework.TestCase;
import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertThrows;

public class FiltersConfigTest extends TestCase {

    private FiltersConfig filtersConfig;

    @Override
    protected void setUp() throws Exception {
        ReverseEngineering engineering = new ReverseEngineering();
        Catalog catalog = new Catalog("catalog");
        Schema schema = new Schema("schema");

        schema.addIncludeTable(new IncludeTable("iT2"));
        schema.addIncludeTable(new IncludeTable("iT1"));

        schema.addExcludeTable(new ExcludeTable("eT2"));
        schema.addExcludeTable(new ExcludeTable("eT1"));

        schema.addIncludeProcedure(new IncludeProcedure("iP2"));
        schema.addIncludeProcedure(new IncludeProcedure("iP1"));

        schema.addExcludeProcedure(new ExcludeProcedure("eP2"));
        schema.addExcludeProcedure(new ExcludeProcedure("eP1"));

        schema.addIncludeColumn(new IncludeColumn("iC2"));
        schema.addIncludeColumn(new IncludeColumn("iC1"));

        schema.addExcludeColumn(new ExcludeColumn("eC2"));
        schema.addExcludeColumn(new ExcludeColumn("eC1"));

        catalog.addSchema(schema);
        engineering.addCatalog(catalog);

        FiltersConfigBuilder configBuilder = new FiltersConfigBuilder(engineering);
        configBuilder.compact();

        filtersConfig = configBuilder.build();
    }

    public void testToString_01() {
        FiltersConfig config = FiltersConfig.create(null, null,
                TableFilter.everything(), PatternFilter.INCLUDE_EVERYTHING);

        assertEquals("Catalog: null\n" +
                     "  Schema: null\n" +
                     "    Tables: \n" +
                     "      Include: null Columns: ALL\n" +
                     "    Procedures: ALL\n", config.toString());
    }

    public void testToString_02() {
        FiltersConfig config = new FiltersConfig(
                new CatalogFilter("catalog_01",
                        new SchemaFilter("schema_11", TableFilter.everything(), PatternFilter.INCLUDE_EVERYTHING)),
                new CatalogFilter("catalog_02",
                        new SchemaFilter("schema_21", TableFilter.everything(), PatternFilter.INCLUDE_NOTHING),
                        new SchemaFilter("schema_22",
                                new TableFilter(
                                        includes(new IncludeTableFilter(null, PatternFilter.INCLUDE_NOTHING)),
                                        excludes("aaa")),
                                PatternFilter.INCLUDE_NOTHING),
                        new SchemaFilter("schema_23", TableFilter.include("include"), PatternFilter.INCLUDE_NOTHING)
                )
        );

        assertEquals("Catalog: catalog_01\n" +
                     "  Schema: schema_11\n" +
                     "    Tables: \n" +
                     "      Include: null Columns: ALL\n" +
                     "    Procedures: ALL\n" +
                     "Catalog: catalog_02\n" +
                     "  Schema: schema_21\n" +
                     "    Tables: \n" +
                     "      Include: null Columns: ALL\n" +
                     "    Procedures: NONE\n" +
                     "  Schema: schema_22\n" +
                     "    Tables: \n" +
                     "      Include: null Columns: NONE\n" +
                     "      aaa\n" +
                     "    Procedures: NONE\n" +
                     "  Schema: schema_23\n" +
                     "    Tables: \n" +
                     "      Include: include Columns: ALL\n" +
                     "    Procedures: NONE\n", config.toString());
    }

    public void testTableFilter(){
        assertNull(filtersConfig.tableFilter(null,null));

        TableFilter tableFilter = filtersConfig.tableFilter("catalog", "schema");
        assertNotNull(tableFilter);
        assertEquals(2,tableFilter.getIncludes().size());
        assertEquals(2,tableFilter.getExcludes().size());
    }

    public void testProceduresFilter(){
        assertNull(filtersConfig.proceduresFilter(null,null));

        PatternFilter patternFilter = filtersConfig.proceduresFilter("catalog", "schema");
        assertNotNull(patternFilter);
        assertEquals(2,patternFilter.getIncludes().size());
        assertEquals(2,patternFilter.getExcludes().size());
    }

    public void testGetCatalogs(){
        CatalogFilter[] catalogs = filtersConfig.getCatalogs();
        assertNotNull(catalogs);
        assertEquals(1,catalogs.length);
    }

    public void testGetCatalog(){
        assertNull(filtersConfig.getCatalog(null));
        CatalogFilter catalog = filtersConfig.getCatalog("catalog");
        assertNotNull(catalog);
        assertEquals("catalog",catalog.name);
        assertEquals(1,catalog.schemas.length);
    }

    public void testGetSchemaFilter()  {
        assertNull(filtersConfig.getSchemaFilter(null,null));

        SchemaFilter schemaFilter = filtersConfig.getSchemaFilter("catalog", "schema");
        assertNotNull(schemaFilter);
        assertEquals("schema",schemaFilter.name);
        assertEquals(2,schemaFilter.procedures.getIncludes().size());
        assertEquals(2,schemaFilter.procedures.getExcludes().size());
    }

    public void testNullArgumentBuild() {
        FiltersConfigBuilder configBuilder = new FiltersConfigBuilder(null);
        assertThrows(NullPointerException.class, configBuilder::build);
    }

    public void testKeepingOrder()  {
        SchemaFilter schemaFilter = filtersConfig.getCatalog("catalog").getSchema("schema");

        List<IncludeTableFilter> tablesIncludes = schemaFilter.tables.getIncludes();
        List<Pattern> includeColumns = tablesIncludes.get(0).columnsFilter.getIncludes();
        List<Pattern> excludeColumns = tablesIncludes.get(0).columnsFilter.getExcludes();
        List<Pattern> tablesExcludes = schemaFilter.tables.getExcludes();
        List<Pattern> proceduresIncludes = schemaFilter.procedures.getIncludes();
        List<Pattern> proceduresExcludes = schemaFilter.procedures.getExcludes();

        assertEquals("iT2", tablesIncludes.get(0).pattern.pattern());
        assertEquals("iT1", tablesIncludes.get(1).pattern.pattern());

        assertEquals("eT2", tablesExcludes.get(0).pattern());
        assertEquals("eT1", tablesExcludes.get(1).pattern());

        assertEquals("iC2", includeColumns.get(0).pattern());
        assertEquals("iC1", includeColumns.get(1).pattern());

        assertEquals("eC2", excludeColumns.get(0).pattern());
        assertEquals("eC1", excludeColumns.get(1).pattern());

        assertEquals("iP2", proceduresIncludes.get(0).pattern());
        assertEquals("iP1", proceduresIncludes.get(1).pattern());

        assertEquals("eP2", proceduresExcludes.get(0).pattern());
        assertEquals("eP1", proceduresExcludes.get(1).pattern());
    }

    private List<Pattern> excludes(String ... p) {
        List<Pattern> patterns = new ArrayList<Pattern>();
        for (String pattern : p) {
            patterns.add(PatternFilter.pattern(pattern));
        }
        return patterns;
    }

    protected List<IncludeTableFilter> includes(IncludeTableFilter ... filters) {
        List<IncludeTableFilter> includeTableFilters = new ArrayList<>();
        Collections.addAll(includeTableFilters, filters);

        return includeTableFilters;
    }

}