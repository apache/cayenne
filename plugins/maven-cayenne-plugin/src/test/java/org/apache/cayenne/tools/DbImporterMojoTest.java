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

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.cayenne.tools.dbimport.DbImportConfiguration;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.xml.sax.SAXException;

public class DbImporterMojoTest extends AbstractMojoTestCase {

    static {
        XMLUnit.setIgnoreWhitespace(true);
    }

    public void testToParameters_MeaningfulPk() throws Exception {

        DbImportConfiguration parameters1 = getCdbImport("dbimporter-pom1.xml").toParameters();
        assertNull(parameters1.getMeaningfulPkTables());
        assertPathEquals("target/test/org/apache/cayenne/tools/dbimporter-map1.map.xml",
                parameters1.getDataMapFile().getPath());

        assertEquals("x,b*", getCdbImport("dbimporter-pom2.xml").toParameters().getMeaningfulPkTables());
        assertEquals("*", getCdbImport("dbimporter-pom3.xml").toParameters().getMeaningfulPkTables());
    }

    public void testToParameters_Map() throws Exception {

        DbImportConfiguration parameters1 = getCdbImport("dbimporter-pom1.xml").toParameters();
        assertNotNull(parameters1.getDataMapFile());
        assertPathEquals("target/test/org/apache/cayenne/tools/dbimporter-map1.map.xml",
                parameters1.getDataMapFile().getPath());

        assertNull(getCdbImport("dbimporter-pom2.xml").toParameters().getDataMapFile());
    }

    private DbImporterMojo getCdbImport(String pomFileName) throws Exception {
        return (DbImporterMojo) lookupMojo("cdbimport",
                getTestFile("src/test/resources/org/apache/cayenne/tools/" + pomFileName));
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

        prepareDatabase(name, cdbImport.toParameters());

        try {
            cdbImport.execute();
            verifyResult(mapFile, mapFileCopy);
        } finally {
            cleanDb(cdbImport.toParameters());
        }
    }

    private void cleanDb(DbImportConfiguration dbImportConfiguration) throws ClassNotFoundException, IllegalAccessException, InstantiationException, SQLException {
        Class.forName(dbImportConfiguration.getDriver()).newInstance();
        // Get a connection
        Connection connection = DriverManager.getConnection(dbImportConfiguration.getUrl());
        Statement stmt = connection.createStatement();

        ResultSet tables = connection.getMetaData().getTables(null, null, null, new String[]{"TABLE"});
        while (tables.next()) {
            System.out.println("DROP TABLE " + tables.getString("TABLE_NAME"));
            stmt.execute("DROP TABLE " + tables.getString("TABLE_NAME"));
        }
    }

    private void verifyResult(File map, File mapFileCopy) {
        try {
            assertXMLEqual(new FileReader(map.getAbsolutePath() + "-result"), new FileReader(mapFileCopy));
        } catch (SAXException e) {
            e.printStackTrace();
            fail();
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    private void prepareDatabase(String sqlFile, DbImportConfiguration dbImportConfiguration) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, IOException, URISyntaxException {
        Class.forName(dbImportConfiguration.getDriver()).newInstance();
        // Get a connection
        Statement stmt = DriverManager.getConnection(dbImportConfiguration.getUrl()).createStatement();

        for (String sql : FileUtils.fileRead(sqlFile(sqlFile + ".sql")).split(";")) {
            stmt.execute(sql);
        }
    }

	private File sqlFile(String name) throws URISyntaxException {
		URL url = DbImporterMojoTest.class.getResource("dbimport/" + name);
		assertNotNull("Can't find resource: " + name);
		return new File(url.toURI());
	}
}
