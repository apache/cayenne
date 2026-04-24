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
import org.apache.cayenne.modeler.ui.preferences.PreferenceDialogController;
import org.apache.cayenne.util.Util;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
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
    private final String systemEncoding;
    private final Preferences preferences;

    private boolean autoLoadProjectPreference;
    private String encoding;
    private boolean defaultEncoding;
    private boolean deletePromptPreference;

    public GeneralPreferencesController(PreferenceDialogController parent) {
        super(parent);

        this.preferences = application.getPreferencesNode(GeneralPreferencesController.class, "");
        this.encoding = preferences.get(ENCODING_PREFERENCE, null);
        this.autoLoadProjectPreference = preferences.getBoolean(AUTO_LOAD_PROJECT_PREFERENCE, false);
        this.deletePromptPreference = preferences.getBoolean(DELETE_PROMPT_PREFERENCE, false);

        this.systemEncoding = detectPlatformEncoding();

        this.view = new GeneralPreferencesView();
        this.view.setEnabled(true);

        Vector allEncodings = supportedEncodings(systemEncoding);
        view.getEncodingChoices().setModel(new DefaultComboBoxModel(allEncodings));
        view.getDefaultEncodingLabel().setText("Default (" + systemEncoding + ")");
        view.getDefaultEncoding().setSelected(true);

        view.getDefaultEncoding().addActionListener(e -> setDefaultEncoding(view.getDefaultEncoding().isSelected()));
        view.getOtherEncoding().addActionListener(e -> setDefaultEncoding(!view.getOtherEncoding().isSelected()));
        view.getEncodingChoices().addActionListener(e -> {
            Object sel = view.getEncodingChoices().getSelectedItem();
            setEncoding(sel != null ? sel.toString() : null);
        });

        setSelectedEncoding(encoding);

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
        if (!Util.nullSafeEquals(this.encoding, encoding)) {
            addChangedPreferences(ENCODING_PREFERENCE, encoding);
            this.encoding = encoding;
        }
    }

    /**
     * Returns default encoding on the current platform.
     */
    protected String detectPlatformEncoding() {
        // this info is private until 1.5, so have to hack it...
        return new OutputStreamWriter(new ByteArrayOutputStream()).getEncoding();
    }

    /**
     * Returns an array of charsets that all JVMs must support cross-platform combined
     * with a default platform charset. See Javadocs for java.nio.charset.Charset for the
     * list of "standard" charsets.
     */
    protected Vector supportedEncodings(String platformEncoding) {
        String[] defaultCharsets = new String[] {
                "US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE", "UTF-16"
        };

        Vector charsets = new Vector(Arrays.asList(defaultCharsets));
        if (!charsets.contains(platformEncoding)) {
            charsets.add(platformEncoding);
        }

        Collections.sort(charsets);
        return charsets;
    }

    private void setSelectedEncoding(String encoding) {
        this.encoding = encoding;
        this.defaultEncoding = encoding == null || encoding.equals(systemEncoding);

        view.getEncodingChoices().setSelectedItem(encoding);
        view.getDefaultEncoding().setSelected(defaultEncoding);
        view.getOtherEncoding().setSelected(!defaultEncoding);
        view.getEncodingChoices().setEnabled(!defaultEncoding);
        view.getDefaultEncodingLabel().setEnabled(defaultEncoding);
    }

    private void setDefaultEncoding(boolean b) {
        if (b != defaultEncoding) {
            this.defaultEncoding = b;

            if (b) {
                setEncoding(systemEncoding);
                view.getEncodingChoices().setEnabled(false);
                view.getDefaultEncodingLabel().setEnabled(true);
            }
            else {
                view.getEncodingChoices().setEnabled(true);
                view.getDefaultEncodingLabel().setEnabled(false);
            }
        }
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
        Map<String, Boolean> map = parent.getContext().getChangedBooleanPreferences().get(preferences);
        if (map == null) {
            map = new HashMap<>();
        }
        map.put(key, value);
        parent.getContext().getChangedBooleanPreferences().put(preferences, map);
    }

    public void addChangedPreferences(String key, String value) {
        Map<String, String> map = parent.getContext().getChangedPreferences().get(preferences);
        if (map == null) {
            map = new HashMap<>();
        }
        map.put(key, value);
        parent.getContext().getChangedPreferences().put(preferences, map);
    }
}
