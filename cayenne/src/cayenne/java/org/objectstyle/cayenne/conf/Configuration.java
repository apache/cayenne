/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.conf;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.lang.Validate;
import org.apache.commons.collections.Predicate;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ConfigurationException;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.util.CayenneMap;
import org.objectstyle.cayenne.util.ResourceLocator;
import org.objectstyle.cayenne.dataview.DataView;

/**
 * This class is an entry point to Cayenne. It loads all
 * configuration files and instantiates main Cayenne objects. Used as a
 * singleton via the {@link #getSharedConfiguration} method.
 *
 * <p>To use a custom subclass of Configuration, Java applications must
 * call {@link #initializeSharedConfiguration} with the subclass as argument.
 * This will create and initialize a Configuration singleton instance of the
 * specified class. By default {@link DefaultConfiguration} is instantiated.
 * </p>
 *
 * @author Andrei Adamchik
 * @author Holger Hoffstaette
 */
public abstract class Configuration {
    private static Logger logObj = Logger.getLogger(Configuration.class);

    public static final String DEFAULT_LOGGING_PROPS_FILE = ".cayenne/cayenne-log.properties";
    public static final String DEFAULT_DOMAIN_FILE = "cayenne.xml";
    public static final Class DEFAULT_CONFIGURATION_CLASS = DefaultConfiguration.class;

    protected static Configuration sharedConfiguration = null;
    private static boolean loggingConfigured = false;

    public static final Predicate ACCEPT_ALL_DATAVIEWS = new Predicate() {
      public boolean evaluate(Object dataViewName) {
        return true;
      }
    };

    /**
     * Defines a ClassLoader to use for resource lookup.
     * Configuration objects that are using ClassLoaders
     * to locate resources may need to be bootstrapped
     * explicitly.
     */
	protected static ClassLoader resourceLoader = Configuration.class.getClassLoader();

	static {
		if(Configuration.resourceLoader == null) {
			Configuration.resourceLoader = ClassLoader.getSystemClassLoader();
		}
	}

    /** Lookup map that stores DataDomains with names as keys. */
	protected CayenneMap dataDomains = new CayenneMap(this);
	protected Collection dataDomainsRef = Collections.unmodifiableCollection(dataDomains.values());
	protected DataSourceFactory overrideFactory;
	protected ConfigStatus loadStatus = new ConfigStatus();
	protected String domainConfigurationName = DEFAULT_DOMAIN_FILE;
	protected boolean ignoringLoadFailures;
    protected ConfigLoaderDelegate loaderDelegate;
    protected ConfigurationShutdownHook configurationShutdownHook = new ConfigurationShutdownHook();
    protected Map dataViewLocations = new HashMap();
    protected String projectVersion;

	/**
	 * Sets <code>cl</code> class's ClassLoader to serve
	 * as shared configuration resource ClassLoader.
	 * If shared Configuration object does not use ClassLoader,
	 * this method call will have no effect on how resources are loaded.
	 */
	public static void bootstrapSharedConfiguration(Class cl) {
		if(cl.getClassLoader() != null) {
		    resourceLoader = cl.getClassLoader();
		}
		else {
			logObj.debug("An attempt to bootstrap configuration with null class loader for class " + cl.getName());
		}
	}

    /**
     * Configures Cayenne logging properties.
     * Search for the properties file called <code>cayenne-log.properties</code>
     * is first done in $HOME/.cayenne, then in CLASSPATH.
     */
    public synchronized static void configureCommonLogging() {
        if (!Configuration.isLoggingConfigured()) {
			// create a simple CLASSPATH/$HOME locator
            ResourceLocator locator = new ResourceLocator();
            locator.setSkipAbsolutePath(true);
            locator.setSkipClasspath(false);
            locator.setSkipCurrentDirectory(true);
            locator.setSkipHomeDirectory(false);

            // and load the default logging config file
            URL configURL = locator.findResource(DEFAULT_LOGGING_PROPS_FILE);
			Configuration.configureCommonLogging(configURL);
        }
    }

    /**
     * Configures Cayenne logging properties using properties found at the specified URL.
     */
    public synchronized static void configureCommonLogging(URL propsFile) {
        if (!Configuration.isLoggingConfigured()) {
            if (propsFile != null) {
                PropertyConfigurator.configure(propsFile);
				logObj.debug("configured log4j from: " + propsFile);
            } else {
                BasicConfigurator.configure();
                logObj.debug("configured log4j with BasicConfigurator.");
            }

			// remember configuration success
            Configuration.setLoggingConfigured(true);
        }
    }

	/**
	 * Indicates whether Log4j has been initialized, either by cayenne
	 * or otherwise. If an external setup has been detected,
	 * {@link #setLoggingConfigured} will be called to remember this.
	 */
	public static boolean isLoggingConfigured() {
		if (!loggingConfigured) {
			// check for existing log4j setup
			if (Logger.getRootLogger().getAllAppenders().hasMoreElements()) {
				Configuration.setLoggingConfigured(true);
			}
		}

		return loggingConfigured;
	}

