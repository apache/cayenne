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

import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.util.Util;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @since 4.1
 */
public class DefaultUpgradeServiceTest {

    DefaultUpgradeService upgradeService;

    List<UpgradeHandler> handlers;

    @Before
    public void createService() {
        createHandlers();
        upgradeService = new DefaultUpgradeService(handlers);
    }

    @Test
    public void getUpgradeType() throws Exception {
        UpgradeMetaData metaData = upgradeService.getUpgradeType(getResourceForVersion("5"));
        assertEquals(UpgradeType.INTERMEDIATE_UPGRADE_NEEDED, metaData.getUpgradeType());

        metaData = upgradeService.getUpgradeType(getResourceForVersion("6"));
        assertEquals(UpgradeType.UPGRADE_NEEDED, metaData.getUpgradeType());

        metaData = upgradeService.getUpgradeType(getResourceForVersion("10"));
        assertEquals(UpgradeType.UPGRADE_NEEDED, metaData.getUpgradeType());

        metaData = upgradeService.getUpgradeType(getResourceForVersion("11"));
        assertEquals(UpgradeType.UPGRADE_NEEDED, metaData.getUpgradeType());

        metaData = upgradeService.getUpgradeType(getResourceForVersion("12"));
        assertEquals(UpgradeType.UPGRADE_NOT_NEEDED, metaData.getUpgradeType());

        metaData = upgradeService.getUpgradeType(getResourceForVersion("99"));
        assertEquals(UpgradeType.DOWNGRADE_NEEDED, metaData.getUpgradeType());
    }

    @Test
    public void getHandlersForVersion() throws Exception {

        List<UpgradeHandler> handlers = upgradeService.getHandlersForVersion("6");
        assertEquals(5, handlers.size());

        handlers = upgradeService.getHandlersForVersion("9");
        assertEquals(2, handlers.size());
        assertEquals("10", handlers.get(0).getVersion());
        assertEquals("11", handlers.get(1).getVersion());
    }

    @Test
    public void getAdditionalDatamapResources() throws Exception {
        URL url = getClass().getResource("../cayenne-PROJECT1.xml");
        Resource resource = new URLResource(url);
        Document document = readDocument(url);
        UpgradeUnit unit = new UpgradeUnit(resource, document);
        List<Resource> resources = upgradeService.getAdditionalDatamapResources(unit);

        assertEquals(2, resources.size());
        assertTrue(resources.get(0).getURL().sameFile(getClass().getResource("../testProjectMap1_1.map.xml")));
    }

    @Test
    public void loadProjectVersion() throws Exception {
        assertEquals("3.2.1.0", upgradeService.loadProjectVersion(getResourceForVersion("3.2.1.0")));
        assertEquals("10", upgradeService.loadProjectVersion(getResourceForVersion("10")));
    }

    @Test
    public void decodeVersion() throws Exception {
        assertEquals(1.2340, DefaultUpgradeService.decodeVersion("1.2.3.4"), 0.000001);
        assertEquals(1.0004, DefaultUpgradeService.decodeVersion("1.0.0.0.4"), 0.000001);
        assertEquals(10, DefaultUpgradeService.decodeVersion("10"), 0.000001);
    }

    @Test
    public void upgradeDOM() throws Exception {
        Resource resource = new URLResource(getClass().getResource("../cayenne-PROJECT1.xml"));

        // Mock service so it will use actual reading but skip actual saving part
        upgradeService = mock(DefaultUpgradeService.class);
        when(upgradeService.upgradeDOM(any(Resource.class), ArgumentMatchers.<UpgradeHandler>anyList()))
                .thenCallRealMethod();
        when(upgradeService.getAdditionalDatamapResources(any(UpgradeUnit.class)))
                .thenCallRealMethod();

        upgradeService.upgradeDOM(resource, handlers);

        // one for project and two for data maps
//        verify(upgradeService, times(3)).saveDocument(any(UpgradeUnit.class));
        for(UpgradeHandler handler : handlers) {
            verify(handler).getVersion();
            verify(handler).processProjectDom(any(UpgradeUnit.class));
            // two data maps
            verify(handler, times(2)).processDataMapDom(any(UpgradeUnit.class));
            verify(handler).processAllDataMapDomes(anyList());
            verifyNoMoreInteractions(handler);
        }
    }

    @Test
    public void readDocument() throws Exception {
        Document document = Util.readDocument(getClass().getResource("../cayenne-PROJECT1.xml"));
        assertEquals(String.valueOf(Project.VERSION), document.getDocumentElement().getAttribute("project-version"));
    }

    private Document readDocument(URL url) throws Exception {
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return db.parse(new InputSource(new InputStreamReader(url.openStream())));
    }

    private void createHandlers() {
        handlers = new ArrayList<>();
        String[] versions = {"7", "8", "9", "10", "11"};
        for(String version : versions) {
            handlers.add(createHandler(version));
        }
    }

    private UpgradeHandler createHandler(String version) {
        UpgradeHandler handler = mock(UpgradeHandler.class);
        when(handler.getVersion()).thenReturn(version);
        return handler;
    }

    private Resource getResourceForVersion(String version) {
        return new URLResource(getClass().getResource("handlers/cayenne-project-v"+version+".xml"));
    }

}