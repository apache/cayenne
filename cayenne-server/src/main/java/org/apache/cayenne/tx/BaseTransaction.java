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

package org.apache.cayenne.tx;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A Cayenne transaction. Currently supports managing JDBC connections.
 * 
 * @since 4.0
 */
public abstract class BaseTransaction implements Transaction {

	/**
	 * A ThreadLocal that stores current thread transaction.
	 */
	static final ThreadLocal<Transaction> currentTransaction = new InheritableThreadLocal<Transaction>();

	protected static final int STATUS_ACTIVE = 1;
	protected static final int STATUS_COMMITTING = 2;
	protected static final int STATUS_COMMITTED = 3;
	protected static final int STATUS_ROLLEDBACK = 4;
	protected static final int STATUS_ROLLING_BACK = 5;
	protected static final int STATUS_NO_TRANSACTION = 6;
	protected static final int STATUS_MARKED_ROLLEDBACK = 7;

	protected Map<String, Connection> connections;
	protected int status;

	static String decodeStatus(int status) {
		switch (status) {
		case STATUS_ACTIVE:
			return "STATUS_ACTIVE";
		case STATUS_COMMITTING:
			return "STATUS_COMMITTING";
		case STATUS_COMMITTED:
			return "STATUS_COMMITTED";
		case STATUS_ROLLEDBACK:
			return "STATUS_ROLLEDBACK";
		case STATUS_ROLLING_BACK:
			return "STATUS_ROLLING_BACK";
		case STATUS_NO_TRANSACTION:
			return "STATUS_NO_TRANSACTION";
		case STATUS_MARKED_ROLLEDBACK:
			return "STATUS_MARKED_ROLLEDBACK";
		default:
			return "Unknown Status - " + status;
		}
	}

	/**
	 * Binds a Transaction to the current thread.
	 */
	public static void bindThreadTransaction(Transaction transaction) {
		currentTransaction.set(transaction);
	}

	/**
	 * Returns a Transaction associated with the current thread, or null if
	 * there is no such Transaction.
	 */
	public static Transaction getThreadTransaction() {
		return currentTransaction.get();
	}

	/**
	 * Creates new inactive transaction.
	 */
	protected BaseTransaction() {
		this.status = STATUS_NO_TRANSACTION;
	}

	@Override
	public void setRollbackOnly() {
		this.status = STATUS_MARKED_ROLLEDBACK;
	}

	@Override
	public boolean isRollbackOnly() {
		return status == STATUS_MARKED_ROLLEDBACK;
	}

	/**
	 * Starts a Transaction. If Transaction is not started explicitly, it will
	 * be started when the first connection is added.
	 */
	@Override
	public void begin() {
		if (status != BaseTransaction.STATUS_NO_TRANSACTION) {
			throw new IllegalStateException("Transaction must have 'STATUS_NO_TRANSACTION' to begin. "
					+ "Current status: " + BaseTransaction.decodeStatus(status));
		}

		status = BaseTransaction.STATUS_ACTIVE;
	}

	@Override
	public void commit() {

		if (status == BaseTransaction.STATUS_NO_TRANSACTION) {
			return;
		}

		if (status != BaseTransaction.STATUS_ACTIVE) {
			throw new IllegalStateException("Transaction must have 'STATUS_ACTIVE' to be committed. "
					+ "Current status: " + BaseTransaction.decodeStatus(status));
		}

		processCommit();

		status = BaseTransaction.STATUS_COMMITTED;

		close();
	}

	protected abstract void processCommit();

	@Override
	public void rollback() {

		try {
			if (status == BaseTransaction.STATUS_NO_TRANSACTION || status == BaseTransaction.STATUS_ROLLEDBACK
					|| status == BaseTransaction.STATUS_ROLLING_BACK) {
				return;
			}

			if (status != BaseTransaction.STATUS_ACTIVE && status != BaseTransaction.STATUS_MARKED_ROLLEDBACK) {
				throw new IllegalStateException(
						"Transaction must have 'STATUS_ACTIVE' or 'STATUS_MARKED_ROLLEDBACK' to be rolled back. "
								+ "Current status: " + BaseTransaction.decodeStatus(status));
			}

			processRollback();

			status = BaseTransaction.STATUS_ROLLEDBACK;

		} finally {
			close();
		}
	}

	protected abstract void processRollback();

	@Override
	public Connection getConnection(String name) {
		return (connections != null) ? connections.get(name) : null;
	}

	@Override
	public void addConnection(String name, Connection connection) {

		if (connections == null) {
			connections = new HashMap<>();
		}

		if (connections.put(name, connection) != connection) {
			connectionAdded(connection);
		}
	}

	protected void connectionAdded(Connection connection) {

		// implicitly begin transaction
		if (status == BaseTransaction.STATUS_NO_TRANSACTION) {
			begin();
		}

		if (status != BaseTransaction.STATUS_ACTIVE) {
			throw new IllegalStateException("Transaction must have 'STATUS_ACTIVE' to add a connection. "
					+ "Current status: " + BaseTransaction.decodeStatus(status));
		}
	}

	/**
	 * Closes all connections associated with transaction.
	 */
	protected void close() {
		if (connections == null || connections.isEmpty()) {
			return;
		}

		Iterator<?> it = connections.values().iterator();
		while (it.hasNext()) {
			try {

				((Connection) it.next()).close();
			} catch (Throwable th) {
				// TODO: chain exceptions...
				// ignore for now
			}
		}
	}
}
