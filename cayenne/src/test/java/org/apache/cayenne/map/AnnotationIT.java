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
package org.apache.cayenne.map;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.reflect.LifecycleCallbackRegistry;
import org.apache.cayenne.testdo.annotation.ArtistAnnotation;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsExt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @since 4.2
 */
public class AnnotationIT {

    @RegisterExtension
    static final CayenneTestsExt env = CayenneTestsExt.forProject(CayenneProjects.ANNOTATION);

    private ObjectContext objectContext;

    @BeforeEach
    public void setUp() {
        objectContext = env.context();
    }

    @Test
    public void availableCallback() {

        LifecycleCallbackRegistry lifecycleCallbackRegistry = objectContext.getEntityResolver().getCallbackRegistry();

        assertFalse(lifecycleCallbackRegistry.isEmpty(LifecycleEvent.POST_ADD));
        assertFalse(lifecycleCallbackRegistry.isEmpty(LifecycleEvent.PRE_PERSIST));
        assertFalse(lifecycleCallbackRegistry.isEmpty(LifecycleEvent.POST_PERSIST));
        assertFalse(lifecycleCallbackRegistry.isEmpty(LifecycleEvent.POST_LOAD));
        assertFalse(lifecycleCallbackRegistry.isEmpty(LifecycleEvent.PRE_UPDATE));
        assertFalse(lifecycleCallbackRegistry.isEmpty(LifecycleEvent.POST_UPDATE));
        assertFalse(lifecycleCallbackRegistry.isEmpty(LifecycleEvent.PRE_REMOVE));
        assertFalse(lifecycleCallbackRegistry.isEmpty(LifecycleEvent.POST_REMOVE));
    }

    @Test
    public void workCallback() {
        ArtistAnnotation artist = objectContext.newObject(ArtistAnnotation.class);
        assertEquals(artist.getPostCallback(), "testPostAdd");
        assertNull(artist.getPreCallback());

        objectContext.commitChanges();
        assertEquals(artist.getPostCallback(), "testPostPersist");
        assertEquals(artist.getPreCallback(), "testPrePersist");

        artist = ObjectSelect.query(ArtistAnnotation.class).selectFirst(objectContext);
        assertEquals(artist.getPostCallback(), "testPostLoad");

        artist.setPostCallback(null);
        objectContext.commitChanges();
        assertEquals(artist.getPostCallback(), "testPostUpdate");
        assertEquals(artist.getPreCallback(), "testPreUpdate");

        objectContext.deleteObject(artist);
        assertEquals(artist.getPreCallback(), "testPreRemove");
        objectContext.commitChanges();
        assertEquals(artist.getPostCallback(), "testPostRemove");
    }

}
