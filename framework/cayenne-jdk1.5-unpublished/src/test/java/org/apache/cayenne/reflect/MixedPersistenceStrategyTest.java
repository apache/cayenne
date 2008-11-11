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

package org.apache.cayenne.reflect;

import org.apache.art.MixedPersistenceStrategy;
import org.apache.art.MixedPersistenceStrategy2;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.unit.CayenneCase;

/**
 * Tests conflicts between field and map-based persistence.
 * 
 */
public class MixedPersistenceStrategyTest extends CayenneCase {

    public void testConflictingField1() throws Exception {
        deleteTestData();
        createTestData("testConflictingField");

        DataContext c = createDataContext();
        MixedPersistenceStrategy object = DataObjectUtils.objectForPK(
                c,
                MixedPersistenceStrategy.class,
                1);

        assertEquals(2, object.getDetails().size());
        assertTrue(object.getDetails() instanceof ValueHolder);
    }

    /**
     * This test case reproduces CAY-582 bug.
     */
    public void testConflictingField2() throws Exception {
        deleteTestData();
        createTestData("testConflictingField");

        DataContext c = createDataContext();
        MixedPersistenceStrategy2 detail1 = DataObjectUtils.objectForPK(
                c,
                MixedPersistenceStrategy2.class,
                1);

        MixedPersistenceStrategy2 detail2 = DataObjectUtils.objectForPK(
                c,
                MixedPersistenceStrategy2.class,
                2);

        // resolve master (this is where CAY-582 exception happens)
        assertEquals("n1", detail1.getMaster().getName());

        assertEquals(2, detail2.getMaster().getDetails().size());
        assertTrue(detail2.getMaster().getDetails() instanceof ValueHolder);
    }
}
