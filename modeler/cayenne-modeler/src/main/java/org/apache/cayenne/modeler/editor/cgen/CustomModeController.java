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

package org.apache.cayenne.modeler.editor.cgen;

import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.CodeTemplateManager;
import org.apache.cayenne.modeler.dialog.pref.PreferenceDialog;
import org.apache.cayenne.modeler.pref.DataMapDefaults;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.ObjectBinding;
import org.apache.cayenne.util.Util;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * A controller for the custom generation mode.
 */
public class CustomModeController extends GeneratorController {

    // correspond to non-public constants on MapClassGenerator.
    static final String MODE_DATAMAP = "datamap";
    static final String MODE_ENTITY = "entity";
    static final String MODE_ALL = "all";

    static final String DATA_MAP_MODE_LABEL = "DataMap generation";
    static final String ENTITY_MODE_LABEL = "Entity and Embeddable generation";
    static final String ALL_MODE_LABEL = "Generate all";

    static final Map<String, String> modesByLabel = new HashMap<>();

    static {
        modesByLabel.put(DATA_MAP_MODE_LABEL, MODE_DATAMAP);
        modesByLabel.put(ENTITY_MODE_LABEL, MODE_ENTITY);
        modesByLabel.put(ALL_MODE_LABEL, MODE_ALL);
    }

    protected CustomModePanel view;
    protected CodeTemplateManager templateManager;

    protected ObjectBinding superTemplate;
    protected ObjectBinding subTemplate;

    private BindingBuilder builder;

    private CustomPreferencesUpdater preferencesUpdater;

    public CustomPreferencesUpdater getCustomPreferencesUpdater() {
        return preferencesUpdater;
    }

    public CustomModeController(CodeGeneratorControllerBase parent) {
        super(parent);
        this.view = new CustomModePanel();
        bind();
    }

    private void bind() {
        initBindings(new BindingBuilder(getApplication().getBindingFactory(), this));
        builder = new BindingBuilder(getApplication().getBindingFactory(), this);
        builder.bindToAction(view.getManageTemplatesLink(), "popPreferencesAction()");

        Object[] modeChoices = new Object[]{ENTITY_MODE_LABEL, DATA_MAP_MODE_LABEL, ALL_MODE_LABEL};
        view.getGenerationMode().setModel(new DefaultComboBoxModel(modeChoices));
    }

    public void startup(DataMap dataMap) {
        super.startup(dataMap);

        // bind preferences and init defaults...
        DataMapDefaults dataMapDefaults = getMapPreferences().get(getParentController().getDataMap());

        if (Util.isEmptyString(dataMapDefaults.getSuperclassTemplate())) {
            dataMapDefaults.setSuperclassTemplate(CodeTemplateManager.STANDARD_SERVER_SUPERCLASS);
        }

        if (Util.isEmptyString(dataMapDefaults.getSubclassTemplate())) {
            dataMapDefaults.setSubclassTemplate(CodeTemplateManager.STANDARD_SERVER_SUBCLASS);
        }

        if (Util.isEmptyString(dataMapDefaults.getProperty("mode"))) {
            dataMapDefaults.setProperty("mode", MODE_ENTITY);
        }

        if (Util.isEmptyString(dataMapDefaults.getProperty("overwrite"))) {
            dataMapDefaults.setBooleanProperty("overwrite", false);
        }

        if (Util.isEmptyString(dataMapDefaults.getProperty("pairs"))) {
            dataMapDefaults.setBooleanProperty("pairs", true);
        }

        if (Util.isEmptyString(dataMapDefaults.getProperty("usePackagePath"))) {
            dataMapDefaults.setBooleanProperty("usePackagePath", true);
        }

        if (Util.isEmptyString(dataMapDefaults.getProperty("outputPattern"))) {
            dataMapDefaults.setProperty("outputPattern", "*.java");
        }

        builder.bindToComboSelection(view.getGenerationMode(), "customPreferencesUpdater.mode").updateView();

        builder.bindToStateChange(view.getOverwrite(), "customPreferencesUpdater.overwrite").updateView();

        builder.bindToStateChange(view.getPairs(), "customPreferencesUpdater.pairs").updateView();

        builder.bindToStateChange(view.getUsePackagePath(), "customPreferencesUpdater.usePackagePath").updateView();

        subTemplate = builder.bindToComboSelection(view.getSubclassTemplate(),
                "customPreferencesUpdater.subclassTemplate");

        superTemplate = builder.bindToComboSelection(view.getSuperclassTemplate(),
                "customPreferencesUpdater.superclassTemplate");

        builder.bindToTextField(view.getOutputPattern(), "customPreferencesUpdater.outputPattern").updateView();

        builder.bindToStateChange(view.getCreatePropertyNames(), "customPreferencesUpdater.createPropertyNames")
                .updateView();

        updateTemplates();
    }

