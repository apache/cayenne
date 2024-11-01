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

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * @since 5.0
 */
public class UpgradeHandler_V12Test extends BaseUpgradeHandlerTest {

    UpgradeHandler newHandler() {
        return new UpgradeHandler_V12();
    }

    @Test
    public void testProjectDomUpgrade() throws Exception {
        Document document = processProjectDom("cayenne-project-v11.xml");

        Element root = document.getDocumentElement();
        assertEquals("12", root.getAttribute("project-version"));
        assertEquals("http://cayenne.apache.org/schema/12/domain", root.getAttribute("xmlns"));
        assertEquals(2, root.getElementsByTagName("map").getLength());
    }

    @Test
    public void testDataMapDomUpgrade() throws Exception {
        Document document = processDataMapDom("test-map-v11.map.xml");

        Element root = document.getDocumentElement();
        assertEquals("12", root.getAttribute("project-version"));
        assertEquals("http://cayenne.apache.org/schema/12/modelMap", root.getAttribute("xmlns"));
    }

    @Test
    public void testToDepPkToFkUpgrade() throws Exception {
        List<String> resources = Arrays.asList( "fkTestmap-1.map.xml","fkTestmap-2.map.xml");
        List<Document> documents = processAllDataMapDomes(resources);
        Document fkTestmap_1 = documents.get(0);
        Document fkTestmap_2 = documents.get(1);

        Element rootMap_1 = fkTestmap_1.getDocumentElement();
        NodeList dbRelationshipsMap_1 = rootMap_1.getElementsByTagName("db-relationship");

        Element rootMap_2 = fkTestmap_2.getDocumentElement();
        NodeList dbRelationshipsMap_2 = rootMap_2.getElementsByTagName("db-relationship");

        for (int i = 0; i < dbRelationshipsMap_1.getLength(); i++) {
            NamedNodeMap attributes = dbRelationshipsMap_1.item(i).getAttributes();
            String name = attributes.getNamedItem("name").getNodeValue();
            Node fk = attributes.getNamedItem("fk");
            switch (name) {
                case "reverse_several_matching_joins":
                case "noReverse_fk":
                case "joinsAnotherOrder":
                case "toDepPk":
                case "nPk_Pk":
                case "nPk_nPk":
                case "PK_PK":
                case "toDepPK_toDepPK":
                    assertNotNull(fk);
                    assertEquals(fk.getNodeValue(), "true");
                    break;
                case "noReverse_notFk":
                case "reverse_nPk_Pk":
                case "reverse_nPk_nPk":
                case "reverse_PK_PK":
                case "several_matching_joins":
                case "reverse_toDepPK_toDepPK":
                    assertNull(fk);
                    break;
            }
        }

        for (int i = 0; i < dbRelationshipsMap_2.getLength(); i++) {
            NamedNodeMap attributes = dbRelationshipsMap_2.item(i).getAttributes();
            String name = attributes.getNamedItem("name").getNodeValue();
            Node fk = attributes.getNamedItem("fk");
            switch (name) {
                case "reverse_reverseInAnotherDatamap":
                case "reverse_reverseInAnotherDatamapToDepPK":
                    assertNotNull(fk);
                    assertEquals(fk.getNodeValue(), "true");
                    break;
            }
        }

        for (int i = 0; i < dbRelationshipsMap_1.getLength(); i++) {
            NamedNodeMap attributes = dbRelationshipsMap_1.item(i).getAttributes();
            String name = attributes.getNamedItem("name").getNodeValue();
            Node toDepPK = attributes.getNamedItem("toDependentPK");
            switch (name) {
                case "reverse_toDepPk":
                case "reverseInAnotherDatamapToDepPK":
                case "toDepPK_toDepPK":
                case "reverse_toDepPK_toDepPK":
                    assertNull(toDepPK);
                    break;
            }
        }

        assertEquals(18, dbRelationshipsMap_1.getLength());
        assertEquals(2, dbRelationshipsMap_2.getLength());

    }

    @Test
    public void testModelUpgrade() {
        DataChannelDescriptor descriptor = mock(DataChannelDescriptor.class);
        handler.processModel(descriptor);
        verifyNoInteractions(descriptor);
    }
}