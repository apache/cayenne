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

package org.apache.cayenne.dba;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.ResultIterator;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbKeyGenerator;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.util.IDUtil;

/**
 * Default primary key generator implementation. Uses a lookup table named
 * "AUTO_PK_SUPPORT" to search and increment primary keys for tables.
 */
public class JdbcPkGenerator implements PkGenerator {

    public static final int DEFAULT_PK_CACHE_SIZE = 20;
    static final long DEFAULT_PK_START_VALUE = 200;

    protected JdbcAdapter adapter;
    protected Map<String, LongPkRange> pkCache = new HashMap<String, LongPkRange>();
    protected int pkCacheSize = DEFAULT_PK_CACHE_SIZE;
    protected long pkStartValue = DEFAULT_PK_START_VALUE;

    public JdbcPkGenerator(JdbcAdapter adapter) {
        this.adapter = adapter;
    }

    public JdbcAdapter getAdapter() {
        return adapter;
    }

    public void createAutoPk(DataNode node, List<DbEntity> dbEntities) throws Exception {
        // check if a table exists

        // create AUTO_PK_SUPPORT table
        if (!autoPkTableExists(node)) {
            runUpdate(node, pkTableCreateString());
        }

        // delete any existing pk entries
        runUpdate(node, pkDeleteString(dbEntities));

        // insert all needed entries
        for (DbEntity ent : dbEntities) {
            runUpdate(node, pkCreateString(ent.getName()));
        }
    }

    public List<String> createAutoPkStatements(List<DbEntity> dbEntities) {
        List<String> list = new ArrayList<String>(dbEntities.size() + 2);

        list.add(pkTableCreateString());
        list.add(pkDeleteString(dbEntities));

        for (DbEntity ent : dbEntities) {
            list.add(pkCreateString(ent.getName()));
        }

        return list;
    }

    /**
     * Drops table named "AUTO_PK_SUPPORT" if it exists in the database.
     */
    public void dropAutoPk(DataNode node, List<DbEntity> dbEntities) throws Exception {
        if (autoPkTableExists(node)) {
            runUpdate(node, dropAutoPkString());
        }
    }

    public List<String> dropAutoPkStatements(List<DbEntity> dbEntities) {
        List<String> list = new ArrayList<String>(1);
        list.add(dropAutoPkString());
        return list;
    }

    protected String pkTableCreateString() {
        StringBuilder buf = new StringBuilder();
        buf
                .append("CREATE TABLE AUTO_PK_SUPPORT (")
                .append("  TABLE_NAME CHAR(100) NOT NULL,")
                .append("  NEXT_ID BIGINT NOT NULL,")
                .append("  PRIMARY KEY(TABLE_NAME)")
                .append(")");

        return buf.toString();
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
        StringBuilder buf = new StringBuilder();
        buf
                .append("INSERT INTO AUTO_PK_SUPPORT")
                .append(" (TABLE_NAME, NEXT_ID)")
                .append(" VALUES ('")
                .append(entName)
                .append("', ").append(pkStartValue).append(")");
        return buf.toString();
    }

    protected String pkSelectString(String entName) {
        StringBuilder buf = new StringBuilder();
        buf
                .append("SELECT NEXT_ID FROM AUTO_PK_SUPPORT WHERE TABLE_NAME = '")
                .append(entName)
                .append('\'');
        return buf.toString();
    }

    protected String pkUpdateString(String entName) {
        StringBuilder buf = new StringBuilder();
        buf
                .append("UPDATE AUTO_PK_SUPPORT")
                .append(" SET NEXT_ID = NEXT_ID + ")
                .append(pkCacheSize)
                .append(" WHERE TABLE_NAME = '")
                .append(entName)
                .append('\'');
        return buf.toString();
    }

    protected String dropAutoPkString() {
        return "DROP TABLE AUTO_PK_SUPPORT";
    }

    /**
     * Checks if AUTO_PK_TABLE already exists in the database.
     */
    protected boolean autoPkTableExists(DataNode node) throws SQLException {
        Connection con = node.getDataSource().getConnection();
        boolean exists = false;
        try {
            DatabaseMetaData md = con.getMetaData();
            ResultSet tables = md.getTables(null, null, "AUTO_PK_SUPPORT", null);
            try {
                exists = tables.next();
            }
            finally {
                tables.close();
            }
        }
        finally {
            // return connection to the pool
            con.close();
        }

        return exists;
    }

    /**
     * Runs JDBC update over a Connection obtained from DataNode. Returns a number of
     * objects returned from update.
     * 
     * @throws SQLException in case of query failure.
     */
    public int runUpdate(DataNode node, String sql) throws SQLException {
        adapter.getJdbcEventLogger().logQuery(sql, Collections.EMPTY_LIST);

        Connection con = node.getDataSource().getConnection();
        try {
            Statement upd = con.createStatement();
            try {
                return upd.executeUpdate(sql);
            }
            finally {
                upd.close();
            }
        }
        finally {
            con.close();
        }
    }

