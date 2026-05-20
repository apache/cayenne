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

import org.apache.cayenne.modeler.pref.GeneralPrefs;
import org.apache.cayenne.modeler.pref.PreferenceMigration;
import org.apache.cayenne.modeler.pref.PrefsLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Copies legacy general preferences from {@code org/apache/cayenne/modeler/dialog/pref}
 * into the new {@code app/general} layout. Leaves the legacy node intact so an older
 * Modeler installation on the same machine still works.
 */
public class _3_GeneralPrefsMigration implements PreferenceMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(_3_GeneralPrefsMigration.class);

    private static final String LEGACY_PATH = "org/apache/cayenne/modeler/dialog/pref";

    @Override
    public int version() {
        return 3;
    }

    @Override
    public void apply(PrefsLocator locator) {
        Preferences legacy;
        try {
            if (!Preferences.userRoot().nodeExists(LEGACY_PATH)) {
                return;
            }
            legacy = Preferences.userRoot().node(LEGACY_PATH);
        } catch (BackingStoreException e) {
            LOGGER.warn("Error checking legacy general prefs node", e);
            return;
        }

        Preferences target = locator.appNode(GeneralPrefs.NODE);
        target.putBoolean(GeneralPrefs.AUTO_LOAD_PROJECT, legacy.getBoolean("autoLoadProject", false));
        String encoding = normalizeEncoding(legacy.get("encoding", ""));
        if (encoding != null) {
            target.put(GeneralPrefs.ENCODING, encoding);
        }
        target.put(GeneralPrefs.FAVOURITE_DATA_SOURCE, legacy.get("favouriteDataSource", ""));

        // legacy used the wrong name - "deletePrompt", for what was meant to be "delete with no prompt"
        target.putBoolean(GeneralPrefs.NO_DELETE_PROMPT, legacy.getBoolean("deletePrompt", false));
    }

    /**
     * Converts a legacy encoding name to its canonical Charset form. Older Modeler
     * versions wrote whatever {@code OutputStreamWriter.getEncoding()} returned —
     * historical aliases like "UTF8" — which no longer match the canonical names
     * ("UTF-8") shown in the encoding dropdown. Returns {@code null} for empty
     * or unrecognized values to signal that nothing should be written to the new
     * prefs node.
     */
    static String normalizeEncoding(String encoding) {
        if (encoding == null || encoding.isEmpty()) {
            return null;
        }
        try {
            return Charset.forName(encoding).name();
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            LOGGER.warn("Dropping unsupported legacy encoding '{}'", encoding);
            return null;
        }
    }
}
