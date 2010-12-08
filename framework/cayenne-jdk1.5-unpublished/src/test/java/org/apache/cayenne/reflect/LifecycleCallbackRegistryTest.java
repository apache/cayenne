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
package org.apache.cayenne.reflect;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.annotation.PostAdd;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Exhibit;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.testdo.testmap.annotations.Tag1;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class LifecycleCallbackRegistryTest extends ServerCase {

    @Inject
    private ObjectContext context;

    public void testAddListener_PostAdd() {
        LifecycleCallbackRegistry registry = new LifecycleCallbackRegistry(context
                .getEntityResolver());

        context.getEntityResolver().setCallbackRegistry(registry);

        PostAddListener listener = new PostAddListener();
        registry.addListener(listener);

        assertEquals(3, registry.getHandler(LifecycleEvent.POST_ADD).listenersSize());

        context.newObject(Gallery.class);
        assertEquals("e:Gallery;", listener.getAndReset());

        context.newObject(Artist.class);
        assertEquals("a:Artist;", listener.getAndReset());

        context.newObject(Exhibit.class);
        assertEquals("", listener.getAndReset());

        context.newObject(Painting.class);
        assertEquals("e:Painting;", listener.getAndReset());
    }

    public void testAddListener_PostAdd_InheritedListenerMethods() {
        LifecycleCallbackRegistry registry = new LifecycleCallbackRegistry(context
                .getEntityResolver());

        context.getEntityResolver().setCallbackRegistry(registry);

        PostAddListenerSubclass listener = new PostAddListenerSubclass();
        registry.addListener(listener);

        assertEquals(3, registry.getHandler(LifecycleEvent.POST_ADD).listenersSize());

        context.newObject(Gallery.class);
        assertEquals("e:Gallery;", listener.getAndReset());

        context.newObject(Artist.class);
        assertEquals("a:Artist;", listener.getAndReset());

        context.newObject(Exhibit.class);
        assertEquals("", listener.getAndReset());

        context.newObject(Painting.class);
        assertEquals("e:Painting;", listener.getAndReset());
    }

}

class PostAddListener {

    StringBuilder callbackBuffer = new StringBuilder();

    @PostAdd( {
            Gallery.class, Painting.class
    })
    void postAddEntities(Persistent object) {
        callbackBuffer.append("e:" + object.getObjectId().getEntityName() + ";");
    }

    @PostAdd(entityAnnotations = Tag1.class)
    void postAddAnnotated(Persistent object) {
        callbackBuffer.append("a:" + object.getObjectId().getEntityName() + ";");
    }

    String getAndReset() {
        String v = callbackBuffer.toString();
        callbackBuffer = new StringBuilder();
        return v;
    }
}

class PostAddListenerSubclass extends PostAddListener {

}
