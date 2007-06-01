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
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * WebApplicationListener utilizes Servlet specification 2.3 features to react on
 * webapplication container events inializing Cayenne.
 * <p>
 * It performs the following tasks:
 * <ul>
 * <li>Loads Cayenne configuration when the application is started within container.
 * </li>
 * <li>Assigns new DataContext to every new session created within the application.</li>
 * </ul>
 * </p>
 * <p>
 * CayenneWebappListener must be configured in <code>web.xml</code> deployment
 * descriptor as a listener of context and session events:
 * </p>
 * 
 * <pre>
 * &lt;listener&gt;
 *  &lt;listener-class&gt;org.objectstyle.cayenne.conf.WebApplicationListener&lt;/listener-class&gt;
 *  &lt;/listener&gt;
 * </pre>
 * 
 * <p>
 * Note that to set WebApplicationListener as a listener of web application events, you
 * must use servlet containers compatible with Servlet Specification 2.3 (such as Tomcat
 * 4.0). Listeners were only added to servlet specification in 2.3. If you are using an
 * older container, you will need to configure Cayenne in your code.
 * </p>
 * 
 * @author Andrei Adamchik
 */
public class WebApplicationListener implements HttpSessionListener,
        ServletContextListener {

    public WebApplicationListener() {
    }

    /**
     * Establishes a Cayenne shared Configuration object that can later be obtained by
     * calling <code>Configuration.getSharedConfiguration()</code>. This method is a
     * part of ServletContextListener interface and is called on application startup.
     */
    public void contextInitialized(ServletContextEvent sce) {
        setConfiguration(newConfiguration(sce.getServletContext()));
    }

    /**
     * Currently does nothing. <i>In the future it should close down any database
     * connections if they wheren't obtained via JNDI. </i> This method is a part of
     * ServletContextListener interface and is called on application shutdown.
     */
    public void contextDestroyed(ServletContextEvent sce) {
    }

    /**
     * Creates and assigns a new data context based on default domain to the session
     * object associated with this event. This method is a part of HttpSessionListener
     * interface and is called every time when a new session is created.
     */
    public void sessionCreated(HttpSessionEvent se) {
        se.getSession().setAttribute(
                BasicServletConfiguration.DATA_CONTEXT_KEY,
                getConfiguration().getDomain().createDataContext());
    }

    /**
     * Does nothing. This method is a part of HttpSessionListener interface and is called
     * every time when a session is destroyed.
     */
    public void sessionDestroyed(HttpSessionEvent se) {
    }

    /**
     * Return an instance of Configuration that will be initialized as the shared
     * configuration. Provides an extension point for the developer to provide their own
     * custom configuration.
     */
    protected Configuration newConfiguration(ServletContext sc) {
        return new BasicServletConfiguration(sc);
    }

    /** Initializes the configuration. */
    protected void setConfiguration(Configuration configuration) {
        Configuration.initializeSharedConfiguration(configuration);
    }

    /** Returns the current configuration. */
    protected Configuration getConfiguration() {
        return Configuration.getSharedConfiguration();
    }
}