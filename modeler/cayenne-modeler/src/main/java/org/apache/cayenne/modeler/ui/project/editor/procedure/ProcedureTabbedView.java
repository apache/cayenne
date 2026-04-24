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

package org.apache.cayenne.modeler.ui.project.editor.procedure;

import org.apache.cayenne.modeler.ui.project.editor.query.ExistingSelectionProcessor;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.action.RemoveProcedureParameterAction;
import org.apache.cayenne.modeler.event.display.ProcedureDisplayEvent;
import org.apache.cayenne.modeler.event.display.ProcedureParameterDisplayEvent;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;

public class ProcedureTabbedView extends JTabbedPane {

    private final ProcedureTab procedurePanel;
    private final ProcedureParameterTab procedureParameterPanel;

    public ProcedureTabbedView(ProjectController controller) {
        setTabPlacement(JTabbedPane.TOP);
        procedurePanel = new ProcedureTab(controller);
        addTab("Procedure", new JScrollPane(procedurePanel));
        procedureParameterPanel = new ProcedureParameterTab(controller);
        addTab("Parameters", procedureParameterPanel);

        controller.addProcedureDisplayListener(this::currentProcedureChanged);
        controller.addProcedureParameterDisplayListener(this::currentProcedureParameterChanged);
        addChangeListener(this::stateChanged);
    }

    private void stateChanged(ChangeEvent e) {
        Component selected = getSelectedComponent();
        while (selected instanceof JScrollPane) {
            selected = ((JScrollPane) selected).getViewport().getView();
        }

        ((ExistingSelectionProcessor) selected).processExistingSelection(e);
    }

    private void currentProcedureChanged(ProcedureDisplayEvent e) {
        Application.getInstance().getActionManager().getAction(
                RemoveProcedureParameterAction.class).setEnabled(false);

        if (e.getProcedure() == null)
            setVisible(false);
        else {
            if (e.isTabReset()) {
                this.setSelectedIndex(0);
            }
            this.setVisible(true);
        }
    }

    private void currentProcedureParameterChanged(ProcedureParameterDisplayEvent e) {
        ProcedureParameter[] parameters = e.getProcedureParameters();

        if (parameters.length > 0) {
            setSelectedComponent(procedureParameterPanel);
            procedureParameterPanel.selectParameters(parameters);
        }
    }
}
