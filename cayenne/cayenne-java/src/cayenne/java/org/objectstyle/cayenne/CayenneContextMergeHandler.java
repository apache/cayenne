/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne;

import org.objectstyle.cayenne.graph.GraphChangeHandler;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.graph.GraphEvent;
import org.objectstyle.cayenne.property.ArcProperty;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.property.Property;
import org.objectstyle.cayenne.util.Util;

/**
 * An object that merges "backdoor" modifications of the object graph coming from the
 * underlying DataChannel. When doing an update, CayenneContextMergeHandler blocks
 * broadcasting of GraphManager events.
 * 
 * @since 1.2
 * @author Andrus Adamchik
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
            final boolean hadChanges = context.internalGraphManager().hasChanges();

            runWithEventsDisabled(new Runnable() {

                public void run() {

                    if (e.getDiff() != null) {
                        e.getDiff().apply(CayenneContextMergeHandler.this);
                    }

                    if (!hadChanges) {
                        context.internalGraphManager().stateLog.graphCommitted();
                        context.internalGraphManager().reset();
                    }
                }
            });

            // post event outside of "execute" to make sure it is sent
            repostAfterMerge(e);
        }
    }

    public void graphRolledback(final GraphEvent e) {

        if (shouldProcessEvent(e)) {

            // do we need to merge anything?
            if (context.internalGraphManager().hasChanges()) {
                runWithEventsDisabled(new Runnable() {

                    public void run() {
                        context.internalGraphManager().graphReverted();
                    }
                });

                // post event outside of "execute" to make sure it is sent
                repostAfterMerge(e);
            }
        }
    }

    // ******* End DataChannelListener methods *******

    void repostAfterMerge(GraphEvent originalEvent) {
        // though the subject is CHANGE, "merge" events are really lifecycle.
        if (context.isLifecycleEventsEnabled()) {
            context.internalGraphManager().send(
                    originalEvent.getDiff(),
                    DataChannel.GRAPH_CHANGED_SUBJECT,
                    originalEvent.getSource());
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
        context.createNewObject((ObjectId) nodeId);
    }

    public void nodeRemoved(Object nodeId) {
        Object object = context.internalGraphManager().getNode(nodeId);
        if (object != null) {
            context.deleteObject((Persistent) object);
        }
    }

    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {

        Object object = context.internalGraphManager().getNode(nodeId);
        if (object != null) {

            // do not override local changes....
            Property p = propertyForId(nodeId, property);
            if (Util.nullSafeEquals(p.readPropertyDirectly(object), oldValue)) {

                p.writePropertyDirectly(object, oldValue, newValue);
                context.internalGraphAction().handleSimplePropertyChange(
                        (Persistent) object,
                        property,
                        oldValue,
                        newValue);
            }
        }
    }

    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        // null source or target likely means the object is not faulted yet... Faults
        // shouldn't get disturbed by adding/removing arcs

        Object source = context.internalGraphManager().getNode(nodeId);
        if (source == null) {
            source = context.createFault((ObjectId) nodeId);
        }

        Object target = context.internalGraphManager().getNode(targetNodeId);
        if (target == null) {
            target = context.createFault((ObjectId) targetNodeId);
        }

        // TODO (Andrus, 10/17/2005) - check for local modifications to avoid
        // overwriting...

        ArcProperty p = (ArcProperty) propertyForId(nodeId, arcId.toString());
       
        try {
            context.internalGraphAction().handleArcPropertyChange(
                    (Persistent) source,
                    p,
                    null,
                    target);
        }
        finally {
            context.internalGraphAction().setArcChangeInProcess(false);
        }
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {

        // null source or target likely means the object is not faulted yet... Faults
        // shouldn't get disturbed by adding/removing arcs

        Object source = context.internalGraphManager().getNode(nodeId);
        if (source == null) {
            source = context.createFault((ObjectId) nodeId);
        }

        Object target = context.internalGraphManager().getNode(targetNodeId);
        if (target == null) {
            target = context.createFault((ObjectId) targetNodeId);
        }

        // (see "TODO" in 'arcCreated')
        ArcProperty p = (ArcProperty) propertyForId(nodeId, arcId.toString());
        p.writePropertyDirectly(source, target, null);

        try {
            context.internalGraphAction().handleArcPropertyChange(
                    (Persistent) source,
                    p,
                    target,
                    null);
        }
        finally {
            context.internalGraphAction().setArcChangeInProcess(false);
        }
    }

    private Property propertyForId(Object nodeId, String propertyName) {
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
