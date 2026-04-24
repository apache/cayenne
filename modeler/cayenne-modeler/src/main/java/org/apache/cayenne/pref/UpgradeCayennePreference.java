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
package org.apache.cayenne.pref;

import org.apache.cayenne.modeler.ModelerPreferences;
import org.apache.cayenne.modeler.util.CayenneUserDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class UpgradeCayennePreference extends PreferenceDecorator {

    /** Name of the preferences file. */
    public static final String PREFERENCES_NAME_OLD = "modeler.preferences";

    public static final String LAST_PROJ_FILES_OLD = "Editor.lastSeveralProjectFiles";

    public static final String DELIMITER = ",";

    public UpgradeCayennePreference(Preference delegate) {
        super(delegate);
    }

    public void upgrade() {
        try {

            if (!Preferences.userRoot().nodeExists(CAYENNE_PREFERENCES_PATH)) {

                File prefsFile = new File(preferencesDirectory(), PREFERENCES_NAME_OLD);
                if (prefsFile.exists()) {
                    Properties ep = new Properties();
                    try {
                        ep.load(new FileInputStream(prefsFile));

                        Preferences prefEditor = Preferences.userRoot().node(CAYENNE_PREFERENCES_PATH).node(EDITOR);

                        Preferences frefLastProjFiles = prefEditor.node(LAST_PROJ_FILES);

                        List<String> arr = getVector(ep.getProperty(LAST_PROJ_FILES_OLD));
                        while (arr.size() > ModelerPreferences.LAST_PROJ_FILES_SIZE) {
                            arr.remove(arr.size() - 1);
                        }

                        frefLastProjFiles.clear();

                        for (int i = 0; i < arr.size(); i++) {
                            frefLastProjFiles.put(String.valueOf(i), arr.get(i));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (BackingStoreException ignored) {
        }
    }

    private List<String> getVector(String value) {
        return Arrays.asList(value.split(DELIMITER));
    }

    /**
     * Returns preferences directory <code>$HOME/.cayenne</code>. If such directory does
     * not exist, it is created as a side effect of this method.
     */
    public File preferencesDirectory() {
        return CayenneUserDir.getInstance().getDirectory();
    }

    @Override
    public Preferences getRootPreference() {
        upgrade();
        return delegate.getRootPreference();
    }

    @Override
    public Preferences getCayennePreference() {
        upgrade();
        return delegate.getCayennePreference();
    }

    @Override
    public Preferences getCurrentPreference() {
        upgrade();
        return delegate.getCayennePreference();
    }
}
