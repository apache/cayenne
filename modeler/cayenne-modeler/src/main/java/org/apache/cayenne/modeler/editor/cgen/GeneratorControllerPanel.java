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
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.validation.ValidationException;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @since 4.1
 * A generic panel that is a superclass of generator panels, defining common fields.
 */
public class GeneratorControllerPanel extends JPanel {

    protected TextAdapter outputFolder;
    protected JButton selectOutputFolder;
    protected ProjectController projectController;
    protected CodeGeneratorController codeGeneratorController;

    public GeneratorControllerPanel(ProjectController projectController, CodeGeneratorController codeGeneratorController) {
        this.projectController = projectController;
        this.codeGeneratorController = codeGeneratorController;
        this.outputFolder = new TextAdapter(new JTextField()) {
            @Override
            protected void updateModel(String text) throws ValidationException {

                CgenConfiguration cgenByDataMap = getCgenConfig();

                if (cgenByDataMap != null) {

                    if (cgenByDataMap.getRootPath() == null && !Paths.get(text).isAbsolute()) {
                        throw new ValidationException("You should save project to use rel path as output directory ");
                    }
                    cgenByDataMap.setRelPath(text);
                    checkConfigDirty();
                }
            }
        };
        this.selectOutputFolder = new JButton("..");
    }

    public TextAdapter getOutputFolder() {
        return outputFolder;
    }

    public JButton getSelectOutputFolder() {
        return selectOutputFolder;
    }

    protected void checkConfigDirty() {
        if (!codeGeneratorController.isInitFromModel()) {
            codeGeneratorController.checkCgenConfigDirty();
        }
    }

    protected CgenConfiguration getCgenConfig() {
        return codeGeneratorController.getCgenConfiguration();
    }
}
