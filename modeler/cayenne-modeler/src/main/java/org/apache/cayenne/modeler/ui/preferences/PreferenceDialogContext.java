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

package org.apache.cayenne.modeler.ui.preferences;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.pref.CayenneProjectPreferences;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Holds preferences editing state.
 */
public class PreferenceDialogContext {

    private final Application application;
    private final CayenneProjectPreferences cayenneProjectPreferences;
    private final Map<Preferences, Map<String, String>> changedPreferences;
    private final Map<Preferences, Map<String, String>> removedPreferences;
    private final Map<Preferences, Map<String, Boolean>> changedBooleanPreferences;

    public PreferenceDialogContext(Application application) {
        this.application = application;
        this.cayenneProjectPreferences = application.getCayenneProjectPreferences();
        this.changedPreferences = new HashMap<>();
        this.removedPreferences = new HashMap<>();
        this.changedBooleanPreferences = new HashMap<>();
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

    public void save() {
        cayenneProjectPreferences.getDetailObject(DBConnectionInfo.class).save();

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

        application.initClassLoader();
    }

    public void revert() {
        cayenneProjectPreferences.getDetailObject(DBConnectionInfo.class).cancel();
    }
}
