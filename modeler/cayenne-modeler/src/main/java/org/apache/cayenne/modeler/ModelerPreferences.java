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

package org.apache.cayenne.modeler;

import org.apache.cayenne.pref.CayennePreference;
import org.apache.cayenne.pref.Preference;
import org.apache.cayenne.pref.UpgradeCayennePreference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

/**
 * ModelerPreferences class supports persistent user preferences. Preferences are saved in
 * the user home directory in "<code>$HOME/.cayenne/modeler.preferences</code>" file.
 * <p>
 * <i>This class is obsolete; its users will be migrated to use preference service. </i>
 * </p>
 */
public class ModelerPreferences implements PreferenceChangeListener {

    private static Preferences cayennePrefs;

    private static final Log logObj = LogFactory.getLog(ModelerPreferences.class);

    /** Name of the log file. */
    public static final String LOGFILE_NAME = "modeler.log";

    /** List of the last 12 opened project files. */
    public static final int LAST_PROJ_FILES_SIZE = 12;

    /** Log file */
    public static final String EDITOR_LOGFILE_ENABLED = "logfileEnabled";
    public static final String EDITOR_LOGFILE = "logfile";

    /**
     * Number of items in combobox visible without scrolling
     */
    public static final int COMBOBOX_MAX_VISIBLE_SIZE = 12;

    /**
     * Returns Cayenne preferences singleton.
     */
    public static Preferences getPreferences() {
        if (cayennePrefs == null) {
            Preference decoratedPref = new UpgradeCayennePreference(new CayennePreference());
            cayennePrefs = decoratedPref.getCayennePreference();
            cayennePrefs.addPreferenceChangeListener(new ModelerPreferences());
        }
        return cayennePrefs;
    }

    public static Preferences getEditorPreferences() {
        return getPreferences().node(CayennePreference.EDITOR);
    }

    public static Preferences getLastProjFilesPref() {
        return getEditorPreferences().node(CayennePreference.LAST_PROJ_FILES);
    }

    public static List<File> getLastProjFiles() {
        Preferences filesPrefs = getLastProjFilesPref();
        String[] keys;
        try {
            keys = filesPrefs.keys();
        } catch (BackingStoreException e) {
            logObj.warn("Error reading preferences file.", e);
            return new ArrayList<>();
        }

        int len = keys.length;
        List<File> lastProjectsFiles = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            String fileName = filesPrefs.get(Integer.toString(i), "");
            if(!fileName.isEmpty()) {
                File file = new File(fileName);
                if(!lastProjectsFiles.contains(file)) {
                    lastProjectsFiles.add(file);
                }
            }
        }
        return lastProjectsFiles;
    }

    public void preferenceChange(PreferenceChangeEvent evt) {
        evt.getNode().put(evt.getKey(), evt.getNewValue());
    }
}
