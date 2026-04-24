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
package org.apache.cayenne.modeler.ui.project.editor.datamap;

import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.ui.project.editor.datamap.cgen.CgenController;
import org.apache.cayenne.modeler.ui.project.editor.datadomain.cgen.CgenTab;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.DbImportView;
import org.apache.cayenne.modeler.ui.project.editor.datadomain.dbimport.DbImportTab;
import org.apache.cayenne.modeler.event.display.DataMapDisplayEvent;
import org.apache.cayenne.modeler.ui.project.editor.datamap.main.DataMapMainView;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;

public class DataMapView extends JTabbedPane {

    private final JScrollPane dbImportPane;
    private final JScrollPane cgenPane;

    private int lastTabIndex;

    public DataMapView(ProjectController controller) {

        setTabPlacement(JTabbedPane.TOP);

        // add panels to tabs
        // note that those panels that have no internal scrollable tables
        // must be wrapped in a scroll pane
        JScrollPane dataMapScrollPane = new JScrollPane(new DataMapMainView(controller));
        dbImportPane = new JScrollPane(new DbImportView(controller));
        cgenPane = new JScrollPane(new CgenController(controller).getView());
        addTab("DataMap", dataMapScrollPane);
        addTab("DB Import", dbImportPane);
        addTab("Class Generation", cgenPane);

        addChangeListener(this::stateChanged);
        controller.addDataMapDisplayListener(this::currentDataMapChanged);
    }

    private void currentDataMapChanged(DataMapDisplayEvent e) {
        if (e.getSource() instanceof CgenTab) {
            setSelectedComponent(cgenPane);
        } else if (e.getSource() instanceof DbImportTab) {
            setSelectedComponent(dbImportPane);
        } else {
            if (e.isMainTabFocus()) {
                lastTabIndex = 0;
            }

            setSelectedIndex(lastTabIndex);
        }
    }

    private void stateChanged(ChangeEvent e) {
        lastTabIndex = getSelectedIndex();
    }
}

