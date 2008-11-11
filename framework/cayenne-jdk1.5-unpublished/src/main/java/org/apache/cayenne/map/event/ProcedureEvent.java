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

import org.apache.cayenne.map.Procedure;

/** 
 * An event generated when a Procedure object is added to a DataMap, 
 * removed from a DataMap, or changed within a DataMap.
 * 
 */
public class ProcedureEvent extends MapEvent {
    protected Procedure procedure;

    public ProcedureEvent(Object source, Procedure procedure) {
        super(source);
        setProcedure(procedure);
    }

    public ProcedureEvent(Object source, Procedure procedure, String oldName) {
        this(source, procedure);
        setOldName(oldName);
    }

    public ProcedureEvent(Object source, Procedure procedure, int type) {
        this(source, procedure);
        setId(type);
    }

    public Procedure getProcedure() {
        return procedure;
    }

    public void setProcedure(Procedure procedure) {
        this.procedure = procedure;
    }

    @Override
    public String getNewName() {
        return (procedure != null) ? procedure.getName() : null;
    }
}
