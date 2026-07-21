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
import org.apache.cayenne.project.upgrade.UpgradeContext;
import org.apache.cayenne.resource.URLResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

public class UpgradeHandler_V12Test extends BaseUpgradeHandlerTest {

    @TempDir
    public File tempFolder;

    @Override
    UpgradeHandler newHandler() {
        return new UpgradeHandler_V12();
    }

    @Test
    public void projectDomUpgrade() throws Exception {
        File projectFile = copyResourceToTemp("../v12/cayenne-project1.xml", "cayenne-project.xml");
        File graphFile = copyResourceToTemp("../v12/project1.graph.xml", "project1.graph.xml");
        assertTrue(graphFile.exists(), "precondition: graph file exists");

        UpgradeContext unit = processProjectDomFromFile(projectFile);
        Document document = unit.getDocument();

        Element root = document.getDocumentElement();
        assertEquals("12", root.getAttribute("project-version"));
        assertEquals("http://cayenne.apache.org/schema/12/domain", root.getAttribute("xmlns"));

        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList includes = (NodeList) xpath.evaluate("/domain/*[local-name()='include']",
                document, XPathConstants.NODESET);
        assertEquals(0, includes.getLength(), "xi:include must be removed");

        NodeList validation = (NodeList) xpath.evaluate("/domain/*[local-name()='validation']",
                document, XPathConstants.NODESET);
        assertEquals(1, validation.getLength());
        assertEquals("http://cayenne.apache.org/schema/12/validation",
                ((Element) validation.item(0)).getAttribute("xmlns"));

        assertFalse(graphFile.exists(), "graph file must be deleted");

        assertEquals(1, unit.getPostUpgradeMessages().size());
        assertEquals("The 'graph' diagram layout is no longer supported and was deleted from the project", unit.getPostUpgradeMessages().getFirst());
    }

    @Test
    public void projectDomUpgradeNoGraphFile() throws Exception {
        File projectFile = copyResourceToTemp("../v12/cayenne-project1.xml", "cayenne-project.xml");

        // graph file intentionally absent — upgrade must complete without exception
        Document document = processProjectDomFromFile(projectFile).getDocument();

        Element root = document.getDocumentElement();
        assertEquals("12", root.getAttribute("project-version"));

        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList includes = (NodeList) xpath.evaluate("/domain/*[local-name()='include']",
                document, XPathConstants.NODESET);
        assertEquals(0, includes.getLength(), "xi:include must be removed");
    }

    @Test
    public void dataMapDomUpgrade() throws Exception {
        Document document = processDataMapDom("../v12/map1.map.xml");

        Element root = document.getDocumentElement();
        assertEquals("12", root.getAttribute("project-version"));
        assertEquals("http://cayenne.apache.org/schema/12/modelMap", root.getAttribute("xmlns"));

        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList cgen = (NodeList) xpath.evaluate("/data-map/*[local-name()='cgen']",
                document, XPathConstants.NODESET);
        assertEquals(1, cgen.getLength());
        assertEquals("http://cayenne.apache.org/schema/12/cgen",
                ((Element) cgen.item(0)).getAttribute("xmlns"));

        NodeList dbImport = (NodeList) xpath.evaluate("/data-map/*[local-name()='dbImport']",
                document, XPathConstants.NODESET);
        assertEquals(1, dbImport.getLength());
        assertEquals("http://cayenne.apache.org/schema/12/dbimport",
                ((Element) dbImport.item(0)).getAttribute("xmlns"));

        NodeList properties = (NodeList) xpath.evaluate("//*[local-name()='property']",
                document, XPathConstants.NODESET);
        int infoComments = 0;
        for (int i = 0; i < properties.getLength(); i++) {
            Element property = (Element) properties.item(i);
            if (property.hasAttribute("xmlns:info")) {
                infoComments++;
                assertEquals("http://cayenne.apache.org/schema/12/info", property.getAttribute("xmlns:info"));
            }
        }
        assertEquals(1, infoComments, "info:property comment must be preserved and namespace-bumped");
    }

    @Test
    public void modelUpgrade() {
        DataChannelDescriptor descriptor = mock(DataChannelDescriptor.class);
        handler.processModel(descriptor);
        verifyNoInteractions(descriptor);
    }

    private File copyResourceToTemp(String resourcePath, String targetName) throws Exception {
        File target = new File(tempFolder, targetName);
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            Files.copy(in, target.toPath());
        }
        return target;
    }

    private UpgradeContext processProjectDomFromFile(File file) throws Exception {
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc;
        try (InputStream in = Files.newInputStream(file.toPath())) {
            doc = db.parse(in);
        }
        UpgradeContext unit = new UpgradeContext(new URLResource(file.toURI().toURL()), doc);
        handler.processProjectDom(unit);
        return unit;
    }
}
