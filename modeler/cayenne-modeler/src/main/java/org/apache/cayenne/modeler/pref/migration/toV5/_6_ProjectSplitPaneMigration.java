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
import org.apache.cayenne.modeler.pref.PreferencesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Copies the legacy project tree / editor splitter position from
 * {@code org/apache/cayenne/modeler/editor/splitPane/divider} into the new
 * {@code app/ui/project/splitPane} layout. Leaves the legacy node intact so an
 * older Modeler installation on the same machine still works.
 */
public class _6_ProjectSplitPaneMigration implements PreferenceMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(_6_ProjectSplitPaneMigration.class);

    private static final String LEGACY_PATH = "org/apache/cayenne/modeler/editor/splitPane/divider";

    private static final String DIVIDER_LOCATION = "dividerLocation";

    @Override
    public int version() {
        return 6;
    }

    @Override
    public void apply(PreferencesRepository repo) {
        Preferences legacy;
        try {
            if (!Preferences.userRoot().nodeExists(LEGACY_PATH)) {
                return;
            }
            legacy = Preferences.userRoot().node(LEGACY_PATH);
        } catch (BackingStoreException e) {
            LOGGER.warn("Error checking legacy project splitPane node", e);
            return;
        }

        int dividerLocation = legacy.getInt(DIVIDER_LOCATION, Integer.MIN_VALUE);
        if (dividerLocation != Integer.MIN_VALUE) {
            repo.uiPref("project/splitPane").putInt(DIVIDER_LOCATION, dividerLocation);
        }
    }
}
