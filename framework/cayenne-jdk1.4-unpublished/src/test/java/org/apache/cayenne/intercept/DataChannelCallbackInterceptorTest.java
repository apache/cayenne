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
package org.apache.cayenne.intercept;

import org.apache.art.Artist;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.ObjectStore;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.LifecycleEventCallback;
import org.apache.cayenne.map.LifecycleEventCallbackMap;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

public class DataChannelCallbackInterceptorTest extends CayenneCase {

    protected void setUp() throws Exception {
        deleteTestData();
    }

    protected void tearDown() throws Exception {
        EntityResolver resolver = getDomain().getEntityResolver();

        resolver.getCallbacks(LifecycleEventCallback.POST_LOAD).removeAll();
        resolver.getCallbacks(LifecycleEventCallback.POST_PERSIST).removeAll();
        resolver.getCallbacks(LifecycleEventCallback.POST_REMOVE).removeAll();
        resolver.getCallbacks(LifecycleEventCallback.POST_UPDATE).removeAll();
        resolver.getCallbacks(LifecycleEventCallback.PRE_PERSIST).removeAll();
        resolver.getCallbacks(LifecycleEventCallback.PRE_REMOVE).removeAll();
        resolver.getCallbacks(LifecycleEventCallback.PRE_UPDATE).removeAll();
    }

    public void testPostLoad() throws Exception {
        LifecycleEventCallbackMap postLoad = getDomain()
                .getEntityResolver()
                .getCallbacks(LifecycleEventCallback.POST_LOAD);

        DataChannelCallbackInterceptor i = new DataChannelCallbackInterceptor();
        i.setChannel(getDomain());

        ObjectContext context = new DataContext(i, new ObjectStore(getDomain()
                .getSharedSnapshotCache()));

        postLoad.addListener(Artist.class, "postLoadCallback");
        MockCallingBackListener listener = new MockCallingBackListener();
        postLoad.addListener(Artist.class, listener, "publicCallback");

        Artist a1 = (Artist) context.newObject(Artist.class);
        a1.setArtistName("XX");
        context.commitChanges();
        assertFalse(a1.isPostLoaded());
        assertNull(listener.getPublicCalledbackEntity());

        SelectQuery q = new SelectQuery(Artist.class);
        context.performQuery(q);
        assertTrue(a1.isPostLoaded());
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
        assertFalse(a1.isPostLoaded());
        assertNull(listener.getPublicCalledbackEntity());

        // now change and rollback the artist - postLoad must be called
        a1.setArtistName("YY");
        context.rollbackChanges();
        assertTrue(a1.isPostLoaded());
        assertSame(a1, listener.getPublicCalledbackEntity());

        // test invalidated
        a1.resetCallbackFlags();
        listener.reset();
        assertFalse(a1.isPostLoaded());
        assertNull(listener.getPublicCalledbackEntity());

        context.performQuery(new RefreshQuery(a1));
        assertFalse(a1.isPostLoaded());
        assertNull(listener.getPublicCalledbackEntity());

        a1.getArtistName();
        assertTrue(a1.isPostLoaded());
        assertSame(a1, listener.getPublicCalledbackEntity());
    }

    public void testPreUpdate() {

        LifecycleEventCallbackMap preUpdate = getDomain()
                .getEntityResolver()
                .getCallbacks(LifecycleEventCallback.PRE_UPDATE);

        DataChannelCallbackInterceptor i = new DataChannelCallbackInterceptor();
        i.setChannel(getDomain());

        ObjectContext context = new DataContext(i, new ObjectStore(getDomain()
                .getSharedSnapshotCache()));

        Artist a1 = (Artist) context.newObject(Artist.class);
        a1.setArtistName("XX");
        context.commitChanges();
        assertFalse(a1.isPreUpdated());

        a1.setArtistName("YY");
        context.commitChanges();
        assertFalse(a1.isPreUpdated());

        preUpdate.addListener(Artist.class, "preUpdateCallback");
        a1.setArtistName("ZZ");
        context.commitChanges();
        assertTrue(a1.isPreUpdated());

        a1.resetCallbackFlags();
        assertFalse(a1.isPreUpdated());

        MockCallingBackListener listener2 = new MockCallingBackListener();
        preUpdate.addListener(Artist.class, listener2, "publicCallback");

        a1.setArtistName("AA");
        context.commitChanges();

        assertTrue(a1.isPreUpdated());
        assertSame(a1, listener2.getPublicCalledbackEntity());
    }

