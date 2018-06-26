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
import org.apache.cayenne.modeler.CodeTemplateManager;
import org.apache.cayenne.modeler.dialog.pref.PreferenceDialog;
import org.apache.cayenne.swing.BindingBuilder;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * A controller for the custom generation mode.
 */
public class CustomModeController extends GeneratorController {

    // correspond to non-public constants on MapClassGenerator.
    private static final String MODE_DATAMAP = "datamap";
    private static final String MODE_ENTITY = "entity";
    private static final String MODE_ALL = "all";

    private static final String DATA_MAP_MODE_LABEL = "DataMap generation";
    private static final String ENTITY_MODE_LABEL = "Entity and Embeddable generation";
    static final String ALL_MODE_LABEL = "Generate all";

    static final Map<String, String> modesByLabel = new HashMap<>();

    private static final Map<String, String> labelByMode = new HashMap<>();

    static {
        modesByLabel.put(DATA_MAP_MODE_LABEL, MODE_DATAMAP);
        modesByLabel.put(ENTITY_MODE_LABEL, MODE_ENTITY);
        modesByLabel.put(ALL_MODE_LABEL, MODE_ALL);
        labelByMode.put(MODE_DATAMAP, DATA_MAP_MODE_LABEL);
        labelByMode.put(MODE_ENTITY, ENTITY_MODE_LABEL);
        labelByMode.put(MODE_ALL, ALL_MODE_LABEL);
    }

    protected CustomModePanel view;

    private ClassGenerationAction classGenerationAction;

    public CustomModeController(CodeGeneratorControllerBase parent) {
        super(parent);
        this.view = new CustomModePanel(parent.getProjectController());
        bind();
        initListeners();
    }

    private void bind() {
        initBindings(new BindingBuilder(getApplication().getBindingFactory(), this));
        BindingBuilder builder = new BindingBuilder(getApplication().getBindingFactory(), this);
        builder.bindToAction(view.getManageTemplatesLink(), "popPreferencesAction()");

        updateTemplates();
    }

    protected void updateTemplates() {
        Object[] modeChoices = new Object[]{ENTITY_MODE_LABEL, DATA_MAP_MODE_LABEL, ALL_MODE_LABEL};
        view.getGenerationMode().getComboBox().setModel(new DefaultComboBoxModel(modeChoices));

        CodeTemplateManager templateManager = getApplication().getCodeTemplateManager();

        List<String> customTemplates = new ArrayList<>(templateManager.getCustomTemplates().keySet());
        Collections.sort(customTemplates);

        List<String> superTemplates = new ArrayList<>(templateManager.getStandardSuperclassTemplates());
        Collections.sort(superTemplates);
        superTemplates.addAll(customTemplates);

        List<String> subTemplates = new ArrayList<>(templateManager.getStandardSubclassTemplates());
        Collections.sort(subTemplates);
        subTemplates.addAll(customTemplates);

        List<String> embeddableTemplates = new ArrayList<>(templateManager.getStandartEmbeddableTemplates());
        Collections.sort(embeddableTemplates);
        embeddableTemplates.addAll(customTemplates);

        List<String> embeddableSuperTemplates = new ArrayList<>(templateManager.getStandartEmbeddableSuperclassTemplates());
        Collections.sort(embeddableSuperTemplates);
        embeddableSuperTemplates.addAll(customTemplates);

        List<String> dataMapTemplates = new ArrayList<>(templateManager.getStandartDataMapTemplates());
        Collections.sort(dataMapTemplates);
        dataMapTemplates.addAll(customTemplates);

        List<String> dataMapSuperTemplates = new ArrayList<>(templateManager.getStandartDataMapSuperclassTemplates());
        Collections.sort(dataMapSuperTemplates);
        dataMapSuperTemplates.addAll(customTemplates);

        this.view.getSubclassTemplate().getComboBox().setModel(new DefaultComboBoxModel(subTemplates.toArray()));
        this.view.getSuperclassTemplate().getComboBox().setModel(new DefaultComboBoxModel(superTemplates.toArray()));

        this.view.getEmbeddableTemplate().getComboBox().setModel(new DefaultComboBoxModel(embeddableTemplates.toArray()));
        this.view.getEmbeddableSuperTemplate().getComboBox().setModel(new DefaultComboBoxModel(embeddableSuperTemplates.toArray()));

        this.view.getDataMapTemplate().getComboBox().setModel(new DefaultComboBoxModel(dataMapTemplates.toArray()));
        this.view.getDataMapSuperTemplate().getComboBox().setModel(new DefaultComboBoxModel(dataMapSuperTemplates.toArray()));
    }

    public Component getView() {
        return view;
    }

    public void popPreferencesAction() {
        new PreferenceDialog(getApplication().getFrameController()).startupAction(PreferenceDialog.TEMPLATES_KEY);
        updateTemplates();
        updateComboBoxes();
    }

    private void updateComboBoxes() {
        view.getEmbeddableTemplate().setItem(getApplication().getCodeTemplateManager().getNameByPath(classGenerationAction.getEmbeddableTemplate()));
        view.getEmbeddableSuperTemplate().setItem(getApplication().getCodeTemplateManager().getNameByPath(classGenerationAction.getEmbeddableSuperTemplate()));
        view.getSubclassTemplate().setItem(getApplication().getCodeTemplateManager().getNameByPath(classGenerationAction.getTemplate()));
        view.getSuperclassTemplate().setItem(getApplication().getCodeTemplateManager().getNameByPath(classGenerationAction.getSuperclassTemplate()));
        view.getGenerationMode().setItem(labelByMode.get(classGenerationAction.getArtifactsGenerationMode()));
        view.getDataMapTemplate().setItem(getApplication().getCodeTemplateManager().getNameByPath(classGenerationAction.getQueryTemplate()));
        view.getDataMapSuperTemplate().setItem(getApplication().getCodeTemplateManager().getNameByPath(classGenerationAction.getQuerySuperTemplate()));

        view.setDisableSuperComboBoxes(view.getPairs().isSelected());
    }

    @Override
    protected ClassGenerationAction newGenerator() {
        ClassGenerationAction action = new ClassGenerationAction();
        action.setDefaults();
        getApplication().getInjector().injectMembers(action);
        return action;
    }

    private void initListeners(){
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
    }

    public void initForm(ClassGenerationAction classGenerationAction){
        this.classGenerationAction = classGenerationAction;
        view.getOutputFolder().setText(classGenerationAction.getDir());
        view.setDataMapName(classGenerationAction.getDataMap().getName());
        view.getOutputPattern().setText(classGenerationAction.getOutputPattern());
        view.getPairs().setSelected(classGenerationAction.isMakePairs());
        view.getUsePackagePath().setSelected(classGenerationAction.isUsePkgPath());
        view.getOverwrite().setSelected(classGenerationAction.isOverwrite());
        view.getCreatePropertyNames().setSelected(classGenerationAction.isCreatePropertyNames());
        view.getSuperclassPackage().setText(classGenerationAction.getSuperPkg());
        view.getEncoding().setText(classGenerationAction.getEncoding());
        updateComboBoxes();
    }
}
