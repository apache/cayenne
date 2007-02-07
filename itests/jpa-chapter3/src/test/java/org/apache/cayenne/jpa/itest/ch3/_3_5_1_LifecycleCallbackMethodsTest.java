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
import org.apache.cayenne.jpa.itest.ch3.entity.CallbackEntity;
import org.apache.cayenne.jpa.itest.ch3.entity.CallbackEntity2;

public class _3_5_1_LifecycleCallbackMethodsTest extends EntityManagerCase {

    public void testPrePersist() {
        CallbackEntity e = new CallbackEntity();
        assertFalse(e.isPrePersistCalled());

        EntityManager em = getEntityManager();

        // spec reqires the callback to be invoked as a part of persist, without waiting
        // for flush or commit.
        em.persist(e);
        assertTrue(e.isPrePersistCalled());

        CallbackEntity2 e2 = new CallbackEntity2();
        assertFalse(e2.isMixedCallbackCalled());
        em.persist(e2);
        assertTrue(e2.isMixedCallbackCalled());
    }
}
