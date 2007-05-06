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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.event.EventSubject;
import org.objectstyle.cayenne.graph.ArcCreateOperation;
import org.objectstyle.cayenne.graph.ArcDeleteOperation;
import org.objectstyle.cayenne.graph.GraphChangeHandler;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.graph.GraphEvent;
import org.objectstyle.cayenne.graph.GraphMap;
import org.objectstyle.cayenne.graph.NodeCreateOperation;
import org.objectstyle.cayenne.graph.NodeDeleteOperation;
import org.objectstyle.cayenne.graph.NodeIdChangeOperation;
import org.objectstyle.cayenne.graph.NodePropertyChangeOperation;

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
    Collection deadIds;
    boolean changeEventsEnabled;
    boolean lifecycleEventsEnabled;

    ObjectContextStateLog stateLog;
    ObjectContextChangeLog changeLog;

    Map queryResultMap = new HashMap();

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

    synchronized void cacheQueryResult(String name, List results) {
        queryResultMap.put(name, results);
    }

    synchronized List getCachedQueryResult(String name) {
        return (List) queryResultMap.get(name);
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

    Collection dirtyNodes() {
        return stateLog.dirtyNodes();
    }

    Collection dirtyNodes(int state) {
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
            Iterator it = deadIds.iterator();
            while (it.hasNext()) {
                nodes.remove(it.next());
            }

            deadIds = null;
        }
    }

    Collection deadIds() {
        if (deadIds == null) {
            deadIds = new ArrayList();
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
