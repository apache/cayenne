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

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.event.ProcedureParameterEvent;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.ProcedureParameterDisplayEvent;
import org.apache.cayenne.modeler.util.CayenneAction;

import java.awt.event.ActionEvent;

public class CreateProcedureParameterAction extends CayenneAction {

    /**
     * Constructor for CreateProcedureParameterAction.
     */
    public CreateProcedureParameterAction(Application application) {
        super(getActionName(), application);
    }

    public static String getActionName() {
        return "Create Parameter";
    }

    /**
     * Fires events when an proc parameter was added
     */
    static void fireProcedureParameterEvent(Object src, ProjectController mediator, Procedure procedure,
                                            ProcedureParameter parameter) {
        mediator.fireProcedureParameterEvent(new ProcedureParameterEvent(src, parameter, MapEvent.ADD));

        mediator.fireProcedureParameterDisplayEvent(new ProcedureParameterDisplayEvent(src, parameter, procedure,
                mediator.getCurrentDataMap(), (DataChannelDescriptor) mediator.getProject().getRootNode()));
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
        if (getProjectController().getCurrentProcedure() != null) {
            createProcedureParameter();
        }
    }

    public void createProcedureParameter() {
        Procedure procedure = getProjectController().getCurrentProcedure();

        ProcedureParameter parameter = new ProcedureParameter();
        parameter.setName(NameBuilder.builder(parameter, procedure).name());
        procedure.addCallParameter(parameter);

        ProjectController mediator = getProjectController();
        fireProcedureParameterEvent(this, mediator, procedure, parameter);
    }

    /**
     * Returns <code>true</code> if path contains a Procedure object.
     */
    public boolean enableForPath(ConfigurationNode object) {
        if (object == null) {
            return false;
        }

        return ((ProcedureParameter) object).getProcedure() != null;
    }
}
