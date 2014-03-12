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

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.jdbc.BatchAction;
import org.apache.cayenne.access.jdbc.EJBQLAction;
import org.apache.cayenne.access.jdbc.ProcedureAction;
import org.apache.cayenne.access.jdbc.SQLTemplateAction;
import org.apache.cayenne.access.jdbc.SelectAction;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SQLActionVisitor;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;

/**
 * A factory of default SQLActions. Adapters usually subclass JdbcActionBuilder to provide
 * custom actions for various query types.
 * 
 * @since 1.2
 */
public class JdbcActionBuilder implements SQLActionVisitor {

    protected DataNode dataNode;

    /**
     * @since 3.2
     */
    public JdbcActionBuilder(DataNode dataNode) {
        this.dataNode = dataNode;
    }

    @Override
    public SQLAction batchAction(BatchQuery query) {
        // check run strategy...

        // optimistic locking is not supported in batches due to JDBC driver limitations
        boolean useOptimisticLock = query.isUsingOptimisticLocking();

        boolean runningAsBatch = !useOptimisticLock && dataNode.getAdapter().supportsBatchUpdates();
        BatchAction action = new BatchAction(query, dataNode);
        action.setBatch(runningAsBatch);
        return action;
    }

    @Override
    public SQLAction procedureAction(ProcedureQuery query) {
        return new ProcedureAction(query, dataNode);
    }

    @Override
    public <T> SQLAction objectSelectAction(SelectQuery<T> query) {
        return new SelectAction(query, dataNode);
    }

    @Override
    public SQLAction sqlAction(SQLTemplate query) {
        return new SQLTemplateAction(query, dataNode);
    }

    /**
     * @since 3.0
     */
    @Override
    public SQLAction ejbqlAction(EJBQLQuery query) {
        return new EJBQLAction(query, this, dataNode);
    }
}
