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

package org.apache.cayenne.configuration.xml;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.PasswordEncoding;
import org.apache.cayenne.conn.DataSourceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;

/**
 * @since 4.1
 */
class DataSourceChildrenHandler extends NamespaceAwareNestedTagHandler {

    private static final Logger logger = LoggerFactory.getLogger(XMLDataChannelDescriptorLoader.class);

    static final String DRIVER_TAG = "driver";
    static final String LOGIN_TAG = "login";
    static final String URL_TAG = "url";
    static final String CONNECTION_POOL_TAG = "connectionPool";


    private XMLDataChannelDescriptorLoader xmlDataChannelDescriptorLoader;
    private DataSourceInfo dataSourceDescriptor;

    DataSourceChildrenHandler(XMLDataChannelDescriptorLoader xmlDataChannelDescriptorLoader,
                              DataNodeChildrenHandler parentHandler,
                              DataSourceInfo dataSourceDescriptor) {
        super(parentHandler);
        this.xmlDataChannelDescriptorLoader = xmlDataChannelDescriptorLoader;
        this.dataSourceDescriptor = dataSourceDescriptor;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) {
        switch (localName) {
            case DRIVER_TAG:
                dataSourceDescriptor.setJdbcDriver(attributes.getValue("value"));
                return true;

            case LOGIN_TAG:
                configureCredentials(attributes);
                return true;

            case URL_TAG:
                dataSourceDescriptor.setDataSourceUrl(attributes.getValue("value"));
                return true;

            case CONNECTION_POOL_TAG:
                configureConnectionPool(attributes);
                return true;
        }

        return false;
    }

    void configureCredentials(Attributes attributes) {
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

        // Replace {} in passwordSource with encoderSalt -- useful for EXECUTABLE & URL options
        if (encoderKey != null) {
            passwordSource = passwordSource.replaceAll("\\{}", encoderKey);
        }

        String encoderType = dataSourceDescriptor.getPasswordEncoderClass();
        PasswordEncoding passwordEncoder = null;
        if (encoderType != null) {
            passwordEncoder = xmlDataChannelDescriptorLoader.objectFactory.newInstance(PasswordEncoding.class, encoderType);
        }

        if (passwordLocation != null) {
            switch (passwordLocation) {
                case DataSourceInfo.PASSWORD_LOCATION_CLASSPATH:

                    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                    URL url = classLoader.getResource(username);
                    if (url != null) {
                        password = XMLDataChannelDescriptorLoader.passwordFromURL(url);
                    } else {
                        logger.error("Could not find resource in CLASSPATH: " + passwordSource);
                    }
                    break;
                case DataSourceInfo.PASSWORD_LOCATION_URL:
                    try {
                        password = XMLDataChannelDescriptorLoader.passwordFromURL(new URL(passwordSource));
                    } catch (MalformedURLException exception) {
                        logger.warn(exception.getMessage(), exception);
                    }
                    break;
                case DataSourceInfo.PASSWORD_LOCATION_EXECUTABLE:
                    try {
                        Process process = Runtime.getRuntime().exec(passwordSource);
                        password = XMLDataChannelDescriptorLoader.passwordFromInputStream(process.getInputStream());
                        process.waitFor();
                    } catch (IOException | InterruptedException exception) {
                        logger.warn(exception.getMessage(), exception);
                    }
                    break;
            }
        }

        if (password != null && passwordEncoder != null) {
            dataSourceDescriptor.setPassword(passwordEncoder.decodePassword(password, encoderKey));
        }
    }

    void configureConnectionPool(Attributes attributes) {
        String min = attributes.getValue("min");
        if (min != null) {
            try {
                dataSourceDescriptor.setMinConnections(Integer.parseInt(min));
            } catch (NumberFormatException nfex) {
                logger.info("Non-numeric 'min' attribute", nfex);
                throw new ConfigurationException("Non-numeric 'min' attribute '%s'", nfex, min);
            }
        }

        String max = attributes.getValue("max");
        if (max != null) {
            try {
                dataSourceDescriptor.setMaxConnections(Integer.parseInt(max));
            } catch (NumberFormatException nfex) {
                logger.info("Non-numeric 'max' attribute", nfex);
                throw new ConfigurationException("Non-numeric 'max' attribute '%s'", nfex, max);
            }
        }

        String maxQueueWaitTime = attributes.getValue("maxQueueWaitTime");
        if (maxQueueWaitTime != null) {
            try {
                dataSourceDescriptor.setMaxQueueWaitTime(Integer.parseInt(maxQueueWaitTime));
            } catch (NumberFormatException nfex) {
                logger.info("Non-numeric 'maxQueueWaitTime' attribute", nfex);
                throw new ConfigurationException("Non-numeric 'maxQueueWaitTime' attribute '%s'", nfex, max);
            }
        }

        String validationQuery = attributes.getValue("validationQuery");
        if (validationQuery != null) {
            dataSourceDescriptor.setValidationQuery(validationQuery);
        }
    }
}
