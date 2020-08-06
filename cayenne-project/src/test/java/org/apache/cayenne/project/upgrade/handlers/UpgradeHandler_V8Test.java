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
import org.w3c.dom.NodeList;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * @since 4.1
 */
public class UpgradeHandler_V8Test extends BaseUpgradeHandlerTest {

    @Override
    UpgradeHandler newHandler() {
        return new UpgradeHandler_V8();
    }

    @Test
    public void testProjectDomUpgrade() throws Exception {
        Document document = processProjectDom("cayenne-project-v7.xml");

        Element root = document.getDocumentElement();
        assertEquals("8", root.getAttribute("project-version"));
        assertEquals("", root.getAttribute("xmlns"));
        assertEquals(2, root.getElementsByTagName("map").getLength());
    }

    @Test
    public void testDataMapDomUpgrade() throws Exception {
        Document document = processDataMapDom("test-map-v7.map.xml");

        Element root = document.getDocumentElement();
        assertEquals("8", root.getAttribute("project-version"));
        assertEquals("http://cayenne.apache.org/schema/8/modelMap", root.getAttribute("xmlns"));
        assertEquals(2, root.getElementsByTagName("db-attribute").getLength());

        NodeList nodeList = root.getElementsByTagName("query");
        for(int i=0; i<nodeList.getLength(); i++) {
            NamedNodeMap attributes = nodeList.item(i).getAttributes();

            // should change factory to type
            assertNull(attributes.getNamedItem("factory"));

            String type = attributes.getNamedItem("type").getNodeValue();
            switch (attributes.getNamedItem("name").getNodeValue()) {
                case "EjbqlQueryTest":
                    assertEquals("EJBQLQuery", type);
                    break;

                case "SQLTemplateTest":
                    assertEquals("SQLTemplate", type);
                    break;

                case "SelectQueryTest":
                    assertEquals("SelectQuery", type);
                    break;

                case "ProcedureQueryTest":
                    assertEquals("ProcedureQuery", type);
                    break;
            }
        }
    }

    @Test
    public void testModelUpgrade() throws Exception {
        DataChannelDescriptor descriptor = mock(DataChannelDescriptor.class);
        handler.processModel(descriptor);
        verifyZeroInteractions(descriptor);
    }
}