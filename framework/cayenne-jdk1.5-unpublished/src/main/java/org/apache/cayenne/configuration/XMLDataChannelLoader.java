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
import java.util.Collection;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.di.DIException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.ResourceLocator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @since 3.1
 */
public class XMLDataChannelLoader implements DataChannelLoader {

    private static Log logger = LogFactory.getLog(XMLDataChannelLoader.class);

    @Inject
    private ResourceLocator resourceLocator;

    protected String getResourceName(String runtimeName) {
        if (runtimeName == null) {
            throw new NullPointerException("Null rumtimeName");
        }

        return "cayenne-" + runtimeName + ".xml";
    }

    public DataChannel get(String runtimeName) throws CayenneRuntimeException {

        logger.debug("starting configuration loading: " + runtimeName);

        String resourceName = getResourceName(runtimeName);
        Collection<Resource> configurations = resourceLocator.findResources(resourceName);

        if (configurations.isEmpty()) {
            throw new CayenneRuntimeException(
                    "[%s] : Configuration file \"%s\" is not found.",
                    getClass().getName(),
                    resourceName);
        }

        Resource configurationResource = configurations.iterator().next();
        URL configurationURL = configurationResource.getURL();

        // no support for multiple configs yet, but this is not a hard error
        if (configurations.size() > 1) {
            logger.info("found "
                    + configurations.size()
                    + " Cayenne configurations, will use the first one: "
                    + configurationURL);
        }

        DataDomain channel;

        InputStream in = null;
        try {
            in = configurationURL.openStream();
            channel = new DomainLoaderAction().loadDomain(in);
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
                logger.info("failure closing input stream for "
                        + configurationURL
                        + ", ignoring", ioex);
            }
        }

        channel.setName(runtimeName);

        logger.debug("finsihed configuration loading: " + runtimeName);
        return channel;
    }
}
