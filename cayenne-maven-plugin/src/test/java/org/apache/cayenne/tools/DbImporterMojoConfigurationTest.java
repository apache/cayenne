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
package org.apache.cayenne.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.DbImportConfiguration;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfigBuilder;
import org.apache.cayenne.dbsync.reverse.filters.IncludeTableFilter;
import org.apache.cayenne.dbsync.reverse.filters.PatternFilter;
import org.apache.cayenne.dbsync.reverse.filters.TableFilter;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineeringUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@MojoTest
public class DbImporterMojoConfigurationTest {

    @Test
    public void loadCatalog(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/config/pom-catalog.xml")
            DbImporterMojo mojo) throws Exception {
        Map<String, Catalog> catalogs = new HashMap<>();
        for (Catalog c : mojo.getReverseEngineering().getCatalogs()) {
            catalogs.put(c.getName(), c);
        }

        assertEquals(3, catalogs.size());
        Catalog c3 = catalogs.get("catalog-name-03");
        assertNotNull(c3);
        assertCatalog(c3);
    }

    @Test
    public void loadSchema(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/config/pom-schema.xml")
            DbImporterMojo mojo) throws Exception {
        Map<String, Schema> schemas = new HashMap<>();
        for (Schema s : mojo.getReverseEngineering().getSchemas()) {
            schemas.put(s.getName(), s);
        }

        assertEquals(3, schemas.size());
        Schema s3 = schemas.get("schema-name-03");
        assertNotNull(s3);
        assertSchemaContent(s3);
    }

    @Test
    public void loadSchema2(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/config/pom-schema-2.xml")
            DbImporterMojo mojo) throws Exception {
        DbImportConfiguration dbImportConfiguration = mojo.createConfig(mock(Logger.class));
        dbImportConfiguration.setFiltersConfig(new FiltersConfigBuilder(mojo.getReverseEngineering()).build());

        FiltersConfig filters = dbImportConfiguration.getDbLoaderConfig().getFiltersConfig();

        List<IncludeTableFilter> includes = new ArrayList<>();
        includes.add(new IncludeTableFilter(null, new PatternFilter().exclude("^ETL_.*")));

        List<Pattern> excludes = new ArrayList<>();
        excludes.add(PatternFilter.pattern("^ETL_.*"));

        assertEquals(filters.tableFilter(null, "NHL_STATS"),
                new TableFilter(includes, excludes));
    }

    @Test
    public void loadCatalogAndSchema(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/config/pom-catalog-and-schema.xml")
            DbImporterMojo mojo) throws Exception {
        assertCatalogAndSchema(mojo.getReverseEngineering());
    }

    @Test
    public void defaultPackage(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/config/pom-default-package.xml")
            DbImporterMojo mojo) throws Exception {
        DbImportConfiguration config = mojo.createConfig(mock(Logger.class));
        assertEquals("com.example.test", config.getDefaultPackage());
    }

    @Test
    public void loadFlat(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/config/pom-flat.xml")
            DbImporterMojo mojo) throws Exception {
        assertFlat(mojo.getReverseEngineering());
    }

    @Test
    public void skipRelationshipsLoading(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/config/pom-skip-relationships-loading.xml")
            DbImporterMojo mojo) throws Exception {
        assertSkipRelationshipsLoading(mojo.getReverseEngineering());
    }

    @Test
    public void skipPrimaryKeyLoading(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/config/pom-skip-primary-key-loading.xml")
            DbImporterMojo mojo) throws Exception {
        assertSkipPrimaryKeyLoading(mojo.getReverseEngineering());
    }

    @Test
    public void tableTypes(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/config/pom-table-types.xml")
            DbImporterMojo mojo) throws Exception {
        assertTableTypes(mojo.getReverseEngineering());
    }
}
