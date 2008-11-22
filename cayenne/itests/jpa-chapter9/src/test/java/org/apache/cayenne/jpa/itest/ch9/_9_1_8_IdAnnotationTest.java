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
import org.apache.cayenne.jpa.itest.ch9.entity.IdColumnEntity;
import org.apache.cayenne.jpa.itest.ch9.entity.IdEntity;

public class _9_1_8_IdAnnotationTest extends EntityManagerCase {

    public void testUserProvidedId() throws Exception {
        getDbHelper().deleteAll("IdEntity");

        IdEntity o1 = new IdEntity();
        o1.setIdValue(15);

        getEntityManager().persist(o1);
        getEntityManager().getTransaction().commit();

        assertEquals(15, getDbHelper().getInt("IdEntity", "id"));
        assertEquals(15, o1.getIdValue());
    }

    public void testGeneratedId() throws Exception {
        getDbHelper().deleteAll("IdEntity");

        IdEntity o1 = new IdEntity();

        getEntityManager().persist(o1);
        getEntityManager().getTransaction().commit();

        assertTrue(getDbHelper().getInt("IdEntity", "id") > 0);
        assertTrue(o1.getIdValue() > 0);
    }

    public void testUserProvidedIdColumnAnnotation() throws Exception {
        getDbHelper().deleteAll("IdColumnEntity");

        IdColumnEntity o1 = new IdColumnEntity();
        o1.setIdValue(15);

        getEntityManager().persist(o1);
        getEntityManager().getTransaction().commit();

        assertEquals(15, getDbHelper().getInt("IdColumnEntity", "idcolumn"));
        assertEquals(15, o1.getIdValue());
    }

    public void testGeneratedIdColumnAnnotation() throws Exception {
        getDbHelper().deleteAll("IdColumnEntity");

        IdColumnEntity o1 = new IdColumnEntity();

        getEntityManager().persist(o1);
        getEntityManager().getTransaction().commit();

        assertTrue(getDbHelper().getInt("IdColumnEntity", "idcolumn") > 0);
        assertTrue(o1.getIdValue() > 0);
    }

    public void testFind() throws Exception {
        getDbHelper().deleteAll("IdEntity");

        getDbHelper().insert("IdEntity", new String[] {
            "id",
        }, new Object[] {
            25
        });

        assertNull(getEntityManager().find(IdEntity.class, new Integer(14)));
        
        IdEntity o1 = (IdEntity) getEntityManager().find(IdEntity.class, new Integer(25)); 
        assertNotNull(o1);
        assertEquals(25, o1.getIdValue());
        
        assertNull(getEntityManager().find(IdEntity.class, new Integer(16)));
        
        IdEntity o2 = (IdEntity) getEntityManager().find(IdEntity.class, new Integer(25)); 
        assertSame(o1, o2);
    }
}
