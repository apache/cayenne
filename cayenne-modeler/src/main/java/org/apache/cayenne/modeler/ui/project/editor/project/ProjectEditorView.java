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
package org.apache.cayenne.modeler.ui.project.editor.project;

import org.apache.cayenne.modeler.event.display.DbEntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.ProjectDisplayEvent;
import org.apache.cayenne.modeler.event.display.ObjEntityDisplayEvent;
import org.apache.cayenne.modeler.toolkit.ProjectTabbedPane;
import org.apache.cayenne.modeler.ui.action.ShowValidationConfigAction;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.project.editor.project.cgen.ProjectCgenTab;
import org.apache.cayenne.modeler.ui.project.editor.project.dbimport.ProjectDbImportTab;
import org.apache.cayenne.modeler.ui.project.editor.project.main.ProjectMainView;
import org.apache.cayenne.modeler.ui.project.editor.project.validation.ValidationTab;

import javax.swing.*;
import javax.swing.event.ChangeEvent;

public class ProjectEditorView extends ProjectTabbedPane {

    private final JComponent cgenView;
    private final ProjectCgenTab cgenTab;
    private final JComponent dbImportView;
    private final ProjectDbImportTab dbImportTab;
    private final ValidationTab validationTab;

    public ProjectEditorView(ProjectSession session) {
        super(session);
        this.dbImportTab = new ProjectDbImportTab(session);
        this.dbImportView = new JScrollPane(dbImportTab);
        this.cgenTab = new ProjectCgenTab(session);
        this.cgenView = new JScrollPane(cgenTab);
        this.validationTab = new ValidationTab(session);
        initLayout();
        initBindings();
    }

    private void initLayout() {
        setTabPlacement(JTabbedPane.TOP);
        addTab("Project", new JScrollPane(new ProjectMainView(session)));
        addTab("Db Import", dbImportView);
        addTab("Class Generation", cgenView);
        addTab("Validation", validationTab);
    }

    private void initBindings() {
        addChangeListener(this::stateChanged);
        session.addProjectDisplayListener(this::currentProjectChanged);
        session.addObjEntityDisplayListener(this::onEntitySelected);
        session.addDbEntityDisplayListener(this::onEntitySelected);
    }

    private void onEntitySelected(ObjEntityDisplayEvent e) {
    }

    private void onEntitySelected(DbEntityDisplayEvent e) {
    }

    private void stateChanged(ChangeEvent e) {
        if (getSelectedComponent() == cgenView) {
            cgenTab.initView();
        } else if (getSelectedComponent() == dbImportView) {
            dbImportTab.initView();
        }
    }

    private void currentProjectChanged(ProjectDisplayEvent e) {
        if (getSelectedComponent() == cgenView) {
            fireStateChanged();
        }
        if (getSelectedComponent() == dbImportView) {
            fireStateChanged();
        }
        if (e.getSource() instanceof ShowValidationConfigAction) {
            setSelectedComponent(validationTab);
        }
    }
}
