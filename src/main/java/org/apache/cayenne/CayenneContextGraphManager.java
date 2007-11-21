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

package org.apache.cayenne;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.event.EventSubject;
import org.apache.cayenne.graph.ArcCreateOperation;
import org.apache.cayenne.graph.ArcDeleteOperation;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.GraphEvent;
import org.apache.cayenne.graph.GraphMap;
import org.apache.cayenne.graph.NodeCreateOperation;
import org.apache.cayenne.graph.NodeDeleteOperation;
import org.apache.cayenne.graph.NodeIdChangeOperation;
import org.apache.cayenne.graph.NodePropertyChangeOperation;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyException;
import org.apache.cayenne.reflect.ToManyMapProperty;

/**
 * A GraphMap extension that works together with ObjectContext to track persistent object
 * changes and send events.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
final class CayenneContextGraphManager extends GraphMap {

    static final String COMMIT_MARKER = "commit";
    static final String FLUSH_MARKER = "flush";

    CayenneContext context;
    Collection<Object> deadIds;
    boolean changeEventsEnabled;
    boolean lifecycleEventsEnabled;

    ObjectContextStateLog stateLog;
    ObjectContextChangeLog changeLog;

    CayenneContextGraphManager(CayenneContext context, boolean changeEventsEnabled,
            boolean lifecycleEventsEnabled) {

        this.context = context;
        this.changeEventsEnabled = changeEventsEnabled;
        this.lifecycleEventsEnabled = lifecycleEventsEnabled;

        this.stateLog = new ObjectContextStateLog(this);
        this.changeLog = new ObjectContextChangeLog();
    }

    boolean hasChanges() {
        return changeLog.size() > 0;
    }

    boolean hasChangesSinceLastFlush() {
        int size = changeLog.hasMarker(FLUSH_MARKER) ? changeLog
                .sizeAfterMarker(FLUSH_MARKER) : changeLog.size();
        return size > 0;
    }

    GraphDiff getDiffs() {
        return changeLog.getDiffs();
    }

    GraphDiff getDiffsSinceLastFlush() {
        return changeLog.hasMarker(FLUSH_MARKER) ? changeLog
                .getDiffsAfterMarker(FLUSH_MARKER) : changeLog.getDiffs();
    }

    Collection<Object> dirtyNodes() {
        return stateLog.dirtyNodes();
    }

    Collection<Object> dirtyNodes(int state) {
        return stateLog.dirtyNodes(state);
    }

    public synchronized Object unregisterNode(Object nodeId) {
        Object node = super.unregisterNode(nodeId);

        // remove node from other collections...
        if (node != null) {
            stateLog.unregisterNode(nodeId);
            changeLog.unregisterNode(nodeId);
            return node;
        }

        return null;
    }

    // ****** Sync Events API *****
    /**
     * Clears commit marker, but keeps all recorded operations.
     */
    void graphCommitAborted() {
        changeLog.removeMarker(COMMIT_MARKER);
    }

    /**
     * Sets commit start marker in the change log. If events are enabled, posts commit
     * start event.
     */
    void graphCommitStarted() {
        changeLog.setMarker(COMMIT_MARKER);
    }

    void graphCommitted(GraphDiff parentSyncDiff) {
        if (parentSyncDiff != null) {
            new CayenneContextMergeHandler(context).merge(parentSyncDiff);
        }

        remapTargets();

        if (lifecycleEventsEnabled) {
            GraphDiff diff = changeLog.getDiffsAfterMarker(COMMIT_MARKER);

            stateLog.graphCommitted();
            reset();

            // include all diffs after the commit start marker.
            send(diff, DataChannel.GRAPH_FLUSHED_SUBJECT, context);
        }
        else {
            stateLog.graphCommitted();
            reset();
        }
    }

    /**
     * Remaps keys in to-many map relationships that contain dirty objects with
     * potentially modified properties.
     */
    private void remapTargets() {

        Iterator<Object> it = stateLog.dirtyIds().iterator();

        EntityResolver resolver = context.getEntityResolver();

        // avoid processing callbacks when updating the map...
        boolean changeCallbacks = context.isPropertyChangeCallbacksDisabled();
        context.setPropertyChangeCallbacksDisabled(true);

        try {
            while (it.hasNext()) {
                ObjectId id = (ObjectId) it.next();
                ClassDescriptor descriptor = resolver.getClassDescriptor(id
                        .getEntityName());

                Iterator<ArcProperty> mapArcProperties = descriptor.getMapArcProperties();
                if (mapArcProperties.hasNext()) {

                    Object object = getNode(id);

                    while (mapArcProperties.hasNext()) {
                        ArcProperty arc = mapArcProperties.next();
                        ToManyMapProperty reverseArc = (ToManyMapProperty) arc
                                .getComplimentaryReverseArc();

                        Object source = arc.readPropertyDirectly(object);
                        if (source != null && !reverseArc.isFault(source)) {
                            remapTarget(reverseArc, source, object);
                        }
                    }
                }
            }
        }
        finally {
            context.setPropertyChangeCallbacksDisabled(changeCallbacks);
        }
    }

    // clone of DataDomainSyncBucket.remapTarget
    private final void remapTarget(
            ToManyMapProperty property,
            Object source,
            Object target) throws PropertyException {

        Map<Object, Object> map = (Map<Object, Object>) property.readProperty(source);
        Object newKey = property.getMapKey(target);
        Object currentValue = map.get(newKey);

        if (currentValue == target) {
            // nothing to do
            return;
        }
        // else - do not check for conflicts here (i.e. another object mapped for the same
        // key), as we have no control of the order in which this method is called, so
        // another object may be remapped later by the caller

        // must do a slow map scan to ensure the object is not mapped under a different
        // key...
        Iterator<?> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) it.next();
            if (e.getValue() == target) {
                it.remove();
                break;
            }
        }

        map.put(newKey, target);
    }

    void graphFlushed() {
        changeLog.setMarker(FLUSH_MARKER);
    }

    void graphReverted() {
        GraphDiff diff = changeLog.getDiffs();

        diff.undo(new NullChangeHandler());
        stateLog.graphReverted();
        reset();

        if (lifecycleEventsEnabled) {
            send(diff, DataChannel.GRAPH_ROLLEDBACK_SUBJECT, context);
        }
    }

    // ****** GraphChangeHandler API ******
    // =====================================================

    public synchronized void nodeIdChanged(Object nodeId, Object newId) {
        stateLog.nodeIdChanged(nodeId, newId);
        processChange(new NodeIdChangeOperation(nodeId, newId));
    }

    public synchronized void nodeCreated(Object nodeId) {
        stateLog.nodeCreated(nodeId);
        processChange(new NodeCreateOperation(nodeId));
    }

    public synchronized void nodeRemoved(Object nodeId) {
        stateLog.nodeRemoved(nodeId);
        processChange(new NodeDeleteOperation(nodeId));
    }

    public synchronized void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {

        stateLog.nodePropertyChanged(nodeId, property, oldValue, newValue);
        processChange(new NodePropertyChangeOperation(
                nodeId,
                property,
                oldValue,
                newValue));
    }

    public synchronized void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        stateLog.arcCreated(nodeId, targetNodeId, arcId);
        processChange(new ArcCreateOperation(nodeId, targetNodeId, arcId));
    }

    public synchronized void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        stateLog.arcDeleted(nodeId, targetNodeId, arcId);
        processChange(new ArcDeleteOperation(nodeId, targetNodeId, arcId));
    }

    // ****** helper methods ******
    // =====================================================

    private void processChange(GraphDiff diff) {
        changeLog.addOperation(diff);

        if (changeEventsEnabled) {
            send(diff, DataChannel.GRAPH_CHANGED_SUBJECT, context);
        }
    }

    /**
     * Wraps GraphDiff in a GraphEvent and sends it via EventManager with specified
     * subject.
     */
    void send(GraphDiff diff, EventSubject subject, Object eventSource) {
        EventManager manager = (context.getChannel() != null) ? context
                .getChannel()
                .getEventManager() : null;

        if (manager != null) {
            GraphEvent e = new GraphEvent(context, eventSource, diff);
            manager.postEvent(e, subject);
        }
    }

    void reset() {
        changeLog.reset();

        if (deadIds != null) {
            // unregister dead ids...
            Iterator<Object> it = deadIds.iterator();
            while (it.hasNext()) {
                nodes.remove(it.next());
            }

            deadIds = null;
        }
    }

    Collection<Object> deadIds() {
        if (deadIds == null) {
            deadIds = new ArrayList<Object>();
        }

        return deadIds;
    }

    class NullChangeHandler implements GraphChangeHandler {

        public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        }

        public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        }

        public void nodeCreated(Object nodeId) {
        }

        public void nodeIdChanged(Object nodeId, Object newId) {
        }

        public void nodePropertyChanged(
                Object nodeId,
                String property,
                Object oldValue,
                Object newValue) {
        }

        public void nodeRemoved(Object nodeId) {
        }
    }
}
