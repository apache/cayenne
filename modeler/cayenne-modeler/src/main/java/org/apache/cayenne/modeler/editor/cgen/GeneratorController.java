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
public abstract class GeneratorController extends CayenneController {

    protected CgenConfiguration cgenConfiguration;

    public GeneratorController(CodeGeneratorController parent) {
        super(parent);

        createView();
        initBindings(new BindingBuilder(getApplication().getBindingFactory(), this));
    }

    protected void initBindings(BindingBuilder bindingBuilder) {
        JButton outputSelect = getView().getSelectOutputFolder();
        bindingBuilder.bindToAction(outputSelect, "selectOutputFolderAction()");
    }

    protected CodeGeneratorController getParentController() {
        return (CodeGeneratorController) getParent();
    }

    protected abstract void createView();

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
}
