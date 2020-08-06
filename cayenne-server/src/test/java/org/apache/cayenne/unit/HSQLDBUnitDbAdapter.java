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

package org.apache.cayenne.unit;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;

import java.sql.Connection;

public class HSQLDBUnitDbAdapter extends UnitDbAdapter {

    public HSQLDBUnitDbAdapter(DbAdapter adapter) {
        super(adapter);
    }

    @Override
    public boolean supportsLobs() {
        return true;
    }

    /**
     * Note that out of all SP tests HSQLDB (as of 8.0.2) supports only updates that do
     * not return a ResultSet (see HSQL CallableStatement JavaDocs). Once HSQL implements
     * the rest of callable statement we can enable our unit test.
     */
    @Override
    public boolean supportsStoredProcedures() {
        return false;
    }

    @Override
    public boolean supportsHaving() {
        return false;
    }

    @Override
    public void createdTables(Connection con, DataMap map) throws Exception {
        if (map.getProcedureMap().containsKey("cayenne_tst_select_proc")) {
            executeDDL(con, "hsqldb", "create-sp-aliases.sql");
        }
    }

    /**
     * In HyperSQL, REAL, FLOAT, DOUBLE are equivalent and all mapped to double in Java
     * @see <a href="http://hsqldb.org/doc/guide/sqlgeneral-chapt.html#sgc_numeric_types">HSQLDB data types documentation</a>
     */
    @Override
    public boolean realAsDouble() {
        return true;
    }

    @Override
    public boolean supportsPKGeneratorConcurrency() {
        // HSQL is not locking AUTO_PK_TABLE, so running PkGenerator in parallel may result in conflicting ranges
        return false;
    }

    @Override
    public boolean supportsGeneratedKeysAdd() {
        return true;
    }

    @Override
    public boolean supportsGeneratedKeysDrop() {
        return true;
    }
}
