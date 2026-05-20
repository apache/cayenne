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

import java.util.prefs.Preferences;

/**
 * Resolves common types of preference nodes against the common root.
 */
public final class PrefsLocator {

    private static final String ALL_ROOT = "org/apache/cayenne/modeler";

    // TODO: move under MODELER_ROOT?
    private static final String HANDSHAKE_ROOT = ALL_ROOT + "/mcp-handshake";

    private static final String MODELER_ROOT = ALL_ROOT + "/v5";
    private static final String APP_ROOT = MODELER_ROOT + "/app";
    private static final String PROJECT_ROOT = MODELER_ROOT + "/project";
    private static final String DATAMAP_ROOT = MODELER_ROOT + "/datamap";


    private final Preferences root;

    public PrefsLocator() {
        this(Preferences.userRoot());
    }

    /**
     * Test-friendly constructor letting callers point the locator at an isolated
     * preferences subtree instead of {@link Preferences#userRoot()}.
     */
    public PrefsLocator(Preferences root) {
        this.root = root;
    }

    /**
     * The root of the Modeler's preferences' tree.
     */
    public Preferences modelerRoot() {
        return root.node(MODELER_ROOT);
    }

    /**
     * Returns a node under {@code v5/app}, optionally descending into a subtree.
     * Pass {@code null} or empty for the {@code app} node itself.
     */
    public Preferences appNode(String relativePath) {
        Preferences appRoot = root.node(APP_ROOT);
        return relativePath == null || relativePath.isEmpty() ? appRoot : appRoot.node(relativePath);
    }

    /**
     * Returns the preferences node for the project with the given id. The id is preferences node name derived from
     * the project's absolute XML path — a truncated SHA-256 hash with a sanitized basename suffix.
     */
    public Preferences projectNode(String projectId) {
        return root.node(PROJECT_ROOT).node(projectId);
    }

    /**
     * Returns the preferences node for the DataMap with the given id. The id is preferences node name derived from
     * the DataMap absolute XML path — a truncated SHA-256 hash with a sanitized basename suffix.
     */
    public Preferences dataMapNode(String dataMapId) {
        return root.node(DATAMAP_ROOT).node(dataMapId);
    }

    /**
     * Returns the per-nonce node under the MCP launch-handshake namespace
     * ({@code org/apache/cayenne/modeler/mcp-handshake/<nonce>}). This subtree
     * is intentionally a sibling of {@link #modelerRoot()} so that Modeler-wide
     * resets do not clobber in-flight handshakes.
     */
    public Preferences handshakeNode(String nonce) {
        return root.node(HANDSHAKE_ROOT).node(nonce);
    }
}
