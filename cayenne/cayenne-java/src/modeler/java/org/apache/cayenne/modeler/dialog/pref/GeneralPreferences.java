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

import org.apache.cayenne.gen.DefaultClassGenerator;
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
 * @author Andrei Adamchik
 */
public class GeneralPreferences extends CayenneController {

    public static final String ENCODING_PREFERENCE = "encoding";

    protected GeneralPreferencesView view;
    protected CayennePreferenceEditor editor;
    protected PreferenceDetail classGeneratorPreferences;

    protected ObjectBinding saveIntervalBinding;
    protected ObjectBinding encodingBinding;

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
                .getPreferenceDomain()).getSubdomain(DefaultClassGenerator.class);
        this.classGeneratorPreferences = classGeneratorDomain
                .getDetail(ENCODING_PREFERENCE, true);

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

    public PreferenceDetail getClassGeneratorPreferences() {
        return classGeneratorPreferences;
    }
}
