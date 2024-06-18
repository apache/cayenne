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
package org.apache.cayenne.unit.di;

import org.apache.cayenne.di.BeforeScopeEnd;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.spi.DefaultScope;

public class DefaultUnitTestLifecycleManager implements UnitTestLifecycleManager {

    @Inject
    protected Injector injector;

    protected DefaultScope scope;

    public DefaultUnitTestLifecycleManager(DefaultScope scope) {
        this.scope = scope;
    }

    public <T> void setUp(T testCase) {
        injector.injectMembers(testCase);
    }

    @BeforeScopeEnd
    public <T> void tearDown(T testCase) {
        scope.shutdown();
    }
}
