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

import org.apache.cayenne.CayenneRuntimeException;

public abstract class CayennePreferenceForProject extends CayennePreference {

    private static List<Preferences> newNode; 
    private static List<Preferences> oldNode;

    public CayennePreferenceForProject(Preferences pref) {
        setCurrentPreference(pref);
    }

    public void copyPreferences(String newName) {

        Preferences oldPref = getCurrentPreference();
        try {
            
            // copy all preferences in this node
            String[] names = oldPref.keys();
            Preferences parent = oldPref.parent();
            Preferences newPref = parent.node(newName);
            for (int i = 0; i < names.length; i++) {
                newPref.put(names[i], oldPref.get(names[i], ""));
            }

            String[] children = oldPref.childrenNames();
            String oldPath = oldPref.absolutePath();
            String newPath = newPref.absolutePath();

            // copy children nodes and its preferences

            ArrayList<Preferences> childrenOldPref = childrenCopy(oldPref, oldPath, newPath);

            while (childrenOldPref.size() > 0) {

                ArrayList<Preferences> childrenPrefTemp = new ArrayList<Preferences>();

                Iterator<Preferences> it = childrenOldPref.iterator();
                while (it.hasNext()) {
                    Preferences child = it.next();
                    ArrayList<Preferences> childArray = childrenCopy(child, oldPath, newPath);

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

            newNode.add(newPref);
            oldNode.add(oldPref);
            setCurrentPreference(newPref);
        }
        catch (BackingStoreException e) {
            new CayenneRuntimeException("Error remane preferences");
        }
    }

    private ArrayList<Preferences> childrenCopy(Preferences pref, String oldPath, String newPath) {

        try {
            String[] children = pref.childrenNames();

            ArrayList<Preferences> prefChild = new ArrayList<Preferences>();

            for (int j = 0; j < children.length; j++) {
                String child = children[j];
                // get old preference
                Preferences childNode = pref.node(child);
                
                // path to node
                String path = childNode.absolutePath().replace(oldPath, newPath);
                
                // copy all preferences in this node
                String[] names = childNode.keys();
                Preferences newPref = Preferences.userRoot().node(path);
                for (int i = 0; i < names.length; i++) {
                    newPref.put(names[i], pref.get(names[i], ""));
                }
                prefChild.add(childNode);
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
            }
            oldNode.clear();
            newNode.clear();
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
            }
            oldNode.clear();
            newNode.clear();
        }
    }
}
