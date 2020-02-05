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
package org.apache.cayenne.modeler.editor;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.editor.cgen.CodeGeneratorController;
import org.apache.cayenne.modeler.editor.cgen.domain.CgenTab;
import org.apache.cayenne.modeler.editor.dbimport.DbImportView;
import org.apache.cayenne.modeler.editor.dbimport.domain.DbImportTab;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

/**
 * Data map editing tabs container
 *
 */
public class DataMapTabbedView extends JTabbedPane {

    ProjectController mediator;
    private DbImportView dbImportView;
    private JScrollPane dbImportScrollPane;
    private CodeGeneratorController codeGeneratorController;
    private JScrollPane cgenView;

    /**
     * constructor
     *
     * @param mediator mediator instance
     */
    public DataMapTabbedView(ProjectController mediator) {
        this.mediator = mediator;
        initView();
    }

    /**
     * create tabs
     */
    private void initView() {
      
        setTabPlacement(JTabbedPane.TOP);

        // add panels to tabs
        // note that those panels that have no internal scrollable tables
        // must be wrapped in a scroll pane
        JScrollPane dataMapScrollPane = new JScrollPane(new DataMapView(mediator));
        dbImportView = new DbImportView(mediator);
        dbImportScrollPane = new JScrollPane(dbImportView);
        codeGeneratorController = new CodeGeneratorController(mediator);
        cgenView = new JScrollPane(codeGeneratorController.getView());
        addTab("DataMap", dataMapScrollPane);
        addTab("DB Import", dbImportScrollPane);
        addTab("Class Generation", cgenView);

        addChangeListener(tab -> {
            if(isCgenTabActive()) {
                codeGeneratorController.initFromModel();
            } else if(isDbImportTabActive()) {
                dbImportView.initFromModel();
            }
        });
        mediator.addDataMapDisplayListener(e -> {
            if(e.getSource() instanceof CgenTab) {
                setSelectedComponent(cgenView);
            } else if(e.getSource() instanceof DbImportTab) {
                setSelectedComponent(dbImportScrollPane);
            } else if(isCgenTabActive() || isDbImportTabActive()) {
                fireStateChanged();
            }
        });
    }

    private boolean isCgenTabActive() {
        return getSelectedComponent() == cgenView;
    }

    private boolean isDbImportTabActive() {
        return getSelectedComponent() == dbImportScrollPane;
    }
}

