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
import java.util.Collection;

import javax.sql.DataSource;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.access.ConnectionLogger;
import org.apache.cayenne.access.QueryLogger;
import org.apache.cayenne.conf.PasswordEncoding;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.conn.PoolManager;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.ResourceLocator;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * A {@link DataSourceFactory} that loads JDBC connection information from an XML resource
 * associated with the DataNodeDescriptor, returning a DataSource with simple connection
 * pooling.
 * 
 * @since 3.1
 */
public class XMLPoolingDataSourceFactory implements DataSourceFactory {

    private static final Log logger = LogFactory
            .getLog(XMLPoolingDataSourceFactory.class);

    @Inject
    protected ResourceLocator resourceLocator;

    public DataSource getDataSource(DataNodeDescriptor nodeDescriptor) throws Exception {

        logger.info("loading driver information from '"
                + nodeDescriptor.getLocation()
                + "'.");

        Resource resource = nodeDescriptor.getConfigurationResource();

        if (resource == null) {
            logger.info("Null configuration resource for node descriptor with location '"
                    + nodeDescriptor.getLocation()
                    + "'");
            throw new ConfigurationException(
                    "Null configuration resource for node descriptor with location '%s'",
                    nodeDescriptor.getLocation());
        }

        DataSourceInfo dataSourceDescriptor = new DataSourceInfo();

        XMLReader parser = Util.createXmlReader();

        DriverHandler handler = new DriverHandler(dataSourceDescriptor, parser);
        parser.setContentHandler(handler);
        parser.setErrorHandler(handler);
        parser.parse(new InputSource(resource.getURL().openStream()));

        return createDataSource(dataSourceDescriptor);
    }

    /**
     * Returns an instance of {@link PoolManager}.
     */
    protected DataSource createDataSource(DataSourceInfo dataSourceDescriptor)
            throws Exception {

        ConnectionLogger logger = new ConnectionLogger();
        try {
            return new PoolManager(
                    dataSourceDescriptor.getJdbcDriver(),
                    dataSourceDescriptor.getDataSourceUrl(),
                    dataSourceDescriptor.getMinConnections(),
                    dataSourceDescriptor.getMaxConnections(),
                    dataSourceDescriptor.getUserName(),
                    dataSourceDescriptor.getPassword(),
                    logger);
        }
        catch (Exception e) {
            QueryLogger.logConnectFailure(e);
            throw e;
        }
    }

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

    private class DriverHandler extends SAXNestedTagHandler {

        private DataSourceInfo dataSourceDescriptor;

        DriverHandler(DataSourceInfo dataSourceDescriptor, XMLReader parser) {
            super(parser, null);
            this.dataSourceDescriptor = dataSourceDescriptor;
        }

        @Override
        protected ContentHandler createChildTagHandler(
                String namespaceURI,
                String localName,
                String name,
                Attributes attributes) {

            if (localName.equals("driver")) {
                String className = attributes.getValue("", "class");
                logger.info("loading driver " + className);
                dataSourceDescriptor.setJdbcDriver(className);
                return new DriverChildrenHandler(parser, this);
            }

            return super.createChildTagHandler(namespaceURI, localName, name, attributes);
        }
    }

    private class DriverChildrenHandler extends SAXNestedTagHandler {

        private DataSourceInfo dataSourceDescriptor;

        DriverChildrenHandler(XMLReader parser, DriverHandler parentHandler) {
            super(parser, parentHandler);
            this.dataSourceDescriptor = parentHandler.dataSourceDescriptor;
        }

        @Override
        protected ContentHandler createChildTagHandler(
                String namespaceURI,
                String localName,
                String name,
                Attributes attributes) {

            if (localName.equals("login")) {

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

                PasswordEncoding passwordEncoder = dataSourceDescriptor
                        .getPasswordEncoder();

                if (passwordLocation != null) {
                    if (passwordLocation
                            .equals(DataSourceInfo.PASSWORD_LOCATION_CLASSPATH)) {
                        Collection<Resource> urls = resourceLocator
                                .findResources(passwordLocation);
                        if (urls.size() > 0) {
                            password = passwordFromURL(urls.iterator().next().getURL());
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
