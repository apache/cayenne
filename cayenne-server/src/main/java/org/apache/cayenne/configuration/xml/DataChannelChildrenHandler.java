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

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

/**
 * @since 4.1
 */
final class DataChannelChildrenHandler extends NamespaceAwareNestedTagHandler {

    private static final Logger logger = LoggerFactory.getLogger(XMLDataChannelDescriptorLoader.class);

    static final String OLD_MAP_TAG = "map";
    static final String NODE_TAG = "node";
    static final String PROPERTY_TAG = "property";
    static final String DATA_MAP_TAG = "data-map";
    static final String DOMAIN_TAG = "domain";


    private XMLDataChannelDescriptorLoader xmlDataChannelDescriptorLoader;
    private DataChannelDescriptor descriptor;

    private DataNodeDescriptor nodeDescriptor;

    DataChannelChildrenHandler(XMLDataChannelDescriptorLoader xmlDataChannelDescriptorLoader, DataChannelHandler parentHandler) {
        super(parentHandler);
        this.xmlDataChannelDescriptorLoader = xmlDataChannelDescriptorLoader;
        this.descriptor = parentHandler.descriptor;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) {
        switch (localName) {
            case PROPERTY_TAG:
                addProperty(attributes);
                return true;

            case OLD_MAP_TAG:
                addMap(attributes);
                return true;

            case NODE_TAG:
                addNode(attributes);
                return true;

            case DOMAIN_TAG:
                return true;
        }

        return false;
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName,
                                                   String name, Attributes attributes) {
        if (NODE_TAG.equals(localName)) {
            nodeDescriptor = new DataNodeDescriptor();
            return new DataNodeChildrenHandler(xmlDataChannelDescriptorLoader, this, nodeDescriptor);
        }

        if (DATA_MAP_TAG.equals(localName)) {
            return new DataMapHandler(loaderContext);
        }

        return super.createChildTagHandler(namespaceURI, localName, name, attributes);
    }

    private void addProperty(Attributes attributes) {
        String key = attributes.getValue("name");
        String value = attributes.getValue("value");
        if (key != null && value != null) {
            descriptor.getProperties().put(key, value);
        }
    }

    private void addMap(Attributes attributes) {
        String dataMapName = attributes.getValue("name");
        Resource baseResource = descriptor.getConfigurationSource();

        String dataMapLocation = xmlDataChannelDescriptorLoader.nameMapper.configurationLocation(DataMap.class, dataMapName);

        Resource dataMapResource = baseResource.getRelativeResource(dataMapLocation);

        logger.info("Loading XML DataMap resource from " + dataMapResource.getURL());

        DataMap dataMap = xmlDataChannelDescriptorLoader.dataMapLoader.load(dataMapResource);
        dataMap.setName(dataMapName);
        dataMap.setLocation(dataMapLocation);
        dataMap.setDataChannelDescriptor(descriptor);

        descriptor.getDataMaps().add(dataMap);
    }

    private void addNode(Attributes attributes) {
        String nodeName = attributes.getValue("name");
        if (nodeName == null) {
            throw new ConfigurationException("Error: <node> without 'name'.");
        }

        nodeDescriptor.setConfigurationSource(descriptor.getConfigurationSource());
        nodeDescriptor.setName(nodeName);
        nodeDescriptor.setAdapterType(attributes.getValue("adapter"));
        nodeDescriptor.setParameters(attributes.getValue("parameters"));
        nodeDescriptor.setDataSourceFactoryType(attributes.getValue("factory"));
        nodeDescriptor.setSchemaUpdateStrategyType(attributes.getValue("schema-update-strategy"));
        nodeDescriptor.setDataChannelDescriptor(descriptor);

        descriptor.getNodeDescriptors().add(nodeDescriptor);
    }
}
