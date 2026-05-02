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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Base class for preferences whose node path embeds a name that can change at runtime
 * (DataChannelDescriptor, DataNodeDescriptor, DataMap, ...). The Java {@link Preferences}
 * API has no native rename, so on rename we copy the entire subtree to the new path and
 * defer cleanup until the user saves or exits.
 * <p>
 * Two process-wide buffers track the speculative rename:
 * <ul>
 *   <li>{@code oldNodes} — original nodes to delete on save (commit)</li>
 *   <li>{@code newNodes} — copied nodes to delete on unsaved exit (rollback)</li>
 * </ul>
 * The buffers are shared across all rename sites because the commit/rollback decision
 * is made far from the rename itself, in {@code SaveAction} and {@code ExitAction}.
 * Access is implicitly single-threaded (Swing EDT).
 */
public abstract class RenamedPreferences {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenamedPreferences.class);

    private static final List<Preferences> newNodes = new ArrayList<>();
    private static final List<Preferences> oldNodes = new ArrayList<>();

    public static Preferences copyPreferences(String newName, Preferences oldPref) {
        Preferences parent = oldPref.parent();
        Preferences newPref = parent.node(newName);
        return copyTracked(newPref, oldPref);
    }

    /**
     * Copies the {@code oldPref} subtree onto {@code newPref} and registers both nodes
     * for deferred cleanup: {@code oldPref} on save, {@code newPref} on unsaved exit.
     */
    public static Preferences copyTracked(Preferences newPref, Preferences oldPref) {
        return copy(newPref, oldPref, true);
    }

    /**
     * Copies the {@code oldPref} subtree onto {@code newPref} without registering either
     * node for deferred cleanup. Used by Save As, where the caller manages cleanup itself.
     */
    public static Preferences copyUntracked(Preferences newPref, Preferences oldPref) {
        return copy(newPref, oldPref, false);
    }

    public static void removeOldPreferences() {
        for (Preferences pref : oldNodes) {
            removeNodeQuietly(pref);
        }
        clearPreferences();
    }

    public static void removeNewPreferences() {
        for (Preferences pref : newNodes) {
            removeNodeQuietly(pref);
        }
        clearPreferences();
    }

    public static void clearPreferences() {
        oldNodes.clear();
        newNodes.clear();
    }

    protected Preferences pref;

    public RenamedPreferences(Preferences pref) {
        this.pref = pref;
    }

    public Preferences getPref() {
        return pref;
    }

    public void copyPreferences(String newName) {
        this.pref = copyPreferences(newName, pref);
    }

    private static Preferences copy(Preferences newPref, Preferences oldPref, boolean track) {
        try {
            String[] names = oldPref.keys();
            for (String name : names) {
                newPref.put(name, oldPref.get(name, ""));
            }

            String oldPath = oldPref.absolutePath();
            String newPath = newPref.absolutePath();

            List<Preferences> childrenOldPref = childrenCopy(oldPref, oldPath, newPath);
            while (!childrenOldPref.isEmpty()) {
                List<Preferences> childrenPrefTemp = new ArrayList<>();
                for (Preferences child : childrenOldPref) {
                    childrenPrefTemp.addAll(childrenCopy(child, oldPath, newPath));
                }
                childrenOldPref = childrenPrefTemp;
            }

            if (track) {
                if (!containsByPath(newNodes, newPref)) {
                    newNodes.add(newPref);
                }
                if (!containsByPath(oldNodes, oldPref)) {
                    oldNodes.add(oldPref);
                }
            }

            return newPref;
        } catch (BackingStoreException e) {
            throw new RuntimeException("Error renaming preferences", e);
        }
    }

    private static List<Preferences> childrenCopy(Preferences pref, String oldPath, String newPath) {
        try {
            String[] children = pref.childrenNames();
            List<Preferences> prefChild = new ArrayList<>();

            for (String child : children) {
                Preferences childNode = pref.node(child);

                if (!containsByPath(oldNodes, childNode)) {
                    String path = childNode.absolutePath().replace(oldPath, newPath);

                    String[] names = childNode.keys();
                    Preferences newPref = Preferences.userRoot().node(path);
                    for (String name : names) {
                        newPref.put(name, childNode.get(name, ""));
                    }
                    prefChild.add(newPref);
                }
            }

            return prefChild;
        } catch (BackingStoreException e) {
            LOGGER.warn("Error reading preference children at '{}'", pref.absolutePath(), e);
            return Collections.emptyList();
        }
    }

    private static void removeNodeQuietly(Preferences pref) {
        try {
            pref.removeNode();
        } catch (BackingStoreException | IllegalStateException e) {
            LOGGER.warn("Error removing preference node '{}'", safePath(pref), e);
        }
    }

    private static String safePath(Preferences pref) {
        try {
            return pref.absolutePath();
        } catch (IllegalStateException e) {
            return "<removed>";
        }
    }

    private static boolean containsByPath(List<Preferences> list, Preferences pref) {
        String path = pref.absolutePath();
        for (Preferences next : list) {
            if (next.absolutePath().equals(path)) {
                return true;
            }
        }
        return false;
    }
}
