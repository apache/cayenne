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
package org.apache.cayenne.project.upgrade.v6;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.SAXNestedTagHandler;
import org.apache.cayenne.configuration.server.DBCPDataSourceFactory;
import org.apache.cayenne.configuration.server.JNDIDataSourceFactory;
import org.apache.cayenne.configuration.server.XMLPoolingDataSourceFactory;
import org.apache.cayenne.conn.DataSourceInfo;
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
 * A loader of Cayenne projects descriptor for version "3.0.0.1".
 */
class XMLDataChannelDescriptorLoader_V3_0_0_1 {

    private static Log logger = LogFactory
            .getLog(XMLDataChannelDescriptorLoader_V3_0_0_1.class);

    static final String DOMAINS_TAG = "domains";
    static final String DOMAIN_TAG = "domain";
    static final String MAP_TAG = "map";
    static final String NODE_TAG = "node";
    static final String PROPERTY_TAG = "property";
    static final String MAP_REF_TAG = "map-ref";

    private static final Map<String, String> dataSourceFactoryLegacyNameMapping;

    static {
        dataSourceFactoryLegacyNameMapping = new HashMap<String, String>();
        dataSourceFactoryLegacyNameMapping.put(
                "org.apache.cayenne.conf.DriverDataSourceFactory",
                XMLPoolingDataSourceFactory.class.getName());
        dataSourceFactoryLegacyNameMapping.put(
                "org.apache.cayenne.conf.JNDIDataSourceFactory",
                JNDIDataSourceFactory.class.getName());
        dataSourceFactoryLegacyNameMapping.put(
                "org.apache.cayenne.conf.DBCPDataSourceFactory",
                DBCPDataSourceFactory.class.getName());
    }

    // implementation is statically typed and is intentionally not DI-provided
    protected XMLDataMapLoader_V3_0_0_1 mapLoader;
    protected XMLDataSourceInfoLoader_V3_0_0_1 dataSourceInfoLoader;

    XMLDataChannelDescriptorLoader_V3_0_0_1() {
        mapLoader = new XMLDataMapLoader_V3_0_0_1();
        dataSourceInfoLoader = new XMLDataSourceInfoLoader_V3_0_0_1();
    }

