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
package org.apache.cayenne.projecttools;

import org.apache.cayenne.project.ConfigurationNameMapper;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.DefaultConfigurationNameMapper;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.map.DataMap;
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

        saver = new FileProjectSaver();
        Injector injector = DIBootstrap.createInjector(testModule);
        injector.injectMembers(saver);
    }

    @Test
    public void saveAs_Sorted() throws Exception {

        Project rootNode = new Project();
        rootNode.setName("test");

        // add maps in reverse alpha order. Check that they are saved in alpha order
        rootNode.getDataMaps().add(new DataMap("C"));
        rootNode.getDataMaps().add(new DataMap("B"));
        rootNode.getDataMaps().add(new DataMap("A"));

        saver.saveAs(rootNode, new URLResource(tempDir.toURI().toURL()));

        File target = new File(tempDir, "cayenne-test.xml");
        assertTrue(target.isFile());
        assertSaveAs_Sorted(target);
    }

    private void assertSaveAs_Sorted(File file) throws Exception {
        Document document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(file);

        XPath xpath = XPathFactory.newInstance().newXPath();
        assertEquals("", xpath.evaluate("/project/@name", document));

        NodeList maps = (NodeList) xpath.evaluate(
                "/project/map",
                document,
                XPathConstants.NODESET);
        assertEquals(3, maps.getLength());

        assertEquals("A", xpath.evaluate("@name", maps.item(0)));
        assertEquals("B", xpath.evaluate("@name", maps.item(1)));
        assertEquals("C", xpath.evaluate("@name", maps.item(2)));
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
        saver.save(testDataMap);

        File target = new File(tempDir, mapFileName + ".map.xml");
        assertTrue(target.isFile());
    }

}
