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

package org.apache.cayenne.modeler.util.state;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.ProcedureDisplayEvent;
import org.apache.cayenne.modeler.event.ProcedureParameterDisplayEvent;

import java.util.ArrayList;
import java.util.List;

class ProcedureParameterDisplayEventType extends ProcedureDisplayEventType {

    public ProcedureParameterDisplayEventType(ProjectController controller) {
        super(controller);
    }

    @Override
    public void fireLastDisplayEvent() {
        DataChannelDescriptor dataChannel = (DataChannelDescriptor) controller.getProject().getRootNode();
        if (!dataChannel.getName().equals(preferences.getDomain())) {
            return;
        }

        DataMap dataMap = dataChannel.getDataMap(preferences.getDataMap());
        if (dataMap == null) {
            return;
        }

        Procedure procedure = dataMap.getProcedure(preferences.getProcedure());
        if (procedure == null) {
            return;
        }

        ProcedureDisplayEvent procedureDisplayEvent = new ProcedureDisplayEvent(this, procedure, dataMap, dataChannel);
        controller.fireProcedureDisplayEvent(procedureDisplayEvent);

        ProcedureParameter[] procedureParameters = getLastProcedureParameters(procedure);
        ProcedureParameterDisplayEvent procedureParameterDisplayEvent =
                new ProcedureParameterDisplayEvent(this, procedureParameters, procedure, dataMap, dataChannel);
        controller.fireProcedureParameterDisplayEvent(procedureParameterDisplayEvent);
    }

    @Override
    public void saveLastDisplayEvent() {
        preferences.setEvent(ProcedureParameterDisplayEvent.class.getSimpleName());
        preferences.setDomain(controller.getCurrentDataChanel().getName());
        preferences.setDataMap(controller.getCurrentDataMap().getName());
        preferences.setProcedure(controller.getCurrentProcedure().getName());
        preferences.setProcedureParams(parseToString(controller.getCurrentProcedureParameters()));
    }

    protected ProcedureParameter[] getLastProcedureParameters(Procedure procedure) {
        List<ProcedureParameter> procedureParameterList = new ArrayList<ProcedureParameter>();
        ProcedureParameter[] parameters = new ProcedureParameter[0];

        String procedureParams = preferences.getProcedureParams();
        if (procedureParams.isEmpty()) {
            return procedureParameterList.toArray(parameters);
        }

        for (String procedureParamName : procedureParams.split(",")) {
            for (ProcedureParameter procedureParameter : procedure.getCallParameters()) {
                if (procedureParameter.getName().equals(procedureParamName)) {
                    procedureParameterList.add(procedureParameter);
                }
            }
        }

        return procedureParameterList.toArray(parameters);
    }
}