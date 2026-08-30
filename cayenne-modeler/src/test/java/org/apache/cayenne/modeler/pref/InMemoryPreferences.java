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

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;

/**
 * A {@link java.util.prefs.Preferences} node backed by a plain map, with no persistent store behind it.
 * Pass it to {@code new PrefsLocator(root)} to keep a test off the platform preferences.
 * <p>
 * Unit tests must never touch the real {@link java.util.prefs.Preferences#userRoot()}: on macOS that is a
 * single CFPreferences domain shared by every JVM of the current user, and the JDK writes the whole cached
 * tree back on flush. Two Maven modules testing in parallel therefore corrupt each other - keys read back as
 * {@code null} moments after being flushed, and {@code removeNode()} silently fails to stick - no matter how
 * unique a node path each test picks. Integration tests are a different story: their handshake spans two
 * processes, so they need the platform store.
 */
public class InMemoryPreferences extends AbstractPreferences {

    private final Map<String, String> values = new HashMap<>();
    private final Map<String, InMemoryPreferences> children = new HashMap<>();

    /**
     * Creates a root node.
     */
    public InMemoryPreferences() {
        this(null, "");
    }

    private InMemoryPreferences(InMemoryPreferences parent, String name) {
        super(parent, name);
    }

    @Override
    protected void putSpi(String key, String value) {
        values.put(key, value);
    }

    @Override
    protected String getSpi(String key) {
        return values.get(key);
    }

    @Override
    protected void removeSpi(String key) {
        values.remove(key);
    }

    @Override
    protected void removeNodeSpi() {
        ((InMemoryPreferences) parent()).children.remove(name());
    }

    @Override
    protected String[] keysSpi() {
        return values.keySet().toArray(new String[0]);
    }

    @Override
    protected String[] childrenNamesSpi() {
        return children.keySet().toArray(new String[0]);
    }

    @Override
    protected AbstractPreferences childSpi(String name) {
        return children.computeIfAbsent(name, n -> new InMemoryPreferences(this, n));
    }

    @Override
    protected void syncSpi() {
        // nothing to sync - the map is the store
    }

    @Override
    protected void flushSpi() {
        // nothing to flush - the map is the store
    }
}
