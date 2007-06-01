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
package org.objectstyle.cayenne.conn;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Map;

/**
 * ConnectionWrapper is a <code>java.sql.Connection</code> implementation that wraps another
 * Connection, delegating method calls to this connection. It 
 * works in conjunction with PooledConnectionImpl, to generate pool events, provide
 * limited automated reconnection functionality, etc.
 * 
 * @author Andrei Adamchik
 */
public class ConnectionWrapper implements Connection {
    private Connection connectionObj;
    private PooledConnectionImpl pooledConnection;
    private long lastReconnected;
    private int reconnectCount;

    /** 
     * Fixes Sybase problems with autocommit. Used idea from
     * Jonas org.objectweb.jonas.jdbc_xa.ConnectionImpl 
     * (http://www.objectweb.org/jonas/).
     * 
     * <p>If problem is not the one that can be fixed by this patch,
     * original exception is rethrown. If exception occurs when fixing 
     * the problem, new exception is thrown.</p>
     */
    static void sybaseAutoCommitPatch(
        Connection c,
        SQLException e,
        boolean autoCommit)
        throws SQLException {

        String s = e.getMessage().toLowerCase();
        if (s.indexOf("set chained command not allowed") >= 0) {
            c.commit();
            c.setAutoCommit(autoCommit); // Shouldn't fail now.
        } else {
            throw e;
        }
    }

    /** Creates new ConnectionWrapper */
    public ConnectionWrapper(
        Connection connectionObj,
        PooledConnectionImpl pooledConnection) {
        this.connectionObj = connectionObj;
        this.pooledConnection = pooledConnection;
    }

    protected void reconnect(SQLException exception) throws SQLException {

        // if there was a relatively recent reconnect, just rethrow an error
        // and retire itself. THIS WILL PREVENT RECONNECT LOOPS
        if (reconnectCount > 0
            && System.currentTimeMillis() - lastReconnected < 60000) {

            retire(exception);
            throw exception;
        }

        pooledConnection.reconnect();
        
		// Pooled connection will wrap returned connection into
		// another ConnectionWrapper.... lets get the real connection 
		// underneath...
		Connection connection = pooledConnection.getConnection();
		if (connection instanceof ConnectionWrapper) {
			this.connectionObj = ((ConnectionWrapper) connection).connectionObj;
		}
		else {
			this.connectionObj = connection;
		}
		
        lastReconnected = System.currentTimeMillis();
        reconnectCount++;
    }

    protected void retire(SQLException exception) {
        // notify all the listeners....
        pooledConnection.connectionErrorNotification(exception);
    }

    public void clearWarnings() throws SQLException {
        try {
            connectionObj.clearWarnings();
        } catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public void close() throws SQLException {
        pooledConnection.returnConnectionToThePool();
        connectionObj = null;
        pooledConnection = null;
    }

    public void commit() throws SQLException {
        try {
            connectionObj.commit();
        } catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public Statement createStatement() throws SQLException {
        try {
            return connectionObj.createStatement();
        } catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return createStatement();
        }
    }

    public Statement createStatement(
        int resultSetType,
        int resultSetConcurrency)
        throws SQLException {
        try {
            return connectionObj.createStatement(
                resultSetType,
                resultSetConcurrency);
        } catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return createStatement(resultSetType, resultSetConcurrency);
        }
    }

