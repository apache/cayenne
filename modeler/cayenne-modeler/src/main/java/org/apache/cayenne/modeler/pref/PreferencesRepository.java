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
import org.apache.cayenne.modeler.pref.migration.toV5._1_DbConnectorsMigration;
import org.apache.cayenne.modeler.pref.migration.toV5._2_ClasspathMigration;
import org.apache.cayenne.modeler.pref.migration.toV5._3_GeneralPrefsMigration;
import org.apache.cayenne.modeler.pref.migration.toV5._4_RecentProjectsMigration;
import org.apache.cayenne.modeler.pref.migration.toV5._5_FrameGeometryMigration;
import org.apache.cayenne.modeler.pref.migration.toV5._6_ProjectSplitPaneMigration;
import org.apache.cayenne.modeler.pref.migration.toV5._7_EntityTablePrefsMigration;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private static List<PreferenceMigration> toV5Migrations() {
        return Stream.of(
                        new _1_DbConnectorsMigration(),
                        new _2_ClasspathMigration(),
                        new _3_GeneralPrefsMigration(),
                        new _4_RecentProjectsMigration(),
                        new _5_FrameGeometryMigration(),
                        new _6_ProjectSplitPaneMigration(),
                        new _7_EntityTablePrefsMigration())

                // just in case, sort to prevent any ordering issues with manual insertion
                .sorted(Comparator.comparingInt(PreferenceMigration::version))
                .collect(Collectors.toList());
    }

    public PreferencesRepository() {
        this.root = Preferences.userRoot().node(ROOT_PATH);
        this.migrations = toV5Migrations();
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
     * Serializes the entire preferences subtree owned by this repository as a
     * pretty-printed JSON string. Each {@link Preferences} node becomes a JSON
     * object whose own keys map to string values and whose child node names map
     * to nested objects.
     */
    public String exportAsJson() {
        StringBuilder sb = new StringBuilder();
        appendNode(sb, root, 0);
        return sb.toString();
    }

    /**
     * Removes every child node of the repository root, wiping all Modeler
     * preferences. Also clears in-memory unsaved-id bookkeeping so subsequent
     * {@link #projectPref}/{@link #dataMapPref} lookups don't reference deleted
     * subtrees.
     *
     * <p>If {@code importLegacyPreferences} is {@code true}, the wipe includes
     * {@code app/_meta/migrations.appliedVersion}, so {@link #runMigrations()}
     * will re-run every registered migration on next startup and import
     * preferences from earlier Cayenne versions.
     *
     * <p>If {@code false}, after the wipe the repository writes the highest
     * registered migration version under
     * {@code app/_meta/migrations.appliedVersion}, marking all migrations as
     * already applied so legacy preferences are not re-imported on next startup.
     */
    public void resetToDefaults(boolean importLegacyPreferences) {
        try {
            for (String childName : root.childrenNames()) {
                root.node(childName).removeNode();
            }
            root.flush();
        } catch (BackingStoreException e) {
            LOGGER.warn("Error deleting all preferences under '{}'", root.absolutePath(), e);
        }
        unsavedProjectIds.clear();
        unsavedDataMapIds.clear();

        if (!importLegacyPreferences && !migrations.isEmpty()) {
            int highest = migrations.get(migrations.size() - 1).version();
            Preferences meta = appPref(META_NODE);
            meta.putInt(MIGRATIONS_VERSION_KEY, highest);
            try {
                meta.flush();
            } catch (BackingStoreException e) {
                LOGGER.warn("Error flushing migration version under '{}'", meta.absolutePath(), e);
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

    private static void appendNode(StringBuilder sb, Preferences node, int indent) {
        String[] keys;
        String[] children;
        try {
            keys = node.keys();
            children = node.childrenNames();
        } catch (BackingStoreException e) {
            LOGGER.warn("Error reading preferences node '{}'", node.absolutePath(), e);
            sb.append("{}");
            return;
        }

        if (keys.length == 0 && children.length == 0) {
            sb.append("{}");
            return;
        }

        Arrays.sort(keys);
        Arrays.sort(children);

        sb.append("{\n");
        boolean first = true;
        for (String key : keys) {
            if (!first) {
                sb.append(",\n");
            }
            indent(sb, indent + 1);
            appendString(sb, key);
            sb.append(": ");
            appendString(sb, node.get(key, ""));
            first = false;
        }
        for (String childName : children) {
            if (!first) {
                sb.append(",\n");
            }
            indent(sb, indent + 1);
            appendString(sb, childName);
            sb.append(": ");
            appendNode(sb, node.node(childName), indent + 1);
            first = false;
        }
        sb.append('\n');
        indent(sb, indent);
        sb.append('}');
    }

    private static void appendString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    private static void indent(StringBuilder sb, int level) {
        sb.append("  ".repeat(Math.max(0, level)));
    }
}
