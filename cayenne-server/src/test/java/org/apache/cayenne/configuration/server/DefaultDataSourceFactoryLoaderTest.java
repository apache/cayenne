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

import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.configuration.mock.MockDataSourceFactory1;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.di.spi.DefaultClassLoaderManager;
import org.apache.cayenne.log.CommonsJdbcEventLogger;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.resource.ResourceLocator;
import org.apache.cayenne.resource.mock.MockResourceLocator;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultDataSourceFactoryLoaderTest {

    private Injector injector;

    @Before
    public void setUp() throws Exception {
        Module testModule = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);
                binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
                binder.bind(ResourceLocator.class).to(MockResourceLocator.class);
                binder.bind(Key.get(ResourceLocator.class, Constants.SERVER_RESOURCE_LOCATOR)).to(MockResourceLocator.class);
                binder.bind(RuntimeProperties.class).toInstance(mock(RuntimeProperties.class));
                binder.bind(JdbcEventLogger.class).to(CommonsJdbcEventLogger.class);
            }
        };

        this.injector = DIBootstrap.createInjector(testModule);
    }

    @Test
    public void testGetDataSourceFactory_Implicit() throws Exception {

        DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor();
        nodeDescriptor.setName("node1");
        nodeDescriptor.setDataSourceDescriptor(new DataSourceInfo());

        DelegatingDataSourceFactory factoryLoader = new DelegatingDataSourceFactory();
        injector.injectMembers(factoryLoader);

        DataSourceFactory factory = factoryLoader.getDataSourceFactory(nodeDescriptor);
        assertNotNull(factory);
        assertTrue(factory instanceof XMLPoolingDataSourceFactory);
    }

    @Test
    public void testGetDataSourceFactory_Explicit() throws Exception {

        DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor();
        nodeDescriptor.setName("node1");
        nodeDescriptor.setDataSourceFactoryType(MockDataSourceFactory1.class.getName());

        DelegatingDataSourceFactory factoryLoader = new DelegatingDataSourceFactory();
        injector.injectMembers(factoryLoader);

        DataSourceFactory factory = factoryLoader.getDataSourceFactory(nodeDescriptor);
        assertNotNull(factory);
        assertTrue(factory instanceof MockDataSourceFactory1);
        assertSame("Injection on the factory hasn't been performed", injector,
                ((MockDataSourceFactory1) factory).getInjector());
    }

    @Test
    public void testGetDataSourceFactory_Property() throws Exception {

        final RuntimeProperties properties = mock(RuntimeProperties.class);
        when(properties.get(Constants.JDBC_DRIVER_PROPERTY)).thenReturn("x");
        when(properties.get(Constants.JDBC_URL_PROPERTY)).thenReturn("y");

        DataChannelDescriptor channelDescriptor = new DataChannelDescriptor();
        channelDescriptor.setName("X");
        DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor();
        nodeDescriptor.setName("node1");
        nodeDescriptor.setDataSourceFactoryType(MockDataSourceFactory1.class.getName());
        nodeDescriptor.setDataChannelDescriptor(channelDescriptor);

        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);
                binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
                binder.bind(ResourceLocator.class).to(MockResourceLocator.class);
                binder.bind(Key.get(ResourceLocator.class, Constants.SERVER_RESOURCE_LOCATOR)).to(MockResourceLocator.class);
                binder.bind(RuntimeProperties.class).toInstance(properties);
                binder.bind(JdbcEventLogger.class).to(CommonsJdbcEventLogger.class);
            }
        };

        Injector injector = DIBootstrap.createInjector(testModule);

        DelegatingDataSourceFactory factoryLoader = new DelegatingDataSourceFactory();
        injector.injectMembers(factoryLoader);

        DataSourceFactory factory = factoryLoader.getDataSourceFactory(nodeDescriptor);
        assertNotNull(factory);
        assertTrue(factory instanceof PropertyDataSourceFactory);

        when(properties.get(Constants.JDBC_URL_PROPERTY)).thenReturn(null);
        factory = factoryLoader.getDataSourceFactory(nodeDescriptor);
        assertNotNull(factory);
        assertFalse(factory instanceof PropertyDataSourceFactory);

        when(properties.get(Constants.JDBC_URL_PROPERTY + ".X.node2")).thenReturn("y");
        factory = factoryLoader.getDataSourceFactory(nodeDescriptor);
        assertNotNull(factory);
        assertFalse(factory instanceof PropertyDataSourceFactory);

        when(properties.get(Constants.JDBC_URL_PROPERTY + ".X.node1")).thenReturn("y");
        factory = factoryLoader.getDataSourceFactory(nodeDescriptor);
        assertNotNull(factory);
        assertTrue(factory instanceof PropertyDataSourceFactory);
    }
}
