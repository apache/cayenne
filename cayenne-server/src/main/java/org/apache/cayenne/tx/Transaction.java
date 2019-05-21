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
package org.apache.cayenne.tx;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Cayenne Transaction interface.
 *
 * @since 4.0
 */
public interface Transaction {

    /**
     * Starts a Transaction. If Transaction is not started explicitly, it will be started when the first connection is
     * added.
     */
    void begin();

    void commit();

    void rollback();

    void setRollbackOnly();

    boolean isRollbackOnly();

    /**
     * Retrieves a connection for the given symbolic name. If it does not exists, creates a new connection using
     * provided DataSource, and registers it internally.
     *
     * @param connectionName a symbolic name of the connection. Cayenne DataNodes generate a name in the form of
     *                       "DataNode.Connection.nodename".
     * @param dataSource     DataSource that provides new connections.
     * @return a connection that participates in the current transaction.
     */
    Connection getOrCreateConnection(String connectionName, DataSource dataSource) throws SQLException;

    /**
     * Returns all connections associated with the transaction.
     *
     * @return connections associated with the transaction.
     */
    Map<String, Connection> getConnections();

    void addListener(TransactionListener listener);

    /**
     * Is this transaction managed by external transaction manager
     */
    boolean isExternal();
}
