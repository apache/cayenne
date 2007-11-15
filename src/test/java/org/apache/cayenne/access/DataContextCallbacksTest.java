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
import org.apache.art.Painting;
import org.apache.cayenne.LifecycleListener;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.reflect.LifecycleCallbackRegistry;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextCallbacksTest extends CayenneCase {

    protected void setUp() throws Exception {
        deleteTestData();
    }

    protected void tearDown() throws Exception {
        EntityResolver resolver = getDomain().getEntityResolver();
        resolver.getCallbackRegistry().clear();
    }

    public void testPrePersistCallbacks() {
        LifecycleCallbackRegistry registry = getDomain()
                .getEntityResolver()
                .getCallbackRegistry();

        DataContext context = createDataContext();

        // no callbacks
        Artist a1 = (Artist) context.newObject(Artist.class);
        assertNotNull(a1);
        assertFalse(a1.isPrePersisted());

        registry.addListener(
                LifecycleListener.PRE_PERSIST,
                Artist.class,
                "prePersistCallback");

        Artist a2 = (Artist) context.newObject(Artist.class);
        assertNotNull(a2);
        assertTrue(a2.isPrePersisted());

        MockCallingBackListener listener2 = new MockCallingBackListener();
        registry.addListener(
                LifecycleListener.PRE_PERSIST,
                Artist.class,
                listener2,
                "publicCallback");

        Artist a3 = (Artist) context.newObject(Artist.class);
        assertNotNull(a3);
        assertTrue(a3.isPrePersisted());

        assertSame(a3, listener2.getPublicCalledbackEntity());

        Painting p3 = (Painting) context.newObject(Painting.class);
        assertNotNull(p3);
        assertFalse(p3.isPrePersisted());
        assertSame(a3, listener2.getPublicCalledbackEntity());
    }

    public void testPreRemoveCallbacks() {
        LifecycleCallbackRegistry registry = getDomain()
                .getEntityResolver()
                .getCallbackRegistry();

        DataContext context = createDataContext();

        // no callbacks
        Artist a1 = (Artist) context.newObject(Artist.class);
        a1.setArtistName("XX");
        context.commitChanges();
        context.deleteObject(a1);
        assertFalse(a1.isPrePersisted());
        assertFalse(a1.isPreRemoved());

        registry.addListener(
                LifecycleListener.PRE_REMOVE,
                Artist.class,
                "preRemoveCallback");

        Artist a2 = (Artist) context.newObject(Artist.class);
        a2.setArtistName("XX");
        context.commitChanges();
        context.deleteObject(a2);
        assertFalse(a2.isPrePersisted());
        assertTrue(a2.isPreRemoved());

        MockCallingBackListener listener2 = new MockCallingBackListener();
        registry.addListener(
                LifecycleListener.PRE_REMOVE,
                Artist.class,
                listener2,
                "publicCallback");

        Artist a3 = (Artist) context.newObject(Artist.class);
        a3.setArtistName("XX");
        context.commitChanges();
        context.deleteObject(a3);
        assertFalse(a3.isPrePersisted());
        assertTrue(a3.isPreRemoved());

        assertSame(a3, listener2.getPublicCalledbackEntity());

        Painting p3 = (Painting) context.newObject(Painting.class);
        p3.setPaintingTitle("XX");
        context.commitChanges();
        context.deleteObject(p3);
        assertFalse(p3.isPrePersisted());
        assertFalse(p3.isPreRemoved());
        assertSame(a3, listener2.getPublicCalledbackEntity());
    }
}
