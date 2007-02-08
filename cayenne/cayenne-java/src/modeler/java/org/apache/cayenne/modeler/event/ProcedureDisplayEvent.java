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

package org.apache.cayenne.modeler.event;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Procedure;

/**
 * Display event for Stored Procedures.
 * 
 * @author Andrei Adamchik
 */
public class ProcedureDisplayEvent extends DataMapDisplayEvent {
	protected Procedure procedure;
	protected boolean procedureChanged = true;
	protected boolean tabReset;

    /**
     * Creates a new ProcedureDisplayEvent
     */
    public ProcedureDisplayEvent(Object src, Procedure procedure, DataMap map, DataDomain domain) {
        super(src, map, domain);
        this.procedure = procedure;
    }


    public Procedure getProcedure() {
        return procedure;
    }

    public void setProcedure(Procedure procedure) {
        this.procedure = procedure;
    }
    
    public boolean isProcedureChanged() {
        return procedureChanged;
    }

    public void setProcedureChanged(boolean b) {
        procedureChanged = b;
    }
    
    public boolean isTabReset() {
        return tabReset;
    }

    public void setTabReset(boolean b) {
        tabReset = b;
    }
}
