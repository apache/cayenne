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
package org.apache.cayenne.jpa.itest.ch3;

import javax.persistence.EntityManager;

import org.apache.cayenne.itest.jpa.EntityManagerCase;
import org.apache.cayenne.itest.jpa.ItestSetup;
import org.apache.cayenne.jpa.itest.ch3.entity.CallbackEntity;
import org.apache.cayenne.jpa.itest.ch3.entity.CallbackEntity2;
import org.apache.cayenne.jpa.itest.ch3.entity.EntityListener1;
import org.apache.cayenne.jpa.itest.ch3.entity.EntityListener2;
import org.apache.cayenne.jpa.itest.ch3.entity.EntityListenerState;
import org.apache.cayenne.jpa.itest.ch3.entity.ListenerEntity1;
import org.apache.cayenne.jpa.itest.ch3.entity.ListenerEntity2;

public class _3_5_1_LifecycleCallbackMethodsTest extends EntityManagerCase {

    public void testPrePersist() {

        // regular entity
        CallbackEntity e = new CallbackEntity();
        assertFalse(e.isPrePersistCalled());

        EntityManager em = getEntityManager();

        // spec reqires the callback to be invoked as a part of persist, without waiting
        // for flush or commit.
        em.persist(e);
        assertTrue(e.isPrePersistCalled());

        // entity with same callback method handling multiple callbacks
        CallbackEntity2 e2 = new CallbackEntity2();
        assertFalse(e2.isMixedCallbackCalled());
        em.persist(e2);
        assertTrue(e2.isMixedCallbackCalled());

        // external listeners
        EntityListenerState.reset();
        assertEquals("", EntityListenerState.getPrePersistCalled());
        ListenerEntity1 e3 = new ListenerEntity1();
        em.persist(e3);
        assertEquals(":"
                + EntityListener1.class.getName()
                + ":"
                + EntityListener2.class.getName(), EntityListenerState
                .getPrePersistCalled());

        EntityListenerState.reset();
        assertEquals("", EntityListenerState.getPrePersistCalled());
        ListenerEntity2 e4 = new ListenerEntity2();
        em.persist(e4);
        // here annotations must be called in a different order from e3.
        assertEquals(":"
                + EntityListener2.class.getName()
                + ":"
                + EntityListener1.class.getName(), EntityListenerState
                .getPrePersistCalled());
    }

    public void testPostPersist() {

        // regular entity
        CallbackEntity e = new CallbackEntity();
        assertFalse(e.isPostPersistCalled());

        // don't use super getEntityManager - it starts a tran
        EntityManager em = ItestSetup.getInstance().createEntityManager();

        // spec reqires the callback to be invoked as a part of persist, without waiting
        // for flush or commit.
        em.getTransaction().begin();
        em.persist(e);
        assertFalse(e.isPostPersistCalled());
        assertEquals(0, e.getPostPersistedId());
        em.getTransaction().commit();

        assertTrue(e.isPostPersistCalled());
        
        // Per spec, id must be availble during PostPersist
        assertEquals(e.getId(), e.getPostPersistedId());
        assertTrue(e.getId() > 0);

        // external listeners
        EntityListenerState.reset();
        assertEquals("", EntityListenerState.getPostPersistCalled());
        ListenerEntity1 e3 = new ListenerEntity1();

        // reset EM
        em = ItestSetup.getInstance().createEntityManager();
        assertEquals("", EntityListenerState.getPostPersistCalled());
        em.getTransaction().begin();
        em.persist(e3);
        assertEquals(":" + EntityListener2.class.getName(), EntityListenerState
                .getPostPersistCalled(), EntityListenerState.getPostPersistCalled());
        EntityListenerState.reset();
        
        em.getTransaction().commit();
        assertEquals(":"
                + EntityListener1.class.getName()
                + ":"
                + EntityListener2.class.getName(), EntityListenerState
                .getPostPersistCalled());
    }
}