    public void testPostUpdate() {

        LifecycleEventCallbackMap postUpdate = getDomain()
                .getEntityResolver()
                .getCallbacks(LifecycleEventCallback.POST_UPDATE);

        DataChannelCallbackInterceptor i = new DataChannelCallbackInterceptor();
        i.setChannel(getDomain());

        ObjectContext context = new DataContext(i, new ObjectStore(getDomain()
                .getSharedSnapshotCache()));

        Artist a1 = (Artist) context.newObject(Artist.class);
        a1.setArtistName("XX");
        context.commitChanges();
        assertFalse(a1.isPostUpdated());

        a1.setArtistName("YY");
        context.commitChanges();
        assertFalse(a1.isPostUpdated());

        postUpdate.addListener(Artist.class, "postUpdateCallback");
        a1.setArtistName("ZZ");
        context.commitChanges();
        assertTrue(a1.isPostUpdated());

        a1.resetCallbackFlags();
        assertFalse(a1.isPostUpdated());

        MockCallingBackListener listener2 = new MockCallingBackListener();
        postUpdate.addListener(Artist.class, listener2, "publicCallback");

        a1.setArtistName("AA");
        context.commitChanges();

        assertTrue(a1.isPostUpdated());
        assertSame(a1, listener2.getPublicCalledbackEntity());
    }

    public void testPostRemove() {

        LifecycleEventCallbackMap postRemove = getDomain()
                .getEntityResolver()
                .getCallbacks(LifecycleEventCallback.POST_REMOVE);

        DataChannelCallbackInterceptor i = new DataChannelCallbackInterceptor();
        i.setChannel(getDomain());

        ObjectContext context = new DataContext(i, new ObjectStore(getDomain()
                .getSharedSnapshotCache()));

        Artist a1 = (Artist) context.newObject(Artist.class);
        a1.setArtistName("XX");
        context.commitChanges();

        postRemove.addListener(Artist.class, "postRemoveCallback");
        MockCallingBackListener listener2 = new MockCallingBackListener();
        postRemove.addListener(Artist.class, listener2, "publicCallback");

        context.deleteObject(a1);
        context.commitChanges();

        assertTrue(a1.isPostRemoved());
        assertSame(a1, listener2.getPublicCalledbackEntity());
    }

    public void testPostPersist() {

        LifecycleEventCallbackMap postPersist = getDomain()
                .getEntityResolver()
                .getCallbacks(LifecycleEventCallback.POST_PERSIST);

        DataChannelCallbackInterceptor i = new DataChannelCallbackInterceptor();
        i.setChannel(getDomain());

        ObjectContext context = new DataContext(i, new ObjectStore(getDomain()
                .getSharedSnapshotCache()));

        Artist a1 = (Artist) context.newObject(Artist.class);
        a1.setArtistName("XX");
        context.commitChanges();
        assertFalse(a1.isPostPersisted());

        postPersist.addListener(Artist.class, "postPersistCallback");
        MockCallingBackListener listener2 = new MockCallingBackListener();
        postPersist.addListener(Artist.class, listener2, "publicCallback");

        Artist a2 = (Artist) context.newObject(Artist.class);
        a2.setArtistName("XX");
        context.commitChanges();

        assertFalse(a1.isPostPersisted());
        assertTrue(a2.isPostPersisted());
        assertSame(a2, listener2.getPublicCalledbackEntity());
    }
}
