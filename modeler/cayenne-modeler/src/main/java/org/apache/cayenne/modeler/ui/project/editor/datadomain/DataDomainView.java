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
package org.apache.cayenne.modeler.ui.project.editor.datadomain;

import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.ui.action.GenerateCodeAction;
import org.apache.cayenne.modeler.ui.action.ShowValidationConfigAction;
import org.apache.cayenne.modeler.ui.project.editor.datadomain.cgen.DataDomainCgenController;
import org.apache.cayenne.modeler.ui.project.editor.datadomain.dbimport.DataDomainDbImportController;
import org.apache.cayenne.modeler.ui.project.editor.datadomain.main.DataDomainMainView;
import org.apache.cayenne.modeler.ui.project.editor.datadomain.validation.ValidationTabController;
import org.apache.cayenne.modeler.event.display.DomainDisplayEvent;
import org.apache.cayenne.modeler.event.display.EntityDisplayEvent;
import org.apache.cayenne.modeler.ui.project.editor.datadomain.graph.DataDomainGraphTab;

import javax.swing.*;
import javax.swing.event.ChangeEvent;

public class DataDomainView extends JTabbedPane {

    private final DataDomainGraphTab graphTab;
    private final JComponent cgenView;
    private final DataDomainCgenController cgenController;
    private final JComponent dbImportView;
    private final DataDomainDbImportController dbImportController;
    private final JComponent validationTabView;
    private final ValidationTabController validationTabController;

    public DataDomainView(ProjectController controller) {

        setTabPlacement(JTabbedPane.TOP);

        // add panels to tabs
        // note that those panels that have no internal scrollable tables
        // must be wrapped in a scroll pane
        JScrollPane domainView = new JScrollPane(new DataDomainMainView(controller));
        addTab("Main", domainView);

        dbImportController = new DataDomainDbImportController(controller);
        dbImportView = new JScrollPane(dbImportController.getView());
        addTab("Db Import", dbImportView);

        cgenController = new DataDomainCgenController(controller);
        cgenView = new JScrollPane(cgenController.getView());
        addTab("Class Generation", cgenView);

        graphTab = new DataDomainGraphTab(controller);
        addTab("Graph", graphTab);

        validationTabController = new ValidationTabController(controller);
        validationTabView = validationTabController.getView();
        addTab("Validation", validationTabView);

        addChangeListener(this::stateChanged);
        controller.addDomainDisplayListener(this::currentDomainChanged);
    }

    private void stateChanged(ChangeEvent e) {
        if (getSelectedComponent() == graphTab) {
            graphTab.refresh();
        } else if (getSelectedComponent() == cgenView) {
            cgenController.getView().initView();
        } else if (getSelectedComponent() == dbImportView) {
            dbImportController.getView().initView();
        } else if (getSelectedComponent() == validationTabView) {
            validationTabController.getView().initView();
        }
    }

    private void currentDomainChanged(DomainDisplayEvent e) {
        if (e instanceof EntityDisplayEvent) {
            //need select an entity
            setSelectedComponent(graphTab);
        }
        if (getSelectedComponent() == cgenView) {
            fireStateChanged();
        }
        if (e.getSource() instanceof GenerateCodeAction) {
            setSelectedComponent(cgenView);
        }
        if (getSelectedComponent() == dbImportView) {
            fireStateChanged();
        }
        if (e.getSource() instanceof ShowValidationConfigAction) {
            setSelectedComponent(validationTabView);
        }
    }
}
