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
import org.apache.cayenne.jpa.itest.ch9.entity.SecondaryTableEntity;

public class _9_1_2_SecondaryTableAnnotationTest extends EntityManagerCase {

    public void testPersist() throws Exception {
        getDbHelper().deleteAll("SecondaryTable");
        getDbHelper().deleteAll("PrimaryTable");

        SecondaryTableEntity o1 = new SecondaryTableEntity();
        o1.setPrimaryTableProperty("p1");
        o1.setSecondaryTableProperty("s1");

        getEntityManager().persist(o1);
        getEntityManager().getTransaction().commit();
        
        assertTrue(o1.getId() > 0);

        assertEquals("p1", getDbHelper().getObject("PrimaryTable", "primaryTableProperty"));
        assertEquals("s1", getDbHelper().getObject("SecondaryTable", "secondaryTableProperty"));
        assertEquals(o1.getId(), getDbHelper().getObject("SecondaryTable", "id"));
    }
}
