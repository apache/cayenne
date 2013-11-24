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
package org.apache.cayenne.project.upgrade.v7;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.DefaultConfigurationNameMapper;
import org.apache.cayenne.configuration.XMLDataMapLoader;
import org.apache.cayenne.configuration.server.DBCPDataSourceFactory;
import org.apache.cayenne.configuration.server.JNDIDataSourceFactory;
import org.apache.cayenne.configuration.server.XMLPoolingDataSourceFactory;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
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
import org.apache.cayenne.test.resource.ResourceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ProjectUpgrader_V7Test extends Project2Case {

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

        ProjectUpgrader_V7 upgrader = new ProjectUpgrader_V7();
        Injector injector = DIBootstrap.createInjector(testModule);
        injector.injectMembers(upgrader);

        Resource source = new URLResource(url);
        UpgradeHandler handler = upgrader.getUpgradeHandler(source);

        assertNotNull(handler);
        assertSame(source, handler.getProjectSource());

        UpgradeMetaData md = handler.getUpgradeMetaData();
        assertNotNull(md);

        assertSame(UpgradeType.UPGRADE_NEEDED, md.getUpgradeType());
        // assertEquals("6", md.getIntermediateUpgradeVersion());
        assertNull(md.getIntermediateUpgradeVersion());
        assertEquals("3.0.0.1", md.getProjectVersion());
        assertEquals("7", md.getSupportedVersion());
    }

    public void testMetadata_Type2_0() {
        String baseUrl = getClass().getPackage().getName().replace('.', '/');
        URL url = getClass().getClassLoader().getResource(baseUrl + "/2_0a/cayenne.xml");
        assertNotNull(url);

        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(ProjectSaver.class).to(FileProjectSaver.class);
                binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);
            }
        };

        ProjectUpgrader_V7 upgrader = new ProjectUpgrader_V7();
        Injector injector = DIBootstrap.createInjector(testModule);
        injector.injectMembers(upgrader);

        Resource source = new URLResource(url);
        UpgradeHandler handler = upgrader.getUpgradeHandler(source);

        assertNotNull(handler);
        assertSame(source, handler.getProjectSource());

        UpgradeMetaData md = handler.getUpgradeMetaData();
        assertNotNull(md);
        assertSame(UpgradeType.INTERMEDIATE_UPGRADE_NEEDED, md.getUpgradeType());
        assertEquals("3.0.0.1", md.getIntermediateUpgradeVersion());
        assertEquals("2.0", md.getProjectVersion());
        assertEquals("7", md.getSupportedVersion());
    }

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

        ProjectUpgrader_V7 upgrader = new ProjectUpgrader_V7();
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
        assertEquals("7", md.getSupportedVersion());
    }

    public void testMetadata_Type7() {
        String baseUrl = getClass().getPackage().getName().replace('.', '/');
        URL url = getClass().getClassLoader().getResource(baseUrl + "/7a/cayenne-PROJECT1.xml");
        assertNotNull(url);

        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(ProjectSaver.class).to(FileProjectSaver.class);
                binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);
            }
        };

        ProjectUpgrader_V7 upgrader = new ProjectUpgrader_V7();
        Injector injector = DIBootstrap.createInjector(testModule);
        injector.injectMembers(upgrader);

        Resource source = new URLResource(url);
        UpgradeHandler handler = upgrader.getUpgradeHandler(source);

        assertNotNull(handler);
        assertSame(source, handler.getProjectSource());

        UpgradeMetaData md = handler.getUpgradeMetaData();
        assertNotNull(md);
        assertSame(UpgradeType.UPGRADE_NOT_NEEDED, md.getUpgradeType());
        assertNull(md.getIntermediateUpgradeVersion());
        assertEquals("7", md.getProjectVersion());
        assertEquals("7", md.getSupportedVersion());
    }

    public void testPerformUpgradeFrom3() throws Exception {

        System.out.println("TEST3");
        File testFolder = setupTestDirectory("testPerformUpgrade_3_0_0_1");
        String sourceUrl = getClass().getPackage().getName().replace('.', '/') + "/3_0_0_1a/";

        List<String> sources = new ArrayList<String>();

        sources.add("cayenne.xml");
        sources.add("d1Map1.map.xml");
        sources.add("d1Map2.map.xml");
        sources.add("d1NodeDriver.driver.xml");

        // upgrades are done in-place, so copy it first
        List<File> targetsBefore = new ArrayList<File>();
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

        ProjectUpgrader_V7 upgrader = new ProjectUpgrader_V7();
        Injector injector = DIBootstrap.createInjector(testModule);
        injector.injectMembers(upgrader);

        Resource source = new URLResource(targetsBefore.get(0).toURL());
        UpgradeHandler handler = upgrader.getUpgradeHandler(source);

        Resource upgraded = handler.performUpgrade();
        assertNotNull(upgraded);
        assertNotSame(source, upgrader);

        // check that all the new files are created...
        String[] targetsAfterNames = new String[] { "cayenne-d1.xml", "cayenne-d2.xml", "d1Map1.map.xml",
                "d1Map2.map.xml" };

        File[] targetsAfter = new File[targetsAfterNames.length];
        for (int i = 0; i < targetsAfter.length; i++) {
            targetsAfter[i] = new File(testFolder, targetsAfterNames[i]);
            assertTrue("File was not created: " + targetsAfter[i].getAbsolutePath(), targetsAfter[i].exists());
        }

        // DataMap files should remain the same; all others need to be deleted
        for (File file : targetsBefore) {
            if (file.getName().endsWith(".map.xml")) {
                assertTrue("DataMap file disappeared: " + file.getAbsolutePath(), file.exists());
            } else {
                assertFalse("File expected to be deleted: " + file.getAbsolutePath(), file.exists());
            }
        }

        // assert XML structure of the generated files
        assertPerformUpgrade_3_0_0_1_cayenne_d1(targetsAfter[0]);
        assertPerformUpgrade_3_0_0_1_cayenne_d2(targetsAfter[1]);
        assertPerformUpgrade_3_0_0_1_d1Map1(targetsAfter[2]);
        assertPerformUpgrade_3_0_0_1_d1Map2(targetsAfter[3]);
    }

    private void assertPerformUpgrade_3_0_0_1_cayenne_d1(File file) throws Exception {
        Document document = toDOMTree(file);

        XPath xpath = XPathFactory.newInstance().newXPath();
        assertEquals("", xpath.evaluate("/domain/@name", document));
        assertEquals("7", xpath.evaluate("/domain/@project-version", document));

        NodeList maps = (NodeList) xpath.evaluate("/domain/map", document, XPathConstants.NODESET);
        assertNotNull(maps);
        assertEquals(2, maps.getLength());

        Node map1 = maps.item(0);
        Node map2 = maps.item(1);

        assertEquals("d1Map1", xpath.evaluate("@name", map1));
        assertEquals("d1Map2", xpath.evaluate("@name", map2));

        NodeList nodes = (NodeList) xpath.evaluate("/domain/node", document, XPathConstants.NODESET);
        assertNotNull(nodes);
        assertEquals(1, nodes.getLength());

        Node node1 = nodes.item(0);

        assertEquals("d1NodeDriver", xpath.evaluate("@name", node1));
        assertEquals(XMLPoolingDataSourceFactory.class.getName(), xpath.evaluate("@factory", node1));

        NodeList mapRefs = (NodeList) xpath.evaluate("map-ref", node1, XPathConstants.NODESET);
        assertNotNull(mapRefs);
        assertEquals(2, mapRefs.getLength());

        assertEquals("d1Map1", xpath.evaluate("@name", mapRefs.item(0)));
        assertEquals("d1Map2", xpath.evaluate("@name", mapRefs.item(1)));

        NodeList dataSources = (NodeList) xpath.evaluate("data-source", node1, XPathConstants.NODESET);
        assertNotNull(dataSources);
        assertEquals(1, dataSources.getLength());

        Node ds = dataSources.item(0);
        assertEquals("org.hsqldb.jdbcDriver", xpath.evaluate("driver/@value", ds));
        assertEquals("jdbc:hsqldb:mem:xdb", xpath.evaluate("url/@value", ds));
    }

    private void assertPerformUpgrade_3_0_0_1_cayenne_d2(File file) throws Exception {
        Document document = toDOMTree(file);

        XPath xpath = XPathFactory.newInstance().newXPath();
        assertEquals("", xpath.evaluate("/domain/@name", document));
        assertEquals("7", xpath.evaluate("/domain/@project-version", document));

        NodeList maps = (NodeList) xpath.evaluate("/domain/map", document, XPathConstants.NODESET);
        assertNotNull(maps);
        assertEquals(0, maps.getLength());

        NodeList nodes = (NodeList) xpath.evaluate("/domain/node", document, XPathConstants.NODESET);
        assertNotNull(nodes);
        assertEquals(2, nodes.getLength());

        Node node1 = nodes.item(0);
        Node node2 = nodes.item(1);

        assertEquals("d2NodeDBCP", xpath.evaluate("@name", node1));
        assertEquals("dbcpx", xpath.evaluate("@parameters", node1));
        assertEquals(DBCPDataSourceFactory.class.getName(), xpath.evaluate("@factory", node1));

        NodeList dataSources1 = (NodeList) xpath.evaluate("data-source", node1, XPathConstants.NODESET);
        assertNotNull(dataSources1);
        assertEquals(0, dataSources1.getLength());

        assertEquals("d2NodeJNDI", xpath.evaluate("@name", node2));
        assertEquals("jndi/x", xpath.evaluate("@parameters", node2));
        assertEquals(JNDIDataSourceFactory.class.getName(), xpath.evaluate("@factory", node2));

        NodeList dataSources2 = (NodeList) xpath.evaluate("data-source", node2, XPathConstants.NODESET);
        assertNotNull(dataSources2);
        assertEquals(0, dataSources2.getLength());
    }

    private void assertPerformUpgrade_3_0_0_1_d1Map1(File file) throws Exception {
        Document document = toDOMTree(file);

        XPath xpath = XPathFactory.newInstance().newXPath();
        assertEquals("7", xpath.evaluate("/data-map/@project-version", document));
    }

    private void assertPerformUpgrade_3_0_0_1_d1Map2(File file) throws Exception {
        Document document = toDOMTree(file);

        XPath xpath = XPathFactory.newInstance().newXPath();
        assertEquals("7", xpath.evaluate("/data-map/@project-version", document));
    }

    public void testPerformUpgradeFrom6() throws Exception {
        File testForlder = setupTestDirectory("testUpgrade6a");
        String sourceUrl = getClass().getPackage().getName().replace('.', '/') + "/6a/";
        System.out.println(sourceUrl);
        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);
                binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);

                binder.bind(ProjectSaver.class).to(FileProjectSaver.class);
                binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);
                binder.bind(DataMapLoader.class).to(XMLDataMapLoader.class);
            }
        };

        String[] resources = { "cayenne-PROJECT1.xml", "testProjectMap1_1.map.xml", "testProjectMap1_2.map.xml" };
        List<File> files = new ArrayList<File>();

        for (String name : resources) {
            URL xmlUrl = getClass().getClassLoader().getResource(sourceUrl + name);
            File target = new File(testForlder, name);
            ResourceUtil.copyResourceToFile(xmlUrl, target);
            files.add(target);
        }

        Injector injector = DIBootstrap.createInjector(testModule);
        ProjectUpgrader_V7 upgrader = new ProjectUpgrader_V7();
        injector.injectMembers(upgrader);

        Resource source = new URLResource(files.get(0).toURL());
        assertNotNull(source);
        UpgradeHandler handler = upgrader.getUpgradeHandler(source);
        assertNotNull(handler);

        Resource upgraded = handler.performUpgrade();
        assertNotNull(upgraded);
        assertNotSame(source, upgraded);

        assertPerformUpgrade6Cayenne(files.get(0));
        assertPerformUpgrade6Map1(files.get(1));
        assertPerformUpgradeMap2(files.get(2));
    }

    private void assertPerformUpgrade6Map1(File file) throws Exception {
        Document document = toDOMTree(file);

        XPath xpath = XPathFactory.newInstance().newXPath();
        assertEquals("7", xpath.evaluate("/data-map/@project-version", document));

        NodeList maps = (NodeList) xpath.evaluate("/data-map/obj-entity/entity-listener", document,
                XPathConstants.NODESET);
        assertNotNull(maps);
        assertEquals(0, maps.getLength());
    }

    private void assertPerformUpgrade6Cayenne(File file) throws Exception {
        Document document = toDOMTree(file);

        XPath xpath = XPathFactory.newInstance().newXPath();
        assertEquals("7", xpath.evaluate("/domain/@project-version", document));
    }

    private void assertPerformUpgradeMap2(File file) throws Exception {
        Document document = toDOMTree(file);

        XPath xpath = XPathFactory.newInstance().newXPath();
        assertEquals("7", xpath.evaluate("/data-map/@project-version", document));
    }

}
