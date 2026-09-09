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

package org.apache.cayenne.access;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.util.ShallowMergeOperation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A {@link DataChannel} that connects a nested ObjectContext to its parent {@link DataContext}. All channel
 * operations are delegated to the parent context. Note that the parent context posts channel events on its own behalf,
 * so listeners interested in those events should listen to the context, not to this channel.
 *
 * @since 5.0
 */
public record DataContextChannel(DataContext context) implements DataChannel {

    public DataContextChannel {
        Objects.requireNonNull(context, "Null parent context");
    }

    @Override
    public EventManager getEventManager() {
        return context.getChannel().getEventManager();
    }

    @Override
    public EntityResolver getEntityResolver() {
        return context.getEntityResolver();
    }

    @Override
    public DataChannel getParent() {
        return context.getChannel();
    }

    @Override
    public QueryResponse onQuery(ObjectContext originatingContext, Query query, boolean iteratedResult) {
        checkOriginatingContext(originatingContext);
        return context.onQuery(originatingContext, query, iteratedResult);
    }

    @Override
    public void onInvalidate(ObjectContext originatingContext, Collection<ObjectId> objectIds) {
        checkOriginatingContext(originatingContext);
        context.invalidateIds(objectIds);
    }

    @Override
    public List<Persistent> onResolveRelationship(ObjectContext originatingContext, ObjectId sourceId, String relationshipName) {
        checkOriginatingContext(originatingContext);

        List<? extends Persistent> related = context.resolveRelationship(sourceId, relationshipName, true);
        ShallowMergeOperation merger = new ShallowMergeOperation(originatingContext);
        List<Persistent> childObjects = new ArrayList<>(related.size());
        for (Persistent object : related) {
            childObjects.add(merger.merge(object));
        }
        return childObjects;
    }

    @Override
    public GraphDiff onSync(ObjectContext originatingContext, GraphDiff changes, int syncType) {
        checkOriginatingContext(originatingContext);
        return context.onSync(originatingContext, changes, syncType);
    }

    private void checkOriginatingContext(ObjectContext originatingContext) {
        if (originatingContext == null) {
            throw new IllegalArgumentException("Originating context must be a child of " + context + ", not null");
        }

        if (originatingContext == context) {
            throw new IllegalArgumentException("Originating context must be a child of " + context
                    + ", not the context itself");
        }
    }
}
