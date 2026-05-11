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

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.DbImportConfiguration;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.apache.cayenne.test.jdbc.SQLReader;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.xmlunit.matchers.CompareMatcher;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Objects;

import static org.apache.cayenne.util.Util.isBlank;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;


@MojoTest
public class DbImporterMojoTest {

    private static DerbyManager derbyAssembly;

    @BeforeAll
    public static void beforeAll() throws IOException, SQLException {
        derbyAssembly = new DerbyManager("target/derby");
    }

    @AfterAll
    public static void afterAll() throws IOException, SQLException {
        derbyAssembly.shutdown();
        derbyAssembly = null;
    }

    @Test
    public void toParameters_MeaningfulPkTables(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimporter-pom1.xml") DbImporterMojo mojo1,
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimporter-pom2.xml") DbImporterMojo mojo2,
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimporter-pom3.xml") DbImporterMojo mojo3) throws Exception {

        DbImportConfiguration parameters1 = mojo1.createConfig(mock(Logger.class));
        assertNull(parameters1.getMeaningfulPkTables());
        assertPathEquals("target/test/org/apache/cayenne/tools/dbimporter-map1.map.xml", parameters1.getTargetDataMap().getPath());

        assertEquals("x,b*", mojo2.createConfig(mock(Logger.class)).getMeaningfulPkTables());
        assertEquals("*", mojo3.createConfig(mock(Logger.class)).getMeaningfulPkTables());
    }

    @Test
    public void toParameters_Map(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimporter-pom1.xml") DbImporterMojo mojo1,
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimporter-pom2.xml") DbImporterMojo mojo2) throws Exception {

        DbImportConfiguration parameters1 = mojo1.createConfig(mock(Logger.class));
        assertNotNull(parameters1.getTargetDataMap());
        assertPathEquals("target/test/org/apache/cayenne/tools/dbimporter-map1.map.xml", parameters1.getTargetDataMap().getPath());

        assertNull(mojo2.createConfig(mock(Logger.class)).getTargetDataMap());
    }

    @Test
    public void importNewDataMap(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testImportNewDataMap-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testImportNewDataMap");
    }

    @Test
    public void importNewRelationship(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testImportNewRelationship-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testImportNewRelationship");
    }

    @Test
    public void importWithoutChanges(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testImportWithoutChanges-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testImportWithoutChanges");
    }

    @Test
    public void importAddTableAndColumn(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testImportAddTableAndColumn-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testImportAddTableAndColumn");
    }

    @Test
    public void filteringWithSchema(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testFilteringWithSchema-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testFilteringWithSchema");
    }

    @Test
    public void schemasAndTableExclude(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testSchemasAndTableExclude-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testSchemasAndTableExclude");
    }

    @Test
    public void viewsExclude(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testViewsExclude-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testViewsExclude");
    }

    @Test
    public void tableTypes(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testTableTypes-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testTableTypes");
    }

    @Test
    public void defaultPackage(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testDefaultPackage-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testDefaultPackage");
    }

    @Test
    public void skipRelationshipsLoading(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testSkipRelationshipsLoading-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testSkipRelationshipsLoading");
    }

    @Test
    public void skipPrimaryKeyLoading(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testSkipPrimaryKeyLoading-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testSkipPrimaryKeyLoading");
    }

    @Test
    public void oneToOne(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testOneToOne-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testOneToOne");
    }

    @Test
    public void excludeRelationship(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testExcludeRelationship-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testExcludeRelationship");
    }

    @Test
    public void excludeRelationshipFirst(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testExcludeRelationshipFirst-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testExcludeRelationshipFirst");
    }

    @Test
    public void namingStrategy(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testNamingStrategy-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testNamingStrategy");
    }

    /**
     * Q: what happens if an attribute or relationship is unmapped in the object layer, but then the underlying table
     * changes.
     * A: it should not recreate unmapped attributes/relationships. Only add an attribute for the new column.
     */
    @Test
    public void preserveCustomObjMappings(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testPreserveCustomObjMappings-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testPreserveCustomObjMappings");
    }

