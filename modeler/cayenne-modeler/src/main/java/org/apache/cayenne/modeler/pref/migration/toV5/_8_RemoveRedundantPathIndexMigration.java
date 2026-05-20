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
 * Removes the write-only {@code app/projectIndex} and {@code app/dataMapIndex}
 * subtrees and the per-node {@code path} key from {@code project/&lt;id&gt;} /
 * {@code datamap/&lt;id&gt;} entries. All three were introduced during the v5
 * Modeler preferences refactor but never read by any code path; their keys
 * fully duplicate the addressing already used by the per-project and
 * per-DataMap subtrees.
 */
public class _8_RemoveRedundantPathIndexMigration implements PreferenceMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(_8_RemoveRedundantPathIndexMigration.class);

    private static final String PATH_KEY = "path";
    private static final String PROJECT_INDEX_NODE = "projectIndex";
    private static final String DATAMAP_INDEX_NODE = "dataMapIndex";
    private static final String PROJECT_ROOT_NODE = "project";
    private static final String DATAMAP_ROOT_NODE = "datamap";

    @Override
    public int version() {
        return 8;
    }

    @Override
    public void apply(PrefsLocator locator) {
        removeIndexNode(locator.appNode(null), PROJECT_INDEX_NODE);
        removeIndexNode(locator.appNode(null), DATAMAP_INDEX_NODE);
        stripPathKey(locator.modelerRoot(), PROJECT_ROOT_NODE);
        stripPathKey(locator.modelerRoot(), DATAMAP_ROOT_NODE);
    }

    private static void removeIndexNode(Preferences appNode, String name) {
        try {
            if (appNode.nodeExists(name)) {
                appNode.node(name).removeNode();
            }
        } catch (BackingStoreException e) {
            LOGGER.warn("Error removing redundant prefs index '{}'", name, e);
        }
    }

    private static void stripPathKey(Preferences modelerRoot, String rootNodeName) {
        try {
            if (!modelerRoot.nodeExists(rootNodeName)) {
                return;
            }
            Preferences root = modelerRoot.node(rootNodeName);
            for (String childName : root.childrenNames()) {
                root.node(childName).remove(PATH_KEY);
            }
        } catch (BackingStoreException e) {
            LOGGER.warn("Error stripping '{}' from prefs under '{}'", PATH_KEY, rootNodeName, e);
        }
    }
}
