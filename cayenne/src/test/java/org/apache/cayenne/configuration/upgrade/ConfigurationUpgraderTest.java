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
import org.apache.cayenne.configuration.xml.XMLReaderProvider;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.w3c.dom.Element;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class ConfigurationUpgraderTest {

    ConfigurationUpgrader upgrader;

    @BeforeEach
    public void createUpgrader() {
        upgrader = new ConfigurationUpgrader(new XMLReaderProvider(false));
    }

    @ParameterizedTest
    @CsvSource({
        "0,       INTERMEDIATE_UPGRADE_NEEDED",
        "3.2.1.0, INTERMEDIATE_UPGRADE_NEEDED",
        "5,       INTERMEDIATE_UPGRADE_NEEDED",
        "6,       UPGRADE_NEEDED",
        "10,      UPGRADE_NEEDED",
        "11,      UPGRADE_NEEDED",
        "12,      UPGRADE_NEEDED",
        "13,      UPGRADE_NOT_NEEDED",
        "14,      DOWNGRADE_NEEDED"
    })
    public void checkUpgradeNeeded(String version, UpgradeType expectedType) {
        assertEquals(expectedType, upgrader.checkUpgradeNeeded(version));
    }

    @Test
    public void all() {
        List<String> versions = UpgradeHandler.all().stream().map(UpgradeHandler::getVersion).toList();
        assertEquals(List.of("7", "8", "9", "10", "11", "12", "13"), versions,
                "handlers must cover every version after the oldest supported one, in order");
        assertEquals(UpgradeHandler.CURRENT_VERSION, versions.getLast());
    }

    @Test
    public void handlersForVersion() {
        assertEquals(7, upgrader.handlersForVersion(UpgradeHandler.MIN_SUPPORTED_VERSION).size());
        assertTrue(upgrader.handlersForVersion(UpgradeHandler.CURRENT_VERSION).isEmpty());

        List<String> versions = upgrader.handlersForVersion("9").stream().map(UpgradeHandler::getVersion).toList();
        assertEquals(List.of("10", "11", "12", "13"), versions);
    }

    @Test
    public void readVersion() {
        assertEquals("3.2.1.0", upgrader.readVersion(getResourceForVersion("3.2.1.0")));
        assertEquals("10", upgrader.readVersion(getResourceForVersion("10")));
        assertEquals("6", upgrader.readVersion(new URLResource(getClass().getResource("test-map-v6.map.xml"))));
    }

    @ParameterizedTest
    @CsvSource({
        "1.2.3.4,   1.234",
        "1.0.0.0.4, 1.0004",
        "10,        10.0"
    })
    public void decodeVersion(String version, double expected) {
        assertEquals(expected, ConfigurationUpgrader.decodeVersion(version), 0.000001);
    }

    @Test
    public void upgradeProjectDom() {
        UpgradeContext context = upgrader.upgradeProjectDom(getResourceForVersion("6"), "6");

        Element root = context.getDocument().getDocumentElement();
        assertEquals("13", root.getAttribute("project-version"));
        assertEquals("http://cayenne.apache.org/schema/13/domain", root.getAttribute("xmlns"));
        assertEquals(2, root.getElementsByTagName("map").getLength());

        // the version 6 fixture has a DataNode, which version 13 removes without a replacement
        assertEquals(0, root.getElementsByTagName("node").getLength());
        assertEquals(1, context.getChangesAffectingRuntime().size());
    }

    @Test
    public void upgradeDataMapDom() {
        Resource resource = new URLResource(getClass().getResource("test-map-v8.map.xml"));

        // starting at version 9 skips the version 9 handler, which drops the reverse engineering config
        UpgradeContext from9 = upgrader.upgradeDataMapDom(resource, "9");
        Element root = from9.getDocument().getDocumentElement();
        assertEquals("13", root.getAttribute("project-version"));
        assertEquals("http://cayenne.apache.org/schema/13/modelMap", root.getAttribute("xmlns"));
        assertEquals(1, root.getElementsByTagName("reverse-engineering-config").getLength());
        assertTrue(from9.getObsoleteFiles().isEmpty());

        UpgradeContext from8 = upgrader.upgradeDataMapDom(resource, "8");
        root = from8.getDocument().getDocumentElement();
        assertEquals("13", root.getAttribute("project-version"));
        assertEquals(0, root.getElementsByTagName("reverse-engineering-config").getLength());
        assertEquals(List.of("reverseEngineering.xml"), from8.getObsoleteFiles());
    }

    @Test
    public void upgradeModel() {
        DataChannelDescriptor descriptor = mock(DataChannelDescriptor.class);

        // only the version 7 handler has a model-level step
        upgrader.upgradeModel("7", descriptor);
        verifyNoInteractions(descriptor);

        upgrader.upgradeModel("6", descriptor);
        verify(descriptor).getDataMaps();
    }

    private Resource getResourceForVersion(String version) {
        return new URLResource(getClass().getResource("cayenne-project-v" + version + ".xml"));
    }
}
