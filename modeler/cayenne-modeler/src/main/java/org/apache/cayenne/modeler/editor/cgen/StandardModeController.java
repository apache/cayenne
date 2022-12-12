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
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.editor.cgen.templateeditor.TemplateEditorController;
import org.apache.cayenne.modeler.pref.DataMapDefaults;
import org.apache.cayenne.modeler.pref.FSPath;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.util.Util;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import java.io.File;

/**
 * @since 4.1
 */
public class StandardModeController extends CayenneController {

    private CgenConfiguration cgenConfiguration;
    protected StandardModePanel view;
    protected DataMapDefaults preferences;
    protected CodeGeneratorController codeGeneratorController;
    private boolean isEditorOpen;
    private static final String EDITED = " (edited)";

    public StandardModeController(CodeGeneratorController parent) {
        super(parent);
        this.codeGeneratorController = parent;
        this.view = new StandardModePanel(getParentController());
        isEditorOpen = false;
        initListeners();
        initBindings(new BindingBuilder(getApplication().getBindingFactory(), this));
    }


    protected void initListeners() {
        this.view.getPairs().addActionListener(val -> {
            cgenConfiguration.setMakePairs(view.getPairs().isSelected());
            if (!view.getPairs().isSelected()) {
                setSingleclassForDefaults();
            } else {
                setSubclassForDefaults();
            }
            updateTemplateEditorButtons();
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

    private void initBindings(BindingBuilder bindingBuilder) {
        JButton outputSelect = getView().getSelectOutputFolder();
        bindingBuilder.bindToAction(outputSelect, "selectOutputFolderAction()");
    }

    protected CodeGeneratorController getParentController() {
        return (CodeGeneratorController) getParent();
    }


    private void setSubclassForDefaults() {
        if (TemplateType.isDefault(cgenConfiguration.getTemplate().getData())) {
            cgenConfiguration.setTemplate(TemplateType.ENTITY_SUBCLASS.defaultTemplate());
        }
        if (TemplateType.isDefault(cgenConfiguration.getEmbeddableTemplate().getData())) {
            cgenConfiguration.setEmbeddableTemplate(TemplateType.EMBEDDABLE_SUBCLASS.defaultTemplate());
        }
        if (TemplateType.isDefault(cgenConfiguration.getDataMapTemplate().getData())) {
            cgenConfiguration.setDataMapTemplate(TemplateType.DATAMAP_SUBCLASS.defaultTemplate());
        }
    }

    private void setSingleclassForDefaults() {
        if (TemplateType.isDefault(cgenConfiguration.getTemplate().getData())) {
            cgenConfiguration.setTemplate(TemplateType.ENTITY_SINGLE_CLASS.defaultTemplate());
        }
        if (TemplateType.isDefault(cgenConfiguration.getEmbeddableTemplate().getData())) {
            cgenConfiguration.setEmbeddableTemplate(TemplateType.EMBEDDABLE_SINGLE_CLASS.defaultTemplate());
        }
        if (TemplateType.isDefault(cgenConfiguration.getDataMapTemplate().getData())) {
            cgenConfiguration.setDataMapTemplate(TemplateType.DATAMAP_SINGLE_CLASS.defaultTemplate());
        }
    }

    public StandardModePanel getView() {
        return view;
    }

    public void initForm(CgenConfiguration cgenConfiguration) {
        this.cgenConfiguration = cgenConfiguration;

        if (cgenConfiguration.getRootPath() != null) {
            getView().getOutputFolder().setText(cgenConfiguration.buildOutputPath().toString());
        }
        if(cgenConfiguration.getArtifactsGenerationMode().equalsIgnoreCase("all")) {
            getParentController().setCurrentClass(cgenConfiguration.getDataMap());
            getParentController().setSelected(true);
        }
        getView().getOutputFolder().updateModel();
        view.getOutputPattern().setText(cgenConfiguration.getOutputPattern());
        view.getPairs().setSelected(cgenConfiguration.isMakePairs());
        view.getUsePackagePath().setSelected(cgenConfiguration.isUsePkgPath());
        view.getOverwrite().setSelected(cgenConfiguration.isOverwrite());
        view.getCreatePropertyNames().setSelected(cgenConfiguration.isCreatePropertyNames());
        view.getPkProperties().setSelected(cgenConfiguration.isCreatePKProperties());
        view.getSuperPkg().setText(cgenConfiguration.getSuperPkg());
        updateTemplatesLabels(cgenConfiguration);
    }

    /**
     * Adds or removes mark "edited" to template labels.
     * @param configuration
     */
    public void updateTemplatesLabels(CgenConfiguration configuration) {
        updateTemplateLabel(view.getEntityTemplateLbl(), TemplateType.ENTITY_SUBCLASS, configuration.getTemplate().getData());
        updateTemplateLabel(view.getEntitySuperTemplateLbl(), TemplateType.ENTITY_SUPERCLASS, configuration.getSuperTemplate().getData());
        updateTemplateLabel(view.getEmbeddableTemplateLbl(), TemplateType.EMBEDDABLE_SUBCLASS, configuration.getEmbeddableTemplate().getData());
        updateTemplateLabel(view.getEmbeddableSuperTemplateLbl(), TemplateType.EMBEDDABLE_SUPERCLASS, configuration.getEmbeddableSuperTemplate().getData());
        updateTemplateLabel(view.getDatamapTemplateLbl(), TemplateType.DATAMAP_SUBCLASS, configuration.getDataMapTemplate().getData());
        updateTemplateLabel(view.getDatamapSuperTemplateLbl(), TemplateType.DATAMAP_SUPERCLASS, configuration.getDataMapSuperTemplate().getData());
    }

    private void updateTemplateLabel(JLabel label, TemplateType type, String template) {
        if (!TemplateType.isDefault(template)) {
            label.setText(type.readableName() + EDITED);
        } else {
            label.setText(type.readableName());
        }
    }


    /**
     * locks or unlocks buttons depending on the state of the window. Tooltips are changing too.
     * The button locking is affected:
     * - is template editor window open
     * - is MakePairs checkbox selected
     * - is any artefact of appropriate type selected
     */
    public void updateTemplateEditorButtons() {
        boolean isMakePairs = view.getPairs().isSelected();
        boolean isEntitiesSelected = getParentController().isEntitiesSelected();
        boolean isEmbeddableSelected = getParentController().isEmbeddableSelected();
        boolean isDataMapSelected = getParentController().isDataMapSelected();

        view.getEditSubclassTemplateBtn().setEnabled(isEntitiesSelected && !isEditorOpen);
        view.getEditSuperclassTemplateBtn().setEnabled(isMakePairs && isEntitiesSelected && !isEditorOpen);

        view.getEditEmbeddableTemplateBtn().setEnabled(isEmbeddableSelected && !isEditorOpen);
        view.getEditEmbeddableSuperTemplateBtn().setEnabled(isMakePairs && isEmbeddableSelected && !isEditorOpen);

        view.getEditDataMapTemplateBtn().setEnabled(isDataMapSelected&& !isEditorOpen);
        view.getEditDataMapSuperTemplateBtn().setEnabled(isMakePairs && isDataMapSelected && !isEditorOpen);

        setToolTipText(view.getEditSubclassTemplateBtn());
        setToolTipText(view.getEditSuperclassTemplateBtn());
        setToolTipText(view.getEditEmbeddableTemplateBtn());
        setToolTipText(view.getEditEmbeddableSuperTemplateBtn());
        setToolTipText(view.getEditDataMapTemplateBtn());
        setToolTipText(view.getEditDataMapSuperTemplateBtn());
    }

    private void setToolTipText(JButton button) {
        if (button.isEnabled()) {
            button.setToolTipText("Open template editor");
        } else {
            button.setToolTipText("At least one artefact of appropriate type must be selected." +
                    " The Make Pairs checkbox can also affect the blocking");
        }
    }

    /**
     * An action method that pops up a file chooser dialog to pick the
     * generation directory.
     */
    @SuppressWarnings("unused")
    public void selectOutputFolderAction() {

        TextAdapter outputFolder = getView().getOutputFolder();
        String currentDir = outputFolder.getComponent().getText();

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);

        // guess start directory
        if (!Util.isEmptyString(currentDir)) {
            chooser.setCurrentDirectory(new File(currentDir));
        } else {
            FSPath lastDir = Application.getInstance().getFrameController().getLastDirectory();
            lastDir.updateChooser(chooser);
        }

        int result = chooser.showOpenDialog(getView());
        if (result == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();

            // update model
            String path = selected.getAbsolutePath();
            getView().getOutputFolder().setText(path);
            getView().getOutputFolder().updateModel();
        }
    }

    public CodeGeneratorController getCodeGeneratorController() {
        return codeGeneratorController;
    }

    public void setEditorOpen(boolean editorOpen) {
        isEditorOpen = editorOpen;
    }
}
