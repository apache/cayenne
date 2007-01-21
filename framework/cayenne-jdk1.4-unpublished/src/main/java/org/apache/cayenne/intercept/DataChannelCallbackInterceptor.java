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

import java.util.List;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.GraphManager;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.LifecycleEventCallback;
import org.apache.cayenne.map.LifecycleEventCallbackMap;
import org.apache.cayenne.query.Query;

/**
 * Implements JPA-compliant "PreUpdate", "PostUpdate", "PostPersist", "PostRemove",
 * "PostLoad" callbacks for the DataChannel operations. <p/>Depending on how callbacks are
 * registered, they are invoked either on persistent object instances directly or on an
 * instance of an arbitrary listener class. Signature of a callback method of a persistent
 * object is <code>"void method()"</code>, while for a non-persistent listener it is
 * <code>"void method(Object)"</code>. <p/>Note that this interceptor does not apply
 * "PreRemove" and "PrePersist" callbacks during "onSync", assuming that a child
 * ObjectContext did that already. It is often used in conjunction with
 * {@link ObjectContextCallbackInterceptor} that adds those callbacks.
 * 
 * @see ObjectContextCallbackInterceptor
 * @since 3.0
 * @author Andrus Adamchik
 */
public class DataChannelCallbackInterceptor extends DataChannelDecorator {

    protected LifecycleEventCallbackMap preUpdate;
    protected LifecycleEventCallbackMap postPersist;
    protected LifecycleEventCallbackMap postRemove;
    protected LifecycleEventCallbackMap postUpdate;
    protected LifecycleEventCallbackMap postLoad;

    protected boolean contextCallbacksEnabled;
    protected LifecycleEventCallbackMap preRemove;
    protected LifecycleEventCallbackMap prePersist;

    public void setChannel(DataChannel channel) {
        this.channel = channel;

        // init callback ivars for faster access...
        if (channel != null) {
            EntityResolver resolver = getEntityResolver();

            preUpdate = resolver.getCallbacks(LifecycleEventCallback.PRE_UPDATE);
            postPersist = resolver.getCallbacks(LifecycleEventCallback.POST_PERSIST);
            postRemove = resolver.getCallbacks(LifecycleEventCallback.POST_REMOVE);
            postUpdate = resolver.getCallbacks(LifecycleEventCallback.POST_UPDATE);
            postLoad = resolver.getCallbacks(LifecycleEventCallback.POST_LOAD);
            preRemove = resolver.getCallbacks(LifecycleEventCallback.PRE_REMOVE);
            prePersist = resolver.getCallbacks(LifecycleEventCallback.PRE_PERSIST);
        }
        else {
            preUpdate = null;
            postPersist = null;
            postRemove = null;
            postUpdate = null;
            postLoad = null;
            preRemove = null;
            prePersist = null;
        }
    }

    protected boolean isEmpty() {
        if (!(preUpdate.isEmpty()
                && postPersist.isEmpty()
                && postRemove.isEmpty()
                && postUpdate.isEmpty() && postLoad.isEmpty())) {
            return false;
        }

        return contextCallbacksEnabled
                ? preRemove.isEmpty() && prePersist.isEmpty()
                : true;
    }

    public QueryResponse onQuery(ObjectContext originatingContext, Query query) {
        QueryResponse response = channel.onQuery(originatingContext, query);

        // TODO: andrus, 9/21/2006 - this method incorrectly calls "postLoad" when query
        // refresh flag is set to false and object is already there.

        if (!postLoad.isEmpty()) {

            List list = response.firstList();
            if (list != null
                    && !list.isEmpty()
                    && !(query.getMetaData(originatingContext.getEntityResolver()))
                            .isFetchingDataRows()) {
                postLoad.performCallbacks(list);
            }
        }

        return response;
    }

    public GraphDiff onSync(
            ObjectContext originatingContext,
            GraphDiff changes,
            int syncType) {

        if (isEmpty()) {
            return channel.onSync(originatingContext, changes, syncType);
        }

        SyncCallbackProcessor processor = createSyncProcessor(originatingContext
                .getGraphManager(), changes);

        processor.applyPreCommit(syncType);
        GraphDiff parentDiff = channel.onSync(originatingContext, changes, syncType);
        processor.applyPostCommit(syncType);

        return parentDiff;
    }

    SyncCallbackProcessor createSyncProcessor(GraphManager graphManager, GraphDiff changes) {
        return new SyncCallbackProcessor(this, graphManager, changes);
    }

    public LifecycleEventCallbackMap getPostLoad() {
        return postLoad;
    }

    public LifecycleEventCallbackMap getPostPersist() {
        return postPersist;
    }

    public LifecycleEventCallbackMap getPostRemove() {
        return postRemove;
    }

    public LifecycleEventCallbackMap getPostUpdate() {
        return postUpdate;
    }

    public LifecycleEventCallbackMap getPreUpdate() {
        return preUpdate;
    }

    /**
     * Returns whether "PrePersist" and "PreRemove" callbacks should be executed during
     * sync. By default this is false, as they are executed by the parent ObjectContext.
     * This can be changed by calling {@link #setContextCallbacksEnabled(boolean)}.
     */
    public boolean isContextCallbacksEnabled() {
        return contextCallbacksEnabled;
    }

    public void setContextCallbacksEnabled(boolean contextCallbacksEnabled) {
        this.contextCallbacksEnabled = contextCallbacksEnabled;
    }

    
    public LifecycleEventCallbackMap getPrePersist() {
        return prePersist;
    }

    
    public LifecycleEventCallbackMap getPreRemove() {
        return preRemove;
    }
}
