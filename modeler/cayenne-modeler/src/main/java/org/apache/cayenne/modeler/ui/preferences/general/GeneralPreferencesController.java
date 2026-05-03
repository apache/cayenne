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

package org.apache.cayenne.modeler.ui.preferences.general;

import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.modeler.pref.GeneralPrefs;
import org.apache.cayenne.modeler.ui.preferences.PreferenceDialogController;

import java.awt.*;

public class GeneralPreferencesController extends ChildController<PreferenceDialogController> {

    static final String[] STANDARD_ENCODINGS = {
            "ISO-8859-1", "US-ASCII", "UTF-8", "UTF-16", "UTF-16BE", "UTF-16LE"
    };

    private final GeneralPreferencesView view;

    public GeneralPreferencesController(PreferenceDialogController parent) {
        super(parent);

        GeneralPrefs prefs = GeneralPrefs.of(getApplication().getPreferencesRepository());
        this.view = new GeneralPreferencesView(
                STANDARD_ENCODINGS,
                prefs.getEncoding(),
                prefs.isAutoLoadProject(),
                prefs.isNoDeletePrompt());
    }

    @Override
    public Component getView() {
        return view;
    }

    public void commit() {
        GeneralPrefs prefs = GeneralPrefs.of(getApplication().getPreferencesRepository());
        prefs.setEncoding(view.getSelectedEncoding());
        prefs.setAutoLoadProject(view.isAutoLoadProjectSelected());
        prefs.setNoDeletePrompt(view.isNoDeletePromptSelected());
    }
}
