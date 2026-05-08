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

import org.apache.cayenne.modeler.event.display.DbEntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.DomainDisplayEvent;
import org.apache.cayenne.modeler.event.display.ObjEntityDisplayEvent;
import org.apache.cayenne.modeler.toolkit.ProjectTabbedPane;
import org.apache.cayenne.modeler.ui.action.ShowValidationConfigAction;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.project.editor.datadomain.cgen.DataDomainCgenTab;
import org.apache.cayenne.modeler.ui.project.editor.datadomain.dbimport.DataDomainDbImportTab;
import org.apache.cayenne.modeler.ui.project.editor.datadomain.main.DataDomainMainView;
import org.apache.cayenne.modeler.ui.project.editor.datadomain.validation.ValidationTab;

import javax.swing.*;
import javax.swing.event.ChangeEvent;

public class DataDomainView extends ProjectTabbedPane {

    private final JComponent cgenView;
    private final DataDomainCgenTab cgenTab;
    private final JComponent dbImportView;
    private final DataDomainDbImportTab dbImportTab;
    private final ValidationTab validationTab;

    public DataDomainView(ProjectSession session) {
        super(session);
        this.dbImportTab = new DataDomainDbImportTab(session);
        this.dbImportView = new JScrollPane(dbImportTab);
        this.cgenTab = new DataDomainCgenTab(session);
        this.cgenView = new JScrollPane(cgenTab);
        this.validationTab = new ValidationTab(session);
        initLayout();
        initBindings();
    }

    private void initLayout() {
        setTabPlacement(JTabbedPane.TOP);
        addTab("Main", new JScrollPane(new DataDomainMainView(session)));
        addTab("Db Import", dbImportView);
        addTab("Class Generation", cgenView);
        addTab("Validation", validationTab);
    }

    private void initBindings() {
        addChangeListener(this::stateChanged);
        session.addDomainDisplayListener(this::currentDomainChanged);
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

    private void currentDomainChanged(DomainDisplayEvent e) {
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
