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
package org.apache.cayenne.unit.di.client;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.unit.di.DataChannelSyncStats;

public class ClientServerDataChannelDecorator implements DataChannel {

    private DataChannel delegate;
    private boolean blockingMessages;
    private DataChannelSyncStats statsCounter;

    public ClientServerDataChannelDecorator(DataChannel delegate) {
        this.delegate = delegate;
    }
    
    public DataChannel getDelegate() {
        return delegate;
    }

    public EntityResolver getEntityResolver() {
        checkMessageAllowed("getEntityResolver");
        return delegate.getEntityResolver();
    }

    public EventManager getEventManager() {
        checkMessageAllowed("getEventManager");
        return delegate.getEventManager();
    }

    public QueryResponse onQuery(ObjectContext originatingContext, Query query) {
        checkMessageAllowed("onQuery");
        return delegate.onQuery(originatingContext, query);
    }

    public GraphDiff onSync(
            ObjectContext originatingContext,
            GraphDiff changes,
            int syncType) {
        checkMessageAllowed("onSync");
        countDiffs(changes);
        return delegate.onSync(originatingContext, changes, syncType);
    }

    public void setBlockingMessages(boolean blockingMessages) {
        this.blockingMessages = blockingMessages;
    }

    public void setSyncStatsCounter(DataChannelSyncStats statsCounter) {
        this.statsCounter = statsCounter;
    }

    private void checkMessageAllowed(String label) throws AssertionFailedError {
        if (blockingMessages) {
            Assert.fail("Message is unexpected: " + label);
        }
    }

    private void countDiffs(GraphDiff changes) {
        if (statsCounter != null) {
            changes.apply(statsCounter);
        }
    }
}
