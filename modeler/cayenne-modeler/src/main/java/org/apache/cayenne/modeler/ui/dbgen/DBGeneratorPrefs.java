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
package org.apache.cayenne.modeler.ui.dbgen;

import org.apache.cayenne.modeler.pref.PreferenceAdapter;
import org.apache.cayenne.modeler.pref.PreferencesRepository;
import org.apache.cayenne.project.Project;

/**
 * Persistence for the DB Generator dialog's checkbox options.
 */
public class DBGeneratorPrefs extends PreferenceAdapter {

    static final String NODE = "dbGenerator";

    private static final String CREATE_FK_PROPERTY = "createFK";
    private static final String CREATE_PK_PROPERTY = "createPK";
    private static final String CREATE_TABLES_PROPERTY = "createTables";
    private static final String DROP_PK_PROPERTY = "dropPK";
    private static final String DROP_TABLES_PROPERTY = "dropTables";

    public DBGeneratorPrefs(PreferencesRepository repository, Project project) {
        super(repository.projectPref(project, NODE));
    }

    public boolean getCreateFK() {
        return prefs.getBoolean(CREATE_FK_PROPERTY, true);
    }

    public boolean getCreatePK() {
        return prefs.getBoolean(CREATE_PK_PROPERTY, true);
    }

    public boolean getCreateTables() {
        return prefs.getBoolean(CREATE_TABLES_PROPERTY, true);
    }

    public boolean getDropPK() {
        return prefs.getBoolean(DROP_PK_PROPERTY, false);
    }

    public boolean getDropTables() {
        return prefs.getBoolean(DROP_TABLES_PROPERTY, false);
    }

    public void save(boolean createFK, boolean createPK, boolean createTables,
                     boolean dropPK, boolean dropTables) {
        prefs.putBoolean(CREATE_FK_PROPERTY, createFK);
        prefs.putBoolean(CREATE_PK_PROPERTY, createPK);
        prefs.putBoolean(CREATE_TABLES_PROPERTY, createTables);
        prefs.putBoolean(DROP_PK_PROPERTY, dropPK);
        prefs.putBoolean(DROP_TABLES_PROPERTY, dropTables);
    }
}
