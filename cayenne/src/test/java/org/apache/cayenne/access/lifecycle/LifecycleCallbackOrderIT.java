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

package org.apache.cayenne.access.lifecycle;

import org.apache.cayenne.testdo.lifecycle_callbacks_order.Lifecycle;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LifecycleCallbackOrderIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.LIFECYCLE_CALLBACKS_ORDER_PROJECT);

    @Test
    public void lifecycleCallbackOrder() {
        LifecycleEventsManager lifecycleEventsManager = env.context().getChannel().getLifecycleEventsManager();

        lifecycleEventsManager.prePersist().addListener(
                l -> l.getCallbackBuffer().append("PrePersist;"), Lifecycle.class);
        lifecycleEventsManager.postPersist().addListener(
                l -> l.getCallbackBuffer().append("PostPersist;"), Lifecycle.class);
        lifecycleEventsManager.preUpdate().addListener(
                l -> l.getCallbackBuffer().append("PreUpdate;"), Lifecycle.class);
        lifecycleEventsManager.postUpdate().addListener(
                l -> l.getCallbackBuffer().append("PostUpdate;"), Lifecycle.class);
        lifecycleEventsManager.preRemove().addListener(
                l -> l.getCallbackBuffer().append("PreRemove;"), Lifecycle.class);
        lifecycleEventsManager.postRemove().addListener(
                l -> l.getCallbackBuffer().append("PostRemove;"), Lifecycle.class);

        Lifecycle lifecycle = env.context().newObject(Lifecycle.class);
        env.context().commitChanges();
        assertEquals("validateForInsert;PrePersist;PostPersist;", lifecycle.getCallbackBufferValueAndReset());

        lifecycle.setName("CallbackOrderTest");
        env.context().commitChanges();
        assertEquals("validateForUpdate;PreUpdate;PostUpdate;", lifecycle.getCallbackBufferValueAndReset());

        env.context().deleteObject(lifecycle);
        assertEquals("PreRemove;", lifecycle.getCallbackBuffer().toString());
        env.context().commitChanges();
        assertEquals("PreRemove;validateForDelete;PostRemove;", lifecycle.getCallbackBufferValueAndReset());
    }
}
