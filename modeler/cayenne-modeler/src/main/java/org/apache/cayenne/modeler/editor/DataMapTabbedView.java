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
package org.apache.cayenne.modeler.editor;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.editor.cgen.CodeGeneratorController;
import org.apache.cayenne.modeler.editor.cgen.domain.CgenTab;
import org.apache.cayenne.modeler.editor.dbimport.DbImportView;

import javax.swing.*;


/**
 * Data map editing tabs container
 *
 */
public class DataMapTabbedView extends JTabbedPane{
    ProjectController mediator;
    private CodeGeneratorController codeGeneratorController;
    JScrollPane cgenView;


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
        JScrollPane dataMapView = new JScrollPane(new DataMapView(mediator));
        JScrollPane dbImportView = new JScrollPane(new DbImportView(mediator));
        this.codeGeneratorController = new CodeGeneratorController(Application.getInstance().getFrameController(), mediator);
        cgenView = new JScrollPane(codeGeneratorController.getView());
        addTab("DataMap", dataMapView);
        addTab("DbImport", dbImportView);
        addTab("Class Generation", cgenView);

        addChangeListener(tab -> {
            if(isCgenTabActive()) {
                codeGeneratorController.startup(mediator.getCurrentDataMap());
            }
        });
        mediator.addDataMapDisplayListener(e -> {
            if(isCgenTabActive()) {
                fireStateChanged();
            } else if(e.getSource() instanceof CgenTab){
                setSelectedComponent(cgenView);
            }
        });
    }

    private boolean isCgenTabActive() {
        return getSelectedComponent() == cgenView;
    }
}

