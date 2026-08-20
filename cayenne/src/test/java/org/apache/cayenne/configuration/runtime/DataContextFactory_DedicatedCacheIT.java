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
package org.apache.cayenne.configuration.runtime;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.*;

public class DataContextFactory_DedicatedCacheIT {

    // the project turns the shared snapshot cache off, so each DataContext gets a cache of its own
    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.DEDICATED_CACHE_PROJECT);

    @Test
    public void createDataContextWithDedicatedCache() {

        DataDomain domain = env.runtime().getDataDomain();

        DataContext c3 = (DataContext) env.runtime().getInjector()
                .getInstance(ObjectContextFactory.class)
                .createContext();

        assertNotNull(c3.getObjectStore().getDataRowCache());
        assertNull(domain.getSharedSnapshotCache());
        assertNotSame(
                c3.getObjectStore().getDataRowCache(),
                domain.getSharedSnapshotCache());
    }
}
