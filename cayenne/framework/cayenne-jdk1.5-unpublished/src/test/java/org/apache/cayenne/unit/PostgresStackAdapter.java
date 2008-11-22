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

package org.apache.cayenne.unit;

import java.sql.Connection;
import java.sql.Types;
import java.util.Collection;

import org.apache.cayenne.access.DataContextProcedureQueryTest;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;

/**
 */
public class PostgresStackAdapter extends AccessStackAdapter {

    public PostgresStackAdapter(DbAdapter adapter) {
        super(adapter);
    }

    @Override
    public void willDropTables(Connection conn, DataMap map, Collection tablesToDrop)
            throws Exception {
        // avoid dropping constraints...
    }

    @Override
    public boolean supportsLobs() {
        return true;
    }

    @Override
    public boolean supportsStoredProcedures() {
        return true;
    }

    @Override
    public boolean canMakeObjectsOutOfProcedures() {
        // we are a victim of CAY-148 - column capitalization...
        return false;
    }

    @Override
    public void createdTables(Connection con, DataMap map) throws Exception {
        if (map.getProcedureMap().containsKey("cayenne_tst_select_proc")) {
            executeDDL(con, "postgresql", "create-select-sp.sql");
            executeDDL(con, "postgresql", "create-update-sp.sql");
            executeDDL(con, "postgresql", "create-update-sp2.sql");
            executeDDL(con, "postgresql", "create-out-sp.sql");
        }
    }

    @Override
    public void tweakProcedure(Procedure proc) {
        if (DataContextProcedureQueryTest.OUT_STORED_PROCEDURE.equals(proc.getName())
                && proc.getCallParameters().size() == 2) {

            proc.clearCallParameters();
            proc.addCallParameter(new ProcedureParameter(
                    "out_param",
                    Types.INTEGER,
                    ProcedureParameter.OUT_PARAMETER));

            proc.addCallParameter(new ProcedureParameter(
                    "in_param",
                    Types.INTEGER,
                    ProcedureParameter.IN_PARAMETER));
            proc.setReturningValue(true);
        }
    }
}
