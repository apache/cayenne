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

package org.apache.cayenne.modeler.dialog.pref;

import org.apache.cayenne.modeler.mvc.ChildController;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

public class GeneralPreferencesController extends ChildController<PreferenceDialogController> {

    public static final String AUTO_LOAD_PROJECT_PREFERENCE = "autoLoadProject";
    public static final String DELETE_PROMPT_PREFERENCE = "deletePrompt";
    public static final String ENCODING_PREFERENCE = "encoding";

    /**
     * Favourite data source preference
     * Currently used in reengineering dialog where it's selected by default
     * It's not present in preferences dialog hence can't be modified directly
     */
    public static final String FAVOURITE_DATA_SOURCE = "favouriteDataSource";

    private final GeneralPreferencesView view;

    private boolean autoLoadProjectPreference;
    private String encoding;
    private boolean deletePromptPreference;
    private final Preferences preferences;

    public GeneralPreferencesController(PreferenceDialogController parent) {
        super(parent);

        this.preferences = application.getPreferencesNode(GeneralPreferencesController.class, "");
        this.encoding = preferences.get(ENCODING_PREFERENCE, null);
        this.autoLoadProjectPreference = preferences.getBoolean(AUTO_LOAD_PROJECT_PREFERENCE, false);
        this.deletePromptPreference = preferences.getBoolean(DELETE_PROMPT_PREFERENCE, false);

        // TODO: confusing: "encodingController" is dangling in the air, yet it doesn't go out of scope as it is a
        //  listener for its own view events
        EncodingPreferencesController encodingController = new EncodingPreferencesController(this);
        encodingController.addPropertyChangeListener(EncodingPreferencesController.ENCODING_PROPERTY, evt -> setEncoding((String) evt.getNewValue()));
        encodingController.setSelectedEncoding(encoding);

        this.view = new GeneralPreferencesView(encodingController.getView());
        this.view.setEnabled(true);
        this.view.getAutoLoadProject().addActionListener(e -> setAutoLoadProject(view.getAutoLoadProject().isSelected()));
        this.view.getDeletePrompt().addActionListener(e -> setDeletePrompt(view.getDeletePrompt().isSelected()));
        this.view.getAutoLoadProject().setSelected(autoLoadProjectPreference);
        this.view.getDeletePrompt().setSelected(deletePromptPreference);
    }

    @Override
    public Component getView() {
        return view;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        addChangedPreferences(ENCODING_PREFERENCE, encoding);
        this.encoding = encoding;
    }

    private void setAutoLoadProject(boolean autoLoadProject) {
        addChangedBooleanPreferences(AUTO_LOAD_PROJECT_PREFERENCE, autoLoadProject);
        this.autoLoadProjectPreference = autoLoadProject;
    }

    private void setDeletePrompt(boolean deletePrompt) {
        addChangedBooleanPreferences(DELETE_PROMPT_PREFERENCE, deletePrompt);
        this.deletePromptPreference = deletePrompt;
    }

    public void addChangedBooleanPreferences(String key, boolean value) {
        Map<String, Boolean> map = parent.getEditor().getChangedBooleanPreferences().get(preferences);
        if (map == null) {
            map = new HashMap<>();
        }
        map.put(key, value);
        parent.getEditor().getChangedBooleanPreferences().put(preferences, map);
    }

    public void addChangedPreferences(String key, String value) {
        Map<String, String> map = parent.getEditor().getChangedPreferences().get(preferences);
        if (map == null) {
            map = new HashMap<>();
        }
        map.put(key, value);
        parent.getEditor().getChangedPreferences().put(preferences, map);
    }
}
