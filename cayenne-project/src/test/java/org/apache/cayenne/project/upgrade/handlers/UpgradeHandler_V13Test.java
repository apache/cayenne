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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

public class UpgradeHandler_V13Test extends BaseUpgradeHandlerTest {

    @Override
    UpgradeHandler newHandler() {
        return new UpgradeHandler_V13();
    }

    @Test
    public void projectDomUpgrade() throws Exception {
        String resource = "../v13/cayenne-project1.xml";
        UpgradeContext unit = new UpgradeContext(new URLResource(getClass().getResource(resource)),
                documentFromResource(resource));
        handler.processProjectDom(unit);
        Document document = unit.getDocument();

        Element root = document.getDocumentElement();
        assertEquals("13", root.getAttribute("project-version"));
        assertEquals("http://cayenne.apache.org/schema/13/domain", root.getAttribute("xmlns"));

        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList) xpath.evaluate("/domain/*[local-name()='node']",
                document, XPathConstants.NODESET);
        assertEquals(0, nodes.getLength(), "<node> elements must be removed");

        NodeList maps = (NodeList) xpath.evaluate("/domain/*[local-name()='map']",
                document, XPathConstants.NODESET);
        assertEquals(1, maps.getLength(), "<map> elements must be preserved");

        NodeList validation = (NodeList) xpath.evaluate("/domain/*[local-name()='validation']",
                document, XPathConstants.NODESET);
        assertEquals(1, validation.getLength());
        assertEquals("http://cayenne.apache.org/schema/13/validation",
                ((Element) validation.item(0)).getAttribute("xmlns"));

        NodeList excludes = (NodeList) xpath.evaluate("/domain/*[local-name()='validation']/*[local-name()='exclude']",
                document, XPathConstants.NODESET);
        List<String> excludeNames = new ArrayList<>();
        for (int i = 0; i < excludes.getLength(); i++) {
            excludeNames.add(excludes.item(i).getTextContent().trim());
        }
        assertEquals(List.of("DATA_CHANNEL_NO_NAME"), excludeNames,
                "DATA_NODE_* and DATA_MAP_NODE_LINKAGE excludes must be removed, others preserved");

        assertEquals(2, unit.getPostUpgradeMessages().size(), "one post-upgrade message per removed node");
        assertTrue(unit.getPostUpgradeMessages().get(0).contains("'node1'"));
        assertTrue(unit.getPostUpgradeMessages().get(1).contains("'node2'"));
    }

    @Test
    public void dataMapDomUpgrade() throws Exception {
        Document document = processDataMapDom("../v13/map1.map.xml");

        Element root = document.getDocumentElement();
        assertEquals("13", root.getAttribute("project-version"));
        assertEquals("http://cayenne.apache.org/schema/13/modelMap", root.getAttribute("xmlns"));

        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList cgen = (NodeList) xpath.evaluate("/data-map/*[local-name()='cgen']",
                document, XPathConstants.NODESET);
        assertEquals(1, cgen.getLength());
        assertEquals("http://cayenne.apache.org/schema/13/cgen",
                ((Element) cgen.item(0)).getAttribute("xmlns"));

        NodeList dbImport = (NodeList) xpath.evaluate("/data-map/*[local-name()='dbImport']",
                document, XPathConstants.NODESET);
        assertEquals(1, dbImport.getLength());
        assertEquals("http://cayenne.apache.org/schema/13/dbimport",
                ((Element) dbImport.item(0)).getAttribute("xmlns"));

        NodeList properties = (NodeList) xpath.evaluate("//*[local-name()='property']",
                document, XPathConstants.NODESET);
        int infoComments = 0;
        for (int i = 0; i < properties.getLength(); i++) {
            Element property = (Element) properties.item(i);
            if (property.hasAttribute("xmlns:info")) {
                infoComments++;
                assertEquals("http://cayenne.apache.org/schema/13/info", property.getAttribute("xmlns:info"));
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
}
