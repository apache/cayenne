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

import static org.apache.commons.lang.StringUtils.isBlank;

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

import org.apache.cayenne.test.jdbc.SQLReader;
import org.apache.cayenne.test.resource.ResourceUtil;
import org.apache.cayenne.tools.dbimport.DbImportConfiguration;
import org.apache.cayenne.tools.dbimport.config.Catalog;
import org.apache.cayenne.tools.dbimport.config.IncludeTable;
import org.apache.cayenne.tools.dbimport.config.Schema;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.xml.sax.SAXException;

public class DbImporterMojoTest extends AbstractMojoTestCase {

	static {
		XMLUnit.setIgnoreWhitespace(true);
	}

	public void testToParameters_MeaningfulPk() throws Exception {

		DbImportConfiguration parameters1 = getCdbImport("dbimporter-pom1.xml").toParameters();
		assertNull(parameters1.getMeaningfulPkTables());
		assertPathEquals("target/test/org/apache/cayenne/tools/dbimporter-map1.map.xml", parameters1.getDataMapFile()
				.getPath());

		assertEquals("x,b*", getCdbImport("dbimporter-pom2.xml").toParameters().getMeaningfulPkTables());
		assertEquals("*", getCdbImport("dbimporter-pom3.xml").toParameters().getMeaningfulPkTables());
	}

	public void testToParameters_Map() throws Exception {

		DbImportConfiguration parameters1 = getCdbImport("dbimporter-pom1.xml").toParameters();
		assertNotNull(parameters1.getDataMapFile());
		assertPathEquals("target/test/org/apache/cayenne/tools/dbimporter-map1.map.xml", parameters1.getDataMapFile()
				.getPath());

		assertNull(getCdbImport("dbimporter-pom2.xml").toParameters().getDataMapFile());
	}

	private DbImporterMojo getCdbImport(String pomFileName) throws Exception {
		return (DbImporterMojo) lookupMojo("cdbimport", getTestFile("src/test/resources/org/apache/cayenne/tools/"
				+ pomFileName));
	}

	private void assertPathEquals(String expectedPath, String actualPath) {
		assertEquals(new File(expectedPath), new File(actualPath));
	}

	public void testImportNewDataMap() throws Exception {
		test("testImportNewDataMap");
	}

	public void testImportWithoutChanges() throws Exception {
		test("testImportWithoutChanges");
	}

	public void testImportAddTableAndColumn() throws Exception {
		test("testImportAddTableAndColumn");
	}

	public void testSimpleFiltering() throws Exception {
		test("testSimpleFiltering");
	}

	public void testFilteringWithSchema() throws Exception {
		test("testFilteringWithSchema");
	}

	public void testSchemasAndTableExclude() throws Exception {
		test("testSchemasAndTableExclude");
	}

	public void testViewsExclude() throws Exception {
		test("testViewsExclude");
	}
	
	public void testDefaultPackage() throws Exception {
		test("testDefaultPackage");
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

        DbImportConfiguration parameters = cdbImport.toParameters();
        prepareDatabase(name, parameters);

		try {
			cdbImport.execute();
			verifyResult(mapFile, mapFileCopy);
		} finally {
			cleanDb(parameters);
		}
	}

	private void cleanDb(DbImportConfiguration dbImportConfiguration) throws ClassNotFoundException,
			IllegalAccessException, InstantiationException, SQLException {
		Class.forName(dbImportConfiguration.getDriver()).newInstance();
		// Get a connection
		Connection connection = DriverManager.getConnection(dbImportConfiguration.getUrl());
		Statement stmt = connection.createStatement();

		ResultSet views = connection.getMetaData().getTables(null, null, null, new String[] { "VIEW" });
		while (views.next()) {
			String schema = views.getString("TABLE_SCHEM");
			System.out.println("DROP VIEW " + (isBlank(schema) ? "" : schema + ".") + views.getString("TABLE_NAME"));
			stmt.execute("DROP VIEW " + (isBlank(schema) ? "" : schema + ".") + views.getString("TABLE_NAME"));
		}

		ResultSet tables = connection.getMetaData().getTables(null, null, null, new String[] { "TABLE" });
		while (tables.next()) {
			String schema = tables.getString("TABLE_SCHEM");
			System.out.println("DROP TABLE " + (isBlank(schema) ? "" : schema + ".") + tables.getString("TABLE_NAME"));
			stmt.execute("DROP TABLE " + (isBlank(schema) ? "" : schema + ".") + tables.getString("TABLE_NAME"));
		}

		ResultSet schemas = connection.getMetaData().getSchemas();
		while (schemas.next()) {
			String schem = schemas.getString("TABLE_SCHEM");
			if (schem.startsWith("SCHEMA")) {
				System.out.println("DROP SCHEMA " + schem);
				stmt.execute("DROP SCHEMA " + schem + " RESTRICT");
			}
		}
	}

	private void verifyResult(File map, File mapFileCopy) {
		try {
			FileReader control = new FileReader(map.getAbsolutePath() + "-result");
			FileReader test = new FileReader(mapFileCopy);

			DetailedDiff diff = new DetailedDiff(new Diff(control, test));
			if (!diff.similar()) {
				System.out.println(" >>>> " + map.getAbsolutePath() + "-result");
				System.out.println(" >>>> " + mapFileCopy);
				fail(diff.toString());
			}

		} catch (SAXException e) {
			e.printStackTrace();
			fail();
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}

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

	private void prepareDatabase(String sqlFile, DbImportConfiguration dbImportConfiguration) throws Exception {

		URL sqlUrl = ResourceUtil.getResource(getClass(), "dbimport/" + sqlFile + ".sql");
		assertNotNull(sqlUrl);

		Class.forName(dbImportConfiguration.getDriver()).newInstance();
		// Get a connection
		Statement stmt = DriverManager.getConnection(dbImportConfiguration.getUrl()).createStatement();

		for (String sql : SQLReader.statements(sqlUrl, ";")) {
			stmt.execute(sql);
		}
	}
}
