/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.access;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.relationships_flattened.FlattenedTest1;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataContextEJBQLFlattenedRelationshipsIT {
    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.RELATIONSHIPS_FLATTENED_PROJECT);

    protected TableHelper ft1Helper;
    protected TableHelper ft2Helper;
    protected TableHelper ft3Helper;
    protected TableHelper ft4Helper;

    
    @BeforeEach
    public void setUp() throws Exception {
        ft1Helper = env.table("FLATTENED_TEST_1", "FT1_ID", "NAME");

        ft2Helper = env.table("FLATTENED_TEST_2", "FT2_ID", "FT1_ID", "NAME");

        ft3Helper = env.table("FLATTENED_TEST_3", "FT3_ID", "FT2_ID", "NAME");

        ft4Helper = env.table("FLATTENED_TEST_4", "FT4_ID", "FT3_ID", "NAME");
    }

    private void createFt123() throws Exception {
        ft1Helper.insert(1, "ft1").insert(2, "ft12");
        ft2Helper.insert(1, 1, "ft2").insert(2, 2, "ft22");
        ft3Helper.insert(1, 1, "ft3").insert(2, 2, "ft3-a").insert(3, 2, "ft3-b");
    }

    private void createFt1234() throws Exception {
        createFt123();
        ft4Helper.insert(1, 1, "ft4");
    }

    @Test
    public void collectionMemberOfThetaJoin() throws Exception {
        createFt123();

        String ejbql = "SELECT f FROM FlattenedTest3 f, FlattenedTest1 ft "
                + "WHERE f MEMBER OF ft.ft3Array AND ft = :ft";

        FlattenedTest1 ft = Cayenne.objectForPK(env.context(), FlattenedTest1.class, 2);
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("ft", ft);

        List<?> objects = env.context().performQuery(query);
        assertEquals(2, objects.size());

        Set<Object> ids = new HashSet<>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = Cayenne.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(2));
        assertTrue(ids.contains(3));
    }

    @Test
    public void collectionMemberOfThetaJoinLongRelationshipSequence()
            throws Exception {

        createFt1234();
        String ejbql = "SELECT f FROM FlattenedTest4 f, FlattenedTest1 ft "
                + "WHERE f MEMBER OF ft.ft4ArrayFor1 AND ft = :ft";

        FlattenedTest1 ft = Cayenne.objectForPK(env.context(), FlattenedTest1.class, 1);
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("ft", ft);

        List<?> objects = env.context().performQuery(query);
        assertEquals(1, objects.size());

        Set<Object> ids = new HashSet<>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = Cayenne.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(1));

        ft = Cayenne.objectForPK(env.context(), FlattenedTest1.class, 2);
        query = new EJBQLQuery(ejbql);
        query.setParameter("ft", ft);

        objects = env.context().performQuery(query);
        assertEquals(0, objects.size());
    }

    @Test
    public void collectionInnerJoin() throws Exception {

        createFt123();
        String ejbql = "SELECT ft FROM FlattenedTest1 ft INNER JOIN ft.ft3Array f WHERE ft = :ft";

        FlattenedTest1 ft = Cayenne.objectForPK(env.context(), FlattenedTest1.class, 1);
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("ft", ft);

        List<?> objects = env.context().performQuery(query);
        assertNotNull(objects);
        assertFalse(objects.isEmpty());
        assertEquals(1, objects.size());

        Set<Object> ids = new HashSet<>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = Cayenne.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(1));

    }

    @Test
    public void collectionAsInnerJoin() throws Exception {

        createFt123();

        // this query is equivalent to the previous INNER JOIN example
        String ejbql = "SELECT OBJECT(ft) FROM FlattenedTest1 ft, IN(ft.ft3Array) f WHERE ft = :ft";

        FlattenedTest1 ft = Cayenne.objectForPK(env.context(), FlattenedTest1.class, 1);
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("ft", ft);

        List<?> objects = env.context().performQuery(query);
        assertNotNull(objects);
        assertFalse(objects.isEmpty());
        assertEquals(1, objects.size());

        Set<Object> ids = new HashSet<>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = Cayenne.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(1));

    }

    @Test
    public void collectionThetaJoin() throws Exception {
        createFt123();

        String ejbql = "SELECT DISTINCT ft FROM FlattenedTest1 ft , FlattenedTest3 f3 WHERE f3.toFT1 = ft";

        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> objects = env.context().performQuery(query);
        assertNotNull(objects);
        assertFalse(objects.isEmpty());
        assertEquals(2, objects.size());

        Set<Object> ids = new HashSet<>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = Cayenne.pkForObject((Persistent) it.next());
            ids.add(id);
        }
        assertTrue(ids.contains(1));
        assertTrue(ids.contains(2));

    }

    @Test
    public void collectionIdentificationVariable() throws Exception {
        createFt123();

        String ejbql = "SELECT ft.ft3Array FROM FlattenedTest1 ft WHERE ft = :ft";

        FlattenedTest1 ft = Cayenne.objectForPK(env.context(), FlattenedTest1.class, 2);
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("ft", ft);

        List<?> objects = env.context().performQuery(query);
        assertNotNull(objects);
        assertFalse(objects.isEmpty());
        assertEquals(2, objects.size());

        Set<Object> ids = new HashSet<>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = Cayenne.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(2));
        assertTrue(ids.contains(3));

    }

    @Test
    public void associationFieldSelect() throws Exception {
        createFt123();

        String ejbql = "SELECT ft3.toFT1 FROM FlattenedTest3 ft3 WHERE ft3.toFT1 = :ft";

        FlattenedTest1 ft = Cayenne.objectForPK(env.context(), FlattenedTest1.class, 1);
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("ft", ft);

        List<?> objects = env.context().performQuery(query);
        assertEquals(1, objects.size());

        Set<Object> ids = new HashSet<>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = Cayenne.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(1));

    }

    @Test
    public void collectionSubquery() throws Exception {

        createFt123();

        String ejbql = "SELECT ft FROM FlattenedTest1 ft "
                + "WHERE (SELECT COUNT(f) FROM ft.ft3Array f) = 1";

        EJBQLQuery query = new EJBQLQuery(ejbql);

        // TODO fails but not because of flattened relationship,
        // the reason is that property "ft3Array" inside the subquery
        // parses as unmapped
        /*
         * List<?> objects = context.performQuery(query); assertNotNull(objects);
         * assertFalse(objects.isEmpty()); assertEquals(1, objects.size()); Set<Object>
         * ids = new HashSet<>(); Iterator<?> it = objects.iterator(); while
         * (it.hasNext()) { Object id = Cayenne.pkForObject((Persistent) it.next());
         * ids.add(id); } assertTrue(ids.contains(2));
         */

    }

    @Test
    public void collectionSubquery1() throws Exception {
        createFt123();

        String ejbql = "SELECT ft FROM FlattenedTest1 ft "
                + "WHERE (SELECT COUNT(f3) FROM FlattenedTest3 f3 WHERE f3 MEMBER OF ft.ft3Array) > 1";

        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> objects = env.context().performQuery(query);

        assertNotNull(objects);
        assertFalse(objects.isEmpty());
        assertEquals(1, objects.size());

        Set<Object> ids = new HashSet<>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = Cayenne.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(2));

    }

    @Test
    public void groupByFlattenedRelationship() throws Exception {

        createFt123();

        String ejbql = "SELECT COUNT(ft3), ft3.toFT1 FROM FlattenedTest3 ft3  GROUP BY ft3.toFT1 ";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List<?> objects = env.context().performQuery(query);
        assertEquals(2, objects.size());
    }
}
