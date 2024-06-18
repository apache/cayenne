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

import java.sql.Connection;

/**
 * A callback that is notified as transaction progresses through stages. It can customize transaction isolation level,
 * etc.
 *
 * @since 4.0
 */
public interface TransactionListener {

    void willCommit(Transaction tx);

    void willRollback(Transaction tx);

    void willAddConnection(Transaction tx, String connectionName, Connection connection);

    /**
     * This method could be used to decorate or substitute
     * new connection initiated inside a Cayenne transaction.
     * <br/>
     * The default implementation returns the same connection.
     *
     * @param tx transaction that initiated connection
     * @param connection connection (it could be decorated by other listeners)
     * @return connection
     *
     * @since 4.2
     */
    default Connection decorateConnection(Transaction tx, Connection connection){
        return connection;
    }
}
