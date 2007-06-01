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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.util.ResourceLocator;
import org.objectstyle.cayenne.util.WebApplicationResourceLocator;

/**
  * BasicServletConfiguration is a Configuration that uses ServletContext 
  * to locate resources. 
  * This class can only be used in a context of a servlet/jsp container.
  * It resolves configuration file paths relative to the web application
  * "WEB-INF" directory.
  * 
  * <p>
  * BasicServletConfiguration is compatible with Servlet Specification 2.2 and higher.
  * Also look at ServletConfiguration for the information how to utilize listeners 
  * introduced in Servlet Specification 2.3.
  * </p>
  *
  * @author Andrei Adamchik
  * @author Scott Finnerty
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
