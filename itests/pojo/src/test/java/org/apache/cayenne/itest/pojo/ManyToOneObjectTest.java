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
package org.apache.cayenne.itest.pojo;

import java.lang.reflect.Field;
import java.util.List;

import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.query.SelectQuery;

public class ManyToOneObjectTest extends PojoContextCase {

    public void testSelectToOne() throws Exception {
        getDbHelper().deleteAll("many_to_one_entity1");
        getDbHelper().deleteAll("one_to_many_entity1");
        getDbHelper().insert("one_to_many_entity1", new String[] {
            "id"
        }, new Object[] {
            5
        });

        getDbHelper().insert("many_to_one_entity1", new String[] {
                "id", "one_to_many_entity1_id"
        }, new Object[] {
                16, 5
        });

        SelectQuery q = new SelectQuery(ManyToOneEntity1.class);
        List results = context.performQuery(q);
        assertEquals(1, results.size());

        ManyToOneEntity1 o1 = (ManyToOneEntity1) results.get(0);

        Field f = ManyToOneEntity1.class.getDeclaredField("$cay_faultResolved_toOne");
        assertEquals(Boolean.FALSE, f.get(o1));

        OneToManyEntity1 or = o1.getToOne();
        assertNotNull(or);

        assertEquals(Boolean.TRUE, f.get(o1));
    }

    public void testSelectToOneWithPrefetch() throws Exception {
        getDbHelper().deleteAll("many_to_one_entity1");
        getDbHelper().deleteAll("one_to_many_entity1");
        getDbHelper().insert("one_to_many_entity1", new String[] {
            "id"
        }, new Object[] {
            5
        });

        getDbHelper().insert("many_to_one_entity1", new String[] {
                "id", "one_to_many_entity1_id"
        }, new Object[] {
                16, 5
        });

        SelectQuery q = new SelectQuery(ManyToOneEntity1.class);
        q.addPrefetch("toOne");
        List results = context.performQuery(q);
        assertEquals(1, results.size());

        ManyToOneEntity1 o1 = (ManyToOneEntity1) results.get(0);

        // at this point the relationship is still fault, but it must resolve from cache
        // without going to db
        OneToManyEntity1 or;

        blockDomainQueries();
        try {
            or = o1.getToOne();
            assertNotNull(or);

            Field f = ManyToOneEntity1.class.getDeclaredField("$cay_faultResolved_toOne");
            assertEquals(Boolean.TRUE, f.get(o1));
            assertEquals(5, DataObjectUtils.intPKForObject((Persistent) or));
        }
        finally {
            unblockDomainQueries();
        }

        assertSame(o1, or.getToMany().get(0));
    }
}
