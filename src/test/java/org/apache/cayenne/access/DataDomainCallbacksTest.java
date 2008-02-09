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

import org.apache.art.Artist;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.reflect.LifecycleCallbackRegistry;
import org.apache.cayenne.unit.CayenneCase;

public class DataDomainCallbacksTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        deleteTestData();
    }

    @Override
    protected void tearDown() throws Exception {
        EntityResolver resolver = getDomain().getEntityResolver();
        resolver.getCallbackRegistry().clear();
    }

    public void testPostLoad() throws Exception {
        LifecycleCallbackRegistry registry = getDomain()
                .getEntityResolver()
                .getCallbackRegistry();

        ObjectContext context = createDataContext();

        registry.addListener(LifecycleEvent.POST_LOAD, Artist.class, "postLoadCallback");
        MockCallingBackListener listener = new MockCallingBackListener();
        registry.addListener(
                LifecycleEvent.POST_LOAD,
                Artist.class,
                listener,
                "publicCallback");

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("XX");
        context.commitChanges();
        assertEquals(0, a1.getPostLoaded());
        assertNull(listener.getPublicCalledbackEntity());

        SelectQuery q = new SelectQuery(Artist.class);
        context.performQuery(q);
        assertEquals(1, a1.getPostLoaded());
        assertSame(a1, listener.getPublicCalledbackEntity());

        a1.resetCallbackFlags();
        listener.reset();

        // TODO: andrus, 9/21/2006 - this fails as "postLoad" is called when query
        // refresh flag is set to false and object is already there.
        // q.setRefreshingObjects(false);
        //        
        // assertFalse(a1.isPostLoaded());
        // context.performQuery(q);
        // assertFalse(a1.isPostLoaded());
        // assertNull(listener.getPublicCalledbackEntity());

        // post load must be called on rollback...
        a1.resetCallbackFlags();
        listener.reset();

        context.rollbackChanges();
        assertEquals(0, a1.getPostLoaded());
        assertNull(listener.getPublicCalledbackEntity());

        // now change and rollback the artist - postLoad must be called
        a1.setArtistName("YY");
        context.rollbackChanges();
        assertEquals(1, a1.getPostLoaded());
        assertSame(a1, listener.getPublicCalledbackEntity());

        // test invalidated
        a1.resetCallbackFlags();
        listener.reset();
        assertEquals(0, a1.getPostLoaded());
        assertNull(listener.getPublicCalledbackEntity());

        context.performQuery(new RefreshQuery(a1));
        assertEquals(0, a1.getPostLoaded());
        assertNull(listener.getPublicCalledbackEntity());

        a1.getArtistName();
        assertEquals(1, a1.getPostLoaded());
        assertSame(a1, listener.getPublicCalledbackEntity());
    }

    public void testPreUpdate() {

        LifecycleCallbackRegistry registry = getDomain()
                .getEntityResolver()
                .getCallbackRegistry();

        ObjectContext context = createDataContext();

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("XX");
        context.commitChanges();
        assertFalse(a1.isPreUpdated());

        a1.setArtistName("YY");
        context.commitChanges();
        assertFalse(a1.isPreUpdated());

        registry
                .addListener(LifecycleEvent.PRE_UPDATE, Artist.class, "preUpdateCallback");
        a1.setArtistName("ZZ");
        context.commitChanges();
        assertTrue(a1.isPreUpdated());

        a1.resetCallbackFlags();
        assertFalse(a1.isPreUpdated());

        MockCallingBackListener listener2 = new MockCallingBackListener();
        registry.addListener(
                LifecycleEvent.PRE_UPDATE,
                Artist.class,
                listener2,
                "publicCallback");

        a1.setArtistName("AA");
        context.commitChanges();

        assertTrue(a1.isPreUpdated());
        assertSame(a1, listener2.getPublicCalledbackEntity());
    }

    public void testPostUpdate() {

        LifecycleCallbackRegistry registry = getDomain()
                .getEntityResolver()
                .getCallbackRegistry();

        ObjectContext context = createDataContext();

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("XX");
        context.commitChanges();
        assertFalse(a1.isPostUpdated());

        a1.setArtistName("YY");
        context.commitChanges();
        assertFalse(a1.isPostUpdated());

        registry.addListener(
                LifecycleEvent.POST_UPDATE,
                Artist.class,
                "postUpdateCallback");
        a1.setArtistName("ZZ");
        context.commitChanges();
        assertTrue(a1.isPostUpdated());

        a1.resetCallbackFlags();
        assertFalse(a1.isPostUpdated());

        MockCallingBackListener listener2 = new MockCallingBackListener();
        registry.addListener(
                LifecycleEvent.POST_UPDATE,
                Artist.class,
                listener2,
                "publicCallback");

        a1.setArtistName("AA");
        context.commitChanges();

        assertTrue(a1.isPostUpdated());
        assertSame(a1, listener2.getPublicCalledbackEntity());
    }

    public void testPostRemove() {

        LifecycleCallbackRegistry registry = getDomain()
                .getEntityResolver()
                .getCallbackRegistry();

        ObjectContext context = createDataContext();

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("XX");
        context.commitChanges();

        registry.addListener(
                LifecycleEvent.POST_REMOVE,
                Artist.class,
                "postRemoveCallback");
        MockCallingBackListener listener2 = new MockCallingBackListener();
        registry.addListener(
                LifecycleEvent.POST_REMOVE,
                Artist.class,
                listener2,
                "publicCallback");

        context.deleteObject(a1);
        context.commitChanges();

        assertTrue(a1.isPostRemoved());
        assertSame(a1, listener2.getPublicCalledbackEntity());
    }

    public void testPostPersist() {

        LifecycleCallbackRegistry registry = getDomain()
                .getEntityResolver()
                .getCallbackRegistry();

        ObjectContext context = createDataContext();
        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("XX");
        context.commitChanges();
        assertFalse(a1.isPostPersisted());

        registry.addListener(
                LifecycleEvent.POST_PERSIST,
                Artist.class,
                "postPersistCallback");
        MockCallingBackListener listener2 = new MockCallingBackListener() {

            @Override
            public void publicCallback(Object entity) {
                super.publicCallback(entity);
                assertFalse(((Persistent) entity).getObjectId().isTemporary());
            }
        };
        registry.addListener(
                LifecycleEvent.POST_PERSIST,
                Artist.class,
                listener2,
                "publicCallback");

        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("XX");
        context.commitChanges();

        assertFalse(a1.isPostPersisted());
        assertTrue(a2.isPostPersisted());
        assertSame(a2, listener2.getPublicCalledbackEntity());
    }
}
