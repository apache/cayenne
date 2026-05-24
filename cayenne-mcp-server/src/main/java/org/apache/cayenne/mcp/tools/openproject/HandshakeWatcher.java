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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.function.BooleanSupplier;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Polls {@link Preferences} for the handshake entry written by
 * {@code org.apache.cayenne.modeler.mcp.McpHandshakeWriter} once the Modeler has
 * loaded the requested project. Each MCP-driven launch is keyed by a fresh nonce
 * so stale entries and concurrent launches never collide.
 *
 * @since 5.0
 */
class HandshakeWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(HandshakeWatcher.class);

    private static final long POLL_INTERVAL_MS = 200L;
    private static final Duration STALE_NODE_TTL = Duration.ofHours(24);

    enum Outcome {HANDSHAKE_RECEIVED, SPAWNED_PROCESS_EXITED, TIMEOUT}

    record WatchResult(Outcome outcome, HandshakeData data, long waitMs) {
    }

    record HandshakeData(long pid, String startedAt, String resolvedProjectPath) {
    }

    private HandshakeWatcher() {
    }

    /**
     * Waits for the Modeler to write the handshake for the given nonce, or until
     * the spawned process exits (when that signal is meaningful) or the timeout is hit.
     * Regardless of outcome, the nonce's subnode is removed and stale siblings are pruned.
     */
    public static WatchResult await(
            String nonce,
            BooleanSupplier spawnedProcessAlive,
            Duration timeout,
            PrefsLocator locator) {

        long start = System.currentTimeMillis();
        long deadline = start + timeout.toMillis();

        WatchResult result;
        try {
            while (true) {
                if (locator.handshakeNodeExists(nonce)) {
                    HandshakeData data = readHandshake(locator.handshakeNode(nonce));
                    long waitMs = System.currentTimeMillis() - start;
                    result = new WatchResult(Outcome.HANDSHAKE_RECEIVED, data, waitMs);
                    break;
                }

                if (!spawnedProcessAlive.getAsBoolean()) {
                    long waitMs = System.currentTimeMillis() - start;
                    result = new WatchResult(Outcome.SPAWNED_PROCESS_EXITED, null, waitMs);
                    break;
                }

                long now = System.currentTimeMillis();
                if (now >= deadline) {
                    result = new WatchResult(Outcome.TIMEOUT, null, now - start);
                    break;
                }

                long sleepMs = Math.min(POLL_INTERVAL_MS, deadline - now);
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    result = new WatchResult(Outcome.TIMEOUT, null, System.currentTimeMillis() - start);
                    break;
                }
            }
        } finally {
            removeNode(locator, nonce);
            pruneStaleSiblings(locator);
        }
        return result;
    }

    private static HandshakeData readHandshake(Preferences node) {
        try {
            // Force a backing-store reload so keys written by the Modeler process are
            // visible here — nodeExists() can return true before the reader's cache is
            // refreshed on macOS CFPreferences.
            node.sync();
        } catch (BackingStoreException e) {
            LOGGER.debug("sync before handshake read failed, proceeding with cached values: {}", e.toString());
        }
        long pid = node.getLong("pid", -1L);
        String startedAt = node.get("startedAt", null);
        String resolvedProjectPath = node.get("projectPath", null);
        return new HandshakeData(pid, startedAt, resolvedProjectPath);
    }

    private static void removeNode(PrefsLocator locator, String nonce) {
        try {
            if (locator.handshakeNodeExists(nonce)) {
                locator.handshakeNode(nonce).removeNode();
            }
        } catch (BackingStoreException | IllegalStateException e) {
            LOGGER.warn("Failed to remove handshake node for nonce {}: {}", nonce, e.toString());
        }
    }

    /**
     * Removes handshake subnodes whose {@code startedAt} is older than {@link #STALE_NODE_TTL}.
     * Belt-and-suspenders cleanup against an MCP server that crashed between launching
     * the Modeler and reading the handshake.
     */
    private static void pruneStaleSiblings(PrefsLocator locator) {
        if (!locator.handshakeRootNodeExists()) {
            return;
        }

        try {
            Preferences parent = locator.handshakeRootNode();
            Instant cutoff = Instant.now().minus(STALE_NODE_TTL);
            for (String child : parent.childrenNames()) {
                Preferences childNode = parent.node(child);
                String startedAt = childNode.get("startedAt", null);
                if (startedAt == null || isBefore(startedAt, cutoff)) {
                    childNode.removeNode();
                }
            }
        } catch (BackingStoreException | IllegalStateException e) {
            LOGGER.warn("Failed to prune stale handshake nodes: {}", e.toString());
        }
    }

    private static boolean isBefore(String startedAtIso, Instant cutoff) {
        try {
            return Instant.parse(startedAtIso).isBefore(cutoff);
        } catch (RuntimeException e) {
            // Unparseable timestamp: treat as stale and prune.
            return true;
        }
    }
}
