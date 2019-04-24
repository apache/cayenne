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
package org.apache.cayenne.tools;

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

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.DbImportConfiguration;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.apache.cayenne.test.jdbc.SQLReader;
import org.apache.cayenne.test.resource.ResourceUtil;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.plexus.util.FileUtils;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.ElementNameAndAttributeQualifier;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;

import static org.apache.cayenne.util.Util.isBlank;
import static org.mockito.Mockito.mock;


public class DbImporterMojoTest extends AbstractMojoTestCase {

    private static DerbyManager derbyAssembly;

    static {
        XMLUnit.setIgnoreWhitespace(true);
    }

    @BeforeClass
    public static void beforeClass() throws IOException, SQLException {
        derbyAssembly = new DerbyManager("target/derby");
    }

    @AfterClass
    public static void afterClass() throws IOException, SQLException {
        derbyAssembly.shutdown();
        derbyAssembly = null;
    }

    @Test
    public void testToParameters_MeaningfulPkTables() throws Exception {

        DbImportConfiguration parameters1 = getCdbImport("dbimporter-pom1.xml").createConfig(mock(Logger.class));
        assertNull(parameters1.getMeaningfulPkTables());
        assertPathEquals("target/test/org/apache/cayenne/tools/dbimporter-map1.map.xml", parameters1.getTargetDataMap()
                .getPath());

        assertEquals("x,b*", getCdbImport("dbimporter-pom2.xml").createConfig(mock(Logger.class)).getMeaningfulPkTables());
        assertEquals("*", getCdbImport("dbimporter-pom3.xml").createConfig(mock(Logger.class)).getMeaningfulPkTables());
    }

    public void testToParameters_Map() throws Exception {

        DbImportConfiguration parameters1 = getCdbImport("dbimporter-pom1.xml").createConfig(mock(Logger.class));
        assertNotNull(parameters1.getTargetDataMap());
        assertPathEquals("target/test/org/apache/cayenne/tools/dbimporter-map1.map.xml", parameters1.getTargetDataMap()
                .getPath());

        assertNull(getCdbImport("dbimporter-pom2.xml").createConfig(mock(Logger.class)).getTargetDataMap());
    }

    private DbImporterMojo getCdbImport(String pomFileName) throws Exception {
        return (DbImporterMojo) lookupMojo("cdbimport",
                getTestFile("src/test/resources/org/apache/cayenne/tools/" + pomFileName));
    }

    private void assertPathEquals(String expectedPath, String actualPath) {
        assertEquals(new File(expectedPath), new File(actualPath));
    }

    @Test
    public void testImportNewDataMap() throws Exception {
        test("testImportNewDataMap");
    }

    @Test
    public void testImportNewRelationship() throws Exception {
        test("testImportNewRelationship");
    }

    @Test
    public void testImportWithoutChanges() throws Exception {
        test("testImportWithoutChanges");
    }

    @Test
    public void testImportAddTableAndColumn() throws Exception {
        test("testImportAddTableAndColumn");
    }

    @Test
    public void testFilteringWithSchema() throws Exception {
        test("testFilteringWithSchema");
    }

    @Test
    public void testSchemasAndTableExclude() throws Exception {
        test("testSchemasAndTableExclude");
    }

    @Test
    public void testViewsExclude() throws Exception {
        test("testViewsExclude");
    }

    @Test
    public void testTableTypes() throws Exception {
        test("testTableTypes");
    }

    @Test
    public void testDefaultPackage() throws Exception {
        test("testDefaultPackage");
    }

    @Test
    public void testSkipRelationshipsLoading() throws Exception {
        test("testSkipRelationshipsLoading");
    }

    @Test
    public void testSkipPrimaryKeyLoading() throws Exception {
        test("testSkipPrimaryKeyLoading");
    }

    @Test
    public void testOneToOne() throws Exception {
        test("testOneToOne");
    }

    @Test
    public void testExcludeRelationship() throws Exception {
        test("testExcludeRelationship");
    }

    @Test
    public void testExcludeRelationshipFirst() throws Exception {
        test("testExcludeRelationshipFirst");
    }

    @Test
    public void testNamingStrategy() throws Exception {
        test("testNamingStrategy");
    }

    /**
     * Q: what happens if an attribute or relationship is unmapped in the object layer, but then the underlying table
     * changes.
     * A: it should not recreate unmapped attributes/relationships. Only add an attribute for the new column.
     *
     * @throws Exception
     */
    @Test
    public void testPreserveCustomObjMappings() throws Exception {
        test("testPreserveCustomObjMappings");
    }

    /**
     * Q: what happens if a relationship existed over a column that was later deleted? and ‘skipRelLoading’ is true
     * A: it should remove relationship and column
     *
     * @throws Exception
     */
    @Test
    public void testPreserveRelationships() throws Exception {
        test("testPreserveRelationships");
    }

