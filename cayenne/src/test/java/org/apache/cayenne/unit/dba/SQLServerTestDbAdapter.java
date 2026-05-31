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

package org.apache.cayenne.unit.dba;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Procedure;

import java.sql.Connection;
import java.util.Collection;

public class SQLServerTestDbAdapter extends TestDbAdapter {

    public SQLServerTestDbAdapter(DbAdapter adapter) {
        super(adapter);
    }

    @Override
    public String getIdentifiersStartQuote() {
        return "[";
    }

    @Override
    public String getIdentifiersEndQuote() {
        return "]";
    }

    @Override
    public boolean supportsStoredProcedures() {
        return true;
    }

    @Override
    public void createdTables(Connection con, DataMap map) throws Exception {
        Procedure proc = map.getProcedure("cayenne_tst_select_proc");
        if (proc != null && proc.getDataMap() == map) {
            executeDDL(con, "sqlserver", "create-select-sp.sql");
            executeDDL(con, "sqlserver", "create-update-sp.sql");
            executeDDL(con, "sqlserver", "create-update-sp2.sql");
            executeDDL(con, "sqlserver", "create-out-sp.sql");
        }
    }

    @Override
    public boolean supportsLobs() {
        return true;
    }

    @Override
    public boolean handlesNullVsEmptyLOBs() {
        return true;
    }

    @Override
    public void willCreateTables(Connection con, DataMap map) {
    }

    @Override
    public void willDropTables(Connection conn, DataMap map, Collection tablesToDrop) throws Exception {
        dropConstraints(conn, map, tablesToDrop);
        dropProcedures(conn, map);
    }

    protected void dropProcedures(Connection con, DataMap map) throws Exception {
        Procedure proc = map.getProcedure("cayenne_tst_select_proc");
        if (proc != null && proc.getDataMap() == map) {
            executeDDL(con, "sqlserver", "drop-select-sp.sql");
            executeDDL(con, "sqlserver", "drop-update-sp.sql");
            executeDDL(con, "sqlserver", "drop-update-sp2.sql");
            executeDDL(con, "sqlserver", "drop-out-sp.sql");
        }
    }

    @Override
    public boolean supportsLobComparisons() {
        // people are suggesting using LIKE to compare TEXT columns... not sure
        // what the right solution might be, but for now we are getting
        // "The data types varchar(max) and text are incompatible in the equal to operator. in SQL Server2005 how to solve?"
        // http://stackoverflow.com/questions/20180766/the-data-types-varcharmax-and-text-are-incompatible-in-the-equal-to-operator
        return false;
    }

    @Override
    public boolean supportsNullBoolean() {
        return true;
    }

    @Override
    public boolean onlyGenericDateType() {
        return true;
    }

    @Override
    public boolean supportsExpressionInHaving() {
        return false;
    }

    @Override
    public boolean supportsSelectBooleanExpression() {
        return false;
    }

    @Override
    public boolean supportsCatalogs() {
        return true;
    }
}
