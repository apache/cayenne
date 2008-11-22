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

package org.apache.cayenne.map;

import java.util.Collections;
import java.util.List;

import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.relationship.ReflexiveAndToOne;
import org.apache.cayenne.unit.RelationshipCase;

/**
 */
public class AshwoodEntitySorterTest extends RelationshipCase {

    protected AshwoodEntitySorter sorter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();
        sorter = new AshwoodEntitySorter(getDomain().getDataMaps());
    }

    public void testSortObjectsForEntityReflexiveWithFaults() throws Exception {
        createTestData("testSortObjectsForEntityDeletedWithFaults");

        ObjEntity entity = getDomain().getEntityResolver().lookupObjEntity(
                ReflexiveAndToOne.class);

        List<?> objects = createDataContext().performQuery(
                new SelectQuery(ReflexiveAndToOne.class));
        Collections.shuffle(objects);
        assertEquals(3, objects.size());

        sorter.sortObjectsForEntity(entity, objects, true);

        assertEquals("r3", ((ReflexiveAndToOne) objects.get(0)).getName());
        assertEquals("r2", ((ReflexiveAndToOne) objects.get(1)).getName());
        assertEquals("r1", ((ReflexiveAndToOne) objects.get(2)).getName());
    }
}
