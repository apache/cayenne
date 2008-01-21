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
package org.apache.cayenne.jpa.itest.ch2;

import org.apache.cayenne.itest.jpa.EntityManagerCase;
import org.apache.cayenne.jpa.itest.ch2.entity.Embeddable1;
import org.apache.cayenne.jpa.itest.ch2.entity.EmbeddedEntity;
import org.apache.cayenne.jpa.itest.ch2.entity.PropertyEmbeddable;
import org.apache.cayenne.jpa.itest.ch2.entity.PropertyEmbeddedEntity;
import org.apache.cayenne.jpa.itest.ch2.entity.SerializableEmbeddable1;
import org.apache.cayenne.jpa.itest.ch2.entity.SerializableEmbeddedEntity;

public class _2_1_5_EmbeddableTest extends EntityManagerCase {

    public void testEmbeddable() throws Exception {
        getDbHelper().deleteAll("EmbeddedEntity");

        EmbeddedEntity o1 = new EmbeddedEntity();
        Embeddable1 o2 = new Embeddable1();
        o2.setProperty1("p1");
        o1.setEmbeddable(o2);

        getEntityManager().persist(o1);
        getEntityManager().getTransaction().commit();

        assertEquals("p1", getDbHelper().getObject("EmbeddedEntity", "property1"));
    }

    public void testPropertyEmbeddable() throws Exception {
        getDbHelper().deleteAll("PropertyEmbeddedEntity");

        PropertyEmbeddedEntity o1 = new PropertyEmbeddedEntity();
        PropertyEmbeddable o2 = new PropertyEmbeddable();
        o2.setProperty1("p1");
        o1.setEmbeddable(o2);

        getEntityManager().persist(o1);
        getEntityManager().getTransaction().commit();

        assertEquals("p1", getDbHelper().getObject("PropertyEmbeddedEntity", "property1"));
    }

    /**
     * Check that Embeddables that implement Serializable interface are correctly enhanced
     * and handled just as regular embeddables.
     */
    public void testSerializableEmbeddable() throws Exception {
        getDbHelper().deleteAll("SerializableEmbeddedEntity");

        SerializableEmbeddedEntity o1 = new SerializableEmbeddedEntity();
        SerializableEmbeddable1 o2 = new SerializableEmbeddable1();
        o2.setProperty1("p1");
        o1.setEmbeddable(o2);

        getEntityManager().persist(o1);
        getEntityManager().getTransaction().commit();

        assertEquals("p1", getDbHelper().getObject(
                "SerializableEmbeddedEntity",
                "property1"));
    }
}
