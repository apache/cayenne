/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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

package org.objectstyle.cayenne.access;

import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.sql.DataSource;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.jdbc.BatchAction;
import org.objectstyle.cayenne.access.jdbc.ProcedureAction;
import org.objectstyle.cayenne.access.jdbc.RowDescriptor;
import org.objectstyle.cayenne.access.jdbc.SelectAction;
import org.objectstyle.cayenne.access.jdbc.UpdateAction;
import org.objectstyle.cayenne.access.trans.BatchQueryBuilder;
import org.objectstyle.cayenne.conn.PoolManager;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.map.AshwoodEntitySorter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.EntitySorter;
import org.objectstyle.cayenne.query.BatchQuery;
import org.objectstyle.cayenne.query.ProcedureQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * Describes a single physical data source. This can be a database server, LDAP server,
 * etc.
 * <p>
 * <i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide. </a> </i>
 * </p>
 * 
 * @author Andrus Adamchik
 */
public class DataNode implements QueryEngine {

    /**
     * @deprecated since 1.2 unused.
     */
    public static final Class DEFAULT_ADAPTER_CLASS = JdbcAdapter.class;

    protected String name;
    protected DataSource dataSource;
    protected DbAdapter adapter;
    protected String dataSourceLocation;
    protected String dataSourceFactory;
    protected EntityResolver entityResolver;
    protected EntitySorter entitySorter;
    protected Map dataMaps;

    TransactionDataSource readThroughDataSource;

    /**
     * Creates a new unnamed DataNode.
     */
    public DataNode() {
        this(null);
    }

    /**
     * Creates a new DataNode, assigning it a name.
     */
    public DataNode(String name) {
        this.name = name;
        this.dataMaps = new HashMap();
        this.readThroughDataSource = new TransactionDataSource();

        // since 1.2 we always implement entity sorting, regardless of the underlying DB
        // as the right order is needed for deferred PK propagation (and maybe other
        // things too?)
        this.entitySorter = new AshwoodEntitySorter(Collections.EMPTY_LIST);
    }

    /**
     * Returns node name. Name is used to uniquely identify DataNode within a DataDomain.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns a location of DataSource of this node. Depending on how this node was
     * created, location is either a JNDI name, or a location of node XML file, etc.
     */
    public String getDataSourceLocation() {
        return dataSourceLocation;
    }

    public void setDataSourceLocation(String dataSourceLocation) {
        this.dataSourceLocation = dataSourceLocation;
    }

    /**
     * Returns a name of DataSourceFactory class for this node.
     */
    public String getDataSourceFactory() {
        return dataSourceFactory;
    }

    public void setDataSourceFactory(String dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }

    /**
     * Returns an unmodifiable collection of DataMaps handled by this DataNode.
     */
    public Collection getDataMaps() {
        return Collections.unmodifiableCollection(dataMaps.values());
    }

    public void setDataMaps(Collection dataMaps) {
        Iterator it = dataMaps.iterator();
        while (it.hasNext()) {
            DataMap map = (DataMap) it.next();
            this.dataMaps.put(map.getName(), map);
        }

        entitySorter.setDataMaps(dataMaps);
    }

    /**
     * Adds a DataMap to be handled by this node.
     */
    public void addDataMap(DataMap map) {
        this.dataMaps.put(map.getName(), map);

        entitySorter.setDataMaps(getDataMaps());
    }

    public void removeDataMap(String mapName) {
        DataMap map = (DataMap) dataMaps.remove(mapName);
        if (map != null) {
            entitySorter.setDataMaps(getDataMaps());
        }
    }

