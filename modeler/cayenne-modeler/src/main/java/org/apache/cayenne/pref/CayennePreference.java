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

import java.util.prefs.Preferences;

public class CayennePreference implements Preference {

    private Preferences rootPreference;
    private Preferences cayennePreference;
    private Preferences currentPreference;

    public Preferences getCurrentPreference() {
        return currentPreference;
    }

    public void setCurrentPreference(Preferences currentPreference) {
        this.currentPreference = currentPreference;
    }

    public Preferences getRootPreference() {
        if (rootPreference == null) {
            rootPreference = Preferences.userRoot();
        }
        return rootPreference;
    }

    public Preferences getCayennePreference() {
        if (cayennePreference == null) {
            cayennePreference = getRootPreference().node(CAYENNE_PREFERENCES_PATH);
        }
        return cayennePreference;
    }

    public Preferences getNode(Class className, String path) {
        if (path == null || path.length() == 0) {
            return Preferences.userNodeForPackage(className);
        }
        return Preferences.userNodeForPackage(className).node(path);
    }

    public void setCurrentNodeForPreference(Class className, String path) {
        currentPreference = getNode(className, path);
    }

    public void setObject(CayennePreference object) {
    }

    public void saveObjectPreference() {
    }

    public static String filePathToPrefereceNodePath(String path) {
        return path.replace(".xml", "");
    }
}
