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
 * Copies legacy column widths / order / sort prefs for the four primary entity
 * tables (ObjAttribute, ObjRelationship, DbAttribute, DbRelationship) into the
 * new {@code app/ui/...} layout. Legacy keys used snake_case (sort_column,
 * sort_order, width_&lt;n&gt;, order_&lt;n&gt;); the new layout uses camelCase
 * (sortColumn, sortOrder, colWidth&lt;n&gt;, colOrder&lt;n&gt;), so each key is
 * renamed during the copy. Leaves the legacy nodes intact so an older Modeler
 * installation on the same machine still works.
 */
public class _7_EntityTablePrefsMigration implements PreferenceMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(_7_EntityTablePrefsMigration.class);

    private static final String[][] PATHS = {
            // {legacyAbsolutePath, newRelativeUiPath}
            {"org/apache/cayenne/modeler/editor/objEntity/attributeTable", "objEntity/attributeTable"},
            {"org/apache/cayenne/modeler/editor/objEntity/relationshipTable", "objEntity/relationshipTable"},
            {"org/apache/cayenne/modeler/editor/dbentity/attributeTable", "dbEntity/attributeTable"},
            {"org/apache/cayenne/modeler/editor/dbentity/relationshipTable", "dbEntity/relationshipTable"},
    };

    private static final String LEGACY_SORT_COLUMN = "sort_column";
    private static final String LEGACY_SORT_ORDER = "sort_order";
    private static final String LEGACY_WIDTH_PREFIX = "width_";
    private static final String LEGACY_ORDER_PREFIX = "order_";

    @Override
    public int version() {
        return 7;
    }

    @Override
    public void apply(PreferencesRepository repo) {
        for (String[] pair : PATHS) {
            copyTable(repo, pair[0], pair[1]);
        }
    }

    private static void copyTable(PreferencesRepository repo, String legacyPath, String uiPath) {
        Preferences legacy;
        String[] legacyKeys;
        try {
            if (!Preferences.userRoot().nodeExists(legacyPath)) {
                return;
            }
            legacy = Preferences.userRoot().node(legacyPath);
            legacyKeys = legacy.keys();
        } catch (BackingStoreException e) {
            LOGGER.warn("Error checking legacy table prefs node '{}'", legacyPath, e);
            return;
        }

        Preferences target = repo.uiPref(uiPath);
        for (String key : legacyKeys) {
            switch (key) {
                case LEGACY_SORT_COLUMN:
                    target.putInt("sortColumn", legacy.getInt(key, 0));
                    break;
                case LEGACY_SORT_ORDER:
                    target.putBoolean("sortOrder", legacy.getBoolean(key, true));
                    break;
                default:
                    if (key.startsWith(LEGACY_WIDTH_PREFIX)) {
                        int v = legacy.getInt(key, -1);
                        if (v >= 0) {
                            target.putInt("colWidth" + key.substring(LEGACY_WIDTH_PREFIX.length()), v);
                        }
                    } else if (key.startsWith(LEGACY_ORDER_PREFIX)) {
                        int v = legacy.getInt(key, -1);
                        if (v >= 0) {
                            target.putInt("colOrder" + key.substring(LEGACY_ORDER_PREFIX.length()), v);
                        }
                    }
                    // unknown legacy key — silently skip
                    break;
            }
        }
    }
}
