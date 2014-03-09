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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelFilter;
import org.apache.cayenne.DataChannelFilterChain;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.annotation.PostPersist;
import org.apache.cayenne.annotation.PostRemove;
import org.apache.cayenne.annotation.PostUpdate;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.lifecycle.changeset.ChangeSetFilter;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.Query;

/**
 * A {@link DataChannelFilter} that enables audit of entities annotated with
 * {@link Auditable} and {@link AuditableChild}. Note that this filter relies on
 * {@link ChangeSetFilter} presence in the DataDomain filter chain to be able to
 * analyze ignored properties.
 * 
 * @since 3.1
 */
public class AuditableFilter implements DataChannelFilter {

    private ThreadLocal<AuditableAggregator> threadAggregator;
    private ConcurrentMap<String, AuditableEntityDescriptor> entityDescriptors;
    protected AuditableProcessor processor;
    protected EntityResolver entityResolver;

    /**
     * @since 3.2
     */
    public AuditableFilter(AuditableProcessor processor) {
        this.processor = processor;
        this.entityDescriptors = new ConcurrentHashMap<String, AuditableEntityDescriptor>();
        this.threadAggregator = new ThreadLocal<AuditableAggregator>();
    }

    /**
     * @deprecated since 3.1 use {@link #AuditableFilter(AuditableProcessor)}
     *             constructor - EntityResolver will be initialized in 'init'.
     */
    @Deprecated
    public AuditableFilter(EntityResolver entityResolver, AuditableProcessor processor) {
        this(processor);
    }

    public void init(DataChannel channel) {
        this.entityResolver = channel.getEntityResolver();
    }

    public QueryResponse onQuery(ObjectContext originatingContext, Query query, DataChannelFilterChain filterChain) {
        return filterChain.onQuery(originatingContext, query);
    }

    public GraphDiff onSync(ObjectContext originatingContext, GraphDiff changes, int syncType,
            DataChannelFilterChain filterChain) {

        GraphDiff response;

        try {
            response = filterChain.onSync(originatingContext, changes, syncType);
            if (syncType == DataChannel.FLUSH_CASCADE_SYNC || syncType == DataChannel.FLUSH_NOCASCADE_SYNC) {
                postSync();
            }
        } finally {
            cleanupPostSync();
        }

        return response;
    }

    /**
     * A method called at the end of every
     * {@link #onSync(ObjectContext, GraphDiff, int, DataChannelFilterChain)}
     * invocation. This implementation uses it for cleaning up thread-local
     * state of the filter. Subclasses may override it to do their own cleanup,
     * and are expected to call super.
     */
    protected void cleanupPostSync() {
        threadAggregator.set(null);
    }

    void postSync() {
        AuditableAggregator aggregator = threadAggregator.get();
        if (aggregator != null) {
            // must reset thread aggregator before processing the audit
            // operations
            // to avoid an endless processing loop if audit processor commits
            // something
            threadAggregator.set(null);
            aggregator.postSync();
        }
    }

    private AuditableAggregator getAggregator() {
        AuditableAggregator aggregator = threadAggregator.get();
        if (aggregator == null) {
            aggregator = new AuditableAggregator(processor);
            threadAggregator.set(aggregator);
        }

        return aggregator;
    }

    @PostPersist(entityAnnotations = Auditable.class)
    void insertAudit(Persistent object) {
        getAggregator().audit(object, AuditableOperation.INSERT);
    }

    @PostRemove(entityAnnotations = Auditable.class)
    void deleteAudit(Persistent object) {
        getAggregator().audit(object, AuditableOperation.DELETE);
    }

    @PostUpdate(entityAnnotations = Auditable.class)
    void updateAudit(Persistent object) {
        if (isAuditableUpdate(object, false)) {
            getAggregator().audit(object, AuditableOperation.UPDATE);
        }
    }

    // only catching child updates... child insert/delete presumably causes an
    // event on
    // the owner object

    @PostUpdate(entityAnnotations = AuditableChild.class)
    void updateAuditChild(Persistent object) {

        if (isAuditableUpdate(object, true)) {

            Persistent parent = getParent(object);

            if (parent != null) {
                // not calling 'updateAudit' to skip checking
                // 'isAuditableUpdate' on
                // parent
                getAggregator().audit(parent, AuditableOperation.UPDATE);
            } else {
                // TODO: maybe log this fact... shouldn't normally happen, but I
                // can
                // imagine certain combinations of object graphs, disconnected
                // relationships, delete rules, etc. may cause this
            }
        }
    }

    protected Persistent getParent(Persistent object) {

        if (object == null) {
            throw new NullPointerException("Null object");
        }

        if (!(object instanceof DataObject)) {
            throw new IllegalArgumentException("Object is not a DataObject: " + object.getClass().getName());
        }

        DataObject dataObject = (DataObject) object;

        AuditableChild annotation = dataObject.getClass().getAnnotation(AuditableChild.class);
        if (annotation == null) {
            throw new IllegalArgumentException("No 'AuditableChild' annotation found");
        }

        String propertyPath = annotation.value();

        if (propertyPath == null || propertyPath.equals("")) {
            propertyPath = objectIdRelationshipName(annotation.objectIdRelationship());
        }

        if (propertyPath == null || propertyPath.equals("")) {
            throw new IllegalStateException("Either 'value' or 'objectIdRelationship' of @AuditableChild must be set");
        }

        return (Persistent) dataObject.readNestedProperty(propertyPath);
    }

    // TODO: It's a temporary clone method of {@link
    // org.apache.cayenne.lifecycle.relationship.ObjectIdRelationshipHandler#objectIdRelationshipName(String)}.
    // Needs to be encapsulated to some separate class to avoid a code
    // duplication
    private String objectIdRelationshipName(String uuidPropertyName) {
        return "cay:related:" + uuidPropertyName;
    }

    protected boolean isAuditableUpdate(Object object, boolean child) {
        AuditableEntityDescriptor descriptor = getEntityDescriptor(object, child);
        return descriptor.auditableChange((Persistent) object);
    }

    private AuditableEntityDescriptor getEntityDescriptor(Object object, boolean child) {

        ObjEntity entity = entityResolver.lookupObjEntity(object);

        AuditableEntityDescriptor descriptor = entityDescriptors.get(entity.getName());
        if (descriptor == null) {

            String[] ignoredProperties;

            if (child) {
                AuditableChild annotation = object.getClass().getAnnotation(AuditableChild.class);
                ignoredProperties = annotation != null ? annotation.ignoredProperties() : null;
            } else {
                Auditable annotation = object.getClass().getAnnotation(Auditable.class);
                ignoredProperties = annotation != null ? annotation.ignoredProperties() : null;
            }

            descriptor = new AuditableEntityDescriptor(entity, ignoredProperties);

            AuditableEntityDescriptor existingDescriptor = entityDescriptors.putIfAbsent(entity.getName(), descriptor);

            if (existingDescriptor != null) {
                descriptor = existingDescriptor;
            }
        }

        return descriptor;

    }
}
