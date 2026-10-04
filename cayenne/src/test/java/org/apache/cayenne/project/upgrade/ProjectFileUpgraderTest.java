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

import org.apache.cayenne.project.xml.XMLReaderProvider;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.w3c.dom.Element;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProjectFileUpgraderTest {

    ProjectFileUpgrader upgrader;

    @BeforeEach
    public void createUpgrader() {
        upgrader = new ProjectFileUpgrader(new XMLReaderProvider(false));
    }

    @ParameterizedTest
    @CsvSource({
        "0,       INTERMEDIATE_UPGRADE_NEEDED",
        "3.2.1.0, INTERMEDIATE_UPGRADE_NEEDED",
        "5,       INTERMEDIATE_UPGRADE_NEEDED",
        "6,       INTERMEDIATE_UPGRADE_NEEDED",
        "8,       INTERMEDIATE_UPGRADE_NEEDED",
        "9,       UPGRADE_NEEDED",
        "10,      UPGRADE_NEEDED",
        "11,      UPGRADE_NEEDED",
        "12,      UPGRADE_NEEDED",
        "13,      UPGRADE_NEEDED",
        "14,      UPGRADE_NOT_NEEDED",
        "15,      DOWNGRADE_NEEDED"
    })
    public void checkUpgradeNeeded(String version, UpgradeType expectedType) {
        assertEquals(expectedType, upgrader.checkUpgradeNeeded(version));
    }

    @Test
    public void all() {
        List<String> versions = UpgradeHandler.all().stream().map(UpgradeHandler::getVersion).toList();
        assertEquals(List.of("10", "11", "12", "13", "14"), versions,
                "handlers must cover every version after the oldest supported one, in order");
        assertEquals(UpgradeHandler.CURRENT_VERSION, versions.getLast());
    }

    @Test
    public void handlersForVersion() {
        assertEquals(5, upgrader.handlersForVersion(UpgradeHandler.MIN_SUPPORTED_VERSION).size());
        assertTrue(upgrader.handlersForVersion(UpgradeHandler.CURRENT_VERSION).isEmpty());

        List<String> versions = upgrader.handlersForVersion("9").stream().map(UpgradeHandler::getVersion).toList();
        assertEquals(List.of("10", "11", "12", "13", "14"), versions);
    }

    @Test
    public void readVersion() {
        assertEquals("3.2.1.0", upgrader.readVersion(getResourceForVersion("3.2.1.0")));
        assertEquals("10", upgrader.readVersion(getResourceForVersion("10")));

        // starting with version 14 there is no version attribute, and the version is a part of the namespace
        assertEquals("14", upgrader.readVersion(getResourceForVersion("14")));
        assertEquals("15", upgrader.readVersion(getResourceForVersion("15")));
        assertEquals("6", upgrader.readVersion(new URLResource(getClass().getResource("test-map-v6.map.xml"))));
    }

    @ParameterizedTest
    @CsvSource({
        "1.2.3.4,   1.234",
        "1.0.0.0.4, 1.0004",
        "10,        10.0"
    })
    public void decodeVersion(String version, double expected) {
        assertEquals(expected, ProjectFileUpgrader.decodeVersion(version), 0.000001);
    }

    @Test
    public void upgradeProjectDOM() {
        UpgradeContext context = upgrader.upgradeProjectDOM(getResourceForVersion("9"), "9");

        Element root = context.getDocument().getDocumentElement();
        assertFalse(root.hasAttribute("project-version"));
        assertEquals("http://cayenne.apache.org/schema/14/project", root.getAttribute("xmlns"));
        assertEquals(2, root.getElementsByTagName("map").getLength());

        // the version 9 fixture has a DataNode, which version 13 removes without a replacement
        assertEquals(0, root.getElementsByTagName("node").getLength());
        assertEquals(1, context.getChangesAffectingRuntime().size());
    }

    @Test
    public void upgradeDataMapDOM() {
        Resource resource = new URLResource(getClass().getResource("test-map-v9.map.xml"));

        UpgradeContext context = upgrader.upgradeDataMapDOM(resource, "9");
        Element root = context.getDocument().getDocumentElement();
        assertEquals("dataMap", root.getNodeName());
        assertFalse(root.hasAttribute("project-version"));
        assertEquals("http://cayenne.apache.org/schema/14/dataMap", root.getAttribute("xmlns"));
    }

    private Resource getResourceForVersion(String version) {
        return new URLResource(getClass().getResource("cayenne-project-v" + version + ".xml"));
    }
}
