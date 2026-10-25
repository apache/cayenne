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

package org.apache.cayenne.dba;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.jdbc.PSParameter;
import org.apache.cayenne.access.jdbc.RSColumn;
import org.apache.cayenne.access.translator.TranslatedSQL;
import org.apache.cayenne.log.SQLLogger;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbKeyGenerator;
import org.apache.cayenne.util.IDUtil;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * Default primary key generator implementation. Uses a lookup table named
 * "AUTO_PK_SUPPORT" to search and increment primary keys for tables.
 */
public class JdbcPkGenerator implements PkGenerator {

    public static final int DEFAULT_PK_CACHE_SIZE = 20;
    static final long DEFAULT_PK_START_VALUE = 200;

    protected final ConcurrentMap<String, Queue<Long>> pkCache;

    protected int pkCacheSize = DEFAULT_PK_CACHE_SIZE;
    protected long pkStartValue = DEFAULT_PK_START_VALUE;

    public JdbcPkGenerator() {
        this.pkCache = new ConcurrentHashMap<>();
    }

    @Override
    public void createAutoPk(DataNode node, List<DbEntity> dbEntities) {
        DbAdapter adapter = node.getAdapter();
        // check if a table exists

        // create AUTO_PK_SUPPORT table
        if (!autoPkTableExists(node)) {
            runUpdate(node, pkTableCreateString(adapter));
        }

        // delete any existing pk entries
        if (!dbEntities.isEmpty()) {
            runUpdate(node, pkDeleteString(dbEntities));
        }

        // insert all needed entries
        for (DbEntity ent : dbEntities) {
            runUpdate(node, pkCreateString(ent.getName()));
        }
    }

    @Override
    public List<String> createAutoPkStatements(DbAdapter adapter, List<DbEntity> dbEntities) {
        List<String> list = new ArrayList<>(dbEntities.size() + 2);

        list.add(pkTableCreateString(adapter));
        list.add(pkDeleteString(dbEntities));

        for (DbEntity ent : dbEntities) {
            list.add(pkCreateString(ent.getName()));
        }

        return list;
    }

    /**
     * Drops table named "AUTO_PK_SUPPORT" if it exists in the database.
     */
    @Override
    public void dropAutoPk(DataNode node, List<DbEntity> dbEntities) {
        DbAdapter adapter = node.getAdapter();
        if (autoPkTableExists(node)) {
            runUpdate(node, dropAutoPkString(adapter));
        }
    }

    @Override
    public List<String> dropAutoPkStatements(DbAdapter adapter, List<DbEntity> dbEntities) {
        List<String> list = new ArrayList<>(1);
        list.add(dropAutoPkString(adapter));
        return list;
    }

    protected String pkTableCreateString(DbAdapter adapter) {
        return "CREATE TABLE AUTO_PK_SUPPORT (TABLE_NAME CHAR(100) NOT NULL, NEXT_ID BIGINT NOT NULL, PRIMARY KEY(TABLE_NAME))";
    }

    protected String pkDeleteString(List<DbEntity> dbEntities) {
        StringBuilder buf = new StringBuilder();
        buf.append("DELETE FROM AUTO_PK_SUPPORT WHERE TABLE_NAME IN (");
        int len = dbEntities.size();
        for (int i = 0; i < len; i++) {
            if (i > 0) {
                buf.append(", ");
            }
            DbEntity ent = dbEntities.get(i);
            buf.append('\'').append(ent.getName()).append('\'');
        }
        buf.append(')');
        return buf.toString();
    }

    protected String pkCreateString(String entName) {
        return "INSERT INTO AUTO_PK_SUPPORT (TABLE_NAME, NEXT_ID) VALUES ('" + entName + "', " + pkStartValue + ")";
    }

    protected String pkSelectString(String entName) {
        return "SELECT NEXT_ID FROM AUTO_PK_SUPPORT WHERE TABLE_NAME = '" + entName + '\'';
    }

    protected String pkUpdateString(String entName) {
        return "UPDATE AUTO_PK_SUPPORT SET NEXT_ID = NEXT_ID + " + pkCacheSize + " WHERE TABLE_NAME = '" + entName + '\'';
    }

    protected String dropAutoPkString(DbAdapter adapter) {
        return "DROP TABLE AUTO_PK_SUPPORT";
    }

    /**
     * Checks if AUTO_PK_TABLE already exists in the database.
     */
    protected boolean autoPkTableExists(DataNode node) {

        try (Connection con = node.getDataSource().getConnection()) {
            DatabaseMetaData md = con.getMetaData();
            try (ResultSet tables = md.getTables(null, null, "AUTO_PK_SUPPORT", null)) {
                return tables.next();
            }
        } catch (SQLException e) {
            throw new CayenneRuntimeException("Error checking for AUTO_PK_SUPPORT table", e);
        }
    }

    /**
     * Runs JDBC update over a Connection obtained from DataNode. Returns a
     * number of objects returned from update.
     */
    public int runUpdate(DataNode node, String sql) {
        try (Connection con = node.getDataSource().getConnection()) {
            return executeUpdate(con, sql, node.getSqlLogger());
        } catch (SQLException e) {
            throw new CayenneRuntimeException("Error executing PK update: %s", e, sql);
        }
    }

