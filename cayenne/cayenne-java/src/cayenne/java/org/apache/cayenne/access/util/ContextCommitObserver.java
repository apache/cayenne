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

package org.apache.cayenne.access.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.event.DataContextEvent;
import org.apache.cayenne.access.event.DataContextTransactionEventListener;
import org.apache.cayenne.access.event.DataObjectTransactionEventListener;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.util.Util;

/**
 * ContextCommitObserver is used as an observer for DataContext 
 * commit operations.
 * 
 * @deprecated Unused since 1.2
 * @author Andrei Adamchik
 */
public class ContextCommitObserver
    extends DefaultOperationObserver
    implements DataContextTransactionEventListener {

    protected List updObjects;
    protected List delObjects;
    protected List insObjects;
    protected List objectsToNotify;

    protected DataContext context;

    public ContextCommitObserver(
        Level logLevel,
        DataContext context,
        List insObjects,
        List updObjects,
        List delObjects) {
            
        super.setLoggingLevel(logLevel);

        this.context = context;
        this.insObjects = insObjects;
        this.updObjects = updObjects;
        this.delObjects = delObjects;
        this.objectsToNotify = new ArrayList();

        // Build a list of objects that need to be notified about posted
        // DataContext events. When notifying about a successful completion
        // of a transaction we cannot build this list anymore, since all
        // the work will be done by then.
        Iterator collIter =
            (Arrays.asList(new List[] { delObjects, updObjects, insObjects }))
                .iterator();
        while (collIter.hasNext()) {
            Iterator objIter = ((Collection) collIter.next()).iterator();
            while (objIter.hasNext()) {
                Object element = objIter.next();
                if (element instanceof DataObjectTransactionEventListener) {
                    this.objectsToNotify.add(element);
                }
            }
        }
    }

    public void nextQueryException(Query query, Exception ex) {
        super.nextQueryException(query, ex);
        throw new CayenneRuntimeException(
            "Raising from query exception.",
            Util.unwindException(ex));
    }

    public void nextGlobalException(Exception ex) {
        super.nextGlobalException(ex);
        throw new CayenneRuntimeException(
            "Raising from underlyingQueryEngine exception.",
            Util.unwindException(ex));
    }

    public void registerForDataContextEvents() {
        EventManager mgr = context.getEventManager();
        mgr.addListener(
                this,
                "dataContextWillCommit",
                DataContextEvent.class,
                DataContext.WILL_COMMIT,
                this.context);
        mgr.addListener(
                this,
                "dataContextDidCommit",
                DataContextEvent.class,
                DataContext.DID_COMMIT,
                this.context);
        mgr.addListener(
                this,
                "dataContextDidRollback",
                DataContextEvent.class,
                DataContext.DID_ROLLBACK,
                this.context);
    }

    public void unregisterFromDataContextEvents() {
        EventManager mgr = context.getEventManager();
        mgr.removeListener(this, DataContext.WILL_COMMIT);
        mgr.removeListener(this, DataContext.DID_COMMIT);
        mgr.removeListener(this, DataContext.DID_ROLLBACK);
    }

    public void dataContextWillCommit(DataContextEvent event) {
        Iterator iter = objectsToNotify.iterator();
        while (iter.hasNext()) {
            ((DataObjectTransactionEventListener) iter.next()).willCommit(
                event);
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