    /**
     * Q: what happens if a relationship existed over a column that was later deleted? and 'skipRelLoading' is true
     * A: it should remove relationship and column
     */
    @Test
    public void preserveRelationships(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testPreserveRelationships-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testPreserveRelationships");
    }

    /**
     * By default many-to-many are flattened during reverse engineering.
     * But if a user un-flattens a given N:M manually, we'd like this choice to be preserved on the next run
     */
    @Test
    public void unFlattensManyToMany(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testUnFlattensManyToMany-pom.xml")
            DbImporterMojo mojo) throws Exception {
        // TODO: this should be "xYs" : <db-relationship name="xIes"
        test(mojo, "testUnFlattensManyToMany");
    }

    /**
     * Make sure any merges preserve custom object layer settings, like "usePrimitives", PK mapping as attribute, etc.
     */
    @Test
    public void customObjectLayerSettings(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testCustomObjectLayerSettings-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testCustomObjectLayerSettings");
    }

    @Test
    public void dbAttributeChange(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testDbAttributeChange-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testDbAttributeChange");
    }

    @Test
    public void forceDataMapSchema(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testForceDataMapSchema-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testForceDataMapSchema");
    }

    @Test
    public void complexChangeOrder(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testComplexChangeOrder-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testComplexChangeOrder");
    }

    @Test
    public void configFromDataMap(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testConfigFromDataMap-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testConfigFromDataMap");
    }

    @Test
    public void tableTypesFromDataMapConfig(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testTableTypesMap-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testTableTypesMap");
    }

    /**
     * CREATE TABLE APP.A (
     * id INTEGER NOT NULL,
     * <p>
     * PRIMARY KEY (id)
     * );
     * <p>
     * CREATE TABLE APP.A_A (
     * A1_ID INTEGER NOT NULL,
     * A2_ID INTEGER NOT NULL,
     * <p>
     * PRIMARY KEY (A1_ID, A2_ID),
     * CONSTRAINT A_A1 FOREIGN KEY (A1_ID) REFERENCES APP.A (ID),
     * CONSTRAINT A_A2 FOREIGN KEY (A2_ID) REFERENCES APP.A (ID)
     * );
     * <p>
     * If one table has many-to-many relationship with it self ObjEntity should have two
     * collection attributes in both directions
     */
    @Test
    @Disabled("Investigate why on different environment entity relationships order are different.")
    public void flattensManyToManyWithRecursiveLink(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testFlattensManyToManyWithRecursiveLink-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testFlattensManyToManyWithRecursiveLink");
    }

    @Test
    public void fkAttributeRename(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testFkAttributeRename-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testFkAttributeRename");
    }

    @Test
    public void java7Types(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testJava7Types-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testJava7Types");
    }

    @Test
    public void java8Types(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testJava8Types-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testJava8Types");
    }

    @Test
    public void inheritance(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testInheritance-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testInheritance");
    }

    @Test
    public void addedFlattenedRelationship(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testAddedFlattenedRelationship-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testAddedFlattenedRelationship");
    }

    @Test
    public void importProcedure(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testImportProcedure-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testImportProcedure");
    }

    @Test
    public void dropProcedure(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testDropProcedure-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testDropProcedure");
    }

    @Test
    public void sameProcedures(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testSameProcedure-pom.xml")
            DbImporterMojo mojo) throws Exception {
        test(mojo, "testSameProcedure");
    }

