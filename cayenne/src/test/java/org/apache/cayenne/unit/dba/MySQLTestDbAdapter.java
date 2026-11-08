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

import org.apache.cayenne.map.compiled.CompiledDataMap;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;

public class MySQLTestDbAdapter extends TestDbAdapter {

    static final Collection<String> NO_CONSTRAINTS_TABLES = Arrays.asList(
            "REFLEXIVE_AND_TO_ONE",
            "ARTGROUP",
            "FK_OF_DIFFERENT_TYPE");

    public MySQLTestDbAdapter(DbAdapter adapter) {
        super(adapter);
    }
    
    @Override
    public String getIdentifiersStartQuote() {
        return "`";
    }
    
    @Override
    public String getIdentifiersEndQuote() {
        return "`";
    }
    
    @Override
    public boolean supportsCatalogs() {
        return true;
    }
    
    @Override
    public boolean realAsDouble() {
        // this actually depends on the "sql_mode" var in MYSQL. However the
        // default is REAL == DOUBLE
        return true;
    }

    @Override
    public boolean supportsLobs() {
        return true;
    }
    
    @Override
    public boolean supportsBitwiseOps() {
        return true;
    }

    @Override
    public boolean supportsStoredProcedures() {
        return true;
    }

    @Override
    public boolean supportsTrimChar() {
        return true;
    }

    @Override
    public void createdTables(Connection con, DataMap map) throws Exception {

        if (map.getProcedureMap().containsKey("cayenne_tst_select_proc")) {
            executeDDL(con, "mysql", "create-select-sp.sql");
            executeDDL(con, "mysql", "create-update-sp.sql");
            executeDDL(con, "mysql", "create-update-sp2.sql");
            executeDDL(con, "mysql", "create-out-sp.sql");
        }
    }

    @Override
    public void willDropTables(
            Connection conn,
            CompiledMapping mapping,
            CompiledDataMap map,
            Collection<String> tablesToDrop) throws Exception {

        if (hasProcedure(mapping, map, "cayenne_tst_select_proc")) {
            executeDDL(conn, "mysql", "drop-select-sp.sql");
            executeDDL(conn, "mysql", "drop-update-sp.sql");
            executeDDL(conn, "mysql", "drop-update-sp2.sql");
            executeDDL(conn, "mysql", "drop-out-sp.sql");
        }
    }

    @Override
    public boolean supportsFKConstraints(CompiledTable table) {
        // MySQL supports that, but there are problems deleting objects from such
        // tables...
        return !NO_CONSTRAINTS_TABLES.contains(table.name());
    }

    @Override
    public boolean supportsGeneratedKeysAdd() {
        return true;
    }

    @Override
    public boolean supportsGeneratedKeysDrop() {
        return true;
    }

    @Override
    public boolean supportScalarAsExpression() {
        return true;
    }
}
