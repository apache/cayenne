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

import javax.persistence.EntityManager;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;

import org.apache.cayenne.itest.jpa.EntityManagerCase;
import org.apache.cayenne.jpa.itest.ch3.entity.SimpleEntity;

import sun.java2d.pipe.SpanShapeRenderer.Simple;

public class _3_6_1_QueryAPITest extends EntityManagerCase {

    public void testGetResultListEntity() throws Exception {
        getDbHelper().deleteAll("SimpleEntity");
        getTableHelper("SimpleEntity").deleteAll().setColumns("id", "property1").insert(
                1,
                "X").insert(2, "Y");

        EntityManager em = getEntityManager();
        Query query = em.createQuery("SELECT a FROM SimpleEntity a ORDER BY a.property1");

        List result = query.getResultList();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(0) instanceof SimpleEntity);
    }

    public void testGetResultListScalar() throws Exception {
        getDbHelper().deleteAll("SimpleEntity");
        getTableHelper("SimpleEntity").deleteAll().setColumns("id", "property1").insert(
                1,
                "X").insert(2, "Y");

        EntityManager em = getEntityManager();
        Query query = em.createQuery("SELECT count(a) FROM SimpleEntity a");

        List result = query.getResultList();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0) instanceof Long);
        assertEquals(2, ((Long) result.get(0)).intValue());
    }

    public void testGetResultListMixed() throws Exception {
        getDbHelper().deleteAll("SimpleEntity");
        getTableHelper("SimpleEntity").deleteAll().setColumns("id", "property1").insert(
                1,
                "X").insert(2, "Y");

        EntityManager em = getEntityManager();
        Query query = em
                .createQuery("SELECT a, a.id FROM SimpleEntity a ORDER BY a.property1");

        List result = query.getResultList();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(0) instanceof Object[]);

        Object[] row0 = (Object[]) result.get(0);
        assertEquals(2, row0.length);

        assertTrue(row0[0] instanceof SimpleEntity);
        assertEquals(1, ((Number) row0[1]).intValue());
    }

    public void testGetSingleResultListEntity() throws Exception {
        getDbHelper().deleteAll("SimpleEntity");
        getTableHelper("SimpleEntity").deleteAll().setColumns("id", "property1").insert(
                1,
                "X");

        EntityManager em = getEntityManager();
        Query query = em.createQuery("SELECT a FROM SimpleEntity a ORDER BY a.property1");

        Object result = query.getSingleResult();
        assertNotNull(result);
        assertTrue(result instanceof SimpleEntity);
    }

    public void testGetSingleResultListEntityNonUniqueResultException() throws Exception {
        getDbHelper().deleteAll("SimpleEntity");
        getTableHelper("SimpleEntity").deleteAll().setColumns("id", "property1").insert(
                1,
                "X").insert(2, "Y");

        EntityManager em = getEntityManager();
        Query query = em.createQuery("SELECT a FROM SimpleEntity a ORDER BY a.property1");

        try {
            query.getSingleResult();
            fail("Must have thrown NonUniqueResultException");
        }
        catch (NonUniqueResultException e) {
            // expected
        }
    }

    public void testSetMaxResults() throws Exception {
        getTableHelper("SimpleEntity").deleteAll().setColumns("id", "property1").insert(
                1,
                "A").insert(2, "B").insert(3, "C").insert(4, "D");
        
        EntityManager em = getEntityManager();
        Query query = em.createQuery("SELECT a FROM SimpleEntity a ORDER BY a.property1");
        query.setMaxResults(2);
        
        List<?> results = query.getResultList();
        assertEquals(2, results.size());
        SimpleEntity e1 = (SimpleEntity) results.get(0);
        SimpleEntity e2 = (SimpleEntity) results.get(1);
        assertEquals("A", e1.getProperty1());
        assertEquals("B", e2.getProperty1());
    }
    
    public void testSetFirstResult() throws Exception {
        getTableHelper("SimpleEntity").deleteAll().setColumns("id", "property1").insert(
                1,
                "A").insert(2, "B").insert(3, "C").insert(4, "D");
        
        EntityManager em = getEntityManager();
        Query query = em.createQuery("SELECT a FROM SimpleEntity a ORDER BY a.property1");
        query.setFirstResult(2);
        
        List<?> results = query.getResultList();
        assertEquals(2, results.size());
        SimpleEntity e1 = (SimpleEntity) results.get(0);
        SimpleEntity e2 = (SimpleEntity) results.get(1);
        assertEquals("C", e1.getProperty1());
        assertEquals("D", e2.getProperty1());
    }
    
    public void testSetFirstResultMaxResults() throws Exception {
        getTableHelper("SimpleEntity").deleteAll().setColumns("id", "property1").insert(
                1,
                "A").insert(2, "B").insert(3, "C").insert(4, "D");
        
        EntityManager em = getEntityManager();
        Query query = em.createQuery("SELECT a FROM SimpleEntity a ORDER BY a.property1");
        query.setFirstResult(1);
        query.setMaxResults(1);
        
        List<?> results = query.getResultList();
        assertEquals(1, results.size());
        SimpleEntity e1 = (SimpleEntity) results.get(0);
        assertEquals("B", e1.getProperty1());
    }
}
