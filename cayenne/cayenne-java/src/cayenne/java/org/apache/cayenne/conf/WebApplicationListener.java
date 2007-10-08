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
 *   &lt;listener&gt;
 *    &lt;listener-class&gt;org.apache.cayenne.conf.WebApplicationListener&lt;/listener-class&gt;
 *    &lt;/listener&gt;
 * </pre>
 * 
 * <p>
 * Note that to set WebApplicationListener as a listener of web application events, you
 * must use servlet containers compatible with Servlet Specification 2.3 (such as Tomcat
 * 4.0). Listeners were only added to servlet specification in 2.3. If you are using an
 * older container, you will need to configure Cayenne in your code.
 * </p>
 * 
 * @author Andrus Adamchik
 * @deprecated since 1.2. This class is deprecated to reduce confusion due to multiple
 *             redundant choices of web application configuration.
 *             {@link org.apache.cayenne.conf.WebApplicationContextFilter} is the
 *             official configuration choice for Cayenne, however you can still use a
 *             custom listener similar to WebApplicationListener if you want to.
 */
public class WebApplicationListener implements HttpSessionListener,
        ServletContextListener {

    /**
     * Establishes a Cayenne shared Configuration object that can later be obtained by
     * calling <code>Configuration.getSharedConfiguration()</code>. This method is a
     * part of ServletContextListener interface and is called on application startup.
     */
    public void contextInitialized(ServletContextEvent sce) {
        ServletUtil.initializeSharedConfiguration(sce.getServletContext());
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
                ServletUtil.DATA_CONTEXT_KEY,
                getConfiguration().getDomain().createDataContext());
    }

    /**
     * Does nothing. This method is a part of HttpSessionListener interface and is called
     * every time when a session is destroyed.
     */
    public void sessionDestroyed(HttpSessionEvent se) {
    }

    /**
     * Returns an instance of Configuration that will be initialized as the shared
     * configuration. Provides an extension point for the developer to provide their own
     * custom configuration.
     * 
     * @deprecated since 1.2
     */
    protected Configuration newConfiguration(ServletContext sc) {
        return new BasicServletConfiguration(sc);
    }

    /**
     * Initializes the configuration.
     */
    protected void setConfiguration(Configuration configuration) {
        Configuration.initializeSharedConfiguration(configuration);
    }

    /** Returns the current configuration. */
    protected Configuration getConfiguration() {
        return Configuration.getSharedConfiguration();
    }
}
