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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.event.DataContextEvent;
import org.apache.cayenne.access.event.DataContextTransactionEventListener;
import org.apache.cayenne.access.event.DataObjectTransactionEventListener;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.map.LifecycleEventCallback;

/**
 * Handles DataContext events on domain flush.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 * @deprecated since 3.0M1 in favor of {@link LifecycleEventCallback}. Will be removed in
 *             later 3.0 milestones.
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