    /**
     * Returns DataSource used by this DataNode to obtain connections.
     */
    public DataSource getDataSource() {
        return dataSource != null ? readThroughDataSource : null;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Returns DbAdapter object. This is a plugin that handles RDBMS vendor-specific
     * features.
     */
    public DbAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(DbAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Returns a DataNode that should hanlde queries for all DataMap components.
     * 
     * @since 1.1
     */
    public DataNode lookupDataNode(DataMap dataMap) {
        // we don't know any better than to return ourselves...
        return this;
    }

    /**
     * @deprecated since 1.2 as the corresponding interface method is deprecated.
     */
    public void performQueries(
            Collection queries,
            OperationObserver observer,
            Transaction transaction) {

        Transaction old = Transaction.getThreadTransaction();
        Transaction.bindThreadTransaction(transaction);

        try {
            performQueries(queries, observer);
        }
        finally {
            Transaction.bindThreadTransaction(old);
        }
    }

    /**
     * Runs queries using Connection obtained from internal DataSource.
     * 
     * @since 1.1
     */
    public void performQueries(Collection queries, OperationObserver callback) {

        int listSize = queries.size();
        if (listSize == 0) {
            return;
        }

        if (callback.isIteratedResult() && listSize > 1) {
            throw new CayenneRuntimeException(
                    "Iterated queries are not allowed in a batch. Batch size: "
                            + listSize);
        }

        QueryLogger.logQueryStart(listSize);

        // do this meaningless inexpensive operation to trigger AutoAdapter lazy
        // initialization before opening a connection. Otherwise we may end up with two
        // connections open simultaneously, possibly hitting connection pool upper limit.
        getAdapter().getExtendedTypes();

        Connection connection = null;

        try {
            connection = this.getDataSource().getConnection();
        }
        catch (Exception globalEx) {
            QueryLogger.logQueryError(globalEx);

            Transaction transaction = Transaction.getThreadTransaction();
            if (transaction != null) {
                transaction.setRollbackOnly();
            }

            callback.nextGlobalException(globalEx);
            return;
        }

        try {
            DataNodeQueryAction queryRunner = new DataNodeQueryAction(this, callback);
            Iterator it = queries.iterator();
            while (it.hasNext()) {
                Query nextQuery = (Query) it.next();

                // catch exceptions for each individual query
                try {
                    queryRunner.runQuery(connection, nextQuery);
                }
                catch (Exception queryEx) {
                    QueryLogger.logQueryError(queryEx);

                    // notify consumer of the exception,
                    // stop running further queries
                    callback.nextQueryException(nextQuery, queryEx);

                    Transaction transaction = Transaction.getThreadTransaction();
                    if (transaction != null) {
                        transaction.setRollbackOnly();
                    }
                    break;
                }
            }
        }
        finally {
            try {
                connection.close();
            }
            catch (SQLException e) {
                // ignore closing exceptions...
            }
        }
    }

    /**
     * Executes a SelectQuery.
     * 
     * @deprecated since 1.2
     */
    protected void runSelect(
            Connection connection,
            Query query,
            OperationObserver observer) throws SQLException, Exception {

        new SelectAction((SelectQuery) query, getAdapter(), getEntityResolver())
                .performAction(connection, observer);
    }

    /**
     * Executes a non-batched updating query.
     * 
     * @deprecated since 1.2
     */
    protected void runUpdate(Connection con, Query query, OperationObserver delegate)
            throws SQLException, Exception {

        new UpdateAction(query, getAdapter(), getEntityResolver()).performAction(
                con,
                delegate);
    }

    /**
     * Executes a batch updating query.
     * 
     * @deprecated since 1.2
     */
    protected void runBatchUpdate(
            Connection connection,
            BatchQuery query,
            OperationObserver observer) throws SQLException, Exception {

        // check run strategy...

        // optimistic locking is not supported in batches due to JDBC driver limitations
        boolean useOptimisticLock = query.isUsingOptimisticLocking();

        boolean runningAsBatch = !useOptimisticLock && adapter.supportsBatchUpdates();
        BatchAction action = new BatchAction(query, getAdapter(), getEntityResolver());
        action.setBatch(runningAsBatch);
        action.performAction(connection, observer);
    }

    /**
     * Executes batch query using JDBC Statement batching features.
     * 
     * @deprecated since 1.2 SQLActions are used.
     */
    protected void runBatchUpdateAsBatch(
            Connection con,
            BatchQuery query,
            BatchQueryBuilder queryBuilder,
            OperationObserver delegate) throws SQLException, Exception {
        new TempBatchAction(query, true).runAsBatch(con, queryBuilder, delegate);
    }

    /**
     * Executes batch query without using JDBC Statement batching features, running
     * individual statements in the batch one by one.
     * 
     * @deprecated since 1.2 SQLActions are used.
     */
    protected void runBatchUpdateAsIndividualQueries(
            Connection con,
            BatchQuery query,
            BatchQueryBuilder queryBuilder,
            OperationObserver delegate) throws SQLException, Exception {

        new TempBatchAction(query, false).runAsBatch(con, queryBuilder, delegate);
    }

    /**
     * @deprecated since 1.2
     */
    protected void runStoredProcedure(
            Connection con,
            Query query,
            OperationObserver delegate) throws SQLException, Exception {

        new ProcedureAction((ProcedureQuery) query, getAdapter(), getEntityResolver())
                .performAction(con, delegate);
    }

    /**
     * Helper method that reads OUT parameters of a CallableStatement.
     * 
     * @deprecated Since 1.2 this logic is moved to SQLAction.
     */
    protected void readStoredProcedureOutParameters(
            CallableStatement statement,
            org.objectstyle.cayenne.access.util.ResultDescriptor descriptor,
            Query query,
            OperationObserver delegate) throws SQLException, Exception {

        // method is deprecated, so keep this ugly piece here as a placeholder
        new TempProcedureAction((ProcedureQuery) query).readProcedureOutParameters(
                statement,
                delegate);
    }

    /**
     * Helper method that reads a ResultSet.
     * 
     * @deprecated Since 1.2 this logic is moved to SQLAction.
     */
    protected void readResultSet(
            ResultSet resultSet,
            org.objectstyle.cayenne.access.util.ResultDescriptor descriptor,
            org.objectstyle.cayenne.query.GenericSelectQuery query,
            OperationObserver delegate) throws SQLException, Exception {

        // method is deprecated, so keep this ugly piece here as a placeholder
        RowDescriptor rowDescriptor = new RowDescriptor(resultSet, getAdapter()
                .getExtendedTypes());
        new TempProcedureAction((ProcedureQuery) query).readResultSet(
                resultSet,
                rowDescriptor,
                query,
                delegate);
    }

    /**
     * Returns EntityResolver that handles DataMaps of this node.
     */
    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    /**
     * Sets EntityResolver. DataNode relies on externally set EntityResolver, so if the
     * node is created outside of DataDomain stack, a valid EntityResolver must be
     * provided explicitly.
     * 
     * @since 1.1
     */
    public void setEntityResolver(
            org.objectstyle.cayenne.map.EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    /**
     * Returns EntitySorter used by the DataNode.
     */
    public EntitySorter getEntitySorter() {
        return entitySorter;
    }

    /**
     * Sets an EntitySorter that is used to order objects on commit.
     * 
     * @since 1.2
     */
    public void setEntitySorter(EntitySorter entitySorter) {
        this.entitySorter = entitySorter;
    }

    /**
     * Tries to close JDBC connections opened by this node's data source.
     */
    public synchronized void shutdown() {
        try {
            // TODO: theoretically someone maybe using our PoolManager as a container
            // mapped DataSource, so we should use some other logic to determine whether
            // this is a DataNode-managed DS.
            if (dataSource instanceof PoolManager) {
                ((PoolManager) dataSource).dispose();
                dataSource = null;
            }
        }
        catch (SQLException ex) {
        }
    }

    // this class exists to provide deprecated DataNode methods with access to
    // various SQLAction implementations. It will be removed once corresponding
    // DataNode methods are removed
    final class TempProcedureAction extends ProcedureAction {

        public TempProcedureAction(ProcedureQuery query) {
            super(query, DataNode.this.adapter, DataNode.this.entityResolver);
        }

        // changing access to public
        public void readProcedureOutParameters(
                CallableStatement statement,
                OperationObserver delegate) throws SQLException, Exception {
            super.readProcedureOutParameters(statement, delegate);
        }

        // changing access to public
        public void readResultSet(
                ResultSet resultSet,
                RowDescriptor descriptor,
                Query query,
                OperationObserver delegate) throws SQLException, Exception {
            super.readResultSet(resultSet, descriptor, query, delegate);
        }
    }

    // this class exists to provide deprecated DataNode methods with access to
    // various SQLAction implementations. It will be removed once corresponding
    // DataNode methods are removed
    final class TempBatchAction extends BatchAction {

        public TempBatchAction(BatchQuery batchQuery, boolean runningAsBatch) {
            super(batchQuery, DataNode.this.adapter, DataNode.this.entityResolver);
            setBatch(runningAsBatch);
        }

        // making public to access from DataNode
        protected void runAsBatch(
                Connection con,
                BatchQueryBuilder queryBuilder,
                OperationObserver delegate) throws SQLException, Exception {
            super.runAsBatch(con, queryBuilder, delegate);
        }

        // making public to access from DataNode
        public void runAsIndividualQueries(
                Connection con,
                BatchQueryBuilder queryBuilder,
                OperationObserver delegate) throws SQLException, Exception {
            super.runAsIndividualQueries(con, queryBuilder, delegate, false);
        }
    }

    // a read-through DataSource that ensures returning the same connection within
    // transaction.
    final class TransactionDataSource implements DataSource {

        final String CONNECTION_RESOURCE_PREFIX = "DataNode.Connection.";

        public Connection getConnection() throws SQLException {
            Transaction t = Transaction.getThreadTransaction();
            if (t != null) {
                String key = CONNECTION_RESOURCE_PREFIX + name;
                Connection c = t.getConnection(key);

                if (c == null || c.isClosed()) {
                    c = dataSource.getConnection();
                    t.addConnection(key, c);
                }

                // wrap transaction-attached connections in a decorator that prevents them
                // from being closed by callers, as transaction should take care of them
                // on commit or rollback.
                return new TransactionConnectionDecorator(c);
            }

            return dataSource.getConnection();
        }

        public Connection getConnection(String username, String password)
                throws SQLException {

            Transaction t = Transaction.getThreadTransaction();
            if (t != null) {
                String key = CONNECTION_RESOURCE_PREFIX + name;
                Connection c = t.getConnection(key);

                if (c == null || c.isClosed()) {
                    c = dataSource.getConnection();
                    t.addConnection(key, c);
                }

                // wrap transaction-attached connections in a decorator that prevents them
                // from being closed by callers, as transaction should take care of them
                // on commit or rollback.
                return new TransactionConnectionDecorator(c);
            }

            return dataSource.getConnection(username, password);
        }

        public int getLoginTimeout() throws SQLException {
            return dataSource.getLoginTimeout();
        }

        public PrintWriter getLogWriter() throws SQLException {
            return dataSource.getLogWriter();
        }

        public void setLoginTimeout(int seconds) throws SQLException {
            dataSource.setLoginTimeout(seconds);
        }

        public void setLogWriter(PrintWriter out) throws SQLException {
            dataSource.setLogWriter(out);
        }
    }
}