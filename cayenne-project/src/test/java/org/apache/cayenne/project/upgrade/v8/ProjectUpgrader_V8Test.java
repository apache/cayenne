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
package org.apache.cayenne.project.upgrade.v8;

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
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.test.file.FileUtil;
import org.apache.cayenne.test.resource.ResourceUtil;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ProjectUpgrader_V8Test extends Project2Case {

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
    public void testPerformUpgrade() throws Exception {

        File testFolder = setupTestDirectory("testPerformUpgrade_7");
        String sourceUrl = getClass().getPackage().getName().replace('.', '/') + "/7a/";

        List<String> sources = new ArrayList<>();

        sources.add("cayenne-PROJECT1.xml");
        sources.add("testProjectMap1_1.map.xml");
        sources.add("testProjectMap1_2.map.xml");

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

        ProjectUpgrader_V8 upgrader = new ProjectUpgrader_V8();
        Injector injector = DIBootstrap.createInjector(testModule);
        injector.injectMembers(upgrader);

        Resource source = new URLResource(targetsBefore.get(0).toURL());
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
        assertPerformUpgrade_7_cayenne(targetsAfter[0]);
        assertPerformUpgrade_map1_1(targetsAfter[1]);
        assertPerformUpgrade_map1_2(targetsAfter[2]);
    }

    private void assertPerformUpgrade_7_cayenne(File file) throws Exception {
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

    private void assertPerformUpgrade_map1_1(File file) throws Exception {
        Document document = toDOMTree(file);

        XPath xpath = XPathFactory.newInstance().newXPath();
        assertEquals("9", xpath.evaluate("/data-map/@project-version", document));

        NodeList queryNodes = (NodeList) xpath.evaluate("/data-map/query", document, XPathConstants.NODESET);
        assertNotNull(queryNodes);

        Map<String, Element> queries = new HashMap<>();

        for (int i = 0; i < queryNodes.getLength(); i++) {
            Element query = (Element) queryNodes.item(i);
            queries.put(query.getAttribute("name"), query);
        }

        assertEquals("", queries.get("EjbqlQueryTest").getAttribute("factory"));
        assertEquals("", queries.get("SQLTemplateTest").getAttribute("factory"));
        assertEquals("", queries.get("SelectQueryTest").getAttribute("factory"));
        assertEquals("", queries.get("ProcedureQueryTest").getAttribute("factory"));

        assertEquals("EJBQLQuery", queries.get("EjbqlQueryTest").getAttribute("type"));
        assertEquals("SQLTemplate", queries.get("SQLTemplateTest").getAttribute("type"));
        assertEquals("SelectQuery", queries.get("SelectQueryTest").getAttribute("type"));
        assertEquals("ProcedureQuery", queries.get("ProcedureQueryTest").getAttribute("type"));
    }

    private void assertPerformUpgrade_map1_2(File file) throws Exception {
        Document document = toDOMTree(file);

        XPath xpath = XPathFactory.newInstance().newXPath();
        assertEquals("9", xpath.evaluate("/data-map/@project-version", document));
    }
}