    /**
     * Runs a plain SQL update, logging it as a statement with its update count and execution time, the same way
     * the SQL actions log their statements.
     *
     * @since 6.0
     */
    protected static int executeUpdate(Connection con, String sql, SQLLogger logger) throws SQLException {
        TranslatedSQL statement = new TranslatedSQL(sql, new PSParameter[0], new RSColumn[0]);
        long start = System.nanoTime();
        try (Statement update = con.createStatement()) {
            int count = update.executeUpdate(sql);
            logger.logUpdate(statement, count, List.of(), elapsedMillis(start));
            return count;
        } catch (SQLException e) {
            logger.logQueryError(statement, e, elapsedMillis(start));
            throw e;
        }
    }

    /**
     * Runs a plain SQL select of a single long value, logging it as a statement with its row count and execution
     * time, the same way the SQL actions log their statements. Returns null if the select returns no rows, and
     * throws if it returns more than one.
     *
     * @since 6.0
     */
    protected static Long selectLong(Connection con, String sql, SQLLogger logger) throws SQLException {
        TranslatedSQL statement = new TranslatedSQL(sql, new PSParameter[0], new RSColumn[0]);
        long start = System.nanoTime();
        try (Statement select = con.createStatement(); ResultSet rs = select.executeQuery(sql)) {
            if (!rs.next()) {
                logger.logSelect(statement, 0, elapsedMillis(start));
                return null;
            }

            long value = rs.getLong(1);
            if (rs.next()) {
                logger.logSelect(statement, 2, elapsedMillis(start));
                throw new CayenneRuntimeException("Expected one row, got more: %s", sql);
            }

            logger.logSelect(statement, 1, elapsedMillis(start));
            return value;
        } catch (SQLException e) {
            logger.logQueryError(statement, e, elapsedMillis(start));
            throw e;
        }
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    @Override
    public Object generatePk(DataNode node, DbAttribute pk) {

        DbEntity entity = pk.getEntity();

        switch (pk.getType()) {
            case Types.BINARY:
            case Types.VARBINARY:
                return IDUtil.pseudoUniqueSecureByteSequence(pk.getMaxLength());
        }

        DbKeyGenerator pkGenerator = entity.getPrimaryKeyGenerator();
        long cacheSize;
        if (pkGenerator != null && pkGenerator.getKeyCacheSize() != null) {
            cacheSize = pkGenerator.getKeyCacheSize();
        } else {
            cacheSize = getPkCacheSize();
        }

        Long value;

        // if no caching, always generate fresh
        if (cacheSize <= 1) {
            value = longPkFromDatabase(node, entity);
        } else {
            Queue<Long> pks = pkCache.get(entity.getName());

            if (pks == null) {
                // created exhausted LongPkRange
                pks = new ConcurrentLinkedQueue<>();
                Queue<Long> previousPks = pkCache.putIfAbsent(entity.getName(), pks);
                if (previousPks != null) {
                    pks = previousPks;
                }
            }

            value = pks.poll();
            if (value == null) {
                value = longPkFromDatabase(node, entity);
                for (long i = value + 1; i < value + cacheSize; i++) {
                    pks.add(i);
                }
            }
        }

        if (pk.getType() == Types.BIGINT) {
            return value;
        } else {
            // leaving it up to the user to ensure that PK does not exceed max int...
            return value.intValue();
        }
    }

    /**
     * Performs primary key generation ignoring cache. Generates a range of
     * primary keys as specified by "pkCacheSize" bean property.
     * <p>
     * This method is called internally from "generatePkForDbEntity" and then
     * generated range of key values is saved in cache for performance.
     * Subclasses that implement different primary key generation solutions
     * should override this method, not "generatePkForDbEntity".
     * </p>
     *
     * @since 3.0
     */
    protected long longPkFromDatabase(DataNode node, DbEntity entity) {
        String select = "SELECT NEXT_ID FROM AUTO_PK_SUPPORT WHERE TABLE_NAME = '" + entity.getName() + '\'';
        String update = pkUpdateString(entity.getName());

        // plain JDBC on a single connection, so that the select and the update share the transaction, if any
        SQLLogger logger = node.getSqlLogger();
        try (Connection con = node.getDataSource().getConnection()) {
            Long id = selectLong(con, select, logger);
            if (id == null) {
                throw new CayenneRuntimeException("Error generating PK : entity not supported: %s", entity.getName());
            }

            executeUpdate(con, update, logger);
            return id;
        } catch (SQLException e) {
            throw new CayenneRuntimeException("Error generating PK for entity '%s'.", e, entity.getName());
        }
    }

    /**
     * Returns a size of the entity primary key cache. Default value is 20. If
     * cache size is set to a value less or equals than "one", no primary key
     * caching is done.
     */
    public int getPkCacheSize() {
        return pkCacheSize;
    }

    /**
     * Sets the size of the entity primary key cache. If
     * <code>pkCacheSize</code> parameter is less than 1, cache size is set to
     * "one".
     * <p>
     * <i>Note that our tests show that setting primary key cache value to
     * anything much bigger than 20 does not give any significant performance
     * increase. Therefore it does not make sense to use bigger values, since
     * this may potentially create big gaps in the database primary key
     * sequences in cases like application crashes or restarts. </i>
     * </p>
     */
    public void setPkCacheSize(int pkCacheSize) {
        this.pkCacheSize = (pkCacheSize < 1) ? 1 : pkCacheSize;
    }

    long getPkStartValue() {
        return pkStartValue;
    }

    void setPkStartValue(long startValue) {
        this.pkStartValue = startValue;
    }

    @Override
    public void reset() {
        pkCache.clear();
    }
}
