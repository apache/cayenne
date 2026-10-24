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

import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.entity_callbacks.CallbackArtist;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the callbacks of an entity class implementing the callback interfaces, as invoked by the ObjectContext.
 */
public class EntityCallbacksIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.ENTITY_CALLBACKS);

    @Test
    public void callbacksRegistered() {
        LifecycleEventsManager lifecycleEventsManager = env.context().getChannel().getLifecycleEventsManager();
        assertTrue(lifecycleEventsManager.postAdd().hasListeners());
        assertTrue(lifecycleEventsManager.prePersist().hasListeners());
        assertTrue(lifecycleEventsManager.postPersist().hasListeners());
        assertTrue(lifecycleEventsManager.preUpdate().hasListeners());
        assertTrue(lifecycleEventsManager.postUpdate().hasListeners());
        assertTrue(lifecycleEventsManager.preRemove().hasListeners());
        assertTrue(lifecycleEventsManager.postRemove().hasListeners());
        assertTrue(lifecycleEventsManager.postLoad().hasListeners());
    }

    @Test
    public void callbacks() {
        CallbackArtist artist = env.context().newObject(CallbackArtist.class);
        assertEquals("onPostAdd", artist.getPostCallback());
        assertNull(artist.getPreCallback());

        env.context().commitChanges();
        assertEquals("onPostPersist", artist.getPostCallback());
        assertEquals("onPrePersist", artist.getPreCallback());

        artist = ObjectSelect.query(CallbackArtist.class).selectFirst(env.context());
        assertEquals("onPostLoad", artist.getPostCallback());

        artist.setPostCallback(null);
        env.context().commitChanges();
        assertEquals("onPostUpdate", artist.getPostCallback());
        assertEquals("onPreUpdate", artist.getPreCallback());

        env.context().deleteObject(artist);
        assertEquals("onPreRemove", artist.getPreCallback());
        env.context().commitChanges();
        assertEquals("onPostRemove", artist.getPostCallback());
    }
}
