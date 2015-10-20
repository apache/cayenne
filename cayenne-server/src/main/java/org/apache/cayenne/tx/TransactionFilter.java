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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelFilter;
import org.apache.cayenne.DataChannelFilterChain;
import org.apache.cayenne.DataChannelSyncCallbackAction;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.query.Query;

/**
 * A {@link DataChannelFilter} that provides transactions.
 *
 * @since 4.0
 */
public class TransactionFilter implements DataChannelFilter {

    @Inject
    protected TransactionManager transactionManager;

    @Override
    public void init(DataChannel channel) {
    }

    @Override
    public QueryResponse onQuery(ObjectContext originatingContext, Query query, DataChannelFilterChain filterChain) {
        return filterChain.onQuery(originatingContext, query);
    }

    @Override
    public GraphDiff onSync(final ObjectContext originatingContext, final GraphDiff changes, final int syncType, final DataChannelFilterChain filterChain) {
        DataChannelSyncCallbackAction callbackAction = DataChannelSyncCallbackAction.getCallbackAction(
                originatingContext.getChannel().getEntityResolver().getCallbackRegistry(),
                originatingContext.getGraphManager(),
                changes, syncType);

        callbackAction.applyPreCommit();

        GraphDiff result;
        switch (syncType) {
            case DataChannel.ROLLBACK_CASCADE_SYNC:
                result = filterChain.onSync(originatingContext, changes, syncType);;
                break;

            // including transaction handling logic
            case DataChannel.FLUSH_NOCASCADE_SYNC:
            case DataChannel.FLUSH_CASCADE_SYNC:
                result = transactionManager.performInTransaction(new TransactionalOperation<GraphDiff>() {
                    @Override
                    public GraphDiff perform() {
                        return filterChain.onSync(originatingContext, changes, syncType);
                    }
                });

                break;
            default:
                throw new CayenneRuntimeException("Invalid synchronization type: " + syncType);
        }

        callbackAction.applyPostCommit();
        return result;
    }

}