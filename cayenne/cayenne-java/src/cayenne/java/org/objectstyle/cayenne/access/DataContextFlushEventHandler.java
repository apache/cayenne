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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.access.event.DataContextEvent;
import org.objectstyle.cayenne.access.event.DataContextTransactionEventListener;
import org.objectstyle.cayenne.access.event.DataObjectTransactionEventListener;
import org.objectstyle.cayenne.event.EventManager;

/**
 * Handles DataContext events on domain flush.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class DataContextFlushEventHandler implements DataContextTransactionEventListener {

    List objectsToNotify;

    DataContext originatingContext;

    DataContextFlushEventHandler(DataContext originatingContext) {
        this.originatingContext = originatingContext;
        this.objectsToNotify = new ArrayList();

        // remember objects to notify of commit events

        Iterator it = originatingContext.getObjectStore().getObjectIterator();
        while (it.hasNext()) {
            Persistent object = (Persistent) it.next();
            if (object instanceof DataObjectTransactionEventListener) {
                switch (object.getPersistenceState()) {
                    case PersistenceState.NEW:
                    case PersistenceState.MODIFIED:
                    case PersistenceState.DELETED:
                        this.objectsToNotify.add(object);
                        break;
                }
            }
        }
    }

    void registerForDataContextEvents() {
        EventManager eventManager = originatingContext.getEventManager();
        eventManager.addListener(
                this,
                "dataContextWillCommit",
                DataContextEvent.class,
                DataContext.WILL_COMMIT,
                originatingContext);
        eventManager.addListener(
                this,
                "dataContextDidCommit",
                DataContextEvent.class,
                DataContext.DID_COMMIT,
                originatingContext);
        eventManager.addListener(
                this,
                "dataContextDidRollback",
                DataContextEvent.class,
                DataContext.DID_ROLLBACK,
                originatingContext);
    }

    void unregisterFromDataContextEvents() {
        EventManager eventManager = originatingContext.getEventManager();
        eventManager.removeListener(this, DataContext.WILL_COMMIT);
        eventManager.removeListener(this, DataContext.DID_COMMIT);
        eventManager.removeListener(this, DataContext.DID_ROLLBACK);
    }

    public void dataContextWillCommit(DataContextEvent event) {
        Iterator iter = objectsToNotify.iterator();
        while (iter.hasNext()) {
            ((DataObjectTransactionEventListener) iter.next()).willCommit(event);
        }
    }

    public void dataContextDidCommit(DataContextEvent event) {
        Iterator iter = objectsToNotify.iterator();
        while (iter.hasNext()) {
            ((DataObjectTransactionEventListener) iter.next()).didCommit(event);
        }
    }

    public void dataContextDidRollback(DataContextEvent event) {
        // do nothing for now
    }
}
