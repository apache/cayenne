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

package org.apache.cayenne.dba.mysql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.JdbcPkGenerator;
import org.apache.cayenne.map.DbEntity;

/**
 */
public class MySQLPkGenerator extends JdbcPkGenerator {

    MySQLPkGenerator(JdbcAdapter adapter) {
        super(adapter);
    }

    @Override
    protected String dropAutoPkString() {
        return "DROP TABLE IF EXISTS AUTO_PK_SUPPORT";
    }

    /**
     * Overrides superclass's implementation to perform locking of the primary key lookup
     * table.
     * 
     * @since 3.0
     */
    @Override
    protected long longPkFromDatabase(DataNode node, DbEntity entity) throws Exception {

        // must work directly with JDBC connection, since we
        // must unlock the AUTO_PK_SUPPORT table in case of
        // failures.... ah..JDBC is fun...

        // chained SQL exception
        SQLException exception = null;
        long pk = -1l;

        Connection con = node.getDataSource().getConnection();
        try {

            if (con.getAutoCommit()) {
                con.setAutoCommit(false);
            }

            Statement st = con.createStatement();

            try {
                pk = getLongPrimaryKey(st, entity.getName());
                con.commit();
            }
            catch (SQLException pkEx) {

                try {
                    con.rollback();
                }
                catch (SQLException e) {

                }

                exception = processSQLException(pkEx, exception);
            }
            finally {
                // UNLOCK!
                // THIS MUST BE EXECUTED NO MATTER WHAT, OR WE WILL LOCK THE PRIMARY KEY
                // TABLE!!
                try {
                    String unlockString = "UNLOCK TABLES";
                    adapter.getJdbcEventLogger().logQuery(unlockString, Collections.EMPTY_LIST);
                    st.execute(unlockString);
                }
                catch (SQLException unlockEx) {
                    exception = processSQLException(unlockEx, exception);
                }
                finally {
                    // close statement
                    try {
                        st.close();
                    }
                    catch (SQLException stClosingEx) {
                        // ignoring...
                    }
                }
            }
        }
        catch (SQLException otherEx) {
            exception = processSQLException(otherEx, exception);
        }
        finally {
            try {
                con.close();
            }
            catch (SQLException closingEx) {
                // ignoring
            }
        }

        // check errors
        if (exception != null) {
            throw exception;
        }

        return pk;

    }

    /**
     * Appends a new SQLException to the chain. If parent is null, uses the exception as
     * the chain root.
     */
    protected SQLException processSQLException(SQLException exception, SQLException parent) {
        if (parent == null) {
            return exception;
        }

        parent.setNextException(exception);
        return parent;
    }

    @Override
    protected String pkTableCreateString() {
        StringBuilder buf = new StringBuilder();
        buf.append("CREATE TABLE IF NOT EXISTS AUTO_PK_SUPPORT (").append(
                "  TABLE_NAME CHAR(100) NOT NULL,").append(
                "  NEXT_ID BIGINT NOT NULL, UNIQUE (TABLE_NAME)").append(")");

        return buf.toString();
    }

    /**
     * @since 3.0
     */
    protected long getLongPrimaryKey(Statement statement, String entityName)
            throws SQLException {
        // lock
        String lockString = "LOCK TABLES AUTO_PK_SUPPORT WRITE";
        adapter.getJdbcEventLogger().logQuery(lockString, Collections.EMPTY_LIST);
        statement.execute(lockString);

        // select
        long pk = -1;

        String selectString = super.pkSelectString(entityName);
        adapter.getJdbcEventLogger().logQuery(selectString, Collections.EMPTY_LIST);
        ResultSet rs = statement.executeQuery(selectString);
        try {
            if (!rs.next()) {
                throw new SQLException("No rows for '" + entityName + "'");
            }

            pk = rs.getLong(1);

            if (rs.next()) {
                throw new SQLException("More than one row for '" + entityName + "'");
            }
        }
        finally {
            try {
                rs.close();
            }
            catch (Exception ex) {
                // ignoring...
            }
        }

        // update
        String updateString = super.pkUpdateString(entityName) + " AND NEXT_ID = " + pk;
        adapter.getJdbcEventLogger().logQuery(updateString, Collections.EMPTY_LIST);
        int updated = statement.executeUpdate(updateString);
        // optimistic lock failure...
        if (updated != 1) {
            throw new SQLException("Error updating PK count '"
                    + entityName
                    + "': "
                    + updated);
        }

        return pk;
    }
}
