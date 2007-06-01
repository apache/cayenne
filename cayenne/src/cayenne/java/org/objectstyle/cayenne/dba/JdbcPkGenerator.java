/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.dba;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.access.util.DefaultOperationObserver;
import org.objectstyle.cayenne.access.util.SelectObserver;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbKeyGenerator;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SQLTemplate;
import org.objectstyle.cayenne.query.SqlModifyQuery;
import org.objectstyle.cayenne.query.SqlSelectQuery;
import org.objectstyle.cayenne.util.IDUtil;

/**
 * Default primary key generator implementation. Uses a lookup table named
 * "AUTO_PK_SUPPORT" to search and increment primary keys for tables.
 *
 * @author Andrei Adamchik
 */
public class JdbcPkGenerator implements PkGenerator {
    public static final int DEFAULT_PK_CACHE_SIZE = 20;

    protected static final String NEXT_ID = "NEXT_ID";
    protected static final ObjAttribute[] objDesc =
        new ObjAttribute[] { new ObjAttribute("nextId", Integer.class.getName(), null)};
    protected static final DbAttribute[] resultDesc =
        new DbAttribute[] { new DbAttribute(NEXT_ID, Types.INTEGER, null)};

    protected Map pkCache = new HashMap();
    protected int pkCacheSize = DEFAULT_PK_CACHE_SIZE;

    static {
        objDesc[0].setDbAttributePath(NEXT_ID);
    }

    public void createAutoPk(DataNode node, List dbEntities) throws Exception {
        // check if a table exists

        // create AUTO_PK_SUPPORT table
        if (!autoPkTableExists(node)) {
            runUpdate(node, pkTableCreateString());
        }

        // delete any existing pk entries
        runUpdate(node, pkDeleteString(dbEntities));

        // insert all needed entries
        Iterator it = dbEntities.iterator();
        while (it.hasNext()) {
            DbEntity ent = (DbEntity) it.next();
            runUpdate(node, pkCreateString(ent.getName()));
        }
    }

    public List createAutoPkStatements(List dbEntities) {
        List list = new ArrayList();

        list.add(pkTableCreateString());
        list.add(pkDeleteString(dbEntities));

        Iterator it = dbEntities.iterator();
        while (it.hasNext()) {
            DbEntity ent = (DbEntity) it.next();
            list.add(pkCreateString(ent.getName()));
        }

        return list;
    }

    /**
     * Drops table named "AUTO_PK_SUPPORT" if it exists in the
     * database.
     */
    public void dropAutoPk(DataNode node, List dbEntities) throws Exception {
        if (autoPkTableExists(node)) {
            runUpdate(node, dropAutoPkString());
        }
    }

    public List dropAutoPkStatements(List dbEntities) {
        List list = new ArrayList();
        list.add(dropAutoPkString());
        return list;
    }

    protected String pkTableCreateString() {
        StringBuffer buf = new StringBuffer();
        buf
            .append("CREATE TABLE AUTO_PK_SUPPORT (")
            .append("  TABLE_NAME CHAR(100) NOT NULL,")
            .append("  NEXT_ID INTEGER NOT NULL")
            .append(")");

        return buf.toString();
    }

    protected String pkDeleteString(List dbEntities) {
        StringBuffer buf = new StringBuffer();
        buf.append("DELETE FROM AUTO_PK_SUPPORT WHERE TABLE_NAME IN (");
        int len = dbEntities.size();
        for (int i = 0; i < len; i++) {
            if (i > 0) {
                buf.append(", ");
            }
            DbEntity ent = (DbEntity) dbEntities.get(i);
            buf.append('\'').append(ent.getName()).append('\'');
        }
        buf.append(')');
        return buf.toString();
    }

    protected String pkCreateString(String entName) {
        StringBuffer buf = new StringBuffer();
        buf
            .append("INSERT INTO AUTO_PK_SUPPORT")
            .append(" (TABLE_NAME, NEXT_ID)")
            .append(" VALUES ('")
            .append(entName)
            .append("', 200)");
        return buf.toString();
    }

