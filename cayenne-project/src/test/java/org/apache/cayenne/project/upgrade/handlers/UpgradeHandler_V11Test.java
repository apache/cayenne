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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * @since 4.3
 */
public class UpgradeHandler_V11Test extends BaseUpgradeHandlerTest{

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
        NamedNodeMap attributes = objEntities.item(0).getAttributes();
        assertEquals(2, attributes.getLength());
        assertEquals("Artist", attributes.getNamedItem("name").getNodeValue());
        assertEquals("Artist", attributes.getNamedItem("dbEntityName").getNodeValue());

        assertEquals(2, root.getElementsByTagName("db-attribute").getLength());
    }

    @Test
    public void testModelUpgrade() throws Exception {
        DataChannelDescriptor descriptor = mock(DataChannelDescriptor.class);
        handler.processModel(descriptor);
        verifyZeroInteractions(descriptor);
    }
}