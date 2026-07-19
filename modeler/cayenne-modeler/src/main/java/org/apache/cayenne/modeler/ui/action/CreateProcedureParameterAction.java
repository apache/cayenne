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

package org.apache.cayenne.modeler.ui.action;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.modeler.event.model.ProcedureParameterEvent;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.event.display.ProcedureParameterDisplayEvent;
import org.apache.cayenne.modeler.toolkit.AppAction;
import org.apache.cayenne.modeler.undo.CreateProcedureParameterUndoableEdit;

import java.awt.event.ActionEvent;

public class CreateProcedureParameterAction extends AppAction {

    public CreateProcedureParameterAction(Application application) {
        super(application, "Create Parameter");
    }

    /**
     * Fires events when an proc parameter was added
     */
    static void fireProcedureParameterEvent(
            Object src,
            ProjectSession session,
            Procedure procedure,
            ProcedureParameter parameter) {
        session.fireProcedureParameterEvent(ProcedureParameterEvent.ofAdd(src, parameter));

        session.displayProcedureParameter(new ProcedureParameterDisplayEvent(src,
                (DataChannelDescriptor) session.project().getRootNode(),
                session.getSelectedDataMap(),
                procedure,
                parameter));
    }

    @Override
    public String getIconName() {
        return "icon-plus.png";
    }

    /**
     * Creates ProcedureParameter depending on context.
     */
    @Override
    public void performAction(ActionEvent e) {
        ProjectSession session = getProjectSession();

        if (getProjectSession().getSelectedProcedure() != null) {
            Procedure procedure = getProjectSession().getSelectedProcedure();
            ProcedureParameter parameter = new ProcedureParameter();
            parameter.setName(NameBuilder.of(parameter).parent(procedure).name());

            createProcedureParameter(procedure, parameter);

            app.getUndoManager().addEdit(
                    new CreateProcedureParameterUndoableEdit(session,
                            (DataChannelDescriptor) session.project().getRootNode(), session.getSelectedDataMap(),
                            procedure, parameter
                    )
            );
        }
    }

    public void createProcedureParameter(Procedure procedure, ProcedureParameter parameter) {
        procedure.addCallParameter(parameter);
        fireProcedureParameterEvent(this, getProjectSession(), procedure, parameter);
    }

    /**
     * Returns <code>true</code> if path contains a Procedure object.
     */
    @Override
    public boolean enableForPath(ConfigurationNode object) {
        if (object == null) {
            return false;
        }

        return ((ProcedureParameter) object).getProcedure() != null;
    }
}