	/**
	 * Indicate whether Log4j has been initialized. Can be used when
	 * subclasses customize the initialization process, or to configure
     * Log4J outside of Cayenne.
	 */
	public synchronized static void setLoggingConfigured(boolean state) {
		loggingConfigured = state;
	}

	/**
	 * Use this method as an entry point to all Cayenne access objects.
	 * <p>Note that if you want to provide a custom Configuration,
	 * make sure you call one of the {@link #initializeSharedConfiguration} methods
	 * before your application code has a chance to call this method.
	 */
	public synchronized static Configuration getSharedConfiguration() {
		if (Configuration.sharedConfiguration == null) {
			Configuration.initializeSharedConfiguration();
		}

		return Configuration.sharedConfiguration;
	}

	/**
	 * Returns the ClassLoader used to load resources.
	 */
    public static ClassLoader getResourceLoader() {
        return Configuration.resourceLoader;
    }

    /**
     * Returns default log level for loading configuration.
     * Log level is made static so that applications can set it
     * before shared Configuration object is instantiated.
     */
    public static Level getLoggingLevel() {
    	Level l = logObj.getLevel();
    	return (l != null ? l : Level.DEBUG);
    }

    /**
     * Sets the default log level for loading a configuration.
     */
    public static void setLoggingLevel(Level logLevel) {
		logObj.setLevel(logLevel);
    }

	/**
	 * Creates and initializes shared Configuration object.
	 * By default {@link DefaultConfiguration} will be
	 * instantiated and assigned to a singleton instance of
	 * Configuration.
	 */
	public static void initializeSharedConfiguration() {
		Configuration.initializeSharedConfiguration(DEFAULT_CONFIGURATION_CLASS);
	}

	/**
	 * Creates and initializes a shared Configuration object of a
	 * custom Configuration subclass.
	 */
	public static void initializeSharedConfiguration(Class configurationClass) {
		Configuration conf = null;

		try {
			conf = (Configuration)configurationClass.newInstance();
		} catch (Exception ex) {
			logObj.error("Error creating shared Configuration: ", ex);
			throw new ConfigurationException("Error creating shared Configuration." + ex.getMessage(), ex);
		}

		Configuration.initializeSharedConfiguration(conf);
	}

	/**
	 * Sets the shared Configuration object to a new Configuration object.
	 * First calls {@link #canInitialize} and - if permitted -
	 * {@link #initialize} followed by {@link #didInitialize}.
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
		} catch (Exception ex) {
			throw new ConfigurationException("Error during Configuration initialization. " + ex.getMessage(), ex);
		}
	}

	/**
	 * Default constructor for new Configuration instances.
	 * Simply calls {@link Configuration#Configuration(String)}.
	 * @see Configuration#Configuration(String)
	 */
	protected Configuration() {
		this(DEFAULT_DOMAIN_FILE);
	}

	/**
	 * Default constructor for new Configuration instances using the
	 * given resource name as the main domain file.
	 * First calls {@link #configureLogging}, then {@link #setDomainConfigurationName}
	 * with the given domain configuration resource name.
	 */
	protected Configuration(String domainConfigurationName) {
		super();

		// set up logging
		this.configureLogging();

		// set domain configuration name
		this.setDomainConfigurationName(domainConfigurationName);
	}

	/**
	 * Indicates whether {@link #initialize} can be called.
	 * Returning <code>false</code> allows new instances to delay
	 * or refuse the initialization process.
	 */
	public abstract boolean canInitialize();

	/**
	 * Initializes the new instance.
	 * @throws Exception
	 */
	public abstract void initialize() throws Exception;

	/**
	 * Called after successful completion of {@link #initialize}.
	 */
	public abstract void didInitialize();

	/**
	 * Returns the resource locator used for finding and loading resources.
	 */
	protected abstract ResourceLocator getResourceLocator();

	/**
	 * Returns a DataDomain as a stream or <code>null</code>
	 * if it cannot be found.
	 */
	// TODO: this method is only used in sublcass (DefaultConfiguration),
	// should we remove it from here?
	protected abstract InputStream getDomainConfiguration();

	/**
	 * Returns a DataMap with the given name or <code>null</code>
	 * if it cannot be found.
	 */
	protected abstract InputStream getMapConfiguration(String name);

    protected abstract InputStream getViewConfiguration(String location);


    /**
     * Configures log4J. This implementation calls
     * {@link Configuration#configureCommonLogging}.
     */
    protected void configureLogging() {
        Configuration.configureCommonLogging();
    }

	/**
	 * Returns the name of the main domain configuration resource.
	 * Defaults to {@link Configuration#DEFAULT_DOMAIN_FILE}.
	 */
	public String getDomainConfigurationName() {
		return this.domainConfigurationName;
	}

	/**
	 * Sets the name of the main domain configuration resource.
	 * @param domainConfigurationName the name of the resource that contains
	 * this Configuration's domain(s).
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
     * Returns an internal property for the DataSource factory that
     * will override any settings configured in XML.
     * Subclasses may override this method to provide a special factory for
     * DataSource creation that will take precedence over any factories
     * configured in a cayenne project.
     */
    public DataSourceFactory getDataSourceFactory() {
        return this.overrideFactory;
    }

