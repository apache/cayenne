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

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.pref.migration.DbConnectorsMigration;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * App-wide preferences service for the Modeler. Owns the layout under
 * {@code org/apache/cayenne/modeler/v5} and resolves project / DataMap nodes
 * from their on-disk file paths (or stable per-runtime ids while still unsaved).
 */
public class PreferencesRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesRepository.class);

    private static final String VERSION = "v5";
    private static final String ROOT_PATH = "org/apache/cayenne/modeler/" + VERSION;

    static final String APP_NODE = "app";
    static final String PROJECT_NODE = "project";
    static final String DATAMAP_NODE = "datamap";

    static final String META_NODE = "_meta";
    static final String UI_NODE = "ui";
    static final String PROJECT_INDEX_NODE = "projectIndex";
    static final String DATAMAP_INDEX_NODE = "dataMapIndex";

    static final String MIGRATIONS_VERSION_KEY = "migrations.appliedVersion";
    static final String PATH_KEY = "path";
    static final String UNSAVED_PREFIX = "unsaved-";

    private final Preferences root;
    private final Map<Project, String> unsavedProjectIds;
    private final Map<DataMap, String> unsavedDataMapIds;
    private final List<PreferenceMigration> migrations;

    private static List<PreferenceMigration> defaultMigrations() {
        return List.of(new DbConnectorsMigration());
    }

    public PreferencesRepository() {
        this.root = Preferences.userRoot().node(ROOT_PATH);

        this.migrations = new ArrayList<>(defaultMigrations());
        this.migrations.sort(Comparator.comparingInt(PreferenceMigration::version));

        this.unsavedProjectIds = new IdentityHashMap<>();
        this.unsavedDataMapIds = new IdentityHashMap<>();
    }

    public Preferences appPref(String relativePath) {
        Preferences appRoot = root.node(APP_NODE);
        return relativePath == null || relativePath.isEmpty() ? appRoot : appRoot.node(relativePath);
    }

    /**
     * Per-component UI preferences node, addressed by a short stable path
     * chosen by the caller (e.g. {@code "splitPane/templateEditor"}). Pass
     * {@code null} or an empty string to get the {@code ui} base node itself.
     */
    public Preferences uiPref(String relativePath) {
        Preferences uiRoot = appPref(UI_NODE);
        return relativePath == null || relativePath.isEmpty() ? uiRoot : uiRoot.node(relativePath);
    }

    /**
     * Returns the preferences node for the given {@link Project}, optionally
     * descending into a {@code relativePath} sub-tree within it. If the project
     * has been saved to disk, the project's subtree is keyed by a stable hash
     * of its configuration file path; otherwise by a per-runtime
     * {@code unsaved-<ts>} id assigned on first lookup. Pass {@code null} or
     * an empty {@code relativePath} to get the project base node itself.
     */
    public Preferences projectPref(Project project, String relativePath) {
        String id = projectId(project);
        Preferences node = root.node(PROJECT_NODE).node(id);
        recordPath(node, projectPath(project), PROJECT_INDEX_NODE, id);
        return relativePath == null || relativePath.isEmpty() ? node : node.node(relativePath);
    }

    /**
     * Returns the preferences node for the given {@link DataMap}, optionally
     * descending into a {@code relativePath} sub-tree within it. Like
     * {@link #projectPref(Project, String)}, the DataMap's subtree is keyed by
     * file path when saved or by a {@code unsaved-<ts>} id otherwise. Pass
     * {@code null} or an empty {@code relativePath} to get the DataMap base
     * node itself.
     */
    public Preferences dataMapPref(DataMap map, String relativePath) {
        String id = dataMapId(map);
        Preferences node = root.node(DATAMAP_NODE).node(id);
        recordPath(node, dataMapPath(map), DATAMAP_INDEX_NODE, id);
        return relativePath == null || relativePath.isEmpty() ? node : node.node(relativePath);
    }

    private String projectId(Project project) {
        String path = projectPath(project);
        if (path != null) {
            return PreferenceNodeIds.idForPath(path);
        }
        return unsavedProjectIds.computeIfAbsent(project, p -> newUnsavedId());
    }

    private String dataMapId(DataMap map) {
        String path = dataMapPath(map);
        if (path != null) {
            return PreferenceNodeIds.idForPath(path);
        }
        return unsavedDataMapIds.computeIfAbsent(map, m -> newUnsavedId());
    }

    /**
     * Reconciles preferences after a project save. For the project and every
     * DataMap, if the current file-path-derived id differs from where its
     * preferences subtree currently lives (an unsaved id, or a different saved
     * id from a previous Save As), copies the subtree to the new id and removes
     * the old. Updates the {@code projectIndex} / {@code dataMapIndex}
     * reverse-lookup nodes. Idempotent.
     */
    public void commitProject(Project project) {
        if (project == null) {
            return;
        }
        reconcileProject(project);

        DataChannelDescriptor descriptor = (DataChannelDescriptor) project.getRootNode();
        if (descriptor != null) {
            for (DataMap map : descriptor.getDataMaps()) {
                reconcileDataMap(map);
            }
        }
    }

    /**
     * Idempotent. Applies any registered {@link PreferenceMigration}s whose
     * version exceeds {@code app/_meta/migrations.appliedVersion}.
     */
    public void runMigrations() {
        Preferences meta = appPref(META_NODE);
        int applied = meta.getInt(MIGRATIONS_VERSION_KEY, 0);
        int max = applied;
        for (PreferenceMigration m : migrations) {
            if (m.version() > applied) {
                try {
                    m.apply(this);
                } catch (RuntimeException e) {
                    LOGGER.warn("Migration v{} failed: {}", m.version(), e.getMessage(), e);
                }
                if (m.version() > max) {
                    max = m.version();
                }
            }
        }
        if (max > applied) {
            meta.putInt(MIGRATIONS_VERSION_KEY, max);
        }
    }

    private void reconcileProject(Project project) {
        String currentPath = projectPath(project);
        if (currentPath == null) {
            return;
        }
        String savedId = PreferenceNodeIds.idForPath(currentPath);
        String oldId = unsavedProjectIds.remove(project);
        if (oldId != null && !oldId.equals(savedId)) {
            relocate(PROJECT_NODE, oldId, savedId, PROJECT_INDEX_NODE, currentPath);
        } else {
            recordPath(root.node(PROJECT_NODE).node(savedId), currentPath, PROJECT_INDEX_NODE, savedId);
        }
    }

    private void reconcileDataMap(DataMap map) {
        String currentPath = dataMapPath(map);
        if (currentPath == null) {
            return;
        }
        String savedId = PreferenceNodeIds.idForPath(currentPath);
        String oldId = unsavedDataMapIds.remove(map);
        if (oldId != null && !oldId.equals(savedId)) {
            relocate(DATAMAP_NODE, oldId, savedId, DATAMAP_INDEX_NODE, currentPath);
        } else {
            recordPath(root.node(DATAMAP_NODE).node(savedId), currentPath, DATAMAP_INDEX_NODE, savedId);
        }
    }

    private void relocate(String parentNode, String oldId, String newId, String indexNode, String path) {
        Preferences src = root.node(parentNode).node(oldId);
        Preferences dst = root.node(parentNode).node(newId);
        PreferencesCopier.move(src, dst);
        recordPath(dst, path, indexNode, newId);
    }

    private void recordPath(Preferences node, String path, String indexNode, String id) {
        if (path == null) {
            return;
        }
        node.put(PATH_KEY, path);
        appPref(indexNode).put(id, path);
    }

    private static String projectPath(Project project) {
        if (project == null) {
            return null;
        }
        Resource resource = project.getConfigurationResource();
        if (resource == null) {
            return null;
        }
        return resource.getURL().getPath();
    }

    private static String dataMapPath(DataMap map) {
        if (map == null) {
            return null;
        }
        Resource resource = map.getConfigurationSource();
        if (resource == null) {
            return null;
        }
        return resource.getURL().getPath();
    }

    private static String newUnsavedId() {
        return UNSAVED_PREFIX + System.currentTimeMillis() + "-" + System.nanoTime();
    }
}
