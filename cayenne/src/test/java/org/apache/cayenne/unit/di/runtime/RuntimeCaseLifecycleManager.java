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
import org.apache.cayenne.di.spi.DefaultScope;
import org.apache.cayenne.unit.di.DefaultUnitTestLifecycleManager;

public class RuntimeCaseLifecycleManager extends DefaultUnitTestLifecycleManager {

    @Inject
    protected Provider<RuntimeCaseProperties> propertiesProvider;

    @Inject
    protected Provider<RuntimeCaseExtraModules> extraModulesProvider;

    public RuntimeCaseLifecycleManager(DefaultScope scope) {
        super(scope);
    }

    @Override
    public <T> void setUp(T testCase) {

        // init current runtime
        UseCayenneRuntime runtimeName = testCase.getClass().getAnnotation(UseCayenneRuntime.class);
        ExtraModules extraModules = testCase.getClass().getAnnotation(ExtraModules.class);

        String location = runtimeName != null
                ? runtimeName.value()
                : null;
        propertiesProvider.get().setConfigurationLocation(location);

        Class<?>[] modules = extraModules != null
                ? extraModules.value()
                : new Class[]{};
        extraModulesProvider.get().setExtraModules(modules);

        super.setUp(testCase);
    }
}
