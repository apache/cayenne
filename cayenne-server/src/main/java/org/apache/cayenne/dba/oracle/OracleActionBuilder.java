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

package org.apache.cayenne.dba.oracle;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.JdbcActionBuilder;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;

/**
 * @since 1.2
 */
class OracleActionBuilder extends JdbcActionBuilder {

    OracleActionBuilder(DataNode dataNode) {
        super(dataNode);
    }

    @Override
    public SQLAction sqlAction(SQLTemplate query) {
        return new OracleSQLTemplateAction(query, dataNode);
    }

    @Override
    public SQLAction batchAction(BatchQuery query) {

        // optimistic locking is not supported in batches due to JDBC driver
        // limitations
        // TODO: is this still true with ojdbc6.jar?
        boolean useOptimisticLock = query.isUsingOptimisticLocking();
        boolean runningAsBatch = !useOptimisticLock && dataNode.getAdapter().supportsBatchUpdates();

        return new OracleBatchAction(query, dataNode, runningAsBatch);
    }

    @Override
    public SQLAction procedureAction(ProcedureQuery query) {
        return new OracleProcedureAction(query, dataNode);
    }

    @Override
    public <T> SQLAction objectSelectAction(SelectQuery<T> query) {
        return new OracleSelectAction(query, dataNode);
    }
}
