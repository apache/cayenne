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

package org.apache.cayenne.reflect;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.annotation.PostPersist;
import org.apache.cayenne.annotation.PostRemove;
import org.apache.cayenne.annotation.PostUpdate;
import org.apache.cayenne.annotation.PrePersist;
import org.apache.cayenne.annotation.PreRemove;
import org.apache.cayenne.annotation.PreUpdate;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.lifecycle_callbacks_order.Lifecycle;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UseCayenneRuntime(CayenneProjects.LIFECYCLE_CALLBACKS_ORDER_PROJECT)
public class LifecycleCallbackOrderIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Test
    public void testLifecycleCallbackOrder() {
        LifecycleCallbackRegistry registry = new LifecycleCallbackRegistry(context.getEntityResolver());
        context.getEntityResolver().setCallbackRegistry(registry);

        LifecycleEventListener eventListener = new LifecycleEventListener();
        registry.addListener(eventListener);

        Lifecycle lifecycle = context.newObject(Lifecycle.class);
        context.commitChanges();
        assertEquals("validateForInsert;PrePersist;PostPersist;", lifecycle.getCallbackBufferValueAndReset());

        lifecycle.setName("CallbackOrderTest");
        context.commitChanges();
        assertEquals("validateForUpdate;PreUpdate;PostUpdate;", lifecycle.getCallbackBufferValueAndReset());

        context.deleteObject(lifecycle);
        assertEquals("PreRemove;", lifecycle.getCallbackBuffer().toString());
        context.commitChanges();
        assertEquals("PreRemove;validateForDelete;PostRemove;", lifecycle.getCallbackBufferValueAndReset());
    }

    class LifecycleEventListener {

        @PrePersist(Lifecycle.class)
        void prePersist(Lifecycle lifecycle) {
            lifecycle.getCallbackBuffer().append("PrePersist;");
        }

        @PostPersist(Lifecycle.class)
        void postPersist(Lifecycle lifecycle) {
            lifecycle.getCallbackBuffer().append("PostPersist;");
        }

        @PreUpdate(Lifecycle.class)
        void preUpdate(Lifecycle lifecycle) {
            lifecycle.getCallbackBuffer().append("PreUpdate;");
        }

        @PostUpdate(Lifecycle.class)
        void postUpdate(Lifecycle lifecycle) {
            lifecycle.getCallbackBuffer().append("PostUpdate;");
        }

        @PreRemove(Lifecycle.class)
        void preRemove(Lifecycle lifecycle) {
            lifecycle.getCallbackBuffer().append("PreRemove;");
        }

        @PostRemove(Lifecycle.class)
        void postRemove(Lifecycle lifecycle) {
            lifecycle.getCallbackBuffer().append("PostRemove;");
        }

    }

}
