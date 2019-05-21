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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelSyncCallbackAction;
import org.apache.cayenne.DataChannelSyncFilter;
import org.apache.cayenne.DataChannelSyncFilterChain;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.graph.GraphDiff;

/**
 * A {@link DataChannelSyncFilter} that provides transactions.
 *
 * @since 4.0
 */
public class TransactionFilter implements DataChannelSyncFilter {

    @Inject
    protected TransactionManager transactionManager;

    @Override
    public GraphDiff onSync(ObjectContext originatingContext, GraphDiff changes, int syncType, DataChannelSyncFilterChain filterChain) {
        DataChannelSyncCallbackAction callbackAction = DataChannelSyncCallbackAction.getCallbackAction(
                originatingContext.getChannel().getEntityResolver().getCallbackRegistry(),
                originatingContext.getGraphManager(),
                changes,
                syncType
        );

        callbackAction.applyPreCommit();

        GraphDiff result;
        switch (syncType) {
            case DataChannel.ROLLBACK_CASCADE_SYNC:
                result = filterChain.onSync(originatingContext, changes, syncType);
                break;

            // including transaction handling logic
            case DataChannel.FLUSH_NOCASCADE_SYNC:
            case DataChannel.FLUSH_CASCADE_SYNC:
                result = transactionManager.performInTransaction(() -> filterChain.onSync(originatingContext, changes, syncType));
                break;

            default:
                throw new CayenneRuntimeException("Invalid synchronization type: %d", syncType);
        }

        callbackAction.applyPostCommit();
        return result;
    }

}