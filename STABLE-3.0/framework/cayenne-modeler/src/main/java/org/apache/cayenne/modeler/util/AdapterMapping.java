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

package org.apache.cayenne.modeler.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.dba.db2.DB2Adapter;
import org.apache.cayenne.dba.derby.DerbyAdapter;
import org.apache.cayenne.dba.frontbase.FrontBaseAdapter;
import org.apache.cayenne.dba.h2.H2Adapter;
import org.apache.cayenne.dba.hsqldb.HSQLDBAdapter;
import org.apache.cayenne.dba.ingres.IngresAdapter;
import org.apache.cayenne.dba.mysql.MySQLAdapter;
import org.apache.cayenne.dba.openbase.OpenBaseAdapter;
import org.apache.cayenne.dba.oracle.OracleAdapter;
import org.apache.cayenne.dba.postgres.PostgresAdapter;
import org.apache.cayenne.dba.sqlite.SQLiteAdapter;
import org.apache.cayenne.dba.sqlserver.SQLServerAdapter;
import org.apache.cayenne.dba.sybase.SybaseAdapter;

/**
 * Contains mappings for guessing defaults for various adapter and JDBC settings.
 * 
 */
public class AdapterMapping {

    protected Map<String, String> adapterToJDBCDriverMap;
    protected Map<String, String> adapterToJDBCURLMap;
    protected Map<String, String> jdbcDriverToAdapterMap;
    protected Map<String, String> eofPluginToAdapterMap;

    public AdapterMapping() {
        this.adapterToJDBCDriverMap = new HashMap<String, String>();
        this.adapterToJDBCURLMap = new HashMap<String, String>();
        this.jdbcDriverToAdapterMap = new HashMap<String, String>();
        this.eofPluginToAdapterMap = new HashMap<String, String>();

        initDefaults();
    }

