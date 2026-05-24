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
package org.apache.cayenne.project;

import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.DefaultConfigurationNameMapper;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.extension.BaseSaverDelegate;
import org.apache.cayenne.project.extension.LoaderDelegate;
import org.apache.cayenne.project.extension.ProjectExtension;
import org.apache.cayenne.project.extension.SaverDelegate;
import org.apache.cayenne.resource.URLResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileProjectSaverTest {

    @TempDir
    public File tempDir;

    private FileProjectSaver saver;

    @BeforeEach
    public void setUp() throws Exception {
        Module testModule = binder -> binder
                .bind(ConfigurationNameMapper.class)
                .to(DefaultConfigurationNameMapper.class);

        saver = new FileProjectSaver(Collections.<ProjectExtension>emptyList());
        Injector injector = DIBootstrap.createInjector(testModule);
        injector.injectMembers(saver);
    }

    @Test
    public void saveAs_Sorted() throws Exception {

        DataChannelDescriptor rootNode = new DataChannelDescriptor();
        rootNode.setName("test");

        // add maps and nodes in reverse alpha order. Check that they are saved in alpha order
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

        Project project = new Project(new ConfigurationTree<DataChannelDescriptor>(rootNode));

        saver.saveAs(project, new URLResource(tempDir.toURI().toURL()));

        File target = new File(tempDir, "cayenne-test.xml");
        assertTrue(target.isFile());
        assertSaveAs_Sorted(target);
    }

    private void assertSaveAs_Sorted(File file) throws Exception {
        Document document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(file);

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

    @Test
    public void extensionsAreSortedByOrder() {
        ProjectExtension ext10 = stubExtension(10);
        ProjectExtension ext20 = stubExtension(20);
        ProjectExtension ext30 = stubExtension(30);

        // supply in reverse order — saver must still store them sorted ascending
        FileProjectSaver s1 = new FileProjectSaver(List.of(ext30, ext20, ext10));
        assertEquals(List.of(ext10, ext20, ext30), s1.extensions);

        // supply in random order
        FileProjectSaver s2 = new FileProjectSaver(List.of(ext20, ext10, ext30));
        assertEquals(List.of(ext10, ext20, ext30), s2.extensions);
    }

    private static ProjectExtension stubExtension(int order) {
        return new ProjectExtension() {
            @Override public LoaderDelegate createLoaderDelegate() { return null; }
            @Override public SaverDelegate createSaverDelegate() { return new BaseSaverDelegate(); }
            @Override public ConfigurationNodeVisitor<String> createNamingDelegate() { return null; }
            @Override public int order() { return order; }
        };
    }

    /**
     * Regression test for CAY-1780: relative fragments (e.g. ./../) in target
     * file path must be resolved correctly.
     */
    @Test
    public void saveForProjectFileWithRelatedPaths() throws Exception {
        File subDir = new File(tempDir, "sub");
        subDir.mkdirs();

        String mapFileName = "test";
        String mapFilePath = subDir.toURI() + "../" + mapFileName + ".map.xml";
        DataMap testDataMap = new DataMap(mapFileName);
        testDataMap.setConfigurationSource(new URLResource(new URL(mapFilePath)));
        Project project = new Project(new ConfigurationTree<DataMap>(testDataMap));

        saver.save(project);

        File target = new File(tempDir, mapFileName + ".map.xml");
        assertTrue(target.isFile());
    }

}
