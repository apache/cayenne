/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.docs;

import org.apache.cayenne.CayenneRuntime;
import org.apache.cayenne.access.lifecycle.LifecycleEventsManager;
import org.apache.cayenne.CoreModule;
import org.apache.cayenne.docs.lifecycle.CommittedObjectCounter;
import org.apache.cayenne.docs.lifecycle.Order;
import org.apache.cayenne.docs.lifecycle.OrderListener;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class LifecycleTest {

    @Test
    public void listeners() {
        // tag::listeners[]
        OrderListener listener = new OrderListener();

        CayenneRuntime runtime = CayenneRuntime.of()
                // ..
                .addModule(binder ->
                        CoreModule.extend(binder)
                                // listener of an event on one entity (and its subclasses)
                                .addPostAddListener(listener::orderCreated, Order.class)
                                // listener of an event on all entities
                                .addPostPersistListener(listener::objectCommitted)
                                .addPostUpdateListener(listener::objectCommitted)
                )
                // ..
                .build();
        // end::listeners[]

        assertNotNull(runtime);
        runtime.shutdown();
    }

    @Test
    public void runtimeRegistration() {
        CayenneRuntime runtime = CayenneRuntime.of().build();
        OrderListener listener = new OrderListener();

        // tag::runtimeRegistration[]
        runtime.getDataDomain().getLifecycleEventsManager()
                .postAdd().addListener(listener::orderCreated, Order.class);
        // end::runtimeRegistration[]

        runtime.shutdown();
    }

    @Test
    public void syncFilter() {
        // tag::syncFilter[]
        // this will also register the filter's listeners
        CayenneRuntime runtime = CayenneRuntime.of()
                // ..
                .addModule(b ->
                        CoreModule.extend(b)
                                .addSyncFilter(CommittedObjectCounter.class)
                )
                // ..
                .build();
        // end::syncFilter[]

        assertNotNull(runtime);
        runtime.shutdown();
    }
}