    public boolean getAutoCommit() throws SQLException {
        try {
            return connectionObj.getAutoCommit();
        } catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public String getCatalog() throws SQLException {
        try {
            return connectionObj.getCatalog();
        } catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        try {
            return connectionObj.getMetaData();
        } catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public int getTransactionIsolation() throws SQLException {
        try {
            return connectionObj.getTransactionIsolation();
        } catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public SQLWarning getWarnings() throws SQLException {
        try {
            return connectionObj.getWarnings();
        } catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public boolean isClosed() throws SQLException {
        if (connectionObj != null) {
            try {
                return connectionObj.isClosed();
            } catch (SQLException sqlEx) {
                retire(sqlEx);
                throw sqlEx;
            }
        } else
            return true;
    }

    public boolean isReadOnly() throws SQLException {
        try {
            return connectionObj.isReadOnly();
        } catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public String nativeSQL(String sql) throws SQLException {
        try {
            return connectionObj.nativeSQL(sql);
        } catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        try {
            return connectionObj.prepareCall(sql);
        } catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return prepareCall(sql);
        }
    }

    public CallableStatement prepareCall(
        String sql,
        int resultSetType,
        int resultSetConcurrency)
        throws SQLException {
        try {
            return connectionObj.prepareCall(
                sql,
                resultSetType,
                resultSetConcurrency);
        } catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return prepareCall(sql, resultSetType, resultSetConcurrency);
        }
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        try {
            return connectionObj.prepareStatement(sql);
        } catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return prepareStatement(sql);
        }
    }

    public PreparedStatement prepareStatement(
        String sql,
        int resultSetType,
        int resultSetConcurrency)
        throws SQLException {
        try {
            return connectionObj.prepareStatement(
                sql,
                resultSetType,
                resultSetConcurrency);
        } catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return prepareStatement(sql, resultSetType, resultSetConcurrency);
        }
    }

    public void rollback() throws SQLException {
        try {
            connectionObj.rollback();
        } catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        try {
            connectionObj.setAutoCommit(autoCommit);
        } catch (SQLException sqlEx) {

            try {
                // apply Sybase patch
                sybaseAutoCommitPatch(connectionObj, sqlEx, autoCommit);
            } catch (SQLException patchEx) {
                retire(sqlEx);
                throw sqlEx;
            }
        }
    }

    public void setCatalog(String catalog) throws SQLException {
        try {
            connectionObj.setCatalog(catalog);
        } catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        try {
            connectionObj.setReadOnly(readOnly);
        } catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public void setTransactionIsolation(int level) throws SQLException {
        try {
            connectionObj.setTransactionIsolation(level);
        } catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public Map getTypeMap() throws SQLException {
        try {
            return connectionObj.getTypeMap();
        } catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public void setTypeMap(Map map) throws SQLException {
        try {
            connectionObj.setTypeMap(map);
        } catch (SQLException sqlEx) {
            retire(sqlEx);
            throw sqlEx;
        }
    }

    public void setHoldability(int holdability) throws SQLException {
        throw new java.lang.UnsupportedOperationException(
            "Method setHoldability() not yet implemented.");
    }

    public int getHoldability() throws SQLException {
        throw new java.lang.UnsupportedOperationException(
            "Method getHoldability() not yet implemented.");
    }

    public Savepoint setSavepoint() throws SQLException {
        throw new java.lang.UnsupportedOperationException(
            "Method setSavepoint() not yet implemented.");
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        throw new java.lang.UnsupportedOperationException(
            "Method setSavepoint() not yet implemented.");
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        throw new java.lang.UnsupportedOperationException(
            "Method rollback() not yet implemented.");
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        throw new java.lang.UnsupportedOperationException(
            "Method releaseSavepoint() not yet implemented.");
    }

    public Statement createStatement(
        int resultSetType,
        int resultSetConcurrency,
        int resultSetHoldability)
        throws SQLException {
        throw new java.lang.UnsupportedOperationException(
            "Method createStatement() not yet implemented.");
    }

    public PreparedStatement prepareStatement(
        String sql,
        int resultSetType,
        int resultSetConcurrency,
        int resultSetHoldability)
        throws SQLException {
        throw new java.lang.UnsupportedOperationException(
            "Method prepareStatement() not yet implemented.");
    }

    public CallableStatement prepareCall(
            String sql,
            int resultSetType,
            int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        try {
            return connectionObj.prepareCall(
                    sql,
                    resultSetType,
                    resultSetConcurrency,
                    resultSetHoldability);
        }
        catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return prepareCall(
                    sql,
                    resultSetType,
                    resultSetConcurrency,
                    resultSetHoldability);
        }
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
            throws SQLException {
        
        try {
            return connectionObj.prepareStatement(sql, autoGeneratedKeys);
        }
        catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return prepareStatement(sql, autoGeneratedKeys);
        }
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
            throws SQLException {
        try {
            return connectionObj.prepareStatement(sql, columnIndexes);
        }
        catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return prepareStatement(sql, columnIndexes);
        }
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames)
            throws SQLException {
        try {
            return connectionObj.prepareStatement(sql, columnNames);
        }
        catch (SQLException sqlEx) {

            // reconnect has code to prevent loops
            reconnect(sqlEx);
            return prepareStatement(sql, columnNames);
        }
    }

}