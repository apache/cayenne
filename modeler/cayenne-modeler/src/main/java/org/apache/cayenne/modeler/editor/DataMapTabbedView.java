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

import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.editor.cgen.CgenController;
import org.apache.cayenne.modeler.editor.cgen.domain.CgenTab;
import org.apache.cayenne.modeler.editor.dbimport.DbImportView;
import org.apache.cayenne.modeler.editor.dbimport.domain.DbImportTab;
import org.apache.cayenne.modeler.event.display.DataMapDisplayEvent;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;

public class DataMapTabbedView extends JTabbedPane {

    private final DbImportView dbImportView;
    private final JScrollPane dbImportScrollPane;
    private final CgenController cgenController;
    private final JScrollPane cgenView;
    private int lastTabIndex;

    public DataMapTabbedView(ProjectController controller) {

        setTabPlacement(JTabbedPane.TOP);

        // add panels to tabs
        // note that those panels that have no internal scrollable tables
        // must be wrapped in a scroll pane
        JScrollPane dataMapScrollPane = new JScrollPane(new DataMapView(controller));
        dbImportView = new DbImportView(controller);
        dbImportScrollPane = new JScrollPane(dbImportView);
        cgenController = new CgenController(controller);
        cgenView = new JScrollPane(cgenController.getView());
        addTab("DataMap", dataMapScrollPane);
        addTab("DB Import", dbImportScrollPane);
        addTab("Class Generation", cgenView);

        addChangeListener(this::stateChanged);
        controller.addDataMapDisplayListener(this::currentDataMapChanged);
    }

    private void currentDataMapChanged(DataMapDisplayEvent e) {
        if (e.getSource() instanceof CgenTab) {
            setSelectedComponent(cgenView);
        } else if (e.getSource() instanceof DbImportTab) {
            setSelectedComponent(dbImportScrollPane);
        } else {
            if (e.isMainTabFocus()) {
                lastTabIndex = 0;
            }
            setSelectedIndex(lastTabIndex);
        }
    }

    private void stateChanged(ChangeEvent e) {
        lastTabIndex = getSelectedIndex();
        if (getSelectedComponent() == cgenView) {
            cgenController.initFromModel();
        } else if (getSelectedComponent() == dbImportScrollPane) {
            dbImportView.initFromModel();
        }
    }
}

