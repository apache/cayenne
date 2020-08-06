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
package org.apache.cayenne.configuration.xml;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.DefaultConfigurationNameMapper;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.di.spi.DefaultClassLoaderManager;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.URLResource;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.XMLReader;

import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.*;

public class XMLDataChannelDescriptorLoaderTest {

    private Injector injector;

    @Before
    public void setUp() {
        Module testModule = binder -> {
            binder.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);
            binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
            binder.bind(DataMapLoader.class).to(XMLDataMapLoader.class);
            binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);
            binder.bind(HandlerFactory.class).to(DefaultHandlerFactory.class);
            binder.bind(DataChannelMetaData.class).to(NoopDataChannelMetaData.class);
            binder.bind(XMLReader.class).toProviderInstance(new XMLReaderProvider(false)).withoutScope();
        };

        this.injector = DIBootstrap.createInjector(testModule);
    }

    @Test
    public void testLoadEmpty() {

        // create and initialize loader instance to test
        XMLDataChannelDescriptorLoader loader = new XMLDataChannelDescriptorLoader();
        injector.injectMembers(loader);

        String testConfigName = "testConfig1";

        URL url = getClass().getResource("cayenne-" + testConfigName + ".xml");
        ConfigurationTree<DataChannelDescriptor> tree = loader.load(new URLResource(url));

        assertNotNull(tree);
        assertNotNull(tree.getRootNode());
        assertEquals(testConfigName, tree.getRootNode().getName());
    }

    @Test
    public void testLoad_MissingConfig() throws Exception {

        // create and initialize loader instance to test
        XMLDataChannelDescriptorLoader loader = new XMLDataChannelDescriptorLoader();
        injector.injectMembers(loader);

        try {
            loader.load(new URLResource(new URL("file:///no_such_resource")));
            fail("No exception was thrown on bad absent config name");
        } catch (ConfigurationException e) {
            // expected
        }
    }

    @Test(expected = CayenneRuntimeException.class)
    public void loadInvalidVersion() throws Exception {
        XMLDataChannelDescriptorLoader loader = new XMLDataChannelDescriptorLoader();
        injector.injectMembers(loader);

        URL url = getClass().getResource("cayenne-testConfig4.xml");
        loader.load(new URLResource(url));
    }

    @Test(expected = CayenneRuntimeException.class)
    public void loadInvalidNamespace() throws Exception {
        XMLDataChannelDescriptorLoader loader = new XMLDataChannelDescriptorLoader();
        injector.injectMembers(loader);

        URL url = getClass().getResource("cayenne-testConfig5.xml");
        loader.load(new URLResource(url));
    }

    @Test
    public void testLoadDataMap() {

        // create and initialize loader instance to test
        XMLDataChannelDescriptorLoader loader = new XMLDataChannelDescriptorLoader();
        injector.injectMembers(loader);

        String testConfigName = "testConfig2";
        URL url = getClass().getResource("cayenne-" + testConfigName + ".xml");

        ConfigurationTree<DataChannelDescriptor> tree = loader.load(new URLResource(url));

        assertNotNull(tree);
        assertNotNull(tree.getRootNode());

        assertEquals(testConfigName, tree.getRootNode().getName());

        Collection<DataMap> maps = tree.getRootNode().getDataMaps();
        assertEquals(1, maps.size());
        assertEquals("testConfigMap2", maps.iterator().next().getName());
    }

    @Test
    public void testLoadDataEverything() {

        // create and initialize loader instance to test
        XMLDataChannelDescriptorLoader loader = new XMLDataChannelDescriptorLoader();
        injector.injectMembers(loader);

        String testConfigName = "testConfig3";
        URL url = getClass().getResource("cayenne-" + testConfigName + ".xml");

        ConfigurationTree<DataChannelDescriptor> tree = loader.load(new URLResource(url));

        assertNotNull(tree);

        DataChannelDescriptor descriptor = tree.getRootNode();
        assertNotNull(descriptor);
        assertEquals(testConfigName, descriptor.getName());

        Collection<DataMap> maps = descriptor.getDataMaps();
        assertEquals(2, maps.size());

        Iterator<DataMap> mapsIt = maps.iterator();

        DataMap map1 = mapsIt.next();
        DataMap map2 = mapsIt.next();

        assertEquals("testConfigMap3_1", map1.getName());
        assertEquals("testConfigMap3_2", map2.getName());

        Collection<DataNodeDescriptor> nodes = descriptor.getNodeDescriptors();
        assertEquals(1, nodes.size());

        DataNodeDescriptor node1 = nodes.iterator().next();
        assertEquals("testConfigNode3", node1.getName());
        assertNull(node1.getParameters());
        assertNotNull(node1.getDataSourceDescriptor());
        assertEquals(1, node1.getDataSourceDescriptor().getMinConnections());
        assertEquals(1, node1.getDataSourceDescriptor().getMaxConnections());

        assertEquals("org.example.test.Adapter", node1.getAdapterType());
        assertEquals("org.example.test.DataSourceFactory", node1.getDataSourceFactoryType());
        assertEquals("org.example.test.SchemaUpdateStartegy", node1.getSchemaUpdateStrategyType());
        assertNotNull(node1.getDataMapNames());

        assertEquals(1, node1.getDataMapNames().size());

        assertEquals("testConfigMap3_2", node1.getDataMapNames().iterator().next());
    }
}
