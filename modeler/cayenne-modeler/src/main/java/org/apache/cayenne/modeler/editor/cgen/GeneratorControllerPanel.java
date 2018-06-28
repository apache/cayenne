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
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.TextAdapter;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.io.File;

/**
 * A generic panel that is a superclass of generator panels, defining common fields.
 * 
 */
public class GeneratorControllerPanel extends JPanel {

    protected TextAdapter outputFolder;
    protected JButton selectOutputFolder;

    ProjectController projectController;

    public GeneratorControllerPanel(ProjectController projectController) {
        this.projectController = projectController;
        JTextField outputFolderField = new JTextField();
        this.outputFolder = new TextAdapter(outputFolderField) {
            protected void updateModel(String text) {
                getCgenByDataMap().setDestDir(new File(text));
                projectController.setDirty(true);
            }
        };
        this.selectOutputFolder = new JButton("Select");
    }

    public ClassGenerationAction getCgenByDataMap() {
        DataMap dataMap = projectController.getCurrentDataMap();
        return projectController.getApplication().getMetaData().get(dataMap, ClassGenerationAction.class);
    }
    public TextAdapter getOutputFolder() {
        return outputFolder;
    }

    public JButton getSelectOutputFolder() {
        return selectOutputFolder;
    }
}
