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
package org.apache.cayenne.modeler.pref.migration;

import org.apache.cayenne.modeler.pref.PreferenceMigration;
import org.apache.cayenne.modeler.pref.PreferencesCopier;
import org.apache.cayenne.modeler.pref.PreferencesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Copies legacy column widths / order / sort prefs for the four primary entity
 * tables (ObjAttribute, ObjRelationship, DbAttribute, DbRelationship) into the
 * new {@code app/ui/...} layout. Legacy nodes were derived from
 * {@code userNodeForPackage(<table-model class>).node(<sub-path>)}; the
 * {@code width_*}, {@code order_*}, {@code sort_column}, and {@code sort_order}
 * key formats are identical to the new layout, so a recursive subtree copy
 * suffices. Leaves the legacy nodes intact so an older Modeler installation on
 * the same machine still works.
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
        try {
            if (!Preferences.userRoot().nodeExists(legacyPath)) {
                return;
            }
            legacy = Preferences.userRoot().node(legacyPath);
        } catch (BackingStoreException e) {
            LOGGER.warn("Error checking legacy table prefs node '{}'", legacyPath, e);
            return;
        }

        PreferencesCopier.copy(legacy, repo.uiPref(uiPath));
    }
}
