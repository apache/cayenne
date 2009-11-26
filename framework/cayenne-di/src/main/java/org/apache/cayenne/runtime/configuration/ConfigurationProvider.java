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
package org.apache.cayenne.runtime.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;

import org.apache.cayenne.conf.ConfigLoader;
import org.apache.cayenne.conf.ConfigLoaderDelegate;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.conf.RuntimeLoadDelegate;
import org.apache.cayenne.di.DIException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.runtime.resource.Resource;
import org.apache.cayenne.runtime.resource.ResourceLocator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ConfigurationProvider implements Provider<Configuration> {

    private static Log logger = LogFactory.getLog(ConfigurationProvider.class);

    @Inject
    private ResourceLocator resourceLocator;

    protected String getConfigurationName() {
        return Configuration.DEFAULT_DOMAIN_FILE;
    }

    public Configuration get() throws DIException {

        logger.debug("starting configuration loading");

        String configurationName = getConfigurationName();
        Collection<Resource> configurations = resourceLocator
                .findResources(configurationName);

        if (configurations.isEmpty()) {
            throw new DIException(
                    "[%s] : Configuration file \"%s\" is not found.",
                    getClass().getName(),
                    configurationName);
        }

        Resource configurationResource = configurations.iterator().next();
        URL configurationURL = configurationResource.getURL();

        // no support for multiple configs yet, but this is not a hard error
        if (configurations.size() > 1) {
            logger.info(String.format(
                    "found %d Cayenne configurations, will use the first one: %s",
                    configurations.size(),
                    configurationURL));
        }

        Configuration configuration = new DIConfiguration();

        ConfigLoaderDelegate delegate = new RuntimeLoadDelegate(
                configuration,
                configuration.getLoadStatus());

        InputStream in = null;
        try {
            in = configurationURL.openStream();
            new ConfigLoader(delegate).loadDomains(in);
        }
        catch (Exception e) {
            throw new DIException(
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
                logger.info(String.format(
                        "failure closing input stream for %s, ignoring",
                        configurationURL), ioex);
            }
        }

        logger.debug("finsihed configuration loading");
        return configuration;
    }
}
