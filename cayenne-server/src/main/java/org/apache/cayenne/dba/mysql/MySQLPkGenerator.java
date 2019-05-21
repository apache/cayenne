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
import org.apache.cayenne.tx.BaseTransaction;
import org.apache.cayenne.tx.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class MySQLPkGenerator extends JdbcPkGenerator {

    private static final Logger logger = LoggerFactory.getLogger(MySQLPkGenerator.class);

    /**
     * Used by DI
     * @since 4.1
     */
    public MySQLPkGenerator() {
        super();
    }

    MySQLPkGenerator(JdbcAdapter adapter) {
        super(adapter);
    }

    /**
     * Overrides superclass's implementation to perform locking of the primary
     * key lookup table.
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
        long pk = -1L;

        // Start new transaction if needed, can any way lead to problems when
        // using external transaction manager. We can only warn about it.
        // See https://issues.apache.org/jira/browse/CAY-2186 for details.
        Transaction transaction = BaseTransaction.getThreadTransaction();
        if (transaction != null && transaction.isExternal()) {
            logger.warn("Using MysqlPkGenerator with external transaction manager may lead to inconsistent state.");
        }
        BaseTransaction.bindThreadTransaction(null);

        try (Connection con = node.getDataSource().getConnection()) {

            if (con.getAutoCommit()) {
                con.setAutoCommit(false);
            }

            try (Statement st = con.createStatement()) {
                try {
                    pk = getLongPrimaryKey(st, entity.getName());
                    con.commit();
                } catch (SQLException pkEx) {
                    try {
                        con.rollback();
                    } catch (SQLException ignored) {
                    }

                    exception = processSQLException(pkEx, null);
                } finally {
                    // UNLOCK!
                    // THIS MUST BE EXECUTED NO MATTER WHAT, OR WE WILL LOCK THE PRIMARY KEY TABLE!!
                    try {
                        String unlockString = "UNLOCK TABLES";
                        adapter.getJdbcEventLogger().log(unlockString);
                        st.execute(unlockString);
                    } catch (SQLException unlockEx) {
                        exception = processSQLException(unlockEx, exception);
                    }
                }
            }
        } catch (SQLException otherEx) {
            exception = processSQLException(otherEx, null);
        } finally {
            BaseTransaction.bindThreadTransaction(transaction);
        }

        // check errors
        if (exception != null) {
            throw exception;
        }

        return pk;

    }

    /**
     * Appends a new SQLException to the chain. If parent is null, uses the
     * exception as the chain root.
     */
    protected SQLException processSQLException(SQLException exception, SQLException parent) {
        if (parent == null) {
            return exception;
        }

        parent.setNextException(exception);
        return parent;
    }

    @Override
    protected String dropAutoPkString() {
        return "DROP TABLE IF EXISTS AUTO_PK_SUPPORT";
    }

    @Override
    protected String pkTableCreateString() {
        return "CREATE TABLE IF NOT EXISTS AUTO_PK_SUPPORT " +
                "(TABLE_NAME CHAR(100) NOT NULL, NEXT_ID BIGINT NOT NULL, UNIQUE (TABLE_NAME)) " +
                "ENGINE=" + MySQLAdapter.DEFAULT_STORAGE_ENGINE;
    }

    /**
     * @since 3.0
     */
    protected long getLongPrimaryKey(Statement statement, String entityName) throws SQLException {
        // lock
        String lockString = "LOCK TABLES AUTO_PK_SUPPORT WRITE";
        adapter.getJdbcEventLogger().log(lockString);
        statement.execute(lockString);

        // select
        String selectString = super.pkSelectString(entityName);
        adapter.getJdbcEventLogger().log(selectString);
        long pk;
        try (ResultSet rs = statement.executeQuery(selectString)) {
            if (!rs.next()) {
                throw new SQLException("No rows for '" + entityName + "'");
            }

            pk = rs.getLong(1);
            if (rs.next()) {
                throw new SQLException("More than one row for '" + entityName + "'");
            }
        }

        // update
        String updateString = super.pkUpdateString(entityName) + " AND NEXT_ID = " + pk;
        adapter.getJdbcEventLogger().log(updateString);
        int updated = statement.executeUpdate(updateString);
        // optimistic lock failure...
        if (updated != 1) {
            throw new SQLException("Error updating PK count '" + entityName + "': " + updated);
        }

        return pk;
    }
}