    @Test
    public void filteringConfig(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/config/pom-01.xml")
            DbImporterMojo cdbImport) throws Exception {

        assertEquals(2, cdbImport.getReverseEngineering().getCatalogs().size());
        Iterator<Catalog> iterator = cdbImport.getReverseEngineering().getCatalogs().iterator();
        assertEquals("catalog-name-01", iterator.next().getName());

        Catalog catalog = iterator.next();
        assertEquals("catalog-name-02", catalog.getName());
        Iterator<Schema> schemaIterator = catalog.getSchemas().iterator();

        assertEquals("schema-name-01", schemaIterator.next().getName());

        Schema schema = schemaIterator.next();
        assertEquals("schema-name-02", schema.getName());

        Iterator<IncludeTable> includeTableIterator = schema.getIncludeTables().iterator();
        assertEquals("incTable-01", includeTableIterator.next().getPattern());

        IncludeTable includeTable = includeTableIterator.next();
        assertEquals("incTable-02", includeTable.getPattern());
        assertEquals("includeColumn-01", includeTable.getIncludeColumns().iterator().next().getPattern());
        assertEquals("excludeColumn-01", includeTable.getExcludeColumns().iterator().next().getPattern());

        assertEquals("includeColumn-02", schema.getIncludeColumns().iterator().next().getPattern());
        assertEquals("excludeColumn-02", schema.getExcludeColumns().iterator().next().getPattern());

        assertEquals("includeColumn-03", catalog.getIncludeColumns().iterator().next().getPattern());
        assertEquals("excludeColumn-03", catalog.getExcludeColumns().iterator().next().getPattern());

        schemaIterator = cdbImport.getReverseEngineering().getSchemas().iterator();
        schema = schemaIterator.next();
        assertEquals("schema-name-03", schema.getName());

        schema = schemaIterator.next();
        assertEquals("schema-name-04", schema.getName());

        includeTableIterator = schema.getIncludeTables().iterator();
        assertEquals("incTable-04", includeTableIterator.next().getPattern());
        assertEquals("excTable-04", schema.getExcludeTables().iterator().next().getPattern());

        includeTable = includeTableIterator.next();
        assertEquals("incTable-05", includeTable.getPattern());
        assertEquals("includeColumn-04", includeTable.getIncludeColumns().iterator().next().getPattern());
        assertEquals("excludeColumn-04", includeTable.getExcludeColumns().iterator().next().getPattern());

        assertEquals("includeColumn-04", schema.getIncludeColumns().iterator().next().getPattern());
        assertEquals("excludeColumn-04", schema.getExcludeColumns().iterator().next().getPattern());

        assertEquals("includeColumn-03", catalog.getIncludeColumns().iterator().next().getPattern());
        assertEquals("excludeColumn-03", catalog.getExcludeColumns().iterator().next().getPattern());
    }

    @Test
    public void supportsCatalogsOnReverseEngineering(
            @InjectMojo(goal = "cdbimport", pom = "src/test/resources/org/apache/cayenne/tools/dbimport/testSupportsCatalogsOnReverseEngineering-pom.xml")
            DbImporterMojo cdbImport) throws Exception {
        cdbImport.getReverseEngineering().addCatalog(new Catalog("DbImporterMojoTest2"));

        String expectedMessage = "Your database does not support catalogs on reverse engineering. " +
                "It allows to connect to only one at the moment. Please don't note catalogs in <dbimport> configuration.";

        MojoExecutionException ex = assertThrows(MojoExecutionException.class, cdbImport::execute);
        assertEquals(expectedMessage, ex.getCause().getMessage());
    }

    private void assertPathEquals(String expectedPath, String actualPath) {
        assertEquals(new File(expectedPath).getAbsoluteFile(), new File(actualPath).getAbsoluteFile());
    }

    private void test(DbImporterMojo cdbImport, String name) throws Exception {
        File mapFile = cdbImport.getMap();
        File mapFileCopy = new File(mapFile.getParentFile(), "copy-" + mapFile.getName());
        if (mapFile.exists()) {
            FileUtils.copyFile(mapFile, mapFileCopy);
            cdbImport.setMap(mapFileCopy);
        } else {
            mapFileCopy = mapFile;
        }

        DbImportDataSourceConfig dataSource = cdbImport.getDataSource();
        prepareDatabase(name, dataSource);

        try {
            cdbImport.execute();
            verifyResult(mapFile, mapFileCopy);
        } finally {
            cleanDb(dataSource);
        }
    }

