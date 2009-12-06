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

import java.util.Collection;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.ResourceLocator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @since 3.1
 */
public class XMLDataChannelDescriptorLoader implements DataChannelDescriptorLoader {

    private static Log logger = LogFactory.getLog(XMLDataChannelDescriptorLoader.class);

    @Inject
    protected ResourceLocator resourceLocator;

    @Inject
    protected DataMapLoader dataMapLoader;

    protected String getResourceName(String runtimeName) {
        if (runtimeName == null) {
            throw new NullPointerException("Null rumtimeName");
        }

        return "cayenne-" + runtimeName + ".xml";
    }

    public DataChannelDescriptor load(String runtimeName) throws CayenneRuntimeException {

        long t0 = System.currentTimeMillis();
        if (logger.isDebugEnabled()) {
            logger.debug("starting configuration loading: " + runtimeName);
        }

        String resourceName = getResourceName(runtimeName);
        Collection<Resource> configurations = resourceLocator.findResources(resourceName);

        if (configurations.isEmpty()) {
            throw new CayenneRuntimeException(
                    "[%s] : Configuration file \"%s\" is not found.",
                    getClass().getName(),
                    resourceName);
        }

        Resource configurationResource = configurations.iterator().next();

        // no support for multiple configs yet, but this is not a hard error
        if (configurations.size() > 1) {
            logger.info("found "
                    + configurations.size()
                    + " Cayenne configurations, will use the first one: "
                    + configurationResource.getURL());
        }

        DataChannelDescriptor descriptor = new XMLDataChannelDescriptorLoaderAction(
                dataMapLoader,
                logger).load(configurationResource);

        descriptor.setName(runtimeName);

        long t1 = System.currentTimeMillis();

        if (logger.isDebugEnabled()) {
            logger.debug("finished configuration loading: "
                    + runtimeName
                    + " in "
                    + (t1 - t0)
                    + " ms.");
        }
        return descriptor;
    }
}