    protected void initDefaults() {
        // TODO: make configuration external...

        // drivers
        jdbcDriverToAdapterMap.put("oracle.jdbc.driver.OracleDriver", OracleAdapter.class
                .getName());
        jdbcDriverToAdapterMap.put("com.sybase.jdbc2.jdbc.SybDriver", SybaseAdapter.class
                .getName());
        jdbcDriverToAdapterMap.put("com.mysql.jdbc.Driver", MySQLAdapter.class.getName());
        jdbcDriverToAdapterMap.put("com.ibm.db2.jcc.DB2Driver", DB2Adapter.class
                .getName());
        jdbcDriverToAdapterMap
                .put("org.hsqldb.jdbcDriver", HSQLDBAdapter.class.getName());
        jdbcDriverToAdapterMap.put("org.h2.Driver", H2Adapter.class.getName());
        jdbcDriverToAdapterMap.put("org.postgresql.Driver", PostgresAdapter.class
                .getName());
        jdbcDriverToAdapterMap.put("com.openbase.jdbc.ObDriver", OpenBaseAdapter.class
                .getName());
        jdbcDriverToAdapterMap.put(
                "com.microsoft.sqlserver.jdbc.SQLServerDriver",
                SQLServerAdapter.class.getName());
        jdbcDriverToAdapterMap.put(
                "org.apache.derby.jdbc.EmbeddedDriver",
                DerbyAdapter.class.getName());
        jdbcDriverToAdapterMap.put("jdbc.FrontBase.FBJDriver", FrontBaseAdapter.class
                .getName());
        jdbcDriverToAdapterMap.put("com.ingres.jdbc.IngresDriver", IngresAdapter.class
                .getName());
        jdbcDriverToAdapterMap.put("org.sqlite.JDBC", SQLiteAdapter.class.getName());

        // urls
        adapterToJDBCURLMap.put(
                OracleAdapter.class.getName(),
                "jdbc:oracle:thin:@//localhost:1521/database");
        adapterToJDBCURLMap.put(
                SybaseAdapter.class.getName(),
                "jdbc:sybase:Tds:localhost:port/database");
        adapterToJDBCURLMap.put(
                MySQLAdapter.class.getName(),
                "jdbc:mysql://localhost/database");
        adapterToJDBCURLMap.put(
                DB2Adapter.class.getName(),
                "jdbc:db2://localhost:port/database");
        adapterToJDBCURLMap.put(
                HSQLDBAdapter.class.getName(),
                "jdbc:hsqldb:hsql://localhost/database");
        adapterToJDBCURLMap.put(
                H2Adapter.class.getName(),
                "jdbc:h2:mem:database;MVCC=TRUE");
        adapterToJDBCURLMap.put(
                PostgresAdapter.class.getName(),
                "jdbc:postgresql://localhost:5432/database");
        adapterToJDBCURLMap.put(
                OpenBaseAdapter.class.getName(),
                "jdbc:openbase://localhost/database");
        adapterToJDBCURLMap
                .put(
                        SQLServerAdapter.class.getName(),
                        "jdbc:microsoft:sqlserver://host;databaseName=database;SelectMethod=cursor");
        adapterToJDBCURLMap.put(SQLiteAdapter.class.getName(), "jdbc:sqlite:testdb");

        // TODO: embedded Derby Mode... change to client-server once we figure it out
        adapterToJDBCURLMap.put(
                DerbyAdapter.class.getName(),
                "jdbc:derby:testdb;create=true");

        adapterToJDBCURLMap.put(
                FrontBaseAdapter.class.getName(),
                "jdbc:FrontBase://localhost/database");
        adapterToJDBCURLMap.put(
                IngresAdapter.class.getName(),
                "jdbc:ingres://127.0.0.1:II7/database");

        // adapters
        adapterToJDBCDriverMap.put(
                OracleAdapter.class.getName(),
                "oracle.jdbc.driver.OracleDriver");
        adapterToJDBCDriverMap.put(
                SybaseAdapter.class.getName(),
                "com.sybase.jdbc2.jdbc.SybDriver");
        adapterToJDBCDriverMap.put(MySQLAdapter.class.getName(), "com.mysql.jdbc.Driver");
        adapterToJDBCDriverMap.put(
                DB2Adapter.class.getName(),
                "com.ibm.db2.jcc.DB2Driver");
        adapterToJDBCDriverMap
                .put(HSQLDBAdapter.class.getName(), "org.hsqldb.jdbcDriver");
        adapterToJDBCDriverMap.put(H2Adapter.class.getName(), "org.h2.Driver");
        adapterToJDBCDriverMap.put(
                PostgresAdapter.class.getName(),
                "org.postgresql.Driver");
        adapterToJDBCDriverMap.put(
                OpenBaseAdapter.class.getName(),
                "com.openbase.jdbc.ObDriver");
        adapterToJDBCDriverMap.put(
                SQLServerAdapter.class.getName(),
                "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        adapterToJDBCDriverMap.put(
                DerbyAdapter.class.getName(),
                "org.apache.derby.jdbc.EmbeddedDriver");

        adapterToJDBCDriverMap.put(
                FrontBaseAdapter.class.getName(),
                "jdbc.FrontBase.FBJDriver");

        adapterToJDBCDriverMap.put(
                IngresAdapter.class.getName(),
                "com.ingres.jdbc.IngresDriver");
        adapterToJDBCDriverMap.put(SQLiteAdapter.class.getName(), "org.sqlite.JDBC");

        // EOF plugins...
        eofPluginToAdapterMap.put(
                "com.webobjects.jdbcadaptor.SybasePlugIn",
                SybaseAdapter.class.getName());
        eofPluginToAdapterMap.put(
                "com.webobjects.jdbcadaptor.MerantPlugIn",
                SQLServerAdapter.class.getName());
        eofPluginToAdapterMap.put(
                "com.webobjects.jdbcadaptor.MicrosoftPlugIn",
                SQLServerAdapter.class.getName());
        eofPluginToAdapterMap.put(
                "com.webobjects.jdbcadaptor.MySQLPlugIn",
                MySQLAdapter.class.getName());
        eofPluginToAdapterMap.put(
                "com.webobjects.jdbcadaptor.OraclePlugIn",
                OracleAdapter.class.getName());
        eofPluginToAdapterMap.put(
                "com.webobjects.jdbcadaptor.FrontbasePlugIn",
                FrontBaseAdapter.class.getName());
        eofPluginToAdapterMap.put("PostgresqlPlugIn", PostgresAdapter.class.getName());
    }

    public String jdbcURLForAdapter(String adapterClass) {
        return adapterToJDBCURLMap.get(adapterClass);
    }

    public String jdbcDriverForAdapter(String adapterClass) {
        return adapterToJDBCDriverMap.get(adapterClass);
    }

    public String adapterForJDBCDriver(String driverClass) {
        return jdbcDriverToAdapterMap.get(driverClass);
    }

    public String adapterForEOFPlugin(String eofPlugin) {
        return eofPluginToAdapterMap.get(eofPlugin);
    }

    public String adapterForEOFPluginOrDriver(String eofPlugin, String jdbcDriver) {
        String adapter = adapterForEOFPlugin(eofPlugin);
        return (adapter != null) ? adapter : adapterForJDBCDriver(jdbcDriver);
    }
}
