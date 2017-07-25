/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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

import org.apache.cayenne.configuration.DataChannelDescriptorLoader;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.project.ProjectSaver;
import org.apache.cayenne.project.upgrade.UpgradeService;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler_V10;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler_V7;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler_V8;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler_V9;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.junit.Test;
import org.w3c.dom.Document;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * @since 4.1
 */
public class CompatibilityUpgradeServiceIT {

    @Test
    public void testUpgradeDom() throws Exception {
        Injector injector = DIBootstrap.createInjector(new Module() {
            @Override
            public void configure(Binder binder) {
                binder.bind(UpgradeService.class).to(CompatibilityUpgradeService.class);
                binder.bind(DocumentProvider.class).to(DefaultDocumentProvider.class);
                binder.bindList(UpgradeHandler.class)
                        .add(UpgradeHandler_V7.class)
                        .add(UpgradeHandler_V8.class)
                        .add(UpgradeHandler_V9.class)
                        .add(UpgradeHandler_V10.class);

                binder.bind(ProjectSaver.class).toInstance(mock(ProjectSaver.class));
                binder.bind(DataChannelDescriptorLoader.class).toInstance(mock(DataChannelDescriptorLoader.class));
            }
        });

        CompatibilityUpgradeService upgradeService = (CompatibilityUpgradeService)injector
                .getInstance(UpgradeService.class);

        DocumentProvider documentProvider = injector.getInstance(DocumentProvider.class);

        URL resourceUrl = getClass().getResource("cayenne-project-v6.xml");
        Resource resource = new URLResource(resourceUrl);
        upgradeService.upgradeProject(resource);

        Document domainDocument = documentProvider.getDocument(resourceUrl);

        assertNotNull(domainDocument);
        assertEquals("10", domainDocument.getDocumentElement().getAttribute("project-version"));

        URL dataMapUrl = getClass().getResource("test-map-v6.map.xml");
        Document dataMapDocument = documentProvider.getDocument(dataMapUrl);
        assertNotNull(dataMapDocument);
        assertEquals("10", dataMapDocument.getDocumentElement().getAttribute("project-version"));
        assertEquals(1, dataMapDocument.getElementsByTagName("obj-entity").getLength());
        assertEquals(1, dataMapDocument.getElementsByTagName("db-entity").getLength());
        assertEquals(2, dataMapDocument.getElementsByTagName("db-attribute").getLength());
    }

}