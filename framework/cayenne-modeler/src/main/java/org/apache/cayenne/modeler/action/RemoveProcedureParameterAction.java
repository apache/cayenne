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


package org.apache.cayenne.modeler.action;

import java.awt.event.ActionEvent;

import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.event.ProcedureParameterEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ConfirmDeleteDialog;
import org.apache.cayenne.project.ProjectPath;

/**
 * Removes currently selected parameter from the current procedure.
 * 
 * @author Garry Watkins
 */
public class RemoveProcedureParameterAction extends RemoveAction {

    private final static String ACTION_NAME = "Remove Parameter";

    public static String getActionName() {
        return ACTION_NAME;
    }

    public RemoveProcedureParameterAction(Application application) {
        super(ACTION_NAME, application);
    }

    /**
     * Returns <code>true</code> if last object in the path contains a removable
     * parameter.
     */
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        return path.getObject() instanceof ProcedureParameter;
    }

    public void performAction(ActionEvent e) {
        ConfirmDeleteDialog dialog = getConfirmDeleteDialog();

        if (getProjectController().getCurrentProcedureParameter() != null) {
            if (dialog.shouldDelete("procedure parameter", getProjectController()
                    .getCurrentProcedureParameter().getName())) {
                removeProcedureParameter();
            }
        }
    }

    protected void removeProcedureParameter() {
        ProjectController mediator = getProjectController();
        ProcedureParameter parameter = mediator.getCurrentProcedureParameter();
        mediator.getCurrentProcedure().removeCallParameter(parameter.getName());

        ProcedureParameterEvent e = new ProcedureParameterEvent(
                Application.getFrame(),
                parameter,
                MapEvent.REMOVE);

        mediator.fireProcedureParameterEvent(e);
    }
}
