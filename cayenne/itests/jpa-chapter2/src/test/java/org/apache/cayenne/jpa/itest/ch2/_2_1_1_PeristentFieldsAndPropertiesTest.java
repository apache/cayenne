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

import java.util.Collections;

import org.apache.cayenne.itest.jpa.EntityManagerCase;
import org.apache.cayenne.jpa.itest.ch2.entity.CollectionFieldPersistenceEntity;
import org.apache.cayenne.jpa.itest.ch2.entity.FieldPersistenceEntity;
import org.apache.cayenne.jpa.itest.ch2.entity.HelperEntity1;
import org.apache.cayenne.jpa.itest.ch2.entity.HelperEntity2;
import org.apache.cayenne.jpa.itest.ch2.entity.HelperEntity3;
import org.apache.cayenne.jpa.itest.ch2.entity.HelperEntity4;
import org.apache.cayenne.jpa.itest.ch2.entity.MapFieldPersistenceEntity;
import org.apache.cayenne.jpa.itest.ch2.entity.PropertyPersistenceEntity;
import org.apache.cayenne.jpa.itest.ch2.entity.TransientFieldsEntity;

public class _2_1_1_PeristentFieldsAndPropertiesTest extends EntityManagerCase {

    public void testFieldBasedPersistence() throws Exception {

        getDbHelper().deleteAll("FieldPersistenceEntity");

        FieldPersistenceEntity o1 = new FieldPersistenceEntity();
        getEntityManager().persist(o1);
        getEntityManager().getTransaction().commit();

        assertEquals(FieldPersistenceEntity.INITIAL_VALUE, getDbHelper().getObject(
                "FieldPersistenceEntity",
                "property1"));
    }

    public void testPropertyBasedPersistence() throws Exception {
        getDbHelper().deleteAll("PropertyPersistenceEntity");

        PropertyPersistenceEntity o1 = new PropertyPersistenceEntity();
        o1.setProperty1("p1");
        o1.setProperty2(true);
        getEntityManager().persist(o1);
        getEntityManager().getTransaction().commit();

        assertEquals("p1", getDbHelper().getObject(
                "PropertyPersistenceEntity",
                "property1"));
    }

    public void testSkipTransientProperties() {
        TransientFieldsEntity o1 = new TransientFieldsEntity();
        getEntityManager().persist(o1);
        getEntityManager().getTransaction().commit();
    }

    // TODO: andrus 8/30/2006 - fails
    public void _testCollectionTypesProperties() {
        CollectionFieldPersistenceEntity o1 = new CollectionFieldPersistenceEntity();
        o1.setCollection(Collections.singleton(new HelperEntity1()));
        o1.setSet(Collections.singleton(new HelperEntity2()));
        o1.setList(Collections.singletonList(new HelperEntity3()));

        getEntityManager().persist(o1);
        getEntityManager().getTransaction().commit();
    }

    public void testMapProperties() {
        MapFieldPersistenceEntity o1 = new MapFieldPersistenceEntity();
        o1.setMap(Collections.singletonMap(new Object(), new HelperEntity4()));

        getEntityManager().persist(o1);
        getEntityManager().getTransaction().commit();
    }

    // TODO: andrus 8/30/2006 - implement
    public void testExceptionInPropertyAccessors() {
        // Runtime exceptions thrown by property accessor methods cause the current
        // transaction to be rolled back.
        // Exceptions thrown by such methods when used by the persistence runtime to load
        // or store persistent state cause the persistence runtime to rollback the current
        // transaction and to throw a PersistenceException that wraps the application
        // exception.
    }
}
