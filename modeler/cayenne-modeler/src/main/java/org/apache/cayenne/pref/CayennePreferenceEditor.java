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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An editor for modifying CayennePreferenceService.
 * 
 */
public abstract class CayennePreferenceEditor implements PreferenceEditor {

    protected boolean restartRequired;
    protected CayenneProjectPreferences cayenneProjectPreferences;
    private Map<Preferences, Map<String, String>> changedPreferences;
    private Map<Preferences, Map<String, String>> removedPreferences;
    private Map<Preferences, Map<String, Boolean>> changedBooleanPreferences;
    private List<Preferences> removedNode;
    private List<Preferences> addedNode;

    private static Log logger = LogFactory.getLog(CayennePreferenceEditor.class);

    public CayennePreferenceEditor(CayenneProjectPreferences cayenneProjectPreferences) {
        this.cayenneProjectPreferences = cayenneProjectPreferences;
        this.changedPreferences = new HashMap<>();
        this.removedPreferences = new HashMap<>();
        this.changedBooleanPreferences = new HashMap<>();
        this.removedNode = new ArrayList<Preferences>();
        this.addedNode = new ArrayList<Preferences>();
    }

    public List<Preferences> getAddedNode() {
        return addedNode;
    }

    public List<Preferences> getRemovedNode() {
        return removedNode;
    }

    public Map<Preferences, Map<String, String>> getRemovedPreferences() {
        return removedPreferences;
    }

    public Map<Preferences, Map<String, String>> getChangedPreferences() {
        return changedPreferences;
    }

    public Map<Preferences, Map<String, Boolean>> getChangedBooleanPreferences() {
        return changedBooleanPreferences;
    }

    protected boolean isRestartRequired() {
        return restartRequired;
    }

    protected void setRestartRequired(boolean restartOnSave) {
        this.restartRequired = restartOnSave;
    }

    public void save() {
        cayenneProjectPreferences.getDetailObject(DBConnectionInfo.class).save();

        if (restartRequired) {
            restart();
        }

        // update boolean preferences
        Iterator it = changedBooleanPreferences.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Preferences pref = (Preferences) entry.getKey();
            Map<String, Boolean> map = (Map<String, Boolean>) entry.getValue();

            Iterator iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry en = (Map.Entry) iterator.next();
                String key = (String) en.getKey();
                Boolean value = (Boolean) en.getValue();

                pref.putBoolean(key, value);
            }
        }

        // update string preferences
        Iterator iter = changedPreferences.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Preferences pref = (Preferences) entry.getKey();
            Map<String, String> map = (Map<String, String>) entry.getValue();

            Iterator iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry en = (Map.Entry) iterator.next();
                String key = (String) en.getKey();
                String value = (String) en.getValue();

                pref.put(key, value);
            }
        }

        // remove string preferences
        Iterator iterator = removedPreferences.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Preferences pref = (Preferences) entry.getKey();
            Map<String, String> map = (Map<String, String>) entry.getValue();

            Iterator itRem = map.entrySet().iterator();
            while (itRem.hasNext()) {
                Map.Entry en = (Map.Entry) itRem.next();
                String key = (String) en.getKey();
                pref.remove(key);
            }
        }

        // remove preferences node
        Iterator<Preferences> iteratorNode = removedNode.iterator();
        while (iteratorNode.hasNext()) {
            Preferences pref = iteratorNode.next();
            try {
                pref.removeNode();
            }
            catch (BackingStoreException e) {
                logger.warn("Error removing preferences");
            }
        }

        Application.getInstance().initClassLoader();
    }

    public void revert() {

        // remove added preferences node
        Iterator<Preferences> iteratorNode = addedNode.iterator();
        while (iteratorNode.hasNext()) {
            Preferences pref = iteratorNode.next();
            try {
                pref.removeNode();
            }
            catch (BackingStoreException e) {
                // do nothing
            }
        }

        cayenneProjectPreferences.getDetailObject(DBConnectionInfo.class).cancel();
        restartRequired = false;
    }

    protected abstract void restart();
}
