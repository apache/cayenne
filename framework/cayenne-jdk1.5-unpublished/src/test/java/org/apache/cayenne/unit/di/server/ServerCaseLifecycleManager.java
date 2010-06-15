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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.di.spi.DefaultScope;
import org.apache.cayenne.unit.di.DefaultUnitTestLifecycleManager;

public class ServerCaseLifecycleManager extends DefaultUnitTestLifecycleManager {

    @Inject
    protected Provider<ServerCaseProperties> propertiesProvider;

    public ServerCaseLifecycleManager(DefaultScope scope) {
        super(scope);
    }

    @Override
    public <T extends TestCase> void setUp(T testCase) {

        // init current runtime
        UseServerRuntime runtimeName = testCase.getClass().getAnnotation(
                UseServerRuntime.class);

        String location = runtimeName != null ? runtimeName.value() : null;
        propertiesProvider.get().setConfigurationLocation(location);

        super.setUp(testCase);
    }
}
