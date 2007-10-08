package org.objectstyle.cayenne.modeler.util;

import java.util.HashMap;
import java.util.Map;

import org.objectstyle.cayenne.dba.db2.DB2Adapter;
import org.objectstyle.cayenne.dba.derby.DerbyAdapter;
import org.objectstyle.cayenne.dba.firebird.FirebirdAdapter;
import org.objectstyle.cayenne.dba.frontbase.FrontBaseAdapter;
import org.objectstyle.cayenne.dba.hsqldb.HSQLDBAdapter;
import org.objectstyle.cayenne.dba.ingres.IngresAdapter;
import org.objectstyle.cayenne.dba.mysql.MySQLAdapter;
import org.objectstyle.cayenne.dba.openbase.OpenBaseAdapter;
import org.objectstyle.cayenne.dba.oracle.OracleAdapter;
import org.objectstyle.cayenne.dba.postgres.PostgresAdapter;
import org.objectstyle.cayenne.dba.sqlserver.SQLServerAdapter;
import org.objectstyle.cayenne.dba.sybase.SybaseAdapter;

/**
 * Contains mappings for guessing defaults for various adapter and JDBC settings.
 * 
 * @author Andrei Adamchik
 */
public class AdapterMapping {

    protected Map adapterToJDBCDriverMap;
    protected Map adapterToJDBCURLMap;
    protected Map jdbcDriverToAdapterMap;
    protected Map eofPluginToAdapterMap;

    public AdapterMapping() {
        this.adapterToJDBCDriverMap = new HashMap();
        this.adapterToJDBCURLMap = new HashMap();
        this.jdbcDriverToAdapterMap = new HashMap();
        this.eofPluginToAdapterMap = new HashMap();

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
        jdbcDriverToAdapterMap.put("org.postgresql.Driver", PostgresAdapter.class
                .getName());
        jdbcDriverToAdapterMap.put("org.firebirdsql.jdbc.FBDriver", FirebirdAdapter.class
                .getName());
        jdbcDriverToAdapterMap.put("com.openbase.jdbc.ObDriver", OpenBaseAdapter.class
                .getName());
        jdbcDriverToAdapterMap.put(
                "com.microsoft.jdbc.sqlserver.SQLServerDriver",
                SQLServerAdapter.class.getName());
        jdbcDriverToAdapterMap.put(
                "org.apache.derby.jdbc.EmbeddedDriver",
                DerbyAdapter.class.getName());
        jdbcDriverToAdapterMap.put("jdbc.FrontBase.FBJDriver", FrontBaseAdapter.class
                .getName());
        jdbcDriverToAdapterMap.put("com.ingres.jdbc.IngresDriver", IngresAdapter.class
                .getName());

        // urls
        adapterToJDBCURLMap.put(
                OracleAdapter.class.getName(),
                "jdbc:oracle:thin:@localhost:1521:database");
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
                PostgresAdapter.class.getName(),
                "jdbc:postgresql://localhost:5432/database");
        adapterToJDBCURLMap.put(
                FirebirdAdapter.class.getName(),
                "jdbc:firebirdsql:localhost/port:/path/to/file.gdb");
        adapterToJDBCURLMap.put(
                OpenBaseAdapter.class.getName(),
                "jdbc:openbase://localhost/database");
        adapterToJDBCURLMap
                .put(
                        SQLServerAdapter.class.getName(),
                        "jdbc:microsoft:sqlserver://host;databaseName=database;SelectMethod=cursor");

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
        adapterToJDBCDriverMap.put(
                PostgresAdapter.class.getName(),
                "org.postgresql.Driver");
        adapterToJDBCDriverMap.put(
                FirebirdAdapter.class.getName(),
                "org.firebirdsql.jdbc.FBDriver");
        adapterToJDBCDriverMap.put(
                OpenBaseAdapter.class.getName(),
                "com.openbase.jdbc.ObDriver");
        adapterToJDBCDriverMap.put(
                SQLServerAdapter.class.getName(),
                "com.microsoft.jdbc.sqlserver.SQLServerDriver");
        adapterToJDBCDriverMap.put(
                DerbyAdapter.class.getName(),
                "org.apache.derby.jdbc.EmbeddedDriver");

        adapterToJDBCDriverMap.put(
                FrontBaseAdapter.class.getName(),
                "jdbc.FrontBase.FBJDriver");

        adapterToJDBCDriverMap.put(
                IngresAdapter.class.getName(),
                "com.ingres.jdbc.IngresDriver");

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
        return (String) adapterToJDBCURLMap.get(adapterClass);
    }

    public String jdbcDriverForAdapter(String adapterClass) {
        return (String) adapterToJDBCDriverMap.get(adapterClass);
    }

    public String adapterForJDBCDriver(String driverClass) {
        return (String) jdbcDriverToAdapterMap.get(driverClass);
    }

    public String adapterForEOFPlugin(String eofPlugin) {
        return (String) eofPluginToAdapterMap.get(eofPlugin);
    }

    public String adapterForEOFPluginOrDriver(String eofPlugin, String jdbcDriver) {
        String adapter = adapterForEOFPlugin(eofPlugin);
        return (adapter != null) ? adapter : adapterForJDBCDriver(jdbcDriver);
    }
}