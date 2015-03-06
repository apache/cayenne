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

import static org.apache.cayenne.tools.dbimport.config.DefaultReverseEngineeringLoaderTest.*;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.cayenne.test.file.FileUtil;
import org.apache.cayenne.test.jdbc.SQLReader;
import org.apache.cayenne.test.resource.ResourceUtil;
import org.apache.cayenne.tools.dbimport.DbImportConfiguration;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.util.FileUtils;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceListener;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

// TODO: we are only testing on Derby. We may need to dynamically switch between DBs 
// based on "cayenneTestConnection", like we do in cayenne-server, etc.
public class DbImporterTaskTest {

	static {
		XMLUnit.setIgnoreWhitespace(true);
	}

	@Test
	public void testLoadCatalog() throws Exception {
		assertCatalog(getCdbImport("build-catalog.xml").getReverseEngineering());
	}

	@Test
	public void testLoadSchema() throws Exception {
		assertSchema(getCdbImport("build-schema.xml").getReverseEngineering());
	}

	@Test
	public void testLoadCatalogAndSchema() throws Exception {
		assertCatalogAndSchema(getCdbImport("build-catalog-and-schema.xml").getReverseEngineering());
	}

	@Test
	public void testLoadFlat() throws Exception {
		assertFlat(getCdbImport("build-flat.xml").getReverseEngineering());
	}

	@Test
	public void testSkipRelationshipsLoading() throws Exception {
		assertSkipRelationshipsLoading(getCdbImport("build-skip-relationships-loading.xml").getReverseEngineering());
	}

    @Test
    public void testTableTypes() throws Exception {
        assertTableTypes(getCdbImport("build-table-types.xml").getReverseEngineering());
    }

	@Test
	public void testIncludeTable() throws Exception {
		test("build-include-table.xml");
	}

	private DbImporterTask getCdbImport(String buildFile) {
		Project project = new Project();

		File map = distDir(buildFile);
		ResourceUtil.copyResourceToFile(getPackagePath() + "/" + buildFile, map);
		ProjectHelper.configureProject(project, map);

		UnknownElement task = (UnknownElement) project.getTargets().get("dist").getTasks()[0];
		task.maybeConfigure();

		return (DbImporterTask) task.getRealThing();
	}

	private static File distDir(String name) {
		File distDir = new File(FileUtil.baseTestDirectory(), "cdbImport");
		File file = new File(distDir, name);
		distDir = file.getParentFile();
		// prepare destination directory
		if (!distDir.exists()) {
			assertTrue(distDir.mkdirs());
		}
		return file;
	}

	private String getPackagePath() {
		return getClass().getPackage().getName().replace('.', '/');
	}

	private void test(String name) throws Exception {
		DbImporterTask cdbImport = getCdbImport("dbimport/" + name);
		File mapFile = cdbImport.getMap();
		URL mapUrl = this.getClass().getResource("dbimport/" + mapFile.getName());
		if (mapUrl != null && new File(mapUrl.toURI()).exists()) {
			ResourceUtil.copyResourceToFile(mapUrl, mapFile);
		}

		URL mapUrlRes = this.getClass().getResource("dbimport/" + mapFile.getName() + "-result");
		if (mapUrlRes != null && new File(mapUrlRes.toURI()).exists()) {
			ResourceUtil
					.copyResourceToFile(mapUrlRes, new File(mapFile.getParentFile(), mapFile.getName() + "-result"));
		}

		File mapFileCopy = distDir("copy-" + mapFile.getName());
		if (mapFile.exists()) {
			FileUtils.getFileUtils().copyFile(mapFile, mapFileCopy);
			cdbImport.setMap(mapFileCopy);
		} else {
			mapFileCopy = mapFile;
		}

		prepareDatabase(name, cdbImport.toParameters());

		try {
			cdbImport.execute();
			verifyResult(mapFile, mapFileCopy);
		} finally {
			cleanDb(cdbImport.toParameters());
		}
	}

	private void cleanDb(DbImportConfiguration dbImportConfiguration) throws ClassNotFoundException,
			IllegalAccessException, InstantiationException, SQLException {
		Class.forName(dbImportConfiguration.getDriver()).newInstance();
		// Get a connection
		Connection connection = DriverManager.getConnection(dbImportConfiguration.getUrl());
		Statement stmt = connection.createStatement();

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
                for (Difference d : ((List<Difference>) diff.getAllDifferences())) {


                    System.out.println("-------------------------------------------");
                    System.out.println(d.getTestNodeDetail().getNode());
                    System.out.println(d.getControlNodeDetail().getValue());
                }
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

	private void prepareDatabase(String sqlFile, DbImportConfiguration dbImportConfiguration) throws Exception {

		URL sqlUrl = ResourceUtil.getResource(getClass(), "dbimport/" + sqlFile + ".sql");
		assertNotNull(sqlUrl);

		Class.forName(dbImportConfiguration.getDriver()).newInstance();

		Connection c = DriverManager.getConnection(dbImportConfiguration.getUrl());
		try {

			Statement stmt = c.createStatement();

			// TODO: move parsing SQL files to a common utility (DBHelper?) .
			// ALso see UnitDbApater.executeDDL - this should use the same
			// utility

			try {
				for (String sql : SQLReader.statements(sqlUrl, ";")) {

					// skip comments
					if (sql.startsWith("-- ")) {
						continue;
					}

					stmt.execute(sql);
				}
			} finally {
				stmt.close();
			}
		} finally {
			c.close();
		}
	}

}