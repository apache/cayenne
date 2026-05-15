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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dba.db2.DB2Adapter;
import org.apache.cayenne.dba.derby.DerbyAdapter;
import org.apache.cayenne.dba.firebird.FirebirdAdapter;
import org.apache.cayenne.dba.frontbase.FrontBaseAdapter;
import org.apache.cayenne.dba.h2.H2Adapter;
import org.apache.cayenne.dba.hsqldb.HSQLDBAdapter;
import org.apache.cayenne.dba.ingres.IngresAdapter;
import org.apache.cayenne.dba.mysql.MySQLAdapter;
import org.apache.cayenne.dba.oracle.Oracle8Adapter;
import org.apache.cayenne.dba.oracle.OracleAdapter;
import org.apache.cayenne.dba.postgres.PostgresAdapter;
import org.apache.cayenne.dba.sqlite.SQLiteAdapter;
import org.apache.cayenne.dba.sqlserver.SQLServerAdapter;
import org.apache.cayenne.dba.sybase.SybaseAdapter;
import org.apache.cayenne.exp.parser.ASTExtract;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Procedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.Objects;

/**
 * Defines API and a common superclass for testing various database features.
 * Different databases support different feature sets that need to be tested
 * differently. Many things implemented in subclasses may become future
 * candidates for inclusion in the corresponding adapter code.
 */
