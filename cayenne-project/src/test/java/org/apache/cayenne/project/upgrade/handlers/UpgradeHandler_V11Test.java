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
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * @since 5.0
 */
public class UpgradeHandler_V11Test extends BaseUpgradeHandlerTest {

    UpgradeHandler newHandler() {
        return new UpgradeHandler_V11();
    }

    @Test
    public void testProjectDomUpgrade() throws Exception {
        Document document = processProjectDom("cayenne-project-v10.xml");

        Element root = document.getDocumentElement();
        assertEquals("11", root.getAttribute("project-version"));
        assertEquals("http://cayenne.apache.org/schema/11/domain", root.getAttribute("xmlns"));
        assertEquals(2, root.getElementsByTagName("map").getLength());

        NodeList nodes = document.getElementsByTagName("node");
        assertEquals(1, nodes.getLength());

        Element node = (Element)nodes.item(0);
        assertEquals("org.apache.cayenne.configuration.runtime.XMLPoolingDataSourceFactory", node.getAttribute("factory"));
    }

    @Test
    public void testDataMapDomUpgrade() throws Exception {
        Document document = processDataMapDom("test-map-v10.map.xml");

        Element root = document.getDocumentElement();
        assertEquals("11", root.getAttribute("project-version"));
        assertEquals("http://cayenne.apache.org/schema/11/modelMap", root.getAttribute("xmlns"));

        NodeList properties = root.getElementsByTagName("property");
        assertEquals(1, properties.getLength());
        assertEquals("defaultPackage", properties.item(0).getAttributes().getNamedItem("name").getNodeValue());

        NodeList objEntities = root.getElementsByTagName("obj-entity");
        assertEquals(1, objEntities.getLength());
        Node objEntity = objEntities.item(0);
        NamedNodeMap attributes = objEntity.getAttributes();
        assertEquals(2, attributes.getLength());
        assertEquals("Artist", attributes.getNamedItem("name").getNodeValue());
        assertEquals("Artist", attributes.getNamedItem("dbEntityName").getNodeValue());
        assertEquals(3, objEntity.getChildNodes().getLength());
        assertEquals("http://cayenne.apache.org/schema/11/info",
                     objEntity.getFirstChild().getNextSibling().getAttributes().getNamedItem("xmlns:info")
                             .getNodeValue());

        assertEquals(2, root.getElementsByTagName("db-attribute").getLength());
    }

    @Test
    public void testCgenDomUpgrade() throws Exception {
        Document document = processDataMapDom("test-map-v10.map.xml");
        Element root = document.getDocumentElement();

        // check cgen config is updated
        NodeList cgens = root.getElementsByTagName("cgen");
        assertEquals(1, cgens.getLength());
        Node cgenConfig = cgens.item(0);
        assertEquals("http://cayenne.apache.org/schema/11/cgen",
                     cgenConfig.getAttributes().getNamedItem("xmlns").getNodeValue());

        NodeList childNodes = cgenConfig.getChildNodes();
        boolean dataMapTemplateSeen = false;
        boolean dataMapSuperTemplateSeen = false;
        int elements = 0;
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                elements++;
                switch (node.getNodeName()) {
                    case "client":
                        fail("<client> tag is still present in the <cgen> config");
                    case "queryTemplate":
                        fail("<queryTemplate> tag is still present in the <cgen> config");
                    case "querySuperTemplate":
                        fail("<querySuperTemplate> tag is still present in the <cgen> config");
                    case "dataMapTemplate":
                        dataMapTemplateSeen = true;
                        break;
                    case "dataMapSuperTemplate":
                        dataMapSuperTemplateSeen = true;
                        break;
                }
            }
        }
        assertEquals(8, elements);
        assertTrue(dataMapTemplateSeen);
        assertTrue(dataMapSuperTemplateSeen);

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                switch (node.getNodeName()) {
                    case "template":
                    case "embeddableTemplate":
                    case "dataMapTemplate":
                        assertEquals(TEST_TEMPLATE_CONTENT, node.getFirstChild().getNodeValue());
                        break;
                    case "superTemplate":
                        assertEquals("The template /org/apache/cayenne/project/upgrade/handlers was not found " +
                                     "during the project upgrade. Use the template editor in Cayenne modeler to set the template",
                                     node.getFirstChild().getNodeValue());
                        break;
                    case "embeddableSuperTemplate":
                        assertEquals("The template ../../testWrongPath was not found during the project upgrade." +
                                     " Use the template editor in Cayenne modeler to set the template",
                                     node.getFirstChild().getNodeValue());
                        break;
                    case "dataMapSuperTemplate":
                        assertEquals("templates/v4_1/datamap-superclass.vm", node.getFirstChild().getNodeValue());
                        break;
                }
            }
        }
    }

    @Test
    public void testDbImportDomUpgrade() throws Exception {
        Document document = processDataMapDom("test-map-v10.map.xml");
        Element root = document.getDocumentElement();

        // check cgen config is updated
        NodeList dbimport = root.getElementsByTagName("dbImport");
        assertEquals(1, dbimport.getLength());
        Node dbimportConfig = dbimport.item(0);
        assertEquals("http://cayenne.apache.org/schema/11/dbimport",
                dbimportConfig.getAttributes().getNamedItem("xmlns").getNodeValue());

        NodeList childNodes = dbimportConfig.getChildNodes();
        int elements = 0;
        boolean defaultPackageSeen = false;
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                elements++;
                switch (node.getNodeName()) {
                    case "usePrimitives":
                        fail("<usePrimitives> tag is still present in the <dbimport> config");
                    case "defaultPackage":
                        defaultPackageSeen = true;
                        break;
                    default:
                        fail("Unexpected element <" + node.getNodeName() + "> in the <dbimport> config");
                }
            }
        }

        assertTrue(defaultPackageSeen);
        assertEquals(1, elements);
    }

    @Test
    public void testModelUpgrade() {
        DataChannelDescriptor descriptor = mock(DataChannelDescriptor.class);
        handler.processModel(descriptor);
        verifyNoInteractions(descriptor);
    }

    private static final String TEST_TEMPLATE_CONTENT =
            String.join(System.lineSeparator(),
                        "##   Licensed to the Apache Software Foundation (ASF) under one",
                        "##  or more contributor license agreements.  See the NOTICE file",
                        "##  distributed with this work for additional information",
                        "##  regarding copyright ownership.  The ASF licenses this file",
                        "##  to you under the Apache License, Version 2.0 (the",
                        "##  \"License\"); you may not use this file except in compliance",
                        "##  with the License.  You may obtain a copy of the License at",
                        "##",
                        "##    https://www.apache.org/licenses/LICENSE-2.0",
                        "##",
                        "##  Unless required by applicable law or agreed to in writing,",
                        "##  software distributed under the License is distributed on an",
                        "##  \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY",
                        "##  KIND, either express or implied.  See the License for the",
                        "##  specific language governing permissions and limitations",
                        "##  under the License.",
                        "velocity template stub");
}