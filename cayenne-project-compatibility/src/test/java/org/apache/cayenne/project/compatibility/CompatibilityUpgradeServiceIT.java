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

package org.apache.cayenne.project.compatibility;

import java.net.URL;

import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.project.upgrade.UpgradeService;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.junit.Test;
import org.w3c.dom.Document;

import static org.junit.Assert.*;

/**
 * @since 4.1
 */
public class CompatibilityUpgradeServiceIT {

    @Test
    public void testUpgradeFullProjectDom() {
        Injector injector = getInjector();

        CompatibilityUpgradeService upgradeService = (CompatibilityUpgradeService)injector
                .getInstance(UpgradeService.class);

        DocumentProvider documentProvider = injector.getInstance(DocumentProvider.class);

        URL resourceUrl = getClass().getResource("cayenne-project-v6.xml");
        Resource resource = new URLResource(resourceUrl);
        upgradeService.upgradeProject(resource);

        Document domainDocument = documentProvider.getDocument(resourceUrl);

        assertNotNull(domainDocument);
        assertEquals("12", domainDocument.getDocumentElement().getAttribute("project-version"));

        URL dataMapUrl = getClass().getResource("test-map-v6.map.xml");
        Document dataMapDocument = documentProvider.getDocument(dataMapUrl);
        assertNotNull(dataMapDocument);
        assertEquals("12", dataMapDocument.getDocumentElement().getAttribute("project-version"));
        assertEquals(1, dataMapDocument.getElementsByTagName("obj-entity").getLength());
        assertEquals(1, dataMapDocument.getElementsByTagName("db-entity").getLength());
        assertEquals(2, dataMapDocument.getElementsByTagName("db-attribute").getLength());
    }

    @Test
    public void testUpgradeStandAloneDataMapDom() {
        Injector injector = getInjector();

        CompatibilityUpgradeService upgradeService = (CompatibilityUpgradeService)injector
                .getInstance(UpgradeService.class);

        DocumentProvider documentProvider = injector.getInstance(DocumentProvider.class);

        URL dataMapUrl = getClass().getResource("test-map-v6.map.xml");
        Document dataMapDocument = documentProvider.getDocument(dataMapUrl);
        assertNull(dataMapDocument);

        Resource resource = new URLResource(dataMapUrl);
        upgradeService.upgradeDataMap(resource);

        dataMapDocument = documentProvider.getDocument(dataMapUrl);
        assertNotNull(dataMapDocument);
        assertEquals("12", dataMapDocument.getDocumentElement().getAttribute("project-version"));
    }

    private Injector getInjector() {
        return DIBootstrap.createInjector(new CompatibilityTestModule());
    }
}