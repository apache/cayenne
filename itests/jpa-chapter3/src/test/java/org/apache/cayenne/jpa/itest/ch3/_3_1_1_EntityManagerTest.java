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

import java.util.List;

import javax.persistence.EntityExistsException;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.apache.cayenne.itest.jpa.EntityManagerCase;
import org.apache.cayenne.jpa.itest.ch3.entity.NonEntity;
import org.apache.cayenne.jpa.itest.ch3.entity.SimpleEntity;

public class _3_1_1_EntityManagerTest extends EntityManagerCase {

    public void testPersist() throws Exception {
        getDbHelper().deleteAll("SimpleEntity");

        SimpleEntity e = new SimpleEntity();
        e.setProperty1("XXX");
        getEntityManager().persist(e);
        getEntityManager().getTransaction().commit();

        assertEquals(1, getDbHelper().getRowCount("SimpleEntity"));
    }

    public void testPersistNonEntity() throws Exception {
        try {
            getEntityManager().persist(new NonEntity());
            fail("Must have thrown IllegalARgumentException on non entity");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testPersistEntityExistsException() {
        SimpleEntity e1 = new SimpleEntity();
        e1.updateIdField(3);

        SimpleEntity e2 = new SimpleEntity();
        e2.updateIdField(3);

        getEntityManager().persist(e1);

        try {
            getEntityManager().persist(e2);
        }
        catch (EntityExistsException e) {
            // expected
            return;
        }

        // if no EntityExistsException was thrown immediately, try doing a commit - an
        // exception must be thrown here

        try {
            getEntityManager().flush();
        }
        catch (PersistenceException e) {
            // expected
            return;
        }

        try {
            getEntityManager().getTransaction().commit();
        }
        catch (PersistenceException e) {
            // expected
            return;
        }

        fail("Must have thrown on EntityExists condition.");
    }

    // TODO: andrus, 1/3/2007 - this fails with Null ObjectId exception
    public void _testMerge() throws Exception {
        getDbHelper().deleteAll("SimpleEntity");

        getDbHelper().insert("SimpleEntity", new String[] {
                "id", "property1"
        }, new Object[] {
                1, "XXX"
        });

        SimpleEntity e1 = new SimpleEntity();
        e1.setProperty1("YYY");
        e1.updateIdField(1);

        // detailed merge logic is described in chapter 3.2.4.1
        Object merged = getEntityManager().merge(e1);
        assertNotNull(merged);
        assertTrue(merged instanceof SimpleEntity);

        SimpleEntity e2 = (SimpleEntity) merged;

        assertEquals("YYY", e2.getProperty1());

        getEntityManager().getTransaction().commit();
        assertEquals(1, getDbHelper().getRowCount("SimpleEntity"));
        assertEquals("YYY", getDbHelper().getObject("SimpleEntity", "property1"));
    }

    public void testMergeRemovedEntity() throws Exception {
        getDbHelper().deleteAll("SimpleEntity");

        getDbHelper().insert("SimpleEntity", new String[] {
                "id", "property1"
        }, new Object[] {
                1, "XXX"
        });

        SimpleEntity e1 = (SimpleEntity) getEntityManager().find(SimpleEntity.class, 1);
        assertNotNull(e1);
        getEntityManager().remove(e1);

        try {
            getEntityManager().merge(e1);
            fail("must have thrown IllegalArgumentException on merging a removed entity.");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testMergeNonEntity() throws Exception {

        try {
            getEntityManager().merge(new NonEntity());
            fail("must have thrown IllegalArgumentException on merging a non entity.");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testRemove() throws Exception {
        getDbHelper().deleteAll("SimpleEntity");

        getDbHelper().insert("SimpleEntity", new String[] {
                "id", "property1"
        }, new Object[] {
                1, "XXX"
        });

        SimpleEntity e1 = (SimpleEntity) getEntityManager().find(SimpleEntity.class, 1);
        assertNotNull(e1);
        getEntityManager().remove(e1);
        getEntityManager().getTransaction().commit();

        assertEquals(0, getDbHelper().getRowCount("SimpleEntity"));
    }

    public void testRemoveNonEntity() throws Exception {
        try {
            getEntityManager().remove(new NonEntity());
            fail("Must have thrown IllegalArgumentException on non entity");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testRemoveDetachedEntity() throws Exception {
        try {
            getEntityManager().remove(new SimpleEntity());
            fail("Must have thrown IllegalArgumentException on detached entity");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testFind() throws Exception {
        getDbHelper().deleteAll("SimpleEntity");

        getDbHelper().insert("SimpleEntity", new String[] {
                "id", "property1"
        }, new Object[] {
                15, "XXX"
        });

        assertNotNull(getEntityManager().find(SimpleEntity.class, 15));
        assertNull(getEntityManager().find(SimpleEntity.class, 16));
    }

    public void testFindNonEntity() {
        try {
            getEntityManager().find(NonEntity.class, 1);
            fail("must have thrown");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
    }

    // TODO: andrus, 1/3/2007 - this fails - need algorithm for id validation.
    public void _testFindBadIdType() {
        try {
            getEntityManager().find(SimpleEntity.class, "abc");
            fail("must have thrown on invalid id class.");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testCreateQuery() throws Exception {
        getDbHelper().deleteAll("SimpleEntity");

        getDbHelper().insert("SimpleEntity", new String[] {
                "id", "property1"
        }, new Object[] {
                15, "XXX"
        });

        Query query = getEntityManager().createQuery("select x from SimpleEntity x");
        assertNotNull(query);
        List<?> result = query.getResultList();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0) instanceof SimpleEntity);
        assertEquals("XXX", ((SimpleEntity) result.get(0)).getProperty1());
    }

    public void testCreateNativeQuery() throws Exception {
        getDbHelper().deleteAll("SimpleEntity");

        getDbHelper().insert("SimpleEntity", new String[] {
                "id", "property1"
        }, new Object[] {
                15, "XXX"
        });

        Query query = getEntityManager().createNativeQuery(
                "DELETE FROM SimpleEntity WHERE id = 15");
        assertNotNull(query);
        assertEquals(1, query.executeUpdate());

        assertEquals(0, getDbHelper().getRowCount("SimpleEntity"));
    }
}
