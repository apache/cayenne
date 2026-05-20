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

import org.apache.cayenne.modeler.pref.PreferenceMigration;
import org.apache.cayenne.modeler.pref.PrefsLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Copies legacy main-frame geometry (width / height / x / y) from
 * {@code org/apache/cayenne/modeler} into the new {@code app/ui/frame/geometry}
 * layout. The legacy node is the {@code userNodeForPackage} of the modeler
 * package and may hold unrelated keys, so only the four geometry keys are
 * copied. Leaves the legacy node intact so an older Modeler installation on the
 * same machine still works.
 */
public class _5_FrameGeometryMigration implements PreferenceMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(_5_FrameGeometryMigration.class);

    private static final String LEGACY_PATH = "org/apache/cayenne/modeler";

    @Override
    public int version() {
        return 5;
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
            LOGGER.warn("Error checking legacy frame geometry node", e);
            return;
        }

        Preferences target = locator.appNode("ui").node("frame/geometry");
        copyInt(legacy, target, "width");
        copyInt(legacy, target, "height");
        copyInt(legacy, target, "x");
        copyInt(legacy, target, "y");
    }

    private static void copyInt(Preferences src, Preferences dst, String key) {
        int value = src.getInt(key, Integer.MIN_VALUE);
        if (value != Integer.MIN_VALUE) {
            dst.putInt(key, value);
        }
    }
}
