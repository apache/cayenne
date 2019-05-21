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

import org.apache.cayenne.dbsync.reverse.dbimport.DbImportConfiguration;
import org.apache.cayenne.test.file.FileUtil;
import org.apache.cayenne.test.jdbc.SQLReader;
import org.apache.cayenne.test.resource.ResourceUtil;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.util.FileUtils;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.xml.sax.SAXException;

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

import static org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineeringUtils.*;
import static org.apache.cayenne.util.Util.isBlank;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

// TODO: we are only testing on Derby. We may need to dynamically switch between DBs 
// based on "cayenneTestConnection", like we do in cayenne-server, etc.
public class DbImporterTaskTest {

    static {
        XMLUnit.setIgnoreWhitespace(true);
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
    public void testBuildWithProject() throws Exception {
        assertNotNull(getCdbImport("build-with-project.xml").getCayenneProject());
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

    private String getPackagePath() {
        return getClass().getPackage().getName().replace('.', '/');
    }

    private void test(String name) throws Exception {
        DbImporterTask cdbImport = getCdbImport(name);
        File mapFile = cdbImport.getMap();

        URL mapUrlRes = this.getClass().getResource(mapFile.getName() + "-result");
        assertTrue(mapUrlRes != null && new File(mapUrlRes.toURI()).exists());
        assertTrue(ResourceUtil
                .copyResourceToFile(mapUrlRes, new File(mapFile.getParentFile(), mapFile.getName() + "-result")));


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

        ResultSet tables = connection.getMetaData().getTables(null, null, null, new String[]{"TABLE"});
        while (tables.next()) {
            String schema = tables.getString("TABLE_SCHEM");
            stmt.execute("DROP TABLE " + (isBlank(schema) ? "" : schema + ".") + tables.getString("TABLE_NAME"));
        }

        ResultSet schemas = connection.getMetaData().getSchemas();
        while (schemas.next()) {
            String schem = schemas.getString("TABLE_SCHEM");
            if (schem.startsWith("SCHEMA")) {
                stmt.execute("DROP SCHEMA " + schem + " RESTRICT");
            }
        }
    }

    @SuppressWarnings("unchecked")
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

        } catch (SAXException | IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    private void prepareDatabase(String sqlFile, DbImportConfiguration dbImportConfiguration) throws Exception {

        URL sqlUrl = ResourceUtil.getResource(getClass(), sqlFile + ".sql");
        assertNotNull(sqlUrl);

        Class.forName(dbImportConfiguration.getDriver()).newInstance();

        try (Connection c = DriverManager.getConnection(dbImportConfiguration.getUrl());) {

            // TODO: move parsing SQL files to a common utility (DBHelper?) .
            // Also see UnitDbApater.executeDDL - this should use the same utility

            try (Statement stmt = c.createStatement();) {
                for (String sql : SQLReader.statements(sqlUrl, ";")) {

                    // skip comments
                    if (sql.startsWith("-- ")) {
                        continue;
                    }

                    stmt.execute(sql);
                }
            }
        }
    }

}