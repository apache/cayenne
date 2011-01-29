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
package org.apache.cayenne.lifecycle.audit;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelFilter;
import org.apache.cayenne.DataChannelFilterChain;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.annotation.PostPersist;
import org.apache.cayenne.annotation.PostRemove;
import org.apache.cayenne.annotation.PostUpdate;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.query.Query;

/**
 * A {@link DataChannelFilter} that enables audit of entities annotated with
 * {@link Auditable} and {@link AuditableChild}.
 * 
 * @since 3.1
 */
public class AuditableFilter implements DataChannelFilter {

    private ThreadLocal<AuditableAggregator> aggregator;
    protected AuditableProcessor processor;

    public AuditableFilter(AuditableProcessor processor) {
        this.processor = processor;
        this.aggregator = new ThreadLocal<AuditableAggregator>();
    }

    public void init(DataChannel channel) {
        // noop
    }

    public QueryResponse onQuery(
            ObjectContext originatingContext,
            Query query,
            DataChannelFilterChain filterChain) {
        return filterChain.onQuery(originatingContext, query);
    }

    public GraphDiff onSync(
            ObjectContext originatingContext,
            GraphDiff changes,
            int syncType,
            DataChannelFilterChain filterChain) {

        try {
            GraphDiff response = filterChain
                    .onSync(originatingContext, changes, syncType);

            postSync();

            return response;
        }
        finally {
            aggregator.set(null);
        }
    }

    void postSync() {
        AuditableAggregator aggregator = this.aggregator.get();
        if (aggregator != null) {
            aggregator.postSync();
        }
    }

    private AuditableAggregator getAggregator() {
        AuditableAggregator aggregator = this.aggregator.get();
        if (aggregator == null) {
            aggregator = new AuditableAggregator(processor);
            this.aggregator.set(aggregator);
        }

        return aggregator;
    }

    @PostPersist(entityAnnotations = Auditable.class)
    void insertAudit(Object object) {
        getAggregator().audit(object, AuditableOperation.INSERT);
    }

    @PostRemove(entityAnnotations = Auditable.class)
    void deleteAudit(Object object) {
        getAggregator().audit(object, AuditableOperation.DELETE);
    }

    @PostUpdate(entityAnnotations = Auditable.class)
    void updateAudit(Object object) {
        getAggregator().audit(object, AuditableOperation.UPDATE);
    }

    // only catching child updates... child insert/delete presumably causes an event on
    // the owner object

    @PostUpdate(entityAnnotations = AuditableChild.class)
    void updateAuditChild(Object object) {

        Object parent = getParent(object);

        if (parent != null) {
            updateAudit(parent);
        }
        else {
            // TODO: maybe log this fact... shouldn't normally happen, but I can imagine
            // certain combinations of object graphs, disconnected relationships, delete
            // rules, etc. may cause this
        }
    }

    protected Object getParent(Object object) {

        if (object == null) {
            throw new NullPointerException("Null object");
        }

        if (!(object instanceof DataObject)) {
            throw new IllegalArgumentException("Object is not a DataObject: "
                    + object.getClass().getName());
        }

        DataObject dataObject = (DataObject) object;

        AuditableChild annotation = dataObject.getClass().getAnnotation(
                AuditableChild.class);
        if (annotation == null) {
            throw new IllegalArgumentException("No 'AuditableChild' annotation found");
        }

        return dataObject.readNestedProperty(annotation.value());
    }
}
