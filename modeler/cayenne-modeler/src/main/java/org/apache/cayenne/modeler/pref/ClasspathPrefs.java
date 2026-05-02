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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class ClasspathPrefs implements PreferenceAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClasspathPrefs.class);

    public static final String NODE = "classpath";

    private final Preferences prefs;

    private ClasspathPrefs(Preferences prefs) {
        this.prefs = prefs;
    }

    public static ClasspathPrefs of(PreferencesRepository repository) {
        return new ClasspathPrefs(repository.appPref(NODE));
    }

    // Returns classpath entries in numeric-key order. Defensive against any
    // non-numeric sub-keys; we only own numeric keys.
    public List<String> getEntries() {
        List<int[]> indexed = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (String key : keys()) {
            int idx;
            try {
                idx = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                continue;
            }
            String value = prefs.get(key, "");
            if (!value.isEmpty()) {
                indexed.add(new int[]{idx, values.size()});
                values.add(value);
            }
        }
        indexed.sort(Comparator.comparingInt(a -> a[0]));
        List<String> sorted = new ArrayList<>(indexed.size());
        for (int[] pair : indexed) {
            sorted.add(values.get(pair[1]));
        }
        return sorted;
    }

    public void setEntries(List<String> paths) {
        for (String key : keys()) {
            try {
                Integer.parseInt(key);
            } catch (NumberFormatException e) {
                continue;
            }
            prefs.remove(key);
        }
        int i = 1;
        for (String path : paths) {
            prefs.put(Integer.toString(i++), path);
        }
    }

    private String[] keys() {
        try {
            return prefs.keys();
        } catch (BackingStoreException e) {
            LOGGER.info("Error loading preferences", e);
            return new String[0];
        }
    }
}
