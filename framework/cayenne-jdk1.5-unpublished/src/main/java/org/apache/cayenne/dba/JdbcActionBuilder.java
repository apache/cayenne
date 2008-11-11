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

import org.apache.cayenne.access.jdbc.BatchAction;
import org.apache.cayenne.access.jdbc.EJBQLAction;
import org.apache.cayenne.access.jdbc.ProcedureAction;
import org.apache.cayenne.access.jdbc.SQLTemplateAction;
import org.apache.cayenne.access.jdbc.SelectAction;
import org.apache.cayenne.access.jdbc.UpdateAction;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.DeleteQuery;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SQLActionVisitor;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.UpdateQuery;

/**
 * A factory of default SQLActions. Adapters usually subclass JdbcActionBuilder to provide
 * custom actions for various query types.
 * 
 * @since 1.2
 */
public class JdbcActionBuilder implements SQLActionVisitor {

    protected JdbcAdapter adapter;
    protected EntityResolver entityResolver;

    /**
     * @deprecated since 3.0 use "JdbcActionBuilder(JdbcAdapter,EntityResolver)"
     *             constructor instead.
     */
    public JdbcActionBuilder(DbAdapter adapter, EntityResolver resolver) {
        if (!(adapter instanceof JdbcAdapter)) {
            throw new IllegalArgumentException("Expected a non-null JdbcAdapter, got: "
                    + adapter);
        }
        this.adapter = (JdbcAdapter) adapter;
        this.entityResolver = resolver;
    }

    public JdbcActionBuilder(JdbcAdapter adapter, EntityResolver resolver) {
        this.adapter = adapter;
        this.entityResolver = resolver;
    }

    public SQLAction batchAction(BatchQuery query) {
        // check run strategy...

        // optimistic locking is not supported in batches due to JDBC driver limitations
        boolean useOptimisticLock = query.isUsingOptimisticLocking();

        boolean runningAsBatch = !useOptimisticLock && adapter.supportsBatchUpdates();
        BatchAction action = new BatchAction(query, adapter, entityResolver);
        action.setBatch(runningAsBatch);
        return action;
    }

    public SQLAction procedureAction(ProcedureQuery query) {
        return new ProcedureAction(query, adapter, entityResolver);
    }

    public SQLAction objectSelectAction(SelectQuery query) {
        return new SelectAction(query, adapter, entityResolver);
    }

    public SQLAction sqlAction(SQLTemplate query) {
        return new SQLTemplateAction(query, adapter, entityResolver);
    }

    /**
     * @deprecated since 3.0 as the corresponding {@link UpdateQuery} and
     *             {@link DeleteQuery} queries are deprecated.
     */
    public SQLAction updateAction(Query query) {
        if (query instanceof SQLTemplate) {
            return new SQLTemplateAction((SQLTemplate) query, adapter, entityResolver);
        }

        return new UpdateAction(query, adapter, entityResolver);
    }

    /**
     * @since 3.0
     */
    public SQLAction ejbqlAction(EJBQLQuery query) {
        return new EJBQLAction(query, this, adapter, entityResolver);
    }

    /**
     * Returns DbAdapter used associated with this action builder.
     */
    public DbAdapter getAdapter() {
        return adapter;
    }

    /**
     * Returns EntityResolver that can be used to gain access to the mapping objects.
     */
    public EntityResolver getEntityResolver() {
        return entityResolver;
    }
}
