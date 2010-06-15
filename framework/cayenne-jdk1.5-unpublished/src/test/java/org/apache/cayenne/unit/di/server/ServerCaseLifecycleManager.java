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
package org.apache.cayenne.unit.di.server;

import junit.framework.TestCase;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.unit.di.DefaultUnitTestLifecycleManager;
import org.apache.cayenne.unit.di.UnitTestScope;

public class ServerCaseLifecycleManager extends DefaultUnitTestLifecycleManager {

    @Inject
    protected Provider<ServerCaseProperties> propertiesProvider;

    @Inject
    protected ServerRuntimeFactory runtimeFactory;

    public ServerCaseLifecycleManager(UnitTestScope scope) {
        super(scope);
    }

    @Override
    public <T extends TestCase> void setUp(T testCase) {

        // init current runtime
        UseServerRuntime runtimeName = testCase.getClass().getAnnotation(
                UseServerRuntime.class);

        String location = runtimeName != null ? runtimeName.value() : null;
        propertiesProvider.get().setConfigurationLocation(location);

        // clear shared caches
        if (location != null) {
            DataDomain channel = (DataDomain) runtimeFactory.get(location).getChannel();
            channel.getEventManager().removeAllListeners(
                    channel.getSharedSnapshotCache().getSnapshotEventSubject());
            channel.getSharedSnapshotCache().clear();
            channel.getQueryCache().clear();
        }

        super.setUp(testCase);
    }
}
