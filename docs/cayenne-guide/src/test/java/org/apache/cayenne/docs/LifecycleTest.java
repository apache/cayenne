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

import org.apache.cayenne.configuration.runtime.CoreModule;
import org.apache.cayenne.docs.lifecycle.CommittedObjectCounter;
import org.apache.cayenne.docs.lifecycle.Listener1;
import org.apache.cayenne.docs.lifecycle.Listener2;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class LifecycleTest {

    @Test
    public void listeners() {
        // tag::listeners[]
        CayenneRuntime runtime = CayenneRuntime.of()
                // ..
                .addModule(binder ->
                        CoreModule.extend(binder)
                                .addListenerType(Listener1.class)
                                .addListener(new Listener2())
                )
                // ..
                .build();
        // end::listeners[]

        assertNotNull(runtime);
        runtime.shutdown();
    }

    @Test
    public void syncFilter() {
        // tag::syncFilter[]
        // this will also add filter as a listener
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
