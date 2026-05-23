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
package org.apache.cayenne.mcp.tools.openproject;

import org.apache.cayenne.modeler.pref.PrefsLocator;
import org.apache.cayenne.mcp.tools.openproject.HandshakeWatcher.Outcome;
import org.apache.cayenne.mcp.tools.openproject.HandshakeWatcher.WatchResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HandshakeWatcherTest {

    private static final BooleanSupplier ALIVE = () -> true;
    private static final PrefsLocator LOCATOR = new PrefsLocator();
    private final String nonce = "test-" + UUID.randomUUID().toString().replace("-", "");

    @AfterEach
    public void cleanup() throws BackingStoreException {
        if (LOCATOR.handshakeNodeExists(nonce)) {
            LOCATOR.handshakeNode(nonce).removeNode();
        }
    }

    @Test
    public void handshakeReceivedReadsPidAndPath() throws BackingStoreException {
        Preferences node = LOCATOR.handshakeNode(nonce);
        node.putLong("pid", 4242L);
        node.put("startedAt", "2026-05-17T10:00:00Z");
        node.put("projectPath", "/abs/cayenne-project.xml");
        node.flush();

        WatchResult result = HandshakeWatcher.await(nonce, ALIVE, Duration.ofSeconds(2), LOCATOR);

        assertEquals(Outcome.HANDSHAKE_RECEIVED, result.outcome());
        assertNotNull(result.data());
        assertEquals(4242L, result.data().pid());
        assertEquals("2026-05-17T10:00:00Z", result.data().startedAt());
        assertEquals("/abs/cayenne-project.xml", result.data().resolvedProjectPath());
        assertTrue(result.waitMs() >= 0);

        // Watcher must remove the node after reading.
        assertFalse(LOCATOR.handshakeNodeExists(nonce));
    }

    @Test
    public void timeoutOnMissingHandshake() {
        WatchResult result = HandshakeWatcher.await(nonce, ALIVE, Duration.ofMillis(400), LOCATOR);

        assertEquals(Outcome.TIMEOUT, result.outcome());
        assertNull(result.data());
        assertTrue(result.waitMs() >= 400, "should have waited at least the full timeout: " + result.waitMs());
        assertTrue(result.waitMs() < 1500, "should not have waited much past the timeout: " + result.waitMs());
    }

    @Test
    public void earlyExitWhenSpawnedProcessDies() {
        AtomicBoolean alive = new AtomicBoolean(true);
        // Flip to dead after a brief delay so the watcher gets at least one poll iteration
        // showing alive=true.
        new Thread(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
            }
            alive.set(false);
        }, "test-killer").start();

        long start = System.currentTimeMillis();
        WatchResult result = HandshakeWatcher.await(nonce, alive::get, Duration.ofSeconds(10), LOCATOR);
        long elapsed = System.currentTimeMillis() - start;

        assertEquals(Outcome.SPAWNED_PROCESS_EXITED, result.outcome());
        assertNull(result.data());
        assertTrue(elapsed < 3000,
                "must return promptly after process death, not wait for the full timeout: " + elapsed);
    }

    @Test
    public void prunesSiblingsOlderThan24Hours() throws BackingStoreException {
        String freshNonce = "test-fresh-" + UUID.randomUUID().toString().replace("-", "");
        String staleNonce = "test-stale-" + UUID.randomUUID().toString().replace("-", "");
        try {
            Preferences fresh = LOCATOR.handshakeNode(freshNonce);
            fresh.put("startedAt", Instant.now().toString());
            fresh.flush();

            Preferences stale = LOCATOR.handshakeNode(staleNonce);
            stale.put("startedAt", Instant.now().minus(Duration.ofDays(2)).toString());
            stale.flush();

            // Trigger pruning by running the watcher with a missing-handshake nonce.
            HandshakeWatcher.await(nonce, ALIVE, Duration.ofMillis(200), LOCATOR);

            assertTrue(LOCATOR.handshakeNodeExists(freshNonce),
                    "Recent sibling must be preserved");
            assertFalse(LOCATOR.handshakeNodeExists(staleNonce),
                    "Stale sibling must be pruned");
        } finally {
            if (LOCATOR.handshakeNodeExists(freshNonce)) {
                LOCATOR.handshakeNode(freshNonce).removeNode();
            }
            if (LOCATOR.handshakeNodeExists(staleNonce)) {
                LOCATOR.handshakeNode(staleNonce).removeNode();
            }
        }
    }
}
