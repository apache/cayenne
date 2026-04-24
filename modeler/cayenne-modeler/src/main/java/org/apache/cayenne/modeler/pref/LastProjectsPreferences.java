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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class LastProjectsPreferences {

    private static final Logger LOGGER = LoggerFactory.getLogger(LastProjectsPreferences.class);

    // TODO: relocate this node from under "editor"
    private static final String LAST_X_PROJECTS_PREF = "editor/lastSeveralProjectFiles";
    public static final int LAST_PROJ_FILES_SIZE = 12;

    private static final Preferences LAST_X_PROJECTS;

    static {
        LAST_X_PROJECTS = CayennePreference.getRoot().node(LAST_X_PROJECTS_PREF);
    }

    public static List<File> getFiles() {
        String[] keys;
        try {
            keys = LAST_X_PROJECTS.keys();
        } catch (BackingStoreException e) {
            LOGGER.warn("Error reading preferences file.", e);
            return new ArrayList<>();
        }

        int len = keys.length;
        List<File> files = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            String fileName = LAST_X_PROJECTS.get(Integer.toString(i), "");
            if (!fileName.isEmpty()) {
                File file = new File(fileName);
                if (!files.contains(file) && file.exists()) {
                    files.add(file);
                }
            }
        }
        return files;
    }

    public static Preferences getPrefs() {
        return LAST_X_PROJECTS;
    }
}