    protected void createDefaults() {
        TreeMap<DataMap, DataMapDefaults> map = new TreeMap<>();
        DataMap dataMap = getParentController().getDataMap();
        DataMapDefaults preferences;
        preferences = getApplication().getFrameController().getProjectController()
                .getDataMapPreferences(this.getClass().getName().replace(".", "/"), dataMap);
        preferences.setSuperclassPackage("");
        preferences.updateSuperclassPackage(dataMap, false);

        map.put(dataMap, preferences);

        if (getOutputPath() == null) {
            setOutputPath(preferences.getOutputPath());
        }

        setMapPreferences(map);
        preferencesUpdater = new CustomPreferencesUpdater(map);
    }

    protected GeneratorControllerPanel createView() {
        if (getParentController().getDataMap() != view.getStandardPanelComponent().getDataMap()) {
            DataMapDefaults dataMapDefaults = getMapPreferences().get(getParentController().getDataMap());
            view.getStandardPanelComponent().setDataMap(getParentController().getDataMap());
            view.getStandardPanelComponent().setPreferences(dataMapDefaults);
            view.getStandardPanelComponent().getDataMapName().setText(view.getStandardPanelComponent().getDataMap().getName());
            BindingBuilder builder = new BindingBuilder(getApplication().getBindingFactory(), view.getStandardPanelComponent());
            builder.bindToTextField(view.getStandardPanelComponent().getSuperclassPackage(), "preferences.superclassPackage").updateView();
        }
        return view;
    }

    protected void updateTemplates() {
        this.templateManager = getApplication().getCodeTemplateManager();

        List<String> customTemplates = new ArrayList<>(templateManager.getCustomTemplates().keySet());
        Collections.sort(customTemplates);

        List<String> superTemplates = new ArrayList<>(templateManager.getStandardSuperclassTemplates());
        Collections.sort(superTemplates);
        superTemplates.addAll(customTemplates);

        List<String> subTemplates = new ArrayList<>(templateManager.getStandardSubclassTemplates());
        Collections.sort(subTemplates);
        subTemplates.addAll(customTemplates);

        this.view.getSubclassTemplate().setModel(new DefaultComboBoxModel(subTemplates.toArray()));
        this.view.getSuperclassTemplate().setModel(new DefaultComboBoxModel(superTemplates.toArray()));

        superTemplate.updateView();
        subTemplate.updateView();
    }

    public Component getView() {
        return view;
    }

    public Collection<ClassGenerationAction> createGenerator() {

        mode = modesByLabel.get(view.getGenerationMode().getSelectedItem()).toString();

        Collection<ClassGenerationAction> generators = super.createGenerator();

        String superKey = view.getSuperclassTemplate().getSelectedItem().toString();
        String superTemplate = templateManager.getTemplatePath(superKey);

        String subKey = view.getSubclassTemplate().getSelectedItem().toString();
        String subTemplate = templateManager.getTemplatePath(subKey);

        for (ClassGenerationAction generator : generators) {
            generator.setSuperTemplate(superTemplate);
            generator.setTemplate(subTemplate);
            generator.setOverwrite(view.getOverwrite().isSelected());
            generator.setUsePkgPath(view.getUsePackagePath().isSelected());
            generator.setMakePairs(view.getPairs().isSelected());
            generator.setCreatePropertyNames(view.getCreatePropertyNames().isSelected());

            if (!Util.isEmptyString(view.getOutputPattern().getText())) {
                generator.setOutputPattern(view.getOutputPattern().getText());
            }
        }

        return generators;
    }

    public void popPreferencesAction() {
        new PreferenceDialog(getApplication().getFrameController()).startupAction(PreferenceDialog.TEMPLATES_KEY);
        updateTemplates();
    }

    @Override
    protected ClassGenerationAction newGenerator() {
        ClassGenerationAction action = new ClassGenerationAction();
        getApplication().getInjector().injectMembers(action);
        return action;
    }
}
