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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.testdo.relationship.FlattenedTest1;
import org.apache.cayenne.unit.RelationshipCase;
import org.apache.cayenne.util.Cayenne;

public class DataContextEJBQLFlattenedRelationshipsTest extends RelationshipCase {

    public void testCollectionMemberOfThetaJoin() throws Exception {
        createTestData("testCollectionMemberOfThetaJoin");

        String ejbql = "SELECT f FROM FlattenedTest3 f, FlattenedTest1 ft "
                + "WHERE f MEMBER OF ft.ft3Array AND ft = :ft";

        ObjectContext context = createDataContext();
        FlattenedTest1 ft = Cayenne.objectForPK(context, FlattenedTest1.class, 1);
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("ft", ft);

        // TODO: andrus 2008/06/09 - this fails until we fix CAY-1069 (for correlated join
        // case see EJBQLConditionTranslator.visitMemberOf(..)
        // List<?> objects = context.performQuery(query);
        // assertEquals(2, objects.size());
        //
        // Set<Object> ids = new HashSet<Object>();
        // Iterator<?> it = objects.iterator();
        // while (it.hasNext()) {
        // Object id = Cayenne.pkForObject((Persistent) it.next());
        // ids.add(id);
        // }
        //
        // assertTrue(ids.contains(new Integer(2)));
        // assertTrue(ids.contains(new Integer(3)));
    }
}
