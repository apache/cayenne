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

package org.apache.cayenne.modeler.editor.cgen;

import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.TemplateType;
import org.apache.cayenne.modeler.dialog.pref.PreferenceDialog;
import org.apache.cayenne.modeler.editor.cgen.templateeditor.TemplateEditorController;
import org.apache.cayenne.modeler.pref.DataMapDefaults;

import javax.swing.JLabel;

/**
 * @since 4.1
 */
public class StandardModeController extends GeneratorController {

    private static final String EDITED = " (edited)";
    protected StandardModePanel view;
    protected DataMapDefaults preferences;
    protected CodeGeneratorController codeGeneratorController;

    public StandardModeController(CodeGeneratorController codeGeneratorController) {
        super(codeGeneratorController);
        this.codeGeneratorController = codeGeneratorController;
        initListeners();
    }


    protected void initListeners() {
        this.view.getPairs().addActionListener(val -> {
            cgenConfiguration.setMakePairs(view.getPairs().isSelected());
            if (!view.getPairs().isSelected()) {
                setSingleclassForDefaults();
            } else {
                setSubclassForDefaults();
            }
            view.getEditSuperclassTemplateBtn().setEnabled(view.getPairs().isSelected());
            view.getEditEmbeddableSuperTemplateBtn().setEnabled(view.getPairs().isSelected());
            view.getEditDataMapSuperTemplateBtn().setEnabled(view.getPairs().isSelected());
            initForm(cgenConfiguration);
            getParentController().checkCgenConfigDirty();
        });

        view.getOverwrite().addActionListener(val -> {
            cgenConfiguration.setOverwrite(view.getOverwrite().isSelected());
            getParentController().checkCgenConfigDirty();
        });

        view.getCreatePropertyNames().addActionListener(val -> {
            cgenConfiguration.setCreatePropertyNames(view.getCreatePropertyNames().isSelected());
            getParentController().checkCgenConfigDirty();
        });

        view.getUsePackagePath().addActionListener(val -> {
            cgenConfiguration.setUsePkgPath(view.getUsePackagePath().isSelected());
            getParentController().checkCgenConfigDirty();
        });

        view.getPkProperties().addActionListener(val -> {
            cgenConfiguration.setCreatePKProperties(view.getPkProperties().isSelected());
            getParentController().checkCgenConfigDirty();
        });


        view.getEditSubclassTemplateBtn().addActionListener(val ->
                new TemplateEditorController(this, TemplateType.ENTITY_SUBCLASS).startupAction());

        view.getEditSuperclassTemplateBtn().addActionListener(val ->
                new TemplateEditorController(this, TemplateType.ENTITY_SUPERCLASS).startupAction());

        view.getEditEmbeddableTemplateBtn().addActionListener(val ->
                new TemplateEditorController(this, TemplateType.EMBEDDABLE_SUBCLASS).startupAction());

        view.getEditEmbeddableSuperTemplateBtn().addActionListener(val ->
                new TemplateEditorController(this, TemplateType.EMBEDDABLE_SUPERCLASS).startupAction());

        view.getEditDataMapTemplateBtn().addActionListener(val ->
                new TemplateEditorController(this, TemplateType.DATAMAP_SUBCLASS).startupAction());

        view.getEditDataMapSuperTemplateBtn().addActionListener(val ->
                new TemplateEditorController(this, TemplateType.DATAMAP_SUPERCLASS).startupAction());
    }

    private void setSubclassForDefaults() {
        if (TemplateType.isDefault(cgenConfiguration.getTemplate())) {
            cgenConfiguration.setTemplate(TemplateType.ENTITY_SUBCLASS.pathFromSourceRoot());
        }
        if (TemplateType.isDefault(cgenConfiguration.getEmbeddableTemplate())) {
            cgenConfiguration.setEmbeddableTemplate(TemplateType.EMBEDDABLE_SUBCLASS.pathFromSourceRoot());
        }
        if (TemplateType.isDefault(cgenConfiguration.getDataMapTemplate())) {
            cgenConfiguration.setDataMapTemplate(TemplateType.DATAMAP_SUBCLASS.pathFromSourceRoot());
        }
    }

