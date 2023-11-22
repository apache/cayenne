/*
 *    Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.unit.util;

import org.apache.cayenne.di.spi.ModuleProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ModuleProviderChecker {

    private Class<? extends ModuleProvider> expectedProvider;
    private Class<? extends ModuleProvider> providerInterface;

    public static void testProviderPresent(Class<? extends ModuleProvider> expectedProvider,
                                           Class<? extends ModuleProvider> providerInterface) {
        new ModuleProviderChecker(expectedProvider, providerInterface).testProviderPresent();
    }

    protected ModuleProviderChecker(Class<? extends ModuleProvider> expectedProvider,
                                    Class<? extends ModuleProvider> providerInterface) {
        this.expectedProvider = Objects.requireNonNull(expectedProvider);
        this.providerInterface = Objects.requireNonNull(providerInterface);
        assertTrue("Provider interface expected", providerInterface.isInterface());
        if(expectedProvider.equals(providerInterface)) {
            fail("Expected provider class and required interface should be different.");
        }
    }

    protected void testProviderPresent() {

        List<ModuleProvider> providers = new ArrayList<>();
        for (ModuleProvider p : ServiceLoader.load(providerInterface)) {
            if (expectedProvider.equals(p.getClass())) {
                providers.add(p);
            }
        }

        switch (providers.size()) {
            case 0:
                fail("Expected provider '" + expectedProvider.getName() + "' is not found");
                break;
            case 1:
                break;
            default:
                fail("Expected provider '" + expectedProvider.getName() + "' is found more then once: " + providers.size());
                break;
        }
    }
}
