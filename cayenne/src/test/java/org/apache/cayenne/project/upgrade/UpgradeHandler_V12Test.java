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

package org.apache.cayenne.project.upgrade;

import org.apache.cayenne.project.Project;
import org.apache.cayenne.resource.URLResource;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

public class UpgradeHandler_V12Test extends BaseUpgradeHandlerTest {

    @Override
    UpgradeHandler newHandler() {
        return new UpgradeHandler_V12();
    }

    @Test
    public void projectDomUpgrade() throws Exception {
        String resource = "v12/cayenne-project1.xml";
        UpgradeContext unit = new UpgradeContext(new URLResource(getClass().getResource(resource)),
                documentFromResource(resource));
        handler.processProjectDom(unit);
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

        // the graph file is reported as obsolete, not deleted, by the handler
        assertEquals(List.of("project1.graph.xml"), unit.getObsoleteFiles());
        assertEquals(1, unit.getChangeNotifications().size());
        assertEquals("The 'graph' diagram layout is no longer supported and was removed from the project",
                unit.getChangeNotifications().getFirst());
        assertTrue(unit.getChangesAffectingRuntime().isEmpty(), "dropping the graph layout is not destructive");
    }

    @Test
    public void dataMapDomUpgrade() throws Exception {
        Document document = processDataMapDom("v12/map1.map.xml");

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
        Project project = mock(Project.class);
        handler.processModel(project);
        verifyNoInteractions(project);
    }


}
