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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.util.ResourceLocator;
import org.apache.cayenne.util.WebApplicationResourceLocator;

/**
 * BasicServletConfiguration is a Configuration that uses ServletContext to locate
 * resources. This class can only be used in a context of a servlet/jsp container. It
 * resolves configuration file paths relative to the web application "WEB-INF" directory.
 * <p>
 * BasicServletConfiguration is compatible with Servlet Specification 2.2 and higher. Also
 * look at ServletConfiguration for the information how to utilize listeners introduced in
 * Servlet Specification 2.3.
 * </p>
 * 
 * @author Andrei Adamchik
 * @author Scott Finnerty
 * @deprecated Since 1.2 ServletUtil is used instead, as the actual file loading strategy
 *             is defined at the ResourceLocator elevel, and this class provides no value
 *             of its own.
 */
public class BasicServletConfiguration extends DefaultConfiguration {
	private static Logger logObj = Logger.getLogger(BasicServletConfiguration.class);

	public static final String CONFIGURATION_PATH_KEY =
		"cayenne.configuration.path";
	public static final String DATA_CONTEXT_KEY = "cayenne.datacontext";

	protected ServletContext servletContext;

	public synchronized static BasicServletConfiguration initializeConfiguration(ServletContext ctxt) {
		// check if this web application already has a servlet configuration
		// sometimes multiple initializations are done by mistake...
		
		// Andrus: are there any cases when reinitialization is absolutely required?
		
		// don't use static getter, since it will do initialization on demand!!!
		Configuration oldConfiguration = Configuration.sharedConfiguration;
		if (oldConfiguration instanceof BasicServletConfiguration) {
			BasicServletConfiguration basicConfiguration =
				(BasicServletConfiguration) oldConfiguration;
			if (basicConfiguration.getServletContext() == ctxt) {
				logObj.info(
					"BasicServletConfiguration is already initialized, reusing.");
				return basicConfiguration;
			}
		}

		BasicServletConfiguration conf = new BasicServletConfiguration(ctxt);
		Configuration.initializeSharedConfiguration(conf);

		return conf;
	}

	/** 
	 * Returns default Cayenne DataContext associated with the HttpSession.
	 * If no DataContext exists in the session, it is created on the spot. 
	 */
	public static DataContext getDefaultContext(HttpSession session) {
		synchronized (session) {
			DataContext ctxt =
				(DataContext) session.getAttribute(DATA_CONTEXT_KEY);

			if (ctxt == null) {
				ctxt = DataContext.createDataContext();
				session.setAttribute(
					BasicServletConfiguration.DATA_CONTEXT_KEY,
					ctxt);
			}

			return ctxt;
		}
	}

	public BasicServletConfiguration() {
		super();

	}

	public BasicServletConfiguration(ServletContext ctxt) {
		super();
		this.setServletContext(ctxt);

		ResourceLocator l = new WebApplicationResourceLocator(servletContext);
		l.setSkipAbsolutePath(true);
		l.setSkipClasspath(false);
		l.setSkipCurrentDirectory(true);
		l.setSkipHomeDirectory(true);

		// check for a configuration path in the context parameters
		String configurationPath =
			ctxt.getInitParameter(CONFIGURATION_PATH_KEY);
		if (configurationPath != null
			&& configurationPath.trim().length() > 0) {
			l.addFilesystemPath(configurationPath);
		}

		this.setResourceLocator(l);
	}

	/**
	 * Sets the servletContext.
	 * @param servletContext The servletContext to set
	 */
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/** Returns current application context object. */
	public ServletContext getServletContext() {
		return servletContext;
	}

	public boolean canInitialize() {
		return (getServletContext() != null);
	}
}