    /**
     * By default many-to-many are flattened during reverse engineering.
     * But if a user un-flattens a given N:M manually, we’d like this choice to be preserved on the next run
     */
    @Test
    public void testUnFlattensManyToMany() throws Exception {
        // TODO: this should be "xYs" : <db-relationship name="xIes"
        test("testUnFlattensManyToMany");
    }

    /**
     * Make sure any merges preserve custom object layer settings, like "usePrimitives", PK mapping as attribute, etc.
     */
    @Test
    public void testCustomObjectLayerSettings() throws Exception {
        test("testCustomObjectLayerSettings");
    }

    @Test
    public void testDbAttributeChange() throws Exception {
        test("testDbAttributeChange");
    }

	@Test
	public void testForceDataMapSchema() throws Exception {
		test("testForceDataMapSchema");
	}

    @Test
    public void testComplexChangeOrder() throws Exception {
        test("testComplexChangeOrder");
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
     *
     * @throws Exception
     */
    @Test
    @Ignore("Investigate why on different environment entity relationships order are different.")
    public void te_stFlattensManyToManyWithRecursiveLink() throws Exception {
        test("testFlattensManyToManyWithRecursiveLink");
    }

    @Test
    public void testFkAttributeRename() throws Exception {
        test("testFkAttributeRename");
    }

    @Test
    public void testJava7Types() throws Exception {
        test("testJava7Types");
    }

    @Test
    public void testJava8Types() throws Exception {
        test("testJava8Types");
    }

    @Test
    public void testInheritance() throws Exception {
        test("testInheritance");
    }

    @Test
    public void testAddedFlattenedRelationship() throws Exception {
        test("testAddedFlattenedRelationship");
    }

    @Test
    public void testFilteringConfig() throws Exception {
        DbImporterMojo cdbImport = getCdbImport("config/pom-01.xml");

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
    public void testSupportsCatalogsOnReverseEngineering() throws Exception {
        DbImporterMojo cdbImport = getCdbImport("dbimport/testSupportsCatalogsOnReverseEngineering-pom.xml");
        cdbImport.getReverseEngineering().addCatalog(new Catalog("DbImporterMojoTest2"));

        Exception exceptedException = null;
        String exceptedMessage = "Your database does not support catalogs on reverse engineering. " +
                "It allows to connect to only one at the moment. Please don't note catalogs in <dbimport> configuration.";

        try {
            cdbImport.execute();
        } catch (MojoExecutionException ex) {
            exceptedException = ex;
        }

        assertNotNull(exceptedException);
        assertEquals(exceptedMessage, exceptedException.getCause().getMessage());
    }

    private void test(String name) throws Exception {
        DbImporterMojo cdbImport = getCdbImport("dbimport/" + name + "-pom.xml");
        File mapFile = cdbImport.getMap();
        File mapFileCopy = new File(mapFile.getParentFile(), "copy-" + mapFile.getName());
        if (mapFile.exists()) {
            FileUtils.copyFile(mapFile, mapFileCopy);
            cdbImport.setMap(mapFileCopy);
        } else {
            mapFileCopy = mapFile;
        }

        DbImportConfiguration parameters = cdbImport.createConfig(mock(Logger.class));
        prepareDatabase(name, parameters);

        try {
            cdbImport.execute();
            verifyResult(mapFile, mapFileCopy);
        } finally {
            cleanDb(parameters);
        }
    }

    private void cleanDb(DbImportConfiguration dbImportConfiguration) throws Exception {

        // TODO: refactor to common DB management code... E.g. bootique-jdbc-test?
        // TODO: with in-memory Derby, it is easier to just stop and delete the database.. again see bootique-jdbc-test

        Class.forName(dbImportConfiguration.getDriver()).newInstance();

        try (Connection connection = DriverManager.getConnection(dbImportConfiguration.getUrl())) {

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

    private void verifyResult(File map, File mapFileCopy) {
        try {
            FileReader control = new FileReader(map.getAbsolutePath() + "-result");
            FileReader test = new FileReader(mapFileCopy);

            Diff prototype = new Diff(control, test);
            prototype.overrideElementQualifier(new ElementNameAndAttributeQualifier());
            DetailedDiff diff = new DetailedDiff(prototype);

            if (!diff.similar()) {
                fail(diff.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    private void prepareDatabase(String sqlFile, DbImportConfiguration dbImportConfiguration) throws Exception {

        URL sqlUrl = Objects.requireNonNull(ResourceUtil.getResource(getClass(), "dbimport/" + sqlFile + ".sql"));

        // TODO: refactor to common DB management code... E.g. bootique-jdbc-test?

        Class.forName(dbImportConfiguration.getDriver()).newInstance();

        try (Connection connection = DriverManager.getConnection(dbImportConfiguration.getUrl())) {
            try (Statement stmt = connection.createStatement();) {
                for (String sql : SQLReader.statements(sqlUrl, ";")) {
                    stmt.execute(sql);
                }
            }
        }
    }


    private MavenProject getMavenProject(String pomPath) throws Exception {
        File pom = new File(pomPath);
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setPom(pom);
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        return lookup( ProjectBuilder.class ).build( pom, configuration ).getProject();
    }
}