    List<DataChannelDescriptor> load(Resource configurationSource)
            throws ConfigurationException {

        if (configurationSource == null) {
            throw new NullPointerException("Null configurationSource");
        }

        URL configurationURL = configurationSource.getURL();

        List<DataChannelDescriptor> domains = new ArrayList<DataChannelDescriptor>();
        InputStream in = null;

        try {
            in = configurationURL.openStream();
            XMLReader parser = Util.createXmlReader();

            DomainsHandler rootHandler = new DomainsHandler(
                    configurationSource,
                    domains,
                    parser);
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

        return domains;
    }

    /**
     * Make sure the domain name is only made up of Java-identifier-safe characters.
     */
    protected String scrubDomainName(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }

        StringBuilder buffer = new StringBuilder(name.length());

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i == 0 && !Character.isJavaIdentifierStart(c)) {
                buffer.append('_');
            }
            else if (i > 0 && !Character.isJavaIdentifierPart(c)) {
                buffer.append('_');
            }
            else {
                buffer.append(c);
            }
        }

        return buffer.toString();
    }

    /**
     * Converts the names of standard Cayenne-supplied DataSourceFactories from the legacy
     * names to the current names.
     */
    private String convertDataSourceFactory(String dataSourceFactory) {

        if (dataSourceFactory == null) {
            return null;
        }

        String converted = dataSourceFactoryLegacyNameMapping.get(dataSourceFactory);
        return converted != null ? converted : dataSourceFactory;
    }

    final class DomainsHandler extends SAXNestedTagHandler {

        private Collection<DataChannelDescriptor> domains;
        private Resource configurationSource;

        DomainsHandler(Resource configurationSource,
                Collection<DataChannelDescriptor> domains, XMLReader parser) {
            super(parser, null);
            this.domains = domains;
            this.configurationSource = configurationSource;
        }

        @Override
        protected ContentHandler createChildTagHandler(
                String namespaceURI,
                String localName,
                String qName,
                Attributes attributes) {

            if (localName.equals(DOMAINS_TAG)) {
                return new DomainsChildrenHandler(parser, this);
            }

            return super
                    .createChildTagHandler(namespaceURI, localName, qName, attributes);
        }
    }

    final class DomainsChildrenHandler extends SAXNestedTagHandler {

        private Collection<DataChannelDescriptor> domains;
        private Resource configurationSource;

        DomainsChildrenHandler(XMLReader parser, DomainsHandler parent) {
            super(parser, parent);
            this.domains = parent.domains;
            this.configurationSource = parent.configurationSource;
        }

        @Override
        protected ContentHandler createChildTagHandler(
                String namespaceURI,
                String localName,
                String name,
                Attributes attributes) {

            if (localName.equals(DOMAIN_TAG)) {

                String domainName = attributes.getValue("", "name");
                DataChannelDescriptor descriptor = new DataChannelDescriptor();
                descriptor.setName(scrubDomainName(domainName));
                descriptor.setConfigurationSource(configurationSource);

                domains.add(descriptor);
                return new DataChannelChildrenHandler(descriptor, parser, this);
            }

            logger.info(unexpectedTagMessage(localName, DOMAIN_TAG));
            return super.createChildTagHandler(namespaceURI, localName, name, attributes);
        }
    }

    final class DataChannelChildrenHandler extends SAXNestedTagHandler {

        private DataChannelDescriptor descriptor;

        DataChannelChildrenHandler(DataChannelDescriptor descriptor, XMLReader parser,
                DomainsChildrenHandler parentHandler) {
            super(parser, parentHandler);
            this.descriptor = descriptor;
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
                Resource baseResource = descriptor.getConfigurationSource();

                String dataMapLocation = attributes.getValue("", "location");
                Resource dataMapResource = baseResource
                        .getRelativeResource(dataMapLocation);

                DataMap dataMap = mapLoader.load(dataMapResource);
                dataMap.setName(dataMapName);
                dataMap.setLocation(dataMapLocation);
                dataMap.setConfigurationSource(dataMapResource);

                descriptor.getDataMaps().add(dataMap);
            }
            else if (localName.equals(NODE_TAG)) {

                String nodeName = attributes.getValue("", "name");
                if (nodeName == null) {
                    // TODO: assign dummy name?
                    throw new ConfigurationException("Error: <node> without 'name'.");
                }

                DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor();
                nodeDescriptor.setName(nodeName);

                String dataSourceFactory = attributes.getValue("", "factory");
                String dataSourceFactory6 = convertDataSourceFactory(dataSourceFactory);
                nodeDescriptor.setDataSourceFactoryType(dataSourceFactory6);

                // depending on the factory, "datasource" attribute is interpreted
                // differently
                String datasource = attributes.getValue("", "datasource");
                if (XMLPoolingDataSourceFactory.class
                        .getName()
                        .equals(dataSourceFactory6)) {
                    Resource baseResource = descriptor.getConfigurationSource();
                    Resource dataNodeResource = baseResource
                            .getRelativeResource(datasource);
                    nodeDescriptor.setConfigurationSource(dataNodeResource);

                    DataSourceInfo dataSourceInfo = dataSourceInfoLoader
                            .load(dataNodeResource);
                    nodeDescriptor.setDataSourceDescriptor(dataSourceInfo);
                }
                else {
                    nodeDescriptor.setParameters(datasource);
                }

                descriptor.getNodeDescriptors().add(nodeDescriptor);
                nodeDescriptor.setAdapterType(attributes.getValue("", "adapter"));
                nodeDescriptor.setSchemaUpdateStrategyType(attributes.getValue(
                        "",
                        "schema-update-strategy"));

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
