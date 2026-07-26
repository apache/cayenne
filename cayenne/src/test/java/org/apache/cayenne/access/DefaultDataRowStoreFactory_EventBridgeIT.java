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

import org.apache.cayenne.event.EventBridge;
import org.apache.cayenne.event.MockEventBridge;
import org.apache.cayenne.event.MockEventBridgeProvider;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultDataRowStoreFactory_EventBridgeIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.MULTI_TIER_PROJECT)
            .withExtraModules(b -> b.bind(EventBridge.class).toProvider(MockEventBridgeProvider.class));

    @Test
    public void createDataRowStore() {
        DataRowStore dataStore = env.runtime().getInjector()
                .getInstance(DataRowStoreFactory.class)
                .createDataRowStore("test");

        assertEquals(MockEventBridge.class, dataStore.getEventBridge().getClass());
    }
}
