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

package org.apache.cayenne.modeler.undo;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.modeler.action.CreateProcedureParameterAction;
import org.apache.cayenne.modeler.action.RemoveProcedureParameterAction;
import org.apache.cayenne.modeler.event.ProcedureDisplayEvent;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

public class CreateProcedureParameterUndoableEdit extends CayenneUndoableEdit {

    private DataChannelDescriptor domain;
    private DataMap dataMap;
    private Procedure procedure;
    private ProcedureParameter parameter;

    public CreateProcedureParameterUndoableEdit(
            DataChannelDescriptor dataDomain, DataMap dataMap, Procedure procedure, ProcedureParameter parameter) {

        this.domain = dataDomain;
        this.dataMap = dataMap;
        this.procedure = procedure;
        this.parameter = parameter;
    }

    @Override
    public void undo() throws CannotUndoException {
        RemoveProcedureParameterAction action = actionManager.getAction(RemoveProcedureParameterAction.class);

        if (procedure != null) {
            action.removeProcedureParameters(procedure, new ProcedureParameter[] {
                    parameter
            });

            controller.fireProcedureDisplayEvent(new ProcedureDisplayEvent(this, procedure, dataMap, domain));
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        CreateProcedureParameterAction action = actionManager.getAction(CreateProcedureParameterAction.class);
        if (procedure != null) {
            action.createProcedureParameter(procedure, parameter);
        }
    }

    @Override
    public String getPresentationName() {
        return "Create Procedure Parameter";
    }
}
