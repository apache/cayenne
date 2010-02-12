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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategy;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.util.ResourceLocator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is an entry point to Cayenne. It loads all configuration files and
 * instantiates main Cayenne objects. Used as a singleton via the
 * {@link #getSharedConfiguration}method.
 * <p>
 * To use a custom subclass of Configuration, Java applications must call
 * {@link #initializeSharedConfiguration}with the subclass as argument. This will create
 * and initialize a Configuration singleton instance of the specified class. By default
 * {@link DefaultConfiguration}is instantiated.
 * </p>
 * 
 */
public abstract class Configuration {

    private static Log logObj = LogFactory.getLog(Configuration.class);

    public static final String DEFAULT_DOMAIN_FILE = "cayenne.xml";
    public static final Class<DefaultConfiguration> DEFAULT_CONFIGURATION_CLASS = DefaultConfiguration.class;

    protected static Configuration sharedConfiguration;

    /**
     * Lookup map that stores DataDomains with names as keys.
     */
    protected SortedMap<String, DataDomain> dataDomains = new TreeMap<String, DataDomain>();
    protected DataSourceFactory overrideFactory;
    protected SchemaUpdateStrategy overrideStrategy;
    protected ConfigStatus loadStatus = new ConfigStatus();
    protected String domainConfigurationName = DEFAULT_DOMAIN_FILE;
    protected boolean ignoringLoadFailures;
    protected ConfigLoaderDelegate loaderDelegate;
    protected ConfigSaverDelegate saverDelegate;
    protected ConfigurationShutdownHook configurationShutdownHook;
    protected Map<String, String> dataViewLocations = new HashMap<String, String>();
    protected String projectVersion;

    /**
     * @since 1.2
     */
    protected EventManager eventManager;

    /**
     * Use this method as an entry point to all Cayenne access objects.
     * <p>
     * Note that if you want to provide a custom Configuration, make sure you call one of
     * the {@link #initializeSharedConfiguration}methods before your application code has
     * a chance to call this method.
     */
    public synchronized static Configuration getSharedConfiguration() {
        if (Configuration.sharedConfiguration == null) {
            Configuration.initializeSharedConfiguration();
        }

        return Configuration.sharedConfiguration;
    }

    /**
     * Returns EventManager used by this configuration.
     * 
     * @since 1.2
     */
    public EventManager getEventManager() {
        return eventManager;
    }

    /**
     * Sets EventManager used by this configuration.
     * 
     * @since 1.2
     */
    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    /**
     * Creates and initializes shared Configuration object. By default
     * {@link DefaultConfiguration}will be instantiated and assigned to a singleton
     * instance of Configuration.
     */
    public static void initializeSharedConfiguration() {
        Configuration.initializeSharedConfiguration(DEFAULT_CONFIGURATION_CLASS);
    }

    /**
     * Creates and initializes a shared Configuration object of a custom Configuration
     * subclass.
     */
    public static void initializeSharedConfiguration(
            Class<? extends Configuration> configurationClass) {
        Configuration conf = null;

        try {
            conf = configurationClass.newInstance();
        }
        catch (Exception ex) {
            logObj.error("Error creating shared Configuration: ", ex);
            throw new ConfigurationException("Error creating shared Configuration."
                    + ex.getMessage(), ex);
        }

        Configuration.initializeSharedConfiguration(conf);
    }

    /**
     * Sets the shared Configuration object to a new Configuration object. First calls
     * {@link #canInitialize}and - if permitted -{@link #initialize}followed by
     * {@link #didInitialize}.
     */
    public static void initializeSharedConfiguration(Configuration conf) {
        // check to see whether we can proceed
        if (!conf.canInitialize()) {
            throw new ConfigurationException("Configuration of class "
                    + conf.getClass().getName()
                    + " refused to be initialized.");
        }

        try {
            // initialize configuration
            conf.initialize();

            // call post-initialization hook
            conf.didInitialize();

            // set the initialized Configuration only after success
            Configuration.sharedConfiguration = conf;
        }
        catch (Exception ex) {
            throw new ConfigurationException(
                    "Error during Configuration initialization. " + ex.getMessage(),
                    ex);
        }
    }

    /**
     * Default constructor for new Configuration instances. Simply calls
     * {@link Configuration#Configuration(String)}.
     * 
     * @see Configuration#Configuration(String)
     */
    protected Configuration() {
        this(DEFAULT_DOMAIN_FILE);
    }

    /**
     * Default constructor for new Configuration instances using the given resource name
     * as the main domain file.
     */
    protected Configuration(String domainConfigurationName) {

        // set domain configuration name
        this.setDomainConfigurationName(domainConfigurationName);

        this.eventManager = new EventManager();
    }

    /**
     * Indicates whether {@link #initialize}can be called. Returning <code>false</code>
     * allows new instances to delay or refuse the initialization process. This
     * impementation returns true unconditionally.
     * 
     * @deprecated since 3.0 - this method is redundant, as subclasses can prevent
     *             initialization by overriding {@link #initialize()} and throwing an
     *             exception.
     */
    public boolean canInitialize() {
        return true;
    }

    /**
     * Initializes the new instance.
     * 
     * @throws Exception
     */
    public abstract void initialize() throws Exception;

    /**
     * Called after successful completion of {@link #initialize}. This implementation is
     * a noop.
     * 
     * @deprecated since 3.0 subclasses are recommended to override {@link #initialize()}.
     */
    public void didInitialize() {
        // noop
    }

    /**
     * Returns the resource locator used for finding and loading resources.
     * 
     * @deprecated since 3.0 use/override {@link #getResourceFinder()}.
     */
    protected ResourceLocator getResourceLocator() {
        ResourceFinder finder = getResourceFinder();
        if (finder == null) {
            return null;
        }

        if (finder instanceof ResourceLocator) {
            return (ResourceLocator) finder;
        }

        throw new IllegalStateException("ResourceFinder is not a ResourceLocator: "
                + finder);
    }

    /**
     * Returns {@link ResourceFinder} associated with this Configuration that is to be
     * used for config files lookup.
     * 
     * @since 3.0
     */
    protected abstract ResourceFinder getResourceFinder();

    /**
     * Returns a DataDomain as a stream or <code>null</code> if it cannot be found.
     * 
     * @deprecated since 3.0 This method is specific to subclass, so it should not be in
     *             the superclass.
     */
    protected InputStream getDomainConfiguration() {
        URL url = getResourceFinder().getResource(getDomainConfigurationName());
        try {
            return url != null ? url.openStream() : null;
        }
        catch (IOException e) {
            throw new ConfigurationException("Can't open config file URL: " + url, e);
        }
    }

    /**
     * Returns a DataMap with the given name or <code>null</code> if it cannot be found.
     */
    protected InputStream getMapConfiguration(String name) {
        URL url = getResourceFinder().getResource(name);
        try {
            return url != null ? url.openStream() : null;
        }
        catch (IOException e) {
            throw new ConfigurationException("Can't open config file URL: " + url, e);
        }
    }

    /**
     * See 'https://svn.apache.org/repos/asf/cayenne/dataviews/trunk' for DataViews code,
     * which is not a part of Cayenne since 3.0.
     * 
     * @deprecated since 3.0 as Cayenne no longer cares to read view config files.
     */
    protected InputStream getViewConfiguration(String location) {
        URL url = getResourceFinder().getResource(location);
        try {
            return url != null ? url.openStream() : null;
        }
        catch (IOException e) {
            throw new ConfigurationException("Can't open config file URL: " + url, e);
        }
    }

    /**
     * Returns the name of the main domain configuration resource. Defaults to
     * {@link Configuration#DEFAULT_DOMAIN_FILE}.
     */
    public String getDomainConfigurationName() {
        return domainConfigurationName;
    }

    /**
     * Sets the name of the main domain configuration resource.
     * 
     * @param domainConfigurationName the name of the resource that contains this
     *            Configuration's domain(s).
     */
    protected void setDomainConfigurationName(String domainConfigurationName) {
        this.domainConfigurationName = domainConfigurationName;
    }

    /**
     * @since 1.1
     */
    public String getProjectVersion() {
        return projectVersion;
    }

    /**
     * @since 1.1
     */
    public void setProjectVersion(String projectVersion) {
        this.projectVersion = projectVersion;
    }

    /**
     * Returns a DataSourceFactory that should override a given factory specified by
     * caller. Returns null if the user factory should not be overriden.
     * 
     * @since 3.0
     */
    public DataSourceFactory getDataSourceFactory(String userFactoryName) {
        // call the old implementation for backwards compatibility
        return getDataSourceFactory();
    }

    
    public SchemaUpdateStrategy getSchemaUpdateStrategy() {
        return overrideStrategy;
    }

    
    public void setSchemaUpdateStrategy(SchemaUpdateStrategy overrideStrategy) {
        this.overrideStrategy = overrideStrategy;
    }

    
    /**
     * Returns an internal DataSourceFactory that will override any settings configured in
     * XML. Subclasses may override this method to provide a special factory for
     * DataSource creation that will take precedence over any factories configured in a
     * Cayenne project.
     * 
     * @deprecated since 3.0 this method is no longer called when configuration is loaded.
     *             Instead {@link #getDataSourceFactory(String)} is invoked, and this is
     *             the method that should be overriden.
     */
    public DataSourceFactory getDataSourceFactory() {
        return overrideFactory;
    }

    public void setDataSourceFactory(DataSourceFactory overrideFactory) {
        this.overrideFactory = overrideFactory;
    }

    /**
     * Adds new DataDomain to the list of registered domains. Injects EventManager used by
     * this configuration into the domain.
     */
    public void addDomain(DataDomain domain) {
        if (domain == null) {
            throw new NullPointerException("Attempt to work with a null DataDomain.");
        }

        if (domain.getName() == null) {
            throw new NullPointerException("Attempt to add DataDomain with no name.");
        }

        DataDomain old = dataDomains.put(domain.getName(), domain);
        if (old != null && old != domain) {
            dataDomains.put(domain.getName(), old);
            throw new IllegalArgumentException("Attempt to overwrite domain with name "
                    + domain.getName());
        }

        // inject EventManager
        domain.setEventManager(getEventManager());

        logObj.debug("added domain: " + domain.getName());
    }

    /**
     * Returns registered domain matching <code>name</code> or <code>null</code> if no
     * such domain is found.
     */
    public DataDomain getDomain(String name) {
        return dataDomains.get(name);
    }

    /**
     * Returns default domain of this configuration. If no domains are configured,
     * <code>null</code> is returned. If more than one domain exists in this
     * configuration, a CayenneRuntimeException is thrown, indicating that the domain name
     * must be explicitly specified. In such cases {@link #getDomain(String name)}must be
     * used instead.
     */
    public DataDomain getDomain() {
        int size = dataDomains.size();
        if (size == 0) {
            return null;
        }
        else if (size == 1) {
            return dataDomains.values().iterator().next();
        }
        else {
            throw new CayenneRuntimeException(
                    "More than one domain is configured; use 'getDomain(String name)' instead.");
        }
    }

    /**
     * Unregisters DataDomain matching <code>name<code> from
     * this Configuration object. Note that any domain database
     * connections remain open, and it is a responsibility of a
     * caller to clean it up.
     */
    public void removeDomain(String name) {
        DataDomain domain = dataDomains.remove(name);

        if (domain != null) {
            domain.setEventManager(null);
        }
    }

    /**
     * Returns an unmodifiable collection of registered DataDomains sorted by domain name.
     */
    public Collection<DataDomain> getDomains() {
        return Collections.unmodifiableCollection(dataDomains.values());
    }

    /**
     * Returns whether to ignore any failures during map loading or not.
     * 
     * @return boolean
     */
    public boolean isIgnoringLoadFailures() {
        return this.ignoringLoadFailures;
    }

    /**
     * Sets whether to ignore any failures during map loading or not.
     * 
     * @param ignoringLoadFailures <code>true</code> or <code>false</code>
     */
    protected void setIgnoringLoadFailures(boolean ignoringLoadFailures) {
        this.ignoringLoadFailures = ignoringLoadFailures;
    }

    /**
     * Returns the load status.
     * 
     * @return ConfigStatus
     */
    public ConfigStatus getLoadStatus() {
        return this.loadStatus;
    }

    /**
     * Sets the load status.
     */
    protected void setLoadStatus(ConfigStatus status) {
        this.loadStatus = status;
    }

    /**
     * Returns a delegate used for controlling the loading of configuration elements.
     */
    public ConfigLoaderDelegate getLoaderDelegate() {
        return loaderDelegate;
    }

    /**
     * @since 1.1
     */
    public void setLoaderDelegate(ConfigLoaderDelegate loaderDelegate) {
        this.loaderDelegate = loaderDelegate;
    }

    /**
     * @since 1.2
     */
    public ConfigSaverDelegate getSaverDelegate() {
        return saverDelegate;
    }

    /**
     * @since 1.2
     */
    public void setSaverDelegate(ConfigSaverDelegate saverDelegate) {
        this.saverDelegate = saverDelegate;
    }

    /**
     * Initializes configuration with the location of data views.
     * 
     * @since 1.1
     * @param dataViewLocations Map of DataView locations.
     */
    public void setDataViewLocations(Map<String, String> dataViewLocations) {
        if (dataViewLocations == null)
            this.dataViewLocations = new HashMap<String, String>();
        else
            this.dataViewLocations = dataViewLocations;
    }

    /**
     * See 'https://svn.apache.org/repos/asf/cayenne/dataviews/trunk' for DataViews code,
     * which is not a part of Cayenne since 3.0.
     * 
     * @since 1.1
     */
    public Map<String, String> getDataViewLocations() {
        return dataViewLocations;
    }

    /**
     * Shutdowns all owned domains. Invokes DataDomain.shutdown().
     */
    public void shutdown() {
        for (DataDomain domain : getDomains()) {
            domain.shutdown();
        }

        if (eventManager != null) {
            eventManager.shutdown();
        }
    }

    private class ConfigurationShutdownHook extends Thread {

        @Override
        public void run() {
            shutdown();
        }
    }

    public void installConfigurationShutdownHook() {
        uninstallConfigurationShutdownHook();

        if (configurationShutdownHook == null) {
            configurationShutdownHook = new ConfigurationShutdownHook();
        }

        Runtime.getRuntime().addShutdownHook(configurationShutdownHook);
    }

    public void uninstallConfigurationShutdownHook() {
        if (configurationShutdownHook != null) {
            Runtime.getRuntime().removeShutdownHook(configurationShutdownHook);
        }
    }
}
