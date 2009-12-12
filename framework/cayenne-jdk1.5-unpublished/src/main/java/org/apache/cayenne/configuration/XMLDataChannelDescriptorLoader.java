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
package org.apache.cayenne.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * @since 3.1
 */
public class XMLDataChannelDescriptorLoader implements DataChannelDescriptorLoader {

    private static Log logger = LogFactory.getLog(XMLDataChannelDescriptorLoader.class);

    static final String DOMAIN_TAG = "domain";
    static final String MAP_TAG = "map";
    static final String NODE_TAG = "node";
    static final String PROPERTY_TAG = "property";
    static final String MAP_REF_TAG = "map-ref";

    private static final Map<String, String> dataSourceFactoryNameMapping;

    static {
        dataSourceFactoryNameMapping = new HashMap<String, String>();
        dataSourceFactoryNameMapping.put(
                "org.apache.cayenne.conf.DriverDataSourceFactory",
                XMLPoolingDataSourceFactory.class.getName());
        dataSourceFactoryNameMapping.put(
                "org.apache.cayenne.conf.JNDIDataSourceFactory",
                JNDIDataSourceFactory.class.getName());
        dataSourceFactoryNameMapping.put(
                "org.apache.cayenne.conf.DBCPDataSourceFactory",
                DBCPDataSourceFactory.class.getName());
    }

    @Inject
    protected DataMapLoader dataMapLoader;

    public DataChannelDescriptor load(Resource configurationResource)
            throws ConfigurationException {

        if (configurationResource == null) {
            throw new NullPointerException("Null configurationResource");
        }

        URL configurationURL = configurationResource.getURL();

        DataChannelDescriptor descriptor = new DataChannelDescriptor();
        descriptor.setConfigurationSource(configurationResource);

        DataChannelHandler rootHandler;

        InputStream in = null;

        try {
            in = configurationURL.openStream();
            XMLReader parser = Util.createXmlReader();

            rootHandler = new DataChannelHandler(descriptor, parser);
            parser.setContentHandler(rootHandler);
            parser.setErrorHandler(rootHandler);
            parser.parse(new InputSource(in));
        }
        catch (Exception e) {
            throw new ConfigurationException(
                    "Error loading configuration from %s",
                    e,
                    configurationURL);
        }
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            }
            catch (IOException ioex) {
                logger.info("failure closing input stream for "
                        + configurationURL
                        + ", ignoring", ioex);
            }
        }

        return descriptor;
    }

    /**
     * Converts the names of standard Cayenne-supplied DataSourceFactories from the legacy
     * names to the current names.
     */
    private String convertDataSourceFactory(String dataSourceFactory) {

        if (dataSourceFactory == null) {
            return null;
        }

        String converted = dataSourceFactoryNameMapping.get(dataSourceFactory);
        return converted != null ? converted : dataSourceFactory;
    }

    final class DataChannelHandler extends SAXNestedTagHandler {

        private DataChannelDescriptor descriptor;

        DataChannelHandler(DataChannelDescriptor dataChannelDescriptor, XMLReader parser) {
            super(parser, null);
            this.descriptor = dataChannelDescriptor;
        }

        @Override
        protected ContentHandler createChildTagHandler(
                String namespaceURI,
                String localName,
                String name,
                Attributes attributes) {

            if (localName.equals(DOMAIN_TAG)) {
                String version = attributes.getValue("", "project-version");
                descriptor.setVersion(version);

                return new DataChannelChildrenHandler(parser, this);
            }

            logger.info(unexpectedTagMessage(localName, DOMAIN_TAG));
            return super.createChildTagHandler(namespaceURI, localName, name, attributes);
        }
    }

    final class DataChannelChildrenHandler extends SAXNestedTagHandler {

        private DataChannelDescriptor descriptor;

        DataChannelChildrenHandler(XMLReader parser, DataChannelHandler parentHandler) {
            super(parser, parentHandler);
            this.descriptor = parentHandler.descriptor;
        }

        @Override
        protected ContentHandler createChildTagHandler(
                String namespaceURI,
                String localName,
                String name,
                Attributes attributes) {

            if (localName.equals(PROPERTY_TAG)) {

                String key = attributes.getValue("", "name");
                String value = attributes.getValue("", "value");
                if (key != null && value != null) {
                    descriptor.getProperties().put(key, value);
                }
            }
            else if (localName.equals(MAP_TAG)) {

                String dataMapName = attributes.getValue("", "name");
                String dataMapLocation = attributes.getValue("", "location");

                Resource baseResource = descriptor.getConfigurationSource();

                Resource dataMapResource = baseResource
                        .getRelativeResource(dataMapLocation);

                DataMap dataMap = dataMapLoader.load(dataMapResource);
                dataMap.setName(dataMapName);
                dataMap.setLocation(dataMapLocation);
                dataMap.setConfigurationSource(dataMapResource);

                descriptor.getDataMaps().add(dataMap);
            }
            else if (localName.equals(NODE_TAG)) {

                String nodeName = attributes.getValue("", "name");
                if (nodeName == null) {
                    throw new ConfigurationException("Error: <node> without 'name'.");
                }

                DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor();
                descriptor.getDataNodeDescriptors().add(nodeDescriptor);

                nodeDescriptor.setName(nodeName);
                nodeDescriptor.setAdapterType(attributes.getValue("", "adapter"));

                String location = attributes.getValue("", "datasource");
                nodeDescriptor.setLocation(location);

                String dataSourceFactory = attributes.getValue("", "factory");
                nodeDescriptor
                        .setDataSourceFactoryType(convertDataSourceFactory(dataSourceFactory));
                nodeDescriptor.setSchemaUpdateStrategyType(attributes.getValue(
                        "",
                        "schema-update-strategy"));

                // this may be bogus for some nodes, such as JNDI, but here we can't
                // tell for sure
                if (location != null) {
                    nodeDescriptor.setConfigurationSource(descriptor
                            .getConfigurationSource()
                            .getRelativeResource(location));
                }

                return new DataNodeChildrenHandler(parser, this, nodeDescriptor);
            }

            return super.createChildTagHandler(namespaceURI, localName, name, attributes);
        }
    }

    final class DataNodeChildrenHandler extends SAXNestedTagHandler {

        private DataNodeDescriptor nodeDescriptor;

        DataNodeChildrenHandler(XMLReader parser, SAXNestedTagHandler parentHandler,
                DataNodeDescriptor nodeDescriptor) {
            super(parser, parentHandler);
            this.nodeDescriptor = nodeDescriptor;
        }

        @Override
        protected ContentHandler createChildTagHandler(
                String namespaceURI,
                String localName,
                String name,
                Attributes attributes) {

            if (localName.equals(MAP_REF_TAG)) {

                String mapName = attributes.getValue("", "name");
                nodeDescriptor.getDataMapNames().add(mapName);
            }
            return super.createChildTagHandler(namespaceURI, localName, name, attributes);
        }
    }
}
