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

package org.apache.cayenne.modeler.mcp;

import org.apache.cayenne.modeler.pref.PrefsLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class McpHandshakeWriterTest {

    // Unique per test method - keeps parallel runs and prior aborted runs from colliding.
    private final String nonce = UUID.randomUUID().toString().replace("-", "");
    private final PrefsLocator locator = new PrefsLocator();

    @AfterEach
    public void cleanup() throws BackingStoreException {
        Preferences node = locator.handshakeNode(nonce);
        if (node != null) {
            node.removeNode();
            Preferences.userRoot().flush();
        }
    }

    @Test
    public void writesAllFourKeys() throws Exception {
        String[] argv = {"--mcp-handshake", nonce, "/path/to/cayenne-foo.xml"};
        long before = System.currentTimeMillis() - 1;

        McpHandshakeWriter.write(locator, nonce, argv, "/path/to/cayenne-foo.xml");

        Preferences prefs = pollForNode();

        String startedAt = prefs.get("startedAt", null);
        long pid = prefs.getLong("pid", -1);
        String args = prefs.get("args", null);
        String projectPath = prefs.get("projectPath", null);

        assertNotNull(startedAt, "startedAt should be written");
        Instant parsed = Instant.parse(startedAt);
        assertTrue(parsed.toEpochMilli() >= before,
                "startedAt should be a reasonable wall-clock timestamp");

        assertEquals(ProcessHandle.current().pid(), pid, "pid should be current process pid");
        assertEquals("--mcp-handshake " + nonce + " /path/to/cayenne-foo.xml", args);
        assertEquals("/path/to/cayenne-foo.xml", projectPath);
    }

    @Test
    public void nullArgvWritesEmptyArgsString() throws Exception {
        McpHandshakeWriter.write(locator, nonce, null, "/p");
        Preferences prefs = pollForNode();
        assertEquals("", prefs.get("args", null));
    }

    @Test
    public void nullProjectPathWritesEmptyString() throws Exception {
        McpHandshakeWriter.write(locator, nonce, new String[]{"x"}, null);
        Preferences prefs = pollForNode();
        assertEquals("", prefs.get("projectPath", null));
    }

    /**
     * Wait for the daemon writer thread to finish writing the node. Polls because the
     * write is asynchronous; checks for all four keys (not just `startedAt`) so we don't
     * race the writer's sequential puts.
     */
    private Preferences pollForNode() throws Exception {
        Preferences node = locator.handshakeNode(nonce);
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            if (hasAllKeys(node)) {
                return node;
            }
            Thread.sleep(25);
        }
        fail("Handshake node did not appear within 5s");
        return null; // unreachable
    }

    private boolean hasAllKeys(Preferences node) throws BackingStoreException {
        var keys = java.util.Set.of(node.keys());
        return keys.contains("startedAt")
                && keys.contains("pid")
                && keys.contains("args")
                && keys.contains("projectPath");
    }
}
