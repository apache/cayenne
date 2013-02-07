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
package org.apache.cayenne.project;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.DefaultConfigurationNameMapper;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.FileProjectSaver;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.unit.Project2Case;
import org.apache.cayenne.resource.URLResource;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class FileProjectSaverTest extends Project2Case {

    private FileProjectSaver saver;

    @Override
    public void setUp() throws Exception {
        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(ConfigurationNameMapper.class).to(
                        DefaultConfigurationNameMapper.class);
            }
        };

        saver = new FileProjectSaver();
        Injector injector = DIBootstrap.createInjector(testModule);
        injector.injectMembers(saver);
    }

    public void testSaveAs_Sorted() throws Exception {

        File testFolder = setupTestDirectory("testSaveAs_Sorted");

        DataChannelDescriptor rootNode = new DataChannelDescriptor();
        rootNode.setName("test");

        // add maps and nodes in reverse alpha order. Check that they are saved in alpha
        // order
        rootNode.getDataMaps().add(new DataMap("C"));
        rootNode.getDataMaps().add(new DataMap("B"));
        rootNode.getDataMaps().add(new DataMap("A"));

        DataNodeDescriptor[] nodes = new DataNodeDescriptor[3];
        nodes[0] = new DataNodeDescriptor("Z");
        nodes[1] = new DataNodeDescriptor("Y");
        nodes[2] = new DataNodeDescriptor("X");

        nodes[0].getDataMapNames().add("C");
        nodes[0].getDataMapNames().add("B");
        nodes[0].getDataMapNames().add("A");

        rootNode.getNodeDescriptors().addAll(Arrays.asList(nodes));

        Project project = new Project(new ConfigurationTree<DataChannelDescriptor>(
                rootNode));

        saver.saveAs(project, new URLResource(testFolder.toURL()));

        File target = new File(testFolder, "cayenne-test.xml");
        assertTrue(target.isFile());
        assertSaveAs_Sorted(target);
    }

    private void assertSaveAs_Sorted(File file) throws Exception {
        Document document = toDOMTree(file);

        XPath xpath = XPathFactory.newInstance().newXPath();
        assertEquals("", xpath.evaluate("/domain/@name", document));

        NodeList maps = (NodeList) xpath.evaluate(
                "/domain/map",
                document,
                XPathConstants.NODESET);
        assertEquals(3, maps.getLength());

        assertEquals("A", xpath.evaluate("@name", maps.item(0)));
        assertEquals("B", xpath.evaluate("@name", maps.item(1)));
        assertEquals("C", xpath.evaluate("@name", maps.item(2)));

        NodeList nodes = (NodeList) xpath.evaluate(
                "/domain/node",
                document,
                XPathConstants.NODESET);
        assertEquals(3, nodes.getLength());

        assertEquals("X", xpath.evaluate("@name", nodes.item(0)));
        assertEquals("Y", xpath.evaluate("@name", nodes.item(1)));
        assertEquals("Z", xpath.evaluate("@name", nodes.item(2)));

        NodeList mapRefs = (NodeList) xpath.evaluate(
                "map-ref",
                nodes.item(2),
                XPathConstants.NODESET);
        assertEquals(3, mapRefs.getLength());

        assertEquals("A", xpath.evaluate("@name", mapRefs.item(0)));
        assertEquals("B", xpath.evaluate("@name", mapRefs.item(1)));
        assertEquals("C", xpath.evaluate("@name", mapRefs.item(2)));

    }

    /**
     * Method test fix for CAY-1780. If specify related fragments (for example ./../)
     * in target file path then file must be created successfully.
     * @throws Exception
     */
    public void testSaveForProjectFileWithRelatedPaths() throws Exception {
        File testFolder = setupTestDirectory("testSaveForProjectFileWithRelatedPaths");

        String mapFilePath = testFolder.toURI() + "../test.map.xml";
        String mapFileName = "test";
        DataMap testDataMap = new DataMap(mapFileName);
        testDataMap.setConfigurationSource(new URLResource(new URL(mapFilePath)));
        Project project = new Project(new ConfigurationTree<DataMap>(testDataMap));

        saver.save(project);

        File target = new File(testFolder.getParentFile(), mapFileName + ".map.xml");
        assertTrue(target.isFile());
    }

}
