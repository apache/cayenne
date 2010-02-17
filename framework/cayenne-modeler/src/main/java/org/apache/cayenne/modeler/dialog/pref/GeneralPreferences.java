/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.dialog.pref;

import java.awt.Component;
import java.util.prefs.Preferences;

import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.pref.CayennePreferenceEditor;
import org.apache.cayenne.pref.CayennePreferenceService;
import org.apache.cayenne.pref.PrefDetail;
import org.apache.cayenne.pref.PreferenceEditor;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.ObjectBinding;
import org.apache.cayenne.validation.ValidationException;

/**
 */
public class GeneralPreferences extends CayenneController {

    public static final String AUTO_LOAD_PROJECT_PREFERENCE = "autoLoadProject";
    public static final String DELETE_PROMPT_PREFERENCE = "deletePrompt";
    public static final String ENCODING_PREFERENCE = "encoding";

    protected GeneralPreferencesView view;
    protected CayennePreferenceEditor editor;
    protected boolean autoLoadProjectPreference;
    protected PrefDetail classGeneratorPreferences;
    protected boolean deletePromptPreference;
    
    protected Preferences preferences;

    protected ObjectBinding saveIntervalBinding;
    protected ObjectBinding encodingBinding;
    protected ObjectBinding autoLoadProjectBinding;
    protected ObjectBinding deletePromptBinding;

    public GeneralPreferences(PreferenceDialog parentController) {
        super(parentController);
        this.view = new GeneralPreferencesView();

        PreferenceEditor editor = parentController.getEditor();
        if (editor instanceof CayennePreferenceEditor) {
            this.editor = (CayennePreferenceEditor) editor;
            this.view.setEnabled(true);
            initBindings();

            saveIntervalBinding.updateView();
            encodingBinding.updateView();
            autoLoadProjectBinding.updateView();
            deletePromptBinding.updateView();
        }
        else {
            this.view.setEnabled(false);
        }
    }

    public Component getView() {
        return view;
    }

    protected void initBindings() {
        // init model objects
        preferences = application.getPreferencesNode(ClassGenerationAction.class, "");
        
        this.classGeneratorPreferences = new PrefDetail(preferences.node(ENCODING_PREFERENCE));
        this.autoLoadProjectPreference = preferences.getBoolean(AUTO_LOAD_PROJECT_PREFERENCE, false);
        this.deletePromptPreference = preferences.getBoolean(DELETE_PROMPT_PREFERENCE, false);

        // build child controllers...
        EncodingSelector encodingSelector = new EncodingSelector(this, view
                .getEncodingSelector());

        // create bindings...
        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        this.saveIntervalBinding = builder.bindToTextField(view.getSaveInterval(),
                "timeInterval");

        this.encodingBinding = builder.bindToProperty(encodingSelector,
                "classGeneratorPreferences.property[\"encoding\"]",
                EncodingSelector.ENCODING_PROPERTY_BINDING);

        this.autoLoadProjectBinding = builder.bindToCheckBox(view.getAutoLoadProject(),
                "autoLoadProject");

        this.deletePromptBinding = builder.bindToCheckBox(view.getDeletePrompt(),
                "deletePrompt");
    }

    public double getTimeInterval() {
        return this.editor.getSaveInterval() / 1000.0;
    }

    public void setTimeInterval(double d) {
        int ms = (int) (d * 1000.0);
        if (ms < CayennePreferenceService.MIN_SAVE_INTERVAL) {
            throw new ValidationException(
                    "Time interval is too small, minimum allowed is "
                            + (CayennePreferenceService.MIN_SAVE_INTERVAL / 1000.0));
        }

        this.editor.setSaveInterval(ms);
    }

    public boolean getAutoLoadProject() {
        return preferences.getBoolean(GeneralPreferences.AUTO_LOAD_PROJECT_PREFERENCE, false);
    }

    public void setAutoLoadProject(boolean autoLoadProject) {
        preferences.putBoolean(GeneralPreferences.AUTO_LOAD_PROJECT_PREFERENCE, autoLoadProject);
    }

    public boolean getDeletePrompt() {
        return preferences.getBoolean(GeneralPreferences.DELETE_PROMPT_PREFERENCE, false);
    }

    public void setDeletePrompt(boolean deletePrompt) {
        preferences.putBoolean(GeneralPreferences.DELETE_PROMPT_PREFERENCE, deletePrompt);
    }

    public PrefDetail getClassGeneratorPreferences() {
        return classGeneratorPreferences;
    }
}
