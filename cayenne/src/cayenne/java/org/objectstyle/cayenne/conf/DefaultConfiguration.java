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

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.ConfigurationException;
import org.objectstyle.cayenne.util.ResourceLocator;
import org.objectstyle.cayenne.util.Util;

/**
 * Subclass of Configuration that uses the System CLASSPATH to locate resources.
 * If Cayenne classes are loaded using a different ClassLoader from
 * the application classes, this configuration needs to be bootstrapped
 * by calling {@link Configuration#bootstrapSharedConfiguration(Class)}</code>.
 *
 * @author Andrei Adamchik
 */
public class DefaultConfiguration extends Configuration {
	private static Logger logObj = Logger.getLogger(DefaultConfiguration.class);

	/**
	 * the default ResourceLocator used for CLASSPATH loading
	 */
	private ResourceLocator locator;

	/**
	 * Default constructor.
	 * Simply calls {@link DefaultConfiguration#DefaultConfiguration(String)}
	 * with {@link Configuration#DEFAULT_DOMAIN_FILE} as argument.
	 * @see Configuration#Configuration()
	 */
	public DefaultConfiguration() {
		this(Configuration.DEFAULT_DOMAIN_FILE);
	}

	/**
	 * Constructor with a named domain configuration resource.
	 * Simply calls {@link Configuration#Configuration(String)}.
	 * @throws ConfigurationException when <code>domainConfigurationName</code>
	 * is <code>null</code>.
	 * @see Configuration#Configuration(String)
	 */
	public DefaultConfiguration(String domainConfigurationName) {
		super(domainConfigurationName);

		if (domainConfigurationName == null) {
			throw new ConfigurationException("cannot use null as domain file name.");
		}

		logObj.debug("using domain file name: " + domainConfigurationName);

		// configure CLASSPATH-only locator
		ResourceLocator l = new ResourceLocator();
		l.setSkipAbsolutePath(true);
		l.setSkipClasspath(false);
		l.setSkipCurrentDirectory(true);
		l.setSkipHomeDirectory(true);

		// add the current Configuration subclass' package as additional path.
		if (!(this.getClass().equals(DefaultConfiguration.class))) {
			l.addClassPath(Util.getPackagePath(this.getClass().getName()));
		}

		// The Configuration superclass statically defines what
		// ClassLoader to use for resources. This allows applications to
		// control where resources are loaded from.
		l.setClassLoader(Configuration.getResourceLoader());

		// remember configured ResourceLocator
		this.setResourceLocator(l);
	}

	/**
	 * Adds a custom path for class path lookups.
	 * Format should be "my/package/name" <i>without</i> leading "/".
	 *
	 * This allows for easy customization of custom search paths after
	 * Constructor invocation:
	 * <pre>
	 * conf = new DefaultConfiguration();
	 * conf.addClassPath("my/package/name");
	 * Configuration.initializeSharedConfiguration(conf);
	 * </pre>
	 *
	 */
	public void addClassPath(String customPath) {
		this.getResourceLocator().addClassPath(customPath);
	}

	/**
	 * Default implementation of {@link Configuration#canInitialize}.
	 * Creates a ResourceLocator suitable for loading from the CLASSPATH,
	 * unless it has already been set in a subclass.
	 * Always returns <code>true</code>.
	 */
	public boolean canInitialize() {
		logObj.debug("canInitialize started.");
		// allow to proceed
		return true;
	}

	/**
	 * Initializes all Cayenne resources. Loads all configured domains and their
	 * data maps, initializes all domain Nodes and their DataSources.
	 */
	public void initialize() throws Exception {
		logObj.debug("initialize starting.");

		InputStream in = this.getDomainConfiguration();
		if (in == null) {
			StringBuffer msg = new StringBuffer();
			msg
				.append("[")
				.append(this.getClass().getName())
				.append("] : Domain configuration file \"")
				.append(DEFAULT_DOMAIN_FILE)
				.append("\" is not found.");

			throw new ConfigurationException(msg.toString());
		}

		ConfigLoaderDelegate delegate = this.getLoaderDelegate();
		if (delegate == null) {
			delegate = new RuntimeLoadDelegate(this, this.getLoadStatus(), Configuration.getLoggingLevel());
		}

		ConfigLoader loader = new ConfigLoader(delegate);

		try {
			loader.loadDomains(in);
		} finally {
			this.setLoadStatus(delegate.getStatus());
			in.close();
		}

		// log successful initialization
		logObj.debug("initialize finished.");
	}

	/**
	 * Default implementation of {@link Configuration#didInitialize}.
	 * Currently does nothing except logging.
	 */
	public void didInitialize() {
		// empty default implementation
		logObj.debug("didInitialize finished.");
	}

	/**
	 * Returns the default ResourceLocator configured for CLASSPATH lookups.
	 */
	protected ResourceLocator getResourceLocator() {
		return this.locator;
	}

	/**
	 * Sets the specified {@link ResourceLocator}.
	 * Currently called from {@link #initialize}.
	 */
	protected void setResourceLocator(ResourceLocator locator) {
		this.locator = locator;
	}

	/**
	 * Returns the domain configuration as a stream or <code>null</code> if it
	 * cannot be found. Uses the configured {@link ResourceLocator} to
	 * find the file.
	 */
	protected InputStream getDomainConfiguration() {
		return locator.findResourceStream(this.getDomainConfigurationName());
	}

	/**
	 * Returns the {@link org.objectstyle.cayenne.map.DataMap} configuration
	 * from a specified location or <code>null</code> if it cannot be found.
	 * Uses the configured {@link ResourceLocator} to find the file.
	 */
	protected InputStream getMapConfiguration(String location) {
		return locator.findResourceStream(location);
	}

    protected InputStream getViewConfiguration(String location) {
        return locator.findResourceStream(location);
	}

	/**
	 * @see Object#toString()
	 */
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