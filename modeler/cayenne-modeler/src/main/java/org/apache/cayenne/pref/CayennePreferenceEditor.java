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
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static Logger logger = LoggerFactory.getLogger(CayennePreferenceEditor.class);

    public CayennePreferenceEditor(CayenneProjectPreferences cayenneProjectPreferences) {
        this.cayenneProjectPreferences = cayenneProjectPreferences;
        this.changedPreferences = new HashMap<>();
        this.removedPreferences = new HashMap<>();
        this.changedBooleanPreferences = new HashMap<>();
        this.removedNode = new ArrayList<>();
        this.addedNode = new ArrayList<>();
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
        for (Map.Entry<Preferences, Map<String, Boolean>> entry : changedBooleanPreferences.entrySet()) {
            Preferences pref = entry.getKey();
            for (Map.Entry<String, Boolean> en : entry.getValue().entrySet()) {
                pref.putBoolean(en.getKey(), en.getValue());
            }
        }

        // update string preferences
        for (Map.Entry<Preferences, Map<String, String>> entry : changedPreferences.entrySet()) {
            Preferences pref = entry.getKey();
            for (Map.Entry<String, String> en : entry.getValue().entrySet()) {
                pref.put(en.getKey(), en.getValue());
            }
        }

        // remove string preferences
        for (Map.Entry<Preferences, Map<String, String>> entry : removedPreferences.entrySet()) {
            Preferences pref = entry.getKey();
            for (Map.Entry<String, String> en : entry.getValue().entrySet()) {
                pref.remove(en.getKey());
            }
        }

        // remove preferences node
        for (Preferences pref : removedNode) {
            try {
                pref.removeNode();
            } catch (BackingStoreException e) {
                logger.warn("Error removing preferences");
            }
        }

        Application.getInstance().initClassLoader();
    }

    public void revert() {
        // remove added preferences node
        for (Preferences pref : addedNode) {
            try {
                pref.removeNode();
            } catch (BackingStoreException ignored) {
            }
        }

        cayenneProjectPreferences.getDetailObject(DBConnectionInfo.class).cancel();
        restartRequired = false;
    }

    protected abstract void restart();
}
