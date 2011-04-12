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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.PasswordEncoding;
import org.apache.cayenne.configuration.SAXNestedTagHandler;
import org.apache.cayenne.conn.DataSourceInfo;
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
class XMLDataSourceInfoLoader_V3_0_0_1 {

    private static Log logger = LogFactory.getLog(XMLDataSourceInfoLoader_V3_0_0_1.class);

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

    DataSourceInfo load(Resource configurationSource) {
        if (configurationSource == null) {
            throw new NullPointerException("Null configurationSource");
        }

        URL configurationURL = configurationSource.getURL();

        DataSourceInfo dataSourceInfo = new DataSourceInfo();

        InputStream in = null;

        try {
            in = configurationURL.openStream();
            XMLReader parser = Util.createXmlReader();

            DriverHandler rootHandler = new DriverHandler(parser, dataSourceInfo);
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

        return dataSourceInfo;
    }

    final class DriverHandler extends SAXNestedTagHandler {

        private DataSourceInfo dataSourceDescriptor;

        DriverHandler(XMLReader parser, DataSourceInfo dataSourceDescriptor) {
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
                dataSourceDescriptor.setJdbcDriver(className);

                return new DataSourceChildrenHandler(parser, this);
            }

            return super.createChildTagHandler(namespaceURI, localName, name, attributes);
        }
    }

    class DataSourceChildrenHandler extends SAXNestedTagHandler {

        private DataSourceInfo dataSourceDescriptor;

        DataSourceChildrenHandler(XMLReader parser, DriverHandler parentHandler) {
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
