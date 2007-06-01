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

import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.objectstyle.cayenne.access.DataContext;

/**
 * WebApplicationContextProvider is a helper class integrating Cayenne with web applications.
 * It performs two related operations - (1) whenever an HttpSession is started, it creates a 
 * new instance of DataContext and sets it as a session attribute; (2) whenever a new request 
 * comes in for the session, it retrieves previously stored session DataContext and binds it to
 * the request worker thread for the duration of the request. This way any class can get access to 
 * the correct instance of DataContext without knowing anything about the servlet environment. This 
 * is useful for decoupling the controller layer (Servlets, Struts, etc.) from a DAO layer, 
 * making public DAO API independent from DataContext.
 * 
 * <p><b>Installation in the web container:</b> WebApplicationContextProvider should be
 * installed as a listener, similar to the WebApplicationListener). Per servlet specification,
 * listener is configured in <code>web.xml</code> deployment descriptor:
 * </p>
 * 
 * <pre>
 * &lt;listener&gt;
 *   &lt;listener-class&gt;org.objectstyle.cayenne.conf.WebApplicationContextProvider&lt;/listener-class&gt;
 * &lt;/listener&gt;
 * </pre>
 * 
 * <p><b>Accessing thread DataContext:</b> A DataContext bound to a worker thread can be retrieved 
 * for the duration of the request processing by calling the static method
 * {@link DataContext#getThreadDataContext()}.
 * </p>
 * 
 * <p>
 * <p><b>Upgrading from {@link WebApplicationListener}:</b> WebApplicationContextProvider supersedes
 * WebApplicationListener. WebApplicationContextProvider relies on ServletRequestListener
 * API that is a part of Servlet Specification since version 2.4. Therefore it requires a
 * container that supports 2.4 spec, e.g. Tomcat 5.* </p>
 * </p>
 * 
 * @author <a href="mailto:scott@smartblob.com">Scott McClure </a>
 * @since 1.1
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
            DataContext dataContext = BasicServletConfiguration
                    .getDefaultContext(session);

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