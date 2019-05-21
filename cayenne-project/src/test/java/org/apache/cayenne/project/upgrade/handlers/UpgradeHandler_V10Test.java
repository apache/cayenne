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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * @since 4.1
 */
public class UpgradeHandler_V10Test extends BaseUpgradeHandlerTest{

    UpgradeHandler newHandler() {
        return new UpgradeHandler_V10();
    }

    @Test
    public void testProjectDomUpgrade() throws Exception {
        Document document = processProjectDom("cayenne-project-v9.xml");

        Element root = document.getDocumentElement();
        assertEquals("10", root.getAttribute("project-version"));
        assertEquals("http://cayenne.apache.org/schema/10/domain", root.getAttribute("xmlns"));
        assertEquals(2, root.getElementsByTagName("map").getLength());
    }

    @Test
    public void testDataMapDomUpgrade() throws Exception {
        Document document = processDataMapDom("test-map-v9.map.xml");

        Element root = document.getDocumentElement();
        assertEquals("10", root.getAttribute("project-version"));
        assertEquals("http://cayenne.apache.org/schema/10/modelMap", root.getAttribute("xmlns"));
        assertEquals(2, root.getElementsByTagName("db-attribute").getLength());
    }

    @Test
    public void testModelUpgrade() throws Exception {
        DataChannelDescriptor descriptor = mock(DataChannelDescriptor.class);
        handler.processModel(descriptor);
        verifyZeroInteractions(descriptor);
    }
}