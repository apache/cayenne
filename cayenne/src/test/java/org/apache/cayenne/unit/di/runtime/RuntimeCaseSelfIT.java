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
package org.apache.cayenne.unit.di.runtime;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class RuntimeCaseSelfIT extends RuntimeCase {

    @Inject
    protected CayenneRuntime runtime;
    
    @Inject
    protected Provider<CayenneRuntime> runtimeProvider;

    @Inject
    protected RuntimeCaseProperties properties;

    @Test
    public void testSetup_TearDown_Runtime() throws Exception {

        assertNotNull(properties);
        assertEquals(CayenneProjects.TESTMAP_PROJECT, properties.getConfigurationLocation());

        CayenneRuntime local = this.runtime;
        assertNotNull(local);
        assertSame(local, runtimeProvider.get());

        tearDownLifecycleManager();

        setUpLifecycleManager();
        assertNotSame(local, this.runtime);
    }

}
