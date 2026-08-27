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

package org.apache.cayenne.modeler.pref;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PrefsLocatorTest {

    private final Preferences root = Preferences.userRoot()
            .node("cayenne-test/" + UUID.randomUUID().toString().replace("-", ""));
    private final PrefsLocator locator = new PrefsLocator(root);

    @AfterEach
    public void cleanup() throws BackingStoreException {
        root.removeNode();
    }

    @Test
    public void modelerRootIsUnderAllRoot() {
        assertTrue(locator.modelerRoot().absolutePath().endsWith("/v5"));
    }

    @Test
    public void appNodeNullReturnsAppRoot() {
        assertTrue(locator.appNode(null).absolutePath().endsWith("/v5/app"));
    }

    @Test
    public void appNodeEmptyReturnsAppRoot() {
        assertTrue(locator.appNode("").absolutePath().endsWith("/v5/app"));
    }

    @Test
    public void appNodeDescendsIntoSubtree() {
        assertTrue(locator.appNode("foo/bar").absolutePath().endsWith("/v5/app/foo/bar"));
    }

    @Test
    public void projectNodeIncludesId() {
        assertTrue(locator.projectNode("abc123").absolutePath().endsWith("/v5/project/abc123"));
    }

    @Test
    public void dataMapNodeIncludesId() {
        assertTrue(locator.dataMapNode("map42").absolutePath().endsWith("/v5/datamap/map42"));
    }

    @Test
    public void handshakeNodeExistsFalseByDefault() {
        assertFalse(locator.handshakeNodeExists("nonce-x"));
    }

    @Test
    public void handshakeNodeExistsTrueAfterWrite() throws BackingStoreException {
        locator.handshakeNode("nonce-y").put("pid", "123");
        root.flush();

        assertTrue(locator.handshakeNodeExists("nonce-y"));
    }

    @Test
    public void handshakeRootNodeExistsFalseByDefault() {
        assertFalse(locator.handshakeRootNodeExists());
    }

    @Test
    public void handshakeRootNodeExistsTrueAfterWrite() throws BackingStoreException {
        locator.handshakeNode("any-nonce").put("pid", "1");
        root.flush();

        assertTrue(locator.handshakeRootNodeExists());
    }
}
