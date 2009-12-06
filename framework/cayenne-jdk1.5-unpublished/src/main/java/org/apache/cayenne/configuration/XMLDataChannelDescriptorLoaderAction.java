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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * A helper class to load the main Cayenne project descriptor as well as dependent map and
 * node descriptors.
 * 
 * @since 3.1
 */
class XMLDataChannelDescriptorLoaderAction {

    static final String DOMAIN_TAG = "domain";
    static final String MAP_TAG = "map";
    static final String NODE_TAG = "node";
    static final String PROPERTY_TAG = "property";
    static final String MAP_REF_TAG = "map-ref";

    private DataMapLoader mapLoader;
    private Log logger;

    XMLDataChannelDescriptorLoaderAction(DataMapLoader mapLoader, Log logger) {
        this.mapLoader = mapLoader;
        this.logger = logger;
    }

    DataChannelDescriptor load(Resource resource) {

        URL configurationURL = resource.getURL();

        DataChannelDescriptor descriptor = new DataChannelDescriptor();
        descriptor.setConfigurationResource(resource);

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
            throw new CayenneRuntimeException(
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
                Attributes atts) {

            if (localName.equals(DOMAIN_TAG)) {
                return new DataChannelChildrenHandler(parser, this);
            }

            logger.info(unexpectedTagMessage(localName, DOMAIN_TAG));
            return super.createChildTagHandler(namespaceURI, localName, name, atts);
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

                DataMap dataMap = mapLoader
                        .load(descriptor, dataMapLocation, dataMapName);
                descriptor.getDataMaps().add(dataMap);
            }
            else if (localName.equals(NODE_TAG)) {

                String nodeName = attributes.getValue("", "name");
                if (nodeName == null) {
                    throw new CayenneRuntimeException("Error: <node> without 'name'.");
                }

                DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor();
                descriptor.getDataNodeDescriptors().add(nodeDescriptor);

                nodeDescriptor.setName(nodeName);
                nodeDescriptor.setAdapterType(attributes.getValue("", "adapter"));

                // TODO: andrus, 11.29.2009 : should we rename that to "location"??
                String location = attributes.getValue("", "datasource");
                nodeDescriptor.setLocation(location);

                nodeDescriptor.setDataSourceFactoryType(attributes
                        .getValue("", "factory"));
                nodeDescriptor.setSchemaUpdateStrategyType(attributes.getValue(
                        "",
                        "schema-update-strategy"));

                // this may be bogus for nodes other than driver nodes, but here we can't
                // tell for sure
                if (location != null) {
                    nodeDescriptor.setConfigurationResource(descriptor
                            .getConfigurationResource()
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
