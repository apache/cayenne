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
package org.apache.cayenne.unit.di.client;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.di.spi.DefaultScope;
import org.apache.cayenne.unit.di.server.ServerCaseLifecycleManager;

public class ClientCaseLifecycleManager extends ServerCaseLifecycleManager {

    @Inject
    protected Provider<ClientCaseProperties> propertiesProvider;

    public ClientCaseLifecycleManager(DefaultScope scope) {
        super(scope);
    }

    @Override
    public <T extends TestCase> void setUp(T testCase) {

        Map<String, String> map = new HashMap<String, String>();

        ClientRuntimeProperty properties = testCase.getClass().getAnnotation(
                ClientRuntimeProperty.class);

        if (properties != null) {
            String[] pairs = properties.value();
            if (pairs != null && pairs.length > 1) {

                String key = null;

                for (int i = 0; i < pairs.length; i++) {
                    if (i % 2 == 0) {
                        key = pairs[i];
                    }
                    else {
                        map.put(key, pairs[i]);
                    }
                }
            }
        }

        propertiesProvider.get().setRuntimeProperties(map);

        super.setUp(testCase);
    }
}