    /**
     * Generates a unique and non-repeating primary key for specified dbEntity.
     * <p>
     * This implementation is naive since it does not lock the database rows when
     * executing select and subsequent update. Adapter-specific implementations are more
     * robust.
     * </p>
     * 
     * @since 3.0
     */
    public Object generatePk(DataNode node, DbAttribute pk) throws Exception {

        DbEntity entity = (DbEntity) pk.getEntity();

        switch (pk.getType()) {
            case Types.BINARY:
            case Types.VARBINARY:
                return IDUtil.pseudoUniqueSecureByteSequence(pk.getMaxLength());
        }

        DbKeyGenerator pkGenerator = entity.getPrimaryKeyGenerator();
        long cacheSize;
        if (pkGenerator != null && pkGenerator.getKeyCacheSize() != null)
            cacheSize = pkGenerator.getKeyCacheSize().intValue();
        else
            cacheSize = pkCacheSize;

        long value;

        // if no caching, always generate fresh
        if (cacheSize <= 1) {
            value = longPkFromDatabase(node, entity);
        }
        else {
            synchronized (pkCache) {
                LongPkRange r = pkCache.get(entity.getName());

                if (r == null) {
                    // created exhausted LongPkRange
                    r = new LongPkRange(1l, 0l);
                    pkCache.put(entity.getName(), r);
                }

                if (r.isExhausted()) {
                    long val = longPkFromDatabase(node, entity);
                    r.reset(val, val + cacheSize - 1);
                }

                value = r.getNextPrimaryKey();
            }
        }

        if (pk.getType() == Types.BIGINT) {
            return Long.valueOf(value);
        }
        else {
            // leaving it up to the user to ensure that PK does not exceed max int...
            return Integer.valueOf((int) value);
        }
    }

    /**
     * Performs primary key generation ignoring cache. Generates a range of primary keys
     * as specified by "pkCacheSize" bean property.
     * <p>
     * This method is called internally from "generatePkForDbEntity" and then generated
     * range of key values is saved in cache for performance. Subclasses that implement
     * different primary key generation solutions should override this method, not
     * "generatePkForDbEntity".
     * </p>
     * 
     * @since 3.0
     */
    protected long longPkFromDatabase(DataNode node, DbEntity entity) throws Exception {
        String select = "SELECT #result('NEXT_ID' 'long' 'NEXT_ID') "
                + "FROM AUTO_PK_SUPPORT "
                + "WHERE TABLE_NAME = '"
                + entity.getName()
                + '\'';

        // run queries via DataNode to utilize its transactional behavior
        List<Query> queries = new ArrayList<Query>(2);
        queries.add(new SQLTemplate(entity, select));
        queries.add(new SQLTemplate(entity, pkUpdateString(entity.getName())));

        PkRetrieveProcessor observer = new PkRetrieveProcessor(entity.getName());
        node.performQueries(queries, observer);
        return observer.getId();
    }

    /**
     * Returns a size of the entity primary key cache. Default value is 20. If cache size
     * is set to a value less or equals than "one", no primary key caching is done.
     */
    public int getPkCacheSize() {
        return pkCacheSize;
    }

    /**
     * Sets the size of the entity primary key cache. If <code>pkCacheSize</code>
     * parameter is less than 1, cache size is set to "one".
     * <p>
     * <i>Note that our tests show that setting primary key cache value to anything much
     * bigger than 20 does not give any significant performance increase. Therefore it
     * does not make sense to use bigger values, since this may potentially create big
     * gaps in the database primary key sequences in cases like application crashes or
     * restarts. </i>
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

    public void reset() {
        pkCache.clear();
    }

    /**
     * OperationObserver for primary key retrieval.
     */
    final class PkRetrieveProcessor implements OperationObserver {

        Number id;
        String entityName;

        PkRetrieveProcessor(String entityName) {
            this.entityName = entityName;
        }

        public boolean isIteratedResult() {
            return false;
        }

        public long getId() {
            if (id == null) {
                throw new CayenneRuntimeException("No key was retrieved for entity "
                        + entityName);
            }

            return id.longValue();
        }

        public void nextRows(Query query, List<?> dataRows) {

            // process selected object, issue an update query
            if (dataRows == null || dataRows.size() == 0) {
                throw new CayenneRuntimeException(
                        "Error generating PK : entity not supported: " + entityName);
            }

            if (dataRows.size() > 1) {
                throw new CayenneRuntimeException(
                        "Error generating PK : too many rows for entity: " + entityName);
            }

            DataRow lastPk = (DataRow) dataRows.get(0);
            id = (Number) lastPk.get("NEXT_ID");
        }

        public void nextCount(Query query, int resultCount) {
            if (resultCount != 1) {
                throw new CayenneRuntimeException("Error generating PK for entity '"
                        + entityName
                        + "': update count is wrong - "
                        + resultCount);
            }
        }

        public void nextBatchCount(Query query, int[] resultCount) {
        }

        public void nextGeneratedRows(Query query, ResultIterator keysIterator) {
        }

        public void nextRows(Query q, ResultIterator it) {
        }

        public void nextQueryException(Query query, Exception ex) {

            throw new CayenneRuntimeException("Error generating PK for entity '"
                    + entityName
                    + "'.", ex);
        }

        public void nextGlobalException(Exception ex) {

            throw new CayenneRuntimeException("Error generating PK for entity: "
                    + entityName, ex);
        }
    }
}
