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
import org.apache.cayenne.Persistent;
import org.apache.cayenne.annotation.PostAdd;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.testdo.inheritance_flat.Group;
import org.apache.cayenne.testdo.inheritance_flat.Role;
import org.apache.cayenne.testdo.inheritance_flat.User;
import org.apache.cayenne.testdo.inheritance_flat.UserProperties;
import org.apache.cayenne.testdo.testmap.annotations.Tag2;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UseCayenneRuntime(CayenneProjects.INHERITANCE_SINGLE_TABLE1_PROJECT)
public class LifecycleCallbackRegistry_InheritanceIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Test
    public void testAddListener_PostAdd_EntityInheritance() {
        LifecycleCallbackRegistry registry = new LifecycleCallbackRegistry(context
                .getEntityResolver());

        context.getEntityResolver().setCallbackRegistry(registry);

        PostAddListenerInherited listener = new PostAddListenerInherited();
        registry.addListener(listener);

        assertEquals(1, registry.getHandler(LifecycleEvent.POST_ADD).listenersSize());

        context.newObject(User.class);
        assertEquals("a:User;", listener.getAndReset());

        context.newObject(Group.class);
        assertEquals("a:Group;", listener.getAndReset());

        context.newObject(Role.class);
        assertEquals("", listener.getAndReset());

        context.newObject(UserProperties.class);
        assertEquals("", listener.getAndReset());
    }
}

class PostAddListenerInherited {

    StringBuilder callbackBuffer = new StringBuilder();

    @PostAdd(entityAnnotations = Tag2.class)
    void postAddAnnotated(Persistent object) {
        callbackBuffer.append("a:" + object.getObjectId().getEntityName() + ";");
    }

    String getAndReset() {
        String v = callbackBuffer.toString();
        callbackBuffer = new StringBuilder();
        return v;
    }
}