public abstract class TestDbAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestDbAdapter.class);

    public static TestDbAdapter of(DbAdapter adapter) {

        DbAdapter realAdapter = adapter.unwrap();

        // the order of cases is important due to random adapter subclasses
        return switch (realAdapter) {
            case FirebirdAdapter a -> new FirebirdTestDbAdapter(a);
            case Oracle8Adapter a -> new OracleTestDbAdapter(a);
            case OracleAdapter a -> new OracleTestDbAdapter(a);
            case DerbyAdapter a -> new DerbyTestDbAdapter(a);
            case SQLServerAdapter a -> new SQLServerTestDbAdapter(a);
            case SybaseAdapter a -> new SybaseTestDbAdapter(a);
            case MySQLAdapter a -> new MySQLTestDbAdapter(a);
            case PostgresAdapter a -> new PostgresTestDbAdapter(a);
            case DB2Adapter a -> new DB2TestDbAdapter(a);
            case HSQLDBAdapter a -> new HSQLDBTestDbAdapter(a);
            case H2Adapter a -> new H2TestDbAdapter(a);
            case FrontBaseAdapter a -> new FrontBaseTestDbAdapter(a);
            case IngresAdapter a -> new IngresTestDbAdapter(a);
            case SQLiteAdapter a -> new SQLiteTestDbAdapter(a);

            default -> throw new IllegalStateException("Unmapped adapter type: " + realAdapter.getClass().getName());
        };
    }

    protected final DbAdapter adapter;

    protected TestDbAdapter(DbAdapter adapter) {
        this.adapter = Objects.requireNonNull(adapter);
    }

    public boolean supportsPKGeneratorConcurrency() {
        return true;
    }

    public String getIdentifiersStartQuote() {
        return "\"";
    }

    public String getIdentifiersEndQuote() {
        return "\"";
    }

    /**
     * Returns whether the target DB treats REAL values as DOUBLEs. Default is
     * false, i.e. REALs are treated as FLOATs.
     */
    public boolean realAsDouble() {
        return false;
    }

    /**
     * Drops all table constraints.
     */
    public void willDropTables(Connection conn, DataMap map, Collection<String> tablesToDrop) throws Exception {
        dropConstraints(conn, map, tablesToDrop);
    }

    protected void dropConstraints(Connection conn, DataMap map, Collection<String> tablesToDrop) throws Exception {
        Map<String, Collection<String>> constraintsMap = getConstraints(conn, map, tablesToDrop);

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

            QuotingStrategy strategy = adapter.getQuotingStrategy();

            for (String constraint : constraints) {

                String drop = "ALTER TABLE " + strategy.quotedFullyQualifiedName(entity) +
                        " DROP CONSTRAINT " + constraint;
                executeDDL(conn, drop);
            }
        }
    }

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
     * Returns whether the target database supports expressions in the WHERE
     * clause in the form "VALUE = COLUMN".
     */
    public boolean supportsReverseComparison() {
        return true;
    }

    /**
     * Returns whether an aggregate query like "SELECT min(X) FROM" returns a
     * NULL row when no data matched the WHERE clause. Most DB's do.
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
     * Returns whether the DB supports a TRIM function for an arbitrary
     * character.
     */
    public boolean supportsTrimChar() {
        return false;
    }

    /**
     * Returns false if stored procedures are not supported or if it is a victim
     * of CAY-148 (column name capitalization).
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
        return !"FK_OF_DIFFERENT_TYPE".equals(entity.getName());
    }

    public boolean supportsFKConstraints() {
        return true;
    }

    /**
     * Returns true if the target database has support for large objects (BLOB,
     * CLOB).
     */
    public boolean supportsLobs() {
        return false;
    }

    public boolean supportsLobComparisons() {
        return supportsLobs();
    }

    /**
     * Returns true if the target database has native json data type.
     */
    public boolean supportsJsonType() {
        return false;
    }

    public boolean supportsBinaryPK() {
        return true;
    }

    public boolean supportsCaseInsensitiveOrder() {
        return true;
    }

    public boolean supportsCatalogs() {
        return false;
    }

    public boolean supportsBatchPK() {
        return true;
    }

    public boolean supportsBitwiseOps() {
        return false;
    }

    protected void executeDDL(Connection con, String ddl) throws Exception {
        LOGGER.info(ddl);

        try (Statement st = con.createStatement()) {
            st.execute(ddl);
        }
    }

    protected void executeDDL(Connection con, String database, String name) throws Exception {
        executeDDL(con, ddlString(database, name));
    }

    /**
     * Returns a file under test resources DDL directory for the specified
     * database.
     */
    private String ddlString(String database, String name) {
        StringBuilder location = new StringBuilder();
        location.append("ddl/").append(database).append("/").append(name);

        InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(location.toString());

        if (resource == null) {
            throw new CayenneRuntimeException("Can't find DDL file: " + location);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(resource));
        StringBuilder buf = new StringBuilder();
        try {
            String line;
            while ((line = in.readLine()) != null) {
                buf.append(line).append('\n');
            }
        } catch (IOException e) {
            throw new CayenneRuntimeException("Error reading DDL file: " + location);
        } finally {

            try {
                in.close();
            } catch (IOException e) {

            }
        }
        return buf.toString();
    }

    public boolean handlesNullVsEmptyLOBs() {
        return supportsLobs();
    }

    /**
     * Returns a map of database constraints with DbEntity names used as keys,
     * and Collections of constraint names as values.
     */
    protected Map<String, Collection<String>> getConstraints(Connection conn, DataMap map,
                                                             Collection<String> includeTables) throws SQLException {

        Map<String, Collection<String>> constraintMap = new HashMap<>();

        DatabaseMetaData metadata = conn.getMetaData();

        for (String name : includeTables) {
            DbEntity entity = map.getDbEntity(name);
            if (entity == null) {
                continue;
            }

            QuotingStrategy strategy = adapter.getQuotingStrategy();

            // Get all constraints for the table
            try (ResultSet rs = metadata.getExportedKeys(entity.getCatalog(), entity.getSchema(), entity.getName())) {
                while (rs.next()) {
                    String fk = rs.getString("FK_NAME");
                    String fkTable = rs.getString("FKTABLE_NAME");

                    if (fk != null && fkTable != null) {
                        Collection<String> constraints = constraintMap
                                .computeIfAbsent(fkTable, k -> new HashSet<String>());
                        // use a set to avoid duplicate constraints
                        constraints.add(strategy.quotedIdentifier(entity, fk));
                    }
                }
            }
        }

        return constraintMap;
    }

    public boolean isLowerCaseNames() {
        return false;
    }

    public boolean onlyGenericNumberType() {
        return false;
    }

    public boolean supportsTimeSqlType() {
        return true;
    }

    public boolean onlyGenericDateType() {
        return false;
    }

    public boolean supportsNullBoolean() {
        return true;
    }

    public boolean supportsBoolean() {
        return true;
    }

    public boolean supportsGeneratedKeys() {
        return adapter.supportsGeneratedKeys();
    }

    public boolean supportsGeneratedKeysAdd() {
        return false;
    }

    public boolean supportsGeneratedKeysDrop() {
        return false;
    }

    public boolean supportsEscapeInLike() {
        return true;
    }

    public boolean supportsExpressionInHaving() {
        return true;
    }

    /**
     * Support for select like this:
     * SELECT (intColumn < 10) AS bool FROM table
     */
    public boolean supportsSelectBooleanExpression() {
        return true;
    }

    public boolean supportsExtractPart(ASTExtract.DateTimePart part) {
        return true;
    }

    public boolean supportsSerializableTransactionIsolation() {
        return false;
    }

    public boolean supportsNullComparison() {
        return true;
    }

    /**
     * Support for select like this:
     * SELECT t0.ARTIST_NAME FROM ARTIST t0 WHERE 'abc'
     */
    public boolean supportScalarAsExpression() {
        return false;
    }

    /**
     * Returns true if the target database has time type with fractional seconds.
     */
    public boolean supportsPreciseTime() {
        return true;
    }
}
