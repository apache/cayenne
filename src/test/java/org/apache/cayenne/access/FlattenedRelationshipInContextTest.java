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

import java.util.List;

import org.apache.cayenne.Fault;
import org.apache.cayenne.FlattenedRelationshipsTest;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.testdo.relationship.FlattenedTest3;
import org.apache.cayenne.unit.RelationshipCase;

public class FlattenedRelationshipInContextTest extends RelationshipCase {

    public void testIsToOneTargetModifiedFlattenedFault1() throws Exception {
        deleteTestData();
        getAccessStack().createTestData(FlattenedRelationshipsTest.class, "test", null);
        DataContext context = createDataContext();

        // fetch
        List ft3s = context.performQuery(new SelectQuery(FlattenedTest3.class));
        assertEquals(1, ft3s.size());
        FlattenedTest3 ft3 = (FlattenedTest3) ft3s.get(0);

        // mark as dirty for the purpose of the test...
        ft3.setPersistenceState(PersistenceState.MODIFIED);

        assertTrue(ft3.readPropertyDirectly("toFT1") instanceof Fault);

        // test that checking for modifications does not trigger a fault, and generally
        // works well

        ClassDescriptor d = context.getEntityResolver().getClassDescriptor(
                "FlattenedTest3");
        ArcProperty flattenedRel = (ArcProperty) d.getProperty("toFT1");

        ObjectDiff diff = context.getObjectStore().registerDiff(ft3.getObjectId(), null);
        assertFalse(DataRowUtils.isToOneTargetModified(flattenedRel, ft3, diff));
        assertTrue(ft3.readPropertyDirectly("toFT1") instanceof Fault);
    }

}
