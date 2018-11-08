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

import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.modeler.CodeTemplateManager;
import org.apache.cayenne.modeler.dialog.pref.PreferenceDialog;
import org.apache.cayenne.swing.BindingBuilder;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A controller for the custom generation mode.
 */
public class CustomModeController extends GeneratorController {

    protected CustomModePanel view;

    public CustomModeController(CodeGeneratorControllerBase parent) {
        super(parent);
        bind();
        initListeners();
    }

    @Override
    protected GeneratorControllerPanel createView() {
        this.view = new CustomModePanel(getApplication().getFrameController().getProjectController());
        return view;
    }

    private void bind() {
        BindingBuilder builder = new BindingBuilder(getApplication().getBindingFactory(), this);
        builder.bindToAction(view.getManageTemplatesLink(), "popPreferencesAction()");

        updateTemplates();
    }

    protected void updateTemplates() {
        CodeTemplateManager templateManager = getApplication().getCodeTemplateManager();

        List<String> customTemplates = new ArrayList<>(templateManager.getCustomTemplates().keySet());
        Collections.sort(customTemplates);

        List<String> superTemplates = new ArrayList<>(templateManager.getStandardSuperclassTemplates());
        Collections.sort(superTemplates);
        superTemplates.addAll(customTemplates);

        List<String> subTemplates = new ArrayList<>(templateManager.getStandardSubclassTemplates());
        Collections.sort(subTemplates);
        subTemplates.addAll(customTemplates);

        this.view.getSubclassTemplate().getComboBox().setModel(new DefaultComboBoxModel<>(subTemplates.toArray(new String[0])));
        this.view.getSuperclassTemplate().getComboBox().setModel(new DefaultComboBoxModel<>(superTemplates.toArray(new String[0])));
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
        view.getSubclassTemplate().setItem(getApplication().getCodeTemplateManager().getNameByPath(cgenConfiguration.getTemplate()));
        view.getSuperclassTemplate().setItem(getApplication().getCodeTemplateManager().getNameByPath(cgenConfiguration.getSuperTemplate()));
        view.setDisableSuperComboBoxes(view.getPairs().isSelected());
    }

    @Override
    public CgenConfiguration createConfiguration() {
        return super.createConfiguration();
    }

    private void initListeners(){
        view.getPairs().addActionListener(val -> {
            cgenConfiguration.setMakePairs(view.getPairs().isSelected());
            if(!view.getPairs().isSelected()) {
                cgenConfiguration.setTemplate(ClassGenerationAction.SINGLE_CLASS_TEMPLATE);
                cgenConfiguration.setEmbeddableTemplate(ClassGenerationAction.EMBEDDABLE_SINGLE_CLASS_TEMPLATE);
                cgenConfiguration.setQueryTemplate(ClassGenerationAction.DATAMAP_SINGLE_CLASS_TEMPLATE);
            } else {
                cgenConfiguration.setTemplate(ClassGenerationAction.SUBCLASS_TEMPLATE);
                cgenConfiguration.setEmbeddableTemplate(ClassGenerationAction.EMBEDDABLE_SUBCLASS_TEMPLATE);
                cgenConfiguration.setQueryTemplate(ClassGenerationAction.DATAMAP_SUBCLASS_TEMPLATE);
            }
            initForm(cgenConfiguration);
            getParentController().getProjectController().setDirty(true);
        });

        view.getOverwrite().addActionListener(val -> {
            cgenConfiguration.setOverwrite(view.getOverwrite().isSelected());
            getParentController().getProjectController().setDirty(true);
        });

        view.getCreatePropertyNames().addActionListener(val -> {
            cgenConfiguration.setCreatePropertyNames(view.getCreatePropertyNames().isSelected());
            getParentController().getProjectController().setDirty(true);
        });

        view.getUsePackagePath().addActionListener(val -> {
            cgenConfiguration.setUsePkgPath(view.getUsePackagePath().isSelected());
            getParentController().getProjectController().setDirty(true);
        });

        view.getPkProperties().addActionListener(val -> {
            cgenConfiguration.setCreatePKProperties(view.getPkProperties().isSelected());
            getParentController().getProjectController().setDirty(true);
        });
    }

    public void initForm(CgenConfiguration cgenConfiguration){
        super.initForm(cgenConfiguration);
        view.getOutputPattern().setText(cgenConfiguration.getOutputPattern());
        view.getPairs().setSelected(cgenConfiguration.isMakePairs());
        view.getUsePackagePath().setSelected(cgenConfiguration.isUsePkgPath());
        view.getOverwrite().setSelected(cgenConfiguration.isOverwrite());
        view.getCreatePropertyNames().setSelected(cgenConfiguration.isCreatePropertyNames());
        view.getPkProperties().setSelected(cgenConfiguration.isCreatePKProperties());
        if(cgenConfiguration.getArtifactsGenerationMode().equalsIgnoreCase("all")) {
            ((CodeGeneratorControllerBase) parent).setCurrentClass(cgenConfiguration.getDataMap());
            ((CodeGeneratorControllerBase) parent).setSelected(true);
        }
        updateComboBoxes();
    }

    @Override
    public void updateConfiguration(CgenConfiguration cgenConfiguration) {
        cgenConfiguration.setClient(false);
    }
}
