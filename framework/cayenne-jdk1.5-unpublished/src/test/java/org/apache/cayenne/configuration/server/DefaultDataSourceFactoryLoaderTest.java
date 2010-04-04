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
package org.apache.cayenne.configuration.server;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cayenne.configuration.AdhocObjectFactory;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.DefaultAdhocObjectFactory;
import org.apache.cayenne.configuration.mock.MockDataSourceFactory1;
import org.apache.cayenne.configuration.server.DataSourceFactory;
import org.apache.cayenne.configuration.server.DefaultDataSourceFactoryLoader;
import org.apache.cayenne.configuration.server.PropertyDataSourceFactory;
import org.apache.cayenne.configuration.server.XMLPoolingDataSourceFactory;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.resource.ResourceLocator;
import org.apache.cayenne.resource.mock.MockResourceLocator;

public class DefaultDataSourceFactoryLoaderTest extends TestCase {

    public void testGetDataSourceFactory_Implicit() throws Exception {

        DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor();
        nodeDescriptor.setName("node1");
        nodeDescriptor.setDataSourceDescriptor(new DataSourceInfo());

        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
                binder.bind(ResourceLocator.class).to(MockResourceLocator.class);
            }
        };

        Injector injector = DIBootstrap.createInjector(testModule);

        DefaultDataSourceFactoryLoader factoryLoader = new DefaultDataSourceFactoryLoader();
        injector.injectMembers(factoryLoader);

        DataSourceFactory factory = factoryLoader.getDataSourceFactory(nodeDescriptor);
        assertNotNull(factory);
        assertTrue(factory instanceof XMLPoolingDataSourceFactory);
    }

    public void testGetDataSourceFactory_Explicit() throws Exception {

        DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor();
        nodeDescriptor.setName("node1");
        nodeDescriptor.setDataSourceFactoryType(MockDataSourceFactory1.class.getName());

        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
                binder.bind(ResourceLocator.class).to(MockResourceLocator.class);
            }
        };

        Injector injector = DIBootstrap.createInjector(testModule);

        DefaultDataSourceFactoryLoader factoryLoader = new DefaultDataSourceFactoryLoader();
        injector.injectMembers(factoryLoader);

        DataSourceFactory factory = factoryLoader.getDataSourceFactory(nodeDescriptor);
        assertNotNull(factory);
        assertTrue(factory instanceof MockDataSourceFactory1);
        assertSame(
                "Injection on the factory hasn't been performed",
                injector,
                ((MockDataSourceFactory1) factory).getInjector());
    }

    public void testGetDataSourceFactory_Property() throws Exception {

        DataChannelDescriptor channelDescriptor = new DataChannelDescriptor();
        channelDescriptor.setName("X");
        DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor();
        nodeDescriptor.setName("node1");
        nodeDescriptor.setDataSourceFactoryType(MockDataSourceFactory1.class.getName());
        nodeDescriptor.setDataChannelDescriptor(channelDescriptor);

        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
                binder.bind(ResourceLocator.class).to(MockResourceLocator.class);
            }
        };

        Injector injector = DIBootstrap.createInjector(testModule);

        final Map<String, String> properties = new HashMap<String, String>();

        properties.put(PropertyDataSourceFactory.JDBC_DRIVER_PROPERTY, "x");
        properties.put(PropertyDataSourceFactory.JDBC_URL_PROPERTY, "y");
        DefaultDataSourceFactoryLoader factoryLoader = new DefaultDataSourceFactoryLoader() {

            @Override
            protected String getProperty(String key) {
                return properties.get(key);
            }
        };
        injector.injectMembers(factoryLoader);

        DataSourceFactory factory = factoryLoader.getDataSourceFactory(nodeDescriptor);
        assertNotNull(factory);
        assertTrue(factory instanceof PropertyDataSourceFactory);

        properties.remove(PropertyDataSourceFactory.JDBC_URL_PROPERTY);
        factory = factoryLoader.getDataSourceFactory(nodeDescriptor);
        assertNotNull(factory);
        assertFalse(factory instanceof PropertyDataSourceFactory);

        properties.put(PropertyDataSourceFactory.JDBC_URL_PROPERTY + ".X.node2", "y");
        factory = factoryLoader.getDataSourceFactory(nodeDescriptor);
        assertNotNull(factory);
        assertFalse(factory instanceof PropertyDataSourceFactory);
        
        properties.put(PropertyDataSourceFactory.JDBC_URL_PROPERTY + ".X.node1", "y");
        factory = factoryLoader.getDataSourceFactory(nodeDescriptor);
        assertNotNull(factory);
        assertTrue(factory instanceof PropertyDataSourceFactory);
    }
}
