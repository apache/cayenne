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
import org.apache.cayenne.util.ResourceLocator;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Subclass of Configuration that uses the System CLASSPATH to locate resources.
 * 
 * @author Andrus Adamchik
 */
public class DefaultConfiguration extends Configuration {

    private static Log logger = LogFactory.getLog(DefaultConfiguration.class);

    /**
     * the default ResourceLocator used for CLASSPATH loading
     */
    private ResourceLocator locator;

    /**
     * Default constructor. Simply calls
     * {@link DefaultConfiguration#DefaultConfiguration(String)} with
     * {@link Configuration#DEFAULT_DOMAIN_FILE} as argument.
     * 
     * @see Configuration#Configuration()
     */
    public DefaultConfiguration() {
        this(Configuration.DEFAULT_DOMAIN_FILE);
    }

    /**
     * Constructor with a named domain configuration resource. Simply calls
     * {@link Configuration#Configuration(String)}.
     * 
     * @throws ConfigurationException when <code>domainConfigurationName</code> is
     *             <code>null</code>.
     * @see Configuration#Configuration(String)
     */
    public DefaultConfiguration(String domainConfigurationName) {
        super(domainConfigurationName);

        if (domainConfigurationName == null) {
            throw new ConfigurationException("cannot use null as domain file name.");
        }

        logger.debug("using domain file name: " + domainConfigurationName);

        // configure CLASSPATH-only locator
        ResourceLocator locator = new ResourceLocator();
        locator.setSkipAbsolutePath(true);
        locator.setSkipClasspath(false);
        locator.setSkipCurrentDirectory(true);
        locator.setSkipHomeDirectory(true);

        // add the current Configuration subclass' package as additional path.
        if (!(this.getClass().equals(DefaultConfiguration.class))) {
            locator.addClassPath(Util.getPackagePath(this.getClass().getName()));
        }

        setResourceLocator(locator);
    }

    /**
     * Creates DefaultConfiguration with specified cayenne project file name and
     * ResourceLocator.
     * 
     * @since 1.2
     */
    public DefaultConfiguration(String domainConfigurationName, ResourceLocator locator) {
        super(domainConfigurationName);
        setResourceLocator(locator);
    }

    /**
     * Adds a custom path for class path lookups. Format should be "my/package/name"
     * <i>without</i> leading "/". This allows for easy customization of custom search
     * paths after Constructor invocation:
     * 
     * <pre>
     * conf = new DefaultConfiguration();
     * conf.addClassPath(&quot;my/package/name&quot;);
     * Configuration.initializeSharedConfiguration(conf);
     * </pre>
     */
    public void addClassPath(String customPath) {
        this.getResourceLocator().addClassPath(customPath);
    }

    /**
     * Adds the given String as a custom path for resource lookups. The path can be
     * relative or absolute and is <i>not </i> checked for existence. Depending on the
     * underlying ResourceLocator configuration this can for instance be a path in the web
     * application context or a filesystem path.
     * 
     * @throws IllegalArgumentException if <code>path</code> is <code>null</code>.
     * @since 1.2 moved from subclass - FileConfiguration.
     */
    public void addResourcePath(String path) {
        this.getResourceLocator().addFilesystemPath(path);
    }

    /**
     * Default implementation of {@link Configuration#canInitialize}. Creates a
     * ResourceLocator suitable for loading from the CLASSPATH, unless it has already been
     * set in a subclass. Always returns <code>true</code>.
     */
    @Override
    public boolean canInitialize() {
        logger.debug("canInitialize started.");
        // allow to proceed
        return true;
    }

    /**
     * Initializes all Cayenne resources. Loads all configured domains and their data
     * maps, initializes all domain Nodes and their DataSources.
     */
    @Override
    public void initialize() throws Exception {
        logger.debug("initialize starting.");

        InputStream in = this.getDomainConfiguration();
        if (in == null) {
            StringBuffer msg = new StringBuffer();
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

        // log successful initialization
        logger.debug("initialize finished.");
    }

    /**
     * Default implementation of {@link Configuration#didInitialize}. Currently does
     * nothing except logging.
     */
    @Override
    public void didInitialize() {
        // empty default implementation
        logger.debug("didInitialize finished.");
    }

    /**
     * Returns the default ResourceLocator configured for CLASSPATH lookups.
     */
    @Override
    protected ResourceLocator getResourceLocator() {
        return this.locator;
    }

    /**
     * Sets the specified {@link ResourceLocator}. Currently called from
     * {@link #initialize}.
     */
    protected void setResourceLocator(ResourceLocator locator) {
        this.locator = locator;
    }

    /**
     * Returns the domain configuration as a stream or <code>null</code> if it cannot be
     * found. Uses the configured {@link ResourceLocator} to find the file.
     */
    @Override
    protected InputStream getDomainConfiguration() {
        return locator.findResourceStream(this.getDomainConfigurationName());
    }

    /**
     * Returns the {@link org.apache.cayenne.map.DataMap} configuration from a
     * specified location or <code>null</code> if it cannot be found. Uses the
     * configured {@link ResourceLocator} to find the file.
     */
    @Override
    protected InputStream getMapConfiguration(String location) {
        return locator.findResourceStream(location);
    }

    @Override
    protected InputStream getViewConfiguration(String location) {
        return locator.findResourceStream(location);
    }

    /**
     * @see Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf
                .append('[')
                .append(this.getClass().getName())
                .append(": classloader=")
                .append(locator.getClassLoader())
                .append(']');
        return buf.toString();
    }

}
