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

package org.apache.cayenne.modeler.pref.migration.toV5;

import org.apache.cayenne.modeler.pref.PrefsLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

public class _8_RemoveRedundantPathIndexMigrationTest {

    private Preferences testRoot;
    private PrefsLocator locator;

    @BeforeEach
    public void setUp() {
        testRoot = Preferences.userRoot().node("test-cayenne-prefs-" + UUID.randomUUID());
        locator = new PrefsLocator(testRoot);
    }

    @AfterEach
    public void tearDown() throws Exception {
        testRoot.removeNode();
    }

    @Test
    public void removesIndexSubtreesAndPathKeys() throws Exception {
        Preferences app = locator.appNode(null);
        app.node("projectIndex").put("abc-cayenne-project", "/tmp/cayenne-project.xml");
        app.node("dataMapIndex").put("def-datamap", "/tmp/datamap.map.xml");

        Preferences projectEntry = locator.projectNode("abc-cayenne-project");
        projectEntry.put("path", "/tmp/cayenne-project.xml");
        projectEntry.put("domain", "project");

        Preferences dataMapEntry = locator.dataMapNode("def-datamap");
        dataMapEntry.put("path", "/tmp/datamap.map.xml");
        dataMapEntry.put("superclassPackage", "org.example");

        app.flush();

        new _8_RemoveRedundantPathIndexMigration().apply(locator);

        assertFalse(app.nodeExists("projectIndex"),
                "projectIndex subtree should be removed");
        assertFalse(app.nodeExists("dataMapIndex"),
                "dataMapIndex subtree should be removed");

        assertNull(projectEntry.get("path", null),
                "path key on project node should be removed");
        assertEquals("project", projectEntry.get("domain", null),
                "unrelated project node keys should survive");

        assertNull(dataMapEntry.get("path", null),
                "path key on datamap node should be removed");
        assertEquals("org.example", dataMapEntry.get("superclassPackage", null),
                "unrelated datamap node keys should survive");
    }

    @Test
    public void isIdempotentOnEmptyTree() {
        new _8_RemoveRedundantPathIndexMigration().apply(locator);
        new _8_RemoveRedundantPathIndexMigration().apply(locator);
    }

    @Test
    public void version() {
        assertEquals(8, new _8_RemoveRedundantPathIndexMigration().version());
    }
}