    private void setSingleclassForDefaults() {
        if (TemplateType.isDefault(cgenConfiguration.getTemplate())) {
            cgenConfiguration.setTemplate(TemplateType.ENTITY_SINGLE_CLASS.pathFromSourceRoot());
        }
        if (TemplateType.isDefault(cgenConfiguration.getEmbeddableTemplate())) {
            cgenConfiguration.setEmbeddableTemplate(TemplateType.EMBEDDABLE_SINGLE_CLASS.pathFromSourceRoot());
        }
        if (TemplateType.isDefault(cgenConfiguration.getDataMapTemplate())) {
            cgenConfiguration.setDataMapTemplate(TemplateType.DATAMAP_SINGLE_CLASS.pathFromSourceRoot());
        }
    }


    protected void createView() {
        this.view = new StandardModePanel(getParentController());
    }

    public StandardModePanel getView() {
        return view;
    }

    @Override
    public void updateConfiguration(CgenConfiguration cgenConfiguration) {
        //noop
    }


    @SuppressWarnings("unused")
    public void popPreferencesAction() {
        new PreferenceDialog(getApplication().getFrameController()).startupAction(PreferenceDialog.TEMPLATES_KEY);
    }

    public void addTemplateAction(String template, String superTemplate) {
        new PreferenceDialog(getApplication().getFrameController()).startupToCreateTemplate(template, superTemplate);
    }


    @Override
    public void initForm(CgenConfiguration cgenConfiguration) {
        super.initForm(cgenConfiguration);
        view.getOutputPattern().setText(cgenConfiguration.getOutputPattern());
        view.getPairs().setSelected(cgenConfiguration.isMakePairs());
        view.getUsePackagePath().setSelected(cgenConfiguration.isUsePkgPath());
        view.getOverwrite().setSelected(cgenConfiguration.isOverwrite());
        view.getCreatePropertyNames().setSelected(cgenConfiguration.isCreatePropertyNames());
        view.getPkProperties().setSelected(cgenConfiguration.isCreatePKProperties());
        view.getSuperPkg().setText(cgenConfiguration.getSuperPkg());
        view.getEditSuperclassTemplateBtn().setEnabled(cgenConfiguration.isMakePairs());
        view.getEditEmbeddableSuperTemplateBtn().setEnabled(cgenConfiguration.isMakePairs());
        view.getEditDataMapSuperTemplateBtn().setEnabled(cgenConfiguration.isMakePairs());
        updateTemplatesLabels(cgenConfiguration);

    }

    public void updateTemplatesLabels(CgenConfiguration configuration) {
        updateTemplateLabel(view.getEntityTemplateLbl(), TemplateType.ENTITY_SUBCLASS, configuration.getTemplate());
        updateTemplateLabel(view.getEntitySuperTemplateLbl(), TemplateType.ENTITY_SUPERCLASS, configuration.getSuperTemplate());
        updateTemplateLabel(view.getEmbeddableTemplateLbl(), TemplateType.EMBEDDABLE_SUBCLASS, configuration.getEmbeddableTemplate());
        updateTemplateLabel(view.getEmbeddableSuperTemplateLbl(), TemplateType.EMBEDDABLE_SUPERCLASS, configuration.getEmbeddableSuperTemplate());
        updateTemplateLabel(view.getDatamapTemplateLbl(), TemplateType.DATAMAP_SUBCLASS, configuration.getDataMapTemplate());
        updateTemplateLabel(view.getDatamapSuperTemplateLbl(), TemplateType.DATAMAP_SUPERCLASS, configuration.getDataMapSuperTemplate());
    }

    private void updateTemplateLabel(JLabel label, TemplateType type, String template) {
        if (!TemplateType.isDefault(template)) {
            label.setText(type.readableName() + EDITED);
        } else {
            label.setText(type.readableName());
        }
    }

    public CodeGeneratorController getCodeGeneratorController() {
        return codeGeneratorController;
    }
}
