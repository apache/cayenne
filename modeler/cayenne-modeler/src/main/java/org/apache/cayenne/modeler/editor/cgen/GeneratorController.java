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
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.pref.FSPath;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.util.Util;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import java.io.File;

/**
 * @since 4.1
 * A mode-specific part of the code generation dialog.
 */
public abstract class GeneratorController <T extends StandardModePanel> extends CayenneController {

    protected CgenConfiguration cgenConfiguration;
    protected T view;

    protected GeneratorController(CodeGeneratorController parent) {
        super(parent);
        initializeView();
        initBindings(new BindingBuilder(getApplication().getBindingFactory(), this));
        initListeners();
    }

    protected CodeGeneratorController getParentController() {
        return (CodeGeneratorController) getParent();
    }

    protected void initializeView() {
        this.view = createView();
    }

    protected abstract T createView() ;

    protected void initBindings(BindingBuilder bindingBuilder) {
        JButton outputSelect = getView().getSelectOutputFolder();
        bindingBuilder.bindToAction(outputSelect, "selectOutputFolderAction()");
    }

    @Override
    public abstract GeneratorControllerPanel getView();

    protected void initForm(CgenConfiguration cgenConfiguration) {
        this.cgenConfiguration = cgenConfiguration;

        if (cgenConfiguration.getRootPath() != null) {
            getView().getOutputFolder().setText(cgenConfiguration.buildPath().toString());
        }
        if(cgenConfiguration.getArtifactsGenerationMode().equalsIgnoreCase("all")) {
            getParentController().setCurrentClass(cgenConfiguration.getDataMap());
            getParentController().setSelected(true);
        }
        view.getPairs().setSelected(cgenConfiguration.isMakePairs());
        view.getUsePackagePath().setSelected(cgenConfiguration.isUsePkgPath());
        view.getOverwrite().setSelected(cgenConfiguration.isOverwrite());
        view.getCreatePropertyNames().setSelected(cgenConfiguration.isCreatePropertyNames());
        view.getPkProperties().setSelected(cgenConfiguration.isCreatePKProperties());
        view.getSuperPkg().setText(cgenConfiguration.getSuperPkg());
    }

    public abstract void updateConfiguration(CgenConfiguration cgenConfiguration);

    /**
     * An action method that pops up a file chooser dialog to pick the
     * generation directory.
     */
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

    protected void initListeners() {
        this.view.getPairs().addActionListener(val -> {
            cgenConfiguration.setMakePairs(view.getPairs().isSelected());
            if (!view.getPairs().isSelected()) {
                cgenConfiguration.setTemplate(ClassGenerationAction.SINGLE_CLASS_TEMPLATE);
                cgenConfiguration.setEmbeddableTemplate(ClassGenerationAction.EMBEDDABLE_SINGLE_CLASS_TEMPLATE);
                cgenConfiguration.setQueryTemplate(ClassGenerationAction.DATAMAP_SINGLE_CLASS_TEMPLATE);
            } else {
                cgenConfiguration.setTemplate(ClassGenerationAction.SUBCLASS_TEMPLATE);
                cgenConfiguration.setEmbeddableTemplate(ClassGenerationAction.EMBEDDABLE_SUBCLASS_TEMPLATE);
                cgenConfiguration.setQueryTemplate(ClassGenerationAction.DATAMAP_SUBCLASS_TEMPLATE);
            }
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
            GeneratorController.this.getParentController().checkCgenConfigDirty();
        });

    }

}
