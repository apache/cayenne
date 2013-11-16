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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.access.DataContextProcedureQueryTest;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.oracle.OracleAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;

/**
 */
public class OracleUnitDbAdapter extends UnitDbAdapter {

    /**
     * Constructor for OracleDelegate.
     * 
     * @param adapter
     */
    public OracleUnitDbAdapter(DbAdapter adapter) {
        super(adapter);
    }

    @Override
    public boolean supportsStoredProcedures() {
        return true;
    }

    @Override
    public boolean supportsBoolean() {
        return false;
    }

    @Override
    public void willDropTables(Connection conn, DataMap map, Collection tablesToDrop) throws Exception {
        // avoid dropping constraints...
    }

    /**
     * Oracle 8i does not support more then 1 "LONG xx" column per table
     * PAINTING_INFO need to be fixed.
     */
    @Override
    public void willCreateTables(Connection con, DataMap map) {
        DbEntity paintingInfo = map.getDbEntity("PAINTING_INFO");

        if (paintingInfo != null) {
            DbAttribute textReview = paintingInfo.getAttribute("TEXT_REVIEW");
            textReview.setType(Types.VARCHAR);
            textReview.setMaxLength(255);
        }
    }

    @Override
    public void createdTables(Connection con, DataMap map) throws Exception {
        if (map.getProcedureMap().containsKey("cayenne_tst_select_proc")) {
            executeDDL(con, "oracle", "create-types-pkg.sql");
            executeDDL(con, "oracle", "create-select-sp.sql");
            executeDDL(con, "oracle", "create-update-sp.sql");
            executeDDL(con, "oracle", "create-update-sp2.sql");
            executeDDL(con, "oracle", "create-out-sp.sql");
        }
    }

    @Override
    public boolean supportsLobs() {
        return true;
    }

    @Override
    public boolean supportsLobComparisons() {
        // we can actually allow LOB comparisons with some Oracle trickery.
        // E.g.:
        // DBMS_LOB.SUBSTR(CLOB_COLUMN, LENGTH('string') + 1, 1) = 'string'
        return false;
    }

    @Override
    public void tweakProcedure(Procedure proc) {
        if (DataContextProcedureQueryTest.SELECT_STORED_PROCEDURE.equals(proc.getName())
                && proc.getCallParameters().size() == 2) {
            List params = new ArrayList(proc.getCallParameters());

            proc.clearCallParameters();
            proc.addCallParameter(new ProcedureParameter("result", OracleAdapter.getOracleCursorType(),
                    ProcedureParameter.OUT_PARAMETER));
            Iterator it = params.iterator();
            while (it.hasNext()) {
                ProcedureParameter param = (ProcedureParameter) it.next();
                proc.addCallParameter(param);
            }

            proc.setReturningValue(true);
        }
    }
}
