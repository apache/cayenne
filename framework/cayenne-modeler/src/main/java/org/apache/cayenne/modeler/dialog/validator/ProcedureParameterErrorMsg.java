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

package org.apache.cayenne.modeler.dialog.validator;

import javax.swing.JFrame;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.ProcedureDisplayEvent;
import org.apache.cayenne.modeler.event.ProcedureParameterDisplayEvent;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.project.validator.ValidationInfo;

/**
 */
public class ProcedureParameterErrorMsg extends ValidationDisplayHandler {

    public ProcedureParameterErrorMsg(ValidationInfo validationInfo) {
        super(validationInfo);
    }

    public void displayField(ProjectController mediator, JFrame frame) {
        ProjectPath path = super.validationInfo.getPath();
        DataDomain domain = path.firstInstanceOf(DataDomain.class);
        DataMap map = path.firstInstanceOf(DataMap.class);
        Procedure procedure = path.firstInstanceOf(Procedure.class);
        ProcedureParameter procedureParameter = path
                .firstInstanceOf(ProcedureParameter.class);

        // Race condition between the two events...?

        // first display the stored procedure
        // for whatever reason, other validators do not require this step
        // (E.g. DbAttributeErrorMsg)
        ProcedureDisplayEvent procedureEvent = new ProcedureDisplayEvent(
                frame,
                procedure,
                map,
                domain);
        procedureEvent.setTabReset(true);
        mediator.fireProcedureDisplayEvent(procedureEvent);

        // now show the failed parameter
        ProcedureParameterDisplayEvent event = new ProcedureParameterDisplayEvent(
                frame,
                procedureParameter,
                procedure,
                map,
                domain);

        event.setTabReset(true);
        mediator.fireProcedureParameterDisplayEvent(event);
    }

}
