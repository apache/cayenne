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

import java.util.List;

import javax.persistence.Query;

import org.apache.cayenne.itest.ItestTableUtils;
import org.apache.cayenne.itest.jpa.EntityManagerCase;
import org.apache.cayenne.jpa.itest.ch2.entity.SingleTableInheritanceSub1;
import org.apache.cayenne.jpa.itest.ch2.entity.SingleTableInheritanceSub2;
import org.apache.cayenne.jpa.itest.ch2.entity.SingleTableInheritanceSuper1;

public class _2_1_10_1_SingleTablePerClassTest extends EntityManagerCase {

    public void testSelectSuper() throws Exception {
        ItestTableUtils helper = getTableHelper("ST_INHERITANCE");
        helper.deleteAll();
        helper.setColumns("id", "objectType", "propertyA", "propertyB", "propertyC");
        helper.insert(1, "A", "1", null, null);
        helper.insert(2, "A", "2", null, null);
        helper.insert(3, "B", "3", "BX", null);
        helper.insert(4, "C", "4", null, "CX");

        Query query = getEntityManager().createQuery(
                "select a FROM SingleTableInheritanceSuper1 a ORDER BY a.propertyA");
        List<?> results = query.getResultList();
        assertEquals(4, results.size());

        assertEquals(SingleTableInheritanceSuper1.class.getName(), results
                .get(0)
                .getClass()
                .getName());
        assertEquals(SingleTableInheritanceSuper1.class.getName(), results
                .get(1)
                .getClass()
                .getName());
        assertEquals(SingleTableInheritanceSub1.class.getName(), results
                .get(2)
                .getClass()
                .getName());
        assertEquals(SingleTableInheritanceSub2.class.getName(), results
                .get(3)
                .getClass()
                .getName());
    }

    public void testSelectSub() throws Exception {
        ItestTableUtils helper = getTableHelper("ST_INHERITANCE");
        helper.deleteAll();
        helper.setColumns("id", "objectType", "propertyA", "propertyB", "propertyC");
        helper.insert(1, "A", "1", null, null);
        helper.insert(2, "A", "2", null, null);
        helper.insert(3, "B", "3", "BX", null);
        helper.insert(4, "C", "4", null, "CX");

        Query query = getEntityManager().createQuery(
                "select a FROM SingleTableInheritanceSub1 a ORDER BY a.propertyA");
        List<?> results = query.getResultList();
        assertEquals(1, results.size());

        assertEquals(SingleTableInheritanceSub1.class.getName(), results
                .get(0)
                .getClass()
                .getName());
    }
}
