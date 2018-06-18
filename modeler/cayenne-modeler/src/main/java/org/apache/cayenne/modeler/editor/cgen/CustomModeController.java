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

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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

    static final Map<String, String> labelByMode = new HashMap<>();

    static {
        modesByLabel.put(DATA_MAP_MODE_LABEL, MODE_DATAMAP);
        modesByLabel.put(ENTITY_MODE_LABEL, MODE_ENTITY);
        modesByLabel.put(ALL_MODE_LABEL, MODE_ALL);
        labelByMode.put(MODE_DATAMAP, DATA_MAP_MODE_LABEL);
        labelByMode.put(MODE_ENTITY, ENTITY_MODE_LABEL);
        labelByMode.put(MODE_ALL, ALL_MODE_LABEL);
    }

    protected CustomModePanel view;
    private CodeTemplateManager templateManager;

    protected ObjectBinding superTemplate;
    protected ObjectBinding subTemplate;

    private BindingBuilder builder;

    private CustomPreferencesUpdater preferencesUpdater;

    private ClassGenerationAction classGenerationAction;

    public CustomPreferencesUpdater getCustomPreferencesUpdater() {
        return preferencesUpdater;
    }

    public CustomModeController(CodeGeneratorControllerBase parent) {
        super(parent);
        this.view = new CustomModePanel();
        initListeners();
        bind();
    }

    private void bind() {
        initBindings(new BindingBuilder(getApplication().getBindingFactory(), this));
        builder = new BindingBuilder(getApplication().getBindingFactory(), this);
        builder.bindToAction(view.getManageTemplatesLink(), "popPreferencesAction()");

        updateTemplates();
    }

    public void startup(DataMap dataMap) {
        createDefaults();
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

    protected void updateTemplates() {
        Object[] modeChoices = new Object[]{ENTITY_MODE_LABEL, DATA_MAP_MODE_LABEL, ALL_MODE_LABEL};
        view.getGenerationMode().setModel(new DefaultComboBoxModel(modeChoices));

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
    }

    public Component getView() {
        return view;
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

    private void initListeners(){
        view.getOutputFolder().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                classGenerationAction.setDestDir(view.getOutputDir());
                getParentController().getProjectController().setDirty(true);
            }
            @Override
            public void removeUpdate(DocumentEvent e) {}

            @Override
            public void changedUpdate(DocumentEvent e) {}
        });

        view.getPairs().addActionListener(val -> {
            classGenerationAction.setMakePairs(view.getPairs().isSelected());
            getParentController().getProjectController().setDirty(true);
        });

        view.getOverwrite().addActionListener(val -> {
            classGenerationAction.setOverwrite(view.getOverwrite().isSelected());
            getParentController().getProjectController().setDirty(true);
        });

        view.getCreatePropertyNames().addActionListener(val -> {
            classGenerationAction.setCreatePropertyNames(view.getCreatePropertyNames().isSelected());
            getParentController().getProjectController().setDirty(true);
        });

        view.getUsePackagePath().addActionListener(val -> {
            classGenerationAction.setUsePkgPath(view.getUsePackagePath().isSelected());
            getParentController().getProjectController().setDirty(true);
        });

        view.getSubclassTemplate().addActionListener(val -> {
            classGenerationAction.setTemplate(getApplication().getCodeTemplateManager().getTemplatePath(String.valueOf(view.getSubclassTemplate().getSelectedItem())));
            getParentController().getProjectController().setDirty(true);
        });

        view.getSuperclassTemplate().addActionListener(val -> {
            classGenerationAction.setSuperTemplate(getApplication().getCodeTemplateManager().getTemplatePath(String.valueOf(view.getSuperclassTemplate().getSelectedItem())));
            getParentController().getProjectController().setDirty(true);
        });

        view.getGenerationMode().addActionListener(val -> {
            classGenerationAction.setArtifactsGenerationMode(modesByLabel.get(view.getGenerationMode().getSelectedItem()));
            getParentController().getProjectController().setDirty(true);
        });

        view.getOutputPattern().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                classGenerationAction.setOutputPattern(view.getOutputPattern().getText());
                getParentController().getProjectController().setDirty(true);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {}

            @Override
            public void changedUpdate(DocumentEvent e) {}
        });

        view.getSuperclassPackage().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                classGenerationAction.setSuperPkg(view.getSuperclassPackage().getText());
                getParentController().getProjectController().setDirty(true);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {}

            @Override
            public void changedUpdate(DocumentEvent e) {}
        });
    }

    public void initForm(ClassGenerationAction classGenerationAction){
        this.classGenerationAction = classGenerationAction;
        view.setOutputFolder(classGenerationAction.getDir());
        view.setGenerationMode(labelByMode.get(classGenerationAction.getArtifactsGenerationMode()));
        view.setTemplate(getApplication().getCodeTemplateManager().getNameByPath(classGenerationAction.getTemplate()));
        view.setSuperclassTemplate(getApplication().getCodeTemplateManager().getNameByPath(classGenerationAction.getSuperclassTemplate()));
        view.setDataMapName(classGenerationAction.getDataMap().getName());
        view.setOutputPattern(classGenerationAction.getOutputPattern());
        view.setPairs(classGenerationAction.isMakePairs());
        view.setUsePackagePath(classGenerationAction.isUsePkgPath());
        view.setOverwrite(classGenerationAction.isOverwrite());
        view.setCreatePropertyNames(classGenerationAction.isCreatePropertyNames());
        view.setSuperclassPackage(classGenerationAction.getSuperPkg());
    }
}
