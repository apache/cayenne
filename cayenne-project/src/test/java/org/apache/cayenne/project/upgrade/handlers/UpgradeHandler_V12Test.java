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

package org.apache.cayenne.project.upgrade.handlers;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.project.upgrade.UpgradeUnit;
import org.apache.cayenne.resource.URLResource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * @since 5.0
 */
public class UpgradeHandler_V12Test extends BaseUpgradeHandlerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Override
    UpgradeHandler newHandler() {
        return new UpgradeHandler_V12();
    }

    @Test
    public void testProjectDomUpgrade() throws Exception {
        File projectFile = copyResourceToTemp("../v12/cayenne-project1.xml", "cayenne-project.xml");
        File graphFile = copyResourceToTemp("../v12/project1.graph.xml", "project1.graph.xml");
        assertTrue("precondition: graph file exists", graphFile.exists());

        Document document = processProjectDomFromFile(projectFile);

        Element root = document.getDocumentElement();
        assertEquals("12", root.getAttribute("project-version"));
        assertEquals("http://cayenne.apache.org/schema/12/domain", root.getAttribute("xmlns"));

        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList includes = (NodeList) xpath.evaluate("/domain/*[local-name()='include']",
                document, XPathConstants.NODESET);
        assertEquals("xi:include must be removed", 0, includes.getLength());

        assertFalse("graph file must be deleted", graphFile.exists());
    }

    @Test
    public void testProjectDomUpgradeNoGraphFile() throws Exception {
        File projectFile = copyResourceToTemp("../v12/cayenne-project1.xml", "cayenne-project.xml");

        // graph file intentionally absent — upgrade must complete without exception
        Document document = processProjectDomFromFile(projectFile);

        Element root = document.getDocumentElement();
        assertEquals("12", root.getAttribute("project-version"));

        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList includes = (NodeList) xpath.evaluate("/domain/*[local-name()='include']",
                document, XPathConstants.NODESET);
        assertEquals("xi:include must be removed", 0, includes.getLength());
    }

    @Test
    public void testDataMapDomUpgrade() throws Exception {
        Document document = processDataMapDom("../v12/map1.map.xml");

        Element root = document.getDocumentElement();
        assertEquals("12", root.getAttribute("project-version"));
        assertEquals("http://cayenne.apache.org/schema/12/modelMap", root.getAttribute("xmlns"));
    }

    @Test
    public void testModelUpgrade() {
        DataChannelDescriptor descriptor = mock(DataChannelDescriptor.class);
        handler.processModel(descriptor);
        verifyNoInteractions(descriptor);
    }

    private File copyResourceToTemp(String resourcePath, String targetName) throws Exception {
        File target = new File(tempFolder.getRoot(), targetName);
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            Files.copy(in, target.toPath());
        }
        return target;
    }

    private Document processProjectDomFromFile(File file) throws Exception {
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc;
        try (InputStream in = Files.newInputStream(file.toPath())) {
            doc = db.parse(in);
        }
        UpgradeUnit unit = new UpgradeUnit(new URLResource(file.toURI().toURL()), doc);
        handler.processProjectDom(unit);
        return doc;
    }
}
