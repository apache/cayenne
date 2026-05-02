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

public final class RecentProjectsPrefs {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecentProjectsPrefs.class);

    public static final int MAX_SIZE = 12;

    // TODO: relocate this node from under "editor"
    private static final String NODE_PATH = "editor/lastSeveralProjectFiles";

    private final Preferences prefs;

    private RecentProjectsPrefs(Preferences prefs) {
        this.prefs = prefs;
    }

    public static RecentProjectsPrefs of() {
        return new RecentProjectsPrefs(CayennePreference.getRoot().node(NODE_PATH));
    }

    public List<File> getFiles() {
        String[] keys = keys();
        List<File> files = new ArrayList<>(keys.length);
        for (int i = 0; i < keys.length; i++) {
            String fileName = prefs.get(Integer.toString(i), "");
            if (!fileName.isEmpty()) {
                File file = new File(fileName);
                if (!files.contains(file) && file.exists()) {
                    files.add(file);
                }
            }
        }
        return files;
    }

    public void addFile(File file) {
        List<File> files = getFiles();
        files.remove(file);
        files.add(0, file);
        if (files.size() > MAX_SIZE) {
            files = files.subList(0, MAX_SIZE);
        }
        rewrite(files);
    }

    public void removeFile(File file) {
        List<File> files = getFiles();
        if (files.remove(file)) {
            rewrite(files);
        }
    }

    private void rewrite(List<File> files) {
        try {
            prefs.clear();
        } catch (BackingStoreException e) {
            LOGGER.warn("Error clearing preferences node.", e);
        }
        for (int i = 0; i < files.size(); i++) {
            prefs.put(Integer.toString(i), files.get(i).getAbsolutePath());
        }
    }

    private String[] keys() {
        try {
            return prefs.keys();
        } catch (BackingStoreException e) {
            LOGGER.warn("Error reading preferences.", e);
            return new String[0];
        }
    }
}
