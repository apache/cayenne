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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Defines API and a common superclass for testing various database features. Different
 * databases support different feature sets that need to be tested differently. Many
 * things implemented in subclasses may become future candidates for inclusion in the
 * corresponding adapter code.
 * 
 * @author Andrus Adamchik
 */
public class AccessStackAdapter {

    private static Log logObj = LogFactory.getLog(AccessStackAdapter.class);

    protected DbAdapter adapter;

    public AccessStackAdapter(DbAdapter adapter) {
        if (adapter == null) {
            throw new CayenneRuntimeException("Null adapter.");
        }
        this.adapter = adapter;
    }
    
    public boolean usePooledDataSource() {
        return true;
    }

    public DbAdapter getAdapter() {
        return adapter;
    }

    public void unchecked(CayenneResources resources) {

    }

    /**
     * Drops all table constraints.
     */
    public void willDropTables(Connection conn, DataMap map, Collection tablesToDrop)
            throws Exception {
        dropConstraints(conn, map, tablesToDrop);
    }

    protected void dropConstraints(Connection conn, DataMap map, Collection tablesToDrop)
            throws Exception {
        Map constraintsMap = getConstraints(conn, map, tablesToDrop);

        Iterator it = constraintsMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();

            Collection constraints = (Collection) entry.getValue();
            if (constraints == null || constraints.isEmpty()) {
                continue;
            }

            Object tableName = entry.getKey();
            Iterator cit = constraints.iterator();
            while (cit.hasNext()) {
                Object constraint = cit.next();
                StringBuffer drop = new StringBuffer();
                drop
                        .append("ALTER TABLE ")
                        .append(tableName)
                        .append(" DROP CONSTRAINT ")
                        .append(constraint);
                executeDDL(conn, drop.toString());
            }
        }
    }

    public void droppedTables(Connection con, DataMap map) throws Exception {

    }

    /**
     * Callback method that allows Delegate to customize test procedure.
     */
    public void tweakProcedure(Procedure proc) {
    }

    public void willCreateTables(Connection con, DataMap map) throws Exception {
    }

    public void createdTables(Connection con, DataMap map) throws Exception {

    }

    public boolean supportsStoredProcedures() {
        return false;
    }

    /**
     * Returns whether the target database supports expressions in the WHERE clause in the
     * form "VALUE = COLUMN".
     */
    public boolean supportsReverseComparison() {
        return true;
    }

    /**
     * Returns whether an aggregate query like "SELECT min(X) FROM" returns a NULL row
     * when no data matched the WHERE clause. Most DB's do.
     */
    public boolean supportNullRowForAggregateFunctions() {
        return true;
    }

    /**
     * Returns whether the database supports synatax like "X = NULL".
     */
    public boolean supportsEqualNullSyntax() {
        return true;
    }

    /**
     * Returns whether the DB supports a TRIM function for an arbitrary character.
     */
    public boolean supportsTrimChar() {
        return false;
    }

    /**
     * Returns false if stored procedures are not supported or if it is a victim of
     * CAY-148 (column name capitalization).
     */
    public boolean canMakeObjectsOutOfProcedures() {
        return supportsStoredProcedures();
    }

    /**
     * Returns whether LOBs can be inserted from test XML files.
     */
    public boolean supportsLobInsertsAsStrings() {
        return supportsLobs();
    }

    public boolean supportsFKConstraints(DbEntity entity) {
        if ("FK_OF_DIFFERENT_TYPE".equals(entity.getName())) {
            return false;
        }

        return true;
    }

    /**
     * Returns true if the target database has support for large objects (BLOB, CLOB).
     */
    public boolean supportsLobs() {
        return false;
    }

    public boolean supportsBinaryPK() {
        return true;
    }

    public boolean supportsHaving() {
        return true;
    }

    public boolean supportsCaseSensitiveLike() {
        return true;
    }

    public boolean supportsCaseInsensitiveOrder() {
        return true;
    }

    public boolean supportsBatchPK() {
        return true;
    }

    protected void executeDDL(Connection con, String ddl) throws Exception {
        logObj.info(ddl);
        Statement st = con.createStatement();

        try {
            st.execute(ddl);
        }
        finally {
            st.close();
        }
    }

    protected void executeDDL(Connection con, String database, String name)
            throws Exception {
        executeDDL(con, ddlString(database, name));
    }

    /**
     * Returns a file under test resources DDL directory for the specified database.
     */
    String ddlString(String database, String name) {
        StringBuffer location = new StringBuffer();
        location.append("ddl/").append(database).append("/").append(name);

        InputStream resource = Thread
                .currentThread()
                .getContextClassLoader()
                .getResourceAsStream(location.toString());

        if (resource == null) {
            throw new CayenneRuntimeException("Can't find DDL file: " + location);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(resource));
        StringBuffer buf = new StringBuffer();
        try {
            String line = null;
            while ((line = in.readLine()) != null) {
                buf.append(line).append('\n');
            }
        }
        catch (IOException e) {
            throw new CayenneRuntimeException("Error reading DDL file: " + location);
        }
        finally {

            try {
                in.close();
            }
            catch (IOException e) {

            }
        }
        return buf.toString();
    }

    public boolean handlesNullVsEmptyLOBs() {
        return supportsLobs();
    }

    /**
     * Returns a map of database constraints with DbEntity names used as keys, and
     * Collections of constraint names as values.
     */
    protected Map getConstraints(Connection conn, DataMap map, Collection includeTables)
            throws SQLException {
        Map constraintMap = new HashMap();

        DatabaseMetaData metadata = conn.getMetaData();
        Iterator it = includeTables.iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            DbEntity entity = map.getDbEntity(name);
            if (entity == null) {
                continue;
            }

            // Get all constraints for the table
            ResultSet rs = metadata.getExportedKeys(entity.getCatalog(), entity
                    .getSchema(), entity.getName());
            try {
                while (rs.next()) {
                    String fk = rs.getString("FK_NAME");
                    String fkTable = rs.getString("FKTABLE_NAME");

                    if (fk != null && fkTable != null) {
                        Collection constraints = (Collection) constraintMap.get(fkTable);
                        if (constraints == null) {
                            // use a set to avoid duplicate constraints
                            constraints = new HashSet();
                            constraintMap.put(fkTable, constraints);
                        }

                        constraints.add(fk);
                    }
                }
            }
            finally {
                rs.close();
            }
        }

        return constraintMap;
    }
}
