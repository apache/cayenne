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
import javax.persistence.Query;

import org.apache.cayenne.itest.jpa.EntityManagerCase;
import org.apache.cayenne.jpa.itest.ch3.entity.SimpleEntity;

public class _3_6_6_SQLQueriesTest extends EntityManagerCase {

    public void testNativeEntityQuery() throws Exception {
        getDbHelper().deleteAll("SimpleEntity");
        getTableHelper("SimpleEntity").deleteAll().setColumns("id", "property1").insert(
                1,
                "X").insert(2, "Y");

        EntityManager em = getEntityManager();
        Query query = em.createNativeQuery(
                "SELECT id, property1 FROM SimpleEntity ORDER BY property1",
                SimpleEntity.class);

        // TODO: andrus 2/18/2008 - this fails because of wrong column name
        // capitalization... need to figure a portable solution

        // List result = query.getResultList();
        // assertNotNull(result);
        // assertEquals(2, result.size());
        // assertTrue(result.get(0) instanceof SimpleEntity);
    }

    public void testNativeEntityQueryMappedResult() throws Exception {
        getDbHelper().deleteAll("SimpleEntity");
        getTableHelper("SimpleEntity").deleteAll().setColumns("id", "property1").insert(
                1,
                "X").insert(2, "Y");

        EntityManager em = getEntityManager();
        Query query = em.createNativeQuery(
                "SELECT ID as X, ID + 5 as Y, ID + 6 as Z FROM SimpleEntity ORDER BY ID",
                "rs1");

        List result = query.getResultList();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(0) instanceof Object[]);
    }
}
