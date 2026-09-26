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

package org.apache.cayenne.configuration.upgrade;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.resource.URLResource;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

public class UpgradeHandler_V9Test extends BaseUpgradeHandlerTest {

    @Override
    UpgradeHandler newHandler() {
        return new UpgradeHandler_V9();
    }

    @Test
    public void projectDomUpgrade() throws Exception {
        Document document = processProjectDom("cayenne-project-v8.xml");

        Element root = document.getDocumentElement();
        assertEquals("9", root.getAttribute("project-version"));
        assertEquals("", root.getAttribute("xmlns"));
        assertEquals(2, root.getElementsByTagName("map").getLength());
    }

    @Test
    public void dataMapDomUpgrade() throws Exception {
        String resource = "test-map-v8.map.xml";
        UpgradeContext unit = new UpgradeContext(new URLResource(getClass().getResource(resource)),
                documentFromResource(resource));
        handler.processDataMapDom(unit);

        Element root = unit.getDocument().getDocumentElement();
        assertEquals("9", root.getAttribute("project-version"));
        assertEquals("http://cayenne.apache.org/schema/9/modelMap", root.getAttribute("xmlns"));
        assertEquals(0, root.getElementsByTagName("reverse-engineering-config").getLength());
        assertEquals(2, root.getElementsByTagName("db-attribute").getLength());

        // the standalone reverse engineering config file is reported, not deleted, by the handler
        assertEquals(List.of("reverseEngineering.xml"), unit.getObsoleteFiles());
    }

    @Test
    public void modelUpgrade() throws Exception {
        DataChannelDescriptor descriptor = mock(DataChannelDescriptor.class);
        handler.processModel(descriptor);
        verifyNoInteractions(descriptor);
    }
}
