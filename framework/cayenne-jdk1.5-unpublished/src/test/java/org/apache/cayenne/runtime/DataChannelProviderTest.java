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
package org.apache.cayenne.runtime;

import java.util.Collections;

import junit.framework.TestCase;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataChannelDescriptorLoader;
import org.apache.cayenne.configuration.DefaultRuntimeProperties;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;

public class DataChannelProviderTest extends TestCase {

    public void testGet() {

        // create dependencies
        final String testConfigName = "testConfig";
        final DataChannelDescriptor testDescriptor = new DataChannelDescriptor();
        
        final DataChannelDescriptorLoader testLoader = new DataChannelDescriptorLoader() {

            public DataChannelDescriptor load(String runtimeName)
                    throws CayenneRuntimeException {
                assertEquals(testConfigName, runtimeName);
                return testDescriptor;
            }
        };

        final DefaultRuntimeProperties testProperties = new DefaultRuntimeProperties(
                Collections.singletonMap(
                        RuntimeProperties.CAYENNE_RUNTIME_NAME,
                        testConfigName));

        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(RuntimeProperties.class).toInstance(testProperties);
                binder.bind(DataChannelDescriptorLoader.class).toInstance(testLoader);
            }
        };

        Injector injector = DIBootstrap.createInjector(testModule);

        // create and initialize provide instance to test
        DataDomainProvider provider = new DataDomainProvider();
        injector.injectMembers(provider);

        DataChannel channel = provider.get();
        assertNotNull(channel);
        
        assertTrue(channel instanceof DataDomain);
    }
}
