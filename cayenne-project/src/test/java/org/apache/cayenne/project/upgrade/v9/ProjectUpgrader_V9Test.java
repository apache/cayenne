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
package org.apache.cayenne.project.upgrade.v9;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.DefaultConfigurationNameMapper;
import org.apache.cayenne.configuration.XMLDataMapLoader;
import org.apache.cayenne.di.*;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.di.spi.DefaultClassLoaderManager;
import org.apache.cayenne.project.FileProjectSaver;
import org.apache.cayenne.project.ProjectSaver;
import org.apache.cayenne.project.unit.Project2Case;
import org.apache.cayenne.project.upgrade.UpgradeHandler;
import org.apache.cayenne.project.upgrade.UpgradeMetaData;
import org.apache.cayenne.project.upgrade.UpgradeType;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.test.file.FileUtil;
import org.apache.cayenne.test.resource.ResourceUtil;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ProjectUpgrader_V9Test extends Project2Case {

    @Test
    public void testMetadata_3_0_0_1() {

        String baseUrl = getClass().getPackage().getName().replace('.', '/');
        URL url = getClass().getClassLoader().getResource(baseUrl + "/3_0_0_1a/cayenne.xml");
        assertNotNull(url);

        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(ProjectSaver.class).to(FileProjectSaver.class);
                binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);
            }
        };

        ProjectUpgrader_V9 upgrader = new ProjectUpgrader_V9();
        Injector injector = DIBootstrap.createInjector(testModule);
        injector.injectMembers(upgrader);

        Resource source = new URLResource(url);
        UpgradeHandler handler = upgrader.getUpgradeHandler(source);

        assertNotNull(handler);
        assertSame(source, handler.getProjectSource());

        UpgradeMetaData md = handler.getUpgradeMetaData();
        assertNotNull(md);

        assertSame(UpgradeType.UPGRADE_NEEDED, md.getUpgradeType());
        assertNull(md.getIntermediateUpgradeVersion());
        assertEquals("3.0.0.1", md.getProjectVersion());
        assertEquals("9", md.getSupportedVersion());
    }

    @Test
    public void testMetadata_Type6() {
        String baseUrl = getClass().getPackage().getName().replace('.', '/');
        URL url = getClass().getClassLoader().getResource(baseUrl + "/6a/cayenne-PROJECT1.xml");
        assertNotNull(url);

        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(ProjectSaver.class).to(FileProjectSaver.class);
                binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);
            }
        };

        ProjectUpgrader_V9 upgrader = new ProjectUpgrader_V9();
        Injector injector = DIBootstrap.createInjector(testModule);
        injector.injectMembers(upgrader);

        Resource source = new URLResource(url);
        UpgradeHandler handler = upgrader.getUpgradeHandler(source);

        assertNotNull(handler);
        assertSame(source, handler.getProjectSource());

        UpgradeMetaData md = handler.getUpgradeMetaData();
        assertNotNull(md);
        assertSame(UpgradeType.UPGRADE_NEEDED, md.getUpgradeType());
        assertNull(md.getIntermediateUpgradeVersion());
        assertEquals("6", md.getProjectVersion());
        assertEquals("9", md.getSupportedVersion());
    }

    protected File setupTestDirectory(String subfolder) {
        String classPath = getClass().getName().replace('.', '/');
        String location = "target/testrun/" + classPath + "/" + subfolder;
        File testDirectory = new File(location);

        // delete old tests
        if (testDirectory.exists()) {
            if (!FileUtil.delete(location, true)) {
                throw new CayenneRuntimeException(
                        "Error deleting test directory '%s'",
                        location);
            }
        }

        if (!testDirectory.mkdirs()) {
            throw new CayenneRuntimeException(
                    "Error creating test directory '%s'",
                    location);
        }

        return testDirectory;
    }

    @Test
    public void testPerformUpgradeFrom8() throws Exception {

        File testFolder = setupTestDirectory("testPerformUpgradeFrom8");
        String sourceUrl = getClass().getPackage().getName().replace('.', '/') + "/";

        List<String> sources = new ArrayList<>();

        sources.add("cayenne-PROJECT1.xml");
        sources.add("testProjectMap1_1.map.xml");
        sources.add("testProjectMap1_2.map.xml");
        sources.add("reverseEngineering.xml");

        // upgrades are done in-place, so copy it first
        List<File> targetsBefore = new ArrayList<>();
        for (String source : sources) {

            URL url = getClass().getClassLoader().getResource(sourceUrl + source);
            File target = new File(testFolder, source);
            assertNotNull(source);
            ResourceUtil.copyResourceToFile(url, target);
            targetsBefore.add(target);
        }

        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);
                binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
                binder.bind(ProjectSaver.class).to(FileProjectSaver.class);
                binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);
                binder.bind(DataMapLoader.class).to(XMLDataMapLoader.class);
            }
        };

        ProjectUpgrader_V9 upgrader = new ProjectUpgrader_V9();
        Injector injector = DIBootstrap.createInjector(testModule);
        injector.injectMembers(upgrader);

        Resource source = new URLResource(targetsBefore.get(0).toURI().toURL());
        UpgradeHandler handler = upgrader.getUpgradeHandler(source);

        Resource upgraded = handler.performUpgrade();
        assertNotNull(upgraded);
        assertNotSame(source, upgrader);

        // check that all the new files are created...
        String[] targetsAfterNames = new String[] {
                "cayenne-PROJECT1.xml", "testProjectMap1_1.map.xml", "testProjectMap1_2.map.xml"
        };

        File[] targetsAfter = new File[targetsAfterNames.length];
        for (int i = 0; i < targetsAfter.length; i++) {
            targetsAfter[i] = new File(testFolder, targetsAfterNames[i]);
            assertTrue("File was not created: " + targetsAfter[i].getAbsolutePath(), targetsAfter[i].exists());
        }

        // assert XML structure of the generated files
        assertPerformUpgradeFrom8_cayenne(targetsAfter[0]);
        assertPerformUpgradeFrom8_map1_1(targetsAfter[1]);
        assertPerformUpgradeFrom8_map1_2(targetsAfter[2]);
        assertFalse(new File(testFolder, "reverseEngineering.xml").exists());
    }

    private void assertPerformUpgradeFrom8_cayenne(File file) throws Exception {
        Document document = toDOMTree(file);

        XPath xpath = XPathFactory.newInstance().newXPath();
        assertEquals("", xpath.evaluate("/domain/@name", document));
        assertEquals("9", xpath.evaluate("/domain/@project-version", document));

        NodeList maps = (NodeList) xpath.evaluate("/domain/map", document, XPathConstants.NODESET);
        assertNotNull(maps);
        assertEquals(2, maps.getLength());

        Node map1 = maps.item(0);
        Node map2 = maps.item(1);

        assertEquals("testProjectMap1_1", xpath.evaluate("@name", map1));
        assertEquals("testProjectMap1_2", xpath.evaluate("@name", map2));

        NodeList nodes = (NodeList) xpath.evaluate("/domain/node", document, XPathConstants.NODESET);
        assertNotNull(nodes);
        assertEquals(1, nodes.getLength());
    }

    private void assertPerformUpgradeFrom8_map1_1(File file) throws Exception {
        Document document = toDOMTree(file);

        XPath xpath = XPathFactory.newInstance().newXPath();
        assertEquals("9", xpath.evaluate("/data-map/@project-version", document));

        NodeList reverseEngineeringNodes = (NodeList) xpath.evaluate("/data-map/reverse-engineering-config",
                document, XPathConstants.NODESET);
        assertEquals(0, reverseEngineeringNodes.getLength());
    }

    private void assertPerformUpgradeFrom8_map1_2(File file) throws Exception {
        Document document = toDOMTree(file);

        XPath xpath = XPathFactory.newInstance().newXPath();
        assertEquals("9", xpath.evaluate("/data-map/@project-version", document));
    }

}