    private void cleanDb(DbImportDataSourceConfig dataSource) throws Exception {

        // TODO: refactor to common DB management code... E.g. bootique-jdbc-test?
        // TODO: with in-memory Derby, it is easier to just stop and delete the database.. again see bootique-jdbc-test

        Class.forName(dataSource.getDriver()).getDeclaredConstructor().newInstance();

        try (Connection connection = DriverManager.getConnection(dataSource.getUrl())) {

            try (Statement stmt = connection.createStatement()) {

                try (ResultSet views = connection.getMetaData().getTables(null, null, null, new String[]{"VIEW"})) {
                    while (views.next()) {
                        String schema = views.getString("TABLE_SCHEM");
                        execute(stmt, "DROP VIEW " + (isBlank(schema) ? "" : schema + ".") + views.getString("TABLE_NAME"));
                    }
                }

                try (ResultSet tables = connection.getMetaData().getTables(null, null, null, new String[]{"TABLE"})) {
                    while (tables.next()) {
                        String schema = tables.getString("TABLE_SCHEM");
                        String tableName = tables.getString("TABLE_NAME");
                        String tableNameFull = (isBlank(schema) ? "" : schema + ".") + tableName;

                        ResultSet keys = connection.getMetaData().getExportedKeys(null, schema, tableName);
                        while (keys.next()) {
                            String fkTableSchem = keys.getString("FKTABLE_SCHEM");
                            String fkTableName = keys.getString("FKTABLE_NAME");
                            String fkTableNameFull = (isBlank(fkTableSchem) ? "" : fkTableSchem + ".") + fkTableName;
                            execute(stmt, "ALTER TABLE " + fkTableNameFull + " DROP CONSTRAINT " + keys.getString("FK_NAME"));
                        }

                        String sql = "DROP TABLE " + tableNameFull;
                        execute(stmt, sql);
                    }
                }

                try(ResultSet procedures = connection.getMetaData()
                        .getProcedures(null, null,"PROC")) {
                    while(procedures.next()) {
                        String schema = procedures.getString("PROCEDURE_SCHEM");
                        String name = procedures.getString("PROCEDURE_NAME");
                        execute(stmt, "DROP PROCEDURE " + (isBlank(schema) ? "" : schema + ".") + name);
                    }
                }

                try (ResultSet schemas = connection.getMetaData().getSchemas()) {
                    while (schemas.next()) {
                        String schem = schemas.getString("TABLE_SCHEM");
                        if (schem.startsWith("SCHEMA")) {
                            execute(stmt, "DROP SCHEMA " + schem + " RESTRICT");
                        }
                    }
                }
            }
        }
    }

    private void execute(Statement stmt, String sql) throws SQLException {
        stmt.execute(sql);
    }

    private void verifyResult(File map, File mapFileCopy) throws Exception {
        FileReader control = new FileReader(map.getAbsolutePath() + "-result");
        FileReader test = new FileReader(mapFileCopy);

        assertThat(test, CompareMatcher.isSimilarTo(control).ignoreWhitespace());
    }

    private void prepareDatabase(String sqlFile, DbImportDataSourceConfig dataSource) throws Exception {

        URL sqlUrl = Objects.requireNonNull(DbImporterMojoTest.class.getResource("dbimport/" + sqlFile + ".sql"));

        // TODO: refactor to common DB management code... E.g. bootique-jdbc-test?

        Class.forName(dataSource.getDriver()).getDeclaredConstructor().newInstance();

        try (Connection connection = DriverManager.getConnection(dataSource.getUrl())) {
            try (Statement stmt = connection.createStatement()) {
                for (String sql : SQLReader.statements(sqlUrl, ";")) {
                    stmt.execute(sql);
                }
            }
        }
    }
}
