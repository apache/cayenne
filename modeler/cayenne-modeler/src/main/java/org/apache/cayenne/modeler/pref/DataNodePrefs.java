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

import org.apache.cayenne.project.Project;

import java.util.prefs.Preferences;

public final class DataNodePrefs implements PreferenceAdapter {

    public static final String LOCAL_DATA_SOURCE_PROPERTY = "localDataSource";

    static final String NODE = "dataNode";

    private final Preferences pref;
    private String localDataSource;

    public static DataNodePrefs of(PreferencesRepository repository, Project project, String dataNodeName) {
        return new DataNodePrefs(repository.projectPref(project, NODE + "/" + dataNodeName));
    }

    public static void rename(PreferencesRepository repository, Project project, String oldName, String newName) {
        Preferences parent = repository.projectPref(project, NODE);
        PreferencesCopier.move(parent.node(oldName), parent.node(newName));
    }

    private DataNodePrefs(Preferences pref) {
        this.pref = pref;
    }

    public void setLocalDataSource(String localDataSource) {
        this.localDataSource = localDataSource;
        if (localDataSource != null) {
            pref.put(LOCAL_DATA_SOURCE_PROPERTY, localDataSource);
        }
    }

    public String getLocalDataSource() {
        if (localDataSource == null) {
            localDataSource = pref.get(LOCAL_DATA_SOURCE_PROPERTY, "");
        }
        return localDataSource;
    }
}
