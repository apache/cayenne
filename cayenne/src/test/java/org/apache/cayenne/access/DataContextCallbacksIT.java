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

import org.apache.cayenne.access.lifecycle.LifecycleEventsManager;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataContextCallbacksIT {

    // listeners are registered per test, so each test gets its own runtime
    @RegisterExtension
    final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void postAddCallbacks() {
        DataContext context = env.context();
        LifecycleEventsManager lifecycleEventsManager = env.runtime()
                .getDataDomain()
                .getLifecycleEventsManager();

        // no callbacks
        Artist a1 = context.newObject(Artist.class);
        assertNotNull(a1);
        assertFalse(a1.isPostAdded());

        lifecycleEventsManager.postAdd().addListener(Artist::postAddCallback, Artist.class);

        Artist a2 = context.newObject(Artist.class);
        assertNotNull(a2);
        assertTrue(a2.isPostAdded());

        MockCallingBackListener listener2 = new MockCallingBackListener();
        lifecycleEventsManager.postAdd().addListener(listener2::publicCallback, Artist.class);

        Artist a3 = context.newObject(Artist.class);
        assertNotNull(a3);
        assertTrue(a3.isPostAdded());

        assertSame(a3, listener2.getPublicCalledbackEntity());

        Painting p3 = context.newObject(Painting.class);
        assertNotNull(p3);
        assertFalse(p3.isPostAdded());
        assertSame(a3, listener2.getPublicCalledbackEntity());
    }

    @Test
    public void prePersistCallbacks() {
        DataContext context = env.context();
        LifecycleEventsManager lifecycleEventsManager = env.runtime()
                .getDataDomain()
                .getLifecycleEventsManager();

        // no callbacks
        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("1");
        assertFalse(a1.isPrePersisted());
        context.commitChanges();
        assertFalse(a1.isPrePersisted());

        lifecycleEventsManager.prePersist().addListener(Artist::prePersistCallback, Artist.class);

        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("2");
        assertFalse(a2.isPrePersisted());
        context.commitChanges();
        assertTrue(a2.isPrePersisted());

        MockCallingBackListener listener2 = new MockCallingBackListener();
        lifecycleEventsManager.prePersist().addListener(listener2::publicCallback, Artist.class);

        Artist a3 = context.newObject(Artist.class);
        a3.setArtistName("3");
        assertNull(listener2.getPublicCalledbackEntity());
        context.commitChanges();
        assertSame(a3, listener2.getPublicCalledbackEntity());
    }

    @Test
    public void preRemoveCallbacks() {
        DataContext context = env.context();
        LifecycleEventsManager lifecycleEventsManager = env.runtime()
                .getDataDomain()
                .getLifecycleEventsManager();

        // no callbacks
        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("XX");
        context.commitChanges();
        context.deleteObjects(a1);
        assertFalse(a1.isPostAdded());
        assertFalse(a1.isPreRemoved());

        lifecycleEventsManager
                .preRemove().addListener(Artist::preRemoveCallback, Artist.class);

        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("XX");
        context.commitChanges();
        context.deleteObjects(a2);
        assertFalse(a2.isPostAdded());
        assertTrue(a2.isPreRemoved());

        MockCallingBackListener listener2 = new MockCallingBackListener();
        lifecycleEventsManager.preRemove().addListener(listener2::publicCallback, Artist.class);

        Artist a3 = context.newObject(Artist.class);
        a3.setArtistName("XX");
        context.commitChanges();
        context.deleteObjects(a3);
        assertFalse(a3.isPostAdded());
        assertTrue(a3.isPreRemoved());

        assertSame(a3, listener2.getPublicCalledbackEntity());

        Painting p3 = context.newObject(Painting.class);
        p3.setPaintingTitle("XX");
        context.commitChanges();
        context.deleteObjects(p3);
        assertFalse(p3.isPostAdded());
        assertFalse(p3.isPreRemoved());
        assertSame(a3, listener2.getPublicCalledbackEntity());
    }
}
