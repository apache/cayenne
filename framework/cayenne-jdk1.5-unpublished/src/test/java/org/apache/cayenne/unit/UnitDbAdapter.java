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
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.di.Inject;
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
 */
public class UnitDbAdapter {

    private static Log logger = LogFactory.getLog(UnitDbAdapter.class);
    
    @Inject
    protected RuntimeProperties runtimeProperties;

    protected DbAdapter adapter;

    public UnitDbAdapter(DbAdapter adapter) {
        if (adapter == null) {
            throw new CayenneRuntimeException("Null adapter.");
        }
        this.adapter = adapter;
    }
    
    /**
     * Returns whether the target DB treats REAL values as DOUBLEs. Default is
     * false, i.e. REALs are treated as FLOATs.
     * 
     * @return
     */
    public boolean realAsDouble() {
        return false;
    }

    /**
     * Drops all table constraints.
     */
    public void willDropTables(
            Connection conn,
            DataMap map,
            Collection<String> tablesToDrop) throws Exception {
        dropConstraints(conn, map, tablesToDrop);
    }

    protected void dropConstraints(
            Connection conn,
            DataMap map,
            Collection<String> tablesToDrop) throws Exception {
        Map<String, Collection<String>> constraintsMap = getConstraints(
                conn,
                map,
                tablesToDrop);

        for (Map.Entry<String, Collection<String>> entry : constraintsMap.entrySet()) {

            Collection<String> constraints = entry.getValue();
            if (constraints == null || constraints.isEmpty()) {
                continue;
            }

            Object tableName = entry.getKey();
            DbEntity entity = map.getDbEntity(tableName.toString());
            if (entity == null) {
                continue;
            }
            boolean status = entity.getDataMap() != null
                    && entity.getDataMap().isQuotingSQLIdentifiers();
            QuotingStrategy strategy = adapter.getQuotingStrategy(status);

            for (String constraint : constraints) {
                StringBuilder drop = new StringBuilder();

                drop.append("ALTER TABLE ").append(
                        strategy.quoteFullyQualifiedName(entity)).append(
                        " DROP CONSTRAINT ").append(constraint);
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

    public boolean supportsAllAnySome() {
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

    public boolean supportsFKConstraints() {
        return true;
    }

    public boolean supportsColumnTypeReengineering() {
        return true;
    }

    /**
     * Returns true if the target database has support for large objects (BLOB, CLOB).
     */
    public boolean supportsLobs() {
        return false;
    }
    
    public boolean supportsLobComparisons() {
        return supportsLobs();
    }

    public boolean supportsBinaryPK() {
        return true;
    }

    public boolean supportsHaving() {
        return true;
    }

    public boolean supportsCaseSensitiveLike() {
        return !runtimeProperties.getBoolean(JdbcAdapter.CI_PROPERTY, false);
    }

    public boolean supportsCaseInsensitiveOrder() {
        return true;
    }

    public boolean supportsBatchPK() {
        return true;
    }
    
    public boolean supportsBitwiseOps() {
        return false;
    }

    protected void executeDDL(Connection con, String ddl) throws Exception {
        logger.info(ddl);
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
    private String ddlString(String database, String name) {
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
    protected Map<String, Collection<String>> getConstraints(
            Connection conn,
            DataMap map,
            Collection<String> includeTables) throws SQLException {

        Map<String, Collection<String>> constraintMap = new HashMap<String, Collection<String>>();

        DatabaseMetaData metadata = conn.getMetaData();

        for (String name : includeTables) {
            DbEntity entity = map.getDbEntity(name);
            if (entity == null) {
                continue;
            }
            boolean status = entity.getDataMap() != null
                    && entity.getDataMap().isQuotingSQLIdentifiers();
            QuotingStrategy strategy = adapter.getQuotingStrategy(status);

            // Get all constraints for the table
            ResultSet rs = metadata.getExportedKeys(entity.getCatalog(), entity
                    .getSchema(), entity.getName());
            try {
                while (rs.next()) {
                    String fk = rs.getString("FK_NAME");
                    String fkTable = rs.getString("FKTABLE_NAME");

                    if (fk != null && fkTable != null) {
                        Collection<String> constraints = constraintMap.get(fkTable);
                        if (constraints == null) {
                            // use a set to avoid duplicate constraints
                            constraints = new HashSet<String>();
                            constraintMap.put(fkTable, constraints);
                        }

                        constraints.add(strategy.quoteString(fk));
                    }
                }
            }
            finally {
                rs.close();
            }
        }

        return constraintMap;
    }

    public QuotingStrategy getQuotingStrategy(boolean status) {
        return adapter.getQuotingStrategy(status);
    }

    public boolean supportsNullBoolean() {
        return true;
    }
    
    public boolean supportsBoolean() {
        return true;
    }
}
