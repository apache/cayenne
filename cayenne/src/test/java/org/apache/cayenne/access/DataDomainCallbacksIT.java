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
package org.apache.cayenne.access;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.reflect.LifecycleCallbackRegistry;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.*;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataDomainCallbacksIT extends RuntimeCase {

    @Inject
    private EntityResolver resolver;

    @Inject
    private ObjectContext context;

    @Inject
    private ObjectContext context1;

    @Test
    public void testPostLoad() throws Exception {
        LifecycleCallbackRegistry registry = resolver.getCallbackRegistry();

        registry.addCallback(LifecycleEvent.POST_LOAD, Artist.class, "postLoadCallback");
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

        ObjectSelect.query(Artist.class).select(context);
        assertEquals(1, a1.getPostLoaded());
        assertSame(a1, listener.getPublicCalledbackEntity());

        a1.resetCallbackFlags();
        listener.reset();

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

    @Test
    public void testPostLoad_MixedResult() throws Exception {
        LifecycleCallbackRegistry registry = resolver.getCallbackRegistry();

        registry.addCallback(LifecycleEvent.POST_LOAD, Artist.class, "postLoadCallback");
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

        EJBQLQuery q = new EJBQLQuery("select a, a.artistName from Artist a");
        context.performQuery(q);
        assertEquals(1, a1.getPostLoaded());
        assertSame(a1, listener.getPublicCalledbackEntity());
    }

    @Test
    public void testPostLoad_Relationship() throws Exception {
        LifecycleCallbackRegistry registry = resolver.getCallbackRegistry();

        registry.addCallback(LifecycleEvent.POST_LOAD, Artist.class, "postLoadCallback");
        MockCallingBackListener listener = new MockCallingBackListener();
        registry.addListener(
                LifecycleEvent.POST_LOAD,
                Artist.class,
                listener,
                "publicCallback");

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("XX");
        Painting p1 = context.newObject(Painting.class);
        p1.setToArtist(a1);
        p1.setPaintingTitle("XXX");
        context.commitChanges();

        context.invalidateObjects(a1, p1);

        p1 = ObjectSelect.query(Painting.class).select(context).get(0);

        // this should be a hollow object, so no callback just yet
        a1 = p1.getToArtist();
        assertEquals(PersistenceState.HOLLOW, a1.getPersistenceState());
        assertEquals(0, a1.getPostLoaded());
        assertNull(listener.getPublicCalledbackEntity());

        a1.getArtistName();
        assertEquals(1, a1.getPostLoaded());
        assertSame(a1, listener.getPublicCalledbackEntity());
    }

    @Test
    public void testPostLoad_Prefetch() throws Exception {
        LifecycleCallbackRegistry registry = resolver.getCallbackRegistry();

        registry.addCallback(LifecycleEvent.POST_LOAD, Artist.class, "postLoadCallback");
        MockCallingBackListener listener = new MockCallingBackListener();
        registry.addListener(
                LifecycleEvent.POST_LOAD,
                Artist.class,
                listener,
                "publicCallback");

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("XX");
        Painting p1 = context.newObject(Painting.class);
        p1.setToArtist(a1);
        p1.setPaintingTitle("XXX");
        context.commitChanges();

        p1 = ObjectSelect.query(Painting.class)
                .prefetch(Painting.TO_ARTIST.disjoint())
                .select(context).get(0);

        // artist is prefetched here, and a callback must have been invoked
        a1 = p1.getToArtist();
        assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
        assertEquals(1, a1.getPostLoaded());
        assertSame(a1, listener.getPublicCalledbackEntity());
    }

    @Test
    public void testPostLoad_LocalObject() throws Exception {
        LifecycleCallbackRegistry registry = resolver.getCallbackRegistry();

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("XX");
        context.commitChanges();

        registry.addCallback(LifecycleEvent.POST_LOAD, Artist.class, "postLoadCallback");
        MockCallingBackListener listener = new MockCallingBackListener();
        registry.addListener(
                LifecycleEvent.POST_LOAD,
                Artist.class,
                listener,
                "publicCallback");

        Artist a2 = context1.localObject(a1);

        assertEquals(PersistenceState.HOLLOW, a2.getPersistenceState());
        assertEquals(0, a2.getPostLoaded());
        assertNull(listener.getPublicCalledbackEntity());

        a2.getArtistName();
        assertEquals(1, a2.getPostLoaded());
        assertSame(a2, listener.getPublicCalledbackEntity());
    }

    @Test
    public void testPostLoad_ThatModifiesObject() {
        LifecycleCallbackRegistry registry = resolver.getCallbackRegistry();

        registry.addCallback(LifecycleEvent.POST_LOAD, Artist.class, "postLoadCallback");
        MockCallingBackListener listener = new MockCallingBackListener() {
        	@Override
        	public void publicCallback(Object entity) {
        		super.publicCallback(entity);
        		Artist a = (Artist)entity;
        		a.setArtistName("Changed"); // modify object during postLoad callback
        	}
        };
        registry.addListener(
                LifecycleEvent.POST_LOAD,
                Artist.class,
                listener,
                "publicCallback");

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("XX");
        Painting p1 = context.newObject(Painting.class);
        p1.setToArtist(a1);
        p1.setPaintingTitle("XXX");
        context.commitChanges();

        context.invalidateObjects(a1, p1);

        p1 = ObjectSelect.query(Painting.class)
                .select(context1).get(0);

        // this should be a hollow object, so no callback just yet
        a1 = p1.getToArtist();
        assertEquals(PersistenceState.HOLLOW, a1.getPersistenceState());
        assertEquals(0, a1.getPostLoaded());
        assertNull(listener.getPublicCalledbackEntity());

        a1.getArtistName();
        assertTrue(a1.getPostLoaded() > 0);
        assertSame(a1, listener.getPublicCalledbackEntity());
    }

    @Test
    public void testPreUpdate() {

        LifecycleCallbackRegistry registry = resolver.getCallbackRegistry();

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("XX");
        context.commitChanges();
        assertFalse(a1.isPreUpdated());

        a1.setArtistName("YY");
        context.commitChanges();
        assertFalse(a1.isPreUpdated());

        registry
                .addCallback(LifecycleEvent.PRE_UPDATE, Artist.class, "preUpdateCallback");
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

    @Test
    public void testPostUpdate() {

        LifecycleCallbackRegistry registry = resolver.getCallbackRegistry();

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("XX");
        context.commitChanges();
        assertFalse(a1.isPostUpdated());

        a1.setArtistName("YY");
        context.commitChanges();
        assertFalse(a1.isPostUpdated());

        registry.addCallback(
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

    @Test
    public void testPostRemove() {

        LifecycleCallbackRegistry registry = resolver.getCallbackRegistry();

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("XX");
        context.commitChanges();

        registry.addCallback(
                LifecycleEvent.POST_REMOVE,
                Artist.class,
                "postRemoveCallback");
        MockCallingBackListener listener2 = new MockCallingBackListener();
        registry.addListener(
                LifecycleEvent.POST_REMOVE,
                Artist.class,
                listener2,
                "publicCallback");

        context.deleteObjects(a1);
        context.commitChanges();

        assertTrue(a1.isPostRemoved());
        assertSame(a1, listener2.getPublicCalledbackEntity());
    }

    @Test
    public void testPostRemove_UpdatedDeleted() {

        LifecycleCallbackRegistry registry = resolver.getCallbackRegistry();

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("XX");
        context.commitChanges();

        MockCallingBackListener listener1 = new MockCallingBackListener();
        registry.addListener(
                LifecycleEvent.POST_REMOVE,
                Artist.class,
                listener1,
                "publicCallback");

        MockCallingBackListener listener2 = new MockCallingBackListener();
        registry.addListener(
                LifecycleEvent.POST_UPDATE,
                Artist.class,
                listener2,
                "publicCallback");

        // change before removing
        a1.setArtistName("YY");
        context.deleteObjects(a1);
        context.commitChanges();

        assertNull(listener2.getPublicCalledbackEntity());
        assertSame(a1, listener1.getPublicCalledbackEntity());
    }

    @Test
    public void testPostRemove_InsertedUpdatedDeleted() {

        LifecycleCallbackRegistry registry = resolver.getCallbackRegistry();

        MockCallingBackListener listener0 = new MockCallingBackListener();
        registry.addListener(
                LifecycleEvent.POST_PERSIST,
                Artist.class,
                listener0,
                "publicCallback");

        MockCallingBackListener listener1 = new MockCallingBackListener();
        registry.addListener(
                LifecycleEvent.POST_REMOVE,
                Artist.class,
                listener1,
                "publicCallback");

        MockCallingBackListener listener2 = new MockCallingBackListener();
        registry.addListener(
                LifecycleEvent.POST_UPDATE,
                Artist.class,
                listener2,
                "publicCallback");

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("XX");
        context.deleteObjects(a1);
        context.commitChanges();

        assertNull(listener0.getPublicCalledbackEntity());
        assertNull(listener1.getPublicCalledbackEntity());
        assertNull(listener2.getPublicCalledbackEntity());
    }

    @Test
    public void testPostPersist() {

        LifecycleCallbackRegistry registry = resolver.getCallbackRegistry();

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("XX");
        context.commitChanges();
        assertFalse(a1.isPostPersisted());

        registry.addCallback(
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
