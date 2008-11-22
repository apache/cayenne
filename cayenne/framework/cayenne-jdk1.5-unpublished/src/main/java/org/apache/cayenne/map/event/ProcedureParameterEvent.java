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

package org.apache.cayenne.map.event;

import org.apache.cayenne.map.ProcedureParameter;

/**
 */
public class ProcedureParameterEvent extends MapEvent {
    protected ProcedureParameter parameter;

    public ProcedureParameterEvent(Object source, ProcedureParameter parameter) {
        super(source);
        setParameter(parameter);
    }

    public ProcedureParameterEvent(
        Object source,
        ProcedureParameter parameter,
        String oldName) {
        this(source, parameter);
        setOldName(oldName);
    }

    public ProcedureParameterEvent(
        Object source,
        ProcedureParameter parameter,
        int type) {
        this(source, parameter);
        setId(type);
    }

    @Override
    public String getNewName() {
        return (parameter != null) ? parameter.getName() : null;
    }

    public ProcedureParameter getParameter() {
        return parameter;
    }

    public void setParameter(ProcedureParameter parameter) {
        this.parameter = parameter;
    }
}
