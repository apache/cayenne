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

import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;

/**
 * @since 4.0
 */
public class DefaultTransactionFactory implements TransactionFactory {

    protected boolean externalTransactions;

    protected JdbcEventLogger jdbcEventLogger;

    public DefaultTransactionFactory(@Inject RuntimeProperties properties, @Inject JdbcEventLogger jdbcEventLogger) {
        this.externalTransactions = properties.getBoolean(Constants.EXTERNAL_TX_PROPERTY, false);
        this.jdbcEventLogger = jdbcEventLogger;
    }

    @Override
    public Transaction createTransaction() {
        return createTransaction(TransactionDescriptor.defaultDescriptor());
    }

    /**
     * @since 4.1
     */
    @Override
    public Transaction createTransaction(TransactionDescriptor descriptor) {
        return externalTransactions ? new ExternalTransaction(jdbcEventLogger, descriptor) : new CayenneTransaction(
                jdbcEventLogger, descriptor);
    }

}
