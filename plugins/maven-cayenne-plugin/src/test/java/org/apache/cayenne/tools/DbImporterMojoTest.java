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

import org.apache.cayenne.tools.dbimport.DbImportParameters;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

public class DbImporterMojoTest extends AbstractMojoTestCase {

    public void testToParameters_MeaningfulPk() throws Exception {

        File pom1 = getTestFile("src/test/resources/org/apache/cayenne/tools/dbimporter-pom1.xml");

        DbImporterMojo importer1 = (DbImporterMojo) lookupMojo("cdbimport", pom1);
        DbImportParameters parameters1 = importer1.toParameters();
        assertNull(parameters1.getMeaningfulPkTables());
        assertPathEquals("target/test/org/apache/cayenne/tools/dbimporter-map1.map.xml", parameters1.getDataMapFile()
                .getPath());

        File pom2 = getTestFile("src/test/resources/org/apache/cayenne/tools/dbimporter-pom2.xml");
        DbImporterMojo importer2 = (DbImporterMojo) lookupMojo("cdbimport", pom2);
        DbImportParameters parameters2 = importer2.toParameters();
        assertEquals("x,b*", parameters2.getMeaningfulPkTables());
        
        File pom3 = getTestFile("src/test/resources/org/apache/cayenne/tools/dbimporter-pom3.xml");
        DbImporterMojo importer3 = (DbImporterMojo) lookupMojo("cdbimport", pom3);
        DbImportParameters parameters3 = importer3.toParameters();
        assertEquals("*", parameters3.getMeaningfulPkTables());
    }

    public void testToParameters_Map() throws Exception {

        File pom1 = getTestFile("src/test/resources/org/apache/cayenne/tools/dbimporter-pom1.xml");
        DbImporterMojo importer1 = (DbImporterMojo) lookupMojo("cdbimport", pom1);
        DbImportParameters parameters1 = importer1.toParameters();
        assertNotNull(parameters1.getDataMapFile());
        assertPathEquals("target/test/org/apache/cayenne/tools/dbimporter-map1.map.xml", parameters1.getDataMapFile()
                .getPath());

        File pom2 = getTestFile("src/test/resources/org/apache/cayenne/tools/dbimporter-pom2.xml");
        DbImporterMojo importer2 = (DbImporterMojo) lookupMojo("cdbimport", pom2);
        DbImportParameters parameters2 = importer2.toParameters();
        assertNull(parameters2.getDataMapFile());
    }

    private void assertPathEquals(String expectedPath, String actualPath) {
        assertEquals(new File(expectedPath), new File(actualPath));
    }
}
