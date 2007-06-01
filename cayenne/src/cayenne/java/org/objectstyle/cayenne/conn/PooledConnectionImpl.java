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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.DataSource;
import javax.sql.PooledConnection;

import org.apache.log4j.Logger;

/**
 * PooledConnectionImpl is an implementation of a pooling wrapper 
 * for the database connection as per JDBC3 spec. Most of the modern 
 * JDBC drivers should have its own implementation that may be 
 * used instead of this class.
 * 
 * @author Andrei Adamchik
 */
public class PooledConnectionImpl implements PooledConnection {
	private static Logger logObj = Logger.getLogger(PooledConnectionImpl.class);

	private Connection connectionObj;
	private List connectionEventListeners;
	private boolean hadErrors;
	private DataSource connectionSource;
	private String userName;
	private String password;

	protected PooledConnectionImpl() {
        // TODO: maybe remove synchronization and use
        // FastArrayList from commons-collections? After
        // all the only listener is usually pool manager.
		this.connectionEventListeners =
			Collections.synchronizedList(new ArrayList(10));
	}

	/** Creates new PooledConnection */
	public PooledConnectionImpl(
		DataSource connectionSource,
		String userName,
		String password)
		throws SQLException {

		this();

		this.connectionSource = connectionSource;
		this.userName = userName;
		this.password = password;

	}

	public void reconnect() throws SQLException {
		if (connectionObj != null) {
			try {
				connectionObj.close();
			} catch (SQLException ex) {
				// ignore exception, since connection is expected
				// to be in a bad state
			} finally {
				connectionObj = null;
			}
		}

		connectionObj =
			(userName != null)
				? connectionSource.getConnection(userName, password)
				: connectionSource.getConnection();
	}

	public void addConnectionEventListener(ConnectionEventListener listener) {
		synchronized (connectionEventListeners) {
			if (!connectionEventListeners.contains(listener))
				connectionEventListeners.add(listener);
		}
	}

	public void removeConnectionEventListener(ConnectionEventListener listener) {
		synchronized (connectionEventListeners) {
			connectionEventListeners.remove(listener);
		}
	}

	public void close() throws SQLException {

		synchronized (connectionEventListeners) {
			// remove all listeners
			connectionEventListeners.clear();
		}

		if (connectionObj != null) {
			try {
				connectionObj.close();
			} finally {
				connectionObj = null;
			}
		}
	}

	public Connection getConnection() throws SQLException {
		if (connectionObj == null) {
			reconnect();
		}

		// set autocommit to false to return connection
		// always in consistent state
		if (!connectionObj.getAutoCommit()) {

			try {
				connectionObj.setAutoCommit(true);
			} catch (SQLException sqlEx) {
				// try applying Sybase patch
				ConnectionWrapper.sybaseAutoCommitPatch(
					connectionObj,
					sqlEx,
					true);
			}
		}

		connectionObj.clearWarnings();
		return new ConnectionWrapper(connectionObj, this);
	}

	protected void returnConnectionToThePool() throws SQLException {
		// do not return to pool bad connections
		if (hadErrors)
			close();
		else
			// notify the listeners that connection is no longer used by application...
			this.connectionClosedNotification();
	}

	/** This method creates and sents an event to listeners when an error occurs in the
	 *  underlying connection. Listeners can have special logic to
	 *  analyze the error and do things like closing this PooledConnection
	 *  (if the error is fatal), etc...
	 */
	public void connectionErrorNotification(SQLException exception) {
		// hint for later to avoid returning bad connections to the pool
		hadErrors = true;

		if (logObj.isDebugEnabled()) {
			logObj.debug(
				"Child connection error, retiring pooled connection.",
				exception);
		}

		synchronized (connectionEventListeners) {
			if (connectionEventListeners.size() == 0)
				return;

			ConnectionEvent closedEvent = new ConnectionEvent(this, exception);
			Iterator listeners = connectionEventListeners.iterator();
			while (listeners.hasNext()) {
				ConnectionEventListener nextListener =
					(ConnectionEventListener) listeners.next();
				nextListener.connectionErrorOccurred(closedEvent);
			}
		}
	}

	/** Creates and sends an event to listeners when a user closes
	 *  java.sql.Connection object belonging to this PooledConnection.
	 */
	protected void connectionClosedNotification() {
		synchronized (connectionEventListeners) {
			if (connectionEventListeners.size() == 0)
				return;

			ConnectionEvent closedEvent = new ConnectionEvent(this);
			Iterator listeners = connectionEventListeners.iterator();

			while (listeners.hasNext()) {
				ConnectionEventListener nextListener =
					(ConnectionEventListener) listeners.next();
				nextListener.connectionClosed(closedEvent);
			}
		}
	}
}