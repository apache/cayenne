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

import org.apache.cayenne.modeler.pref.ClasspathPrefs;
import org.apache.cayenne.modeler.pref.PreferenceMigration;
import org.apache.cayenne.modeler.pref.PreferencesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Copies legacy classpath entries from {@code org/apache/cayenne/modeler/dialog/pref}
 * into the new {@code app/classpath} layout. The legacy node was shared with other
 * dialogs, so only numeric keys (the format used by the old ClasspathPreferences) are
 * copied. Leaves the legacy node intact so an older Modeler installation on the same
 * machine still works.
 */
public class _2_ClasspathMigration implements PreferenceMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(_2_ClasspathMigration.class);

    private static final String LEGACY_PATH = "org/apache/cayenne/modeler/dialog/pref";

    @Override
    public int version() {
        return 2;
    }

    @Override
    public void apply(PreferencesRepository repo) {
        Preferences legacy;
        String[] legacyKeys;
        try {
            if (!Preferences.userRoot().nodeExists(LEGACY_PATH)) {
                return;
            }
            legacy = Preferences.userRoot().node(LEGACY_PATH);
            legacyKeys = legacy.keys();
        } catch (BackingStoreException e) {
            LOGGER.warn("Error checking legacy classpath node", e);
            return;
        }

        Preferences target = repo.appPref(ClasspathPrefs.NODE);
        for (String key : legacyKeys) {
            try {
                Integer.parseInt(key);
            } catch (NumberFormatException e) {
                continue;
            }
            String value = legacy.get(key, "");
            if (!value.isEmpty()) {
                target.put(key, value);
            }
        }
    }
}
