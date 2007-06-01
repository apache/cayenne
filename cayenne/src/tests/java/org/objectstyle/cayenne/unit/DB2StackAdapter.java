package org.objectstyle.cayenne.unit;

import java.sql.Connection;
import java.util.Collection;

import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DataMap;

/**
 * @author Andrei Adamchik
 */
public class DB2StackAdapter extends AccessStackAdapter {

    public DB2StackAdapter(DbAdapter adapter) {
        super(adapter);
    }
    
    public void willDropTables(Connection conn, DataMap map, Collection tablesToDrop) throws Exception {
        // avoid dropping constraints...  
    }

    public boolean supportsBinaryPK() {
        return false;
    }

    public boolean supportsLobs() {
        return true;
    }

    public boolean supportsStoredProcedures() {
        return false;
    }

  /*  public void createdTables(Connection con, DataMap map) throws Exception {
        executeDDL(con, super.ddlFile("db2", "create-update-sp.sql"));
        executeDDL(con, super.ddlFile("db2", "create-out-sp.sql"));
        executeDDL(con, super.ddlFile("db2", "create-select-sp.sql"));
    }

    public void willDropTables(Connection con, DataMap map) throws Exception {
        // still have to figure out how to safely drop procedures

        try {
            executeDDL(con, super.ddlFile("db2", "drop-select-sp.sql"));
        }
        catch (SQLException ex) {
            logObj.info("Can't drop procedure, ignoring.", ex);
        }

        try {
            executeDDL(con, super.ddlFile("db2", "drop-update-sp.sql"));
        }
        catch (SQLException ex) {
            logObj.info("Can't drop procedure, ignoring.", ex);
        }

        try {
            executeDDL(con, super.ddlFile("db2", "drop-out-sp.sql"));
        }
        catch (SQLException ex) {
            logObj.info("Can't drop procedure, ignoring.", ex);
        }
    } */
}
