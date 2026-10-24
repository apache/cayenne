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
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.access.lifecycle.LifecycleEventsManager;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

public class DataDomainCallbacksIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private ObjectContext context1;

    @BeforeEach
    public void setUp() {
        context1 = env.runtime().newContext();
    }

    @Test
    public void postLoad() throws Exception {
        LifecycleEventsManager lifecycleEventsManager = env.runtime().getDataDomain().getLifecycleEventsManager();

        lifecycleEventsManager.postLoad().addListener(Artist::postLoadCallback, Artist.class);
        MockCallingBackListener listener = new MockCallingBackListener();
        lifecycleEventsManager.postLoad().addListener(listener::publicCallback, Artist.class);

        Artist a1 = env.context().newObject(Artist.class);
        a1.setArtistName("XX");
        env.context().commitChanges();
        assertEquals(0, a1.getPostLoaded());
        assertNull(listener.getPublicCalledbackEntity());

        ObjectSelect.query(Artist.class).select(env.context());
        assertEquals(1, a1.getPostLoaded());
        assertSame(a1, listener.getPublicCalledbackEntity());

        a1.resetCallbackFlags();
        listener.reset();

        // post load must be called on rollback...
        a1.resetCallbackFlags();
        listener.reset();

        env.context().rollbackChanges();
        assertEquals(0, a1.getPostLoaded());
        assertNull(listener.getPublicCalledbackEntity());

        // now change and rollback the artist - postLoad must be called
        a1.setArtistName("YY");
        env.context().rollbackChanges();
        assertEquals(1, a1.getPostLoaded());
        assertSame(a1, listener.getPublicCalledbackEntity());

        // test invalidated
        a1.resetCallbackFlags();
        listener.reset();
        assertEquals(0, a1.getPostLoaded());
        assertNull(listener.getPublicCalledbackEntity());

        env.context().invalidateObjects(a1);
        assertEquals(0, a1.getPostLoaded());
        assertNull(listener.getPublicCalledbackEntity());

        a1.getArtistName();
        assertEquals(1, a1.getPostLoaded());
        assertSame(a1, listener.getPublicCalledbackEntity());
    }

    @Test
    public void postLoad_MixedResult() throws Exception {
        LifecycleEventsManager lifecycleEventsManager = env.runtime().getDataDomain().getLifecycleEventsManager();

        lifecycleEventsManager.postLoad().addListener(Artist::postLoadCallback, Artist.class);
        MockCallingBackListener listener = new MockCallingBackListener();
        lifecycleEventsManager.postLoad().addListener(listener::publicCallback, Artist.class);

        Artist a1 = env.context().newObject(Artist.class);
        a1.setArtistName("XX");
        env.context().commitChanges();
        assertEquals(0, a1.getPostLoaded());
        assertNull(listener.getPublicCalledbackEntity());

        ObjectSelect.query(Artist.class).columns(Artist.SELF, Artist.ARTIST_NAME).select(env.context());
        assertEquals(1, a1.getPostLoaded());
        assertSame(a1, listener.getPublicCalledbackEntity());
    }

    @Test
    public void postLoad_Relationship() throws Exception {
        LifecycleEventsManager lifecycleEventsManager = env.runtime().getDataDomain().getLifecycleEventsManager();

        lifecycleEventsManager.postLoad().addListener(Artist::postLoadCallback, Artist.class);
        MockCallingBackListener listener = new MockCallingBackListener();
        lifecycleEventsManager.postLoad().addListener(listener::publicCallback, Artist.class);

        Artist a1 = env.context().newObject(Artist.class);
        a1.setArtistName("XX");
        Painting p1 = env.context().newObject(Painting.class);
        p1.setToArtist(a1);
        p1.setPaintingTitle("XXX");
        env.context().commitChanges();

        env.context().invalidateObjects(a1, p1);

        p1 = ObjectSelect.query(Painting.class).select(env.context()).get(0);

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
    public void postLoad_Prefetch() throws Exception {
        LifecycleEventsManager lifecycleEventsManager = env.runtime().getDataDomain().getLifecycleEventsManager();

        lifecycleEventsManager.postLoad().addListener(Artist::postLoadCallback, Artist.class);
        MockCallingBackListener listener = new MockCallingBackListener();
        lifecycleEventsManager.postLoad().addListener(listener::publicCallback, Artist.class);

        Artist a1 = env.context().newObject(Artist.class);
        a1.setArtistName("XX");
        Painting p1 = env.context().newObject(Painting.class);
        p1.setToArtist(a1);
        p1.setPaintingTitle("XXX");
        env.context().commitChanges();

        p1 = ObjectSelect.query(Painting.class)
                .prefetch(Painting.TO_ARTIST.disjoint())
                .select(env.context()).get(0);

        // artist is prefetched here, and a callback must have been invoked
        a1 = p1.getToArtist();
        assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
        assertEquals(1, a1.getPostLoaded());
        assertSame(a1, listener.getPublicCalledbackEntity());
    }

    @Test
    public void postLoad_LocalObject() throws Exception {
        LifecycleEventsManager lifecycleEventsManager = env.runtime().getDataDomain().getLifecycleEventsManager();

        Artist a1 = env.context().newObject(Artist.class);
        a1.setArtistName("XX");
        env.context().commitChanges();

        lifecycleEventsManager.postLoad().addListener(Artist::postLoadCallback, Artist.class);
        MockCallingBackListener listener = new MockCallingBackListener();
        lifecycleEventsManager.postLoad().addListener(listener::publicCallback, Artist.class);

        Artist a2 = context1.localObject(a1);

        assertEquals(PersistenceState.HOLLOW, a2.getPersistenceState());
        assertEquals(0, a2.getPostLoaded());
        assertNull(listener.getPublicCalledbackEntity());

        a2.getArtistName();
        assertEquals(1, a2.getPostLoaded());
        assertSame(a2, listener.getPublicCalledbackEntity());
    }

    @Test
    public void postLoad_ThatModifiesObject() {
        LifecycleEventsManager lifecycleEventsManager = env.runtime().getDataDomain().getLifecycleEventsManager();

        lifecycleEventsManager.postLoad().addListener(Artist::postLoadCallback, Artist.class);
        MockCallingBackListener listener = new MockCallingBackListener() {
        	@Override
        	public void publicCallback(Object entity) {
        		super.publicCallback(entity);
        		Artist a = (Artist)entity;
        		a.setArtistName("Changed"); // modify object during postLoad callback
        	}
        };
        lifecycleEventsManager.postLoad().addListener(listener::publicCallback, Artist.class);

        Artist a1 = env.context().newObject(Artist.class);
        a1.setArtistName("XX");
        Painting p1 = env.context().newObject(Painting.class);
        p1.setToArtist(a1);
        p1.setPaintingTitle("XXX");
        env.context().commitChanges();

        env.context().invalidateObjects(a1, p1);

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
    public void preUpdate() {

        LifecycleEventsManager lifecycleEventsManager = env.runtime().getDataDomain().getLifecycleEventsManager();

        Artist a1 = env.context().newObject(Artist.class);
        a1.setArtistName("XX");
        env.context().commitChanges();
        assertFalse(a1.isPreUpdated());

        a1.setArtistName("YY");
        env.context().commitChanges();
        assertFalse(a1.isPreUpdated());

        lifecycleEventsManager
                .preUpdate().addListener(Artist::preUpdateCallback, Artist.class);
        a1.setArtistName("ZZ");
        env.context().commitChanges();
        assertTrue(a1.isPreUpdated());

        a1.resetCallbackFlags();
        assertFalse(a1.isPreUpdated());

        MockCallingBackListener listener2 = new MockCallingBackListener();
        lifecycleEventsManager.preUpdate().addListener(listener2::publicCallback, Artist.class);

        a1.setArtistName("AA");
        env.context().commitChanges();

        assertTrue(a1.isPreUpdated());
        assertSame(a1, listener2.getPublicCalledbackEntity());
    }

    @Test
    public void postUpdate() {

        LifecycleEventsManager lifecycleEventsManager = env.runtime().getDataDomain().getLifecycleEventsManager();

        Artist a1 = env.context().newObject(Artist.class);
        a1.setArtistName("XX");
        env.context().commitChanges();
        assertFalse(a1.isPostUpdated());

        a1.setArtistName("YY");
        env.context().commitChanges();
        assertFalse(a1.isPostUpdated());

        lifecycleEventsManager.postUpdate().addListener(Artist::postUpdateCallback, Artist.class);
        a1.setArtistName("ZZ");
        env.context().commitChanges();
        assertTrue(a1.isPostUpdated());

        a1.resetCallbackFlags();
        assertFalse(a1.isPostUpdated());

        MockCallingBackListener listener2 = new MockCallingBackListener();
        lifecycleEventsManager.postUpdate().addListener(listener2::publicCallback, Artist.class);

        a1.setArtistName("AA");
        env.context().commitChanges();

        assertTrue(a1.isPostUpdated());
        assertSame(a1, listener2.getPublicCalledbackEntity());
    }

    @Test
    public void postRemove() {

        LifecycleEventsManager lifecycleEventsManager = env.runtime().getDataDomain().getLifecycleEventsManager();

        Artist a1 = env.context().newObject(Artist.class);
        a1.setArtistName("XX");
        env.context().commitChanges();

        lifecycleEventsManager.postRemove().addListener(Artist::postRemoveCallback, Artist.class);
        MockCallingBackListener listener2 = new MockCallingBackListener();
        lifecycleEventsManager.postRemove().addListener(listener2::publicCallback, Artist.class);

        env.context().deleteObjects(a1);
        env.context().commitChanges();

        assertTrue(a1.isPostRemoved());
        assertSame(a1, listener2.getPublicCalledbackEntity());
    }

    @Test
    public void postRemove_UpdatedDeleted() {

        LifecycleEventsManager lifecycleEventsManager = env.runtime().getDataDomain().getLifecycleEventsManager();

        Artist a1 = env.context().newObject(Artist.class);
        a1.setArtistName("XX");
        env.context().commitChanges();

        MockCallingBackListener listener1 = new MockCallingBackListener();
        lifecycleEventsManager.postRemove().addListener(listener1::publicCallback, Artist.class);

        MockCallingBackListener listener2 = new MockCallingBackListener();
        lifecycleEventsManager.postUpdate().addListener(listener2::publicCallback, Artist.class);

        // change before removing
        a1.setArtistName("YY");
        env.context().deleteObjects(a1);
        env.context().commitChanges();

        assertNull(listener2.getPublicCalledbackEntity());
        assertSame(a1, listener1.getPublicCalledbackEntity());
    }

    @Test
    public void postRemove_InsertedUpdatedDeleted() {

        LifecycleEventsManager lifecycleEventsManager = env.runtime().getDataDomain().getLifecycleEventsManager();

        MockCallingBackListener listener0 = new MockCallingBackListener();
        lifecycleEventsManager.postPersist().addListener(listener0::publicCallback, Artist.class);

        MockCallingBackListener listener1 = new MockCallingBackListener();
        lifecycleEventsManager.postRemove().addListener(listener1::publicCallback, Artist.class);

        MockCallingBackListener listener2 = new MockCallingBackListener();
        lifecycleEventsManager.postUpdate().addListener(listener2::publicCallback, Artist.class);

        Artist a1 = env.context().newObject(Artist.class);
        a1.setArtistName("XX");
        env.context().deleteObjects(a1);
        env.context().commitChanges();

        assertNull(listener0.getPublicCalledbackEntity());
        assertNull(listener1.getPublicCalledbackEntity());
        assertNull(listener2.getPublicCalledbackEntity());
    }

    @Test
    public void postPersist() {

        LifecycleEventsManager lifecycleEventsManager = env.runtime().getDataDomain().getLifecycleEventsManager();

        Artist a1 = env.context().newObject(Artist.class);
        a1.setArtistName("XX");
        env.context().commitChanges();
        assertFalse(a1.isPostPersisted());

        lifecycleEventsManager.postPersist().addListener(Artist::postPersistCallback, Artist.class);
        MockCallingBackListener listener2 = new MockCallingBackListener() {

            @Override
            public void publicCallback(Object entity) {
                super.publicCallback(entity);
                assertFalse(((Persistent) entity).getObjectId().isTemporary());
            }
        };
        lifecycleEventsManager.postPersist().addListener(listener2::publicCallback, Artist.class);

        Artist a2 = env.context().newObject(Artist.class);
        a2.setArtistName("XX");
        env.context().commitChanges();

        assertFalse(a1.isPostPersisted());
        assertTrue(a2.isPostPersisted());
        assertSame(a2, listener2.getPublicCalledbackEntity());
    }
}
