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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.di.AdhocObjectFactory;
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
    static final String DATA_SOURCE_TAG = "data-source";

    /**
     * @deprecated the caller should use password resolving strategy instead of resolving
     *             the password on the spot. For one thing this can be used in the Modeler
     *             and no password may be available.
     */
    @Deprecated
    private static String passwordFromURL(URL url) {
        InputStream inputStream = null;
        String password = null;

        try {
            inputStream = url.openStream();
            password = passwordFromInputStream(inputStream);
        }
        catch (IOException exception) {
            // Log the error while trying to open the stream. A null
            // password will be returned as a result.
            logger.warn(exception);
        }

        return password;
    }

    /**
     * @deprecated the caller should use password resolving strategy instead of resolving
     *             the password on the spot. For one thing this can be used in the Modeler
     *             and no password may be available.
     */
    @Deprecated
    private static String passwordFromInputStream(InputStream inputStream) {
        BufferedReader bufferedReader = null;
        String password = null;

        try {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            password = bufferedReader.readLine();
        }
        catch (IOException exception) {
            logger.warn(exception);
        }
        finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            }
            catch (Exception exception) {
            }

            try {
                inputStream.close();
            }
            catch (IOException exception) {
            }
        }

        return password;
    }

    @Inject
    protected DataMapLoader dataMapLoader;

    @Inject
    protected ConfigurationNameMapper nameMapper;
    
    @Inject
    protected AdhocObjectFactory objectFactory;

    public ConfigurationTree<DataChannelDescriptor> load(Resource configurationResource)
            throws ConfigurationException {

        if (configurationResource == null) {
            throw new NullPointerException("Null configurationResource");
        }

        URL configurationURL = configurationResource.getURL();

        logger.info("Loading XML configuration resource from " + configurationURL);

        DataChannelDescriptor descriptor = new DataChannelDescriptor();
        descriptor.setConfigurationSource(configurationResource);
        descriptor.setName(nameMapper.configurationNodeName(
                DataChannelDescriptor.class,
                configurationResource));

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

        // TODO: andrus 03/10/2010 - actually provide load failures here...
        return new ConfigurationTree<DataChannelDescriptor>(descriptor, null);
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
                Resource baseResource = descriptor.getConfigurationSource();

                String dataMapLocation = nameMapper.configurationLocation(
                        DataMap.class,
                        dataMapName);

                Resource dataMapResource = baseResource
                        .getRelativeResource(dataMapLocation);

                logger.info("Loading XML DataMap resource from " + dataMapResource.getURL());

                DataMap dataMap = dataMapLoader.load(dataMapResource);
                dataMap.setName(dataMapName);
                dataMap.setLocation(dataMapLocation);
                dataMap.setConfigurationSource(dataMapResource);
                dataMap.setDataChannelDescriptor(descriptor);

                descriptor.getDataMaps().add(dataMap);
            }
            else if (localName.equals(NODE_TAG)) {

                String nodeName = attributes.getValue("", "name");
                if (nodeName == null) {
                    throw new ConfigurationException("Error: <node> without 'name'.");
                }

                DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor();
                nodeDescriptor
                        .setConfigurationSource(descriptor.getConfigurationSource());
                descriptor.getNodeDescriptors().add(nodeDescriptor);

                nodeDescriptor.setName(nodeName);
                nodeDescriptor.setAdapterType(attributes.getValue("", "adapter"));

                String parameters = attributes.getValue("", "parameters");
                nodeDescriptor.setParameters(parameters);

                String dataSourceFactory = attributes.getValue("", "factory");
                nodeDescriptor.setDataSourceFactoryType(dataSourceFactory);
                nodeDescriptor.setSchemaUpdateStrategyType(attributes.getValue(
                        "",
                        "schema-update-strategy"));
                nodeDescriptor.setDataChannelDescriptor(descriptor);

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
            else if (localName.equals(DATA_SOURCE_TAG)) {

                DataSourceInfo dataSourceDescriptor = new DataSourceInfo();
                nodeDescriptor.setDataSourceDescriptor(dataSourceDescriptor);
                return new DataSourceChildrenHandler(parser, this, dataSourceDescriptor);
            }

            return super.createChildTagHandler(namespaceURI, localName, name, attributes);
        }
    }

    class DataSourceChildrenHandler extends SAXNestedTagHandler {

        private DataSourceInfo dataSourceDescriptor;

        DataSourceChildrenHandler(XMLReader parser,
                DataNodeChildrenHandler parentHandler, DataSourceInfo dataSourceDescriptor) {
            super(parser, parentHandler);
            this.dataSourceDescriptor = dataSourceDescriptor;
        }

        @Override
        protected ContentHandler createChildTagHandler(
                String namespaceURI,
                String localName,
                String name,
                Attributes attributes) {

            if (localName.equals("driver")) {
                String className = attributes.getValue("", "value");
                dataSourceDescriptor.setJdbcDriver(className);
            }
            else if (localName.equals("login")) {

                logger.info("loading user name and password.");

                String encoderClass = attributes.getValue("encoderClass");

                String encoderKey = attributes.getValue("encoderKey");
                if (encoderKey == null) {
                    encoderKey = attributes.getValue("encoderSalt");
                }

                String password = attributes.getValue("password");
                String passwordLocation = attributes.getValue("passwordLocation");
                String passwordSource = attributes.getValue("passwordSource");
                if (passwordSource == null) {
                    passwordSource = DataSourceInfo.PASSWORD_LOCATION_MODEL;
                }

                String username = attributes.getValue("userName");

                dataSourceDescriptor.setPasswordEncoderClass(encoderClass);
                dataSourceDescriptor.setPasswordEncoderKey(encoderKey);
                dataSourceDescriptor.setPasswordLocation(passwordLocation);
                dataSourceDescriptor.setPasswordSource(passwordSource);
                dataSourceDescriptor.setUserName(username);

                // Replace {} in passwordSource with encoderSalt -- useful for EXECUTABLE
                // & URL options
                if (encoderKey != null) {
                    passwordSource = passwordSource.replaceAll("\\{\\}", encoderKey);
                }

                String encoderType = dataSourceDescriptor.getPasswordEncoderClass();
                PasswordEncoding passwordEncoder = null;
                if (encoderType != null) {
                    passwordEncoder = objectFactory.newInstance(PasswordEncoding.class, encoderType);
                }

                if (passwordLocation != null) {
                    if (passwordLocation
                            .equals(DataSourceInfo.PASSWORD_LOCATION_CLASSPATH)) {

                        ClassLoader classLoader = Thread
                                .currentThread()
                                .getContextClassLoader();
                        URL url = classLoader.getResource(username);
                        if (url != null) {
                            password = passwordFromURL(url);
                        }
                        else {
                            logger.error("Could not find resource in CLASSPATH: "
                                    + passwordSource);
                        }
                    }
                    else if (passwordLocation
                            .equals(DataSourceInfo.PASSWORD_LOCATION_URL)) {
                        try {
                            password = passwordFromURL(new URL(passwordSource));
                        }
                        catch (MalformedURLException exception) {
                            logger.warn(exception);
                        }
                    }
                    else if (passwordLocation
                            .equals(DataSourceInfo.PASSWORD_LOCATION_EXECUTABLE)) {
                        if (passwordSource != null) {
                            try {
                                Process process = Runtime.getRuntime().exec(
                                        passwordSource);
                                password = passwordFromInputStream(process
                                        .getInputStream());
                                process.waitFor();
                            }
                            catch (IOException exception) {
                                logger.warn(exception);
                            }
                            catch (InterruptedException exception) {
                                logger.warn(exception);
                            }
                        }
                    }
                }

                if (password != null && passwordEncoder != null) {
                    dataSourceDescriptor.setPassword(passwordEncoder.decodePassword(
                            password,
                            encoderKey));
                }
            }
            else if (localName.equals("url")) {
                dataSourceDescriptor.setDataSourceUrl(attributes.getValue("value"));
            }
            else if (localName.equals("connectionPool")) {
                String min = attributes.getValue("min");
                if (min != null) {
                    try {
                        dataSourceDescriptor.setMinConnections(Integer.parseInt(min));
                    }
                    catch (NumberFormatException nfex) {
                        logger.info("Non-numeric 'min' attribute", nfex);
                        throw new ConfigurationException(
                                "Non-numeric 'min' attribute '%s'",
                                nfex,
                                min);
                    }
                }

                String max = attributes.getValue("max");
                if (max != null) {
                    try {
                        dataSourceDescriptor.setMaxConnections(Integer.parseInt(max));
                    }
                    catch (NumberFormatException nfex) {
                        logger.info("Non-numeric 'max' attribute", nfex);
                        throw new ConfigurationException(
                                "Non-numeric 'max' attribute '%s'",
                                nfex,
                                max);
                    }
                }
            }

            return super.createChildTagHandler(namespaceURI, localName, name, attributes);
        }
    }
}
