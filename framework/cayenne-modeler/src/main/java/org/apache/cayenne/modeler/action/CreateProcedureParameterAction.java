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

import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.event.ProcedureParameterEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.ProcedureParameterDisplayEvent;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.project.NamedObjectFactory;
import org.apache.cayenne.project.ProjectPath;

/**
 */
public class CreateProcedureParameterAction extends CayenneAction {

    

    public static String getActionName() {
    	return "Create Parameter";
    }

    /**
     * Constructor for CreateProcedureParameterAction.
     * @param name
     */
    public CreateProcedureParameterAction(Application application) {
        super(getActionName(), application);
    }

    public String getIconName() {
        return "icon-plus.gif";
    }

    /**
     * Creates ProcedureParameter depending on context.
     */
    public void performAction(ActionEvent e) {
        if (getProjectController().getCurrentProcedure() != null) {
            createProcedureParameter();
        }
    }

    public void createProcedureParameter() {
        Procedure procedure = getProjectController().getCurrentProcedure();

        ProcedureParameter parameter =
            (ProcedureParameter) NamedObjectFactory.createObject(
                ProcedureParameter.class,
                procedure);
                
        procedure.addCallParameter(parameter);

        ProjectController mediator = getProjectController();
        fireProcedureParameterEvent(this, mediator, procedure, parameter);
    }
    
    /**
     * Fires events when an proc parameter was added
     */
    static void fireProcedureParameterEvent(Object src, ProjectController mediator, Procedure procedure, 
            ProcedureParameter parameter) {
        mediator.fireProcedureParameterEvent(
                new ProcedureParameterEvent(src, parameter, MapEvent.ADD));

            mediator.fireProcedureParameterDisplayEvent(
                new ProcedureParameterDisplayEvent(
                    src,
                    parameter,
                    procedure,
                    mediator.getCurrentDataMap(),
                    mediator.getCurrentDataDomain()));
    }

    /**
     * Returns <code>true</code> if path contains a Procedure object.
     */
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        return path.firstInstanceOf(Procedure.class) != null;
    }
}
