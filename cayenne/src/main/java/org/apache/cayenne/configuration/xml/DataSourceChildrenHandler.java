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
import org.apache.cayenne.configuration.DataSourceDescriptor;
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
    private DataSourceDescriptor dataSourceDescriptor;

    DataSourceChildrenHandler(XMLDataChannelDescriptorLoader xmlDataChannelDescriptorLoader,
                              DataNodeChildrenHandler parentHandler,
                              DataSourceDescriptor dataSourceDescriptor) {
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
        String password = attributes.getValue("password");
        String username = attributes.getValue("userName");
        dataSourceDescriptor.setUserName(username);
        dataSourceDescriptor.setPassword(password);
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
    }
}
