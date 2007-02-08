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

import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.cayenne.access.DataContext;

/**
 * WebApplicationContextProvider is a helper class integrating Cayenne with web
 * applications. It performs two related operations - (1) whenever an HttpSession is
 * started, it creates a new instance of DataContext and sets it as a session attribute;
 * (2) whenever a new request comes in for the session, it retrieves previously stored
 * session DataContext and binds it to the request worker thread for the duration of the
 * request. This way any class can get access to the correct instance of DataContext
 * without knowing anything about the servlet environment. This is useful for decoupling
 * the controller layer (Servlets, Struts, etc.) from a DAO layer, making public DAO API
 * independent from DataContext.
 * <p>
 * <b>Installation in the web container:</b> WebApplicationContextProvider should be
 * installed as a listener, similar to the WebApplicationListener). Per servlet
 * specification, listener is configured in <code>web.xml</code> deployment descriptor:
 * </p>
 * 
 * <pre>
 *        &lt;listener&gt;
 *          &lt;listener-class&gt;org.apache.cayenne.conf.WebApplicationContextProvider&lt;/listener-class&gt;
 *        &lt;/listener&gt;
 * </pre>
 * 
 * <p>
 * <b>Accessing thread DataContext:</b> A DataContext bound to a worker thread can be
 * retrieved for the duration of the request processing by calling the static method
 * {@link DataContext#getThreadDataContext()}.
 * </p>
 * <p>
 * <p>
 * <b>Upgrading from {@link WebApplicationListener}:</b> WebApplicationContextProvider
 * supersedes WebApplicationListener. WebApplicationContextProvider relies on
 * ServletRequestListener API that is a part of Servlet Specification since version 2.4.
 * Therefore it requires a container that supports 2.4 spec, e.g. Tomcat 5.*
 * </p>
 * </p>
 * 
 * @author <a href="mailto:scott@smartblob.com">Scott McClure </a>
 * @since 1.1
 * @deprecated since 1.2. This class is deprecated to reduce confusion due to multiple
 *             redundant choices of web application configuration.
 *             {@link org.apache.cayenne.conf.WebApplicationContextFilter} is the
 *             official configuration choice for Cayenne, however you can still use a
 *             custom listener similar to WebApplicationContextProvider if you want to.
 */
public class WebApplicationContextProvider extends WebApplicationListener implements
        ServletRequestListener {

    /**
     * Retrieves the DataContext bound earlier to the HttpSession, and binds it to the
     * current thread.
     */
    public void requestInitialized(ServletRequestEvent sre) {

        ServletRequest req = sre.getServletRequest();
        if (req instanceof HttpServletRequest) {

            HttpSession session = ((HttpServletRequest) req).getSession();
            DataContext dataContext = ServletUtil.getSessionContext(session);

            // even if null, reset to null is a good thing here...
            DataContext.bindThreadDataContext(dataContext);
            if (dataContext == null) {
                throw new IllegalStateException(
                        "DataContext is null for the session, cannot bind to thread.");
            }
        }
    }

    /**
     * Removes the reference to the DataContext from the thread.
     */
    public void requestDestroyed(ServletRequestEvent sre) {
        // Prevent lingering object references
        DataContext.bindThreadDataContext(null);
    }
}
