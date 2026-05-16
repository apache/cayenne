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

package org.apache.cayenne.reflect;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.mixed_persistence_strategy.MixedPersistenceStrategy;
import org.apache.cayenne.testdo.mixed_persistence_strategy.MixedPersistenceStrategy2;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests conflicts between field and map-based persistence.
 */
public class MixedPersistenceStrategyIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.MIXED_PERSISTENCE_STRATEGY_PROJECT);

    protected TableHelper tMixedPersistenceStrategy;
    protected TableHelper tMixedPersistenceStrategy2;

    @BeforeEach
    public void setUp() throws Exception {
        tMixedPersistenceStrategy = env.table("MIXED_PERSISTENCE_STRATEGY", "ID", "DESCRIPTION", "NAME");

        tMixedPersistenceStrategy2 = env.table("MIXED_PERSISTENCE_STRATEGY2", "ID", "MASTER_ID", "NAME");
    }

    protected void createConflictingFieldDataSet() throws Exception {
        tMixedPersistenceStrategy.insert(1, "d1", "n1");
        tMixedPersistenceStrategy2.insert(1, 1, "dn1");
        tMixedPersistenceStrategy2.insert(2, 1, "dn2");
    }

    @Test
    public void conflictingField1() throws Exception {

        createConflictingFieldDataSet();

        MixedPersistenceStrategy object = Cayenne.objectForPK(
                env.context(),
                MixedPersistenceStrategy.class,
                1);

        assertEquals(2, object.getDetails().size());
        assertTrue(object.getDetails() instanceof ValueHolder);
    }

    /**
     * This test case reproduces CAY-582 bug.
     */
    @Test
    public void conflictingField2() throws Exception {

        createConflictingFieldDataSet();

        MixedPersistenceStrategy2 detail1 = Cayenne.objectForPK(
                env.context(),
                MixedPersistenceStrategy2.class,
                1);

        MixedPersistenceStrategy2 detail2 = Cayenne.objectForPK(
                env.context(),
                MixedPersistenceStrategy2.class,
                2);

        // resolve master (this is where CAY-582 exception happens)
        assertEquals("n1", detail1.getMaster().getName());

        assertEquals(2, detail2.getMaster().getDetails().size());
        assertTrue(detail2.getMaster().getDetails() instanceof ValueHolder);
    }
}
