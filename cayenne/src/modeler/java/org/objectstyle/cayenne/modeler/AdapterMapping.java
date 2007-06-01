package org.objectstyle.cayenne.modeler;

import java.util.HashMap;
import java.util.Map;

import org.objectstyle.cayenne.dba.db2.DB2Adapter;
import org.objectstyle.cayenne.dba.firebird.FirebirdAdapter;
import org.objectstyle.cayenne.dba.hsqldb.HSQLDBAdapter;
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

        // urls
        adapterToJDBCURLMap.put(
                OracleAdapter.class.getName(),
                "jdbc:oracle:thin:@host:1521:database");
        adapterToJDBCURLMap.put(
                SybaseAdapter.class.getName(),
                "jdbc:sybase:Tds:host:port/database");
        adapterToJDBCURLMap.put(
                MySQLAdapter.class.getName(),
                "jdbc:mysql://host/database");
        adapterToJDBCURLMap.put(
                DB2Adapter.class.getName(),
                "jdbc:db2://host:port/database");
        adapterToJDBCURLMap.put(
                HSQLDBAdapter.class.getName(),
                "jdbc:hsqldb:hsql://host/database");
        adapterToJDBCURLMap.put(
                PostgresAdapter.class.getName(),
                "jdbc:postgresql://host:5432/database");
        adapterToJDBCURLMap.put(
                FirebirdAdapter.class.getName(),
                "jdbc:firebirdsql:host/port:/path/to/file.gdb");
        adapterToJDBCURLMap.put(
                OpenBaseAdapter.class.getName(),
                "jdbc:openbase://host/database");
        adapterToJDBCURLMap
                .put(
                        SQLServerAdapter.class.getName(),
                        "jdbc:microsoft:sqlserver://host;databaseName=database;SelectMethod=cursor");

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