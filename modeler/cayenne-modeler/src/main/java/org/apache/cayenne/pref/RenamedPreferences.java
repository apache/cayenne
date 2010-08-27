/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.pref;

import java.util.ArrayList;
import java.util.Iterator;
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
        setCurrentPreference(pref);
    }

    public void copyPreferences(String newName) {
        setCurrentPreference(copyPreferences(newName, getCurrentPreference()));
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

            ArrayList<Preferences> prefChild = new ArrayList<Preferences>();

            for (int j = 0; j < children.length; j++) {
                String child = children[j];
                // get old preference
                Preferences childNode = pref.node(child);

                if (!equalsPath(oldNode, childNode)) {
                    // path to node
                    String path = childNode.absolutePath().replace(oldPath, newPath);

                    // copy all preferences in this node
                    String[] names = childNode.keys();
                    Preferences newPref = Preferences.userRoot().node(path);
                    for (int i = 0; i < names.length; i++) {
                        newPref.put(names[i], childNode.get(names[i], ""));
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
            Iterator<Preferences> it = oldNode.iterator();

            while (it.hasNext()) {
                Preferences pref = it.next();
                try {
                    pref.removeNode();
                }
                catch (BackingStoreException e) {
                }
                catch (IllegalStateException e) {
                    // do nothing
                }
            }
            clearPreferences();
        }

    }

    public static void removeNewPreferences() {
        if (newNode != null) {
            Iterator<Preferences> it = newNode.iterator();

            while (it.hasNext()) {
                Preferences pref = it.next();

                try {
                    pref.removeNode();
                }
                catch (BackingStoreException e) {
                }
                catch (IllegalStateException e) {
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

            for (int i = 0; i < names.length; i++) {
                newPref.put(names[i], oldPref.get(names[i], ""));
            }

            String oldPath = oldPref.absolutePath();
            String newPath = newPref.absolutePath();

            // copy children nodes and its preferences
            ArrayList<Preferences> childrenOldPref = childrenCopy(
                    oldPref,
                    oldPath,
                    newPath);

            while (childrenOldPref.size() > 0) {

                ArrayList<Preferences> childrenPrefTemp = new ArrayList<Preferences>();

                Iterator<Preferences> it = childrenOldPref.iterator();
                while (it.hasNext()) {
                    Preferences child = it.next();
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
                newNode = new ArrayList<Preferences>();
            }
            if (oldNode == null) {
                oldNode = new ArrayList<Preferences>();
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
            throw new PreferenceException("Error remane preferences");
        }
    }

    private static boolean equalsPath(List<Preferences> listPref, Preferences pref) {
        if (listPref != null) {
            Iterator<Preferences> it = listPref.iterator();
            while (it.hasNext()) {
                Preferences next = it.next();
                String pathInList = (String) next.absolutePath();
                String path = (String) pref.absolutePath();
                if (pathInList.equals(path)) {
                    return true;
                }
            }
        }
        return false;
    }
}
