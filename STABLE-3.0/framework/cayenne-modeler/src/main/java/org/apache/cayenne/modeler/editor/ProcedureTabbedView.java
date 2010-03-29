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

import java.awt.Component;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.RemoveProcedureParameterAction;
import org.apache.cayenne.modeler.event.ProcedureDisplayEvent;
import org.apache.cayenne.modeler.event.ProcedureDisplayListener;
import org.apache.cayenne.modeler.event.ProcedureParameterDisplayEvent;
import org.apache.cayenne.modeler.event.ProcedureParameterDisplayListener;

/**
 * Tabbed panel for stored procedure editing.
 * 
 */
public class ProcedureTabbedView
    extends JTabbedPane
    implements ProcedureDisplayListener, ProcedureParameterDisplayListener {

    protected ProjectController eventController;
    protected ProcedureTab procedurePanel;
    protected ProcedureParameterTab procedureParameterPanel;

    public ProcedureTabbedView(ProjectController eventController) {
        this.eventController = eventController;

        // init view
        setTabPlacement(JTabbedPane.TOP);
        procedurePanel = new ProcedureTab(eventController);
        addTab("Procedure", new JScrollPane(procedurePanel));
        procedureParameterPanel = new ProcedureParameterTab(eventController);
        addTab("Parameters", procedureParameterPanel);

        // init listeners
        eventController.addProcedureDisplayListener(this);
        eventController.addProcedureParameterDisplayListener(this);
        this.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                // find source view
                Component selected = ProcedureTabbedView.this.getSelectedComponent();
                while (selected instanceof JScrollPane) {
                    selected = ((JScrollPane) selected).getViewport().getView();
                }

                ((ExistingSelectionProcessor) selected).processExistingSelection(e);
            }
        });
    }

    /**
     * Invoked when currently selected Procedure object is changed.
     */
    public void currentProcedureChanged(ProcedureDisplayEvent e) {
        Application.getInstance().getAction(RemoveProcedureParameterAction.getActionName()).setEnabled(false);
        
        if (e.getProcedure() == null)
            setVisible(false);
        else {
            if (e.isTabReset()) {
                this.setSelectedIndex(0);
            }
            this.setVisible(true);
        }
    }

    public void currentProcedureParameterChanged(ProcedureParameterDisplayEvent e) {
        ProcedureParameter[] parameters = e.getProcedureParameters();
        
        if(parameters.length > 0) {
            procedureParameterPanel.selectParameters(parameters);
        }
    }
}
