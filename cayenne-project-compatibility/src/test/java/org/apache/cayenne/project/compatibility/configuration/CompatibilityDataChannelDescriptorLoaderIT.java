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

package org.apache.cayenne.project.compatibility.configuration;

import java.net.URL;

import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataChannelDescriptorLoader;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.xml.XMLReaderProvider;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.compatibility.CompatibilityTestModule;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.junit.Test;
import org.xml.sax.XMLReader;

import static org.junit.Assert.*;

/**
 * @since 4.1
 */
public class CompatibilityDataChannelDescriptorLoaderIT {

    @Test
    public void testLoad() {
        Injector injector = getInjector();

        DataChannelDescriptorLoader loader = injector.getInstance(DataChannelDescriptorLoader.class);
        assertTrue(loader instanceof CompatibilityDataChannelDescriptorLoader);

        URL resourceUrl = getClass().getResource("../cayenne-project-v6.xml");
        Resource resource = new URLResource(resourceUrl);

        ConfigurationTree<DataChannelDescriptor> configurationTree = loader.load(resource);
        assertNotNull(configurationTree.getRootNode());
        assertTrue(configurationTree.getLoadFailures().isEmpty());
        assertEquals(1, configurationTree.getRootNode().getDataMaps().size());

        DataMap dataMap = configurationTree.getRootNode().getDataMaps().iterator().next();
        assertEquals(1, dataMap.getDbEntities().size());
        assertEquals(1, dataMap.getObjEntities().size());
        assertNotNull(dataMap.getObjEntity("Artist"));
        assertNotNull(dataMap.getDbEntity("Artist"));
        assertEquals(2, dataMap.getDbEntity("Artist").getAttributes().size());
    }

    private Injector getInjector() {
        return DIBootstrap.createInjector(
            new CompatibilityTestModule(),
            binder -> {
                binder.bind(XMLReader.class).toProviderInstance(new XMLReaderProvider(false)).withoutScope();
                binder.bind(DataChannelDescriptorLoader.class).to(CompatibilityDataChannelDescriptorLoader.class);
                binder.bind(DataMapLoader.class).to(CompatibilityDataMapLoader.class);
            }
        );
    }
}