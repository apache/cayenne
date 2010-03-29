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

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.query.SelectQuery;

public class OneToManyObjectTest extends PojoContextCase {

    public void testSelectToMany() throws Exception {
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

        getDbHelper().insert("many_to_one_entity1", new String[] {
                "id", "one_to_many_entity1_id"
        }, new Object[] {
                17, 5
        });

        SelectQuery q = new SelectQuery(OneToManyEntity1.class);
        List results = context.performQuery(q);
        assertEquals(1, results.size());

        OneToManyEntity1 o1 = (OneToManyEntity1) results.get(0);

        Field f = OneToManyEntity1.class.getDeclaredField("$cay_faultResolved_toMany");
        assertEquals(Boolean.FALSE, f.get(o1));

        List<ManyToOneEntity1> or = o1.getToMany();
        assertNotNull(or);

        assertEquals(Boolean.TRUE, f.get(o1));
        assertEquals(2, or.size());
        assertSame(o1, or.get(0).getToOne());
    }

    public void testSelectToManyWithPrefetch() throws Exception {
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

        getDbHelper().insert("many_to_one_entity1", new String[] {
                "id", "one_to_many_entity1_id"
        }, new Object[] {
                17, 5
        });

        SelectQuery q = new SelectQuery(OneToManyEntity1.class);
        q.addPrefetch("toMany");
        List results = context.performQuery(q);
        assertEquals(1, results.size());

        OneToManyEntity1 o1 = (OneToManyEntity1) results.get(0);

        Field f = OneToManyEntity1.class.getDeclaredField("$cay_faultResolved_toMany");
        assertEquals(Boolean.TRUE, f.get(o1));

        List<ManyToOneEntity1> or;
        blockDomainQueries();
        try {
            or = o1.getToMany();
            assertNotNull(or);
            assertEquals(2, or.size());
            assertEquals(PersistenceState.COMMITTED, ((Persistent) or.get(0))
                    .getPersistenceState());
            assertSame(o1, or.get(0).getToOne());
        }
        finally {
            unblockDomainQueries();
        }
    }

    public void testNew() throws Exception {
        getDbHelper().deleteAll("many_to_one_entity1");
        getDbHelper().deleteAll("one_to_many_entity1");

        OneToManyEntity1 o1 = (OneToManyEntity1) context
                .newObject(OneToManyEntity1.class);
        assertNotNull(o1.getToMany());
        assertEquals(0, o1.getToMany().size());

        ManyToOneEntity1 o2 = (ManyToOneEntity1) context
                .newObject(ManyToOneEntity1.class);
        o1.getToMany().add(o2);

        assertEquals(1, o1.getToMany().size());

        ManyToOneEntity1 o3 = (ManyToOneEntity1) context
                .newObject(ManyToOneEntity1.class);
        o1.getToMany().add(o3);

        context.commitChanges();
        assertEquals(2, getDbHelper().getRowCount("many_to_one_entity1"));
        assertEquals(1, getDbHelper().getRowCount("one_to_many_entity1"));
    }
}
