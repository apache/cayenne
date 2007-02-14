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
import org.apache.cayenne.intercept.DataChannelCallbackInterceptor;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;

/**
 * A DataChannel that provides a server-side bridge between client and server objects in a
 * Remote Object Persistence stack.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class ClientServerChannel implements DataChannel {

    protected DataContext serverContext;
    protected boolean lifecycleCallbacksEnabled;

    public ClientServerChannel(DataDomain domain) {
        this(domain.createDataContext());
    }

    ClientServerChannel(DataContext serverContext) {
        this.serverContext = serverContext;
    }

    /**
     * @deprecated since 3.0 as DataChannel events (incorrectly called "lifecycleEvents"
     *             in 2.0) are no longer posted by ClientServerChannel.
     */
    public ClientServerChannel(DataDomain domain, boolean lifecycleEventsEnabled) {
        this(domain);
    }

    /**
     * @deprecated Since 3.0 - always returns false. This method was a misnomer referring
     *             to DataChannel events, not lifecycle events introduced in 3.0.
     *             Currently ClientServerChannel posts no channel events.
     */
    public boolean isLifecycleEventsEnabled() {
        return false;
    }

    /**
     * @deprecated Since 3.0 - does nothing. This method was a misnomer referring to
     *             DataChannel events, not lifecycle events introduced in 3.0. Currently
     *             ClientServerChannel posts no channel events.
     */
    public void setLifecycleEventsEnabled(boolean lifecycleEventsEnabled) {

    }

    public QueryResponse onQuery(ObjectContext context, Query query) {
        return new ClientServerChannelQueryAction(this, query).execute();
    }

    QueryCache getQueryCache() {
        return serverContext.getQueryCache();
    }

    DataChannel getParentChannel() {
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

        return getParentChannel().onSync(null, changes, syncType);
    }

    /**
     * @since 3.0
     */
    public boolean isLifecycleCallbacksEnabled() {
        return lifecycleCallbacksEnabled;
    }

    /**
     * Enables or disables lifecycle event callbacks for the underlying ObjectContext used
     * by this channel. Enabling callbacks allows server side logic to be applied to the
     * persistent objects during select and commit operations.
     * 
     * @since 3.0
     */
    public void setLifecycleCallbacksEnabled(boolean lifecycleCallbacksEnabled) {
        if (lifecycleCallbacksEnabled != this.lifecycleCallbacksEnabled) {
            this.lifecycleCallbacksEnabled = lifecycleCallbacksEnabled;

            if (lifecycleCallbacksEnabled) {
                enableCallbacks();
            }
            else {
                disableCallbacks();
            }
        }
    }

    void enableCallbacks() {
        DataChannelCallbackInterceptor interceptor = new DataChannelCallbackInterceptor();

        // must call pre-persist and pre-remove on commit
        interceptor.setContextCallbacksEnabled(true);
        interceptor.setChannel(serverContext.getParentDataDomain());

        serverContext.setChannel(interceptor);
    }

    void disableCallbacks() {
        serverContext.setChannel(serverContext.getParentDataDomain());
    }
}
