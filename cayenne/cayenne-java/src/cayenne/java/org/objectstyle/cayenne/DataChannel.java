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

import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.event.EventSubject;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.query.Query;

/**
 * DataChannel is an abstraction used by ObjectContexts to obtain mapping metadata and
 * access a persistent store. There is rarely a need to use it directly.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public interface DataChannel {

    /**
     * A synchronization type that results in changes from an ObjectContext to be recorded
     * in the parent DataChannel. If the parent is itself an ObjectContext, changes are
     * NOT propagated any further.
     */
    public static final int FLUSH_NOCASCADE_SYNC = 1;

    /**
     * A synchronization type that results in changes from an ObjectContext to be recorded
     * in the parent DataChannel. If the parent is itself an ObjectContext, it is expected
     * to send its own sync message to its parent DataChannel to cascade sycnhronization
     * all the way down the stack.
     */
    public static final int FLUSH_CASCADE_SYNC = 2;

    /**
     * A synchronization type that results in cascading rollback of changes through the
     * DataChannel stack.
     */
    public static final int ROLLBACK_CASCADE_SYNC = 3;

    public static final EventSubject GRAPH_CHANGED_SUBJECT = EventSubject.getSubject(
            DataChannel.class,
            "graphChanged");

    public static final EventSubject GRAPH_FLUSHED_SUBJECT = EventSubject.getSubject(
            DataChannel.class,
            "graphFlushed");

    public static final EventSubject GRAPH_ROLLEDBACK_SUBJECT = EventSubject.getSubject(
            DataChannel.class,
            "graphRolledback");

    /**
     * Returns an EventManager associated with this channel. Channel may return null if
     * EventManager is not available for any reason.
     */
    EventManager getEventManager();

    /**
     * Returns an EntityResolver instance that contains runtime mapping information.
     */
    EntityResolver getEntityResolver();

    /**
     * Executes a query, using provided <em>context</em> to register persistent objects
     * if query returns any objects.
     * 
     * @param originatingContext an ObjectContext that originated the query, used to
     *            register result objects.
     * @return a generic response object that encapsulates result of the execution.
     */
    QueryResponse onQuery(ObjectContext originatingContext, Query query);

    /**
     * Processes synchronization request from a child ObjectContext, returning a GraphDiff
     * that describes changes to objects made on the receiving end as a result of
     * syncronization.
     * @param originatingContext an ObjectContext that initiated the sync. Can be null.
     * @param changes diff from the context that initiated the sync.
     * @param syncType One of {@link #FLUSH_NOCASCADE_SYNC}, {@link #FLUSH_CASCADE_SYNC},
     *            {@link #ROLLBACK_CASCADE_SYNC}.
     */
    GraphDiff onSync(ObjectContext originatingContext, GraphDiff changes, int syncType);
}