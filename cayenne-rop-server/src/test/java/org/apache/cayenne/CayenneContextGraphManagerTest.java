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

package org.apache.cayenne;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @since 4.0
 */
public class CayenneContextGraphManagerTest {

    private CayenneContextGraphManager graphManager;

    @Before
    public void before() {
        CayenneContext mockContext = mock(CayenneContext.class);
        this.graphManager = new CayenneContextGraphManager(mockContext, false, false);
    }

    @Test
    public void testRegisterNode() {

        ObjectId id = ObjectId.of("E1", "ID", 500);
        Persistent object = mock(Persistent.class);

        graphManager.registerNode(id, object);
        assertSame(object, graphManager.getNode(id));
    }

    @Test
    public void testUnregisterNode() {

        ObjectId id = ObjectId.of("E1", "ID", 500);
        Persistent object = mock(Persistent.class);

        graphManager.registerNode(id, object);
        Object unregistered = graphManager.unregisterNode(id);
        assertSame(object, unregistered);

        verify(object, times(0)).setObjectId(null);
        verify(object).setObjectContext(null);
        verify(object).setPersistenceState(PersistenceState.TRANSIENT);
    }
}