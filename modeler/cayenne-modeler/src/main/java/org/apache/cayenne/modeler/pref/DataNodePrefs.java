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

public final class DataNodePrefs extends PreferenceAdapter {

    public static final String LOCAL_DATA_SOURCE_PROPERTY = "localDataSource";

    static final String NODE = "dataNode";

    private final PreferencesRepository repository;
    private final Project project;

    private String localDataSource;

    public DataNodePrefs(PreferencesRepository repository, Project project, String dataNodeName) {
        super(repository.projectPref(project, NODE + "/" + dataNodeName));

        // capture repo and project for the sake of "rename"
        this.repository = repository;
        this.project = project;
    }

    public DataNodePrefs rename(String newName) {
        DataNodePrefs renamed = new DataNodePrefs(repository, project, newName);

        // TODO: is this the correct behavior? In the DataMapPrefs we are doing "copy", not "move" to handle reverts
        PreferencesCopier.move(prefs, renamed.prefs);
        return renamed;
    }

    public void setLocalDataSource(String localDataSource) {
        this.localDataSource = localDataSource;
        if (localDataSource != null) {
            prefs.put(LOCAL_DATA_SOURCE_PROPERTY, localDataSource);
        }
    }

    public String getLocalDataSource() {
        if (localDataSource == null) {
            localDataSource = prefs.get(LOCAL_DATA_SOURCE_PROPERTY, "");
        }
        return localDataSource;
    }
}
