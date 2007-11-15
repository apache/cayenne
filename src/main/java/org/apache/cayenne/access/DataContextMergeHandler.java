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

import org.apache.cayenne.DataChannelListener;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.ObjectStore.SnapshotEventDecorator;
import org.apache.cayenne.access.event.SnapshotEvent;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.GraphEvent;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.Property;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;
import org.apache.cayenne.util.Util;

/**
 * A listener of GraphEvents sent by the DataChannel that merges changes to the
 * DataContext.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
// TODO: andrus, 11/25/2006 - this logic is the same as the logic in DataRowUtils used to
// merge snapshot changes. Any way to reconclie the two? (in fact DataRowUtils is more
// comprehensive)
class DataContextMergeHandler implements GraphChangeHandler, DataChannelListener {

    private boolean active;
    private DataContext context;

    DataContextMergeHandler(DataContext context) {
        this.active = true;
        this.context = context;
    }

    void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Returns true if this object is active and an event came from our channel, but did
     * not originate in it.
     */
    private boolean shouldProcessEvent(GraphEvent e) {

        if (!active) {
            return false;
        }

        // this effectively filters out all events that are not coming from peers or
        // grandparents...
        return e.getSource() == context.getChannel()
                && e.getPostedBy() != context
                && e.getPostedBy() != context.getChannel();

        // the first condition (e.getSource() == context.getChannel()) is actually always
        // 'true' because of how the listener is registered. Still keep it here as an
        // extra safegurad
    }

    private Property propertyForId(Object nodeId, String propertyName) {
        ClassDescriptor descriptor = context.getEntityResolver().getClassDescriptor(
                ((ObjectId) nodeId).getEntityName());
        return descriptor.getProperty(propertyName);
    }

    // *** GraphEventListener methods

    public void graphChanged(GraphEvent event) {
        // parent received external change
        if (shouldProcessEvent(event)) {

            // temp kludge - see TODO in ObjectStore.snapshotsChanged(..)
            GraphDiff diff = event.getDiff();
            if (diff instanceof SnapshotEventDecorator) {
                SnapshotEvent decoratedEvent = ((SnapshotEventDecorator) diff).getEvent();
                context.getObjectStore().processSnapshotEvent(decoratedEvent);
            }
            else {
                synchronized (context.getObjectStore()) {
                    diff.apply(this);
                }
            }

            // repost channel change event for our own children
            context.fireDataChannelChanged(event.getPostedBy(), event.getDiff());
        }
    }

    public void graphFlushed(GraphEvent event) {
        // peer is committed
        if (shouldProcessEvent(event)) {
            event.getDiff().apply(this);

            // repost as change event for our own children
            context.fireDataChannelChanged(event.getPostedBy(), event.getDiff());
        }
    }

    public void graphRolledback(GraphEvent event) {
        // TODO: andrus, 3/26/2006 - enable this once all ObjectStore diffs implement
        // working undo operation

        // if(shouldProcessEvent(e)) {
        // event.getDiff().undo(this);
        // }
    }

    // *** GraphChangeHandler methods

    public void nodeIdChanged(Object nodeId, Object newId) {
        context.getObjectStore().processIdChange(nodeId, newId);
    }

    public void nodeCreated(Object nodeId) {
        // noop
    }

    public void nodeRemoved(Object nodeId) {
        ObjectStore os = context.getObjectStore();
        synchronized (os) {
            os.processDeletedID(nodeId);
        }
    }

    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {

        Persistent object = (Persistent) context.getGraphManager().getNode(nodeId);
        if (object != null && object.getPersistenceState() != PersistenceState.HOLLOW) {

            // do not override local changes....
            Property p = propertyForId(nodeId, property);
            if (Util.nullSafeEquals(p.readPropertyDirectly(object), oldValue)) {
                p.writePropertyDirectly(object, oldValue, newValue);
            }
        }
    }

    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        arcChanged(nodeId, targetNodeId, arcId);
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        arcChanged(nodeId, targetNodeId, arcId);
    }

    // works the same for add and remove as long as we don't get too smart per TODO below.
    private void arcChanged(Object nodeId, Object targetNodeId, Object arcId) {

        final Persistent source = (Persistent) context.getGraphManager().getNode(nodeId);
        if (source != null && source.getPersistenceState() != PersistenceState.HOLLOW) {

            final int state = source.getPersistenceState();

            Property p = propertyForId(nodeId, arcId.toString());
            p.visit(new PropertyVisitor() {

                public boolean visitAttribute(AttributeProperty property) {
                    return false;
                }

                public boolean visitToMany(ToManyProperty property) {
                    if (state == PersistenceState.COMMITTED) {
                        property.invalidate(source);
                    }

                    return false;
                }

                public boolean visitToOne(ToOneProperty property) {
                    if (state == PersistenceState.COMMITTED) {
                        property.invalidate(source);
                    }
                    // TODO: andrus, 11/25/2006 - handle replacement of clean properties
                    // of dirty objects. See DataRowUtils for details.
                    return false;
                }
            });
        }
    }
}
