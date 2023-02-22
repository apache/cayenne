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
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.Test;
import org.slf4j.Logger;

import static org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineeringUtils.*;
import static org.mockito.Mockito.mock;

public class DbImporterMojoConfigurationTest extends AbstractMojoTestCase {

    @Test
    public void testLoadCatalog() throws Exception {
        Map<String, Catalog> catalogs = new HashMap<>();
        for (Catalog c : getCdbImport("pom-catalog.xml").getReverseEngineering().getCatalogs()) {
            catalogs.put(c.getName(), c);
        }

        assertEquals(3, catalogs.size());
        Catalog c3 = catalogs.get("catalog-name-03");
        assertNotNull(c3);
        assertCatalog(c3);
    }

    @Test
    public void testLoadSchema() throws Exception {
        Map<String, Schema> schemas = new HashMap<>();
        for (Schema s : getCdbImport("pom-schema.xml").getReverseEngineering().getSchemas()) {
            schemas.put(s.getName(), s);
        }

        assertEquals(3, schemas.size());
        Schema s3 = schemas.get("schema-name-03");
        assertNotNull(s3);
        assertSchemaContent(s3);
    }

    @Test
    public void testLoadSchema2() throws Exception {
        DbImporterMojo dbImporterMojo = getCdbImport("pom-schema-2.xml");
        DbImportConfiguration dbImportConfiguration = dbImporterMojo.createConfig(mock(Logger.class));
        dbImportConfiguration.setFiltersConfig(new FiltersConfigBuilder(
                dbImporterMojo.getReverseEngineering()).build());

        FiltersConfig filters = dbImportConfiguration.getDbLoaderConfig().getFiltersConfig();

        List<IncludeTableFilter> includes = new ArrayList<>();
        includes.add(new IncludeTableFilter(null, new PatternFilter().exclude("^ETL_.*")));

        List<Pattern> excludes = new ArrayList<>();
        excludes.add(PatternFilter.pattern("^ETL_.*"));

        assertEquals(filters.tableFilter(null, "NHL_STATS"),
                new TableFilter(includes, excludes));
    }

    @Test
    public void testLoadCatalogAndSchema() throws Exception {
        assertCatalogAndSchema(getCdbImport("pom-catalog-and-schema.xml").getReverseEngineering());
    }

    @Test
    public void testDefaultPackage() throws Exception {
        DbImportConfiguration config = getCdbImport("pom-default-package.xml").createConfig(mock(Logger.class));
        assertEquals("com.example.test", config.getDefaultPackage());
    }

    @Test
    public void testLoadFlat() throws Exception {
        assertFlat(getCdbImport("pom-flat.xml").getReverseEngineering());
    }

    @Test
    public void testSkipRelationshipsLoading() throws Exception {
        assertSkipRelationshipsLoading(getCdbImport("pom-skip-relationships-loading.xml").getReverseEngineering());
    }

    @Test
    public void testSkipPrimaryKeyLoading() throws Exception {
        assertSkipPrimaryKeyLoading(getCdbImport("pom-skip-primary-key-loading.xml").getReverseEngineering());
    }

    @Test
    public void testTableTypes() throws Exception {
        assertTableTypes(getCdbImport("pom-table-types.xml").getReverseEngineering());
    }

    private DbImporterMojo getCdbImport(String pomFileName) throws Exception {
        return (DbImporterMojo) lookupMojo("cdbimport",
                getTestFile("src/test/resources/org/apache/cayenne/tools/config/" + pomFileName));
    }

}
