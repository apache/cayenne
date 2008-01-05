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

import javax.persistence.PersistenceException;

import org.apache.cayenne.itest.jpa.EntityManagerCase;
import org.apache.cayenne.jpa.itest.ch2.entity.EmbeddedIdEntity;
import org.apache.cayenne.jpa.itest.ch2.entity.FieldPersistenceEntity;
import org.apache.cayenne.jpa.itest.ch2.entity.IdClassEntity;
import org.apache.cayenne.jpa.itest.ch2.entity.NoPkEntity;

public class _2_1_4_PrimaryKeyAndIdentityTest extends EntityManagerCase {

    public void testNoPkEntity() {
        NoPkEntity o1 = new NoPkEntity();

        try {
            getEntityManager().persist(o1);
        }
        catch (IllegalArgumentException e) {
            return;
        }

        try {
            getEntityManager().getTransaction().commit();
        }
        catch (PersistenceException e) {
            return;
        }

        fail("Must have thrown on an attempt to persist or flush "
                + "an entity without defined id");
    }

    public void testSimplePk() throws Exception {
        getDbHelper().deleteAll("FieldPersistenceEntity");

        FieldPersistenceEntity o1 = new FieldPersistenceEntity();
        getEntityManager().persist(o1);

        assertEquals(0, o1.idField());
        getEntityManager().getTransaction().commit();
        assertTrue(o1.idField() > 0);
        assertEquals(o1.idField(), getDbHelper().getInt("FieldPersistenceEntity", "id"));
    }

    // TODO: andrus 8/10/2006 - fails
    public void _testIdClassPk() throws Exception {
        IdClassEntity o1 = new IdClassEntity();
        o1.setProperty1("p1");
        getEntityManager().persist(o1);
        getEntityManager().getTransaction().commit();

        assertEquals("p1", getDbHelper().getObject("IdClassEntity", "property1"));
    }

    // TODO: andrus 8/10/2006 - fails
    public void _testEmbeddedIdPk() throws Exception {
        EmbeddedIdEntity o1 = new EmbeddedIdEntity();
        o1.setProperty1("p1");
        getEntityManager().persist(o1);
        getEntityManager().getTransaction().commit();

        assertEquals("p1", getDbHelper().getObject("EmbeddedIdEntity", "property1"));
    }
}
