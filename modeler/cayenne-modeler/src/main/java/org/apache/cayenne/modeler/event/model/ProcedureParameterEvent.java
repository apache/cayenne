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

package org.apache.cayenne.modeler.event.model;

import org.apache.cayenne.map.ProcedureParameter;

public class ProcedureParameterEvent extends ModelEvent {

    private final ProcedureParameter parameter;

    public static ProcedureParameterEvent ofAdd(Object source, ProcedureParameter parameter) {
        return new ProcedureParameterEvent(source, parameter, Type.ADD, null);
    }

    public static ProcedureParameterEvent ofChange(Object source, ProcedureParameter parameter) {
        return new ProcedureParameterEvent(source, parameter, Type.CHANGE, null);
    }

    public static ProcedureParameterEvent ofChange(Object source, ProcedureParameter parameter, String oldName) {
        return new ProcedureParameterEvent(source, parameter, Type.CHANGE, oldName);
    }

    public static ProcedureParameterEvent ofRemove(Object source, ProcedureParameter parameter) {
        return new ProcedureParameterEvent(source, parameter, Type.REMOVE, null);
    }

    private ProcedureParameterEvent(Object source, ProcedureParameter parameter, Type type, String oldName) {
        super(source, type, oldName);
        this.parameter = parameter;
    }

    public ProcedureParameter getParameter() {
        return parameter;
    }

    @Override
    public String getNewName() {
        return (parameter != null) ? parameter.getName() : null;
    }
}
