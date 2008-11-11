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
package org.apache.cayenne.jpa.itest.ch9;

import org.apache.cayenne.itest.jpa.EntityManagerCase;
import org.apache.cayenne.jpa.itest.ch9.entity.BasicEntity;

/**
 * Tests not included here: (1) supported types are tested in chapter 2; (2) there is no
 * good way to test optionality, as it is only a hint to the schema generator that is used
 * outside the JPA spec. It should be tested at the provider level.
 * 
 */
public class _9_1_18_BasicAnnotationTest extends EntityManagerCase {

    public void testSelectBasicDefault() throws Exception {
        getDbHelper().deleteAll("BasicEntity");
        getDbHelper().insert("BasicEntity", new String[] {
                "id", "basicDefault", "basicDefaultInt"
        }, new Object[] {
                1, "a", 67
        });

        BasicEntity o1 = getEntityManager().find(BasicEntity.class, 1);
        assertEquals("a", o1.getBasicDefaultX());
        assertEquals(67, o1.getBasicDefaultIntX());
    }

    public void testSelectBasicEager() throws Exception {
        getDbHelper().deleteAll("BasicEntity");
        getDbHelper().insert("BasicEntity", new String[] {
                "id", "basicEager"
        }, new Object[] {
                2, "b"
        });

        BasicEntity o1 = getEntityManager().find(BasicEntity.class, 2);
        assertEquals("b", o1.getBasicEagerX());
    }

    public void testSelectBasicLazy() throws Exception {
        getDbHelper().deleteAll("BasicEntity");
        getDbHelper().insert("BasicEntity", new String[] {
                "id", "basicLazy"
        }, new Object[] {
                3, "c"
        });

        BasicEntity o1 = getEntityManager().find(BasicEntity.class, 3);
        // application may or may not support lazy loading, but when the property is
        // accessed via getter, it must get resolved one way or another...
        assertEquals("c", o1.getBasicLazy());
    }
}
