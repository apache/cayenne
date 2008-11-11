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
package org.apache.cayenne.conf;

import java.io.InputStream;

import org.apache.cayenne.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A Configuration implementation that creates a single virtual runtime project out of
 * multiple Cayenne configurations.
 * 
 * @since 3.0
 */
public class MultiProjectConfiguration extends Configuration {

    private static final Log logger = LogFactory.getLog(MultiProjectConfiguration.class);

    protected ResourceFinder resourceFinder;

    public MultiProjectConfiguration() {
        this.resourceFinder = new ClasspathResourceFinder();
    }

    @Override
    public void initialize() throws Exception {

        logger.debug("loading configuration");

        InputStream in = this.getDomainConfiguration();
        if (in == null) {
            StringBuilder msg = new StringBuilder();
            msg.append("[").append(this.getClass().getName()).append(
                    "] : Domain configuration file \"").append(
                    this.getDomainConfigurationName()).append("\" is not found.");

            throw new ConfigurationException(msg.toString());
        }

        ConfigLoaderDelegate delegate = this.getLoaderDelegate();
        if (delegate == null) {
            delegate = new RuntimeLoadDelegate(this, this.getLoadStatus());
        }

        ConfigLoader loader = new ConfigLoader(delegate);

        try {
            loader.loadDomains(in);
        }
        finally {
            this.setLoadStatus(delegate.getStatus());
            in.close();
        }

        logger.debug("initialize finished.");
    }

    @Override
    protected ResourceFinder getResourceFinder() {
        return resourceFinder;
    }
}
