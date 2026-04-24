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

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * use for preferences where in preference's node path contains dependence from name dataChanelDescriptor, 
 * dataNodeDescriptor, dataMap etc.
 */
public abstract class RenamedPreferences extends CayennePreference {

    private static List<Preferences> newNode;
    private static List<Preferences> oldNode;

    public RenamedPreferences(Preferences pref) {
        this.currentPreference = pref;
    }

    public void copyPreferences(String newName) {
        this.currentPreference = copyPreferences(newName, getCurrentPreference());
    }

    public static Preferences copyPreferences(String newName, Preferences oldPref) {

        Preferences parent = oldPref.parent();
        Preferences newPref = parent.node(newName);
        return copyPreferences(newPref, oldPref, true);
    }

    private static ArrayList<Preferences> childrenCopy(
            Preferences pref,
            String oldPath,
            String newPath) {

        try {
            String[] children = pref.childrenNames();

            ArrayList<Preferences> prefChild = new ArrayList<>();

            for (String child : children) {
                // get old preference
                Preferences childNode = pref.node(child);

                if (!equalsPath(oldNode, childNode)) {
                    // path to node
                    String path = childNode.absolutePath().replace(oldPath, newPath);

                    // copy all preferences in this node
                    String[] names = childNode.keys();
                    Preferences newPref = Preferences.userRoot().node(path);
                    for (String name : names) {
                        newPref.put(name, childNode.get(name, ""));
                    }
                    prefChild.add(newPref);
                }
            }

            return prefChild;
        }
        catch (BackingStoreException e) {
        }
        return null;
    }

    public static void removeOldPreferences() {
        if (oldNode != null) {

            for (Preferences pref : oldNode) {
                try {
                    pref.removeNode();
                } catch (BackingStoreException e) {
                } catch (IllegalStateException e) {
                    // do nothing
                }
            }
            clearPreferences();
        }

    }

    public static void removeNewPreferences() {
        if (newNode != null) {

            for (Preferences pref : newNode) {
                try {
                    pref.removeNode();
                } catch (BackingStoreException e) {
                } catch (IllegalStateException e) {
                    // do nothing
                }
            }
            clearPreferences();
        }
    }

    public static void clearPreferences() {
        oldNode.clear();
        newNode.clear();
    }

    public static Preferences copyPreferences(
            Preferences newPref,
            Preferences oldPref,
            boolean addToPreferenceList) {

        try {
            // copy all preferences in this node
            String[] names = oldPref.keys();

            for (String name : names) {
                newPref.put(name, oldPref.get(name, ""));
            }

            String oldPath = oldPref.absolutePath();
            String newPath = newPref.absolutePath();

            // copy children nodes and its preferences
            ArrayList<Preferences> childrenOldPref = childrenCopy(
                    oldPref,
                    oldPath,
                    newPath);

            while (!childrenOldPref.isEmpty()) {

                ArrayList<Preferences> childrenPrefTemp = new ArrayList<>();

                for (Preferences child : childrenOldPref) {
                    ArrayList<Preferences> childArray = childrenCopy(
                            child,
                            oldPath,
                            newPath);

                    childrenPrefTemp.addAll(childArray);
                }

                childrenOldPref.clear();
                childrenOldPref.addAll(childrenPrefTemp);
            }

            if (newNode == null) {
                newNode = new ArrayList<>();
            }
            if (oldNode == null) {
                oldNode = new ArrayList<>();
            }

            if (addToPreferenceList) {
                if (!equalsPath(newNode, newPref)) {
                    newNode.add(newPref);
                }
                if (!equalsPath(oldNode, oldPref)) {
                    oldNode.add(oldPref);
                }
            }

            return newPref;
        }
        catch (BackingStoreException e) {
            throw new RuntimeException("Error remane preferences");
        }
    }

    private static boolean equalsPath(List<Preferences> listPref, Preferences pref) {
        if (listPref != null) {
            for (Preferences next : listPref) {
                String pathInList = next.absolutePath();
                String path = pref.absolutePath();
                if (pathInList.equals(path)) {
                    return true;
                }
            }
        }
        return false;
    }
}
