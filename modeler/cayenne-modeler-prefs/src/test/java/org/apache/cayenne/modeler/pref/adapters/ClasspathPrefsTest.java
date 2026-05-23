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

package org.apache.cayenne.modeler.pref.adapters;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClasspathPrefsTest {

    private final Preferences root = Preferences.userRoot()
            .node("cayenne-test/" + UUID.randomUUID().toString().replace("-", ""));
    private final ClasspathPrefs prefs = new ClasspathPrefs(root);

    @AfterEach
    public void cleanup() throws BackingStoreException {
        root.removeNode();
    }

    @Test
    public void emptyPrefsReturnsEmptyList() {
        assertTrue(prefs.getEntries().isEmpty());
    }

    @Test
    public void singleEntryRoundtrip() {
        prefs.setEntries(List.of("/lib/foo.jar"));
        assertEquals(List.of("/lib/foo.jar"), prefs.getEntries());
    }

    @Test
    public void multipleEntriesPreserveOrder() {
        List<String> entries = List.of("/a.jar", "/b.jar", "/c.jar");
        prefs.setEntries(entries);
        assertEquals(entries, prefs.getEntries());
    }

    @Test
    public void setEntriesOverwritesClearsPreviousEntries() {
        prefs.setEntries(List.of("/old.jar"));
        prefs.setEntries(List.of("/new.jar"));
        assertEquals(List.of("/new.jar"), prefs.getEntries());
    }

    @Test
    public void numericKeysAreOrderedNumerically() throws BackingStoreException {
        // Write keys manually in a non-lexicographic order: 10 before 2
        root.put("1", "/first.jar");
        root.put("10", "/tenth.jar");
        root.put("2", "/second.jar");
        root.flush();

        // Numeric sort: 1 < 2 < 10, not lexicographic "1" < "10" < "2"
        assertEquals(List.of("/first.jar", "/second.jar", "/tenth.jar"), prefs.getEntries());
    }

    @Test
    public void nonNumericKeysAreIgnoredOnRead() throws BackingStoreException {
        root.put("foo", "/should-be-ignored.jar");
        root.put("1", "/valid.jar");
        root.flush();

        assertEquals(List.of("/valid.jar"), prefs.getEntries());
    }

    @Test
    public void emptyValuesAreSkippedOnRead() throws BackingStoreException {
        root.put("1", "");
        root.put("2", "/valid.jar");
        root.flush();

        assertEquals(List.of("/valid.jar"), prefs.getEntries());
    }
}