    public void setDataSourceFactory(DataSourceFactory overrideFactory) {
        this.overrideFactory = overrideFactory;
    }

    /**
     * Adds new DataDomain to the list of registered domains.
     */
    public void addDomain(DataDomain domain) {
        this.dataDomains.put(domain.getName(), domain);
		logObj.debug("added domain: " + domain.getName());
    }

    /**
     * Returns registered domain matching <code>name</code>
     * or <code>null</code> if no such domain is found.
     */
    public DataDomain getDomain(String name) {
        return (DataDomain)this.dataDomains.get(name);
    }

    /**
     * Returns default domain of this configuration. If no domains are
     * configured, <code>null</code> is returned. If more than one domain
     * exists in this configuration, a CayenneRuntimeException is thrown,
     * indicating that the domain name must be explicitly specified.
     * In such cases {@link #getDomain(String name)} must be used instead.
     */
    public DataDomain getDomain() {
        int size = this.dataDomains.size();
        if (size == 0) {
            return null;
        } else if (size == 1) {
            return (DataDomain)this.dataDomains.values().iterator().next();
        } else {
            throw new CayenneRuntimeException("More than one domain is configured; use 'getDomain(String name)' instead.");
        }
    }

    /**
     * Unregisters DataDomain matching <code>name<code> from
     * this Configuration object. Note that any domain database
     * connections remain open, and it is a responsibility of a
     * caller to clean it up.
     */
    public void removeDomain(String name) {
        this.dataDomains.remove(name);
		logObj.debug("removed domain: " + name);
    }

	/**
	 * Returns an unmodifiable collection of registered {@link DataDomain} objects.
	 */
	public Collection getDomains() {
		return this.dataDomainsRef;
	}

    /**
     * Returns whether to ignore any failures during map loading or not.
     * @return boolean
     */
    public boolean isIgnoringLoadFailures() {
        return this.ignoringLoadFailures;
    }

    /**
     * Sets whether to ignore any failures during map loading or not.
     * @param ignoringLoadFailures <code>true</code> or <code>false</code>
     */
    protected void setIgnoringLoadFailures(boolean ignoringLoadFailures) {
        this.ignoringLoadFailures = ignoringLoadFailures;
    }

	/**
	 * Returns the load status.
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
     * @param loaderDelegate
     */
    public void setLoaderDelegate(ConfigLoaderDelegate loaderDelegate) {
        this.loaderDelegate = loaderDelegate;
    }

    /**
     * Initializes configuration with the location of data views.
     * 
     * @since 1.1
     * @param dataViewLocations Map of DataView locations.
     */
    public void setDataViewLocations(Map dataViewLocations) {
        if (dataViewLocations == null)
            this.dataViewLocations = new HashMap();
        else
            this.dataViewLocations = dataViewLocations;
    }

    /**
     * @since 1.1
     */
    public Map getDataViewLocations() {
        return dataViewLocations;
    }

    /**
     * @since 1.1
     */
    public boolean loadDataView(DataView dataView) throws IOException {
        return loadDataView(dataView, Configuration.ACCEPT_ALL_DATAVIEWS);
    }

    /**
     * @since 1.1
     */
    public boolean loadDataView(DataView dataView, Predicate dataViewNameFilter)
        throws IOException {
            
        Validate.notNull(dataView, "DataView cannot be null.");

        if (dataViewLocations.size() == 0 || dataViewLocations.size() > 512) {
            return false;
        }

        if (dataViewNameFilter == null)
            dataViewNameFilter = Configuration.ACCEPT_ALL_DATAVIEWS;

        List viewXMLSources = new ArrayList(dataViewLocations.size());
        int index = 0;
        for (Iterator i = dataViewLocations.entrySet().iterator();
            i.hasNext();
            index++) {
            Map.Entry entry = (Map.Entry) i.next();
            String name = (String) entry.getKey();
            if (!dataViewNameFilter.evaluate(name))
                continue;
            String location = (String) entry.getValue();
            InputStream in = getViewConfiguration(location);
            if (in != null)
                viewXMLSources.add(in);
        }

        if (viewXMLSources.isEmpty())
            return false;

        dataView.load(
            (InputStream[]) viewXMLSources.toArray(
                new InputStream[viewXMLSources.size()]));
        return true;
    }

    /**
     * Shutdowns all owned domains. Invokes DataDomain.shutdown().
     */
    public void shutdown() {
        Collection domains = getDomains();
        for (Iterator i = domains.iterator(); i.hasNext(); ) {
            DataDomain domain = (DataDomain)i.next();
            domain.shutdown();
        }
    }

    private class ConfigurationShutdownHook extends Thread {
        public void run() {
            shutdown();
        }
    }

    public void installConfigurationShutdownHook() {
        uninstallConfigurationShutdownHook();
        Runtime.getRuntime().addShutdownHook(configurationShutdownHook);
    }

    public void uninstallConfigurationShutdownHook() {
        Runtime.getRuntime().removeShutdownHook(configurationShutdownHook);
    }
}