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

import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.pref.CayennePreferenceEditor;
import org.apache.cayenne.pref.CayennePreferenceService;
import org.apache.cayenne.pref.Domain;
import org.apache.cayenne.pref.PreferenceDetail;
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
    protected PreferenceDetail autoLoadProjectPreference;
    protected PreferenceDetail classGeneratorPreferences;
    protected PreferenceDetail deletePromptPreference;

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
        Domain classGeneratorDomain = editor.editableInstance(getApplication()
                .getPreferenceDomain()).getSubdomain(ClassGenerationAction.class);
        this.classGeneratorPreferences = classGeneratorDomain
                .getDetail(ENCODING_PREFERENCE, true);

        this.autoLoadProjectPreference = editor.editableInstance(getApplication().getPreferenceDomain())
                .getDetail(AUTO_LOAD_PROJECT_PREFERENCE, true);

        this.deletePromptPreference = editor.editableInstance(getApplication().getPreferenceDomain())
                .getDetail(DELETE_PROMPT_PREFERENCE, true);


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
        return autoLoadProjectPreference.getBooleanProperty(GeneralPreferences.AUTO_LOAD_PROJECT_PREFERENCE);
    }

    public void setAutoLoadProject(boolean autoLoadProject) {
        autoLoadProjectPreference.setBooleanProperty(GeneralPreferences.AUTO_LOAD_PROJECT_PREFERENCE, autoLoadProject);
    }

    public boolean getDeletePrompt() {
        return deletePromptPreference.getBooleanProperty(GeneralPreferences.DELETE_PROMPT_PREFERENCE);
    }

    public void setDeletePrompt(boolean deletePrompt) {
        deletePromptPreference.setBooleanProperty(GeneralPreferences.DELETE_PROMPT_PREFERENCE, deletePrompt);
    }

    public PreferenceDetail getClassGeneratorPreferences() {
        return classGeneratorPreferences;
    }
}