    protected String pkSelectString(String entName) {
        StringBuffer buf = new StringBuffer();
        buf
            .append("SELECT NEXT_ID FROM AUTO_PK_SUPPORT WHERE TABLE_NAME = '")
            .append(entName)
            .append('\'');
        return buf.toString();
    }

    protected String pkUpdateString(String entName) {
        StringBuffer buf = new StringBuffer();
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
            } finally {
                tables.close();
            }
        } finally {
            // return connection to the pool
            con.close();
        }

        return exists;
    }

    /**
     * Runs JDBC update over a Connection obtained from DataNode.
     * Returns a number of objects returned from update.
     *
     * @throws SQLException in case of query failure.
     */
    public int runUpdate(DataNode node, String sql) throws SQLException {
        QueryLogger.logQuery(QueryLogger.DEFAULT_LOG_LEVEL, sql, Collections.EMPTY_LIST);

        Connection con = node.getDataSource().getConnection();
        try {
            Statement upd = con.createStatement();
            try {
                return upd.executeUpdate(sql);
            } finally {
                upd.close();
            }
        } finally {
            con.close();
        }
    }

    /**
     * Runs arbitrary SQL over the DataNode.
     *
     * @deprecated Unused since 1.1 
     */
    protected List runSelect(DataNode node, String sql) throws Exception {
        SQLTemplate q = new SQLTemplate(true);
        q.setDefaultTemplate(sql);

        SelectObserver observer = new SelectObserver();
        node.performQueries(Collections.singletonList(q), observer);
        return observer.getResults(q);
    }

    public String generatePkForDbEntityString(DbEntity ent) {
        StringBuffer buf = new StringBuffer();
        buf.append(pkSelectString(ent.getName())).append('\n').append(
            pkUpdateString(ent.getName()));
        return buf.toString();
    }

    /**
     * <p>Generates new (unique and non-repeating) primary key for specified
     * dbEntity.</p>
     *
     * <p>This implementation is naive since it does not lock the database rows
     * when executing select and subsequent update. Adapter-specific implementations 
     * are more robust.</p>
     */

    public Object generatePkForDbEntity(DataNode node, DbEntity ent) throws Exception {

        // check for binary pk
        Object binPK = binaryPK(ent);
        if (binPK != null) {
            return binPK;
        }

        DbKeyGenerator pkGenerator = ent.getPrimaryKeyGenerator();
        int cacheSize;
        if (pkGenerator != null && pkGenerator.getKeyCacheSize() != null)
            cacheSize = pkGenerator.getKeyCacheSize().intValue();
        else
            cacheSize = pkCacheSize;

        // if no caching, always generate fresh
        if (cacheSize <= 1) {
            return new Integer(pkFromDatabase(node, ent));
        }

        synchronized (pkCache) {
            PkRange r = (PkRange) pkCache.get(ent.getName());

            if (r == null) {
                // created exhaused PkRange
                r = new PkRange(1, 0);
                pkCache.put(ent.getName(), r);
            }

            if (r.isExhausted()) {
                int val = pkFromDatabase(node, ent);
                r.reset(val, val + cacheSize - 1);
            }

            return r.getNextPrimaryKey();
        }
    }

    /**
     * @return a binary PK if DbEntity has a BINARY or VARBINARY pk, null otherwise.
     * This method will likely be deprecated in 1.1 in favor of a more generic soultion.
     * @since 1.0.2
     */
    protected byte[] binaryPK(DbEntity entity) {
        List pkColumns = entity.getPrimaryKey();
        if (pkColumns.size() == 1) {
            DbAttribute pk = (DbAttribute) pkColumns.get(0);
            if (pk.getMaxLength() > 0
                && (pk.getType() == Types.BINARY || pk.getType() == Types.VARBINARY)) {
                return IDUtil.pseudoUniqueByteSequence(pk.getMaxLength());
            }
        }

        return null;
    }

    /**
     * Performs primary key generation ignoring cache. Generates
     * a range of primary keys as specified by
     * "pkCacheSize" bean property.
     *
     * <p>This method is called internally from "generatePkForDbEntity"
     * and then generated range of key values is saved in cache for
     * performance. Subclasses that implement different primary key
     * generation solutions should override this method,
     * not "generatePkForDbEntity".</p>
     */
    protected int pkFromDatabase(DataNode node, DbEntity ent) throws Exception {

        // run queries via DataNode to utilize its transactional behavior
        List queries = new ArrayList(2);

        // 1. prepare select
        SqlSelectQuery sel = new SqlSelectQuery(ent, pkSelectString(ent.getName()));
        sel.setObjDescriptors(objDesc);
        sel.setResultDescriptors(resultDesc);
        queries.add(sel);

        // 2. prepare update
        queries.add(new SqlModifyQuery(ent, pkUpdateString(ent.getName())));

        PkRetrieveProcessor observer = new PkRetrieveProcessor(ent.getName());
        node.performQueries(queries, observer);
        return observer.getNextId();
    }

    /**
     * Returns a size of the entity primary key cache.
     * Default value is 20. If cache size is set to a value
     * less or equals than "one", no primary key caching is done.
     */
    public int getPkCacheSize() {
        return pkCacheSize;
    }

    /**
     * Sets the size of the entity primary key cache.
     * If <code>pkCacheSize</code> parameter is less than 1,
     * cache size is set to "one".
     *
     * <p><i>Note that our tests show that setting primary key
     * cache value to anything much bigger than 20 does not give
     * any significant performance increase. Therefore it does
     * not make sense to use bigger values, since this may
     * potentially create big gaps in the database primary
     * key sequences in cases like application crashes or restarts.
     * </i></p>
     */
    public void setPkCacheSize(int pkCacheSize) {
        this.pkCacheSize = (pkCacheSize < 1) ? 1 : pkCacheSize;
    }

    public void reset() {
        pkCache.clear();
    }

    /** OperationObserver for primary key retrieval. */
    protected class PkRetrieveProcessor extends DefaultOperationObserver {
        protected boolean success;
        protected Integer nextId;
        protected String entName;

        public PkRetrieveProcessor(String entName) {
            this.entName = entName;
        }

        /**
         * @deprecated Since 1.1 this method is no longer used by Cayenne.
         */
        public boolean useAutoCommit() {
            return false;
        }

        public int getNextId() {
            if (nextId != null) {
                return nextId.intValue();
            } else {
                throw new CayenneRuntimeException("No key was retrieved.");
            }
        }

        public void nextDataRows(Query query, List dataRows) {
            super.nextDataRows(query, dataRows);

            // process selected object, issue an update query
            if (dataRows == null || dataRows.size() == 0) {
                throw new CayenneRuntimeException(
                    "Error generating PK : entity not supported: " + entName);
            }
            if (dataRows.size() > 1) {
                throw new CayenneRuntimeException(
                    "Error generating PK : too many rows for entity: " + entName);
            }

            Map lastPk = (Map) dataRows.get(0);
            nextId = (Integer) lastPk.get(NEXT_ID);
            if (nextId == null) {
                throw new CayenneRuntimeException("Error generating PK : null nextId.");
            }
        }

        public void nextCount(Query query, int resultCount) {
            super.nextCount(query, resultCount);

            if (resultCount != 1)
                throw new CayenneRuntimeException(
                    "Error generating PK : update count is wrong: " + resultCount);
        }

        public void nextQueryException(Query query, Exception ex) {
            super.nextQueryException(query, ex);
            String entityName =
                ((query != null) && (query.getRoot() != null))
                    ? query.getRoot().toString()
                    : null;
            throw new CayenneRuntimeException(
                "Error generating PK for entity '" + entityName + "'.",
                ex);
        }

        public void nextGlobalException(Exception ex) {
            super.nextGlobalException(ex);
            throw new CayenneRuntimeException("Error generating PK.", ex);
        }
    }
}