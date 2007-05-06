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
package org.objectstyle.cayenne.access;

import org.objectstyle.cayenne.DataChannelListener;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.access.ObjectStore.SnapshotEventDecorator;
import org.objectstyle.cayenne.access.event.SnapshotEvent;
import org.objectstyle.cayenne.graph.GraphChangeHandler;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.graph.GraphEvent;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.property.CollectionProperty;
import org.objectstyle.cayenne.property.Property;
import org.objectstyle.cayenne.property.SingleObjectArcProperty;
import org.objectstyle.cayenne.util.Util;

/**
 * A listener of GraphEvents sent by the DataChannel that merges changes to the
 * DataContext.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
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

        Persistent source = (Persistent) context.getGraphManager().getNode(nodeId);
        if (source != null && source.getPersistenceState() != PersistenceState.HOLLOW) {

            // get target as local object
            Object target = context.localObject((ObjectId) targetNodeId, null);

            Property p = propertyForId(nodeId, arcId.toString());
            if (p instanceof CollectionProperty) {
                ((CollectionProperty) p).addTarget(source, target, false);
            }
            else {
                ((SingleObjectArcProperty) p).setTarget(source, target, false);
            }
        }
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {

        Persistent source = (Persistent) context.getGraphManager().getNode(nodeId);
        if (source != null && source.getPersistenceState() != PersistenceState.HOLLOW) {

            Object target = context.localObject((ObjectId) targetNodeId, null);

            Property p = propertyForId(nodeId, arcId.toString());
            if (p instanceof CollectionProperty) {
                ((CollectionProperty) p).removeTarget(source, target, false);
            }
            else {
                ((SingleObjectArcProperty) p).setTarget(source, null, false);
            }
        }
    }
}
