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

package org.apache.cayenne.modeler.dialog.codegen;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;

import org.apache.cayenne.gen.ClassGenerator;
import org.apache.cayenne.gen.DefaultClassGenerator;
import org.apache.cayenne.modeler.CodeTemplateManager;
import org.apache.cayenne.modeler.dialog.pref.PreferenceDialog;
import org.apache.cayenne.modeler.pref.DataMapDefaults;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.ObjectBinding;
import org.apache.cayenne.util.Util;

/**
 * A controller for the custom generation mode.
 * 
 * @author Andrus Adamchik
 */
public class CustomModeController extends GeneratorController {

    // correspond to non-public constants on MapClassGenerator.
    static final String MODE_DATAMAP = "datamap";
    static final String MODE_ENTITY = "entity";

    static final String DATA_MAP_MODE_LABEL = "One run per DataMap";
    static final String ENTITY_MODE_LABEL = "One run per each selected Entity";

    static final Map modesByLabel = new HashMap();
    static {
        modesByLabel.put(DATA_MAP_MODE_LABEL, MODE_DATAMAP);
        modesByLabel.put(ENTITY_MODE_LABEL, MODE_ENTITY);
    }

    protected CustomModePanel view;
    protected CodeTemplateManager templateManager;

    protected ObjectBinding superTemplate;
    protected ObjectBinding subTemplate;

    public CustomModeController(CodeGeneratorControllerBase parent) {
        super(parent);

        Object[] modeChoices = new Object[] {
                ENTITY_MODE_LABEL, DATA_MAP_MODE_LABEL
        };
        view.getGenerationMode().setModel(new DefaultComboBoxModel(modeChoices));

        Object[] versionChoices = new Object[] {
                ClassGenerator.VERSION_1_1, ClassGenerator.VERSION_1_2
        };
        view.getGeneratorVersion().setModel(new DefaultComboBoxModel(versionChoices));

        // bind preferences and init defaults...

        if (Util.isEmptyString(preferences.getSuperclassTemplate())) {
            preferences
                    .setSuperclassTemplate(CodeTemplateManager.STANDARD_SERVER_SUPERCLASS);
        }

        if (Util.isEmptyString(preferences.getSubclassTemplate())) {
            preferences.setSubclassTemplate(CodeTemplateManager.STANDARD_SERVER_SUBCLASS);
        }

        if (Util.isEmptyString(preferences.getProperty("mode"))) {
            preferences.setProperty("mode", MODE_ENTITY);
        }

        if (Util.isEmptyString(preferences.getProperty("version"))) {
            preferences.setProperty("version", ClassGenerator.VERSION_1_1);
        }

        if (Util.isEmptyString(preferences.getProperty("overwrite"))) {
            preferences.setBooleanProperty("overwrite", false);
        }

        if (Util.isEmptyString(preferences.getProperty("pairs"))) {
            preferences.setBooleanProperty("pairs", true);
        }

        if (Util.isEmptyString(preferences.getProperty("usePackagePath"))) {
            preferences.setBooleanProperty("usePackagePath", true);
        }

        if (Util.isEmptyString(preferences.getProperty("outputPattern"))) {
            preferences.setProperty("outputPattern", "*.java");
        }

        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        builder.bindToAction(view.getManageTemplatesLink(), "popPreferencesAction()");

        builder.bindToTextField(
                view.getSuperclassPackage(),
                "preferences.superclassPackage").updateView();

        builder.bindToComboSelection(
                view.getGenerationMode(),
                "preferences.property['mode']").updateView();

        builder.bindToComboSelection(
                view.getGeneratorVersion(),
                "preferences.property['version']").updateView();

        builder.bindToStateChange(
                view.getOverwrite(),
                "preferences.booleanProperty['overwrite']").updateView();
        builder
                .bindToStateChange(
                        view.getPairs(),
                        "preferences.booleanProperty['pairs']")
                .updateView();

        builder.bindToStateChange(
                view.getUsePackagePath(),
                "preferences.booleanProperty['usePackagePath']").updateView();

        subTemplate = builder.bindToComboSelection(
                view.getSubclassTemplate(),
                "preferences.subclassTemplate");

        superTemplate = builder.bindToComboSelection(
                view.getSuperclassTemplate(),
                "preferences.superclassTemplate");

        builder.bindToTextField(
                view.getOutputPattern(),
                "preferences.property['outputPattern']").updateView();

        updateTemplates();
    }

    protected DataMapDefaults createDefaults() {
        DataMapDefaults prefs = getApplication()
                .getFrameController()
                .getProjectController()
                .getDataMapPreferences("__custom");

        prefs.updateSuperclassPackage(getParentController().getDataMap(), false);
        this.preferences = prefs;
        return prefs;
    }

    protected void updateTemplates() {
        this.templateManager = getApplication().getCodeTemplateManager();

        List customTemplates = new ArrayList(templateManager
                .getCustomTemplates()
                .keySet());
        Collections.sort(customTemplates);

        List superTemplates = new ArrayList(templateManager
                .getStandardSuperclassTemplates()
                .keySet());
        Collections.sort(superTemplates);
        superTemplates.addAll(customTemplates);

        List subTemplates = new ArrayList(templateManager
                .getStandardSubclassTemplates()
                .keySet());
        Collections.sort(subTemplates);
        subTemplates.addAll(customTemplates);

        this.view.getSubclassTemplate().setModel(
                new DefaultComboBoxModel(subTemplates.toArray()));
        this.view.getSuperclassTemplate().setModel(
                new DefaultComboBoxModel(superTemplates.toArray()));

        superTemplate.updateView();
        subTemplate.updateView();
    }

    protected GeneratorControllerPanel createView() {
        this.view = new CustomModePanel();
        return view;
    }

    public Component getView() {
        return view;
    }

    public DefaultClassGenerator createGenerator() {

        DefaultClassGenerator generator = super.createGenerator();

        String mode = modesByLabel
                .get(view.getGenerationMode().getSelectedItem())
                .toString();
        generator.setMode(mode);

        String superKey = view.getSuperclassTemplate().getSelectedItem().toString();
        String superTemplate = templateManager.getTemplatePath(superKey);
        generator.setSuperTemplate(superTemplate);

        String subKey = view.getSubclassTemplate().getSelectedItem().toString();
        String subTemplate = templateManager.getTemplatePath(subKey);
        generator.setTemplate(subTemplate);

        if (view.getGeneratorVersion().getSelectedItem() != null) {
            generator.setVersionString(view
                    .getGeneratorVersion()
                    .getSelectedItem()
                    .toString());
        }

        generator.setOverwrite(view.getOverwrite().isSelected());
        generator.setUsePkgPath(view.getUsePackagePath().isSelected());
        generator.setMakePairs(view.getPairs().isSelected());

        if (!Util.isEmptyString(view.getOutputPattern().getText())) {
            generator.setOutputPattern(view.getOutputPattern().getText());
        }

        return generator;
    }

    public void popPreferencesAction() {
        new PreferenceDialog(getApplication().getFrameController())
                .startupAction(PreferenceDialog.TEMPLATES_KEY);
        updateTemplates();
    }
}
