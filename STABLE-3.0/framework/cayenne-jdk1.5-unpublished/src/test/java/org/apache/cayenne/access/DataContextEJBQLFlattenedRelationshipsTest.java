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
package org.apache.cayenne.access;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.testdo.relationship.FlattenedTest1;
import org.apache.cayenne.unit.RelationshipCase;

public class DataContextEJBQLFlattenedRelationshipsTest extends RelationshipCase {

    public void testCollectionMemberOfThetaJoin() throws Exception {
        createTestData("testCollectionMemberOfThetaJoin");

        String ejbql = "SELECT f FROM FlattenedTest3 f, FlattenedTest1 ft "
                + "WHERE f MEMBER OF ft.ft3Array AND ft = :ft";

        ObjectContext context = createDataContext();
        FlattenedTest1 ft = DataObjectUtils.objectForPK(context, FlattenedTest1.class, 2);
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("ft", ft);

        List<?> objects = context.performQuery(query);
        assertEquals(2, objects.size());

        Set<Object> ids = new HashSet<Object>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = DataObjectUtils.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(2)));
        assertTrue(ids.contains(new Integer(3)));
    }
    
    public void testCollectionMemberOfThetaJoinLongRelationshipSequence() throws Exception {
        String ejbql = "SELECT f FROM FlattenedTest4 f, FlattenedTest1 ft "
                + "WHERE f MEMBER OF ft.ft4ArrayFor1 AND ft = :ft";

        ObjectContext context = createDataContext();
        FlattenedTest1 ft = DataObjectUtils.objectForPK(context, FlattenedTest1.class, 1);
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("ft", ft);

        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Set<Object> ids = new HashSet<Object>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = DataObjectUtils.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(1)));
        
        
        ft = DataObjectUtils.objectForPK(context, FlattenedTest1.class, 2);
        query = new EJBQLQuery(ejbql);
        query.setParameter("ft", ft);

        objects = context.performQuery(query);
        assertEquals(0, objects.size());
    }

    public void testCollectionInnerJoin() throws Exception {
        String ejbql = "SELECT ft FROM FlattenedTest1 ft INNER JOIN ft.ft3Array f WHERE ft = :ft";

        ObjectContext context = createDataContext();
        FlattenedTest1 ft = DataObjectUtils.objectForPK(context, FlattenedTest1.class, 1);
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("ft", ft);

        List<?> objects = context.performQuery(query);
        assertNotNull(objects);
        assertFalse(objects.isEmpty());
        assertEquals(1, objects.size());

        Set<Object> ids = new HashSet<Object>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = DataObjectUtils.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(1)));
        
    }

    
    public void testCollectionAsInnerJoin() throws Exception {
        // this query is equivalent to the previous INNER JOIN example
        String ejbql = "SELECT OBJECT(ft) FROM FlattenedTest1 ft, IN(ft.ft3Array) f WHERE ft = :ft";

        ObjectContext context = createDataContext();
        FlattenedTest1 ft = DataObjectUtils.objectForPK(context, FlattenedTest1.class, 1);
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("ft", ft);
        
        List<?> objects = context.performQuery(query);
        assertNotNull(objects);
        assertFalse(objects.isEmpty());
        assertEquals(1, objects.size());

        Set<Object> ids = new HashSet<Object>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = DataObjectUtils.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(1)));
        
    }

    public void testCollectionThetaJoin() throws Exception {
        String ejbql = "SELECT DISTINCT ft FROM FlattenedTest1 ft , FlattenedTest3 f3 WHERE f3.toFT1 = ft";

        ObjectContext context = createDataContext();
        EJBQLQuery query = new EJBQLQuery(ejbql);
        
        List<?> objects = context.performQuery(query);
        assertNotNull(objects);
        assertFalse(objects.isEmpty());
        assertEquals(2, objects.size());

        Set<Object> ids = new HashSet<Object>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = DataObjectUtils.pkForObject((Persistent) it.next());
            ids.add(id);
        }
        assertTrue(ids.contains(new Integer(1)));
        assertTrue(ids.contains(new Integer(2)));

    }

    public void testCollectionIdentificationVariable() throws Exception {
        String ejbql = "SELECT ft.ft3Array FROM FlattenedTest1 ft WHERE ft = :ft";

        ObjectContext context = createDataContext();
        FlattenedTest1 ft = DataObjectUtils.objectForPK(context, FlattenedTest1.class, 2);
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("ft", ft);

        List<?> objects = context.performQuery(query);
        assertNotNull(objects);
        assertFalse(objects.isEmpty());
        assertEquals(2, objects.size());

        Set<Object> ids = new HashSet<Object>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = DataObjectUtils.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(2)));
        assertTrue(ids.contains(new Integer(3)));

    }

    public void testAssociationFieldSelect() throws Exception {
        
        String ejbql = "SELECT ft3.toFT1 FROM FlattenedTest3 ft3 WHERE ft3.toFT1 = :ft";
        
        ObjectContext context = createDataContext();
        FlattenedTest1 ft = DataObjectUtils.objectForPK(context, FlattenedTest1.class, 1);
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("ft", ft);

        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Set<Object> ids = new HashSet<Object>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = DataObjectUtils.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(1)));

    }
    
    public void testCollectionSubquery() throws Exception {
        String ejbql = "SELECT ft FROM FlattenedTest1 ft "
        + "WHERE (SELECT COUNT(f) FROM ft.ft3Array f) = 1";

        ObjectContext context = createDataContext();
        EJBQLQuery query = new EJBQLQuery(ejbql);
        
        // TODO fails but not because of flattened relationship,
        // the reason is that property "ft3Array" inside the subquery 
        // parses as unmapped
        /*List<?> objects = context.performQuery(query);
        assertNotNull(objects);
        assertFalse(objects.isEmpty());
        assertEquals(1, objects.size());

        Set<Object> ids = new HashSet<Object>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = DataObjectUtils.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(2)));*/
        

    }
    
    public void testCollectionSubquery1() throws Exception {
        String ejbql = "SELECT ft FROM FlattenedTest1 ft "
        + "WHERE (SELECT COUNT(f3) FROM FlattenedTest3 f3 WHERE f3 MEMBER OF ft.ft3Array) > 1";

        ObjectContext context = createDataContext();
        EJBQLQuery query = new EJBQLQuery(ejbql);
        
        List<?> objects = context.performQuery(query);
        
        assertNotNull(objects);
        assertFalse(objects.isEmpty());
        assertEquals(1, objects.size());

        Set<Object> ids = new HashSet<Object>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Object id = DataObjectUtils.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(2)));
    }
    public void testGroupByFlattenedRelationship() throws Exception {
        String ejbql = "SELECT COUNT(ft3), ft3.toFT1 FROM FlattenedTest3 ft3  GROUP BY ft3.toFT1 ";
  
         ObjectContext context = createDataContext();
         EJBQLQuery query = new EJBQLQuery(ejbql);
         List<?> objects = context.performQuery(query);
         assertEquals(2, objects.size());
    }
}
