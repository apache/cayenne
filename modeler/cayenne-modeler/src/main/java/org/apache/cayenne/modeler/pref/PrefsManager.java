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
import org.apache.cayenne.modeler.pref.migration.toV5._8_RemoveRedundantPathIndexMigration;
import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * A stateful preferences manager for the Application. Handles preference versioning, DataMap and project renaming,
 * locating preference nodes under the common root, etc.
 */
public class PrefsManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrefsManager.class);

    private static final String UNSAVED_PREFIX = "unsaved-";

    private static final String UI_NODE = "ui";
    private static final String META_NODE = "_meta";

    private static final String MIGRATIONS_VERSION_KEY = "migrationsAppliedVersion";

    private final ConfigurationNameMapper nameMapper;
    private final List<PreferenceMigration> migrations;

    private final PrefsLocator locator;
    private final Map<Project, String> newProjectIds;
    private final Map<DataMap, String> newDataMapIds;
    private final Map<String, String> stagingDataMap;
    private final Map<String, String> stagingProject;

    private static List<PreferenceMigration> toV5Migrations() {
        return Stream.of(
                        new _1_DbConnectorsMigration(),
                        new _2_ClasspathMigration(),
                        new _3_GeneralPrefsMigration(),
                        new _4_RecentProjectsMigration(),
                        new _5_FrameGeometryMigration(),
                        new _6_ProjectSplitPaneMigration(),
                        new _7_EntityTablePrefsMigration(),
                        new _8_RemoveRedundantPathIndexMigration())

                // just in case, sort to prevent any ordering issues with manual insertion
                .sorted(Comparator.comparingInt(PreferenceMigration::version))
                .collect(Collectors.toList());
    }

    public PrefsManager(ConfigurationNameMapper nameMapper, PrefsLocator locator) {
        this.nameMapper = nameMapper;
        this.locator = locator;
        this.migrations = toV5Migrations();
        this.newProjectIds = new IdentityHashMap<>();
        this.newDataMapIds = new IdentityHashMap<>();
        this.stagingDataMap = new HashMap<>();
        this.stagingProject = new HashMap<>();
    }

    /**
     * Returns the preferences node for the given {@link Project}, optionally descending into a subtree within it.
     */
    public Preferences projectPref(Project project, String relativePath) {
        String path = projectPath(project);
        String id = path != null
                ? PreferenceNodeIds.idForPath(path)
                : newProjectIds.computeIfAbsent(project, p -> newUnsavedId());
        Preferences node = locator.projectNode(id);
        return relativePath == null || relativePath.isEmpty() ? node : node.node(relativePath);
    }

    /**
     * Returns a node under {@code v5/app/ui}, optionally descending into a subtree.
     * Pass {@code null} or empty for the {@code ui} node itself. This is the conventional
     * home for UI-state preferences (frame geometry, split-pane positions, table columns,
     * file-chooser last-dirs) — anything not tied to a specific project or DataMap.
     */
    public Preferences uiNode(String relativePath) {
        Preferences uiRoot = locator.appNode(UI_NODE);
        return relativePath == null || relativePath.isEmpty() ? uiRoot : uiRoot.node(relativePath);
    }

    /**
     * Returns the preferences node for the given {@link DataMap}, optionally descending into a subtree within it.
     */
    public Preferences dataMapPref(DataMap map, String relativePath) {
        String path = dataMapPath(map);
        String id = path != null
                ? PreferenceNodeIds.idForPath(path)
                : newDataMapIds.computeIfAbsent(map, m -> newUnsavedId());
        Preferences node = locator.dataMapNode(id);
        return relativePath == null || relativePath.isEmpty() ? node : node.node(relativePath);
    }

    /**
     * Reconciles preferences after a project save.
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

        resetTransientState();
    }

    /**
     * Records a pending DataMap rename so that {@link #commitProject(Project)} can migrate
     * the DataMap's preferences subtree from the old node to the new one.
     */
    public void stageDataMapRename(DataMap map, String newName) {
        String oldPath = dataMapPath(map);
        if (oldPath == null) {
            return;
        }
        int slash = oldPath.lastIndexOf('/');
        String dir = slash >= 0 ? oldPath.substring(0, slash + 1) : "";
        String newPath = dir + nameMapper.configurationLocation(DataMap.class, newName);
        stagingDataMap.put(PreferenceNodeIds.idForPath(newPath), oldPath);
    }

    /**
     * Records a pending project rename (domain name change) so that {@link #commitProject(Project)} can migrate project
     * preferences from the old node to the new one.
     */
    public void stageProjectRename(Project project, String newDomainName) {
        String oldPath = projectPath(project);
        if (oldPath == null) {
            return;
        }
        int slash = oldPath.lastIndexOf('/');
        String dir = slash >= 0 ? oldPath.substring(0, slash + 1) : "";
        String newPath = dir + nameMapper.configurationLocation(DataChannelDescriptor.class, newDomainName);
        stagingProject.put(PreferenceNodeIds.idForPath(newPath), oldPath);
    }

    /**
     * Records a pending DataMap move (project Save As) so that preferences are carried to
     * the new location. Must be called before the save writes updated paths to disk.
     */
    public void stageDataMapMove(DataMap map, File newProjectDir) {
        String oldPath = dataMapPath(map);
        if (oldPath == null) {
            return;
        }
        String newPath = new File(newProjectDir, nameMapper.configurationLocation(DataMap.class, map.getName())).getPath();
        stagingDataMap.put(PreferenceNodeIds.idForPath(newPath), oldPath);
    }

    /**
     * Records a pending project move (Save As) so that project-level preferences are
     * carried to the new location. Must be called before the save writes updated paths.
     */
    public void stageProjectMove(Project project, File newProjectDir) {
        String oldPath = projectPath(project);
        if (oldPath == null) {
            return;
        }
        String fileName = new File(oldPath).getName();
        String newPath = new File(newProjectDir, fileName).getPath();
        stagingProject.put(PreferenceNodeIds.idForPath(newPath), oldPath);
    }

    /**
     * Resets all transient per-project state: in-memory unsaved-id bookkeeping and
     * persisted staging records. Call on both project open and project close.
     */
    public void resetTransientState() {
        newProjectIds.clear();
        newDataMapIds.clear();
        stagingDataMap.clear();
        stagingProject.clear();
    }

    /**
     * Removes every child node of the repository root, wiping all Modeler
     * preferences. Also clears in-memory unsaved-id bookkeeping so subsequent
     * {@link #projectPref}/{@link #dataMapPref} lookups don't reference deleted
     * subtrees.
     *
     * <p>If {@code importLegacyPreferences} is {@code true}, the wipe includes
     * {@code app/_meta/migrationsAppliedVersion}, so {@link #runMigrations()}
     * will re-run every registered migration on next startup and import
     * preferences from earlier Cayenne versions.
     *
     * <p>If {@code false}, after the wipe the repository writes the highest
     * registered migration version under
     * {@code app/_meta/migrationsAppliedVersion}, marking all migrations as
     * already applied so legacy preferences are not re-imported on next startup.
     */
    public void resetToDefaults(boolean importLegacyPreferences) {
        Preferences root = locator.modelerRoot();
        try {
            for (String childName : root.childrenNames()) {
                root.node(childName).removeNode();
            }
            root.flush();
        } catch (BackingStoreException e) {
            LOGGER.warn("Error deleting all preferences under '{}'", root.absolutePath(), e);
        }
        newProjectIds.clear();
        newDataMapIds.clear();

        if (!importLegacyPreferences && !migrations.isEmpty()) {
            int highest = migrations.getLast().version();
            Preferences meta = locator.appNode(META_NODE);
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
     * version exceeds {@code app/_meta/migrationsAppliedVersion}.
     */
    public void runMigrations() {
        Preferences meta = locator.appNode(META_NODE);
        int applied = meta.getInt(MIGRATIONS_VERSION_KEY, 0);
        int max = applied;
        for (PreferenceMigration m : migrations) {
            if (m.version() > applied) {
                try {
                    m.apply(locator);
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

        String stagedOldPath = stagingProject.get(savedId);
        if (stagedOldPath != null) {
            String oldId = PreferenceNodeIds.idForPath(stagedOldPath);
            if (!oldId.equals(savedId)) {
                relocate(locator::projectNode, oldId, savedId);
            }
            return;
        }

        String oldId = newProjectIds.remove(project);
        if (oldId != null && !oldId.equals(savedId)) {
            relocate(locator::projectNode, oldId, savedId);
        }
    }

    private void reconcileDataMap(DataMap map) {
        String currentPath = dataMapPath(map);
        if (currentPath == null) {
            return;
        }
        String savedId = PreferenceNodeIds.idForPath(currentPath);

        String stagedOldPath = stagingDataMap.get(savedId);
        if (stagedOldPath != null) {
            String oldId = PreferenceNodeIds.idForPath(stagedOldPath);
            if (!oldId.equals(savedId)) {
                relocate(locator::dataMapNode, oldId, savedId);
            }
            return;
        }

        String oldId = newDataMapIds.remove(map);
        if (oldId != null && !oldId.equals(savedId)) {
            relocate(locator::dataMapNode, oldId, savedId);
        }
    }

    private static void relocate(Function<String, Preferences> nodeForId, String oldId, String newId) {
        PreferencesCopier.move(nodeForId.apply(oldId), nodeForId.apply(newId));
    }

    private static String projectPath(Project project) {
        if (project == null) {
            return null;
        }

        Resource resource = project.getConfigurationResource();
        return resource == null ? null : resource.getURL().getPath();
    }

    private static String dataMapPath(DataMap map) {
        if (map == null) {
            return null;
        }

        Resource resource = map.getConfigurationSource();
        return resource == null ? null : resource.getURL().getPath();
    }

    private static String newUnsavedId() {
        return UNSAVED_PREFIX + System.currentTimeMillis() + "-" + System.nanoTime();
    }
}
