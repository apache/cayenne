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
package org.apache.cayenne.itest.pojo;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;

/**
 * A DataChannel that throws on all query or update attempts.
 * 
 */
class BlockingDataChannel implements DataChannel {

    protected DataChannel channel;

    BlockingDataChannel(DataChannel channel) {
        this.channel = channel;
    }

    /**
     * Returns the first DataChannel that is not a blocking DataChannel in the channel
     * chain.
     */
    public DataChannel getChannel() {
        return (channel instanceof BlockingDataChannel) ? ((BlockingDataChannel) channel)
                .getChannel() : channel;
    }

    public EntityResolver getEntityResolver() {
        return channel.getEntityResolver();
    }

    public EventManager getEventManager() {
        return channel.getEventManager();
    }

    public QueryResponse onQuery(ObjectContext originatingContext, Query query) {
        throw new CayenneRuntimeException("Queries are not allowed. Attempted query: "
                + query);
    }

    public GraphDiff onSync(
            ObjectContext originatingContext,
            GraphDiff changes,
            int syncType) {
        throw new CayenneRuntimeException("Commits are not allowed.");
    }
}
