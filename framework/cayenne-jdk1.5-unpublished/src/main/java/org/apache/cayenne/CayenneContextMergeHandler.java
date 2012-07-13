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

import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.GraphEvent;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyDescriptor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.util.Util;

/**
 * An object that merges "backdoor" modifications of the object graph coming from the
 * underlying DataChannel. When doing an update, CayenneContextMergeHandler blocks
 * broadcasting of GraphManager events.
 * 
 * @since 1.2
 */
class CayenneContextMergeHandler implements GraphChangeHandler, DataChannelListener {

    CayenneContext context;
    boolean active;

    CayenneContextMergeHandler(CayenneContext context) {
        this.context = context;
        this.active = true;
    }

    // ******* DataChannelListener methods *******

    public void graphChanged(final GraphEvent e) {
        // process flush
        if (shouldProcessEvent(e) && e.getDiff() != null) {
            runWithEventsDisabled(new Runnable() {

                public void run() {
                    e.getDiff().apply(CayenneContextMergeHandler.this);

                }
            });

            // post event outside of "execute" to make sure it is sent
            repostAfterMerge(e);
        }
    }

    public void graphFlushed(final GraphEvent e) {
        // TODO (Andrus, 10/17/2005) - there are a few problems with commit processing:

        // 1. Event mechanism reliability:
        // - events may come out of order (commit and then preceeding flush)
        // - events may be missing all together (commit arrived, while prior flush did
        // not)
        // Possible solution - an "event_version_id" to be used for optimistic locking

        // 2. We don't know if our own dirty objects were committed or not...
        // For now we will simply merge the changes, and keep the context dirty

        if (shouldProcessEvent(e)) {

            runWithEventsDisabled(new Runnable() {

                public void run() {

                    if (e.getDiff() != null) {
                        e.getDiff().apply(CayenneContextMergeHandler.this);
                    }
                }
            });

            // post event outside of "execute" to make sure it is sent
            repostAfterMerge(e);
        }
    }

    public void graphRolledback(final GraphEvent e) {

        // TODO: andrus, 3/29/2007: per CAY-771, if a LOCAL peer context posted the event,
        // just ignore it, however if the REMOTE peer reverted the parent remote
        // DataContext, we need to invalidate stale committed objects...
    }

    // ******* End DataChannelListener methods *******

    void repostAfterMerge(GraphEvent originalEvent) {
        // though the subject is CHANGE, "merge" events are really lifecycle.
        if (context.isLifecycleEventsEnabled()) {
            context.fireDataChannelChanged(originalEvent.getSource(), originalEvent.getDiff());
        }
    }

    /**
     * Executes merging of the external diff.
     */
    void merge(final GraphDiff diff) {
        runWithEventsDisabled(new Runnable() {

            public void run() {
                diff.apply(CayenneContextMergeHandler.this);
            }
        });
    }

    // ******* GraphChangeHandler methods *********

    public void nodeIdChanged(Object nodeId, Object newId) {
        // do not unregister the node just yet... only put replaced id in deadIds to
        // remove it later. Otherwise stored operations will not work
        Object node = context.internalGraphManager().getNode(nodeId);

        if (node != null) {
            context.internalGraphManager().deadIds().add(nodeId);
            context.internalGraphManager().registerNode(newId, node);

            if (node instanceof Persistent) {
                // inject new id
                ((Persistent) node).setObjectId((ObjectId) newId);
            }
        }
    }

    public void nodeCreated(Object nodeId) {
        // ignore
    }

    public void nodeRemoved(Object nodeId) {
        context.getGraphManager().unregisterNode(nodeId);
    }

    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {

        Object object = context.internalGraphManager().getNode(nodeId);
        if (object != null) {

            // do not override local changes....
            PropertyDescriptor p = propertyForId(nodeId, property);
            if (Util.nullSafeEquals(p.readPropertyDirectly(object), oldValue)) {

                p.writePropertyDirectly(object, oldValue, newValue);
            }
        }
    }

    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        // null source or target likely means the object is not faulted yet... Faults
        // shouldn't get disturbed by adding/removing arcs

        Object source = context.internalGraphManager().getNode(nodeId);
        if (source == null) {
            // no need to connect non-existent object
            return;
        }

        // TODO (Andrus, 10/17/2005) - check for local modifications to avoid
        // overwriting...

        ArcProperty p = (ArcProperty) propertyForId(nodeId, arcId.toString());
        if (p.isFault(source)) {
            return;
        }

        Object target = context.internalGraphManager().getNode(targetNodeId);
        if (target == null) {
            target = context.createFault((ObjectId) targetNodeId);
        }

        try {
            if (p instanceof ToManyProperty) {
                ((ToManyProperty) p).addTargetDirectly(source, target);
            }
            else {
                p.writePropertyDirectly(source, null, target);
            }
        }
        finally {
        }
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {

        // null source or target likely means the object is not faulted yet... Faults
        // shouldn't get disturbed by adding/removing arcs

        Object source = context.internalGraphManager().getNode(nodeId);
        if (source == null) {
            // no need to disconnect non-existent object
            return;
        }

        // (see "TODO" in 'arcCreated')
        ArcProperty p = (ArcProperty) propertyForId(nodeId, arcId.toString());
        if (p.isFault(source)) {
            return;
        }

        Object target = context.internalGraphManager().getNode(targetNodeId);
        if (target == null) {
            target = context.createFault((ObjectId) targetNodeId);
        }

        try {
            if (p instanceof ToManyProperty) {
                ((ToManyProperty) p).removeTargetDirectly(source, target);
            }
            else {
                p.writePropertyDirectly(source, target, null);
            }
        }
        finally {
        }
    }

    private PropertyDescriptor propertyForId(Object nodeId, String propertyName) {
        ClassDescriptor descriptor = context.getEntityResolver().getClassDescriptor(
                ((ObjectId) nodeId).getEntityName());
        return descriptor.getProperty(propertyName);
    }

    // Returns true if this object is active; an event came from our channel, but did not
    // originate in it.
    boolean shouldProcessEvent(GraphEvent e) {
        // only process events that came from our channel, but did not originate in it
        // (i.e. likely posted by EventBridge)
        return active
                && e.getSource() == context.getChannel()
                && e.getPostedBy() != context
                && e.getPostedBy() != context.getChannel();
    }

    // executes a closure, disabling ObjectContext events for the duration of the
    // execution.

    private void runWithEventsDisabled(Runnable closure) {

        synchronized (context.internalGraphManager()) {
            boolean changeEventsEnabled = context.internalGraphManager().changeEventsEnabled;
            context.internalGraphManager().changeEventsEnabled = false;

            boolean lifecycleEventsEnabled = context.internalGraphManager().lifecycleEventsEnabled;
            context.internalGraphManager().lifecycleEventsEnabled = false;

            try {
                closure.run();
            }
            finally {
                context.internalGraphManager().changeEventsEnabled = changeEventsEnabled;
                context.internalGraphManager().lifecycleEventsEnabled = lifecycleEventsEnabled;
            }
        }
    }
}
