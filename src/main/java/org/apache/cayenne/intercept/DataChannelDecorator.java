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
package org.apache.cayenne.intercept;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;

/**
 * A helper {@link DataChannel} implementation that passes all requests to the underlying
 * decorated channel for execution. Intended for subclassing.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public class DataChannelDecorator implements DataChannel {

    protected DataChannel channel;

    protected DataChannelDecorator() {

    }

    public DataChannelDecorator(DataChannel channel) {
        setChannel(channel);
    }

    public EntityResolver getEntityResolver() {
        return channel.getEntityResolver();
    }

    public EventManager getEventManager() {
        return channel.getEventManager();
    }

    public QueryResponse onQuery(ObjectContext originatingContext, Query query) {
        return channel.onQuery(originatingContext, query);
    }

    public GraphDiff onSync(
            ObjectContext originatingContext,
            GraphDiff changes,
            int syncType) {
        return channel.onSync(originatingContext, changes, syncType);
    }

    public DataChannel getChannel() {
        return channel;
    }

    public void setChannel(DataChannel channel) {
        // TODO: andrus, 9/20/2006 - register as listener of the channel events
        this.channel = channel;
    }
}
