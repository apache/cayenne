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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

/**
 * Binds the DB Generator dialog's checkboxes to {@link Preferences}: seeds the
 * checkboxes from prefs on {@link #bind} and writes them back when the dialog window
 * closes. Holds no state of its own — the view is the source of truth while the dialog
 * is open.
 */
public class DBGeneratorPrefs implements PreferenceAdapter {

    static final String NODE = "dbGenerator";

    private static final String CREATE_FK_PROPERTY = "createFK";
    private static final String CREATE_PK_PROPERTY = "createPK";
    private static final String CREATE_TABLES_PROPERTY = "createTables";
    private static final String DROP_PK_PROPERTY = "dropPK";
    private static final String DROP_TABLES_PROPERTY = "dropTables";

    public static DBGeneratorPrefs of(PreferencesRepository repository, Project project) {
        return new DBGeneratorPrefs(repository.projectPref(project, NODE));
    }

    private final Preferences prefs;

    private DBGeneratorPrefs(Preferences prefs) {
        this.prefs = prefs;
    }

    public void bind(DBGeneratorOptionsView view) {
        view.getCreateFK().setSelected(prefs.getBoolean(CREATE_FK_PROPERTY, true));
        view.getCreatePK().setSelected(prefs.getBoolean(CREATE_PK_PROPERTY, true));
        view.getCreateTables().setSelected(prefs.getBoolean(CREATE_TABLES_PROPERTY, true));
        view.getDropPK().setSelected(prefs.getBoolean(DROP_PK_PROPERTY, false));
        view.getDropTables().setSelected(prefs.getBoolean(DROP_TABLES_PROPERTY, false));

        view.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                prefs.putBoolean(CREATE_FK_PROPERTY, view.getCreateFK().isSelected());
                prefs.putBoolean(CREATE_PK_PROPERTY, view.getCreatePK().isSelected());
                prefs.putBoolean(CREATE_TABLES_PROPERTY, view.getCreateTables().isSelected());
                prefs.putBoolean(DROP_PK_PROPERTY, view.getDropPK().isSelected());
                prefs.putBoolean(DROP_TABLES_PROPERTY, view.getDropTables().isSelected());
            }
        });
    }
}
