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

package org.apache.cayenne.access;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;

/**
 * A DataChannel that provides a server-side end of the bridge between client and server
 * objects in a Remote Object Persistence stack.
 * 
 * @since 1.2
 */
public class ClientServerChannel implements DataChannel {

    protected DataContext serverContext;

    /**
     * Creates a ClientServerChannel that wraps a specified DataContext.
     * 
     * @since 3.0
     */
    public ClientServerChannel(DataContext serverContext) {
        this.serverContext = serverContext;
    }

    public QueryResponse onQuery(ObjectContext context, Query query) {
        return new ClientServerChannelQueryAction(this, query).execute();
    }

    QueryCache getQueryCache() {
        return serverContext.getQueryCache();
    }

    /**
     * @since 3.1
     */
    public DataChannel getParentChannel() {
        return serverContext;
    }

    public EntityResolver getEntityResolver() {
        return serverContext.getEntityResolver();
    }

    public EventManager getEventManager() {
        return serverContext != null ? serverContext.getEventManager() : null;
    }

    public GraphDiff onSync(
            ObjectContext originatingContext,
            GraphDiff changes,
            int syncType) {

        GraphDiff diff = getParentChannel().onSync(null, changes, syncType);
        return new ClientReturnDiffFilter(getEntityResolver()).filter(diff);
    }
}
